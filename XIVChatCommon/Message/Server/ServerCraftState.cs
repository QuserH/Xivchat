using MessagePack;

namespace XIVChatCommon.Message.Server {
    /// <summary>
    /// One snapshot of a remotely driven manual craft. ProgressMax/QualityMax may be
    /// 0 when the game does not expose them; clients then track the running maximum.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerCraftState : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public int RecipeId { get; set; }
        [Key(2)] public int Step { get; set; }
        [Key(3)] public int Progress { get; set; }
        [Key(4)] public int ProgressMax { get; set; }
        [Key(5)] public int Quality { get; set; }
        [Key(6)] public int QualityMax { get; set; }
        [Key(7)] public int Durability { get; set; }
        [Key(8)] public int DurabilityMax { get; set; }
        [Key(9)] public int Cp { get; set; }
        [Key(10)] public int CpMax { get; set; }
        [Key(11)] public int ConditionId { get; set; }
        [Key(12)] public bool Finished { get; set; }

        public ServerCraftState() {
        }

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.CraftState;

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
