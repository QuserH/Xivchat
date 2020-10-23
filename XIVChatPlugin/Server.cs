using Dalamud.Game.Chat;
using Dalamud.Game.Chat.SeStringHandling;
using Dalamud.Game.Chat.SeStringHandling.Payloads;
using Dalamud.Game.Internal;
using Dalamud.Plugin;
using Lumina.Data;
using Lumina.Excel;
using Lumina.Excel.GeneratedSheets;
using MessagePack;
using Sodium;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Threading.Channels;
using System.Threading.Tasks;
using XIVChatCommon;

namespace XIVChatPlugin {
    public class Server : IDisposable {
        private readonly Plugin plugin;

        private readonly CancellationTokenSource tokenSource = new CancellationTokenSource();
        private readonly ConcurrentQueue<string> toGame = new ConcurrentQueue<string>();

        private readonly ConcurrentDictionary<Guid, Client> clients = new ConcurrentDictionary<Guid, Client>();
        public IReadOnlyDictionary<Guid, Client> Clients => this.clients;
        public readonly Channel<Tuple<Client, Channel<bool>>> pendingClients = Channel.CreateUnbounded<Tuple<Client, Channel<bool>>>();

        private readonly HashSet<Guid> WaitingForFriendList = new HashSet<Guid>();

        private readonly LinkedList<ServerMessage> Backlog = new LinkedList<ServerMessage>();

        private TcpListener listener;

        private bool sendPlayerData = false;

        private volatile bool _running = false;
        public bool Running { get => this._running; set => this._running = value; }

        private InputChannel currentChannel = InputChannel.Say;

        private const int MAX_MESSAGE_SIZE = 128_000;

        public Server(Plugin plugin) {
            this.plugin = plugin ?? throw new ArgumentNullException(nameof(plugin), "Plugin cannot be null");
            if (this.plugin.Config.KeyPair == null) {
                this.RegenerateKeyPair();
            }

            this.plugin.Functions.ReceiveFriendList += this.OnReceiveFriendList;
        }

        private async void OnReceiveFriendList(List<Player> friends) {
            var msg = new ServerPlayerList {
                Type = PlayerListType.Friend,
                Players = friends.ToArray(),
            };

            foreach (var id in this.WaitingForFriendList) {
                if (!this.Clients.TryGetValue(id, out var client)) {
                    continue;
                }

                try {
                    await SecretMessage.SendSecretMessage(client.Conn.GetStream(), client.Handshake.Keys.tx, msg, client.TokenSource.Token);
                } catch (Exception ex) {
                    PluginLog.LogError($"Could not send message: {ex.Message}");
                }
            }

            this.WaitingForFriendList.Clear();
        }

        public void Spawn() {
            var ip = IPAddress.Parse("0.0.0.0");
            var port = this.plugin.Config.Port;

            Task.Run(async () => {
                this.listener = new TcpListener(ip, port);
                this.listener.Start();

                this._running = true;
                PluginLog.Log("Running...");
                while (!this.tokenSource.IsCancellationRequested) {
                    var conn = await this.listener.GetTcpClient(this.tokenSource);
                    this.SpawnClientTask(conn);
                }
                this._running = false;
            });
        }

        public void RegenerateKeyPair() {
            this.plugin.Config.KeyPair = PublicKeyBox.GenerateKeyPair();
            this.plugin.Config.Save();
        }

