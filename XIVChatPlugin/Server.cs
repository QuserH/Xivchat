using Dalamud.Game.Internal;
using Dalamud.Plugin;
using Lumina.Excel.GeneratedSheets;
using MessagePack;
using Sodium;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Channels;
using System.Threading.Tasks;
using Dalamud.Game.Text;
using Dalamud.Game.Text.SeStringHandling;
using Dalamud.Game.Text.SeStringHandling.Payloads;
using XIVChatCommon;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Client;
using XIVChatCommon.Message.Server;

namespace XIVChatPlugin {
    public class Server : IDisposable {
        private const int MaxMessageLength = 500;

        private static readonly string[] PublicPrefixes = {
            "/t ",
            "/tell ",
            "/reply ",
            "/r ",
            "/say ",
            "/s ",
            "/shout ",
            "/sh ",
            "/yell ",
            "/y ",
        };

        private readonly Plugin _plugin;

        private readonly Stopwatch _sendWatch = new();

        private readonly CancellationTokenSource _tokenSource = new();
        private readonly ConcurrentQueue<string> _toGame = new();

        private readonly ConcurrentDictionary<Guid, BaseClient> _clients = new();
        public IReadOnlyDictionary<Guid, BaseClient> Clients => this._clients;
        public readonly Channel<Tuple<BaseClient, Channel<bool>>> PendingClients = Channel.CreateUnbounded<Tuple<BaseClient, Channel<bool>>>();

        private readonly HashSet<Guid> _waitingForFriendList = new();

        private readonly LinkedList<ServerMessage> _backlog = new();

        private TcpListener? _listener;

        private bool _sendPlayerData;
        private readonly ConcurrentQueue<Guid> _awaitingPlayerData = new();
        private readonly ConcurrentQueue<Guid> _awaitingAvailability = new();

        private volatile bool _running;
        private bool Running => this._running;

        private InputChannel _currentChannel = InputChannel.Say;

        private const int MaxMessageSize = 128_000;

        public Server(Plugin plugin) {
            this._plugin = plugin;
            if (this._plugin.Config.KeyPair == null) {
                this.RegenerateKeyPair();
            }

            this._sendWatch.Start();

            this._plugin.Functions.ReceiveFriendList += this.OnReceiveFriendList;
        }

        private void SpawnPairingModeTask() {
            Task.Run(async () => {
                // delay for 10 seconds because of the jank way we cancel below to prevent port bind issues
                await Task.Delay(10_000);

                const int multicastPort = 17444;
                using var udp = new UdpClient();
                udp.Client.SetSocketOption(SocketOptionLevel.Socket, SocketOptionName.ReuseAddress, true);
                udp.Client.Bind(new IPEndPoint(IPAddress.Any, multicastPort));

                var multicastAddr = IPAddress.Parse("224.0.0.147");
                udp.JoinMulticastGroup(multicastAddr);

                string? lastPlayerName = null;

                Task<UdpReceiveResult>? receiveTask = null;

                while (this.Running) {
                    if (!this._plugin.Config.PairingMode) {
                        await Task.Delay(5_000);
                        continue;
                    }

                    var playerName = this._plugin.Interface.ClientState.LocalPlayer?.Name;

                    if (playerName != null) {
                        lastPlayerName = playerName;
                    }

                    if (lastPlayerName == null) {
                        await Task.Delay(5_000);
                        continue;
                    }

                    receiveTask ??= udp.ReceiveAsync();

                    var result = await Task.WhenAny(
                        receiveTask,
                        Task.Delay(1_500)
                    );

                    if (result != receiveTask) {
                        if (!this.Running) {
                            udp.Close();
                        }

                        continue;
                    }

                    var recv = await receiveTask;
                    receiveTask = null;

                    var data = recv.Buffer;
                    if (data.Length != 1 || data[0] != 14) {
                        continue;
                    }

                    var utf8 = Encoding.UTF8.GetBytes(lastPlayerName);
                    var portBytes = BitConverter.GetBytes(this._plugin.Config.Port).Reverse().ToArray();
                    var key = this._plugin.Config.KeyPair!.PublicKey;
                    // magic + string length + string + port + key
                    var payload = new byte[1 + 1 + utf8.Length + portBytes.Length + key.Length]; // assuming names can only be 32 bytes here
                    payload[0] = 14;
                    payload[1] = (byte) utf8.Length;
                    Array.Copy(utf8, 0, payload, 2, utf8.Length);
                    Array.Copy(portBytes, 0, payload, 2 + utf8.Length, portBytes.Length);
                    Array.Copy(key, 0, payload, 2 + utf8.Length + portBytes.Length, key.Length);

                    await udp.SendAsync(payload, payload.Length, recv.RemoteEndPoint);
                }

                PluginLog.Log("Scan response thread done");
            });
        }

