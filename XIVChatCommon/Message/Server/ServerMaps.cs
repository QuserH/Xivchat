using MessagePack;

namespace XIVChatCommon.Message.Server {
    [MessagePackObject]
    public sealed class ServerMapDestination {
        [Key(0)] public uint RowId { get; set; }
        [Key(1)] public string Name { get; set; } = string.Empty;
        [Key(2)] public byte Order { get; set; }
    }

    [MessagePackObject]
    public sealed class ServerMapRegion {
        [Key(0)] public string Name { get; set; } = string.Empty;
        [Key(1)] public byte Order { get; set; }
        [Key(2)] public ServerMapDestination[] Destinations { get; set; } = [];
    }

    [MessagePackObject]
    public sealed class ServerMapExpansion {
        [Key(0)] public string Name { get; set; } = string.Empty;
        [Key(1)] public byte Order { get; set; }
        [Key(2)] public ServerMapRegion[] Regions { get; set; } = [];
    }

    [MessagePackObject]
    public sealed class ServerMaps : Encodable {
        [Key(0)] public string CurrentZone { get; set; } = string.Empty;
        [Key(1)] public string CurrentRegion { get; set; } = string.Empty;
        [Key(2)] public ServerMapExpansion[] Expansions { get; set; } = [];

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.Maps;

        protected override byte[] PayloadEncode() => MessagePackSerializer.Serialize(this);
    }
}