        [System.Diagnostics.CodeAnalysis.SuppressMessage("Style", "IDE0060:Remove unused parameter", Justification = "delegate")]
        public void OnChat(XivChatType type, uint senderId, ref SeString sender, ref SeString message, ref bool isHandled) {
            if (isHandled) {
                return;
            }

            var chatCode = new ChatCode((ushort)type);

            if (!this.plugin.Config.SendBattle && chatCode.IsBattle()) {
                return;
            }

            var chunks = new List<Chunk>();

            if (sender.Payloads.Count > 0) {
                // FIXME: can't get format straight from game until Lumina stops returning LogKind.Format as a string (it's an SeString)
                // var format = this.FormatFor(chatCode.Type);
                var format = chatCode.NameFormat();
                if (format != null && format.IsPresent) {
                    chunks.Add(new TextChunk {
                        FallbackColour = chatCode.DefaultColour(),
                        Content = format.Before,
                    });
                    chunks.AddRange(ToChunks(sender, chatCode.DefaultColour()));
                    chunks.Add(new TextChunk {
                        FallbackColour = chatCode.DefaultColour(),
                        Content = format.After,
                    });
                }
            }

            chunks.AddRange(ToChunks(message, chatCode.DefaultColour()));

            var msg = new ServerMessage {
                Timestamp = DateTime.UtcNow,
                Channel = (ChatType)type,
                Sender = sender.Encode(),
                Content = message.Encode(),
                Chunks = chunks,
            };

            this.Backlog.AddLast(msg);
            while (this.Backlog.Count > this.plugin.Config.BacklogCount) {
                this.Backlog.RemoveFirst();
            }

            foreach (var client in this.clients.Values) {
                client.Queue.Writer.TryWrite(msg);
            }
        }

        [System.Diagnostics.CodeAnalysis.SuppressMessage("Style", "IDE0060:Remove unused parameter", Justification = "delegate")]
        public void OnFrameworkUpdate(Framework framework) {
            if (this.sendPlayerData && this.plugin.Interface.ClientState.LocalPlayer != null) {
                this.BroadcastPlayerData();
                this.sendPlayerData = false;
            }

            if (!this.toGame.TryDequeue(out var message)) {
                return;
            }

            this.plugin.Functions.ProcessChatBox(message);
        }

        private void SpawnClientTask(TcpClient conn) {
            if (conn == null) {
                return;
            }

            Task.Run(async () => {
                var stream = conn.GetStream();

                var handshake = await KeyExchange.ServerHandshake(this.plugin.Config.KeyPair, stream);
                var newClient = new Client(conn) {
                    Handshake = handshake,
                };

                // if this public key isn't trusted, prompt first
                if (!this.plugin.Config.TrustedKeys.Values.Any(entry => entry.Item2.SequenceEqual(handshake.RemotePublicKey))) {
                    var accepted = Channel.CreateBounded<bool>(1);

                    await this.pendingClients.Writer.WriteAsync(Tuple.Create(newClient, accepted));
                    if (!await accepted.Reader.ReadAsync()) {
                        return;
                    }
                }

                var id = Guid.NewGuid();
                newClient.Connected = true;
                this.clients[id] = newClient;

                // send availability
                var available = this.plugin.Interface.ClientState.LocalPlayer != null;
                try {
                    await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, new Availability(available));
                } catch (Exception ex) {
                    PluginLog.LogError($"Could not send message: {ex.Message}");
                }

                // send player data
                try {
                    await this.SendPlayerData(newClient);
                } catch (Exception ex) {
                    PluginLog.LogError($"Could not send message: {ex.Message}");
                }

                // send current channel
                try {
                    var channel = this.currentChannel;
                    await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, new ServerChannel(
                        channel,
                        this.LocalisedChannelName(channel)
                    ));
                } catch (Exception ex) {
                    PluginLog.LogError($"Could not send message: {ex.Message}");
                }

                var listen = Task.Run(async () => {
                    conn.ReceiveTimeout = 5_000;

                    while (this.clients.TryGetValue(id, out var client) && client.Connected && !client.TokenSource.IsCancellationRequested && conn.Connected) {
                        byte[] msg;
                        try {
                            msg = await SecretMessage.ReadSecretMessage(stream, handshake.Keys.rx, client.TokenSource.Token);
                        } catch (SocketException ex) when (ex.SocketErrorCode == SocketError.TimedOut) {
                            continue;
                        } catch (Exception ex) {
                            PluginLog.LogError($"Could not read message: {ex.Message}");
                            continue;
                        }

                        var op = (ClientOperation)msg[0];

                        var payload = new byte[msg.Length - 1];
                        Array.Copy(msg, 1, payload, 0, payload.Length);

                        switch (op) {
                            case ClientOperation.Ping:
                                try {
                                    await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, Pong.Instance, client.TokenSource.Token);
                                } catch (Exception ex) {
                                    PluginLog.LogError($"Could not send message: {ex.Message}");
                                }
                                break;
                            case ClientOperation.Message:
                                var clientMessage = ClientMessage.Decode(payload);
                                foreach (var part in Wrap(clientMessage.Content)) {
                                    this.toGame.Enqueue(part);
                                }
                                break;
                            case ClientOperation.Shutdown:
                                client.Disconnect();
                                break;
                            case ClientOperation.Backlog:
                                var backlog = ClientBacklog.Decode(payload);

                                var backlogMessages = new List<ServerMessage>();

                                var node = this.Backlog.Last;
                                while (node != null) {
                                    if (backlogMessages.Count >= backlog.Amount) {
                                        break;
                                    }

                                    backlogMessages.Add(node.Value);
                                    node = node.Previous;
                                }

                                backlogMessages.Reverse();

                                await this.SendBacklogs(backlogMessages.ToArray(), stream, client);
                                break;
                            case ClientOperation.CatchUp:
                                var catchUp = ClientCatchUp.Decode(payload);
                                // I'm not sure why this needs to be done, but apparently it does
                                var after = catchUp.After.AddMilliseconds(1);
                                var msgs = this.MessagesAfter(after);

                                await this.SendBacklogs(msgs, stream, client);
                                break;
                            case ClientOperation.PlayerList:
                                var playerList = ClientPlayerList.Decode(payload);

                                if (playerList.Type == PlayerListType.Friend) {
                                    this.WaitingForFriendList.Add(id);

                                    if (!this.plugin.Functions.RequestingFriendList && !this.plugin.Functions.RequestFriendList()) {
                                        this.plugin.Interface.Framework.Gui.Chat.PrintError($"[{this.plugin.Name}] Please open your friend list to enable friend list support. You should only need to do this on initial install or after updates.");
                                    }
                                }

                                break;
                        }
                    }
                });

