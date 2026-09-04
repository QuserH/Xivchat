using Dalamud.Plugin.Services;
using Lumina.Excel.Sheets;
using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.Json;
using XIVChatPlugin;

namespace XIVChatPlugin {
    /// <summary>
    /// Caches market metadata (categories, gil shop items) globally across all characters.
    /// Data is version-stamped and only rebuilt when the game version changes.
    /// </summary>
    internal class MarketDataCache {
        private const int CacheFormatVersion = 2;

        private readonly string _cacheDir;
        private readonly string _cacheFilePath;
        private readonly IDataManager _dataManager;

        private CachedMarketData? _cache;
        private string? _currentGameVersion;
        private readonly object _gilShopGate = new();
        private long _lastGilShopRefreshMs;

        /// <summary>Item IDs that can be bought from NPC vendors with gil.</summary>
        public IReadOnlySet<uint> GilShopItemIds => _cache?.GilShopItems ?? new HashSet<uint>();

        /// <summary>
        /// Whether the GilShopItem sheet has produced a non-empty index.  A zero-sized
        /// index is not a valid "there are no NPC prices" answer: it is also what the
        /// Dalamud service exposes for a short window while Excel data is still loading.
        /// Consumers use this to avoid freezing an all-zero category snapshot forever.
        /// </summary>
        public bool GilShopDataReady => _cache?.GilShopItems.Count > 0;

        /// <summary>
        /// Changes whenever the gil-shop index is loaded/reloaded.  The server keeps this
        /// beside its category snapshot, so a tree built during early startup is rebuilt
        /// once the real vendor rows become available.
        /// </summary>
        public long GilShopDataTimestampMs => _cache?.TimestampMs ?? 0L;

        /// <summary>
        /// Rebuilds the gil-shop index lazily when the plugin was constructed before
        /// Dalamud finished loading the Excel sheets. An empty index is not persisted as a
        /// valid result: otherwise NPC prices would stay hidden until the next game version.
        /// </summary>
        public void EnsureGilShopData() {
            // Excel can finish loading after the plugin constructor.  Refresh the cheap
            // version proxy before deciding that an existing on-disk index is still
            // current; otherwise a cache created while the sheet was empty can survive
            // a patch/login for the whole process lifetime.
            var observedVersion = GetGameVersion();
            if (!string.IsNullOrWhiteSpace(observedVersion) && observedVersion != "unknown"
                && observedVersion != _currentGameVersion) {
                lock (_gilShopGate) {
                    if (observedVersion != _currentGameVersion) {
                        _currentGameVersion = observedVersion;
                        if (_cache != null && _cache.GameVersion != observedVersion) {
                            _cache = null;
                        }
                    }
                }
            }

            if (_cache?.GilShopItems.Count > 0) return;
            lock (_gilShopGate) {
                if (_cache?.GilShopItems.Count > 0) return;
                var now = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
                if (now - _lastGilShopRefreshMs < 30_000) return;
                _lastGilShopRefreshMs = now;
                HashSet<uint> items;
                try {
                    items = BuildGilShopItemSet();
                } catch (Exception ex) {
                    Plugin.Log.Warning($"[MarketDataCache] Could not refresh gil shops: {ex.Message}");
                    return;
                }
                if (items.Count == 0) {
                    Plugin.Log.Warning("[MarketDataCache] GilShopItem sheet is not ready; keeping NPC prices unavailable for now");
                    return;
                }
                _cache ??= new CachedMarketData {
                    FormatVersion = CacheFormatVersion,
                    GameVersion = _currentGameVersion ?? "unknown",
                    Categories = new List<MarketCategoryNode>(),
                };
                _cache.GameVersion = _currentGameVersion ?? "unknown";
                _cache.GilShopItems = items;
                _cache.TimestampMs = now;
                PersistCache();
                Plugin.Log.Information($"[MarketDataCache] Lazily loaded {items.Count} gil-shop items");
            }
        }

        /// <summary>Market board category tree.</summary>
        public List<MarketCategoryNode> Categories => _cache?.Categories ?? new();

        /// <summary>Timestamp when this cache was built (UTC Unix milliseconds).</summary>
        public long CacheTimestampMs => _cache?.TimestampMs ?? 0;

        /// <summary>Game version this cache was built for.</summary>
        public string CacheGameVersion => _cache?.GameVersion ?? "";

        public MarketDataCache(IDataManager dataManager, string pluginConfigDir) {
            _dataManager = dataManager;
            _cacheDir = Path.Combine(pluginConfigDir, "MarketCache");
            _cacheFilePath = Path.Combine(_cacheDir, "market_metadata.json");

            Directory.CreateDirectory(_cacheDir);
        }

