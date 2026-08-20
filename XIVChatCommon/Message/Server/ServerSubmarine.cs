using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerSubmarineVessel {
        [Key(0)] public string Name { get; set; } = string.Empty;
        [Key(1)] public long ReturnUnix { get; set; }
        [Key(2)] public int RankId { get; set; }
        [Key(3)] public long CurrentExp { get; set; }
        [Key(4)] public long NextLevelExp { get; set; }
    }

    [MessagePackObject]
    public sealed class ServerSubmarine : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public ServerSubmarineVessel[] Vessels { get; set; } = [];

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.Submarine;

        public static ServerSubmarine Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerSubmarine>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
