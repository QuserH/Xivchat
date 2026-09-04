using MessagePack;
using Sodium;
using System;
using System.IO;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Runtime.CompilerServices;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Threading.Channels;
using System.Threading.Tasks;
using Dalamud.Game.Inventory;
using Dalamud.Game.Text;
using Dalamud.Game.Text.SeStringHandling;
using Dalamud.Game.Text.SeStringHandling.Payloads;
using Dalamud.Plugin.Services;
using Dalamud.Hooking;
using Dalamud.Utility;
using FFXIVClientStructs.Interop;
using Lumina.Excel.Sheets;
using XIVChatCommon;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Client;
using XIVChatCommon.Message.Server;
using Dalamud.Game.Chat;
using Dalamud.Game.ClientState.Objects.SubKinds;
using FFXIVClientStructs.FFXIV.Client.Game;
using FFXIVClientStructs.FFXIV.Client.Game.Group;
using FFXIVClientStructs.FFXIV.Client.Game.UI;
using FFXIVClientStructs.FFXIV.Client.Graphics.Environment;
using FFXIVClientStructs.FFXIV.Client.UI;
using FFXIVClientStructs.FFXIV.Client.UI.Info;
using FFXIVClientStructs.FFXIV.Client.UI.Misc;

namespace XIVChatPlugin {
    internal class Server : IDisposable {
        private const int MaxMessageLength = 500;

        private static readonly string[] PublicPrefixes = [
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
        ];

        private readonly Plugin _plugin;
        private readonly MarketDataCache _marketCache;

        private readonly Stopwatch _sendWatch = new();
        private readonly Stopwatch _inventoryWatch = new();
        private readonly Stopwatch _walletWatch = new();
        private readonly Stopwatch _weatherWatch = new();
        private readonly Stopwatch _jobsWatch = new();
        private readonly Stopwatch _dailiesWatch = new();
        private readonly Stopwatch _collectionsWatch = new();
        private readonly Stopwatch _fishingWatch = new();
        private readonly Stopwatch _submarineWatch = new();
        private readonly Stopwatch _partyWatch = new();
        private long _lastPartyFingerprint;
        private readonly PhoneActivityTracker _activityTracker;

        private readonly CancellationTokenSource _tokenSource = new();
        private readonly ConcurrentQueue<string> _toGame = new();
        private readonly ConcurrentQueue<ClientFriendAction> _friendActions = new();
        private readonly ConcurrentQueue<int> _jobsActions = new();

        private readonly ConcurrentDictionary<Guid, BaseClient> _clients = new();
        internal IReadOnlyDictionary<Guid, BaseClient> Clients => this._clients;
        internal readonly Channel<Tuple<BaseClient, Channel<bool>>> PendingClients = Channel.CreateUnbounded<Tuple<BaseClient, Channel<bool>>>();

        private readonly HashSet<Guid> _waitingForFriendList = [];

        // 每个角色独立的内存历史缓冲（容量 = 历史消息数量），角色间互不挤占
        private readonly Dictionary<string, LinkedList<ServerMessage>> _backlogByChar = new(StringComparer.OrdinalIgnoreCase);
        private readonly object _backlogLock = new();
        // 只重写“有新消息”的角色文件：避免仅登录角色A时把其它角色文件一并刷新/覆盖
        private readonly HashSet<string> _dirtyBacklogTags = new(StringComparer.OrdinalIgnoreCase);
        private List<Player> _lastFriends = [];
        private DateTime _lastPersist = DateTime.MinValue;

        private TcpListener? _listener;

        private bool _sendPlayerData;
        private readonly ConcurrentQueue<Guid> _awaitingPlayerData = new();
        private readonly ConcurrentQueue<Guid> _awaitingAvailability = new();
        private readonly ConcurrentQueue<Guid> _awaitingHousingLocation = new();
        private readonly ConcurrentQueue<Guid> _awaitingInventory = new();
        private readonly ConcurrentQueue<Guid> _awaitingWallet = new();
        private readonly ConcurrentQueue<Guid> _awaitingWeather = new();
        private readonly ConcurrentQueue<Guid> _awaitingJobs = new();
        private readonly ConcurrentQueue<Guid> _awaitingDailies = new();
        private readonly ConcurrentQueue<Guid> _awaitingActivity = new();
        private readonly ConcurrentQueue<Guid> _awaitingCollections = new();
        private readonly ConcurrentQueue<Guid> _awaitingMaps = new();
        private readonly ConcurrentQueue<Guid> _awaitingFishing = new();

        private readonly ConcurrentQueue<Guid> _awaitingParty = new();

        // Market search is request/reply rather than a polled snapshot: issuing one
        // mutates InfoProxyItemSearch, which the PC's own market UI also owns.
        private readonly ConcurrentQueue<(Guid Client, uint ItemId, bool HqOnly)> _marketSearches = new();
        private readonly ConcurrentQueue<Guid> _awaitingMarketCategories = new();
        private Guid _marketRequester;
        private uint _marketPendingItemId;
        private bool _marketAwaitingListings;

        /// <summary>
        /// Whether WaitingForListings has been observed true since the request went out.
        /// Without this the empty proxy read before the flag flips is indistinguishable
        /// from an empty board.
        /// </summary>
        private bool _marketSawWaiting;
        private readonly Stopwatch _marketWatch = new();
        private uint _marketSavedSearchItemId;

        /// <summary>
        /// Earliest moment the next search may be handed to the game. The client
        /// throttles market searches internally (~7 s); a request fired inside the
        /// window is silently ignored -- WaitingForListings stays up forever, the
        /// 10 s timeout fires, the next queued request repeats the mistake, and the
        /// queue turns into a convoy where every query times out (observed as six
        /// consecutive timeouts with mismatched SearchItemId in the log). Gating the
        /// dequeue on this keeps one request alive long enough for the game to
        /// actually answer it.
        /// </summary>
        private DateTimeOffset _marketCooldownUntil = DateTimeOffset.MinValue;

        // Purchases ride the same single-flight slot as searches, because they need the
        // same thing: a fresh Listings array for this item. A purchase is a search whose
        // ready-state hands off to SendPurchaseRequestPacket instead of replying. Sharing
        // the slot is also what keeps a purchase from racing a query for the array both
        // of them read.
        private readonly ConcurrentQueue<(Guid Client, ClientMarketPurchase Request)> _marketPurchases = new();

        /// <summary>
        /// The purchase whose confirming re-query is in flight, if any. Non-null means
        /// the current market request must end in a purchase attempt rather than a reply.
        /// </summary>
        private ClientMarketPurchase? _marketPurchaseRequest;

        /// <summary>
        /// Set once the packet is away, while waiting for the game's purchase-response
        /// callback. Success is only ever reported from that callback: the request
        /// succeeding locally says nothing about whether the server took it.
        /// </summary>
        private bool _marketAwaitingPurchaseReply;
        private Guid _marketPurchaseRequester;
        private uint _marketPurchaseItemId;
        private ulong _marketPurchaseListingId;
        private uint _marketPurchasePrice;
        private uint _marketPurchaseQuantity;
        private uint _marketPurchaseTax;
        private readonly Stopwatch _marketPurchaseWatch = new();

        private unsafe delegate void PurchaseResponseDelegate(InfoProxyItemSearch* proxy, uint itemId, uint errorId);
        private Hook<PurchaseResponseDelegate>? _purchaseResponseHook;

        private volatile bool _running;
        private bool Running => this._running;

        private InputChannel _currentChannel = InputChannel.Say;
        private SeString? _currentChannelName;

        private ServerHousingLocation _lastHousingLocation;
        private long _lastInventoryFingerprint;
        private bool _hasInventorySnapshot;
        private long _lastWalletFingerprint;
        private bool _hasWalletSnapshot;
        private long _lastWeatherFingerprint;
        private bool _hasWeatherSnapshot;
        private long _lastJobsFingerprint;
        private bool _hasJobsSnapshot;
        private long _lastDailiesFingerprint;
        private bool _hasDailiesSnapshot;
        private long _lastActivityFingerprint;
        private bool _hasActivitySnapshot;
        private long _lastCollectionsFingerprint;
        private bool _hasCollectionsSnapshot;
        private long _lastMapsFingerprint;
        private bool _hasMapsSnapshot;
        private ServerMapExpansion[]? _mapCatalog;
        private long _lastFishingFingerprint;
        private bool _hasFishingSnapshot;
        private bool? _lastGameAvailability;
        // ClientState/ObjectTable can remain populated briefly during the trip to
        // the title screen.  Once Dalamud reports logout, do not advertise that
        // stale player as available again before the next login event.
        private volatile bool _logoutObserved;

        private static readonly GameInventoryType[] PhoneInventoryTypes = [
            GameInventoryType.Inventory1,
            GameInventoryType.Inventory2,
            GameInventoryType.Inventory3,
            GameInventoryType.Inventory4,
            GameInventoryType.EquippedItems,
            GameInventoryType.ArmoryOffHand,
            GameInventoryType.ArmoryHead,
            GameInventoryType.ArmoryBody,
            GameInventoryType.ArmoryHands,
            GameInventoryType.ArmoryWaist,
            GameInventoryType.ArmoryLegs,
            GameInventoryType.ArmoryFeets,
            GameInventoryType.ArmoryEar,
            GameInventoryType.ArmoryNeck,
            GameInventoryType.ArmoryWrist,
            GameInventoryType.ArmoryRings,
            GameInventoryType.ArmorySoulCrystal,
            GameInventoryType.ArmoryMainHand,
            GameInventoryType.SaddleBag1,
            GameInventoryType.SaddleBag2,
            GameInventoryType.PremiumSaddleBag1,
            GameInventoryType.PremiumSaddleBag2,
            GameInventoryType.RetainerPage1,
            GameInventoryType.RetainerPage2,
            GameInventoryType.RetainerPage3,
            GameInventoryType.RetainerPage4,
            GameInventoryType.RetainerPage5,
            GameInventoryType.RetainerPage6,
            GameInventoryType.RetainerPage7,
            GameInventoryType.FreeCompanyPage1,
            GameInventoryType.FreeCompanyPage2,
            GameInventoryType.FreeCompanyPage3,
            GameInventoryType.FreeCompanyPage4,
            GameInventoryType.FreeCompanyPage5,
            GameInventoryType.HousingExteriorStoreroom,
            GameInventoryType.HousingInteriorStoreroom1,
            GameInventoryType.HousingInteriorStoreroom2,
            GameInventoryType.HousingInteriorStoreroom3,
            GameInventoryType.HousingInteriorStoreroom4,
            GameInventoryType.HousingInteriorStoreroom5,
            GameInventoryType.HousingInteriorStoreroom6,
            GameInventoryType.HousingInteriorStoreroom7,
            GameInventoryType.HousingInteriorStoreroom8,
            GameInventoryType.HousingInteriorStoreroom9,
            GameInventoryType.HousingInteriorStoreroom10,
            GameInventoryType.HousingInteriorStoreroom11,
            GameInventoryType.HousingExteriorStoreroom2,
        ];

        private const int MaxMessageSize = 128_000;

        internal Server(Plugin plugin) {
            this._plugin = plugin;
            if (this._plugin.Config.KeyPair == null) {
                this.RegenerateKeyPair();
            }

            // Initialize market data cache (global, version-stamped)
            var cacheDir = Path.Combine(Path.GetDirectoryName(Plugin.Interface.ConfigFile.FullName) ?? "", "XIVChat");
            this._marketCache = new MarketDataCache(Plugin.DataManager, cacheDir);
            this._marketCache.Initialize();

            this._lastHousingLocation = this._plugin.Functions.HousingLocation;

            this._sendWatch.Start();
            this._inventoryWatch.Start();
            this._walletWatch.Start();
            this._weatherWatch.Start();
            this._jobsWatch.Start();
            this._dailiesWatch.Start();
            this._collectionsWatch.Start();
            this._fishingWatch.Start();
            this._submarineWatch.Start();
            this._activityTracker = new PhoneActivityTracker(plugin);

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

                SeString? lastPlayerName = null;

                Task<UdpReceiveResult>? receiveTask = null;

                while (this.Running) {
                    if (!this._plugin.Config.PairingMode) {
                        await Task.Delay(5_000);
                        continue;
                    }

                    var playerName = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer?.Name;

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

                    var utf8 = Encoding.UTF8.GetBytes(lastPlayerName.TextValue);
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

                Plugin.Log.Info("Scan response thread done");
            });
        }

        private async void OnReceiveFriendList(List<Player> friends) {
            this._lastFriends = friends;
            var msg = new ServerPlayerList(PlayerListType.Friend, friends.ToArray());

            foreach (var id in this._waitingForFriendList) {
                if (!this.Clients.TryGetValue(id, out var client)) {
                    continue;
                }

                await client.Queue.Writer.WriteAsync(msg);
            }

            this._waitingForFriendList.Clear();
        }

