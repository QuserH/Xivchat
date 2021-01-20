using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Linq;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text;
using System.Threading;
using System.Threading.Channels;
using System.Threading.Tasks;
using System.Windows;
using System.Windows.Threading;
using XIVChatCommon;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Client;
using XIVChatCommon.Message.Server;

namespace XIVChat_Desktop {
    public class Connection : INotifyPropertyChanged {
        #if DEBUG
        private static readonly IEnumerable<byte> RelayPublicKey = new byte[] {
            8, 202, 178, 253, 125, 176, 212, 227, 110, 108, 113, 80, 110, 126, 57, 248, 182, 251, 122, 48, 80, 49, 57, 202, 119, 126, 69, 66, 170, 25, 126, 115,
        };
        #else
        private static readonly IEnumerable<byte> RelayPublicKey = new byte[] {
            194, 81, 22, 123, 80, 172, 145, 167, 212, 251, 198, 173, 55, 160, 11, 18, 247, 11, 210, 6, 98, 43, 102, 73, 54, 255, 214, 233, 144, 193, 98, 47
        };
        #endif

        #if DEBUG
        private const string RelayHost = "localhost";
        private const ushort RelayPort = 14725;
        #else
        private const string RelayHost = "relay.xiv.chat";
        private const ushort RelayPort = 14777;
        #endif

        private readonly App app;

        private readonly string host;
        private readonly ushort port;
        private readonly string? relayAuth;
        private readonly string? relayTarget;

        private TcpClient? client;

        private readonly Channel<string> outgoing = Channel.CreateUnbounded<string>();
        private readonly Channel<byte[]> outgoingMessages = Channel.CreateUnbounded<byte[]>();
        private readonly Channel<byte[]> incoming = Channel.CreateUnbounded<byte[]>();
        private readonly Channel<byte> cancelChannel = Channel.CreateBounded<byte>(2);

        public readonly CancellationTokenSource cancel = new CancellationTokenSource();

        public delegate void ReceiveMessageDelegate(ServerMessage message);

        public event ReceiveMessageDelegate? ReceiveMessage;

        public event PropertyChangedEventHandler? PropertyChanged;
        public string? CurrentChannel { get; private set; }

        private bool available;

        public bool Available {
            get => this.available;
            private set {
                this.available = value;
                this.OnPropertyChanged(nameof(this.Available));
            }
        }

        public Connection(App app, string host, ushort port, string? relayAuth = null, string? relayTarget = null) {
            this.app = app;

            this.host = host;
            this.port = port;
            this.relayAuth = relayAuth;
            this.relayTarget = relayTarget;
        }

        public void SendMessage(string message) {
            this.outgoing.Writer.TryWrite(message);
        }

        public void RequestFriendList() {
            var msg = new ClientPlayerList {
                Type = PlayerListType.Friend,
            };
            this.outgoingMessages.Writer.TryWrite(msg.Encode());
        }

        public void ChangeChannel(InputChannel channel) {
            var msg = new ClientChannel {
                Channel = channel,
            };
            this.outgoingMessages.Writer.TryWrite(msg.Encode());
        }

        public void Disconnect() {
            this.cancel.Cancel();
            for (var i = 0; i < 2; i++) {
                this.cancelChannel.Writer.TryWrite(1);
            }
        }