                while (this.clients.TryGetValue(id, out var client) && client.Connected && !client.TokenSource.IsCancellationRequested && conn.Connected) {
                    try {
                        var msg = await client.Queue.Reader.ReadAsync(client.TokenSource.Token);
                        await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, msg, client.TokenSource.Token);
                    } catch (Exception ex) {
                        PluginLog.LogError($"Could not send message: {ex.Message}");
                    }
                }

                try {
                    conn.Close();
                } catch (ObjectDisposedException) { }

                await listen;

                this.clients.TryRemove(id, out var _);
                PluginLog.Log($"Client thread ended: {id}");
            });
        }

        private static readonly Regex colorRegex = new Regex(@"<Color\(.+?\)>", RegexOptions.Compiled);
        private Dictionary<ChatType, NameFormatting> Formats { get; } = new Dictionary<ChatType, NameFormatting>();

        [Sheet("LogKind")]
        class LogKind : IExcelRow {
            public byte Unknown0;
            public byte[] Format;
            public bool Unknown2;

            public uint RowId { get; set; }
            public uint SubRowId { get; set; }

            public void PopulateData(RowParser parser, Lumina.Lumina lumina, Language language) {
                this.RowId = parser.Row;
                this.SubRowId = parser.SubRow;

                this.Unknown0 = parser.ReadColumn<byte>(0);
                this.Format = parser.ReadColumn<byte[]>(1);
                this.Unknown2 = parser.ReadColumn<bool>(2);
            }
        }

        private NameFormatting FormatFor(ChatType type) {
            if (this.Formats.TryGetValue(type, out var cached)) {
                return cached;
            }

            var logKind = this.plugin.Interface.Data.GetExcelSheet<LogKind>().GetRow((ushort)type);

            if (logKind == null) {
                return null;
            }

            var sestring = this.plugin.Interface.SeStringManager.Parse(logKind.Format);

            //PluginLog.Log(string.Join("", Encoding.ASCII.GetBytes(logKind.Format).Select(b => b.ToString("x2"))));

            //var sestring = this.plugin.Interface.SeStringManager.Parse(Encoding.ASCII.GetBytes(logKind.Format));

            //var format = colorRegex.Replace(logKind.Format, "")
            //    .Replace("</Color>", "")
            //    .Replace("<Highlight>", "")
            //    .Replace("</Highlight>", "");

            return NameFormatting.Empty();

            //if (format.IndexOf("StringParameter(1)") == -1) {
            //    return NameFormatting.Empty();
            //}

            //var parts = format.Split(new string[] { "StringParameter(1)" }, StringSplitOptions.None);

            //var nameFormatting = NameFormatting.Of(
            //    before: parts[0],
            //    after: parts[1].Split(new string[] { "StringParameter(2)" }, StringSplitOptions.None)[0]
            //);

            //this.Formats[type] = nameFormatting;

            //return nameFormatting;
        }

        private async Task SendBacklogs(ServerMessage[] messages, NetworkStream stream, Client client) {
            var size = 5 + SecretMessage.MacSize(); // assume 5 bytes for payload lead-in, although it's likely to be less
            var responseMessages = new List<ServerMessage>();

            async Task SendBacklog() {
                var resp = new ServerBacklog(responseMessages.ToArray());
                try {
                    await SecretMessage.SendSecretMessage(stream, client.Handshake.Keys.tx, resp, client.TokenSource.Token);
                } catch (Exception ex) {
                    PluginLog.LogError($"Could not send backlog: {ex.Message}");
                }
            }

            foreach (var catchUpMessage in messages) {
                // FIXME: this is very gross
                var len = MessagePackSerializer.Serialize(catchUpMessage).Length;
                // send message if it would've gone over length
                if (size + len >= MAX_MESSAGE_SIZE) {
                    await SendBacklog();

                    size = 5 + SecretMessage.MacSize();
                    responseMessages.Clear();

                }
                size += len;
                responseMessages.Add(catchUpMessage);
            }

            if (responseMessages.Count > 0) {
                await SendBacklog();
            }
        }

        private static List<Chunk> ToChunks(SeString msg, uint? defaultColour) {
            var chunks = new List<Chunk>();

            var italic = false;
            var foreground = new Stack<uint>();
            var glow = new Stack<uint>();

            void Append(string text) {
                chunks.Add(new TextChunk {
                    FallbackColour = defaultColour,
                    Foreground = foreground.Count > 0 ? foreground.Peek() : (uint?)null,
                    Glow = glow.Count > 0 ? glow.Peek() : (uint?)null,
                    Italic = italic,
                    Content = text,
                });
            }

            foreach (var payload in msg.Payloads) {
                switch (payload.Type) {
                    case PayloadType.EmphasisItalic:
                        var newStatus = ((EmphasisItalicPayload)payload).IsEnabled;
                        italic = newStatus;
                        break;
                    case PayloadType.UIForeground:
                        var foregroundPayload = (UIForegroundPayload)payload;
                        if (foregroundPayload.IsEnabled) {
                            foreground.Push(foregroundPayload.UIColor.UIForeground);
                        } else if (foreground.Count > 0) {
                            foreground.Pop();
                        }
                        break;
                    case PayloadType.UIGlow:
                        var glowPayload = (UIGlowPayload)payload;
                        if (glowPayload.IsEnabled) {
                            glow.Push(glowPayload.UIColor.UIGlow);
                        } else if (glow.Count > 0) {
                            glow.Pop();
                        }
                        break;
                    case PayloadType.AutoTranslateText:
                        chunks.Add(new IconChunk { Index = 54 });
                        var autoText = ((AutoTranslatePayload)payload).Text;
                        Append(autoText.Substring(2, autoText.Length - 4));
                        chunks.Add(new IconChunk { Index = 55 });
                        break;
                    case PayloadType.Icon:
                        var index = ((IconPayload)payload).IconIndex;
                        chunks.Add(new IconChunk { Index = (byte)index });
                        break;
                    // FIXME: use ITextProvider directly once it's exposed
                    case PayloadType.RawText:
                        Append(((TextPayload)payload).Text);
                        break;
                    case PayloadType.Unknown:
                        var rawPayload = (RawPayload)payload;
                        if (rawPayload.Data[1] == 0x13) {
                            foreground.Pop();
                            glow.Pop();
                        }
                        break;
                        //default:
                        //    var textProviderType = typeof(SeString).Assembly.GetType("Dalamud.Game.Chat.SeStringHandling.ITextProvider");
                        //    var textProp = textProviderType.GetProperty("Text", BindingFlags.NonPublic | BindingFlags.Instance);
                        //    var text = (string)textProp.GetValue(payload);
                        //    append(text);
                        //    break;
                }
            }

            return chunks;
        }

        private ServerMessage[] MessagesAfter(DateTime time) => this.Backlog.Where(msg => msg.Timestamp > time).ToArray();

        private static string[] Wrap(string input) {
            const int LIMIT = 500;

            if (input.Length <= LIMIT) {
                return new string[] { input };
            }

            string prefix = string.Empty;
            if (input.StartsWith("/")) {
                var space = input.IndexOf(' ');
                if (space != -1) {
                    prefix = input.Substring(0, space);
                    input = input.Substring(space + 1);
                }
            }

            var parts = new List<string>();

            var builder = new StringBuilder(LIMIT);

            foreach (var word in input.Split(' ')) {
                if (word.Length > LIMIT) {
                    int wordParts = (int)Math.Ceiling((float)word.Length / LIMIT);
                    for (int i = 0; i < wordParts; i++) {
                        var start = i == 0 ? 0 : (i * LIMIT);
                        var partLength = LIMIT;
                        if (prefix.Length != 0) {
                            start = start == 0 ? 0 : (start - (prefix.Length + 1) * i);
                            partLength = partLength - prefix.Length - 1;
                        }

                        var part = word.Length - start < partLength ? word.Substring(start) : word.Substring(start, partLength);
                        if (part.Length == 0) {
                            continue;
                        }

                        if (prefix.Length != 0) {
                            part = prefix + " " + part;
                        }

                        parts.Add(part);
                    }
                    continue;
                }

                if (builder.Length + word.Length > LIMIT) {
                    parts.Add(builder.ToString().TrimEnd(' '));
                    builder.Clear();
                }

                if (builder.Length == 0 && prefix.Length != 0) {
                    builder.Append(prefix);
                    builder.Append(' ');
                }

                builder.Append(word);
                builder.Append(' ');
            }

            if (builder.Length != 0) {
                parts.Add(builder.ToString().TrimEnd(' '));
            }

            return parts.ToArray();
        }

        private void BroadcastMessage(IEncodable message) {
            foreach (var client in this.Clients.Values) {
                if (client.Handshake == null || client.Conn == null) {
                    continue;
                }

                Task.Run(async () => {
                    await SecretMessage.SendSecretMessage(client.Conn.GetStream(), client.Handshake.Keys.tx, message);
                });
            }
        }

        private string LocalisedChannelName(InputChannel channel) {
            uint rowId;
            switch (channel) {
                case InputChannel.Tell:
                    rowId = 3;
                    break;
                case InputChannel.Say:
                    rowId = 1;
                    break;
                case InputChannel.Party:
                    rowId = 4;
                    break;
                case InputChannel.Alliance:
                    rowId = 17;
                    break;
                case InputChannel.Yell:
                    rowId = 16;
                    break;
                case InputChannel.Shout:
                    rowId = 2;
                    break;
                case InputChannel.FreeCompany:
                    rowId = 7;
                    break;
                case InputChannel.PvpTeam:
                    rowId = 19;
                    break;
                case InputChannel.NoviceNetwork:
                    rowId = 18;
                    break;
                case InputChannel.CrossLinkshell1:
                    rowId = 20;
                    break;
                case InputChannel.CrossLinkshell2:
                    rowId = 300;
                    break;
                case InputChannel.CrossLinkshell3:
                    rowId = 301;
                    break;
                case InputChannel.CrossLinkshell4:
                    rowId = 302;
                    break;
                case InputChannel.CrossLinkshell5:
                    rowId = 303;
                    break;
                case InputChannel.CrossLinkshell6:
                    rowId = 304;
                    break;
                case InputChannel.CrossLinkshell7:
                    rowId = 305;
                    break;
                case InputChannel.CrossLinkshell8:
                    rowId = 306;
                    break;
                case InputChannel.Linkshell1:
                    rowId = 8;
                    break;
                case InputChannel.Linkshell2:
                    rowId = 9;
                    break;
                case InputChannel.Linkshell3:
                    rowId = 10;
                    break;
                case InputChannel.Linkshell4:
                    rowId = 11;
                    break;
                case InputChannel.Linkshell5:
                    rowId = 12;
                    break;
                case InputChannel.Linkshell6:
                    rowId = 13;
                    break;
                case InputChannel.Linkshell7:
                    rowId = 14;
                    break;
                case InputChannel.Linkshell8:
                    rowId = 15;
                    break;
                default:
                    rowId = 0;
                    break;
            };
            return this.plugin.Interface.Data.GetExcelSheet<LogFilter>().GetRow(rowId).Name;
        }

        public void OnChatChannelChange(uint channel) {
            var inputChannel = (InputChannel)channel;
            this.currentChannel = inputChannel;

            var localisedName = this.LocalisedChannelName(inputChannel);

            var msg = new ServerChannel(inputChannel, localisedName);
            this.BroadcastMessage(msg);
        }

        private void BroadcastAvailability(bool available) {
            this.BroadcastMessage(new Availability(available));
        }

        private PlayerData GeneratePlayerData() {
            var player = this.plugin.Interface.ClientState.LocalPlayer;
            if (player == null) {
                return null;
            }

            var homeWorld = player.HomeWorld.GameData.Name;
            var currentWorld = player.CurrentWorld.GameData.Name;
            var territory = this.plugin.Interface.Data.GetExcelSheet<TerritoryType>().GetRow(this.plugin.Interface.ClientState.TerritoryType);
            var location = territory.PlaceName.Value.Name;
            var name = player.Name;

            return new PlayerData(homeWorld, currentWorld, location, name);
        }

        private async Task SendPlayerData(Client client) {
            var playerData = this.GeneratePlayerData();
            if (playerData == null) {
                return;
            }
            await SecretMessage.SendSecretMessage(client.Conn.GetStream(), client.Handshake.Keys.tx, playerData);
        }

        private void BroadcastPlayerData() {
            var playerData = this.GeneratePlayerData();

            if (playerData == null) {
                this.BroadcastMessage(EmptyPlayerData.Instance);
                return;
            }

            this.BroadcastMessage(playerData);
        }

        public void OnLogIn(object sender, EventArgs e) {
            this.BroadcastAvailability(true);
            // send player data on next framework update
            this.sendPlayerData = true;
        }

        public void OnLogOut(object sender, EventArgs e) {
            this.BroadcastAvailability(false);
            this.BroadcastPlayerData();
        }

        public void OnTerritoryChange(object sender, ushort territoryId) => this.BroadcastPlayerData();

        public void Dispose() {
            // stop accepting new clients
            this.tokenSource.Cancel();
            foreach (var client in this.clients.Values) {
                Task.Run(async () => {
                    // tell clients we're shutting down
                    if (client.Handshake != null) {
                        try {
                            // time out after 5 seconds
                            client.Conn.SendTimeout = 5_000;
                            await SecretMessage.SendSecretMessage(client.Conn.GetStream(), client.Handshake.Keys.tx, ServerShutdown.Instance);
                        } catch (Exception) { }
                    }
                    // cancel threads for open clients
                    client.TokenSource.Cancel();
                });
            }
            this.plugin.Functions.ReceiveFriendList -= this.OnReceiveFriendList;
        }
    }

    public class Client {
        public bool Connected { get; set; } = false;
        public TcpClient Conn { get; private set; }
        public HandshakeInfo Handshake { get; set; }
        public CancellationTokenSource TokenSource { get; } = new CancellationTokenSource();
        public Channel<ServerMessage> Queue { get; } = Channel.CreateUnbounded<ServerMessage>();

        public Client(TcpClient conn) {
            this.Conn = conn;
        }

        public void Disconnect() {
            this.Connected = false;
            this.TokenSource.Cancel();
            this.Conn.Close();
        }
    }

    internal static class TcpListenerExt {
        public static async Task<TcpClient> GetTcpClient(this TcpListener listener, CancellationTokenSource source) {
            using (source.Token.Register(listener.Stop)) {
                try {
                    var client = await listener.AcceptTcpClientAsync().ConfigureAwait(false);
                    return client;
                } catch (ObjectDisposedException ex) {
                    // Token was canceled - swallow the exception and return null
                    if (source.Token.IsCancellationRequested) {
                        return null;
                    }

                    throw ex;
                }
            }
        }
    }
}
