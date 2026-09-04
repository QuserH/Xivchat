using MessagePack;

namespace XIVChatCommon.Message.Client {
    /// <summary>
    /// One price-monitor rule. The plugin re-queries the board for this item on a
    /// timer (the same native item-search proxy the manual queries use, so the two
    /// never overlap) and compares the cheapest matching listing against
    /// <see cref="PriceThreshold"/>.
    /// </summary>
    [MessagePackObject]
    public sealed class ClientMarketMonitorEntry {
        [Key(0)] public uint ItemId { get; set; }

        /// <summary>Trigger (and auto-buy) when a listing's unit price is at or below this.</summary>
        [Key(1)] public uint PriceThreshold { get; set; }

        /// <summary>Only consider HQ listings.</summary>
        [Key(2)] public bool HqOnly { get; set; }

        /// <summary>Buy matching listings automatically, cheapest first.</summary>
        [Key(3)] public bool AutoBuy { get; set; }

        /// <summary>
        /// Total quantity this rule may ever auto-buy; 0 means unlimited. The plugin
        /// accumulates what it has already bought into this budget so a mistyped
        /// threshold cannot drain the wallet in a loop.
        /// </summary>
        [Key(4)] public uint BuyCap { get; set; }
    }

    /// <summary>
    /// Replaces the plugin's whole monitor list with the entries carried here. A
    /// full-list replace (rather than add/remove deltas) means the phone is the
    /// source of truth and a lost message cannot leave the two sides disagreeing.
    /// </summary>
    [MessagePackObject]
    public sealed class ClientMarketMonitorSync : Encodable {
        protected override byte Code => (byte) ClientOperation.MarketMonitorSync;

        [Key(0)] public ClientMarketMonitorEntry[] Entries { get; set; } = [];

        public static ClientMarketMonitorSync Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ClientMarketMonitorSync>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
