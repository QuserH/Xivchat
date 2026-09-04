using MessagePack;

namespace XIVChatCommon.Message.Server {
    /// <summary>
    /// Outcome of a market purchase attempt.
    ///
    /// Codes 0-5 line up with <see cref="MarketStatus"/> so the shared precondition
    /// check can be reused without a translation table. Everything from 6 up is
    /// specific to buying.
    /// </summary>
    public enum MarketPurchaseStatus : byte {
        /// <summary>The server accepted the purchase.</summary>
        Ok = 0,
        NotLoggedIn = 1,
        InDuty = 2,

        /// <summary>Board window open on the PC; it owns the same proxy slot.</summary>
        BoardOpen = 3,
        NotMarketable = 4,

        /// <summary>No reply from the game in time. May or may not have gone through.</summary>
        Timeout = 5,

        /// <summary>
        /// The listing was not on the board any more. Someone else almost certainly
        /// bought it first. Nothing was spent.
        /// </summary>
        ListingGone = 6,

        /// <summary>
        /// The listing is still there but no longer matches what the phone showed
        /// (relisted at a new price, or a different stack). Nothing was spent -- the
        /// player is asked to look again rather than buy at a price they did not see.
        /// </summary>
        Changed = 7,

        /// <summary>Not enough gil for price + tax. Nothing was spent.</summary>
        NotEnoughGil = 8,

        /// <summary>
        /// The game refused to send the request, or the server rejected it (full
        /// inventory being the usual cause). <see cref="ErrorId"/> carries the game's
        /// own log-message id when there is one.
        /// </summary>
        Refused = 9,

        /// <summary>Buying from the phone is switched off in the plugin's settings.</summary>
        Disabled = 10,

        /// <summary>Another purchase or query is already running.</summary>
        Busy = 11,
    }

    /// <summary>
    /// Result of one <see cref="Client.ClientOperation.MarketPurchase"/>.
    ///
    /// Always sent, including on refusal, so the phone never has to infer the outcome
    /// from silence. <see cref="MarketPurchaseStatus.Ok"/> is only reported after the
    /// game's own purchase-response callback comes back clean; a timeout stays a
    /// timeout rather than being optimistically reported as success.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerMarketPurchase : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public uint ItemId { get; set; }
        [Key(2)] public MarketPurchaseStatus Status { get; set; }

        /// <summary>Listing the phone asked for, echoed so replies can be matched.</summary>
        [Key(3)] public ulong ListingId { get; set; }

        /// <summary>Unit price actually paid, when the purchase went through.</summary>
        [Key(4)] public uint UnitPrice { get; set; }

        /// <summary>Quantity actually bought, when the purchase went through.</summary>
        [Key(5)] public uint Quantity { get; set; }

        /// <summary>Tax paid on top of price * quantity.</summary>
        [Key(6)] public uint Tax { get; set; }

        /// <summary>
        /// The game's LogMessage id when it rejected the purchase, else 0. Passed
        /// through rather than interpreted: the phone has no LogMessage sheet, so it
        /// shows a generic reason and this is for the plugin log.
        /// </summary>
        [Key(7)] public uint ErrorId { get; set; }

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.MarketPurchase;

        public ServerMarketPurchase() {
        }

        public ServerMarketPurchase(long updatedUnix, uint itemId, MarketPurchaseStatus status,
            ulong listingId, uint unitPrice = 0, uint quantity = 0, uint tax = 0, uint errorId = 0) {
            this.UpdatedUnix = updatedUnix;
            this.ItemId = itemId;
            this.Status = status;
            this.ListingId = listingId;
            this.UnitPrice = unitPrice;
            this.Quantity = quantity;
            this.Tax = tax;
            this.ErrorId = errorId;
        }

        public static ServerMarketPurchase Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerMarketPurchase>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