        /// <summary>
        /// Load cache from disk if it matches current game version, otherwise rebuild.
        /// Call this once during plugin initialization.
        /// </summary>
        public void Initialize() {
            try {
                _currentGameVersion = GetGameVersion();

                if (File.Exists(_cacheFilePath)) {
                    var json = File.ReadAllText(_cacheFilePath);
                    var cached = JsonSerializer.Deserialize<CachedMarketData>(json);

                    if (cached != null
                        && cached.FormatVersion == CacheFormatVersion
                        && cached.GameVersion == _currentGameVersion) {
                        _cache = cached;
                        Plugin.Log.Information($"[MarketDataCache] Loaded from disk: {cached.GilShopItems.Count} gil items, {cached.Categories.Count} categories (game v{cached.GameVersion})");
                        return;
                    }

                    Plugin.Log.Information($"[MarketDataCache] Version mismatch (disk: {cached?.GameVersion}, current: {_currentGameVersion}), rebuilding...");
                }

                RebuildCache();
            } catch (Exception ex) {
                Plugin.Log.Error($"[MarketDataCache] Failed to initialize: {ex}");
                _cache = new CachedMarketData {
                    FormatVersion = CacheFormatVersion,
                    GameVersion = _currentGameVersion ?? "unknown",
                    TimestampMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                    GilShopItems = new HashSet<uint>(),
                    Categories = new List<MarketCategoryNode>()
                };
            }
        }

        private void RebuildCache() {
            var stopwatch = System.Diagnostics.Stopwatch.StartNew();

            var gilItems = BuildGilShopItemSet();
            var categories = BuildCategoryTree();

            _cache = new CachedMarketData {
                FormatVersion = CacheFormatVersion,
                GameVersion = _currentGameVersion ?? "unknown",
                TimestampMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds(),
                GilShopItems = gilItems,
                Categories = categories
            };

            // Do not freeze an empty sheet into the disk cache. Dalamud can expose the
            // service before Excel data is ready; EnsureGilShopData will fill it later.
            if (gilItems.Count == 0) {
                Plugin.Log.Warning("[MarketDataCache] Built no gil-shop items; cache will be retried lazily");
                return;
            }
            try {
                PersistCache();

                stopwatch.Stop();
                Plugin.Log.Information($"[MarketDataCache] Built and saved: {gilItems.Count} gil items, {categories.Count} categories in {stopwatch.ElapsedMilliseconds}ms");
            } catch (Exception ex) {
                Plugin.Log.Error($"[MarketDataCache] Failed to save cache: {ex}");
            }
        }

        private void PersistCache() {
            if (_cache == null) return;
            try {
                var json = JsonSerializer.Serialize(_cache, new JsonSerializerOptions {
                    WriteIndented = false
                });
                File.WriteAllText(_cacheFilePath, json);
            } catch (Exception ex) {
                Plugin.Log.Error($"[MarketDataCache] Failed to save cache: {ex}");
            }
        }

        private HashSet<uint> BuildGilShopItemSet() {
            var result = new HashSet<uint>();
            var gilSheet = _dataManager.GetSubrowExcelSheet<GilShopItem>();

            if (gilSheet != null) {
                foreach (var shopRow in gilSheet) {
                    foreach (var row in shopRow) {
                        var itemId = row.Item.RowId;
                        if (itemId != 0) {
                            result.Add(itemId);
                        }
                    }
                }
            }

            return result;
        }

        private List<MarketCategoryNode> BuildCategoryTree() {
            // TODO: Implement category tree building from ItemSearchCategory sheet
            // For now return empty list - this will be populated in a follow-up
            return new List<MarketCategoryNode>();
        }

        private string GetGameVersion() {
            // Use Item sheet row count as a proxy for game version
            // This changes only when new items are added (patch updates)
            try {
                var itemSheet = _dataManager.GetExcelSheet<Item>();
                if (itemSheet != null) {
                    var count = 0;
                    foreach (var _ in itemSheet) count++;
                    return $"items_{count}";
                }
                return "unknown";
            } catch {
                return "unknown";
            }
        }
    }

    [Serializable]
    internal class CachedMarketData {
        public int FormatVersion { get; set; }
        public string GameVersion { get; set; } = "";
        public long TimestampMs { get; set; }
        public HashSet<uint> GilShopItems { get; set; } = new();
        public List<MarketCategoryNode> Categories { get; set; } = new();
    }

    [Serializable]
    internal class MarketCategoryNode {
        public uint CategoryId { get; set; }
        public string Name { get; set; } = "";
        public List<uint> ItemIds { get; set; } = new();
        public List<MarketCategoryNode> Children { get; set; } = new();
    }
}
