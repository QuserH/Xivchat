using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerCollectionItem {
        [Key(0)] public uint Id { get; set; }
        [Key(1)] public string? Name { get; set; }
        [Key(2)] public uint IconId { get; set; }
        [Key(3)] public bool Owned { get; set; }
    }

    [MessagePackObject]
    public sealed class ServerCollectionCategory {
        [Key(0)] public uint Id { get; set; }
        [Key(1)] public int Total { get; set; }
        [Key(2)] public int Owned { get; set; }
        [Key(3)] public ServerCollectionItem[] Items { get; set; } = [];
    }

    /// <summary>
    /// Local collection unlocks (mounts, minions, emotes, orchestrions). Only
    /// owned items are sent to keep the frame small; totals convey progress.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerCollections : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public ServerCollectionCategory[] Categories { get; set; } = [];

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.Collections;

        public ServerCollections() {
        }

        public ServerCollections(long updatedUnix, ServerCollectionCategory[] categories) {
            this.UpdatedUnix = updatedUnix;
            this.Categories = categories;
        }

        public static ServerCollections Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerCollections>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
