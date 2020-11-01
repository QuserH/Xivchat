using System;
using System.ComponentModel;
using System.Linq;
using System.Net.Sockets;
using System.Threading;
using System.Threading.Channels;
using System.Threading.Tasks;
using System.Windows;
using XIVChatCommon;

namespace XIVChat_Desktop {
    public class Connection : INotifyPropertyChanged {
        private readonly App app;

        private readonly string host;
        private readonly ushort port;

        private TcpClient? client;

        private readonly Channel<string> outgoing = Channel.CreateUnbounded<string>();
        private readonly Channel<byte[]> incoming = Channel.CreateUnbounded<byte[]>();
        private readonly Channel<byte> cancelChannel = Channel.CreateBounded<byte>(2);

        public readonly CancellationTokenSource cancel = new CancellationTokenSource();

        public event PropertyChangedEventHandler? PropertyChanged;
        public string? CurrentChannel { get; private set; }

        public Connection(App app, string host, ushort port) {
            this.app = app;

            this.host = host;
            this.port = port;
        }

        public void SendMessage(string message) {
            this.outgoing.Writer.TryWrite(message);
        }

        public void Disconnect() {
            this.cancel.Cancel();
            for (var i = 0; i < 2; i++) {
                this.cancelChannel.Writer.TryWrite(1);
            }
        }

        public async Task Connect() {
            this.client = new TcpClient(this.host, this.port);
            var stream = this.client.GetStream();

            // write the magic bytes
            await stream.WriteAsync(new byte[] {
                14, 20, 67,
            });

            // do the handshake
            var handshake = await KeyExchange.ClientHandshake(this.app.Config.KeyPair, stream);

            // check for trust and prompt if not
            if (!this.app.Config.TrustedKeys.Any(trusted => trusted.Key.SequenceEqual(handshake.RemotePublicKey))) {
                var trustChannel = Channel.CreateBounded<bool>(1);

                this.Dispatch(() => {
                    new TrustDialog(this.app.Window, trustChannel.Writer, handshake.RemotePublicKey).Show();
                });

                var trusted = await trustChannel.Reader.ReadAsync(this.cancel.Token);

                if (!trusted) {
                    goto Close;
                }
            }

            // clear messages if connecting to a different host
            var currentHost = $"{this.host}:{this.port}";
            var sameHost = this.app.LastHost == currentHost;
            if (!sameHost) {
                this.Dispatch(() => {
                    this.app.Window.ClearAllMessages();
                    this.app.LastHost = currentHost;
                });
            }
            
            this.Dispatch(() => {
                this.app.Window.AddSystemMessage("Connected");
            });
            
            // check if backlog or catch-up is needed
            if (sameHost) {
                // catch-up
                var lastRealMessage = this.app.Window.Messages.FirstOrDefault(msg => msg.Channel != 0);
                if (lastRealMessage != null) {
                    var catchUp = new ClientCatchUp {
                        After = lastRealMessage.Timestamp,
                    };
                    await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, catchUp, this.cancel.Token);
                }
            } else if (this.app.Config.BacklogMessages > 0) {
                // backlog
                var backlogReq = new ClientBacklog {
                    Amount = this.app.Config.BacklogMessages,
                };
                await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, backlogReq, this.cancel.Token);
            }

            // start a task for accepting incoming messages and sending them down the channel
            _ = Task.Run(async () => {
                var inc = SecretMessage.ReadSecretMessage(stream, handshake.Keys.rx, this.cancel.Token);
                var cancel = this.cancelChannel.Reader.ReadAsync().AsTask();

                while (!this.cancel.IsCancellationRequested) {
                    var result = await Task.WhenAny(inc, cancel);
                    if (result == inc) {
                        if (inc.Exception != null) {
                            this.Dispatch(() => {
                                this.app.Window.AddSystemMessage("Error reading incoming message.");
                                // ReSharper disable once LocalizableElement
                                Console.WriteLine($"Error reading incoming message: {inc.Exception.Message}");
                                foreach (var inner in inc.Exception.InnerExceptions) {
                                    Console.WriteLine(inner.StackTrace);
                                }
                            });
                            break;
                        }

                        var rawMessage = await inc;
                        inc = SecretMessage.ReadSecretMessage(stream, handshake.Keys.rx, this.cancel.Token);
                        await this.incoming.Writer.WriteAsync(rawMessage);
                    } else if (result == cancel) {
                        break;
                    }
                }
            });

