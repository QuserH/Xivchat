using MessagePack;

namespace XIVChatCommon.Message.Client {
    [MessagePackObject]
    public sealed class ClientJobsAction : Encodable {
        [Key(0)] public int GearsetId { get; set; }

        [IgnoreMember]
        protected override byte Code => (byte) ClientOperation.JobsAction;

        public static ClientJobsAction Decode(byte[] bytes) => MessagePackSerializer.Deserialize<ClientJobsAction>(bytes);

        protected override byte[] PayloadEncode() => MessagePackSerializer.Serialize(this);
    }
}
