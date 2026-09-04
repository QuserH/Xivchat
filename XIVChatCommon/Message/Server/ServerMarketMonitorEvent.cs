using MessagePack;
using System;

namespace XIVChatCommon.Message.Server {
    /// <summary>What kind of thing happened to a monitored item.</summary>
    public enum MarketMonitorEventKind : byte {
        /// <summary>A listing at or below the threshold is on the board now.</summary>
        Found = 0,

        /// <summary>The plugin bought a listing automatically.</summary>
        Purchased = 1,

        /// <summary>An automatic purchase was attempted and refused.</summary>
        BuyFailed = 2,

        /// <summary>Auto-buy skipped: the entry has spent its quantity cap.</summary>
        CapReached = 3,

        /// <summary>The phone replaced the monitor list; detail echoes the count.</summary>
        Sync = 4,
    }

    /// <summary>
    /// One price-monitor event, broadcast to every connected client. Monitoring runs
    /// inside the plugin on a timer, so unlike <see cref="ServerMarket"/> this is
    /// unsolicited: the phone did not ask for it.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerMarketMonitorEvent : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public uint ItemId { get; set; }
        [Key(2)] public MarketMonitorEventKind Kind { get; set; }

        /// <summary>Unit price of the cheapest matching listing, or of the purchase.</summary>
        [Key(3)] public uint Price { get; set; }

        /// <summary>Quantity of the cheapest listing / bought stack.</summary>
        [Key(4)] public uint Quantity { get; set; }

        /// <summary>Human-readable context: refusal reason, match count, etc.</summary>
        [Key(5)] public string Detail { get; set; } = string.Empty;

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.MarketMonitor;

        public ServerMarketMonitorEvent() {
        }

        public ServerMarketMonitorEvent(uint itemId, MarketMonitorEventKind kind,
            uint price = 0, uint quantity = 0, string detail = "") {
            this.UpdatedUnix = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();
            this.ItemId = itemId;
            this.Kind = kind;
            this.Price = price;
            this.Quantity = quantity;
            this.Detail = detail;
        }

        public static ServerMarketMonitorEvent Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerMarketMonitorEvent>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
