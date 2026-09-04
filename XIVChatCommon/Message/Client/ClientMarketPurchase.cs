using MessagePack;

namespace XIVChatCommon.Message.Client {
    /// <summary>
    /// Phone asks the game client to buy one market board listing.
    ///
    /// This spends real gil and cannot be undone, so the request carries what the
    /// phone *showed the player* rather than a row index. The plugin re-queries the
    /// board, finds the listing by <see cref="ListingId"/>, and refuses unless the
    /// price, quantity and HQ flag still match. A row index would silently buy the
    /// wrong listing whenever the board changed between display and tap; a listing id
    /// plus an expected price cannot.
    ///
    /// The board sells whole listings only, so there is no quantity to choose --
    /// <see cref="ExpectedQuantity"/> is a check, not an amount.
    /// </summary>
    [MessagePackObject]
    public class ClientMarketPurchase : Encodable {
        protected override byte Code => (byte) ClientOperation.MarketPurchase;

        [Key(0)]
        public uint ItemId { get; set; }

        /// <summary>Identifies the listing itself, stable across a re-query.</summary>
        [Key(1)]
        public ulong ListingId { get; set; }

        /// <summary>Unit price the phone displayed. Mismatch aborts the purchase.</summary>
        [Key(2)]
        public uint ExpectedUnitPrice { get; set; }

        /// <summary>Quantity the phone displayed. Mismatch aborts the purchase.</summary>
        [Key(3)]
        public uint ExpectedQuantity { get; set; }

        /// <summary>HQ flag the phone displayed. Mismatch aborts the purchase.</summary>
        [Key(4)]
        public bool ExpectedHq { get; set; }

        public static ClientMarketPurchase Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ClientMarketPurchase>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
