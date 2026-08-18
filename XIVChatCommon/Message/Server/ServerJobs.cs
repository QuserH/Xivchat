using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerJobEntry {
        [Key(0)] public uint JobId { get; set; }
        [Key(1)] public string Name { get; set; } = string.Empty;
        [Key(2)] public string Abbreviation { get; set; } = string.Empty;
        [Key(3)] public string Category { get; set; } = string.Empty;
        [Key(4)] public int Level { get; set; }
        [Key(5)] public bool Active { get; set; }
        [Key(6)] public int ItemLevel { get; set; }
    }

    [MessagePackObject]
    public sealed class ServerJobs : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public ServerJobEntry[] Entries { get; set; } = [];

        public ServerJobs() { }
        public ServerJobs(long updatedUnix, ServerJobEntry[] entries) {
            UpdatedUnix = updatedUnix;
            Entries = entries;
        }

        [IgnoreMember] protected override byte Code => (byte)ServerOperation.Jobs;
        protected override byte[] PayloadEncode() => MessagePackSerializer.Serialize(this);
    }
}