        internal void Spawn() {
            // 异步加载历史，避免插件启动时同步读大量 backlog 导致登录/选人界面卡顿
            System.Threading.Tasks.Task.Run(() => this.LoadBacklog());
            var port = this._plugin.Config.Port;

            Task.Run(async () => {
                this._listener = new TcpListener(IPAddress.Any, port);
                this._listener.Start();

                this._running = true;
                Plugin.Log.Info("Running...");
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

        internal void RegenerateKeyPair() {
            this._plugin.Config.KeyPair = PublicKeyBox.GenerateKeyPair();
            this._plugin.Config.Save();
        }

        internal void OnChat(IHandleableChatMessage message) {
            if (message.IsHandled) {
                return;
            }

            if (!this._plugin.Config.SendBattle && message.LogKind.IsBattle()) {
                return;
            }

            var chunks = new List<Chunk>();

            var colour = this._plugin.Functions.GetChannelColour(message.LogKind) ?? message.LogKind.DefaultColour();

            chunks.AddRange(ToChunks(message.Message, colour));

            string? senderName = null;
            string? senderWorld = null;
            string? senderStatus = null;
            int? senderStatusIcon = null;
            foreach (var payload in message.Sender.Payloads) {
                if (payload is PlayerPayload playerPayload) {
                    senderName = playerPayload.PlayerName;
                    var worldRow = playerPayload.World;
                    if (worldRow.IsValid) senderWorld = worldRow.Value.Name.ExtractText();
                }
            }
            // 情感动作等消息的发送者 payload 可能缺世界名：从好友名单/周围玩家补上老家世界，保证跨服会话 key 一致
            if (senderWorld == null && senderName != null) {
                var friend = this._lastFriends.FirstOrDefault(f => f.Name == senderName);
                if (friend != null) {
                    senderWorld = string.IsNullOrEmpty(friend.HomeWorldName) ? friend.CurrentWorldName : friend.HomeWorldName;
                } else {
                    foreach (var obj in XIVChatPlugin.Plugin.ObjectTable) {
                        if (obj is IPlayerCharacter nearbySender && nearbySender.Name.TextValue == senderName) {
                            try {
                                var hw = nearbySender.HomeWorld.Value.Name.ExtractText();
                                if (!string.IsNullOrWhiteSpace(hw)) senderWorld = hw;
                            } catch { }
                            break;
                        }
                    }
                }
            }
            senderStatusIcon = GetSenderStatusIcon(message.Sender.Encode());
            int? senderWorldIcon = null;
            if (!string.IsNullOrEmpty(senderWorld)) {
                try {
                    var lw = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer?.HomeWorld.Value.Name.ExtractText();
                    if (!string.IsNullOrEmpty(lw) && !senderWorld.Equals(lw, StringComparison.OrdinalIgnoreCase)) {
                        senderWorldIcon = GetLastSenderIcon(message.Sender.Encode());
                    }
                } catch { }
            }


            // 自用动作补全：部分自用动作游戏不填发送者，用“内容以本角色名开头”判断并补全，App 才能识别为“我发的”
            var localPlayerName = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer?.Name.TextValue;
            var senderBytes = message.Sender.Encode();
            var selfFlag = false;
            if ((message.LogKind == XivChatType.StandardEmote || message.LogKind == XivChatType.CustomEmote) && !string.IsNullOrEmpty(localPlayerName) &&
                (message.Message.TextValue ?? string.Empty).StartsWith(localPlayerName, StringComparison.Ordinal) && string.IsNullOrEmpty(senderName)) {
                senderName = localPlayerName;
                try {
                    var lw = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer?.HomeWorld.Value.Name.ExtractText();
                    if (!string.IsNullOrWhiteSpace(lw)) senderWorld = lw;
                } catch { }
                senderBytes = new SeStringBuilder().AddText(localPlayerName).Build().Encode();
            }
            selfFlag = !string.IsNullOrEmpty(senderName) && senderName == localPlayerName;

            // 情感动作：从内容里提取“目标角色”，App 据此把“我对他人的动作”路由到对方的会话
            string? targetName = null;
            string? targetWorld = null;
            if (message.LogKind == XivChatType.StandardEmote || message.LogKind == XivChatType.CustomEmote) {
                foreach (var payload in message.Message.Payloads) {
                    if (payload is PlayerPayload targetPayload) {
                        if (senderName != null && targetPayload.PlayerName == senderName) continue;
                        targetName = targetPayload.PlayerName;
                        var targetWorldRow = targetPayload.World;
                        if (targetWorldRow.IsValid) targetWorld = targetWorldRow.Value.Name.ExtractText();
                        break;
                    }
                }
            }
            // 自用消息日志（任何类型）：排查自用情感动作是否到达插件、目标解析结果
            if (senderName != null && XIVChatPlugin.Plugin.ObjectTable.LocalPlayer?.Name.TextValue == senderName) {
                var contentText = message.Message.TextValue ?? string.Empty;
                if ((message.LogKind == XivChatType.StandardEmote || message.LogKind == XivChatType.CustomEmote) && targetName == null) {
                    // 先看当前选中目标对象：对某人做动作时目标通常就是对方，非好友也能识别
                    if (XIVChatPlugin.Plugin.ObjectTable.LocalPlayer?.TargetObject is IPlayerCharacter targetChara && !string.IsNullOrEmpty(targetChara.Name.TextValue)) {
                        if (contentText.Contains(targetChara.Name.TextValue, StringComparison.OrdinalIgnoreCase)) {
                            targetName = targetChara.Name.TextValue;
                            try {
                                var tw = targetChara.HomeWorld.Value.Name.ExtractText();
                                if (!string.IsNullOrWhiteSpace(tw)) targetWorld = tw;
                            } catch { }
                        }
                    }
                    // 再扫描周围玩家：对某人做动作时对方就在附近，名字出现在文本里即可认定
                    if (targetName == null) {
                        foreach (var obj in XIVChatPlugin.Plugin.ObjectTable) {
                            if (obj is IPlayerCharacter nearby && !string.IsNullOrEmpty(nearby.Name.TextValue) && nearby.Name.TextValue != localPlayerName) {
                                if (contentText.Contains(nearby.Name.TextValue, StringComparison.OrdinalIgnoreCase)) {
                                    targetName = nearby.Name.TextValue;
                                    try {
                                        var nw = nearby.HomeWorld.Value.Name.ExtractText();
                                        if (!string.IsNullOrWhiteSpace(nw)) targetWorld = nw;
                                    } catch { }
                                    break;
                                }
                            }
                        }
                    }
                    // 再兜底好友名单匹配
                    if (targetName == null) {
                        foreach (var friend in this._lastFriends) {
                            if (string.IsNullOrEmpty(friend.Name)) continue;
                            if (contentText.Contains(friend.Name, StringComparison.OrdinalIgnoreCase)) {
                                targetName = friend.Name;
                                targetWorld = string.IsNullOrEmpty(friend.HomeWorldName) ? friend.CurrentWorldName : friend.HomeWorldName;
                                break;
                            }
                        }
                    }
                }

            }

            var msg = new ServerMessage(
                DateTime.UtcNow,
                (ushort) message.LogKind,
                senderBytes,
                message.Message.Encode(),
                chunks,
                senderName,
                senderWorld,
                senderStatus,
                senderStatusIcon,
                characterTag: null,
                targetName: targetName,
                targetWorld: targetWorld,
                selfFlag: selfFlag,
                senderWorldIcon: senderWorldIcon
            );

            if (this._plugin.Config.BacklogEnabled) {
                msg.CharacterTag = this.CurrentCharacterTag();
                lock (this._backlogLock) {
                    if (!string.IsNullOrEmpty(msg.CharacterTag)) {
                        if (!this._backlogByChar.TryGetValue(msg.CharacterTag, out var charBacklog)) {
                            charBacklog = new LinkedList<ServerMessage>();
                            this._backlogByChar[msg.CharacterTag] = charBacklog;
                        }
                        charBacklog.AddLast(msg);
                        while (charBacklog.Count > this._plugin.Config.BacklogCount) {
                            charBacklog.RemoveFirst();
                        }
                        this._dirtyBacklogTags.Add(msg.CharacterTag);
                    }
                }
                this.MaybePersistBacklog();
            }

            foreach (var client in this._clients.Values) {
                client.Queue.Writer.TryWrite(msg);
            }
        }

        internal unsafe void OnFrameworkUpdate(IFramework framework) {
            var player = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer;
            var gameAvailable = !this._logoutObserved && XIVChatPlugin.Plugin.ClientState.IsLoggedIn && player != null;
            if (this._lastGameAvailability != gameAvailable) {
                this._lastGameAvailability = gameAvailable;
                this.BroadcastAvailability(gameAvailable);
                if (!gameAvailable) {
                    this._sendPlayerData = false;
                    this.BroadcastMessage(EmptyPlayerData.Instance);
                } else {
                    // 传送/过图黑屏时角色短暂不可见会触发离线广播；重新可见时强制重发角色资料，
                    // 避免 App 停留在“正在读取角色资料”直到手动重连
                    this._sendPlayerData = true;
                }
            }
            if (gameAvailable && this._sendPlayerData) {
                this.BroadcastPlayerData();
                this._sendPlayerData = false;
                // Pre-warm the category tree in the background so the phone's first
                // browse request is answered from cache instead of stalling the tick.
                if (this._marketCategoriesCache == null || !this._marketCache.GilShopDataReady) {
                    System.Threading.Tasks.Task.Run(() => {
                        try {
                            this.GetMarketCategories();
                        } catch (Exception ex) {
                            Plugin.Log.Warning($"Could not pre-build market categories: {ex.Message}");
                        }
                    });
                }
            }

            var housingLocation = this._plugin.Functions.HousingLocation;
            if (!Equals(housingLocation, this._lastHousingLocation)) {
                this.BroadcastMessage(housingLocation, ClientPreference.HousingLocationSupport);
                this._lastHousingLocation = housingLocation;
            }

            this.UpdateInventory();
            this.UpdateWallet();
            this.UpdateWeather();
            this.UpdateJobs();
            this.UpdateDailies();
            this.UpdateCollections();
            this.UpdateMaps();
            this.UpdateFishing();
            this.UpdateSubmarine();
            this.UpdateActivity();
            this.UpdateParty();
            this.UpdateMarket();
            this.UpdateMarketPurchase();
            this.UpdateMarketMonitor();

            while (this._friendActions.TryDequeue(out var friendAction)) {
                try {
                    this._plugin.Functions.ExecuteFriendAction(friendAction);
                } catch (Exception ex) {
                    Plugin.Log.Warning($"Could not execute friend action: {ex.Message}");
                }
            }

            while (this._jobsActions.TryDequeue(out var gearsetId)) {
                try {
                    var module = RaptureGearsetModule.Instance();
                    if (module != null && module->IsValidGearset(gearsetId)) {
                        module->EquipGearset(gearsetId);
                    }
                } catch (Exception ex) {
                    Plugin.Log.Warning($"Could not equip gearset {gearsetId}: {ex.Message}");
                }
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

                client.Queue.Writer.TryWrite(new Availability(gameAvailable));
            }

            while (this._awaitingHousingLocation.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) {
                    continue;
                }

                client.Queue.Writer.TryWrite(this._lastHousingLocation);
            }

            while (this._awaitingInventory.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) {
                    continue;
                }

                var inventory = this.BuildInventorySnapshot();
                if (inventory != null) {
                    client.Queue.Writer.TryWrite(inventory);
                }
            }

            while (this._awaitingWallet.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) {
                    continue;
                }

                var wallet = this.BuildWalletSnapshot();
                if (wallet != null) {
                    client.Queue.Writer.TryWrite(wallet);
                }
            }

            while (this._awaitingWeather.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) {
                    continue;
                }

                var weather = this.BuildWeatherSnapshot();
                if (weather != null) {
                    client.Queue.Writer.TryWrite(weather);
                }
            }

