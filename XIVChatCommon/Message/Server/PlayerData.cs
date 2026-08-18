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

        [Key(8)]
        public readonly int currentHp;

        [Key(9)]
        public readonly int maxHp;

        [Key(10)]
        public readonly int currentMp;

        [Key(11)]
        public readonly int maxMp;

        [Key(12)]
        public readonly int currentCp;

        [Key(13)]
        public readonly int maxCp;

        [Key(14)]
        public readonly int currentGp;

        [Key(15)]
        public readonly int maxGp;

        [Key(16)]
        public readonly int itemLevel;

        public PlayerData(string homeWorld, string currentWorld, string location, string name,
            uint classJobId = 0, string jobName = "", byte level = 0, uint territoryId = 0,
            int currentHp = 0, int maxHp = 0, int currentMp = 0, int maxMp = 0,
            int currentCp = 0, int maxCp = 0, int currentGp = 0, int maxGp = 0,
            int itemLevel = 0) {
            this.homeWorld = homeWorld;
            this.currentWorld = currentWorld;
            this.location = location;
            this.name = name;
            this.classJobId = classJobId;
            this.jobName = jobName;
            this.level = level;
            this.territoryId = territoryId;
            this.currentHp = currentHp;
            this.maxHp = maxHp;
            this.currentMp = currentMp;
            this.maxMp = maxMp;
            this.currentCp = currentCp;
            this.maxCp = maxCp;
            this.currentGp = currentGp;
            this.maxGp = maxGp;
            this.itemLevel = itemLevel;
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