        private async void OnReceiveFriendList(List<Player> friends) {
            var msg = new ServerPlayerList(PlayerListType.Friend, friends.ToArray());

            foreach (var id in this._waitingForFriendList) {
                if (!this.Clients.TryGetValue(id, out var client)) {
                    continue;
                }

                await client.Queue.Writer.WriteAsync(msg);
            }

            this._waitingForFriendList.Clear();
        }

        public void Spawn() {
            var port = this._plugin.Config.Port;

            Task.Run(async () => {
                this._listener = new TcpListener(IPAddress.Any, port);
                this._listener.Start();

                this._running = true;
                PluginLog.Log("Running...");
                this.SpawnPairingModeTask();
                while (!this._tokenSource.IsCancellationRequested) {
                    var conn = await this._listener.GetTcpClient(this._tokenSource);
                    if (conn == null) {
                        continue;
                    }

                    var client = new TcpConnected(conn);
                    this.SpawnClientTask(client, true);
                }

                this._running = false;
            });
        }

        public void RegenerateKeyPair() {
            this._plugin.Config.KeyPair = PublicKeyBox.GenerateKeyPair();
            this._plugin.Config.Save();
        }

        [System.Diagnostics.CodeAnalysis.SuppressMessage("Style", "IDE0060:Remove unused parameter", Justification = "delegate")]
        public void OnChat(XivChatType type, uint senderId, ref SeString sender, ref SeString message, ref bool isHandled) {
            if (isHandled) {
                return;
            }

            var chatCode = new ChatCode((ushort) type);

            if (!this._plugin.Config.SendBattle && chatCode.IsBattle()) {
                return;
            }

            var chunks = new List<Chunk>();

            var colour = this._plugin.Functions.GetChannelColour(chatCode) ?? chatCode.DefaultColour();

            if (sender.Payloads.Count > 0) {
                var format = this.FormatFor(chatCode.Type);
                if (format is { IsPresent: true }) {
                    chunks.Add(new TextChunk(format.Before) {
                        FallbackColour = colour,
                    });
                    chunks.AddRange(ToChunks(sender, colour));
                    chunks.Add(new TextChunk(format.After) {
                        FallbackColour = colour,
                    });
                }
            }

            chunks.AddRange(ToChunks(message, colour));

            var msg = new ServerMessage(
                DateTime.UtcNow,
                (ChatType) type,
                sender.Encode(),
                message.Encode(),
                chunks
            );

            this._backlog.AddLast(msg);
            while (this._backlog.Count > this._plugin.Config.BacklogCount) {
                this._backlog.RemoveFirst();
            }

            foreach (var client in this._clients.Values) {
                client.Queue.Writer.TryWrite(msg);
            }
        }

        [System.Diagnostics.CodeAnalysis.SuppressMessage("Style", "IDE0060:Remove unused parameter", Justification = "delegate")]
        public void OnFrameworkUpdate(Framework framework) {
            var player = this._plugin.Interface.ClientState.LocalPlayer;
            if (player != null && this._sendPlayerData) {
                this.BroadcastPlayerData();
                this._sendPlayerData = false;
            }

            while (this._awaitingPlayerData.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client)) {
                    continue;
                }

                var playerData = (Encodable?) this.GeneratePlayerData() ?? EmptyPlayerData.Instance;
                client.Queue.Writer.TryWrite(playerData);
            }

            while (this._awaitingAvailability.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) {
                    continue;
                }

