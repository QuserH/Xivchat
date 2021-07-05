using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public class ServerBacklog : Encodable {
        [Key(0)]
        public readonly ServerMessage[] messages;

        protected override byte Code => (byte)ServerOperation.Backlog;

        public ServerBacklog(ServerMessage[] messages) {
            this.messages = messages;
        }

        public static ServerBacklog Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerBacklog>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}