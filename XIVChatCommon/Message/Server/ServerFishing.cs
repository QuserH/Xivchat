using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerFishing : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public byte[] FishBits { get; set; } = [];
        [Key(2)] public byte[] SpearfishBits { get; set; } = [];

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.Fishing;

        protected override byte[] PayloadEncode() => MessagePackSerializer.Serialize(this);
    }
}