                var available = player != null;
                client.Queue.Writer.TryWrite(new Availability(available));
            }

            int time;
            if (this._toGame.TryPeek(out var peek) && PublicPrefixes.Any(prefix => peek.StartsWith(prefix))) {
                time = 1_000;
            } else if (this._currentChannel is InputChannel.Tell or InputChannel.Say or InputChannel.Shout or InputChannel.Yell) {
                time = 1_000;
            } else {
                time = 250;
            }

            if (this._sendWatch.Elapsed < TimeSpan.FromMilliseconds(time)) {
                return;
            }

            if (!this._toGame.TryDequeue(out var message)) {
                return;
            }

            this._sendWatch.Restart();

            this._plugin.Functions.ProcessChatBox(message);
        }

        private static readonly IReadOnlyList<byte> Magic = new byte[] {
            14, 20, 67,
        };

        internal void SpawnClientTask(BaseClient client, bool requiresMagic) {
            var id = Guid.NewGuid();
            this._clients[id] = client;

            Task.Run(async () => {
                if (requiresMagic) {
                    // get ready for reading magic bytes
                    var magic = new byte[Magic.Count];
                    var read = 0;

                    // only listen for magic for five seconds
                    using var cts = new CancellationTokenSource();
                    cts.CancelAfter(TimeSpan.FromSeconds(5));

                    // read magic bytes
                    while (read < magic.Length) {
                        if (cts.IsCancellationRequested) {
                            return;
                        }

                        read += await client.ReadAsync(magic, read, magic.Length - read, cts.Token);
                    }

                    // ignore this connection if incorrect magic bytes
                    if (!magic.SequenceEqual(Magic)) {
                        return;
                    }
                }

                var handshake = await KeyExchange.ServerHandshake(this._plugin.Config.KeyPair!, client);
                client.Handshake = handshake;

                // if this public key isn't trusted, prompt first
                if (!this._plugin.Config.TrustedKeys.Values.Any(entry => entry.Item2.SequenceEqual(handshake.RemotePublicKey))) {
                    // if configured to not accept new clients, reject connection
                    if (!this._plugin.Config.AcceptNewClients) {
                        return;
                    }

                    var accepted = Channel.CreateBounded<bool>(1);

                    await this.PendingClients.Writer.WriteAsync(Tuple.Create(client, accepted), this._tokenSource.Token);
                    if (!await accepted.Reader.ReadAsync(this._tokenSource.Token)) {
                        return;
                    }
                }

                client.Connected = true;

                // queue sending availability for this client
                this._awaitingAvailability.Enqueue(id);

                // queue sending player data for this client
                this._awaitingPlayerData.Enqueue(id);

                // send current channel
                try {
                    var channel = this._currentChannel;
                    await SecretMessage.SendSecretMessage(
                        client,
                        handshake.Keys.tx,
                        new ServerChannel(
                            channel,
                            this.LocalisedChannelName(channel)
                        ),
                        this._tokenSource.Token
                    );
                } catch (Exception ex) {
                    PluginLog.LogError($"Could not send message: {ex.Message}");
                }

                var listen = Task.Run(async () => {
                    while (this._clients.TryGetValue(id, out var client) && client.Connected && !client.TokenSource.IsCancellationRequested) {
                        byte[] msg;
                        try {
                            msg = await SecretMessage.ReadSecretMessage(client, handshake.Keys.rx, client.TokenSource.Token);
                        } catch (SocketException ex) when (ex.SocketErrorCode == SocketError.TimedOut) {
                            continue;
                        } catch (Exception ex) {
                            PluginLog.LogError($"Could not read message: {ex.Message}");
                            continue;
                        }

                        await this.ProcessMessage(id, client, msg);
                    }
                });

                while (this._clients.TryGetValue(id, out var client) && client.Connected && !client.TokenSource.IsCancellationRequested) {
                    try {
                        var msg = await client.Queue.Reader.ReadAsync(client.TokenSource.Token);
                        await SecretMessage.SendSecretMessage(client, handshake.Keys.tx, msg, client.TokenSource.Token);
                    } catch (Exception ex) {
                        PluginLog.LogError($"Could not send message: {ex.Message}");
                    }
                }

                client.Disconnect();

                await listen;

                this._clients.TryRemove(id, out _);
                PluginLog.Log($"Client thread ended: {id}");
            }).ContinueWith(_ => {
                this.RemoveClient(id);
            });
        }

        internal void RemoveClient(Guid id) {
            if (!this._clients.TryRemove(id, out var client)) {
                return;
            }

            client.Disconnect();
        }

        private async Task ProcessMessage(Guid id, BaseClient client, byte[] msg) {
            var op = (ClientOperation) msg[0];

            var payload = new byte[msg.Length - 1];
            Array.Copy(msg, 1, payload, 0, payload.Length);

            switch (op) {
                case ClientOperation.Ping:
                    try {
                        await client.Queue.Writer.WriteAsync(Pong.Instance);
                    } catch (Exception ex) {
                        PluginLog.LogError($"Could not send message: {ex.Message}");
                    }

                    break;
                case ClientOperation.Message:
                    var clientMessage = ClientMessage.Decode(payload);
                    var sanitised = clientMessage.Content
                        .Replace("\r\n", " ")
                        .Replace('\r', ' ')
                        .Replace('\n', ' ');
                    foreach (var part in Wrap(sanitised)) {
                        this._toGame.Enqueue(part);
                    }

                    break;
                case ClientOperation.Shutdown:
                    client.Disconnect();
                    break;
                case ClientOperation.Backlog:
                    // ReSharper disable once LocalVariableHidesMember
                    var backlog = ClientBacklog.Decode(payload);

                    var backlogMessages = new List<ServerMessage>();

                    var node = this._backlog.Last;
                    while (node != null) {
                        if (backlogMessages.Count >= backlog.Amount) {
                            break;
                        }

                        backlogMessages.Add(node.Value);
                        node = node.Previous;
                    }

                    if (!client.GetPreference(ClientPreference.BacklogNewestMessagesFirst, false)) {
                        backlogMessages.Reverse();
                    }

                    await SendBacklogs(backlogMessages.ToArray(), client);
                    break;
                case ClientOperation.CatchUp:
                    var catchUp = ClientCatchUp.Decode(payload);
                    // I'm not sure why this needs to be done, but apparently it does
                    var after = catchUp.After.AddMilliseconds(1);
                    var msgs = this.MessagesAfter(after);

                    if (client.GetPreference(ClientPreference.BacklogNewestMessagesFirst, false)) {
                        msgs = msgs.Reverse();
                    }

                    await SendBacklogs(msgs, client);
                    break;
                case ClientOperation.PlayerList:
                    var playerList = ClientPlayerList.Decode(payload);

                    if (playerList.Type == PlayerListType.Friend) {
                        this._waitingForFriendList.Add(id);

                        if (!this._plugin.Functions.RequestingFriendList && !this._plugin.Functions.RequestFriendList()) {
                            this._plugin.Interface.Framework.Gui.Chat.PrintError($"[{this._plugin.Name}] Please open your friend list to enable friend list support. You should only need to do this on initial install or after updates.");
                        }
                    }

                    break;
                case ClientOperation.Preferences:
                    var preferences = ClientPreferences.Decode(payload);
                    client.Preferences = preferences;

                    break;
                case ClientOperation.Channel:
                    var channel = ClientChannel.Decode(payload);
                    this._plugin.Functions.ChangeChatChannel(channel.Channel);

                    break;
            }
        }

        public class NameFormatting {
            public string Before { get; private set; } = string.Empty;
            public string After { get; private set; } = string.Empty;
            public bool IsPresent { get; private set; } = true;

            public static NameFormatting Empty() {
                return new() {
                    IsPresent = false,
                };
            }

            public static NameFormatting Of(string before, string after) {
                return new() {
                    Before = before,
                    After = after,
                };
            }
        }

        private Dictionary<ChatType, NameFormatting> Formats { get; } = new();

        private NameFormatting? FormatFor(ChatType type) {
            if (this.Formats.TryGetValue(type, out var cached)) {
                return cached;
            }

            var logKind = this._plugin.Interface.Data.GetExcelSheet<LogKind>().GetRow((ushort) type);

            if (logKind == null) {
                return null;
            }

            var format = logKind.Format;
            var sestring = this._plugin.Interface.SeStringManager.Parse(format.RawData.ToArray());

            static bool IsStringParam(Payload payload, byte num) {
                var data = payload.Encode();

                return data.Length >= 5 && data[1] == 0x29 && data[4] == num + 1;
            }

            var firstStringParam = sestring.Payloads.FindIndex(payload => IsStringParam(payload, 1));
            var secondStringParam = sestring.Payloads.FindIndex(payload => IsStringParam(payload, 2));

            if (firstStringParam == -1 || secondStringParam == -1) {
                return NameFormatting.Empty();
            }

            var before = sestring.Payloads
                .GetRange(0, firstStringParam)
                .Where(payload => payload is ITextProvider)
                .Cast<ITextProvider>()
                .Select(text => text.Text);
            var after = sestring.Payloads
                .GetRange(firstStringParam + 1, secondStringParam - firstStringParam)
                .Where(payload => payload is ITextProvider)
                .Cast<ITextProvider>()
                .Select(text => text.Text);

            var nameFormatting = NameFormatting.Of(
                string.Join("", before),
                string.Join("", after)
            );

            this.Formats[type] = nameFormatting;

            return nameFormatting;
        }

        private static async Task SendBacklogs(IEnumerable<ServerMessage> messages, BaseClient client) {
            const int defaultSize = 5 + SecretMessage.NonceSize + SecretMessage.MacSize;
            var size = defaultSize;
            var responseMessages = new List<ServerMessage>();

            async Task SendBacklog() {
                var resp = new ServerBacklog(responseMessages.ToArray());
                try {
                    await client.Queue.Writer.WriteAsync(resp);
                } catch (Exception ex) {
                    PluginLog.LogError($"Could not send backlog: {ex.Message}");
                }
            }

            foreach (var catchUpMessage in messages) {
                // FIXME: this is very gross
                var len = MessagePackSerializer.Serialize(catchUpMessage).Length;
                // send message if it would've gone over length
                if (size + len >= MaxMessageSize) {
                    await SendBacklog();

                    size = defaultSize;
                    responseMessages.Clear();
                }

                size += len;
                responseMessages.Add(catchUpMessage);
            }

            if (responseMessages.Count > 0) {
                await SendBacklog();
            }
        }

        private static IEnumerable<Chunk> ToChunks(SeString msg, uint? defaultColour) {
            var chunks = new List<Chunk>();

            var italic = false;
            var foreground = new Stack<uint>();
            var glow = new Stack<uint>();

            void Append(string text) {
                chunks.Add(new TextChunk(text) {
                    FallbackColour = defaultColour,
                    Foreground = foreground.Count > 0 ? foreground.Peek() : null,
                    Glow = glow.Count > 0 ? glow.Peek() : null,
                    Italic = italic,
                });
            }

            foreach (var payload in msg.Payloads) {
                switch (payload.Type) {
                    case PayloadType.EmphasisItalic:
                        var newStatus = ((EmphasisItalicPayload) payload).IsEnabled;
                        italic = newStatus;
                        break;
                    case PayloadType.UIForeground:
                        var foregroundPayload = (UIForegroundPayload) payload;
                        if (foregroundPayload.IsEnabled) {
                            foreground.Push(foregroundPayload.UIColor.UIForeground);
                        } else if (foreground.Count > 0) {
                            foreground.Pop();
                        }

                        break;
                    case PayloadType.UIGlow:
                        var glowPayload = (UIGlowPayload) payload;
                        if (glowPayload.IsEnabled) {
                            glow.Push(glowPayload.UIColor.UIGlow);
                        } else if (glow.Count > 0) {
                            glow.Pop();
                        }

                        break;
                    case PayloadType.AutoTranslateText:
                        chunks.Add(new IconChunk {
                            index = 54,
                        });
                        var autoText = ((AutoTranslatePayload) payload).Text;
                        Append(autoText.Substring(2, autoText.Length - 4));
                        chunks.Add(new IconChunk {
                            index = 55,
                        });
                        break;
                    case PayloadType.Icon:
                        var index = ((IconPayload) payload).Icon;
                        chunks.Add(new IconChunk {
                            index = (byte) index,
                        });
                        break;
                    case PayloadType.Unknown:
                        var rawPayload = (RawPayload) payload;
                        if (rawPayload.Data[1] == 0x13) {
                            foreground.Pop();
                            glow.Pop();
                        }

                        break;
                    default:
                        if (payload is ITextProvider textProvider) {
                            Append(textProvider.Text);
                        }

                        break;
                }
            }

            return chunks;
        }

        private IEnumerable<ServerMessage> MessagesAfter(DateTime time) => this._backlog.Where(msg => msg.Timestamp > time).ToArray();

        private static IEnumerable<string> Wrap(string input) {
            if (input.Length <= MaxMessageLength) {
                return new[] {
                    input,
                };
            }

            string prefix = string.Empty;
            if (input.StartsWith("/")) {
                var space = input.IndexOf(' ');
                if (space != -1) {
                    prefix = input.Substring(0, space);
                    // handle wrapping tells
                    if (prefix is "/tell" or "/t") {
                        var tellSpace = input.IndexOfCount(' ', 3);
                        if (tellSpace != -1) {
                            prefix = input.Substring(0, tellSpace);
                            input = input.Substring(tellSpace + 1);
                        }
                    } else {
                        input = input.Substring(space + 1);
                    }
                }
            }

            return NativeTools.Wrap(input, MaxMessageLength)
                .Select(text => $"{prefix} {text}")
                .ToArray();
        }

        private void BroadcastMessage(Encodable message) {
            foreach (var client in this.Clients.Values) {
                client.Queue.Writer.TryWrite(message);
            }
        }

        private string LocalisedChannelName(InputChannel channel) {
            uint rowId = channel switch {
                InputChannel.Tell => 3,
                InputChannel.Say => 1,
                InputChannel.Party => 4,
                InputChannel.Alliance => 17,
                InputChannel.Yell => 16,
                InputChannel.Shout => 2,
                InputChannel.FreeCompany => 7,
                InputChannel.PvpTeam => 19,
                InputChannel.NoviceNetwork => 18,
                InputChannel.CrossLinkshell1 => 20,
                InputChannel.CrossLinkshell2 => 300,
                InputChannel.CrossLinkshell3 => 301,
                InputChannel.CrossLinkshell4 => 302,
                InputChannel.CrossLinkshell5 => 303,
                InputChannel.CrossLinkshell6 => 304,
                InputChannel.CrossLinkshell7 => 305,
                InputChannel.CrossLinkshell8 => 306,
                InputChannel.Linkshell1 => 8,
                InputChannel.Linkshell2 => 9,
                InputChannel.Linkshell3 => 10,
                InputChannel.Linkshell4 => 11,
                InputChannel.Linkshell5 => 12,
                InputChannel.Linkshell6 => 13,
                InputChannel.Linkshell7 => 14,
                InputChannel.Linkshell8 => 15,
                _ => 0,
            };

            return this._plugin.Interface.Data.GetExcelSheet<LogFilter>().GetRow(rowId).Name;
        }

        public void OnChatChannelChange(uint channel) {
            var inputChannel = (InputChannel) channel;
            this._currentChannel = inputChannel;

            var localisedName = this.LocalisedChannelName(inputChannel);

            var msg = new ServerChannel(inputChannel, localisedName);
            this.BroadcastMessage(msg);
        }

        private void BroadcastAvailability(bool available) {
            this.BroadcastMessage(new Availability(available));
        }

        private PlayerData? GeneratePlayerData() {
            var player = this._plugin.Interface.ClientState.LocalPlayer;
            if (player == null) {
                return null;
            }

            var homeWorld = player.HomeWorld.GameData.Name;
            var currentWorld = player.CurrentWorld.GameData.Name;
            var territory = this._plugin.Interface.Data.GetExcelSheet<TerritoryType>().GetRow(this._plugin.Interface.ClientState.TerritoryType);
            var location = territory?.PlaceName?.Value?.Name ?? "???";
            var name = player.Name;

            return new PlayerData(homeWorld, currentWorld, location, name);
        }

        private void BroadcastPlayerData() {
            var playerData = (Encodable?) this.GeneratePlayerData() ?? EmptyPlayerData.Instance;

            this.BroadcastMessage(playerData);
        }

        public void OnLogIn(object sender, EventArgs e) {
            this.BroadcastAvailability(true);
            // send player data on next framework update
            this._sendPlayerData = true;
        }

        public void OnLogOut(object sender, EventArgs e) {
            this.BroadcastAvailability(false);
            this.BroadcastPlayerData();
        }

        public void OnTerritoryChange(object sender, ushort territoryId) => this._sendPlayerData = true;

        public void Dispose() {
            // stop accepting new clients
            this._tokenSource.Cancel();
            foreach (var client in this._clients.Values) {
                Task.Run(async () => {
                    // tell clients we're shutting down
                    if (client.Handshake != null) {
                        try {
                            await SecretMessage.SendSecretMessage(client, client.Handshake.Keys.tx, ServerShutdown.Instance);
                        } catch (Exception) {
                            // ignored
                        }
                    }

                    // cancel threads for open clients
                    client.TokenSource.Cancel();
                });
            }

            this._plugin.Functions.ReceiveFriendList -= this.OnReceiveFriendList;
        }
    }

    internal static class TcpListenerExt {
        public static async Task<TcpClient?> GetTcpClient(this TcpListener listener, CancellationTokenSource source) {
            using (source.Token.Register(listener.Stop)) {
                try {
                    var client = await listener.AcceptTcpClientAsync().ConfigureAwait(false);
                    return client;
                } catch (ObjectDisposedException) {
                    // Token was canceled - swallow the exception and return null
                    if (source.Token.IsCancellationRequested) {
                        return null;
                    }

                    throw;
                }
            }
        }
    }
}
