using MessagePack;

namespace XIVChatCommon.Message.Server {
    /// <summary>A single non-empty slot captured from the game inventory.</summary>
    [MessagePackObject]
    public sealed class ServerInventoryItem {
        [Key(0)] public uint ItemId { get; set; }
        [Key(1)] public uint BaseItemId { get; set; }
        [Key(2)] public int Quantity { get; set; }
        [Key(3)] public uint ContainerType { get; set; }
        [Key(4)] public uint InventorySlot { get; set; }
        [Key(5)] public bool IsHq { get; set; }
        [Key(6)] public uint SpiritbondOrCollectability { get; set; }
        [Key(7)] public uint Condition { get; set; }
        [Key(8)] public string? Name { get; set; }
        [Key(9)] public uint IconId { get; set; }
        [Key(10)] public ulong RetainerId { get; set; }
    }

    [MessagePackObject]
    public sealed class ServerInventoryContainer {
        [Key(0)] public uint ContainerType { get; set; }
        [Key(1)] public int Size { get; set; }
    }

    [MessagePackObject]
    public sealed class ServerRetainer {
        [Key(0)] public ulong RetainerId { get; set; }
        [Key(1)] public string Name { get; set; } = string.Empty;
        [Key(2)] public bool Active { get; set; }
        [Key(3)] public int ItemCount { get; set; }
        [Key(4)] public int Quantity { get; set; }
        [Key(5)] public uint Gil { get; set; }
        [Key(6)] public uint VentureId { get; set; }
        [Key(7)] public long VentureCompleteUnix { get; set; }
    }

    /// <summary>
    /// A complete inventory snapshot. Empty slots are omitted to keep the encrypted
    /// TCP message small enough for the existing 128 KiB frame limit.
    /// </summary>
    [MessagePackObject]
    public sealed class ServerInventory : Encodable {
        [Key(0)] public long UpdatedUnix { get; set; }
        [Key(1)] public ServerInventoryItem[] Items { get; set; } = [];
        [Key(2)] public ServerInventoryContainer[] Containers { get; set; } = [];
        [Key(3)] public ServerRetainer[] Retainers { get; set; } = [];

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.Inventory;

        public ServerInventory() {
        }

        public ServerInventory(long updatedUnix, ServerInventoryItem[] items, ServerInventoryContainer[]? containers = null, ServerRetainer[]? retainers = null) {
            this.UpdatedUnix = updatedUnix;
            this.Items = items;
            this.Containers = containers ?? [];
            this.Retainers = retainers ?? [];
        }

        public static ServerInventory Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerInventory>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
