using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Threading.Channels;
using System.Threading.Tasks;
using Dalamud.Plugin;
using MessagePack;
using WebSocketSharp;
using XIVChatCommon.Message.Relay;

namespace XIVChatPlugin {
    public enum ConnectionStatus {
        Disconnected,
        Connecting,
        Negotiating,
        Connected,
    }

    public class Relay : IDisposable {
        #if DEBUG
        private const string RelayUrl = "ws://localhost:14555/";
        #else
        private const string RelayUrl = "wss://relay.xiv.chat/";
        #endif

        private bool Disposed { get; set; }

        private Plugin Plugin { get; }

        private WebSocket Connection { get; }

        private bool Running { get; set; }

        public ConnectionStatus Status { get; private set; }

        private Channel<IToRelay> ToRelay { get; } = Channel.CreateUnbounded<IToRelay>();

        internal Relay(Plugin plugin) {
            this.Plugin = plugin;

            this.Connection = new WebSocket(RelayUrl) {
                SslConfiguration = {
                    EnabledSslProtocols = System.Security.Authentication.SslProtocols.Tls12,
                },
            };

            this.Connection.OnOpen += this.OnOpen;
            this.Connection.OnMessage += this.OnMessage;
            this.Connection.OnClose += this.OnClose;
            this.Connection.OnError += this.OnError;
        }

        public void Dispose() {
            this.Disposed = true;
            this.Connection.CloseAsync();
            this.Running = false;
        }

        internal void Start() {
            if (this.Plugin.Config.RelayAuth == null) {
                return;
            }

            this.Running = true;

            this.Status = ConnectionStatus.Connecting;
            this.Connection.ConnectAsync();
        }

        internal void ResendPublicKey() {
            var keys = this.Plugin.Config.KeyPair;
            if (keys == null) {
                return;
            }

            var pk = keys.PublicKey.ToHexString();

            this.Connection.Send(pk);
        }

        internal void DisconnectClient(IEnumerable<byte> pk) {
            var msg = new RelayClientDisconnect {
                PublicKey = pk.ToList(),
            };
            var bytes = MessagePackSerializer.Serialize((IToRelay) msg);

            this.Connection.Send(bytes);
        }

        private void OnOpen(object sender, EventArgs e) {
            this.Status = ConnectionStatus.Negotiating;

            var auth = this.Plugin.Config.RelayAuth;
            if (auth == null) {
                return;
            }

            var keys = this.Plugin.Config.KeyPair;
            if (keys == null) {
                return;
            }

            var message = new RelayRegister {
                AuthToken = auth,
                PublicKey = keys.PublicKey,
            };
            var bytes = MessagePackSerializer.Serialize((IToRelay) message);

            this.Connection.Send(bytes);

            Task.Run(async () => {
                while (this.Running) {
                    this.Connection.Ping();
                    await Task.Delay(TimeSpan.FromSeconds(30));
                }
            });

            Task.Run(async () => {
                while (this.Running) {
                    var message = await this.ToRelay.Reader.ReadAsync();
                    var bytes = MessagePackSerializer.Serialize(message);

                    this.Connection.Send(bytes);
                }
            });
        }

        private void OnMessage(object sender, MessageEventArgs e) {
            var message = MessagePackSerializer.Deserialize<IFromRelay>(e.RawData);
            switch (message) {
                case RelaySuccess success:
                    if (success.Success) {
                        this.Status = ConnectionStatus.Connected;
                    } else {
                        PluginLog.LogWarning($"Relay: {success.Info}");
                        this.Status = ConnectionStatus.Disconnected;
                        this.Plugin.StopRelay();
                    }

                    break;
                case RelayNewClient newClient:
                    IPAddress.TryParse(newClient.Address, out var remote);
                    var client = new RelayConnected(
                        newClient.PublicKey.ToArray(),
                        remote,
                        this.ToRelay.Writer,
                        Channel.CreateUnbounded<byte[]>()
                    );

                    this.Plugin.Server.SpawnClientTask(client, false);
                    break;
                case RelayClientDisconnect disconnect:
                    var clientPk = disconnect.PublicKey.ToArray();
                    var id = this.Plugin.Server.Clients
                        .Where(client => client.Value is RelayConnected)
                        .Where(client => client.Value.Handshake?.RemotePublicKey?.SequenceEqual(clientPk) ?? false)
                        .Select(client => client.Key)
                        .FirstOrDefault();
                    if (id != default) {
                        this.Plugin.Server.RemoveClient(id);
                    }

                    break;
                case RelayedMessage relayed:
                    var relayedClient = this.Plugin.Server.Clients.Values
                        .Where(client => client is RelayConnected)
                        .Cast<RelayConnected>()
                        .FirstOrDefault(client => client.PublicKey.SequenceEqual(relayed.PublicKey));

                    relayedClient?.FromRelayWriter.WriteAsync(relayed.Message.ToArray()).AsTask().Wait();
                    break;
            }
        }

        private void OnClose(object sender, CloseEventArgs e) {
            this.Running = false;
            this.Status = ConnectionStatus.Disconnected;

            if (!e.WasClean && !this.Disposed) {
                Task.Run(async () => await Task.Delay(3_000)).ContinueWith(_ => this.Start());
            }
        }

        private void OnError(object sender, ErrorEventArgs e) {
            PluginLog.LogError(e.Exception, $"Error in relay connection: {e.Message}");
            this.Running = false;
            this.Status = ConnectionStatus.Disconnected;

            if (!this.Disposed) {
                Task.Run(async () => await Task.Delay(3_000)).ContinueWith(_ => this.Start());
            }
        }
    }
}