            var incoming = this.incoming.Reader.ReadAsync().AsTask();
            var outgoing = this.outgoing.Reader.ReadAsync().AsTask();
            var cancel = this.cancelChannel.Reader.ReadAsync().AsTask();

            // listen for incoming and outgoing messages and cancel requests
            while (!this.cancel.IsCancellationRequested) {
                var result = await Task.WhenAny(incoming, outgoing, cancel);
                if (result == incoming) {
                    if (this.incoming.Reader.Completion.IsCompleted) {
                        break;
                    }
                    var rawMessage = await incoming;
                    incoming = this.incoming.Reader.ReadAsync().AsTask();

                    await this.HandleIncoming(rawMessage);
                } else if (result == outgoing) {
                    var toSend = await outgoing;
                    outgoing = this.outgoing.Reader.ReadAsync().AsTask();

                    var message = new ClientMessage {
                        Content = toSend,
                    };
                    try {
                        await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, message, this.cancel.Token);
                    } catch (Exception ex) {
                        this.Dispatch(() => {
                            this.app.Window.AddSystemMessage("Error sending message.");
                            // ReSharper disable once LocalizableElement
                            Console.WriteLine($"Error sending message: {ex.Message}");
                            Console.WriteLine(ex.StackTrace);
                        });
                        break;
                    }
                } else if (result == cancel) {
                    try {
                        // NOTE: purposely not including cancellation token because it will already be cancelled here
                        //       and we need to send this message
                        await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, ClientShutdown.Instance);
                    } catch (Exception ex) {
                        this.Dispatch(() => {
                            this.app.Window.AddSystemMessage("Error sending message.");
                            // ReSharper disable once LocalizableElement
                            Console.WriteLine($"Error sending message: {ex.Message}");
                            Console.WriteLine(ex.StackTrace);
                        });
                    }

                    break;
                }
            }
            
            // at this point, we are disconnected, so log it
            this.Dispatch(() => {
                this.app.Window.AddSystemMessage("Disconnected");
            });

            // wait up to a second to send the shutdown packet
            await Task.WhenAny(Task.Delay(1_000), SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, ClientShutdown.Instance));

            Close:
            try {
                this.client.Close();
            } catch (ObjectDisposedException) { }
        }

        private async Task HandleIncoming(byte[] rawMessage) {
            var type = (ServerOperation)rawMessage[0];
            var payload = new byte[rawMessage.Length - 1];
            Array.Copy(rawMessage, 1, payload, 0, payload.Length);

            switch (type) {
                case ServerOperation.Pong:
                    // no-op
                    break;
                case ServerOperation.Message:
                    var message = ServerMessage.Decode(payload);

                    this.Dispatch(() => {
                        this.app.Window.AddMessage(message);
                    });
                    break;
                case ServerOperation.Shutdown:
                    this.Disconnect();
                    break;
                case ServerOperation.PlayerData:
                    var playerData = payload.Length == 0 ? null : PlayerData.Decode(payload);

                    var visibility = playerData == null ? Visibility.Collapsed : Visibility.Visible;

                    this.Dispatch(() => {
                        var window = this.app.Window;

                        window.LoggedInAs.Content = playerData?.name;
                        window.LoggedInAs.Visibility = visibility;

                        window.LoggedInAsSeparator.Visibility = visibility;

                        window.CurrentWorld.Content = playerData?.currentWorld;
                        window.CurrentWorld.Visibility = visibility;

                        window.CurrentWorldSeparator.Visibility = visibility;

                        window.Location.Content = playerData?.location;
                        window.Location.Visibility = visibility;
                    });
                    break;
                case ServerOperation.Availability:
                    break;
                case ServerOperation.Channel:
                    var channel = ServerChannel.Decode(payload);

                    this.CurrentChannel = channel.name;

                    this.Dispatch(() => {
                        this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(this.CurrentChannel)));
                    });
                    break;
                case ServerOperation.Backlog:
                    var backlog = ServerBacklog.Decode(payload);

                    foreach (var msg in backlog.messages) {
                        this.Dispatch(() => {
                            this.app.Window.AddMessage(msg);
                        });
                    }

                    break;
                case ServerOperation.PlayerList:
                    break;
                case ServerOperation.LinkshellList:
                    break;
            }
        }

        private void Dispatch(Action action) {
            this.app.Dispatcher.BeginInvoke(action);
        }
    }
}
