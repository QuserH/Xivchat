using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerDailyEntry {
        [Key(0)] public string Id { get; set; } = string.Empty;
        [Key(1)] public string Label { get; set; } = string.Empty;
        [Key(2)] public bool Weekly { get; set; }
        [Key(3)] public bool Automatic { get; set; }
        [Key(4)] public bool Available { get; set; }
        [Key(5)] public bool Complete { get; set; }
        [Key(6)] public int Remaining { get; set; }
        [Key(7)] public int Goal { get; set; }
        [Key(8)] public string Note { get; set; } = string.Empty;
    }

    [MessagePackObject]
    public sealed class ServerDailies : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public long NextDailyResetUnix { get; set; }
        [Key(2)] public long NextWeeklyResetUnix { get; set; }
        [Key(3)] public ServerDailyEntry[] Entries { get; set; } = [];

        public ServerDailies() { }
        public ServerDailies(long updatedUnix, long nextDailyResetUnix, long nextWeeklyResetUnix, ServerDailyEntry[] entries) {
            UpdatedUnix = updatedUnix;
            NextDailyResetUnix = nextDailyResetUnix;
            NextWeeklyResetUnix = nextWeeklyResetUnix;
            Entries = entries;
        }

        [IgnoreMember] protected override byte Code => (byte)ServerOperation.Dailies;
        protected override byte[] PayloadEncode() => MessagePackSerializer.Serialize(this);
    }
}