        public async Task Connect() {
            var usingRelay = this.relayAuth != null;

            var host = usingRelay ? RelayHost : this.host;
            var port = usingRelay ? RelayPort : this.port;

            this.client = new TcpClient(host, port);

            var stream = this.client.GetStream();

            switch (usingRelay) {
                // do relay auth before connecting if necessary
                case true: {
                    var relayHandshake = await KeyExchange.ClientHandshake(this.app.Config.KeyPair, stream);

                    // ensure the relay's public key is what we expect
                    if (!relayHandshake.RemotePublicKey.SequenceEqual(RelayPublicKey)) {
                        this.app.Dispatch(() => {
                            MessageBox.Show("Unexpected relay public key.");
                        });
                        return;
                    }

                    // send auth token
                    var authBytes = Encoding.UTF8.GetBytes(this.relayAuth!);
                    await SecretMessage.SendSecretMessage(stream, relayHandshake.Keys.tx, authBytes);

                    // TODO: receive response

                    // send the public key of the server
                    var pk = Util.StringToByteArray(this.relayTarget!);
                    await SecretMessage.SendSecretMessage(stream, relayHandshake.Keys.tx, pk);

                    // TODO: receive response
                    break;
                }
                // only send magic bytes if not using the relay
                case false:
                    // write the magic bytes
                    await stream.WriteAsync(new byte[] {
                        14, 20, 67,
                    });
                    break;
            }

            // do the handshake
            var handshake = await KeyExchange.ClientHandshake(this.app.Config.KeyPair, stream);

            // check for trust and prompt if not
            if (!this.app.Config.TrustedKeys.Any(trusted => trusted.Key.SequenceEqual(handshake.RemotePublicKey))) {
                var trustChannel = Channel.CreateBounded<bool>(1);

                this.app.Dispatch(() => {
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
                this.app.Dispatch(() => {
                    this.app.Window.ClearAllMessages();
                    this.app.LastHost = currentHost;
                });
            }

            this.app.Dispatch(() => {
                this.app.Window.AddSystemMessage("Connected");
            });

            // tell the server our preferences
            var preferences = new ClientPreferences {
                Preferences = new Dictionary<ClientPreference, object> {
                    {
                        ClientPreference.BacklogNewestMessagesFirst, true
                    },
                },
            };
            await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, preferences, this.cancel.Token);

            // check if backlog or catch-up is needed
            if (sameHost) {
                // catch-up
                var lastRealMessage = this.app.Window.Messages.LastOrDefault(msg => msg.Channel != 0);
                if (lastRealMessage != null) {
                    _backlogSequence += 1;
                    var catchUp = new ClientCatchUp(lastRealMessage.Timestamp);
                    await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, catchUp, this.cancel.Token);
                }
            } else if (this.app.Config.BacklogMessages > 0) {
                // backlog
                _backlogSequence += 1;
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
                        var ex = inc.Exception;
                        if (ex != null) {
                            this.app.Dispatch(() => {
                                this.app.Window.AddSystemMessage("Error reading incoming message.");
                                // ReSharper disable once LocalizableElement
                                Console.WriteLine($"Error reading incoming message: {ex.Message}");
                                foreach (var inner in ex.InnerExceptions) {
                                    Console.WriteLine(inner.StackTrace);
                                }
                            });
                            if (!(ex.InnerException is CryptographicException)) {
                                this.app.Disconnect();
                                break;
                            }
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
            var outgoingMessage = this.outgoingMessages.Reader.ReadAsync().AsTask();
            var cancel = this.cancelChannel.Reader.ReadAsync().AsTask();

            // listen for incoming and outgoing messages and cancel requests
            while (!this.cancel.IsCancellationRequested) {
                var result = await Task.WhenAny(incoming, outgoing, outgoingMessage, cancel);
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

                    var message = new ClientMessage(toSend);
                    try {
                        await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, message, this.cancel.Token);
                    } catch (Exception ex) {
                        this.app.Dispatch(() => {
                            this.app.Window.AddSystemMessage("Error sending message.");
                            // ReSharper disable once LocalizableElement
                            Console.WriteLine($"Error sending message: {ex.Message}");
                            Console.WriteLine(ex.StackTrace);
                        });
                        break;
                    }
                } else if (result == outgoingMessage) {
                    var toSend = await outgoingMessage;
                    outgoingMessage = this.outgoingMessages.Reader.ReadAsync().AsTask();

                    try {
                        await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, toSend, this.cancel.Token);
                    } catch (Exception ex) {
                        this.app.Dispatch(() => {
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
                        this.app.Dispatch(() => {
                            this.app.Window.AddSystemMessage("Error sending message.");
                            // ReSharper disable once LocalizableElement
                            Console.WriteLine($"Error sending message: {ex.Message}");
                            Console.WriteLine(ex.StackTrace);
                        });
                    }

                    break;
                }
            }

            // remove player data
            this.SetPlayerData(null);

            // set availability
            this.Available = false;

            // at this point, we are disconnected, so log it
            this.app.Dispatch(() => {
                this.app.Window.AddSystemMessage("Disconnected");
            });

            // wait up to a second to send the shutdown packet
            await Task.WhenAny(Task.Delay(1_000), SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, ClientShutdown.Instance));

            Close:
            try {
                this.client.Close();
            } catch (ObjectDisposedException) {
            }
        }

