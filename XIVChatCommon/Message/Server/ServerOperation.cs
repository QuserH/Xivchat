namespace XIVChatCommon.Message.Server {
    public enum ServerOperation : byte {
        Pong = 1,
        Message = 2,
        Shutdown = 3,
        PlayerData = 4,
        Availability = 5,
        Channel = 6,
        Backlog = 7,
        PlayerList = 8,
        LinkshellList = 9,
        HousingLocation = 10,
        /// <summary>Snapshot of the game inventory for Aetherphone clients.</summary>
        Inventory = 11,
        /// <summary>Live gil and currency balances for EorzeaPhone clients.</summary>
        Wallet = 12,
        Weather = 13,
        Jobs = 14,
        Dailies = 15,
        Activity = 16,
        /// <summary>Local collection unlocks (mounts, minions, emotes, orchestrions).</summary>
        Collections = 17,
        /// <summary>Current location and the game map/aetheryte hierarchy.</summary>
        Maps = 18,
        /// <summary>Caught fish and spearfish notebook bitsets for the active character.</summary>
        Fishing = 19,
        /// <summary>Housing workshop submarine vessels and their voyages.</summary>
        Submarine = 20,

        /// <summary>
        /// Live market board listings, read from the game client's own item search
        /// proxy. Sent in reply to <see cref="Client.ClientOperation.MarketSearch"/>,
        /// never unsolicited: a search mutates shared game state, so it only happens
        /// when the phone asks.
        /// </summary>
        Market = 21,

        /// <summary>
        /// Outcome of a <see cref="Client.ClientOperation.MarketPurchase"/>. Always
        /// sent, refusals included, so the phone never has to infer the result of a
        /// gil transaction from silence.
        /// </summary>
        MarketPurchase = 22,

        /// <summary>
        /// List of all market board item categories with their subcategories and
        /// items. Sent in reply to <see cref="Client.ClientOperation.MarketCategories"/>.
        /// </summary>
        MarketCategories = 23,

        /// <summary>
        /// Price-monitor event: a monitored item dropped below its threshold, an
        /// automatic purchase happened (or failed), or the monitor list was replaced.
        /// Broadcast to every client, since monitoring runs inside the plugin whether
        /// or not a phone is connected when it fires.
        /// </summary>
        MarketMonitor = 24,
    }
}
