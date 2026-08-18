using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public class PlayerData : Encodable {
        [Key(0)]
        public readonly string homeWorld;

        [Key(1)]
        public readonly string currentWorld;

        [Key(2)]
        public readonly string location;

        [Key(3)]
        public readonly string name;

        [Key(4)]
        public readonly uint classJobId;

        [Key(5)]
        public readonly string jobName;

        [Key(6)]
        public readonly byte level;

        [Key(7)]
        public readonly uint territoryId;

        public PlayerData(string homeWorld, string currentWorld, string location, string name,
            uint classJobId = 0, string jobName = "", byte level = 0, uint territoryId = 0) {
            this.homeWorld = homeWorld;
            this.currentWorld = currentWorld;
            this.location = location;
            this.name = name;
            this.classJobId = classJobId;
            this.jobName = jobName;
            this.level = level;
            this.territoryId = territoryId;
        }

        [IgnoreMember]
        protected override byte Code => (byte)ServerOperation.PlayerData;

        public static PlayerData Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<PlayerData>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
