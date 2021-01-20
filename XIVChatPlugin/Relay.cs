using System;
using System.Linq;
using System.Net;
using System.Threading.Channels;
using System.Threading.Tasks;
using Dalamud.Plugin;
using MessagePack;
using WebSocketSharp;
using XIVChatCommon.Message.Relay;

namespace XIVChatPlugin {
    public class Relay : IDisposable {
        #if DEBUG
        private const string RelayUrl = "ws://localhost:14555/";
        #else
        private const string RelayUrl = "wss://relay.xiv.chat/";
        #endif

        private Plugin Plugin { get; }

        private WebSocket Connection { get; }

        private bool Running { get; set; }

        private Channel<IToRelay> ToRelay { get; } = Channel.CreateUnbounded<IToRelay>();

        internal Relay(Plugin plugin) {
            this.Plugin = plugin;

            this.Connection = new WebSocket(RelayUrl) {
                SslConfiguration = {
                    EnabledSslProtocols = System.Security.Authentication.SslProtocols.Tls12,
                },
            };
            PluginLog.Log($"using {RelayUrl}");
            this.Connection.OnOpen += this.OnOpen;
            this.Connection.OnMessage += this.OnMessage;
            this.Connection.OnClose += this.OnClose;
            this.Connection.OnError += this.OnError;
        }

        public void Dispose() {
            ((IDisposable) this.Connection).Dispose();
        }

        internal void Start() {
            if (this.Plugin.Config.RelayAuth == null) {
                return;
            }

            this.Running = true;

            this.Connection.ConnectAsync();
            PluginLog.Log("ran connect");
        }

        internal void ResendPublicKey() {
            var keys = this.Plugin.Config.KeyPair;
            if (keys == null) {
                return;
            }

            var pk = keys.PublicKey.ToHexString();

            this.Connection.Send(pk);
        }

        private void OnOpen(object sender, EventArgs e) {
            PluginLog.Log("onopen");
            var auth = this.Plugin.Config.RelayAuth;
            if (auth == null) {
                PluginLog.Log("auth null");
                return;
            }

            var keys = this.Plugin.Config.KeyPair;
            if (keys == null) {
                PluginLog.Log("keys null");
                return;
            }

            PluginLog.Log("making registration message");
            var message = new RelayRegister {
                AuthToken = auth,
                PublicKey = keys.PublicKey,
            };
            PluginLog.Log("serialising");
            var bytes = MessagePackSerializer.Serialize((IToRelay) message);

            PluginLog.Log("sending");
            this.Connection.Send(bytes);
            PluginLog.Log("sent registration");

            Task.Run(async () => {
                while (this.Running) {
                    var message = await this.ToRelay.Reader.ReadAsync();
                    var bytes = MessagePackSerializer.Serialize(message);

                    this.Connection.Send(bytes);
                }
            });
        }

        private void OnMessage(object sender, MessageEventArgs e) {
            PluginLog.Log(e.RawData.ToHexString());
            var message = MessagePackSerializer.Deserialize<IFromRelay>(e.RawData);
            switch (message) {
                case RelaySuccess success:
                    if (!success.Success) {
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
            // TODO ?
        }

        private void OnError(object sender, ErrorEventArgs e) {
            PluginLog.LogError(e.Exception, e.Message);
            // TODO ?
        }
    }
}
