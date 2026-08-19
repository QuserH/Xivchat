using MessagePack;

namespace XIVChatCommon.Message.Client {
    [MessagePackObject]
    public class ClientTeleport : Encodable {
        protected override byte Code => (byte) ClientOperation.Teleport;

        [Key(0)]
        public string PlaceName { get; set; } = string.Empty;

        public static ClientTeleport Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ClientTeleport>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
