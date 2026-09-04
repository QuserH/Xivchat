using MessagePack;

namespace XIVChatCommon.Message.Client {
    /// <summary>
    /// Phone asks the game client to query the market board for one item.
    ///
    /// Read-only. Buying is a separate opcode (<see cref="ClientMarketPurchase"/>)
    /// so that browsing can never spend gil by accident.
    /// </summary>
    [MessagePackObject]
    public class ClientMarketSearch : Encodable {
        protected override byte Code => (byte) ClientOperation.MarketSearch;

        [Key(0)]
        public uint ItemId { get; set; }

        /// <summary>
        /// Restrict to HQ. Ignored by the game for items that cannot be HQ, so the
        /// plugin clears it rather than sending a query that returns nothing.
        /// </summary>
        [Key(1)]
        public bool HqOnly { get; set; }

        public static ClientMarketSearch Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ClientMarketSearch>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
