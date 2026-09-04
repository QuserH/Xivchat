using MessagePack;

namespace XIVChatCommon.Message.Server {
    /// <summary>
    /// One marketable item within a category.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerMarketItem {
        [Key(0)] public uint ItemId { get; set; }
        [Key(1)] public string Name { get; set; } = string.Empty;
        [Key(2)] public uint IconId { get; set; }
        [Key(3)] public byte LevelItem { get; set; }
        [Key(4)] public bool CanBeHq { get; set; }

        /// <summary>
        /// Gil price at an NPC shop, or zero when this item is not actually sold
        /// for gil by any GilShopItem row.  Appended as a wire field so older
        /// clients can continue to decode the first five fields.
        /// </summary>
        [Key(5)] public uint NpcPrice { get; set; }
    }

    /// <summary>
    /// One item search subcategory (e.g., "Legs" under "Armor").
    /// </summary>
    [MessagePackObject]
    public sealed class ServerMarketSubcategory {
        [Key(0)] public byte CategoryId { get; set; }
        [Key(1)] public string Name { get; set; } = string.Empty;
        [Key(2)] public byte Order { get; set; }
        [Key(3)] public ServerMarketItem[] Items { get; set; } = [];

        /// <summary>
        /// Game <c>ItemSearchCategory.Icon</c>, the same art the in-game market
        /// board grid uses. Appended so plugins that only read keys 0-3 still decode.
        /// </summary>
        [Key(4)] public uint IconId { get; set; }
    }

    /// <summary>
    /// Top-level market board category (e.g., "Weapons", "Armor").
    /// </summary>
    [MessagePackObject]
    public sealed class ServerMarketCategory {
        [Key(0)] public byte CategoryId { get; set; }
        [Key(1)] public string Name { get; set; } = string.Empty;
        [Key(2)] public byte Order { get; set; }
        [Key(3)] public ServerMarketSubcategory[] Subcategories { get; set; } = [];

        /// <summary>Game <c>ItemSearchCategory.Icon</c>; see the subcategory note.</summary>
        [Key(4)] public uint IconId { get; set; }
    }

    /// <summary>
    /// Full market board category tree with all tradable items, sent in reply to
    /// <see cref="Client.ClientOperation.MarketCategories"/>.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerMarketCategories : Encodable {
        [Key(0)] public ServerMarketCategory[] Categories { get; set; } = [];

        /// <summary>
        /// Unix timestamp (milliseconds) when this category data was built.
        /// Phone compares this with its cached timestamp to skip re-downloading.
        /// Added as Key(1) so old clients (0.7.x) that only read Key(0) still work.
        /// </summary>
        [Key(1)] public long TimestampMs { get; set; }

        /// <summary>
        /// Game version this data was built for (e.g., "2024.01.00.0000.0000").
        /// Added as Key(2) for future version-aware cache invalidation.
        /// </summary>
        [Key(2)] public string GameVersion { get; set; } = string.Empty;

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.MarketCategories;

        public ServerMarketCategories() {
        }

        public ServerMarketCategories(ServerMarketCategory[] categories, long timestampMs = 0, string gameVersion = "") {
            this.Categories = categories;
            this.TimestampMs = timestampMs;
            this.GameVersion = gameVersion;
        }

        public static ServerMarketCategories Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerMarketCategories>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
