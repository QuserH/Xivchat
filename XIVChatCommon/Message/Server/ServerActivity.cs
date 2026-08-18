using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerActivity : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public long SessionStartedUnix { get; set; }
        [Key(2)] public long SessionPlaySeconds { get; set; }
        [Key(3)] public long SessionExpGained { get; set; }
        [Key(4)] public int SessionLevelsGained { get; set; }
        [Key(5)] public long SessionGilEarned { get; set; }
        [Key(6)] public int SessionDutiesCompleted { get; set; }
        [Key(7)] public long TodayPlaySeconds { get; set; }
        [Key(8)] public long TodayExpGained { get; set; }
        [Key(9)] public int TodayLevelsGained { get; set; }
        [Key(10)] public long TodayGilEarned { get; set; }
        [Key(11)] public int TodayDutiesCompleted { get; set; }
        [Key(12)] public int MountsOwned { get; set; }
        [Key(13)] public int MountsTotal { get; set; }
        [Key(14)] public int MinionsOwned { get; set; }
        [Key(15)] public int MinionsTotal { get; set; }
        [Key(16)] public int RetainerCount { get; set; }
        [Key(17)] public int VenturesReady { get; set; }
        [Key(18)] public int VenturesActive { get; set; }

        [IgnoreMember] protected override byte Code => (byte)ServerOperation.Activity;
        protected override byte[] PayloadEncode() => MessagePackSerializer.Serialize(this);
    }
}
