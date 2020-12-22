using Dalamud.Game.Chat;
using Dalamud.Game.Chat.SeStringHandling;
using Dalamud.Game.Chat.SeStringHandling.Payloads;
using Dalamud.Game.Internal;
using Dalamud.Plugin;
using Lumina.Excel.GeneratedSheets;
using MessagePack;
using Sodium;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using System.Threading.Channels;
using System.Threading.Tasks;
using XIVChatCommon;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Client;
using XIVChatCommon.Message.Server;

namespace XIVChatPlugin {
    public class Server : IDisposable {
        private readonly Plugin plugin;

        private readonly CancellationTokenSource tokenSource = new CancellationTokenSource();
        private readonly ConcurrentQueue<string> toGame = new ConcurrentQueue<string>();

        private readonly ConcurrentDictionary<Guid, Client> clients = new ConcurrentDictionary<Guid, Client>();
        public IReadOnlyDictionary<Guid, Client> Clients => this.clients;
        public readonly Channel<Tuple<Client, Channel<bool>>> pendingClients = Channel.CreateUnbounded<Tuple<Client, Channel<bool>>>();

        private readonly HashSet<Guid> waitingForFriendList = new HashSet<Guid>();

        private readonly LinkedList<ServerMessage> backlog = new LinkedList<ServerMessage>();

        private TcpListener? listener;

        private bool sendPlayerData;

        private volatile bool running;
        private bool Running => this.running;

        private InputChannel currentChannel = InputChannel.Say;

        private const int MaxMessageSize = 128_000;

        public Server(Plugin plugin) {
            this.plugin = plugin ?? throw new ArgumentNullException(nameof(plugin), "Plugin cannot be null");
            if (this.plugin.Config.KeyPair == null) {
                this.RegenerateKeyPair();
            }

            this.plugin.Functions.ReceiveFriendList += this.OnReceiveFriendList;
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
                    if (!this.plugin.Config.PairingMode) {
                        await Task.Delay(5_000);
                        continue;
                    }

                    var playerName = this.plugin.Interface.ClientState.LocalPlayer?.Name;

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
                    var portBytes = BitConverter.GetBytes(this.plugin.Config.Port).Reverse().ToArray();
                    var key = this.plugin.Config.KeyPair!.PublicKey;
                    // magic + string length + string + port + key
                    var payload = new byte[1 + 1 + utf8.Length + portBytes.Length + key.Length]; // assuming names can only be 32 bytes here
                    payload[0] = 14;
                    payload[1] = (byte)utf8.Length;
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

            foreach (var id in this.waitingForFriendList) {
                if (!this.Clients.TryGetValue(id, out var client)) {
                    continue;
                }

                try {
                    await SecretMessage.SendSecretMessage(client.Conn.GetStream(), client.Handshake!.Keys.tx, msg, client.TokenSource.Token);
                } catch (Exception ex) {
                    PluginLog.LogError($"Could not send message: {ex.Message}");
                }
            }

            this.waitingForFriendList.Clear();
        }

