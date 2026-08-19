using MessagePack;
using Sodium;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.Linq;
using System.Net;
using System.Net.Sockets;
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
using Dalamud.Utility;
using Lumina.Excel.Sheets;
using XIVChatCommon;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Client;
using XIVChatCommon.Message.Server;
using Dalamud.Game.Chat;
using FFXIVClientStructs.FFXIV.Client.Game;
using FFXIVClientStructs.FFXIV.Client.Game.UI;
using FFXIVClientStructs.FFXIV.Client.Graphics.Environment;
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

        private readonly Stopwatch _sendWatch = new();
        private readonly Stopwatch _inventoryWatch = new();
        private readonly Stopwatch _walletWatch = new();
        private readonly Stopwatch _weatherWatch = new();
        private readonly Stopwatch _jobsWatch = new();
        private readonly Stopwatch _dailiesWatch = new();
        private readonly Stopwatch _collectionsWatch = new();
        private readonly Stopwatch _fishingWatch = new();
        private readonly PhoneActivityTracker _activityTracker;

        private readonly CancellationTokenSource _tokenSource = new();
        private readonly ConcurrentQueue<string> _toGame = new();
        private readonly ConcurrentQueue<ClientFriendAction> _friendActions = new();
        private readonly ConcurrentQueue<int> _jobsActions = new();

        private readonly ConcurrentDictionary<Guid, BaseClient> _clients = new();
        internal IReadOnlyDictionary<Guid, BaseClient> Clients => this._clients;
        internal readonly Channel<Tuple<BaseClient, Channel<bool>>> PendingClients = Channel.CreateUnbounded<Tuple<BaseClient, Channel<bool>>>();

        private readonly HashSet<Guid> _waitingForFriendList = [];

        private readonly LinkedList<ServerMessage> _backlog = [];

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

            this._lastHousingLocation = this._plugin.Functions.HousingLocation;

            this._sendWatch.Start();
            this._inventoryWatch.Start();
            this._walletWatch.Start();
            this._weatherWatch.Start();
            this._jobsWatch.Start();
            this._dailiesWatch.Start();
            this._collectionsWatch.Start();
            this._fishingWatch.Start();
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

            if (message.Sender.Payloads.Count > 0) {
                var format = this.FormatFor(message.LogKind);
                if (format is { IsPresent: true }) {
                    chunks.Add(new TextChunk(format.Before) {
                        FallbackColour = colour,
                    });
                    chunks.AddRange(ToChunks(message.Sender, colour));
                    chunks.Add(new TextChunk(format.After) {
                        FallbackColour = colour,
                    });
                }
            }

            chunks.AddRange(ToChunks(message.Message, colour));

            var msg = new ServerMessage(
                DateTime.UtcNow,
                (ushort) message.LogKind,
                message.Sender.Encode(),
                message.Message.Encode(),
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

        internal unsafe void OnFrameworkUpdate(IFramework framework) {
            var player = XIVChatPlugin.Plugin.ObjectTable.LocalPlayer;
            var gameAvailable = XIVChatPlugin.Plugin.ClientState.IsLoggedIn && player != null;
            if (this._lastGameAvailability != gameAvailable) {
                this._lastGameAvailability = gameAvailable;
                this.BroadcastAvailability(gameAvailable);
                if (!gameAvailable) {
                    this._sendPlayerData = false;
                    this.BroadcastMessage(EmptyPlayerData.Instance);
                }
            }
            if (gameAvailable && this._sendPlayerData) {
                this.BroadcastPlayerData();
                this._sendPlayerData = false;
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
            this.UpdateActivity();

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
                            XIVChatPlugin.Plugin.ChatGui.PrintError($"[{Plugin.Name}] 请打开一次游戏内好友列表以启用好友列表功能。通常只需在首次安装或更新后执行一次。");
                        }
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
                    this._plugin.Functions.ChangeChatChannel(channel.Channel);

                    break;
                case ClientOperation.FriendAction:
                    this._friendActions.Enqueue(ClientFriendAction.Decode(payload));
                    break;
                case ClientOperation.JobsAction:
                    this._jobsActions.Enqueue(ClientJobsAction.Decode(payload).GearsetId);
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
                            index = itemRow?.Icon ?? 0,
                        });
                        Append(itemPayload.DisplayName ?? string.Empty);
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
            this._lastGameAvailability = true;
            this.BroadcastAvailability(true);
            // send player data on next framework update
            this._sendPlayerData = true;
        }

        internal void OnLogOut(int type, int code) {
            this._lastGameAvailability = false;
            this.BroadcastAvailability(false);
            this.BroadcastPlayerData();
        }

        internal void OnTerritoryChange(uint @uint) => this._sendPlayerData = true;

        public void Dispose() {
            this._activityTracker.Dispose();
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
        }
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