            while (this._awaitingJobs.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) continue;
                var jobs = this.BuildJobsSnapshot();
                if (jobs != null) client.Queue.Writer.TryWrite(jobs);
            }

            while (this._awaitingDailies.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) continue;
                var dailies = this.BuildDailiesSnapshot();
                if (dailies != null) client.Queue.Writer.TryWrite(dailies);
            }

            while (this._awaitingActivity.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) continue;
                client.Queue.Writer.TryWrite(this._activityTracker.Snapshot());
            }

            while (this._awaitingCollections.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) continue;
                var collections = this.BuildCollectionsSnapshot();
                if (collections != null) client.Queue.Writer.TryWrite(collections);
            }

            while (this._awaitingMaps.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) continue;
                var maps = this.BuildMapsSnapshot();
                if (maps != null) client.Queue.Writer.TryWrite(maps);
            }

            while (this._awaitingFishing.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) continue;
                var fishing = this.BuildFishingSnapshot();
                if (fishing != null) client.Queue.Writer.TryWrite(fishing);
            }

            while (this._awaitingMarketCategories.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) continue;
                // Built once per plugin load, then served from cache: the game's item
                // sheet does not change while running, and a full rebuild per request
                // made the first reply take seconds. Pre-warmed on login below, so the
                // phone's first request is answered instantly (how BetterMarketBoard
                // gets its instant category grid).
                // The first category request can arrive before the GilShopItem sheet is
                // ready.  Resolve through the guarded accessor rather than caching that
                // transient all-zero tree for the rest of the session.
                var categories = this.GetMarketCategories();
                if (categories != null) {
                    // Attach cache timestamp so phone can skip re-download if unchanged
                    categories.TimestampMs = _marketCache.CacheTimestampMs;
                    categories.GameVersion = _marketCache.CacheGameVersion;
                    client.Queue.Writer.TryWrite(categories);
                }
            }

            while (this._awaitingParty.TryDequeue(out var id)) {
                if (!this.Clients.TryGetValue(id, out var client) || client.Handshake == null) continue;
                var party = this.BuildPartyList() ?? new ServerPlayerList(PlayerListType.Party, []);
                client.Queue.Writer.TryWrite(party);
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

            Plugin.Log.Info($"[{Plugin.Name}] Sending chat: {message}");
            this._plugin.Functions.ProcessChatBox(message);
        }

        /// <summary>
        /// Capture inventory only on the Dalamud framework thread. Operation 11 is
        /// capability-gated so unmodified XIVChat clients never receive the frame.
        /// </summary>
        private void UpdateInventory() {
            if (this._inventoryWatch.Elapsed < TimeSpan.FromSeconds(1) || this._clients.IsEmpty) {
                return;
            }

            this._inventoryWatch.Restart();
            var snapshot = this.BuildInventorySnapshot();
            if (snapshot == null) {
                return;
            }

            if (this._hasInventorySnapshot && snapshot.Items.Length == 0 && this._lastInventoryFingerprint == 0) {
                return;
            }

            var fingerprint = InventoryFingerprint(snapshot.Items);
            if (this._hasInventorySnapshot && fingerprint == this._lastInventoryFingerprint) {
                return;
            }

            this._hasInventorySnapshot = true;
            this._lastInventoryFingerprint = fingerprint;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneInventorySupport);
        }

        private unsafe ServerInventory? BuildInventorySnapshot() {
            if (!XIVChatPlugin.Plugin.ClientState.IsLoggedIn || XIVChatPlugin.Plugin.ObjectTable.LocalPlayer == null) {
                return null;
            }

            try {
                var items = new List<ServerInventoryItem>();
                var containers = new List<ServerInventoryContainer>();
                var itemSheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Item>();
                var retainerManager = RetainerManager.Instance();
                var activeRetainer = retainerManager == null ? null : retainerManager->GetActiveRetainer();
                var activeRetainerId = activeRetainer == null ? 0UL : activeRetainer->RetainerId;
                var inventoryManager = InventoryManager.Instance();
                var activeRetainerGil = inventoryManager == null || activeRetainer == null ? 0u : inventoryManager->GetRetainerGil();

                foreach (var type in PhoneInventoryTypes) {
                    var containerItems = XIVChatPlugin.Plugin.GameInventory.GetInventoryItems(type).ToArray();
                    containers.Add(new ServerInventoryContainer {
                        ContainerType = (uint) type,
                        Size = containerItems.Length,
                    });
                    foreach (var item in containerItems) {
                        if (item.IsEmpty || item.ItemId == 0 || item.Quantity <= 0) {
                            continue;
                        }

                        string? name = null;
                        uint iconId = 0;
                        try {
                            var row = itemSheet.GetRowOrDefault(item.BaseItemId);
                            if (row != null) {
                                name = row.Value.Name.ExtractText();
                                iconId = row.Value.Icon;
                            }
                        } catch (Exception) {
                            // A missing row should not prevent the rest of the snapshot.
                        }

                        items.Add(new ServerInventoryItem {
                            ItemId = item.ItemId,
                            BaseItemId = item.BaseItemId,
                            Quantity = item.Quantity,
                            ContainerType = (uint) item.ContainerType,
                            InventorySlot = item.InventorySlot,
                            IsHq = item.IsHq,
                            SpiritbondOrCollectability = item.SpiritbondOrCollectability,
                            Condition = item.Condition,
                            Name = name,
                            IconId = iconId,
                            RetainerId = (uint) item.ContainerType is >= 10000 and <= 12001 ? activeRetainerId : 0,
                        });
                    }
                }

                var retainers = new List<ServerRetainer>();
                if (retainerManager != null) {
                    var activeItems = items.Where(item => item.ContainerType is >= 10000 and <= 12001).ToArray();
                    var activeCount = activeItems.Length;
                    var activeQuantity = activeItems.Sum(item => item.Quantity);
                    var retainerCount = retainerManager->GetRetainerCount();
                    for (var index = 0u; index < retainerCount; index++) {
                        var retainer = retainerManager->GetRetainerBySortedIndex(index);
                        if (retainer == null || retainer->RetainerId == 0) continue;
                        var active = retainer == activeRetainer;
                        retainers.Add(new ServerRetainer {
                            RetainerId = retainer->RetainerId,
                            Name = retainer->NameString,
                            Active = active,
                            ItemCount = active ? activeCount : 0,
                            Quantity = active ? activeQuantity : 0,
                            Gil = active ? activeRetainerGil : 0,
                            VentureId = retainer->VentureId,
                            VentureCompleteUnix = retainer->VentureComplete,
                        });
                    }
                }

                return new ServerInventory(DateTimeOffset.UtcNow.ToUnixTimeSeconds(), items.ToArray(), containers.ToArray(), retainers.ToArray());
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not capture inventory: {ex.Message}");
                return null;
            }
        }

        /// <summary>
        /// How long to wait for the game to fill in listings before giving up.
        /// Listings arrive in pages of ten, so a busy item needs several round trips;
        /// 10s leaves room for that while still bounding the client's wait.
        /// </summary>
        private static readonly TimeSpan MarketTimeout = TimeSpan.FromSeconds(10);

        /// <summary>
        /// Drives one market query at a time, in three stages across ticks:
        /// accept a request, wait for the game to populate listings, push the result.
        ///
        /// Serialised deliberately. InfoProxyItemSearch is a single shared slot that
        /// the PC's own market window also writes, so overlapping searches would
        /// return each other's rows.
        /// </summary>
        private unsafe void UpdateMarket() {
            if (this._marketAwaitingListings) {                var proxy = InfoProxyItemSearch.Instance();
                if (proxy == null) {
                    if (this._marketPurchaseRequest != null) {
                        this.FailQueuedPurchase(MarketPurchaseStatus.Timeout);
                    } else {
                        this.FinishMarket(MarketStatus.Timeout, []);
                    }
                    return;
                }

                // Phase one: confirm the request actually started. WaitingForListings is
                // not set the instant RequestData returns -- it flips on a later tick --
                // so anything read before that belongs to the PREVIOUS search. Both
                // earlier bugs came from skipping this: waiting only for the flag to go
                // false timed out (it had not gone true yet), and judging by the array
                // reported "nobody is selling" (an untouched proxy has EntryCount 0).
                if (proxy->WaitingForListings) {
                    this._marketSawWaiting = true;
                }

                if (this._marketSawWaiting && !proxy->WaitingForListings
                    && proxy->SearchItemId == this._marketPendingItemId) {
                    if (this._marketPurchaseRequest != null) {
                        this.AttemptPurchase(proxy);
                    } else {
                        this.FinishMarket(MarketStatus.Ok, this.ReadMarketListings(proxy));
                    }
                    return;
                }

                // Fallback: some replies land without the flag ever being observed true
                // (a tick can straddle both edges). Only trusted once the array is
                // self-consistent AND non-empty, so it can never manufacture a "0 rows"
                // answer -- an empty result must come from the flag path above.
                if (MarketListingsReady(proxy, this._marketPendingItemId)) {
                    if (this._marketPurchaseRequest != null) {
                        this.AttemptPurchase(proxy);
                    } else {
                        this.FinishMarket(MarketStatus.Ok, this.ReadMarketListings(proxy));
                    }
                    return;
                }

                if (this._marketWatch.Elapsed > MarketTimeout) {
                    // Log the proxy state, not just the fact of the timeout: the first
                    // version of this code timed out silently and left nothing to go on.
                    Plugin.Log.Warning(
                        $"Market query for {this._marketPendingItemId} timed out: "
                        + $"SearchItemId={proxy->SearchItemId} "
                        + $"ListingCount={proxy->ListingCount} "
                        + $"EntryCount={proxy->EntryCount} "
                        + $"WaitingForListings={proxy->WaitingForListings} "
                        + $"sawWaiting={this._marketSawWaiting}"
                    );
                    // A timed-out window usually means the throttle ate the request;
                    // give the game a full quiet cycle before the next attempt.
                    this._marketCooldownUntil = DateTimeOffset.UtcNow + TimeSpan.FromSeconds(10);
                    if (this._marketPurchaseRequest != null) {
                        this.FailQueuedPurchase(MarketPurchaseStatus.Timeout);
                    } else {
                        this.FinishMarket(MarketStatus.Timeout, []);
                    }
                }

                return;
            }

            // Dequeue the next search or purchase. Purchases go through the same
            // awaiting-listings flow because they need a fresh Listings array to verify
            // against. The purchase request is stashed so the ready-state can hand off to
            // AttemptPurchase instead of replying.
            if (DateTimeOffset.UtcNow < this._marketCooldownUntil) {
                // Inside the game's search throttle. Holding the queue here (instead of
                // dequeue-and-time-out) is what breaks the convoy: the request at the
                // head keeps its place and gets a fresh 10 s window once the gate opens.
                return;
            }

            if (this._marketPurchases.TryDequeue(out var purchase)) {
                this._marketRequester = purchase.Client;
                this._marketPendingItemId = purchase.Request.ItemId;
                this._marketPurchaseRequest = purchase.Request;
            } else if (this._marketSearches.TryDequeue(out var searchRequest)) {
                if (searchRequest.Client == Guid.Empty) {
                    this._monitorQueued = false;
                }

                this._marketRequester = searchRequest.Client;
                this._marketPendingItemId = searchRequest.ItemId;
                this._marketPurchaseRequest = null;
            } else {
                return;
            }

            var requestedItemId = this._marketPendingItemId;

            var refusal = this.MarketRefusalReason(requestedItemId);
            if (refusal != MarketStatus.Ok) {
                // A purchase refused on preconditions has to answer on the purchase
                // channel; FinishMarket would send a ServerMarket the phone is not
                // waiting for and leave the buy button spinning.
                if (this._marketPurchaseRequest != null) {
                    this.FailQueuedPurchase((MarketPurchaseStatus) refusal);
                } else {
                    this.FinishMarket(refusal, []);
                }
                return;
            }

            var search = InfoProxyItemSearch.Instance();
            if (search == null) {
                if (this._marketPurchaseRequest != null) {
                    this.FailQueuedPurchase(MarketPurchaseStatus.Timeout);
                } else {
                    this.FinishMarket(MarketStatus.Timeout, []);
                }
                return;
            }

            try {
                // Remember what the player had searched on the PC so the query does
                // not silently wipe their own search box.
                this._marketSavedSearchItemId = search->SearchItemId;
                search->EndRequest();
                search->SearchItemId = requestedItemId;
                if (!search->RequestData()) {
                    // Refused outright by the game -- distinct from waiting and never
                    // being answered, and previously indistinguishable in the log.
                    Plugin.Log.Warning(
                        $"Market RequestData refused for {requestedItemId}"
                    );
                    if (this._marketPurchaseRequest != null) {
                        this.FailQueuedPurchase(MarketPurchaseStatus.Timeout);
                    } else {
                        this.FinishMarket(MarketStatus.Timeout, []);
                    }
                    return;
                }

                // The game accepted the call; its internal throttle needs ~7 s before
                // the next one, and a timeout deserves a longer breather so the next
                // queued request starts from a clean window instead of another convoy.
                this._marketCooldownUntil = DateTimeOffset.UtcNow + TimeSpan.FromSeconds(8);
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not request market data: {ex.Message}");
                if (this._marketPurchaseRequest != null) {
                    this.FailQueuedPurchase(MarketPurchaseStatus.Timeout);
                } else {
                    this.FinishMarket(MarketStatus.Timeout, []);
                }
                return;
            }

            this._marketAwaitingListings = true;
            this._marketSawWaiting = false;
            this._marketWatch.Restart();
        }

        /// <summary>
        /// True when the proxy visibly holds rows for <paramref name="itemId"/>.
        ///
        /// Secondary signal only -- <see cref="UpdateMarket"/> prefers the
        /// WaitingForListings edge. Deliberately cannot return true for an empty
        /// result: "nobody is selling" and "the reply has not arrived" look identical
        /// in this struct, and reporting the first when it is really the second is the
        /// bug this guard exists to prevent. An untouched proxy has ListingCount 0 and
        /// EntryCount 0, which is exactly what a genuinely empty board also looks like.
        ///
        /// Shape of the check follows BetterMarketBoard: every priced row must belong to
        /// the current search, and their number must equal <c>ListingCount</c>. Listings
        /// arrive in pages of ten, so <c>EntryCount</c> decides how full "full" is.
        /// </summary>
        private static unsafe bool MarketListingsReady(InfoProxyItemSearch* proxy, uint itemId) {
            if (proxy->SearchItemId != itemId || proxy->ListingCount == 0) {
                return false;
            }

            // Scan only ListingCount rows, not the whole array. Listings.Length is the
            // fixed capacity, and ClearListData resets the counts without wiping the
            // rows, so everything past ListingCount is still the PREVIOUS search's
            // data. Scanning the full span made every query after the first fail on
            // the foreign-item check below and time out.
            var listings = proxy->Listings;
            var upTo = Math.Min((int) proxy->ListingCount, listings.Length);
            var priced = 0;
            for (var i = 0; i < upTo; i++) {
                var listing = listings[i];
                if (listing.UnitPrice == 0) {
                    continue;
                }

                // A row for another item means the array still holds the previous
                // search; not ready, and reading now would mix results.
                if (listing.ItemId != itemId) {
                    return false;
                }

                priced++;
            }

            if (priced != upTo) {
                return false;
            }

            return proxy->EntryCount <= 10 || proxy->ListingCount >= 10;
        }

        /// <summary>
        /// Conditions under which the game will not serve a market query. Mirrors the
        /// gate the BetterMarketBoard module uses, for the same reason: the native
        /// board window and this code share one InfoProxyItemSearch slot.
        /// </summary>
        private unsafe MarketStatus MarketRefusalReason(uint itemId) {
            if (!Plugin.ClientState.IsLoggedIn || Plugin.ObjectTable.LocalPlayer == null) {
                return MarketStatus.NotLoggedIn;
            }

            var gameMain = GameMain.Instance();
            if (gameMain == null || gameMain->CurrentContentFinderConditionId != 0) {
                return MarketStatus.InDuty;
            }

            if (IsMarketBoardOpen()) {
                return MarketStatus.BoardOpen;
            }

            if (!Plugin.DataManager.GetExcelSheet<Item>().TryGetRow(itemId, out var item)
                || item.ItemSearchCategory.RowId == 0) {
                return MarketStatus.NotMarketable;
            }

            return MarketStatus.Ok;
        }

        /// <summary>
        /// True when the player has the market board result window up on the PC.
        /// Checked via the addon manager because this plugin does not take IGameGui.
        /// </summary>
        private static unsafe bool IsMarketBoardOpen() {
            try {
                var manager = RaptureAtkUnitManager.Instance();
                if (manager == null) {
                    return false;
                }

                var addon = manager->GetAddonByName("ItemSearchResult");
                return addon != null && addon->IsVisible;
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not check market board state: {ex.Message}");
                return false;
            }
        }

        /// <summary>
        /// Retainer city for a listing, or empty when the id is unknown.
        /// </summary>
        private static string TownName(byte townId) {
            if (townId == 0) {
                return string.Empty;
            }

            return Plugin.DataManager.GetExcelSheet<Town>().GetRowOrDefault(townId)
                ?.Name.ExtractText() ?? string.Empty;
        }

        /// <summary>
        /// Copies listings out of the proxy.
        ///
        /// No per-listing world: <c>MarketBoardListing</c> has no world field. The
        /// game only exposes the world name through the ItemSearchResult UI string
        /// array, and this path deliberately runs with that window closed, so there
        /// is nothing trustworthy to read. The reply labels the whole result with the
        /// character's current world instead, and cross-world comparison stays on the
        /// Universalis path where world is a real field.
        ///
        /// No retainer name either, and it is not an oversight: the only string on
        /// <c>MarketBoardListing</c> is <c>CharacterName</c>, which FFXIVClientStructs
        /// documents as "only populated when item is being sold as a set". It is blank
        /// for ordinary listings, so reading it produced an always-empty field. The
        /// city (<c>TownId</c>) is real for every listing and is what buyers can act
        /// on, so it takes that slot. Universalis does carry retainer names -- the
        /// cross-world rows still show them.
        /// </summary>
        private unsafe ServerMarketListing[] ReadMarketListings(InfoProxyItemSearch* proxy) {
            var rows = new List<ServerMarketListing>();
            var count = Math.Min((int) proxy->ListingCount, proxy->Listings.Length);
            for (var i = 0; i < count; i++) {
                var listing = proxy->Listings[i];
                if (listing.UnitPrice == 0) {
                    continue;
                }

                // ClearListData leaves stale rows in place, so a row that belongs to
                // another item means we are reading too early. Drop it rather than
                // reporting another item's price for this one.
                if (listing.ItemId != this._marketPendingItemId) {
                    continue;
                }

                rows.Add(new ServerMarketListing {
                    ListingId = listing.ListingId,
                    UnitPrice = listing.UnitPrice,
                    Quantity = listing.Quantity,
                    IsHq = listing.IsHqItem,
                    TownName = TownName(listing.TownId),
                    Tax = listing.TotalTax,
                    IsSet = listing.IsSellingAsSet,
                    MateriaCount = listing.MateriaCount,
                });
            }

            return rows.ToArray();
        }

        /// <summary>
        /// Sends the reply, restores the player's own search, and clears the slot so
        /// the next queued request can run.
        /// </summary>
        private unsafe void FinishMarket(MarketStatus status, ServerMarketListing[] listings) {
            var itemId = this._marketPendingItemId;
            var requester = this._marketRequester;

            this._marketAwaitingListings = false;
            this._marketSawWaiting = false;
            this._marketPendingItemId = 0;
            this._marketRequester = Guid.Empty;
            this._marketWatch.Reset();

            try {
                var proxy = InfoProxyItemSearch.Instance();
                if (proxy != null) {
                    proxy->SearchItemId = this._marketSavedSearchItemId;
                    if (this._marketSavedSearchItemId == 0) {
                        proxy->ClearListData();
                    }
                }
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not restore market search state: {ex.Message}");
            }

            this._marketSavedSearchItemId = 0;

            // A Guid.Empty requester is the price monitor, not a phone: its results
            // feed the monitor rules instead of any client queue.
            if (requester == Guid.Empty) {
                this.ProcessMonitorResult(itemId, status, listings);
                return;
            }

            var world = Plugin.ObjectTable.LocalPlayer?.CurrentWorld.ValueNullable?.Name.ExtractText()
                ?? string.Empty;
            var message = new ServerMarket(
                DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(), itemId, status, listings, world,
                this.NpcGilPrice(itemId));
            Plugin.Log.Info(
                $"Market reply: item {itemId} status {status} rows {listings.Length} npc {message.NpcPrice}");

            // Reply only to the phone that asked; an unsolicited market push would be
            // meaningless to the other clients.
            if (this.Clients.TryGetValue(requester, out var client)) {
                client.Queue.Writer.TryWrite(message);
            }
        }

        /// <summary>
        /// What NPC shops charge for <paramref name="itemId"/> in gil, 0 when none does.
        ///
        /// This is <c>Item.PriceMid</c>, the same column BetterMarketBoard reads out
        /// of GilShop entries, so it matches what the player would actually pay at a
        /// vendor. Items no shop sells carry 0, and the phone hides the benchmark line.
        /// </summary>
        private uint NpcGilPrice(uint itemId) {
            try {
                // Excel sheets can become ready a little after plugin construction. Repair
                // an empty startup cache lazily instead of treating every item as unavailable
                // for the rest of the game session.
                _marketCache.EnsureGilShopData();
                // GilShopItem is the authoritative availability gate. Item.PriceMid by
                // itself is also populated for items that are not directly purchasable, so
                // never expose it unless this item occurs in a real gil vendor shop.
                if (!_marketCache.GilShopItemIds.Contains(itemId)) {
                    return 0;
                }

                // Only return PriceMid if item is actually sold in a Gil shop
                if (Plugin.DataManager.GetExcelSheet<Item>().TryGetRow(itemId, out var item)) {
                    return item.PriceMid > 0 ? item.PriceMid : 0;
                }

                return 0;
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not read NPC price for {itemId}: {ex.Message}");
                return 0;
            }
        }

        // ---- price monitor ----
        //
        // The phone pushes rules (ClientMarketMonitorSync); this side re-queries the
        // board for each rule on a timer and acts on the result. The design copies
        // BetterMarketBoard's: drive the game's own InfoProxyItemSearch on a slow
        // cadence, never craft packets, and reuse the exact same single-flight slot
        // as the manual queries so a monitor poll can never interleave with a phone
        // search or purchase. Guid.Empty as the client id marks monitor traffic.

        /// <summary>How often one item is re-checked, matching BetterMarketBoard.</summary>
        private static readonly TimeSpan MonitorInterval = TimeSpan.FromSeconds(60);

        /// <summary>A standing cheap listing must not re-notify (or re-buy) every minute.</summary>
        private static readonly TimeSpan MonitorRepeatGuard = TimeSpan.FromMinutes(30);

        private int _monitorCursor;
        private DateTimeOffset _lastMonitorPoll = DateTimeOffset.MinValue;

        /// <summary>
        /// True while a monitor-driven query sits in the queue. At most one monitor
        /// request may wait at a time: the queue is shared with the phone's manual
        /// queries, and a backlog of monitor polls starves them (the convoy in the
        /// logs). Manual traffic also outranks it -- see the enqueue conditions.
        /// </summary>
        private bool _monitorQueued;

        /// <summary>
        /// Cached category tree.  The Item sheets are static in-session, but the
        /// GilShopItem subrow sheet can become available a little later than them.  Keep
        /// the vendor-index timestamp beside the tree so an early all-zero snapshot is
        /// replaced as soon as real NPC prices are ready.
        /// </summary>
        private ServerMarketCategories? _marketCategoriesCache;
        private long _marketCategoriesGilShopTimestamp;
        private readonly object _marketCategoriesGate = new();

        private List<MarketMonitorConfig> MonitorRules => this._plugin.Config.MarketMonitors;

        /// <summary>
        /// True while any monitor request could still mutate shared purchase state:
        /// used to keep a fresh phone-initiated purchase from colliding with one the
        /// monitor just queued.
        /// </summary>
        private bool MonitorBusy =>
            this._marketAwaitingListings || this._marketAwaitingPurchaseReply
            || this._marketPurchaseRequest != null;

        /// <summary>
        /// Enqueues at most one board query per interval, round-robin over the rules.
        /// Runs before UpdateMarket in the tick so the request joins the queue behind
        /// anything the phone asked for: manual traffic always wins the slot.
        /// </summary>
        private unsafe void UpdateMarketMonitor() {
            var rules = this.MonitorRules;
            if (rules.Count == 0) {
                return;
            }

            // Manual traffic first: never enqueue while the phone has a search or a
            // purchase pending, never queue a second monitor request, and never run
            // inside the game's search throttle. The monitor can always afford to
            // wait -- the phone's query cannot.
            if (this.MonitorBusy || this._monitorQueued
                || !this._marketSearches.IsEmpty || !this._marketPurchases.IsEmpty
                || DateTimeOffset.UtcNow < this._marketCooldownUntil
                || DateTimeOffset.UtcNow - this._lastMonitorPoll < MonitorInterval) {
                return;
            }

            // Same gates the manual path enforces, checked up front so a monitor poll
            // never even queues while the game cannot answer (duty, board open, logout).
            if (Plugin.ObjectTable.LocalPlayer == null || !Plugin.ClientState.IsLoggedIn) {
                return;
            }

            if (IsMarketBoardOpen()) {
                return;
            }

            var gameMain = GameMain.Instance();
            if (gameMain == null || gameMain->CurrentContentFinderConditionId != 0) {
                return;
            }

            // Round-robin; skip rules whose item is no longer tradable so a stale rule
            // does not consume the whole cadence.
            for (var attempt = 0; attempt < rules.Count; attempt++) {
                this._monitorCursor = (this._monitorCursor + 1) % rules.Count;
                var rule = rules[this._monitorCursor];
                var marketable = false;
                try {
                    marketable = Plugin.DataManager.GetExcelSheet<Item>()
                        .TryGetRow(rule.ItemId, out var item) && item.ItemSearchCategory.RowId != 0;
                } catch {
                    marketable = false;
                }

                if (marketable && rule.PriceThreshold > 0) {
                    this._lastMonitorPoll = DateTimeOffset.UtcNow;
                    this._monitorQueued = true;
                    Plugin.Log.Info($"Monitor poll: item {rule.ItemId} (threshold {rule.PriceThreshold})");
                    this._marketSearches.Enqueue((Guid.Empty, rule.ItemId, rule.HqOnly));
                    return;
                }
            }
        }

        /// <summary>
        /// A board read for a monitor rule finished. Decides whether to notify and
        /// whether to auto-buy, then routes a purchase through the normal queue.
        /// </summary>
        private void ProcessMonitorResult(uint itemId, MarketStatus status, ServerMarketListing[] listings) {
            // The dequeue clears this first; clearing again here is a cheap guarantee
            // that an early return can never strand the flag and silence the monitor.
            this._monitorQueued = false;
            var rule = this.MonitorRules.FirstOrDefault(r => r.ItemId == itemId);
            if (rule == null) {
                return;
            }

            if (status != MarketStatus.Ok) {
                Plugin.Log.Info($"Monitor query for {itemId} returned {status}; will retry next cycle");
                return;
            }

            var matches = listings
                .Where(l => l.UnitPrice > 0 && l.UnitPrice <= rule.PriceThreshold
                            && (!rule.HqOnly || l.IsHq))
                .OrderBy(l => l.UnitPrice)
                .ToArray();

            if (matches.Length == 0) {
                return;
            }

            var cheapest = matches[0];
            var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            var repeat =
                rule.LastFirePrice == cheapest.UnitPrice
                && now - rule.LastFireMs < (long) MonitorRepeatGuard.TotalMilliseconds;

            if (rule.AutoBuy) {
                // Auto-buy has its own, shorter repeat guard: after a failed attempt the
                // same listing is still there, and retrying once a minute for a buy the
                // game keeps refusing helps nobody. A changed price resets the guard.
                var buyRepeat = repeat || now - rule.LastFireMs < TimeSpan.FromMinutes(10).TotalMilliseconds;
                if (!buyRepeat) {
                    this.FireMonitor(rule, cheapest.UnitPrice, cheapest.Quantity,
                        MarketMonitorEventKind.Found, $"{matches.Length} 条低于 {rule.PriceThreshold}");

                    if (rule.BuyCap > 0 && rule.BoughtQty >= rule.BuyCap) {
                        this.FireMonitor(rule, cheapest.UnitPrice, cheapest.Quantity,
                            MarketMonitorEventKind.CapReached,
                            $"已达自动购买上限 {rule.BuyCap} 件");
                    } else if (!this._plugin.Config.AllowMonitorAutoBuy) {
                        this.FireMonitor(rule, cheapest.UnitPrice, cheapest.Quantity,
                            MarketMonitorEventKind.BuyFailed, "插件设置里自动购买已关闭");
                    } else {
                        // The slot is free by construction here: FinishMarket just ran on
                        // the same single-flight state this reads.
                        this._marketPurchases.Enqueue((Guid.Empty, new ClientMarketPurchase {
                            ItemId = rule.ItemId,
                            ListingId = cheapest.ListingId,
                            ExpectedUnitPrice = cheapest.UnitPrice,
                            ExpectedQuantity = cheapest.Quantity,
                            ExpectedHq = cheapest.IsHq,
                        }));
                        Plugin.Log.Info(
                            $"Monitor auto-buy queued: item {itemId} listing {cheapest.ListingId} "
                            + $"at {cheapest.UnitPrice} gil"
                        );
                    }
                }
            } else if (!repeat) {
                this.FireMonitor(rule, cheapest.UnitPrice, cheapest.Quantity,
                    MarketMonitorEventKind.Found, $"{matches.Length} 条低于 {rule.PriceThreshold}");
            }
        }

        /// <summary>
        /// The monitor's own purchase attempt finished. Updates the bought-quantity
        /// budget and broadcasts the outcome.
        /// </summary>
        private void ProcessMonitorPurchaseResult(uint itemId, MarketPurchaseStatus status,
            uint price, uint quantity, uint tax, uint errorId) {
            var rule = this.MonitorRules.FirstOrDefault(r => r.ItemId == itemId);
            if (rule == null) {
                return;
            }

            if (status == MarketPurchaseStatus.Ok) {
                rule.BoughtQty += quantity;
                rule.LastFireMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                rule.LastFirePrice = price;
                this._plugin.Config.Save();

                this.BroadcastMonitorEvent(new ServerMarketMonitorEvent(
                    itemId, MarketMonitorEventKind.Purchased, price, quantity,
                    $"自动买入 {quantity} 件，单价 {price} gil（含税 {tax}）"));
                Plugin.Log.Info(
                    $"Monitor bought {quantity}x item {itemId} at {price} gil/unit (tax {tax})");
                return;
            }

            if (status == MarketPurchaseStatus.Busy) {
                // Normal while a phone purchase holds the slot; not worth notifying.
                return;
            }

            this.FireMonitor(rule, price, quantity, MarketMonitorEventKind.BuyFailed,
                $"自动购买未成功：{status}（错误码 {errorId}）");
        }

        /// <summary>
        /// Records the fire and pushes the event to every connected client, whether
        /// or not a phone is watching: the notification only matters when one is, but
        /// the log line always is.
        /// </summary>
        private void FireMonitor(MarketMonitorConfig rule, uint price, uint quantity,
            MarketMonitorEventKind kind, string detail) {
            rule.LastFireMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            rule.LastFirePrice = price;
            this._plugin.Config.Save();
            this.BroadcastMonitorEvent(new ServerMarketMonitorEvent(
                rule.ItemId, kind, price, quantity, detail));
        }

        private void BroadcastMonitorEvent(ServerMarketMonitorEvent message) {
            Plugin.Log.Info($"Monitor event: item {message.ItemId} {message.Kind} {message.Detail}");
            foreach (var client in this._clients.Values) {
                client.Queue.Writer.TryWrite(message);
            }
        }

        /// <summary>
        /// Replaces the monitor list from the phone. Bookkeeping (bought budget, last
        /// fire) follows the item id across the replace so re-saving an unchanged rule
        /// does not reset its cap or guards.
        /// </summary>
        private void HandleMonitorSync(ClientMarketMonitorSync sync) {
            var old = this.MonitorRules.ToDictionary(r => r.ItemId);
            var rules = sync.Entries.Select(e => {
                if (old.TryGetValue(e.ItemId, out var prev)) {
                    prev.PriceThreshold = e.PriceThreshold;
                    prev.HqOnly = e.HqOnly;
                    prev.AutoBuy = e.AutoBuy;
                    prev.BuyCap = e.BuyCap;
                    return prev;
                }

                return new MarketMonitorConfig {
                    ItemId = e.ItemId,
                    PriceThreshold = e.PriceThreshold,
                    HqOnly = e.HqOnly,
                    AutoBuy = e.AutoBuy,
                    BuyCap = e.BuyCap,
                };
            }).ToList();

            this._plugin.Config.MarketMonitors = rules;
            this._plugin.Config.Save();
            this._monitorCursor = 0;
            this._lastMonitorPoll = DateTimeOffset.MinValue; // let the first check run now

            Plugin.Log.Info($"Monitor list replaced: {rules.Count} rule(s)");
            this.BroadcastMonitorEvent(new ServerMarketMonitorEvent(
                0, MarketMonitorEventKind.Sync, 0, 0, $"{rules.Count} 条监控规则已同步"));
        }

        /// <summary>
        /// Abandons the confirming re-query for a queued purchase and answers on the
        /// purchase channel. Clears the shared market slot the same way FinishMarket
        /// does, minus the reply.
        /// </summary>
        private unsafe void FailQueuedPurchase(MarketPurchaseStatus status) {
            var req = this._marketPurchaseRequest;
            var requester = this._marketRequester;

            this._marketAwaitingListings = false;
            this._marketSawWaiting = false;
            this._marketPendingItemId = 0;
            this._marketRequester = Guid.Empty;
            this._marketWatch.Reset();
            this._marketPurchaseRequest = null;

            try {
                var proxy = InfoProxyItemSearch.Instance();
                if (proxy != null) {
                    proxy->SearchItemId = this._marketSavedSearchItemId;
                    if (this._marketSavedSearchItemId == 0) {
                        proxy->ClearListData();
                    }
                }
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not restore market search state: {ex.Message}");
            }

            this._marketSavedSearchItemId = 0;

            this.FinishPurchase(requester, req?.ItemId ?? 0, req?.ListingId ?? 0, status);
        }

        /// <summary>
        /// Listings are ready and this request is a purchase: find the target listing,
        /// verify it still matches what the phone showed, and send the packet if it does.
        /// </summary>
        private unsafe void AttemptPurchase(InfoProxyItemSearch* proxy) {
            var req = this._marketPurchaseRequest!;
            var itemId = req.ItemId;
            var listingId = req.ListingId;
            var requester = this._marketRequester;

            // Clear the search state now so the slot is free for the next request, then
            // enter purchase-reply wait. The purchase half has its own state machine
            // because success can only be reported from the callback, not from here.
            this._marketAwaitingListings = false;
            this._marketSawWaiting = false;
            this._marketPendingItemId = 0;
            this._marketRequester = Guid.Empty;
            this._marketWatch.Reset();
            this._marketPurchaseRequest = null;

            // Listings is a Span over memory inside the proxy itself, so &span[i] is
            // rejected as unfixed even though the target never moves. Take the address
            // through the span's own reference instead of copying the row out: the game
            // stores the pointer it is handed, so it has to be the real one.
            MarketBoardListing* found = null;
            var listings = proxy->Listings;
            var count = Math.Min((int) proxy->ListingCount, listings.Length);
            for (var i = 0; i < count; i++) {
                if (listings[i].ListingId == listingId) {
                    found = (MarketBoardListing*) Unsafe.AsPointer(ref listings[i]);
                    break;
                }
            }

            if (found == null) {
                this.FinishPurchase(requester, itemId, listingId, MarketPurchaseStatus.ListingGone);
                return;
            }

            if (found->UnitPrice != req.ExpectedUnitPrice || found->Quantity != req.ExpectedQuantity
                || found->IsHqItem != req.ExpectedHq) {
                this.FinishPurchase(requester, itemId, listingId, MarketPurchaseStatus.Changed);
                return;
            }

            var manager = InventoryManager.Instance();
            if (manager == null) {
                this.FinishPurchase(requester, itemId, listingId, MarketPurchaseStatus.Timeout);
                return;
            }

            var gil = manager->GetGil();
            var cost = (ulong) found->UnitPrice * found->Quantity + found->TotalTax;
            if (gil < cost) {
                this.FinishPurchase(requester, itemId, listingId, MarketPurchaseStatus.NotEnoughGil);
                return;
            }

            this._marketPurchaseRequester = requester;
            this._marketPurchaseItemId = itemId;
            this._marketPurchaseListingId = listingId;
            this._marketPurchasePrice = found->UnitPrice;
            this._marketPurchaseQuantity = found->Quantity;
            this._marketPurchaseTax = found->TotalTax;

            if (this._purchaseResponseHook == null) {
                // Member function, not a vtable slot -- the vtable only carries the
                // InfoProxyInterface virtuals.
                var addr = (nint) InfoProxyItemSearch.MemberFunctionPointers.ProcessPurchaseResponse;
                this._purchaseResponseHook = Plugin.GameInteropProvider.HookFromAddress<PurchaseResponseDelegate>(
                    addr, this.OnPurchaseResponse
                );
                this._purchaseResponseHook.Enable();
            }

            try {
                if (!proxy->SetLastPurchasedItem(found)) {
                    this.FinishPurchase(requester, itemId, listingId, MarketPurchaseStatus.Refused, 0);
                    return;
                }

                if (!proxy->SendPurchaseRequestPacket()) {
                    this.FinishPurchase(requester, itemId, listingId, MarketPurchaseStatus.Refused, 0);
                    return;
                }
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not send purchase packet: {ex.Message}");
                this.FinishPurchase(requester, itemId, listingId, MarketPurchaseStatus.Refused, 0);
                return;
            }

            this._marketAwaitingPurchaseReply = true;
            this._marketPurchaseWatch.Restart();
        }

        /// <summary>
        /// Called by the game when the server replies to a market purchase. The only
        /// reliable success signal: SendPurchaseRequestPacket succeeding locally means
        /// nothing if the server rejected it.
        /// </summary>
        private unsafe void OnPurchaseResponse(InfoProxyItemSearch* proxy, uint itemId, uint errorId) {
            try {
                this._purchaseResponseHook?.Original(proxy, itemId, errorId);
            } catch {
                // Calling the original is courtesy, not a requirement; if it throws the
                // purchase still went through (or didn't) and the phone still needs the
                // result.
            }

            if (!this._marketAwaitingPurchaseReply || itemId != this._marketPurchaseItemId) {
                return;
            }

            var requester = this._marketPurchaseRequester;
            var listingId = this._marketPurchaseListingId;

            if (errorId == 0) {
                var price = this._marketPurchasePrice;
                var quantity = this._marketPurchaseQuantity;
                var tax = this._marketPurchaseTax;
                this.FinishPurchase(requester, itemId, listingId, MarketPurchaseStatus.Ok,
                    errorId, price, quantity, tax);
            } else {
                this.FinishPurchase(requester, itemId, listingId, MarketPurchaseStatus.Refused, errorId);
            }
        }

        /// <summary>
        /// Drives the purchase-reply wait and its timeout. Called every tick once a
        /// purchase packet is away.
        /// </summary>
        private void UpdateMarketPurchase() {
            if (!this._marketAwaitingPurchaseReply) {
                return;
            }

            if (this._marketPurchaseWatch.Elapsed > MarketTimeout) {
                Plugin.Log.Warning(
                    $"Market purchase for item {this._marketPurchaseItemId} "
                    + $"listing {this._marketPurchaseListingId} timed out waiting for server reply"
                );
                this.FinishPurchase(
                    this._marketPurchaseRequester,
                    this._marketPurchaseItemId,
                    this._marketPurchaseListingId,
                    MarketPurchaseStatus.Timeout
                );
            }
        }

        /// <summary>
        /// Refuses a purchase without touching the shared purchase state. Cannot go
        /// through FinishPurchase: a Busy refusal arrives while another purchase is in
        /// flight, and clearing the state would strand that one with no reply.
        /// </summary>
        private void RefusePurchase(Guid requester, ClientMarketPurchase request,
            MarketPurchaseStatus status) {
            // The monitor's queued buy can land here (Busy while a manual one runs,
            // Disabled when purchases are off). Answer on the monitor channel instead.
            if (requester == Guid.Empty) {
                this.ProcessMonitorPurchaseResult(
                    request.ItemId, status, 0, 0, 0, 0);
                return;
            }

            var message = new ServerMarketPurchase(
                DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                request.ItemId, status, request.ListingId, 0, 0, 0, 0
            );

            var delivered = this.Clients.TryGetValue(requester, out var client)
                            && client.Queue.Writer.TryWrite(message);
            Plugin.Log.Info(
                $"Market purchase refused: item {request.ItemId} "
                + $"listing {request.ListingId} status {status} delivered={delivered}"
            );
        }

        /// <summary>
        /// Sends the purchase result and clears the purchase wait state.
        /// </summary>
        private void FinishPurchase(Guid requester, uint itemId, ulong listingId,
            MarketPurchaseStatus status, uint errorId = 0, uint price = 0, uint quantity = 0, uint tax = 0) {
            this._marketAwaitingPurchaseReply = false;
            this._marketPurchaseRequester = Guid.Empty;
            this._marketPurchaseItemId = 0;
            this._marketPurchaseListingId = 0;
            this._marketPurchasePrice = 0;
            this._marketPurchaseQuantity = 0;
            this._marketPurchaseTax = 0;
            this._marketPurchaseWatch.Reset();

            var message = new ServerMarketPurchase(
                DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                itemId, status, listingId, price, quantity, tax, errorId
            );

            // Guid.Empty requester = the price monitor's own auto-buy; its outcome
            // becomes a monitor event rather than a reply to a phone.
            if (requester == Guid.Empty) {
                this.ProcessMonitorPurchaseResult(itemId, status, price, quantity, tax, errorId);
                return;
            }

            if (this.Clients.TryGetValue(requester, out var client)) {
                client.Queue.Writer.TryWrite(message);
            }

            // Log every outcome, including refusals: a status that reached the phone and
            // a reply that was never sent are otherwise indistinguishable afterwards.
            if (status == MarketPurchaseStatus.Ok) {
                Plugin.Log.Info(
                    $"Purchased {quantity}x item {itemId} at {price} gil/unit (tax {tax}) via phone"
                );
            } else {
                Plugin.Log.Info(
                    $"Market purchase reply: item {itemId} listing {listingId} "
                    + $"status {status} errorId {errorId} "
                    + $"delivered={requester != Guid.Empty && this.Clients.ContainsKey(requester)}"
                );
            }
        }

        private void UpdateWallet() {
            if (this._walletWatch.Elapsed < TimeSpan.FromSeconds(1) || this._clients.IsEmpty) {
                return;
            }

            this._walletWatch.Restart();
            var snapshot = this.BuildWalletSnapshot();
            if (snapshot == null) {
                return;
            }

            var fingerprint = WalletFingerprint(snapshot);
            if (this._hasWalletSnapshot && fingerprint == this._lastWalletFingerprint) {
                return;
            }

            this._hasWalletSnapshot = true;
            this._lastWalletFingerprint = fingerprint;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneWalletSupport);
        }

        private unsafe ServerWallet? BuildWalletSnapshot() {
            if (XIVChatPlugin.Plugin.ObjectTable.LocalPlayer == null) {
                return null;
            }

            try {
                var manager = InventoryManager.Instance();
                if (manager == null) {
                    return null;
                }

                var entries = new List<ServerWalletEntry>();
                var itemSheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Item>();
                var added = new HashSet<uint>();

                void Add(uint itemId, long cap, string section, bool tomestone = false) {
                    try {
                        if (!added.Add(itemId)) {
                            return;
                        }

                        var row = itemSheet.GetRowOrDefault(itemId);
                        if (row == null) {
                            return;
                        }

                        var amount = tomestone
                            ? (long) manager->GetTomestoneCount(itemId)
                            : (long) manager->GetInventoryItemCount(itemId, false, true, true, 0);
                        entries.Add(new ServerWalletEntry {
                            ItemId = itemId,
                            IconId = row.Value.Icon,
                            Name = row.Value.Name.ExtractText(),
                            Amount = amount,
                            Cap = cap,
                            Section = section,
                        });
                    } catch (Exception ex) {
                        Plugin.Log.Warning($"Wallet item {itemId} failed: {ex.Message}");
                    }
                }

                Add(29, 0, "常用货币");
                Add(21072, 0, "常用货币");
                var playerState = PlayerState.Instance();
                var sealId = playerState == null ? 0u : playerState->GrandCompany switch {
                    1 => 20u,
                    2 => 21u,
                    3 => 22u,
                    _ => 0u,
                };
                if (sealId != 0) Add(sealId, 0, "常用货币");

                Add(27, 4000, "狩猎票据");
                Add(10307, 4000, "狩猎票据");
                Add(26533, 4000, "狩猎票据");

                var tomestones = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<TomestonesItem>()
                    .Select(row => row.Item.RowId)
                    .Where(id => id != 0 && id != 28)
                    .Distinct()
                    .OrderByDescending(id => id)
                    .Take(2)
                    .ToList();
                tomestones.Add(28);
                foreach (var itemId in tomestones) Add(itemId, 2000, "亚拉戈神典石", true);

                Add(25, 20000, "对战货币");
                Add(36656, 20000, "对战货币");
                Add(33913, 4000, "生产采集票据");
                Add(33914, 4000, "生产采集票据");
                Add(41784, 4000, "生产采集票据");
                Add(41785, 4000, "生产采集票据");
                Add(28063, 10000, "生产采集票据");
                Add(26807, 1500, "其他货币");

                return new ServerWallet(DateTimeOffset.UtcNow.ToUnixTimeSeconds(), (long) manager->GetGil(), entries.ToArray());
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not capture wallet: {ex.Message}");
                return null;
            }
        }

        private static long WalletFingerprint(ServerWallet wallet) {
            unchecked {
                long hash = wallet.Gil;
                foreach (var entry in wallet.Entries) {
                    hash = hash * 31 + entry.ItemId;
                    hash = hash * 31 + entry.Amount;
                }
                return hash;
            }
        }

        private void UpdateWeather() {
            if (this._weatherWatch.Elapsed < TimeSpan.FromSeconds(2) || this._clients.IsEmpty) {
                return;
            }

            this._weatherWatch.Restart();
            var snapshot = this.BuildWeatherSnapshot();
            if (snapshot == null) {
                return;
            }

            var fingerprint = WeatherFingerprint(snapshot);
            if (this._hasWeatherSnapshot && fingerprint == this._lastWeatherFingerprint) {
                return;
            }

            this._hasWeatherSnapshot = true;
            this._lastWeatherFingerprint = fingerprint;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneWeatherSupport);
        }

        private unsafe ServerWeather? BuildWeatherSnapshot() {
            var territoryId = XIVChatPlugin.Plugin.ClientState.TerritoryType;
            if (territoryId == 0) {
                return null;
            }

            try {
                var territory = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<TerritoryType>().GetRowOrDefault(territoryId);
                if (territory == null || territory.Value.WeatherRate.RowId == 0) {
                    return null;
                }

                var zone = territory.Value.PlaceName.IsValid ? territory.Value.PlaceName.Value.Name.ExtractText() : "未知区域";
                var rate = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<WeatherRate>().GetRowOrDefault(territory.Value.WeatherRate.RowId);
                if (rate == null) {
                    return null;
                }

                var chances = new List<(byte Id, int Cumulative)>();
                var cumulative = 0;
                var rates = rate.Value.Rate;
                var weathers = rate.Value.Weather;
                for (var index = 0; index < rates.Count && index < weathers.Count; index++) {
                    var id = (byte) weathers[index].RowId;
                    var chance = rates[index];
                    if (id == 0 || chance <= 0) continue;
                    cumulative += chance;
                    chances.Add((id, cumulative));
                }

                if (chances.Count == 0) return null;

                byte Resolve(uint target) {
                    foreach (var chance in chances) {
                        if (target < chance.Cumulative) return chance.Id;
                    }
                    return chances[^1].Id;
                }

                string Name(byte id) => XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Weather>().GetRowOrDefault(id)?.Name.ExtractText() ?? "未知天气";
                var currentId = (byte) 0;
                var environment = EnvManager.Instance();
                if (environment != null) currentId = environment->ActiveWeather;
                var current = currentId == 0 ? Name(Resolve(ForecastTarget(DateTimeOffset.UtcNow.ToUnixTimeSeconds()))) : Name(currentId);

                const long windowSeconds = 1400;
                const long hourSeconds = 175;
                var now = DateTimeOffset.UtcNow.ToUnixTimeSeconds();
                var start = now - now % windowSeconds;
                var forecast = new List<ServerWeatherWindow>(5);
                for (var index = 0; index < 5; index++) {
                    var timestamp = start + index * windowSeconds;
                    var id = index == 0 && currentId != 0 ? currentId : Resolve(ForecastTarget(timestamp));
                    forecast.Add(new ServerWeatherWindow {
                        Name = Name(id),
                        MinutesFromNow = (int) ((timestamp - now) / 60),
                        EorzeaBell = (int) (timestamp / hourSeconds % 24),
                    });
                }

                return new ServerWeather(DateTimeOffset.UtcNow.ToUnixTimeSeconds(), zone, current, forecast.ToArray());
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not capture weather: {ex.Message}");
                return null;
            }
        }

        private static uint ForecastTarget(long unixSeconds) {
            const long hourSeconds = 175;
            const long daySeconds = 4200;
            var eorzeaHour = unixSeconds / hourSeconds;
            var increment = (uint) ((eorzeaHour + 8 - eorzeaHour % 8) % 24);
            var totalDays = (uint) (unixSeconds / daySeconds);
            var calcBase = totalDays * 100u + increment;
            var step1 = (calcBase << 11) ^ calcBase;
            var step2 = (step1 >> 8) ^ step1;
            return step2 % 100u;
        }

        private static long WeatherFingerprint(ServerWeather weather) {
            unchecked {
                long hash = weather.Zone.GetHashCode() * 31L + weather.Current.GetHashCode();
                foreach (var window in weather.Forecast) hash = hash * 31 + window.Name.GetHashCode();
                return hash;
            }
        }

        private void UpdateJobs() {
            if (this._jobsWatch.Elapsed < TimeSpan.FromSeconds(2) || this._clients.IsEmpty) return;
            this._jobsWatch.Restart();
            var snapshot = this.BuildJobsSnapshot();
            if (snapshot == null) return;
            var fingerprint = JobsFingerprint(snapshot);
            if (this._hasJobsSnapshot && fingerprint == this._lastJobsFingerprint) return;
            this._hasJobsSnapshot = true;
            this._lastJobsFingerprint = fingerprint;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneJobsSupport);
        }

        private unsafe ServerJobs? BuildJobsSnapshot() {
            if (XIVChatPlugin.Plugin.ObjectTable.LocalPlayer == null) return null;
            try {
                var playerState = PlayerState.Instance();
                if (playerState == null) return null;
                var levels = playerState->ClassJobLevels;
                var current = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer.ClassJob.RowId;
                var sheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<ClassJob>();
                var itemSheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Item>();
                var entries = new List<ServerJobEntry>();

                int ItemLevelForActiveJob() {
                    try {
                        foreach (var it in XIVChatPlugin.Plugin.GameInventory.GetInventoryItems(GameInventoryType.EquippedItems)) {
                            if (it.ContainerType == GameInventoryType.EquippedItems && it.InventorySlot == 0 && !it.IsEmpty && it.ItemId != 0) {
                                var row = itemSheet.GetRowOrDefault(it.BaseItemId);
                                return row is null ? -1 : (int) row.Value.LevelItem.RowId;
                            }
                        }
                    } catch (Exception ex) {
                        Plugin.Log.Warning($"Item level read failed: {ex.Message}");
                    }
                    return -1;
                }

                var gearsetJobs = new HashSet<uint>();
                var gearsets = RaptureGearsetModule.Instance();
                if (gearsets != null) {
                    foreach (var gearset in gearsets->Entries) {
                        if ((gearset.Flags & RaptureGearsetModule.GearsetFlag.Exists) == 0) continue;
                        var jobId = (uint) gearset.ClassJob;
                        var job = sheet.GetRowOrDefault(jobId);
                        if (job == null || job.Value.ExpArrayIndex < 0 || job.Value.ExpArrayIndex >= levels.Length) continue;
                        var category = JobCategory(job.Value);
                        var displayName = gearset.NameString;
                        if (string.IsNullOrWhiteSpace(displayName)) displayName = job.Value.Name.ExtractText();
                        entries.Add(new ServerJobEntry {
                            JobId = jobId,
                            Name = displayName,
                            Abbreviation = job.Value.Abbreviation.ExtractText(),
                            Category = category,
                            Level = levels[job.Value.ExpArrayIndex],
                            Active = gearsets->CurrentGearsetIndex == gearset.Id,
                            ItemLevel = gearset.ItemLevel,
                            IconId = 62100u + jobId,
                            GearsetId = gearset.Id,
                        });
                        gearsetJobs.Add(jobId);
                    }
                }

                foreach (var job in sheet) {
                    if (job.RowId == 0 || job.ExpArrayIndex < 0 || job.ExpArrayIndex >= levels.Length) continue;
                    var level = levels[job.ExpArrayIndex];
                    if (level <= 0) continue;
                    if (gearsetJobs.Contains(job.RowId)) continue;
                    var category = JobCategory(job);
                    entries.Add(new ServerJobEntry {
                        JobId = job.RowId, Name = job.Name.ExtractText(), Abbreviation = job.Abbreviation.ExtractText(),
                        Category = category, Level = level, Active = job.RowId == current,
                        ItemLevel = job.RowId == current ? ItemLevelForActiveJob() : -1,
                        IconId = 62100u + job.RowId,
                        GearsetId = -1,
                    });
                }
                return new ServerJobs(DateTimeOffset.UtcNow.ToUnixTimeSeconds(), entries.OrderBy(x => x.Category).ThenBy(x => x.JobId).ToArray());
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not capture jobs: {ex.Message}");
                return null;
            }
        }

        private static string JobCategory(ClassJob job) => job.ClassJobCategory.RowId switch {
            32 => "采集",
            33 => "生产",
            _ => job.JobType switch {
                1 => "坦克",
                2 or 6 => "治疗",
                3 => "近战",
                4 => "远程物理",
                5 => "远程魔法",
                _ => "战斗",
            },
        };

        private static long JobsFingerprint(ServerJobs jobs) {
            unchecked {
                long hash = 17;
                foreach (var entry in jobs.Entries) hash = hash * 31 + entry.JobId * 397L + entry.Level + (entry.Active ? 1 : 0);
                return hash;
            }
        }

        private void UpdateDailies() {
            if (this._dailiesWatch.Elapsed < TimeSpan.FromSeconds(2) || this._clients.IsEmpty) return;
            this._dailiesWatch.Restart();
            var snapshot = this.BuildDailiesSnapshot();
            if (snapshot == null) return;
            var fingerprint = DailiesFingerprint(snapshot);
            if (this._hasDailiesSnapshot && fingerprint == this._lastDailiesFingerprint) return;
            this._hasDailiesSnapshot = true;
            this._lastDailiesFingerprint = fingerprint;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneDailiesSupport);
        }

        private unsafe ServerDailies? BuildDailiesSnapshot() {
            if (XIVChatPlugin.Plugin.ObjectTable.LocalPlayer == null) return null;
            try {
                var entries = new List<ServerDailyEntry>();
                void Add(string id, string label, bool weekly, bool automatic, bool available, bool complete, int remaining, int goal, string note = "") =>
                    entries.Add(new ServerDailyEntry { Id = id, Label = label, Weekly = weekly, Automatic = automatic, Available = available, Complete = complete, Remaining = remaining, Goal = goal, Note = note });

                var content = FFXIVClientStructs.FFXIV.Client.Game.UI.InstanceContent.Instance();
                var rouletteIds = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Lumina.Excel.Sheets.ContentRoulette>()
                    .Where(row => row.RowId != 0 && row.IsInDutyFinder && !row.IsGoldSaucer && row.CompletionArrayIndex >= 0 && row.Name.ExtractText().Length > 0)
                    .Select(row => (byte)row.RowId).ToArray();
                var rouletteDone = content == null ? 0 : rouletteIds.Count(id => content->IsRouletteComplete(id));
                Add("daily.roulettes", "随机任务", false, true, content != null && rouletteIds.Length > 0, rouletteDone >= rouletteIds.Length && rouletteIds.Length > 0, rouletteIds.Length - rouletteDone, rouletteIds.Length);

                var quests = QuestManager.Instance();
                if (quests == null) {
                    Add("daily.beastTribe", "友好部族任务", false, true, false, false, 0, 12);
                    Add("daily.levequests", "理符任务", false, true, false, false, 0, 100);
                } else {
                    var beast = Math.Clamp((int)quests->GetBeastTribeAllowance(), 0, 12);
                    Add("daily.beastTribe", "友好部族任务", false, true, true, beast == 0, beast, 12);
                    var leves = Math.Clamp((int)quests->NumLeveAllowances, 0, 100);
                    Add("daily.levequests", "理符额度", false, true, true, true, leves, 100, "当前持有额度");
                }
                Add("daily.miniCactpot", "仙人微彩", false, false, true, false, 3, 3);
                Add("daily.gcSupply", "筹备与补给", false, false, true, false, 1, 1);

                var doman = DomanEnclaveManager.Instance();
                if (doman == null || doman->State.Allowance == 0) Add("weekly.domanEnclave", "多玛飞地捐赠", true, true, false, false, 0, 0);
                else {
                    var allowance = (int)doman->State.Allowance;
                    var remaining = Math.Max(0, allowance - (int)doman->State.Donated);
                    Add("weekly.domanEnclave", "多玛飞地捐赠", true, true, true, remaining == 0, remaining, allowance);
                }

                var player = PlayerState.Instance();
                if (player == null) Add("weekly.wondrousTails", "天书奇谈", true, true, false, false, 0, 9);
                else {
                    var placed = Enumerable.Range(0, 16).Count(index => player->IsWeeklyBingoStickerPlaced(index));
                    var remaining = player->IsWeeklyBingoExpired() ? 9 : Math.Max(0, 9 - placed);
                    Add("weekly.wondrousTails", "天书奇谈", true, true, true, remaining == 0, remaining, 9);
                }

                var supply = SatisfactionSupplyManager.Instance();
                var deliveries = supply == null ? -1 : supply->GetRemainingAllowances();
                Add("weekly.customDeliveries", "老主顾交易", true, true, deliveries >= 0, deliveries == 0, Math.Max(0, deliveries), 12);
                var now = DateTime.UtcNow;
                Add("weekly.jumboCactpot", "仙人彩", true, false, true, false, 3, 3, $"下次开奖：{NextWeeklyUtc(now, DayOfWeek.Saturday, 8):MM-dd HH:mm} UTC");
                Add("weekly.fashionReport", "时尚品鉴", true, false, true, false, 1, 1, "周五 08:00 UTC 开始评分");
                Add("weekly.challengeLog", "挑战笔记", true, false, true, false, 1, 1);
                Add("weekly.raidLockout", "大型任务周限制", true, false, true, false, 1, 1);
                Add("weekly.huntBills", "精英狩猎通缉令", true, false, true, false, 1, 1);

                return new ServerDailies(DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    new DateTimeOffset(NextDailyUtc(now, 15)).ToUnixTimeSeconds(),
                    new DateTimeOffset(NextWeeklyUtc(now, DayOfWeek.Tuesday, 8)).ToUnixTimeSeconds(), entries.ToArray());
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not capture dailies: {ex.Message}");
                return null;
            }
        }

        private static DateTime NextDailyUtc(DateTime now, int hour) {
            var target = new DateTime(now.Year, now.Month, now.Day, hour, 0, 0, DateTimeKind.Utc);
            return target > now ? target : target.AddDays(1);
        }

        private static DateTime NextWeeklyUtc(DateTime now, DayOfWeek day, int hour) {
            var target = new DateTime(now.Year, now.Month, now.Day, hour, 0, 0, DateTimeKind.Utc);
            while (target.DayOfWeek != day || target <= now) target = target.AddDays(1);
            return target;
        }

        private static long DailiesFingerprint(ServerDailies snapshot) {
            unchecked {
                long hash = snapshot.NextDailyResetUnix / 60;
                foreach (var entry in snapshot.Entries) hash = hash * 31 + entry.Id.GetHashCode() + entry.Remaining * 17L + (entry.Complete ? 1 : 0);
                return hash;
            }
        }

        private void UpdateCollections() {
            if (this._collectionsWatch.Elapsed < TimeSpan.FromSeconds(5) || this._clients.IsEmpty) return;
            this._collectionsWatch.Restart();
            var snapshot = this.BuildCollectionsSnapshot();
            if (snapshot == null) return;
            var fingerprint = CollectionsFingerprint(snapshot);
            if (this._hasCollectionsSnapshot && fingerprint == this._lastCollectionsFingerprint) return;
            this._hasCollectionsSnapshot = true;
            this._lastCollectionsFingerprint = fingerprint;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneCollectionsSupport);
        }

        private unsafe ServerCollections? BuildCollectionsSnapshot() {
            if (XIVChatPlugin.Plugin.ObjectTable.LocalPlayer == null) return null;
            try {
                var unlock = XIVChatPlugin.Plugin.UnlockState;
                var categories = new List<ServerCollectionCategory>();

                void AddMounts() {
                    var items = new List<ServerCollectionItem>();
                    var owned = 0;
                    var total = 0;
                    foreach (var row in XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Lumina.Excel.Sheets.Mount>()) {
                        if (row.Singular.IsEmpty || row.Order == -1) continue;
                        total++;
                        var isOwned = unlock.IsMountUnlocked(row);
                        if (isOwned) owned++;
                        items.Add(new ServerCollectionItem {
                            Id = row.RowId,
                            Name = row.Singular.ExtractText(),
                            IconId = row.Icon,
                            Owned = isOwned,
                        });
                    }
                    categories.Add(new ServerCollectionCategory { Id = 0, Total = total, Owned = owned, Items = items.ToArray() });
                }

                void AddMinions() {
                    var items = new List<ServerCollectionItem>();
                    var owned = 0;
                    var total = 0;
                    foreach (var row in XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Lumina.Excel.Sheets.Companion>()) {
                        if (row.Singular.IsEmpty) continue;
                        total++;
                        var isOwned = unlock.IsCompanionUnlocked(row);
                        if (isOwned) owned++;
                        items.Add(new ServerCollectionItem {
                            Id = row.RowId,
                            Name = row.Singular.ExtractText(),
                            IconId = row.Icon,
                            Owned = isOwned,
                        });
                    }
                    categories.Add(new ServerCollectionCategory { Id = 1, Total = total, Owned = owned, Items = items.ToArray() });
                }

                void AddEmotes() {
                    var items = new List<ServerCollectionItem>();
                    var owned = 0;
                    var total = 0;
                    foreach (var row in XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Lumina.Excel.Sheets.Emote>()) {
                        if (row.Name.IsEmpty || row.Icon == 0 || row.UnlockLink == 0) continue;
                        total++;
                        var isOwned = unlock.IsEmoteUnlocked(row);
                        if (isOwned) owned++;
                        items.Add(new ServerCollectionItem {
                            Id = row.RowId,
                            Name = row.Name.ExtractText(),
                            IconId = row.Icon,
                            Owned = isOwned,
                        });
                    }
                    categories.Add(new ServerCollectionCategory { Id = 2, Total = total, Owned = owned, Items = items.ToArray() });
                }

                void AddOrchestrions() {
                    var items = new List<ServerCollectionItem>();
                    var owned = 0;
                    var total = 0;
                    foreach (var row in XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Lumina.Excel.Sheets.Orchestrion>()) {
                        if (row.Name.IsEmpty || row.Name.ExtractText() == "0") continue;
                        total++;
                        var isOwned = unlock.IsOrchestrionUnlocked(row);
                        if (isOwned) owned++;
                        items.Add(new ServerCollectionItem {
                            Id = row.RowId,
                            Name = row.Name.ExtractText(),
                            IconId = 0,
                            Owned = isOwned,
                        });
                    }
                    categories.Add(new ServerCollectionCategory { Id = 3, Total = total, Owned = owned, Items = items.ToArray() });
                }

                void AddFacewear() {
                    var items = new List<ServerCollectionItem>();
                    var owned = 0;
                    var total = 0;
                    foreach (var row in XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Lumina.Excel.Sheets.Glasses>()) {
                        if (row.Icon == 0 || !row.Style.IsValid || row.Style.Value.Name.IsEmpty) continue;
                        total++;
                        var isOwned = unlock.IsGlassesUnlocked(row);
                        if (isOwned) owned++;
                        items.Add(new ServerCollectionItem {
                            Id = row.RowId,
                            Name = row.Style.Value.Name.ExtractText(),
                            IconId = (uint) row.Icon,
                            Owned = isOwned,
                        });
                    }
                    categories.Add(new ServerCollectionCategory { Id = 5, Total = total, Owned = owned, Items = items.ToArray() });
                }

                void AddTriadCards() {
                    var items = new List<ServerCollectionItem>();
                    var owned = 0;
                    var total = 0;
                    foreach (var row in XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Lumina.Excel.Sheets.TripleTriadCard>()) {
                        if (row.Name.IsEmpty || row.Name.ExtractText() == "0") continue;
                        total++;
                        var isOwned = unlock.IsTripleTriadCardUnlocked(row);
                        if (isOwned) owned++;
                        items.Add(new ServerCollectionItem {
                            Id = row.RowId,
                            Name = row.Name.ExtractText(),
                            IconId = 0,
                            Owned = isOwned,
                        });
                    }
                    categories.Add(new ServerCollectionCategory { Id = 7, Total = total, Owned = owned, Items = items.ToArray() });
                }

                foreach (var action in new System.Action[] { AddMounts, AddMinions, AddEmotes, AddOrchestrions, AddFacewear, AddTriadCards }) {
                    try {
                        action();
                    } catch (Exception ex) {
                        Plugin.Log.Warning($"Collection category failed: {ex.Message}");
                    }
                }

                return new ServerCollections(DateTimeOffset.UtcNow.ToUnixTimeSeconds(), categories.ToArray());
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not capture collections: {ex.Message}");
                return null;
            }
        }

        private static long CollectionsFingerprint(ServerCollections snapshot) {
            unchecked {
                long hash = 0;
                foreach (var category in snapshot.Categories) {
                    hash = hash * 31 + category.Id + category.Owned * 7L;
                    foreach (var item in category.Items) hash = hash * 31 + item.Id;
                }
                return hash;
            }
        }

        private void UpdateMaps() {
            if (this._clients.IsEmpty) return;
            var snapshot = this.BuildMapsSnapshot();
            if (snapshot == null) return;
            var fingerprint = HashCode.Combine(snapshot.CurrentZone, snapshot.CurrentRegion);
            if (this._hasMapsSnapshot && fingerprint == this._lastMapsFingerprint) return;
            this._hasMapsSnapshot = true;
            this._lastMapsFingerprint = fingerprint;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneMapsSupport);
        }

        private void UpdateFishing() {
            if (this._fishingWatch.Elapsed < TimeSpan.FromSeconds(2) || this._clients.IsEmpty) return;
            this._fishingWatch.Restart();
            var snapshot = this.BuildFishingSnapshot();
            if (snapshot == null) return;
            var fingerprint = FishingFingerprint(snapshot);
            if (this._hasFishingSnapshot && fingerprint == this._lastFishingFingerprint) return;
            this._hasFishingSnapshot = true;
            this._lastFishingFingerprint = fingerprint;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneFishingSupport);
        }

        private void UpdateSubmarine() {
            if (this._submarineWatch.Elapsed < TimeSpan.FromSeconds(3) || this._clients.IsEmpty) return;
            this._submarineWatch.Restart();
            var snapshot = this.BuildSubmarineSnapshot();
            if (snapshot == null) return;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneSubmarineSupport);
        }

        private unsafe ServerSubmarine? BuildSubmarineSnapshot() {
            try {
                var manager = HousingManager.Instance();
                if (manager == null) return null;
                var territory = manager->WorkshopTerritory;
                if (territory == null) return null;
                var sub = territory->Submersible;
                var vessels = new List<ServerSubmarineVessel>();
                for (var i = 0; i < Math.Min(4, sub.DataPointers.Length); i++) {
                    var vessel = sub.DataPointers[i].Value;
                    if (vessel == null) continue;
                    var name = vessel->Name.ToString().Trim();
                    if (name.Length == 0) continue;
                    var returnTime = (long)(vessel->GetReturnTime().ToUniversalTime() - DateTime.UnixEpoch).TotalSeconds;
                    vessels.Add(new ServerSubmarineVessel {
                        Name = name,
                        ReturnUnix = returnTime,
                        RankId = vessel->RankId,
                        CurrentExp = (long)vessel->CurrentExp,
                        NextLevelExp = (long)vessel->NextLevelExp,
                    });
                }
                return new ServerSubmarine {
                    UpdatedUnix = DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    Vessels = vessels.ToArray(),
                };
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not capture submarine: {ex.Message}");
                return null;
            }
        }

        private unsafe ServerFishing? BuildFishingSnapshot() {
            if (XIVChatPlugin.Plugin.ObjectTable.LocalPlayer == null) return null;
            try {
                var state = PlayerState.Instance();
                if (state == null) return null;
                var fishCount = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<FishParameter>().Count;
                var spearfishCount = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<SpearfishingItem>().Count;
                var fish = new byte[(fishCount + 7) / 8];
                var spearfish = new byte[(spearfishCount + 7) / 8];
                Marshal.Copy((IntPtr) state->CaughtFishBitArray.Pointer, fish, 0, fish.Length);
                Marshal.Copy((IntPtr) state->CaughtSpearfishBitArray.Pointer, spearfish, 0, spearfish.Length);
                return new ServerFishing {
                    UpdatedUnix = DateTimeOffset.UtcNow.ToUnixTimeSeconds(),
                    FishBits = fish,
                    SpearfishBits = spearfish,
                };
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not capture fishing notebook: {ex.Message}");
                return null;
            }
        }

        private static long FishingFingerprint(ServerFishing snapshot) {
            unchecked {
                long hash = 17;
                foreach (var value in snapshot.FishBits) hash = hash * 31 + value;
                foreach (var value in snapshot.SpearfishBits) hash = hash * 31 + value;
                return hash;
            }
        }

        private ServerMaps? BuildMapsSnapshot() {
            if (XIVChatPlugin.Plugin.ObjectTable.LocalPlayer == null) return null;
            try {
                var zone = string.Empty;
                var region = string.Empty;
                var territoryId = XIVChatPlugin.Plugin.ClientState.TerritoryType;
                var territories = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<TerritoryType>();
                if (territoryId != 0 && territories.TryGetRow(territoryId, out var territory)) {
                    zone = this.MapPlaceName(territory.PlaceName.RowId);
                    region = this.MapPlaceName(territory.PlaceNameRegion.RowId);
                }

                this._mapCatalog ??= this.BuildMapCatalog();
                return new ServerMaps { CurrentZone = zone, CurrentRegion = region, Expansions = this._mapCatalog };
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not build map data: {ex.Message}");
                return null;
            }
        }

        private ServerMapExpansion[] BuildMapCatalog() {
            var byTerritory = new Dictionary<uint, List<ServerMapDestination>>();
            var seenNames = new Dictionary<uint, HashSet<string>>();
            foreach (var row in XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Aetheryte>()) {
                if (!row.IsAetheryte || row.Invisible || row.Territory.RowId == 0) continue;
                var name = this.MapPlaceName(row.PlaceName.RowId);
                if (name.Length == 0) continue;
                var territoryId = row.Territory.RowId;
                if (!seenNames.TryGetValue(territoryId, out var names)) {
                    names = new HashSet<string>(StringComparer.Ordinal);
                    seenNames[territoryId] = names;
                }
                if (!names.Add(name)) continue;
                if (!byTerritory.TryGetValue(territoryId, out var entries)) {
                    entries = [];
                    byTerritory[territoryId] = entries;
                }
                entries.Add(new ServerMapDestination { RowId = row.RowId, Name = name, Order = row.Order });
            }

            var regionBuckets = new Dictionary<(byte Order, string Name), List<ServerMapDestination>>();
            foreach (var territory in XIVChatPlugin.Plugin.DataManager.GetExcelSheet<TerritoryType>()) {
                if (!byTerritory.TryGetValue(territory.RowId, out var destinations) || destinations.Count == 0) continue;
                var regionName = this.MapPlaceName(territory.PlaceNameRegion.RowId);
                if (regionName.Length == 0) regionName = "艾欧泽亚";
                var key = ((byte)territory.ExVersion.RowId, regionName);
                if (!regionBuckets.TryGetValue(key, out var bucket)) {
                    bucket = [];
                    regionBuckets[key] = bucket;
                }
                bucket.AddRange(destinations);
            }

            var regions = regionBuckets.Select(pair => new ServerMapRegion {
                Name = pair.Key.Name,
                Order = pair.Key.Order,
                Destinations = pair.Value.OrderBy(value => value.Order).ThenBy(value => value.Name, StringComparer.OrdinalIgnoreCase).ToArray(),
            }).OrderBy(value => value.Order).ThenBy(value => value.Name, StringComparer.OrdinalIgnoreCase).ToArray();

            var exVersions = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<ExVersion>();
            return regions.GroupBy(value => value.Order).Select(group => {
                var name = exVersions.TryGetRow(group.Key, out var version) ? version.Name.ExtractText() : string.Empty;
                return new ServerMapExpansion {
                    Name = name.Length == 0 ? "艾欧泽亚" : name,
                    Order = group.Key,
                    Regions = group.ToArray(),
                };
            }).OrderBy(value => value.Order).ToArray();
        }

        private string MapPlaceName(uint rowId) {
            return rowId != 0 && XIVChatPlugin.Plugin.DataManager.GetExcelSheet<PlaceName>().TryGetRow(rowId, out var row)
                ? row.Name.ExtractText()
                : string.Empty;
        }

        private void UpdateActivity() {
            var snapshot = this._activityTracker.Update();
            if (snapshot == null || this._clients.IsEmpty) return;
            var fingerprint = ActivityFingerprint(snapshot);
            if (this._hasActivitySnapshot && fingerprint == this._lastActivityFingerprint) return;
            this._hasActivitySnapshot = true;
            this._lastActivityFingerprint = fingerprint;
            this.BroadcastMessage(snapshot, ClientPreference.PhoneActivitySupport);
        }
        private void UpdateParty() {
            if (this._partyWatch.Elapsed < TimeSpan.FromSeconds(1) || this._clients.IsEmpty) {
                return;
            }

            this._partyWatch.Restart();
            var snapshot = this.BuildPartyList();
            if (snapshot == null) {
                if (this._lastPartyFingerprint != 0) {
                    this._lastPartyFingerprint = 0;
                    this.BroadcastMessage(new ServerPlayerList(PlayerListType.Party, []));
                }

                return;
            }

            var fingerprint = 0L;
            foreach (var member in snapshot.Players) {
                fingerprint = HashCode.Combine(fingerprint, member.Name, member.Job, member.CurrentWorldName);
            }

            if (fingerprint == this._lastPartyFingerprint) {
                return;
            }

            this._lastPartyFingerprint = fingerprint;
            this.BroadcastMessage(snapshot);
        }

        private ServerPlayerList? BuildPartyList() {
            try {
                var native = this.BuildPartyListNative();
                if (native != null) {
                    Plugin.Log.Info($"[EorzeaPhone] PartyList native members={native.Count}");
                    return new ServerPlayerList(PlayerListType.Party, native.ToArray());
                }
            } catch (Exception ex) {
                Plugin.Log.Warning($"[EorzeaPhone] Could not read native party list: {ex}");
            }

            try {
                var party = XIVChatPlugin.Plugin.PartyList;
                var rawLength = party?.Length ?? 0;
                if (rawLength == 0) {
                    Plugin.Log.Info("[EorzeaPhone] PartyList empty (service=" + (XIVChatPlugin.Plugin.PartyList != null) + ")");
                    return null;
                }

                var sheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<ClassJob>();
                var members = new List<Player>();
                for (var i = 0; i < rawLength; i++) {
                    var member = party[i];
                    if (member == null) {
                        continue;
                    }

                    var name = member.Name?.TextValue ?? string.Empty;
                    if (string.IsNullOrWhiteSpace(name)) {
                        try {
                            name = member.GameObject?.Name?.TextValue ?? string.Empty;
                        } catch {
                            // ignore
                        }
                    }

                    if (string.IsNullOrWhiteSpace(name)) {
                        continue;
                    }

                    var jobId = member.ClassJob.IsValid ? member.ClassJob.RowId : 0u;
                    var jobName = jobId > 0 ? sheet.GetRowOrDefault(jobId)?.Name.ExtractText() : null;
                    var worldName = member.World.IsValid ? member.World.Value.Name.ExtractText() : string.Empty;
                    members.Add(new Player {
                        Name = name,
                        CurrentWorldName = worldName,
                        HomeWorldName = worldName,
                        Job = (byte) jobId,
                        JobName = jobName,
                        ContentId = member.ContentId,
                    });
                }

                Plugin.Log.Info($"[EorzeaPhone] PartyList length={rawLength} members={members.Count}");
                return new ServerPlayerList(PlayerListType.Party, members.ToArray());
            } catch (Exception ex) {
                Plugin.Log.Warning($"[EorzeaPhone] Could not read party list: {ex}");
                return null;
            }
        }

        private unsafe List<Player>? BuildPartyListNative() {
            var members = new List<Player>();
            var seen = new HashSet<ulong>();

            var manager = FFXIVClientStructs.FFXIV.Client.Game.Group.GroupManager.Instance();
            if (manager != null) {
                var group = &manager->MainGroup;
                var count = Math.Min((int) group->MemberCount, 8);
                if (count > 0) {
                    var sheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<ClassJob>();
                    for (var i = 0; i < count; i++) {
                        var member = group->GetPartyMemberByIndex(i);
                        if (member == null || (member->ContentId == 0 && member->EntityId == 0)) {
                            continue;
                        }

                        var player = this.BuildPartyPlayer(member, sheet);
                        if (player == null) {
                            continue;
                        }

                        if (player.ContentId != 0 && !seen.Add(player.ContentId)) {
                            continue;
                        }

                        members.Add(player);
                    }
                }
            }

            // 跨服小队：本队列表在 InfoProxyCrossRealm 中，MainGroup.MemberCount 可能为 0。
            if (members.Count == 0) {
                var cross = FFXIVClientStructs.FFXIV.Client.UI.Info.InfoProxyCrossRealm.Instance();
                if (cross != null && cross->IsCrossRealm) {
                    var sheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<ClassJob>();
                    var groupCount = Math.Min((int) cross->GroupCount, 6);
                    for (var gi = 0; gi < groupCount && members.Count < 8; gi++) {
                        var memberCount = Math.Min((int) FFXIVClientStructs.FFXIV.Client.UI.Info.InfoProxyCrossRealm.GetGroupMemberCount(gi), 8);
                        for (var mi = 0; mi < memberCount && members.Count < 8; mi++) {
                            var member = FFXIVClientStructs.FFXIV.Client.UI.Info.InfoProxyCrossRealm.GetGroupMember((uint) mi, gi);
                            if (member == null || (member->ContentId == 0 && member->EntityId == 0)) {
                                continue;
                            }

                            var player = this.BuildCrossRealmPlayer(member, sheet);
                            if (player == null) {
                                continue;
                            }

                            if (player.ContentId != 0 && !seen.Add(player.ContentId)) {
                                continue;
                            }

                            members.Add(player);
                        }
                    }
                }
            }

            if (members.Count == 0) {
                Plugin.Log.Info("[EorzeaPhone] Native party empty");
                return null;
            }

            Plugin.Log.Info($"[EorzeaPhone] Native party members={members.Count}");
            return members;
        }

        private unsafe Player? BuildPartyPlayer(FFXIVClientStructs.FFXIV.Client.Game.Group.PartyMember* member, Lumina.Excel.ExcelSheet<Lumina.Excel.Sheets.ClassJob>? sheet) {
            var name = this.ReadPartyMemberName(member);
            if (string.IsNullOrWhiteSpace(name)) {
                return null;
            }

            var jobId = member->ClassJob;
            var jobName = jobId > 0 ? sheet.GetRowOrDefault(jobId)?.Name.ExtractText() : null;
            var homeWorld = member->HomeWorld;
            var worldName = this.WorldName(homeWorld) ?? string.Empty;
            return new Player {
                Name = name,
                CurrentWorld = homeWorld,
                CurrentWorldName = worldName,
                HomeWorld = homeWorld,
                HomeWorldName = worldName,
                Job = jobId,
                JobName = jobName,
                ContentId = member->ContentId,
            };
        }

        private unsafe Player? BuildCrossRealmPlayer(FFXIVClientStructs.FFXIV.Client.UI.Info.CrossRealmMember* member, Lumina.Excel.ExcelSheet<Lumina.Excel.Sheets.ClassJob>? sheet) {
            var name = this.ReadCrossRealmMemberName(member);
            if (string.IsNullOrWhiteSpace(name)) {
                return null;
            }

            var jobId = member->ClassJobId;
            var jobName = jobId > 0 ? sheet.GetRowOrDefault(jobId)?.Name.ExtractText() : null;
            var hw = member->HomeWorld;
            var homeWorld = hw < 0 ? (ushort) 0 : (ushort) hw;
            var cw = member->CurrentWorld;
            var currentWorld = cw < 0 ? (ushort) 0 : (ushort) cw;
            var worldName = this.WorldName(homeWorld) ?? string.Empty;
            return new Player {
                Name = name,
                CurrentWorld = currentWorld,
                CurrentWorldName = this.WorldName(currentWorld) ?? worldName,
                HomeWorld = homeWorld,
                HomeWorldName = worldName,
                Job = jobId,
                JobName = jobName,
                ContentId = member->ContentId,
            };
        }

        private unsafe string ReadCrossRealmMemberName(FFXIVClientStructs.FFXIV.Client.UI.Info.CrossRealmMember* member) {
            try {
                var name = member->NameString;
                if (!string.IsNullOrWhiteSpace(name)) {
                    return name;
                }
            } catch {
                // raw name unavailable
            }

            return string.Empty;
        }

        private unsafe string ReadPartyMemberName(FFXIVClientStructs.FFXIV.Client.Game.Group.PartyMember* member) {
            try {
                if (member->NameOverride != null) {
                    var overrideName = member->NameOverride->ToString();
                    if (!string.IsNullOrWhiteSpace(overrideName)) {
                        return overrideName;
                    }
                }
            } catch {
                // name override unavailable; fall through
            }

            try {
                var name = member->NameString;
                if (!string.IsNullOrWhiteSpace(name)) {
                    return name;
                }
            } catch {
                // raw name unavailable
            }

            return string.Empty;
        }

        private string? WorldName(ushort id) {
            if (id == 0) {
                return null;
            }

            return XIVChatPlugin.Plugin.DataManager.GetExcelSheet<World>().GetRowOrDefault(id)?.Name.ExtractText();
        }

        /// <summary>
        /// Subcategory row ids per top-level board group, in display order.
        ///
        /// This is hardcoded because the sheet's own <c>Category</c> column is NOT a
        /// parent pointer: on the CN client its values are 1-4 with unrelated
        /// semantics (1 lumps weapons AND tools together, 4 is furniture), which
        /// produced "制作工具 containing 防具" nonsense. The row ids themselves have
        /// been stable for a decade (7.0 appended 91/92), and unknown ids fall into
        /// 其他 below, so a future patch degrades gracefully.
        /// </summary>
        private static readonly Dictionary<byte, byte[]> BoardGroupSubcategories = new() {
            [1] = [9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 73, 76, 77, 78, 83, 84, 85, 86, 87, 88, 89, 91, 92], // 武器
            [2] = [19, 20, 21, 22, 23, 24, 25, 26],                                                             // 主工具
            [3] = [27, 28, 29, 30],                                                                             // 副工具
            [4] = [31, 32, 33, 34, 35, 36, 37, 38],                                                             // 防具
            [5] = [39, 40, 41, 42],                                                                             // 首饰
            [6] = [43, 44, 45, 46, 53],                                                                         // 药品
            [7] = [47, 48, 49, 50, 51, 52, 54, 55, 57, 58, 59],                                                 // 素材
            [8] = [56, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 74, 75, 79, 80, 81, 82, 90],         // 其他(含家具)
        };

        /// <summary>
        /// Return the market category snapshot, rebuilding it when the vendor sheet has
        /// transitioned from "not ready" to ready (or when its cache generation changed).
        ///
        /// Category requests are drained on the framework thread while the login
        /// pre-warm runs on a worker.  Both paths therefore go through one lock; a plain
        /// null-coalescing assignment was racy and, more importantly, could permanently
        /// retain a tree whose every <c>NpcPrice</c> was zero when GilShopItem had not
        /// loaded yet.
        /// </summary>
        private ServerMarketCategories? GetMarketCategories() {
            lock (this._marketCategoriesGate) {
                this._marketCache.EnsureGilShopData();
                var vendorTimestamp = this._marketCache.GilShopDataTimestampMs;
                if (this._marketCategoriesCache != null
                    && this._marketCategoriesGilShopTimestamp == vendorTimestamp) {
                    return this._marketCategoriesCache;
                }

                var built = this.BuildMarketCategories();
                if (built == null) {
                    // Do not replace a usable previous snapshot with null just because
                    // an Excel sheet briefly disappeared during a zone/login transition.
                    return this._marketCategoriesCache;
                }

                this._marketCategoriesCache = built;
                this._marketCategoriesGilShopTimestamp = vendorTimestamp;
                return built;
            }
        }

        private ServerMarketCategories? BuildMarketCategories() {
            try {
                var itemSheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Item>();
                var categorySheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<ItemSearchCategory>();
                if (itemSheet == null || categorySheet == null) return null;

                // The category tree is also the offline source for the NPC benchmark.
                // Ensure the GilShopItem index has had a chance to load before taking
                // the snapshot; an empty early-start sheet must not permanently turn
                // every vendor price into zero for this plugin session.
                this._marketCache.EnsureGilShopData();
                var gilShopItems = this._marketCache.GilShopItemIds;

                // Items grouped by their search subcategory row.
                var bySub = new Dictionary<byte, List<ServerMarketItem>>();
                foreach (var item in itemSheet) {
                    if (item.RowId == 0) continue;
                    var searchCat = item.ItemSearchCategory.RowId;
                    if (searchCat == 0) continue;

                    var key = (byte) searchCat;
                    if (!bySub.TryGetValue(key, out var list)) {
                        list = new List<ServerMarketItem>();
                        bySub[key] = list;
                    }

                    list.Add(new ServerMarketItem {
                        ItemId = item.RowId,
                        Name = item.Name.ExtractText(),
                        IconId = item.Icon,
                        LevelItem = (byte) item.LevelItem.RowId,
                        CanBeHq = item.CanBeHq,
                        // PriceMid is only exposed when the item is actually present
                        // in a gil shop.  PriceMid by itself is not a sufficient gate
                        // (some unsellable items carry a resale value in the sheet).
                        NpcPrice = gilShopItems.Contains(item.RowId) && item.PriceMid > 0
                            ? item.PriceMid : 0,
                    });
                }

                var categories = new List<ServerMarketCategory>();
                foreach (var (groupId, subIds) in BoardGroupSubcategories.OrderBy(kv => kv.Key)) {
                    // Name/icon come from the sheet's own top-level row (1-8), so the
                    // labels stay localised with the client language.
                    categorySheet.TryGetRow(groupId, out var parentRow);
                    var subs = new List<ServerMarketSubcategory>();
                    foreach (var subId in subIds) {
                        if (!bySub.TryGetValue(subId, out var items)) continue;
                        items.Sort((a, b) => a.LevelItem.CompareTo(b.LevelItem));
                        if (!categorySheet.TryGetRow(subId, out var subRow)) continue;
                        subs.Add(new ServerMarketSubcategory {
                            CategoryId = subId,
                            Name = subRow.Name.ExtractText(),
                            Order = (byte) subs.Count,
                            IconId = (uint) subRow.Icon,
                            Items = items.ToArray(),
                        });
                    }

                    if (subs.Count == 0) continue;
                    categories.Add(new ServerMarketCategory {
                        CategoryId = groupId,
                        Name = parentRow.Name.ExtractText(),
                        Order = groupId,
                        IconId = (uint) parentRow.Icon,
                        Subcategories = subs.ToArray(),
                    });

                    // Anything the mapping did not name (a brand-new patch subcategory)
                    // still has to show up -- append it to 其他 rather than dropping it.
                    if (groupId == 8) {
                        var mapped = subIds.ToHashSet();
                        foreach (var (subId, items) in bySub) {
                            if (mapped.Contains(subId)) continue;
                            if (BoardGroupSubcategories.Values.Any(ids => ids.Contains(subId))) continue;
                            items.Sort((a, b) => a.LevelItem.CompareTo(b.LevelItem));
                            if (!categorySheet.TryGetRow(subId, out var subRow)) continue;
                            var last = categories[^1].Subcategories;
                            categories[^1].Subcategories = last.Append(new ServerMarketSubcategory {
                                CategoryId = subId,
                                Name = subRow.Name.ExtractText(),
                                Order = (byte) last.Length,
                                IconId = (uint) subRow.Icon,
                                Items = items.ToArray(),
                            }).ToArray();
                        }
                    }
                }

                categories.Sort((a, b) => a.Order.CompareTo(b.Order));
                return new ServerMarketCategories(categories.ToArray());
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not build market categories: {ex.Message}");
                return null;
            }
        }


        private static long ActivityFingerprint(ServerActivity value) {
            unchecked {
                var hash = value.SessionPlaySeconds;
                hash = hash * 31 + value.SessionExpGained;
                hash = hash * 31 + value.SessionGilEarned;
                hash = hash * 31 + value.SessionDutiesCompleted;
                hash = hash * 31 + value.MountsOwned;
                hash = hash * 31 + value.MinionsOwned;
                hash = hash * 31 + value.VenturesReady;
                return hash;
            }
        }

        private static long InventoryFingerprint(IEnumerable<ServerInventoryItem> items) {
            unchecked {
                long hash = 17;
                foreach (var item in items) {
                    hash = hash * 31 + item.ItemId;
                    hash = hash * 31 + item.BaseItemId;
                    hash = hash * 31 + item.Quantity;
                    hash = hash * 31 + item.ContainerType;
                    hash = hash * 31 + item.InventorySlot;
                    hash = hash * 31 + item.RetainerId.GetHashCode();
                    hash = hash * 31 + (item.IsHq ? 1 : 0);
                    hash = hash * 31 + item.SpiritbondOrCollectability;
                    hash = hash * 31 + item.Condition;
                }

                return hash;
            }
        }

        private const int PhoneInventoryCapability = 0x4550;

        private static bool HasPhoneInventoryCapability(byte[] payload) {
            try {
                var reader = new MessagePackReader(new ReadOnlyMemory<byte>(payload));
                var fieldCount = reader.ReadArrayHeader();
                if (fieldCount < 2) {
                    return false;
                }

                reader.Skip();
                return reader.ReadInt32() == PhoneInventoryCapability;
            } catch (Exception) {
                return false;
            }
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
                            this._currentChannelName?.TextValue ?? this.LocalisedChannelName(channel)
                        ),
                        this._tokenSource.Token
                    );
                } catch (Exception ex) {
                    Plugin.Log.Error($"Could not send message: {ex.Message}");
                }

                var listen = Task.Run(async () => {
                    while (this._clients.TryGetValue(id, out var client) && client.Connected && !client.TokenSource.IsCancellationRequested) {
                        byte[] msg;
                        try {
                            msg = await SecretMessage.ReadSecretMessage(client, handshake.Keys.rx, client.TokenSource.Token);
                        } catch (SocketException ex) when (ex.SocketErrorCode == SocketError.TimedOut) {
                            continue;
                        } catch (Exception ex) {
                            Plugin.Log.Error($"Could not read message: {ex.Message}");
                            continue;
                        }

                        await this.ProcessMessage(id, client, msg);
                    }
                });

                this._plugin.Events.FireNewClientEvent(id, client);

                while (this._clients.TryGetValue(id, out var client) && client.Connected && !client.TokenSource.IsCancellationRequested) {
                    try {
                        var msg = await client.Queue.Reader.ReadAsync(client.TokenSource.Token);
                        await SecretMessage.SendSecretMessage(client, handshake.Keys.tx, msg, client.TokenSource.Token);
                    } catch (Exception ex) {
                        Plugin.Log.Error($"Could not send message: {ex.Message}");
                    }
                }

                client.Disconnect();

                await listen;

                this._clients.TryRemove(id, out _);
                Plugin.Log.Info($"Client thread ended: {id}");
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
                        Plugin.Log.Error($"Could not send message: {ex.Message}");
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

                    if (HasPhoneInventoryCapability(payload)) {
                        client.Preferences ??= new ClientPreferences();
                        client.Preferences.Preferences[ClientPreference.PhoneInventorySupport] = true;
                        this._awaitingInventory.Enqueue(id);
                    }

                    var backlogMessages = new List<ServerMessage>();

                    var currentTag = this.CurrentCharacterTag();
                    var charBacklog = this.FindBacklogList(currentTag);
                    var node = charBacklog?.Last;
                    while (node != null) {
                        if (backlogMessages.Count >= backlog.Amount) {
                            break;
                        }

                        var m = node.Value;
                        if (MatchesCurrentCharacter(m, currentTag)) backlogMessages.Add(m);
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
                            XIVChatPlugin.Plugin.ChatGui.PrintError($"[{Plugin.Name}] 请打开一次游戏内好友列表以启用好友列表功能。通常只需在首次安装或更新后执行一次。");
                        }
                    } else if (playerList.Type == PlayerListType.Party) {
                        this._awaitingParty.Enqueue(id);
                    }

                    break;
                case ClientOperation.Preferences:
                    var preferences = ClientPreferences.Decode(payload);
                    client.Preferences = preferences;

                    // immediately queue housing location
                    if (client.GetPreference(ClientPreference.HousingLocationSupport, false)) {
                        this._awaitingHousingLocation.Enqueue(id);
                    }

                    if (client.GetPreference(ClientPreference.PhoneInventorySupport, false)) {
                        this._awaitingInventory.Enqueue(id);
                    }

                    if (client.GetPreference(ClientPreference.PhoneWalletSupport, false)) {
                        this._awaitingWallet.Enqueue(id);
                    }

                    if (client.GetPreference(ClientPreference.PhoneWeatherSupport, false)) {
                        this._awaitingWeather.Enqueue(id);
                    }

                    if (client.GetPreference(ClientPreference.PhoneJobsSupport, false)) {
                        this._awaitingJobs.Enqueue(id);
                    }

                    if (client.GetPreference(ClientPreference.PhoneDailiesSupport, false)) {
                        this._awaitingDailies.Enqueue(id);
                    }

                    if (client.GetPreference(ClientPreference.PhoneActivitySupport, false)) {
                        this._awaitingActivity.Enqueue(id);
                    }

                    if (client.GetPreference(ClientPreference.PhoneCollectionsSupport, false)) {
                        this._awaitingCollections.Enqueue(id);
                    }

                    if (client.GetPreference(ClientPreference.PhoneMapsSupport, false)) {
                        this._awaitingMaps.Enqueue(id);
                    }

                    if (client.GetPreference(ClientPreference.PhoneFishingSupport, false)) {
                        this._awaitingFishing.Enqueue(id);
                    }

                    break;
                case ClientOperation.Channel:
                    var channel = ClientChannel.Decode(payload);
                    this._currentChannel = channel.Channel;
                    this._plugin.Functions.ChangeChatChannel(channel.Channel);

                    break;
                case ClientOperation.FriendAction:
                    this._friendActions.Enqueue(ClientFriendAction.Decode(payload));
                    break;
                case ClientOperation.JobsAction:
                    this._jobsActions.Enqueue(ClientJobsAction.Decode(payload).GearsetId);
                    break;
                case ClientOperation.Teleport:
                    var teleport = ClientTeleport.Decode(payload);
                    if (!string.IsNullOrWhiteSpace(teleport.PlaceName)) {
                        this._plugin.Functions.ProcessChatBox($"/tp {teleport.PlaceName}");
                    }
                    break;
                case ClientOperation.MarketSearch:
                    var marketSearch = ClientMarketSearch.Decode(payload);
                    if (marketSearch.ItemId > 0) {
                        this._marketSearches.Enqueue((id, marketSearch.ItemId, marketSearch.HqOnly));
                    }

                    break;
                case ClientOperation.MarketCategories:
                    this._awaitingMarketCategories.Enqueue(id);
                    break;
                case ClientOperation.MarketMonitorSync:
                    this.HandleMonitorSync(ClientMarketMonitorSync.Decode(payload));
                    break;
                case ClientOperation.MarketPurchase:
                    // Decode first so a refusal can echo back the item and listing the
                    // phone asked about. Replying with zeros made the phone unable to
                    // tell the refusal apart from a reply for some other request.
                    var purchase = ClientMarketPurchase.Decode(payload);

                    if (!this._plugin.Config.AllowMarketPurchase) {
                        this.RefusePurchase(id, purchase, MarketPurchaseStatus.Disabled);
                        break;
                    }

                    if (this._marketAwaitingListings || this._marketAwaitingPurchaseReply) {
                        this.RefusePurchase(id, purchase, MarketPurchaseStatus.Busy);
                        break;
                    }

                    if (purchase.ItemId > 0 && purchase.ListingId > 0) {
                        var refusal = this.MarketRefusalReason(purchase.ItemId);
                        if (refusal != MarketStatus.Ok) {
                            this.FinishPurchase(id, purchase.ItemId, purchase.ListingId,
                                (MarketPurchaseStatus) refusal);
                        } else {
                            this._marketPurchases.Enqueue((id, purchase));
                        }
                    }

                    break;
            }
        }

        internal class NameFormatting {
            internal string Before { get; private set; } = string.Empty;
            internal string After { get; private set; } = string.Empty;
            internal bool IsPresent { get; private set; } = true;

            internal static NameFormatting Empty() {
                return new() {
                    IsPresent = false,
                };
            }

            internal static NameFormatting Of(string before, string after) {
                return new() {
                    Before = before,
                    After = after,
                };
            }
        }

        private Dictionary<XivChatType, NameFormatting> Formats { get; } = new();

        private NameFormatting? FormatFor(XivChatType type) {
            if (this.Formats.TryGetValue(type, out var cached)) {
                return cached;
            }

            var logKind = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<LogKind>().GetRowOrDefault((ushort) type);

            if (logKind == null) {
                return null;
            }

            var format = logKind.Value.Format.ToDalamudString();

            var firstStringParam = format.Payloads.FindIndex(payload => IsStringParam(payload, 1));
            var secondStringParam = format.Payloads.FindIndex(payload => IsStringParam(payload, 2));

            if (firstStringParam == -1 || secondStringParam == -1) {
                return NameFormatting.Empty();
            }

            var before = format.Payloads
                .GetRange(0, firstStringParam)
                .Where(payload => payload is ITextProvider)
                .Cast<ITextProvider>()
                .Select(text => text.Text);
            var after = format.Payloads
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

            static bool IsStringParam(Payload payload, byte num) {
                var data = payload.Encode();

                return data is [_, 0x29, _, _, _, ..] && data[4] == num + 1;
            }
        }

        private static async Task SendBacklogs(IEnumerable<ServerMessage> messages, BaseClient client) {
            const int defaultSize = 5 + SecretMessage.NonceSize + SecretMessage.MacSize;
            var size = defaultSize;
            var responseMessages = new List<ServerMessage>();

            async Task SendBacklog() {
                var resp = new ServerBacklog(responseMessages.ToArray(), ++client.BacklogSequence);
                try {
                    await client.Queue.Writer.WriteAsync(resp);
                } catch (Exception ex) {
                    Plugin.Log.Error($"Could not send backlog: {ex.Message}");
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

        private string GetBacklogDirectory() {
            var p = this._plugin.Config.BacklogPath;
            if (string.IsNullOrWhiteSpace(p))
                return XIVChatPlugin.Plugin.Interface.ConfigDirectory.FullName;
            var path = p.Trim();
            // BacklogPath 现在按“目录”理解：每个角色一个 chat_backlog_<角色>.bin 文件。
            if (Directory.Exists(path) || path.EndsWith(Path.DirectorySeparatorChar) || path.EndsWith(Path.AltDirectorySeparatorChar))
                return path.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar);
            var dir = Path.GetDirectoryName(path);
            return string.IsNullOrEmpty(dir) ? XIVChatPlugin.Plugin.Interface.ConfigDirectory.FullName : dir;
        }

        private static string SanitizeForFileName(string tag) {
            var invalid = Path.GetInvalidFileNameChars();
            var sb = new StringBuilder(tag.Length);
            foreach (var c in tag) sb.Append(Array.IndexOf(invalid, c) >= 0 ? '_' : c);
            return sb.ToString();
        }

        private string GetBacklogFilePath(string? characterTag) {
            if (string.IsNullOrWhiteSpace(characterTag)) return string.Empty;
            return Path.Combine(this.GetBacklogDirectory(), $"chat_backlog_{SanitizeForFileName(characterTag)}.bin");
        }

        private void MaybePersistBacklog() {
            if (!this._plugin.Config.BacklogEnabled) return;
            if ((DateTime.UtcNow - this._lastPersist).TotalSeconds < 2) return;
            this._lastPersist = DateTime.UtcNow;
            System.Threading.Tasks.Task.Run(() => this.PersistBacklog());
        }

        internal void FlushBacklog() => this.PersistBacklog();

        private void PersistBacklog() {
            if (!this._plugin.Config.BacklogEnabled) return;
            var dir = this.GetBacklogDirectory();
            try {
                Directory.CreateDirectory(dir);
                HashSet<string> dirtyTags;
                lock (this._backlogLock) {
                    dirtyTags = new HashSet<string>(this._dirtyBacklogTags, StringComparer.OrdinalIgnoreCase);
                }
                if (dirtyTags.Count == 0) return;
                // 只重写“有新消息”的角色文件；文件头部带角色标识，读取时校验。
                foreach (var tag in dirtyTags) {
                    var path = this.GetBacklogFilePath(tag);
                    if (string.IsNullOrEmpty(path)) continue;
                    List<ServerMessage> group;
                    lock (this._backlogLock) {
                        if (!this._backlogByChar.TryGetValue(tag, out var charBacklog) || charBacklog.Count == 0) continue;
                        group = charBacklog.ToList();
                    }
                    var file = new BacklogFile {
                        CharacterTag = tag,
                        WrittenAtUnixMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                        Messages = group,
                    };
                    File.WriteAllBytes(path, MessagePack.MessagePackSerializer.Serialize(file));
                }
                lock (this._backlogLock) {
                    foreach (var tag in dirtyTags) this._dirtyBacklogTags.Remove(tag);
                }
            } catch (Exception ex) {
                Plugin.Log.Error($"Could not persist backlog to '{dir}': {ex.Message}");
            }
        }

        private void LoadBacklog() {
            if (!this._plugin.Config.BacklogEnabled) return;
            var dir = this.GetBacklogDirectory();
            if (!Directory.Exists(dir)) return;
            try {
                var loaded = new List<ServerMessage>();
                // 新格式：每角色一个文件（头部带角色标识）
                foreach (var path in Directory.EnumerateFiles(dir, "chat_backlog_*.bin")) {
                    try {
                        var file = MessagePack.MessagePackSerializer.Deserialize<BacklogFile>(File.ReadAllBytes(path));
                        if (file == null || string.IsNullOrEmpty(file.CharacterTag)) continue;
                        foreach (var m in file.Messages) {
                            // 无角色标识的消息归属不明，丢弃以防串号
                            if (m == null || string.IsNullOrEmpty(m.CharacterTag)) continue;
                            loaded.Add(m);
                        }
                    } catch (Exception ex) {
                        Plugin.Log.Error($"Could not load backlog from '{path}': {ex.Message}");
                    }
                }
                // 旧格式单文件兼容：仅当还没有任何分角色文件时读取并迁移，读完删除旧文件，避免每次启动重复合并
                var hasPerCharFiles = Directory.EnumerateFiles(dir, "chat_backlog_*.bin").Any();
                var legacyPath = Path.Combine(dir, "chat_backlog.bin");
                if (!hasPerCharFiles && File.Exists(legacyPath)) {
                    try {
                        var legacy = MessagePack.MessagePackSerializer.Deserialize<List<ServerMessage>>(File.ReadAllBytes(legacyPath));
                        foreach (var m in legacy) {
                            if (m == null || string.IsNullOrEmpty(m.CharacterTag)) continue;
                            loaded.Add(m);
                        }
                        File.Delete(legacyPath);
                    } catch (Exception ex) {
                        Plugin.Log.Error($"Could not load legacy backlog from '{legacyPath}': {ex.Message}");
                    }
                }
                var configured = this._plugin.Config.BacklogPath?.Trim();
                if (!string.IsNullOrWhiteSpace(configured) && File.Exists(configured) && !configured.Equals(legacyPath, StringComparison.OrdinalIgnoreCase) && !hasPerCharFiles) {
                    try {
                        var legacy2 = MessagePack.MessagePackSerializer.Deserialize<List<ServerMessage>>(File.ReadAllBytes(configured));
                        foreach (var m in legacy2) {
                            if (m == null || string.IsNullOrEmpty(m.CharacterTag)) continue;
                            loaded.Add(m);
                        }
                        File.Delete(configured);
                    } catch (Exception ex) {
                        Plugin.Log.Error($"Could not load legacy backlog from '{configured}': {ex.Message}");
                    }
                }
                lock (this._backlogLock) {
                    this._backlogByChar.Clear();
                    foreach (var m in loaded.OrderBy(x => x.Timestamp)) {
                        if (string.IsNullOrEmpty(m.CharacterTag)) continue;
                        if (!this._backlogByChar.TryGetValue(m.CharacterTag, out var charBacklog)) {
                            charBacklog = new LinkedList<ServerMessage>();
                            this._backlogByChar[m.CharacterTag] = charBacklog;
                        }
                        charBacklog.AddLast(m);
                        while (charBacklog.Count > this._plugin.Config.BacklogCount) {
                            charBacklog.RemoveFirst();
                        }
                    }
                }
                Plugin.Log.Info($"Loaded {loaded.Count} backlog messages from '{dir}'");
            } catch (Exception ex) {
                Plugin.Log.Error($"Could not load backlog from '{dir}': {ex.Message}");
            }
        }

        private string? CurrentCharacterTag() {
            var player = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer;
            if (player == null) return null;
            var name = player.Name.TextValue;
            try {
                var world = player.HomeWorld.Value.Name.ExtractText();
                if (!string.IsNullOrWhiteSpace(world)) return $"{name}@{world}";
            } catch {
                // ignore, fall back to name-only
            }
            return name;
        }

        // 判定一条 backlog 消息是否属于当前角色。新格式是 名字@服务器；
        // 旧格式只有名字（不含@），仅当与当前角色名字一致时才归入（兼容旧文件）。
        private static bool MatchesCurrentCharacter(ServerMessage m, string? fullTag) {
            if (string.IsNullOrEmpty(fullTag)) return false;
            if (string.IsNullOrEmpty(m.CharacterTag)) return false; // 归属不明，丢弃防串号
            var mTag = m.CharacterTag.ToLowerInvariant();
            var full = fullTag.ToLowerInvariant();
            if (mTag == full) return true;
            var at = full.IndexOf('@');
            if (at > 0 && mTag == full.Substring(0, at)) return true;
            return false;
        }

        // 定位某角色的内存历史缓冲：优先 名字@服务器，兼容旧格式纯名字
        private LinkedList<ServerMessage>? FindBacklogList(string? fullTag) {
            if (string.IsNullOrEmpty(fullTag)) return null;
            if (this._backlogByChar.TryGetValue(fullTag, out var list)) return list;
            var at = fullTag.IndexOf('@');
            if (at > 0 && this._backlogByChar.TryGetValue(fullTag.Substring(0, at), out var legacy)) return legacy;
            return null;
        }
        private static int? GetLastSenderIcon(byte[] sender) {
            if (sender == null) return null;
            int? last = null;
            try {
                using var reader = new System.IO.BinaryReader(new System.IO.MemoryStream(sender));
                while (reader.BaseStream.Position < reader.BaseStream.Length) {
                    var b = reader.ReadByte();
                    if (b != 0x02) continue;
                    if (reader.BaseStream.Position >= reader.BaseStream.Length) break;
                    var kind = reader.ReadByte();
                    if (reader.BaseStream.Position >= reader.BaseStream.Length) break;
                    var len = (int) XIVChatCommon.XivString.GetInteger(reader);
                    if (kind == 0x12) {
                        var d = new System.IO.BinaryReader(new System.IO.MemoryStream(reader.ReadBytes(len)));
                        last = (int) XIVChatCommon.XivString.GetInteger(d);
                    } else {
                        reader.ReadBytes(len);
                    }
                    if (reader.BaseStream.Position < reader.BaseStream.Length) reader.ReadByte();
                }
            } catch { }
            return last;
        }
        private static int? GetSenderStatusIcon(byte[] sender) {
            if (sender == null) return null;
            try {
                using var reader = new System.IO.BinaryReader(new System.IO.MemoryStream(sender));
                while (reader.BaseStream.Position < reader.BaseStream.Length) {
                    var b = reader.ReadByte();
                    if (b != 0x02) continue;
                    var kind = reader.ReadByte();
                    if (reader.BaseStream.Position >= reader.BaseStream.Length) break;
                    var len = (int) XIVChatCommon.XivString.GetInteger(reader);
                    if (kind == 0x12) {
                        var d = new System.IO.BinaryReader(new System.IO.MemoryStream(reader.ReadBytes(len)));
                        return (int) XIVChatCommon.XivString.GetInteger(d);
                    }
                    reader.ReadBytes(len);
                    if (reader.BaseStream.Position < reader.BaseStream.Length) reader.ReadByte();
                }
            } catch { }
            return null;
        }

        private static IEnumerable<Chunk> ToChunks(SeString msg, uint? defaultColour) {
            var chunks = new List<Chunk>();

            var italic = false;
            var foreground = new Stack<uint>();
            var glow = new Stack<uint>();
            string? lastItemName = null;

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
                            foreground.Push(foregroundPayload.UIColor.Value.Dark);
                        } else if (foreground.Count > 0) {
                            foreground.Pop();
                        }

                        break;
                    case PayloadType.UIGlow:
                        var glowPayload = (UIGlowPayload) payload;
                        if (glowPayload.IsEnabled) {
                            glow.Push(glowPayload.UIColor.Value.Light);
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
                            index = (int) index,
                        });
                        break;
                    case PayloadType.Item:
                        var itemPayload = (ItemPayload) payload;
                        var itemRow = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Item>().GetRowOrDefault(itemPayload.RawItemId);
                        chunks.Add(new IconChunk {
                            index = 0xE0BB,
                        });
                        var itemName = itemPayload.DisplayName ?? string.Empty;
                        Append(itemName);
                        lastItemName = itemName;
                        break;
                    case PayloadType.Unknown:
                        var rawPayload = (RawPayload) payload;
                        if (rawPayload.Data[1] == 0x13) {
                            if (foreground.Count > 0) {
                                foreground.Pop();
                            }

                            if (glow.Count > 0) {
                                glow.Pop();
                            }
                        }

                        break;
                    default:
                        if (payload is ITextProvider textProvider) {
                            var rawText = textProvider.Text;
                            if (lastItemName != null && rawText == lastItemName) {
                                lastItemName = null;
                            } else {
                                Append(rawText);
                            }
                        } else {
                            lastItemName = null;
                        }

                        break;
                }
            }

            return chunks;
        }

        private IEnumerable<ServerMessage> MessagesAfter(DateTime time) {
            var tag = this.CurrentCharacterTag();
            var charBacklog = this.FindBacklogList(tag);
            if (charBacklog == null) return Array.Empty<ServerMessage>();
            return charBacklog.Where(msg => msg.Timestamp > time && MatchesCurrentCharacter(msg, tag)).ToArray();
        }

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
                    prefix = input[..space];
                    // handle wrapping tells
                    if (prefix is "/tell" or "/t") {
                        var tellSpace = input.IndexOfCount(' ', 3);
                        if (tellSpace != -1) {
                            prefix = input[..tellSpace];
                            input = input[(tellSpace + 1)..];
                        }
                    } else {
                        input = input[(space + 1)..];
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

        private void BroadcastMessage(Encodable message, ClientPreference preference) {
            foreach (var client in this.Clients.Values) {
                if (client.GetPreference(preference, false)) {
                    client.Queue.Writer.TryWrite(message);
                }
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

            return XIVChatPlugin.Plugin.DataManager.GetExcelSheet<LogFilter>().GetRowOrDefault(rowId)?.Name.ExtractText() ?? string.Empty;
        }

        internal void OnChatChannelChange(uint channel, SeString name) {
            // for now, to avoid changing the protocol further, convert crossworld icon into font icon
            for (var i = 0; i < name.Payloads.Count; i++) {
                var payload = name.Payloads[i];
                if (payload is IconPayload { Icon: BitmapFontIcon.CrossWorld }) {
                    name.Payloads[i] = new TextPayload("\ue05d");
                }
            }

            var inputChannel = (InputChannel) channel;
            if (inputChannel == this._currentChannel && name.Encode().SequenceEqual(this._currentChannelName?.Encode() ?? [])) {
                return;
            }

            this._currentChannel = inputChannel;
            this._currentChannelName = name;

            var msg = new ServerChannel(inputChannel, name.TextValue);
            this.BroadcastMessage(msg);
        }

        private void BroadcastAvailability(bool available) {
            this.BroadcastMessage(new Availability(available));
        }

        private PlayerData? GeneratePlayerData() {
            var player = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer;
            if (player == null) {
                return null;
            }

            var homeWorld = player.HomeWorld.Value.Name.ExtractText();
            var currentWorld = player.CurrentWorld.Value.Name.ExtractText();
            var territory = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<TerritoryType>().GetRowOrDefault(XIVChatPlugin.Plugin.ClientState.TerritoryType);
            var location = territory?.PlaceName.Value.Name.ExtractText() ?? "???";
            var name = player.Name.TextValue;
            var classJobId = player.ClassJob.RowId;
            var jobName = classJobId == 0
                ? string.Empty
                : XIVChatPlugin.Plugin.DataManager.GetExcelSheet<ClassJob>().GetRowOrDefault(classJobId)?.Name.ExtractText() ?? string.Empty;

            // Aggregate item level from equipped items.
            var ilvl = 0;
            var ilvlCount = 0;
            try {
                var itemSheet = XIVChatPlugin.Plugin.DataManager.GetExcelSheet<Item>();
                foreach (var it in XIVChatPlugin.Plugin.GameInventory.GetInventoryItems(GameInventoryType.EquippedItems)) {
                    if (it.IsEmpty || it.ItemId == 0) {
                        continue;
                    }
                    var row = itemSheet.GetRowOrDefault(it.ItemId);
                    if (row == null) {
                        continue;
                    }
                    ilvl += (int) row.Value.LevelItem.RowId;
                    ilvlCount++;
                }
            } catch (Exception ex) {
                Plugin.Log.Warning($"Could not compute item level: {ex.Message}");
            }
            if (ilvlCount > 0) {
                ilvl /= ilvlCount;
            }

            return new PlayerData(
                homeWorld, currentWorld, location, name, classJobId, jobName, player.Level,
                XIVChatPlugin.Plugin.ClientState.TerritoryType,
                (int) player.CurrentHp, (int) player.MaxHp, (int) player.CurrentMp, (int) player.MaxMp,
                currentCp: (int) player.MaxCp, maxCp: (int) player.MaxCp,
                currentGp: (int) player.MaxGp, maxGp: (int) player.MaxGp,
                itemLevel: ilvl);
        }

        private void BroadcastPlayerData() {
            var playerData = (Encodable?) this.GeneratePlayerData() ?? EmptyPlayerData.Instance;

            this.BroadcastMessage(playerData);
        }

        internal void OnLogIn() {
            this._logoutObserved = false;
            this._lastGameAvailability = true;
            this.BroadcastAvailability(true);
            // send player data on next framework update
            this._sendPlayerData = true;
        }

        internal void OnLogOut(int type, int code) {
            this._logoutObserved = true;
            this._sendPlayerData = false;
            this._lastGameAvailability = false;
            this.BroadcastAvailability(false);
            // Do not generate a profile here: ObjectTable may still contain the
            // departing player for a few frames after the title screen appears.
            this.BroadcastMessage(EmptyPlayerData.Instance);
        }

        internal void OnTerritoryChange(uint @uint) => this._sendPlayerData = true;

        public void Dispose() {
            this._activityTracker.Dispose();
            this._purchaseResponseHook?.Dispose();
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
                    await client.TokenSource.CancelAsync();
                });
            }

            this._plugin.Functions.ReceiveFriendList -= this.OnReceiveFriendList;
            this.PersistBacklog();
        }
    }

    [MessagePackObject]
    public class BacklogFile {
        [Key(0)]
        public string? CharacterTag { get; set; }

        [Key(1)]
        public long WrittenAtUnixMs { get; set; }

        [Key(2)]
        public List<ServerMessage> Messages { get; set; } = [];
    }

    internal static class TcpListenerExt {
        internal static async Task<TcpClient?> GetTcpClient(this TcpListener listener, CancellationTokenSource source) {
            await using (source.Token.Register(listener.Stop)) {
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
