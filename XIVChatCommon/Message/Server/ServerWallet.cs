using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerWalletEntry {
        [Key(0)] public uint ItemId { get; set; }
        [Key(1)] public uint IconId { get; set; }
        [Key(2)] public string Name { get; set; } = string.Empty;
        [Key(3)] public long Amount { get; set; }
        [Key(4)] public long Cap { get; set; }
        [Key(5)] public string Section { get; set; } = string.Empty;
    }

    [MessagePackObject]
    public sealed class ServerWallet : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public long Gil { get; set; }
        [Key(2)] public ServerWalletEntry[] Entries { get; set; } = [];

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.Wallet;

        public ServerWallet() {
        }

        public ServerWallet(long updatedUnix, long gil, ServerWalletEntry[] entries) {
            this.UpdatedUnix = updatedUnix;
            this.Gil = gil;
            this.Entries = entries;
        }

        protected override byte[] PayloadEncode() => MessagePackSerializer.Serialize(this);
    }
}
