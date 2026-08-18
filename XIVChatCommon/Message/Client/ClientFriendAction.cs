using MessagePack;

namespace XIVChatCommon.Message.Client {
    public enum FriendActionKind : byte {
        AdventurerPlate = 1,
        InviteToParty = 2,
        VisitEstate = 3,
        SearchInfo = 4,
    }

    [MessagePackObject]
    public sealed class ClientFriendAction : Encodable {
        [Key(0)] public FriendActionKind Action { get; set; }
        [Key(1)] public ulong ContentId { get; set; }
        [Key(2)] public ushort WorldId { get; set; }

        [IgnoreMember]
        protected override byte Code => (byte) ClientOperation.FriendAction;

        public static ClientFriendAction Decode(byte[] bytes) => MessagePackSerializer.Deserialize<ClientFriendAction>(bytes);

        protected override byte[] PayloadEncode() => MessagePackSerializer.Serialize(this);
    }
}