        public void Spawn() {
            var port = this.plugin.Config.Port;

            Task.Run(async () => {
                this.listener = new TcpListener(IPAddress.Any, port);
                this.listener.Start();

                this.running = true;
                PluginLog.Log("Running...");
                this.SpawnPairingModeTask();
                while (!this.tokenSource.IsCancellationRequested) {
                    var conn = await this.listener.GetTcpClient(this.tokenSource);
                    this.SpawnClientTask(conn);
                }

                this.running = false;
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

            var colour = this.plugin.Functions.GetChannelColour(chatCode) ?? chatCode.DefaultColour();

            if (sender.Payloads.Count > 0) {
                var format = this.FormatFor(chatCode.Type);
                if (format != null && format.IsPresent) {
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
                (ChatType)type,
                sender.Encode(),
                message.Encode(),
                chunks
            );

            this.backlog.AddLast(msg);
            while (this.backlog.Count > this.plugin.Config.BacklogCount) {
                this.backlog.RemoveFirst();
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

        private static readonly IReadOnlyList<byte> Magic = new byte[] {
            14, 20, 67,
        };

        private void SpawnClientTask(TcpClient? conn) {
            if (conn == null) {
                return;
            }

            Task.Run(async () => {
                var stream = conn.GetStream();

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

                    read += await stream.ReadAsync(magic, read, magic.Length - read, cts.Token);
                }

                // ignore this connection if incorrect magic bytes
                if (!magic.SequenceEqual(Magic)) {
                    return;
                }

                var handshake = await KeyExchange.ServerHandshake(this.plugin.Config.KeyPair!, stream);
                var newClient = new Client(conn) {
                    Handshake = handshake,
                };

                // if this public key isn't trusted, prompt first
                if (!this.plugin.Config.TrustedKeys.Values.Any(entry => entry.Item2.SequenceEqual(handshake.RemotePublicKey))) {
                    // if configured to not accept new clients, reject connection
                    if (!this.plugin.Config.AcceptNewClients) {
                        return;
                    }

                    var accepted = Channel.CreateBounded<bool>(1);

                    await this.pendingClients.Writer.WriteAsync(Tuple.Create(newClient, accepted), this.tokenSource.Token);
                    if (!await accepted.Reader.ReadAsync(this.tokenSource.Token)) {
                        return;
                    }
                }

                var id = Guid.NewGuid();
                newClient.Connected = true;
                this.clients[id] = newClient;

                // send availability
                var available = this.plugin.Interface.ClientState.LocalPlayer != null;
                try {
                    await SecretMessage.SendSecretMessage(stream, handshake.Keys.tx, new Availability(available), this.tokenSource.Token);
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
                    await SecretMessage.SendSecretMessage(
                        stream,
                        handshake.Keys.tx,
                        new ServerChannel(
                            channel,
                            this.LocalisedChannelName(channel)
                        ),
                        this.tokenSource.Token
                    );
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
                                // ReSharper disable once LocalVariableHidesMember
                                var backlog = ClientBacklog.Decode(payload);

                                var backlogMessages = new List<ServerMessage>();

                                var node = this.backlog.Last;
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

                                await SendBacklogs(backlogMessages.ToArray(), stream, client);
                                break;
                            case ClientOperation.CatchUp:
                                var catchUp = ClientCatchUp.Decode(payload);
                                // I'm not sure why this needs to be done, but apparently it does
                                var after = catchUp.After.AddMilliseconds(1);
                                var msgs = this.MessagesAfter(after);

                                if (client.GetPreference(ClientPreference.BacklogNewestMessagesFirst, false)) {
                                    msgs = msgs.Reverse();
                                }

                                await SendBacklogs(msgs, stream, client);
                                break;
                            case ClientOperation.PlayerList:
                                var playerList = ClientPlayerList.Decode(payload);

                                if (playerList.Type == PlayerListType.Friend) {
                                    this.waitingForFriendList.Add(id);

                                    if (!this.plugin.Functions.RequestingFriendList && !this.plugin.Functions.RequestFriendList()) {
                                        this.plugin.Interface.Framework.Gui.Chat.PrintError($"[{this.plugin.Name}] Please open your friend list to enable friend list support. You should only need to do this on initial install or after updates.");
                                    }
                                }

                                break;
                            case ClientOperation.Preferences:
                                var preferences = ClientPreferences.Decode(payload);
                                client.Preferences = preferences;

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

                this.clients.TryRemove(id, out _);
                PluginLog.Log($"Client thread ended: {id}");
            }).ContinueWith(_ => {
                try {
                    conn.Close();
                } catch (ObjectDisposedException) { }
            });
        }

        public class NameFormatting {
            public string Before { get; private set; } = string.Empty;
            public string After { get; private set; } = string.Empty;
            public bool IsPresent { get; private set; } = true;

            public static NameFormatting Empty() {
                return new NameFormatting {
                    IsPresent = false,
                };
            }

            public static NameFormatting Of(string before, string after) {
                return new NameFormatting {
                    Before = before,
                    After = after,
                };
            }
        }

        private Dictionary<ChatType, NameFormatting> Formats { get; } = new Dictionary<ChatType, NameFormatting>();

        private NameFormatting? FormatFor(ChatType type) {
            if (this.Formats.TryGetValue(type, out var cached)) {
                return cached;
            }

            var logKind = this.plugin.Interface.Data.GetExcelSheet<LogKind>().GetRow((ushort)type);

            if (logKind == null) {
                return null;
            }

            var format = logKind.Format;
            var sestring = this.plugin.Interface.SeStringManager.Parse(format.RawData.ToArray());

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

        private static async Task SendBacklogs(IEnumerable<ServerMessage> messages, Stream stream, Client client) {
            var size = 5 + SecretMessage.MacSize(); // assume 5 bytes for payload lead-in, although it's likely to be less
            var responseMessages = new List<ServerMessage>();

            async Task SendBacklog() {
                var resp = new ServerBacklog(responseMessages.ToArray());
                try {
                    await SecretMessage.SendSecretMessage(stream, client.Handshake!.Keys.tx, resp, client.TokenSource.Token);
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

        private static IEnumerable<Chunk> ToChunks(SeString msg, uint? defaultColour) {
            var chunks = new List<Chunk>();

            var italic = false;
            var foreground = new Stack<uint>();
            var glow = new Stack<uint>();

            void Append(string text) {
                chunks.Add(new TextChunk(text) {
                    FallbackColour = defaultColour,
                    Foreground = foreground.Count > 0 ? foreground.Peek() : (uint?)null,
                    Glow = glow.Count > 0 ? glow.Peek() : (uint?)null,
                    Italic = italic,
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
                        chunks.Add(new IconChunk {
                            index = 54,
                        });
                        var autoText = ((AutoTranslatePayload)payload).Text;
                        Append(autoText.Substring(2, autoText.Length - 4));
                        chunks.Add(new IconChunk {
                            index = 55,
                        });
                        break;
                    case PayloadType.Icon:
                        var index = ((IconPayload)payload).IconIndex;
                        chunks.Add(new IconChunk {
                            index = (byte)index,
                        });
                        break;
                    case PayloadType.Unknown:
                        var rawPayload = (RawPayload)payload;
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

        private IEnumerable<ServerMessage> MessagesAfter(DateTime time) => this.backlog.Where(msg => msg.Timestamp > time).ToArray();

        private static IEnumerable<string> Wrap(string input) {
            const int limit = 500;

            if (input.Length <= limit) {
                return new[] {
                    input,
                };
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

            var builder = new StringBuilder(limit);

            foreach (var word in input.Split(' ')) {
                if (word.Length > limit) {
                    int wordParts = (int)Math.Ceiling((float)word.Length / limit);
                    for (int i = 0; i < wordParts; i++) {
                        var start = i == 0 ? 0 : (i * limit);
                        var partLength = limit;
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

                if (builder.Length + word.Length > limit) {
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

        private PlayerData? GeneratePlayerData() {
            var player = this.plugin.Interface.ClientState.LocalPlayer;
            if (player == null) {
                return null;
            }

            var homeWorld = player.HomeWorld.GameData.Name;
            var currentWorld = player.CurrentWorld.GameData.Name;
            var territory = this.plugin.Interface.Data.GetExcelSheet<TerritoryType>().GetRow(this.plugin.Interface.ClientState.TerritoryType);
            var location = territory?.PlaceName?.Value?.Name ?? "???";
            var name = player.Name;

            return new PlayerData(homeWorld, currentWorld, location, name);
        }

        private async Task SendPlayerData(Client client) {
            var playerData = this.GeneratePlayerData();
            if (playerData == null) {
                return;
            }

            await SecretMessage.SendSecretMessage(client.Conn.GetStream(), client.Handshake!.Keys.tx, playerData);
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

        public void OnTerritoryChange(object sender, ushort territoryId) => this.sendPlayerData = true;

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
        public bool Connected { get; set; }
        public TcpClient Conn { get; }
        public HandshakeInfo? Handshake { get; set; }
        public ClientPreferences? Preferences { get; set; }
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

        public T GetPreference<T>(ClientPreference pref, T def = default) {
            var prefs = this.Preferences;

            if (prefs == null) {
                return def;
            }

            return prefs.TryGetValue(pref, out T result) ? result : def;
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
