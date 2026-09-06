using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerCraftSkill {
        [Key(0)] public uint Id { get; set; }
        [Key(1)] public string Name { get; set; } = "";
        [Key(2)] public uint Icon { get; set; }
        [Key(3)] public string Description { get; set; } = "";
        [Key(4)] public int Cp { get; set; }
        [Key(5)] public byte Kind { get; set; } // 0 制作 1 加工 3 增益/其他
    }

    [MessagePackObject]
    public sealed class ServerCraftConsumable {
        [Key(0)] public uint Id { get; set; }
        [Key(1)] public string Name { get; set; } = "";
        [Key(2)] public int Quantity { get; set; }
    }

    /// <summary>
    /// The game's crafting skill sheet + inventory foods/pots that help crafting.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerCraftSkillList : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public ServerCraftSkill[] Skills { get; set; } = [];
        [Key(2)] public ServerCraftConsumable[] Foods { get; set; } = [];
        [Key(3)] public ServerCraftConsumable[] Pots { get; set; } = [];

        public ServerCraftSkillList() {
        }

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.CraftSkillList;

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
