namespace XIVChatCommon.Message.Client {
    public enum ClientOperation : byte {
        Ping = 1,
        Message = 2,
        Shutdown = 3,
        Backlog = 4,
        CatchUp = 5,
        PlayerList = 6,
        LinkshellList = 7,
        Preferences = 8,
        Channel = 9,
        FriendAction = 10,
        JobsAction = 11,
        Teleport = 12,

        /// <summary>Ask the game client to query the market board for one item.</summary>
        MarketSearch = 13,

        /// <summary>
        /// Buy one specific market board listing. Spends real gil, so the plugin
        /// re-reads the board and refuses if the listing moved -- see
        /// <see cref="ClientMarketPurchase"/>.
        /// </summary>
        MarketPurchase = 14,

        /// <summary>Request the list of all market board item categories.</summary>
        MarketCategories = 15,

        /// <summary>
        /// Replace the plugin's price-monitor list. Monitored items are re-queried
        /// inside the game client on a timer; when auto-buy is on for an entry, the
        /// plugin buys matching listings itself -- see <see cref="ClientMarketMonitorSync"/>.
        /// </summary>
        MarketMonitorSync = 16,

        /// <summary>Open the recipe note on the given recipe row and start synthesizing it.</summary>
        CraftStart = 30,

        /// <summary>Use one crafting ability while a remotely driven craft is running.</summary>
        CraftSkill = 31,

        /// <summary>Stop receiving remote-craft state pushes for this client.</summary>
        CraftStop = 32,

        /// <summary>Select the food to keep up while crafting (0 = off).</summary>
        CraftFood = 33,

        /// <summary>Request the crafting skill sheet + helpful consumables (op 42 reply).</summary>
        CraftSkillList = 34,
    }
}
