using MessagePack;

namespace XIVChatCommon.Message.Server {
    /// <summary>Why a market query produced no listings.</summary>
    public enum MarketStatus : byte {
        Ok = 0,

        /// <summary>Not logged in, or no character available.</summary>
        NotLoggedIn = 1,

        /// <summary>
        /// In a duty. The game refuses market queries there, so the plugin does not
        /// even try.
        /// </summary>
        InDuty = 2,

        /// <summary>
        /// The player has the market board window open on the PC. Both sides drive
        /// the same InfoProxyItemSearch state, so querying would fight the UI.
        /// </summary>
        BoardOpen = 3,

        /// <summary>Item has no market category (untradable).</summary>
        NotMarketable = 4,

        /// <summary>The game accepted the request but returned nothing in time.</summary>
        Timeout = 5,
    }

    /// <summary>
    /// One live listing as the game client reports it.
    ///
    /// No world field: the game's MarketBoardListing struct has none. World is only
    /// available through the market board UI's string array, which this path cannot
    /// read (it requires that window to be closed). <see cref="ServerMarket.CurrentWorldName"/>
    /// labels the whole result instead.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerMarketListing {
        [Key(0)] public ulong ListingId { get; set; }
        [Key(1)] public uint UnitPrice { get; set; }
        [Key(2)] public uint Quantity { get; set; }
        [Key(3)] public bool IsHq { get; set; }

        /// <summary>
        /// Retainer city, e.g. 利姆萨·罗敏萨. Was RetainerName, which was always empty:
        /// the struct's only string is CharacterName, populated solely for set sales.
        /// The city is present on every listing, so it takes the slot.
        /// </summary>
        [Key(4)] public string TownName { get; set; } = string.Empty;

        /// <summary>Total tax on this listing in gil. Not included in unit price.</summary>
        [Key(5)] public uint Tax { get; set; }

        /// <summary>Sold as a set, so quantity cannot be split.</summary>
        [Key(6)] public bool IsSet { get; set; }

        /// <summary>Melded materia count; buyers price these differently.</summary>
        [Key(7)] public byte MateriaCount { get; set; }
    }

    /// <summary>
    /// Live market board listings for one item, read from the game client.
    ///
    /// Always carries <see cref="Status"/> so the phone can say *why* a list is
    /// empty instead of showing a blank table.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerMarket : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public uint ItemId { get; set; }
        [Key(2)] public MarketStatus Status { get; set; }
        [Key(3)] public ServerMarketListing[] Listings { get; set; } = [];

        /// <summary>World the character is on, so the phone can label the source.</summary>
        [Key(4)] public string CurrentWorldName { get; set; } = string.Empty;

        /// <summary>
        /// What NPC shops charge for this item (game <c>Item.PriceMid</c>), 0 when no
        /// shop sells it. The phone draws this as a benchmark line: listings above it
        /// are more than just walking to a vendor and buying.
        /// </summary>
        [Key(5)] public uint NpcPrice { get; set; }

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.Market;

        public ServerMarket() {
        }

        public ServerMarket(long updatedUnix, uint itemId, MarketStatus status,
            ServerMarketListing[] listings, string currentWorldName, uint npcPrice = 0) {
            this.UpdatedUnix = updatedUnix;
            this.ItemId = itemId;
            this.Status = status;
            this.Listings = listings;
            this.CurrentWorldName = currentWorldName;
            this.NpcPrice = npcPrice;
        }

        public static ServerMarket Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerMarket>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
