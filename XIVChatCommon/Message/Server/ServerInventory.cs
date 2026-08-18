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
    }

    [MessagePackObject]
    public sealed class ServerInventoryContainer {
        [Key(0)] public uint ContainerType { get; set; }
        [Key(1)] public int Size { get; set; }
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

        [IgnoreMember]
        protected override byte Code => (byte) ServerOperation.Inventory;

        public ServerInventory() {
        }

        public ServerInventory(long updatedUnix, ServerInventoryItem[] items, ServerInventoryContainer[]? containers = null) {
            this.UpdatedUnix = updatedUnix;
            this.Items = items;
            this.Containers = containers ?? [];
        }

        public static ServerInventory Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerInventory>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }
}
