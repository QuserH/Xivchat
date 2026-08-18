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
    }
}