        private async Task HandleIncoming(byte[] rawMessage) {
            var type = (ServerOperation) rawMessage[0];
            var payload = new byte[rawMessage.Length - 1];
            Array.Copy(rawMessage, 1, payload, 0, payload.Length);

            switch (type) {
                case ServerOperation.Pong:
                    // no-op
                    break;
                case ServerOperation.Message:
                    var message = ServerMessage.Decode(payload);

                    this.app.Dispatch(() => {
                        this.ReceiveMessage?.Invoke(message);
                        this.app.Window.AddMessage(message);
                    });
                    break;
                case ServerOperation.Shutdown:
                    this.app.Disconnect();
                    break;
                case ServerOperation.PlayerData:
                    var playerData = payload.Length == 0 ? null : PlayerData.Decode(payload);

                    this.SetPlayerData(playerData);
                    break;
                case ServerOperation.Availability:
                    var availability = Availability.Decode(payload);

                    this.Available = availability.available;
                    break;
                case ServerOperation.Channel:
                    var channel = ServerChannel.Decode(payload);

                    this.CurrentChannel = channel.name;

                    this.app.Dispatch(() => {
                        this.OnPropertyChanged(nameof(this.CurrentChannel));
                    });
                    break;
                case ServerOperation.Backlog:
                    var backlog = ServerBacklog.Decode(payload);

                    var seq = _backlogSequence;
                    foreach (var msg in backlog.messages.ToList().Chunks(100)) {
                        msg.Reverse();
                        var array = msg.ToArray();
                        this.app.Dispatch(DispatcherPriority.Background, () => {
                            this.app.Window.AddReversedChunk(array, seq);
                        });
                    }

                    break;
                case ServerOperation.PlayerList:
                    var playerList = ServerPlayerList.Decode(payload);

                    if (playerList.Type == PlayerListType.Friend) {
                        var players = playerList.Players
                            .OrderBy(player => !player.HasStatus(PlayerStatus.Online));

                        this.app.Dispatch(() => {
                            this.app.Window.FriendList.Clear();
                            foreach (var player in players) {
                                this.app.Window.FriendList.Add(player);
                            }
                        });
                    }

                    break;
                case ServerOperation.LinkshellList:
                    break;
            }
        }

        private static int _backlogSequence = -1;

        private void SetPlayerData(PlayerData? playerData) {
            var visibility = playerData == null ? Visibility.Collapsed : Visibility.Visible;

            this.app.Dispatch(() => {
                var window = this.app.Window;

                window.LoggedInAs.Content = playerData?.name ?? "Not logged in";

                window.LoggedInAsSeparator.Visibility = visibility;

                window.CurrentWorld.Content = playerData?.currentWorld;
                window.CurrentWorld.Visibility = visibility;

                window.CurrentWorldSeparator.Visibility = visibility;

                window.Location.Content = playerData?.location;
                window.Location.Visibility = visibility;
            });
        }

        private void OnPropertyChanged(string prop) {
            Action action;

            if (prop == nameof(this.Available)) {
                action = () => {
                    this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(nameof(this.Available)));
                    this.app.Window.OnPropertyChanged(nameof(MainWindow.InputPlaceholder));
                };
            } else {
                action = () => {
                    this.PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(prop));
                };
            }

            this.app.Dispatch(action);
        }
    }
}
