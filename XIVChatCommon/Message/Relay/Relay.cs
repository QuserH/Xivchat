using System.Collections.Generic;
using MessagePack;

namespace XIVChatCommon.Message.Relay {
    [Union(1, typeof(RelayRegister))]
    [Union(2, typeof(RelayedMessage))]
    [Union(3, typeof(RelayClientDisconnect))]
    public interface IToRelay {
    }

    [MessagePackObject]
    public class RelayRegister : IToRelay {
        [Key(0)]
        public string AuthToken { get; set; }

        [Key(1)]
        public byte[] PublicKey { get; set; }
    }

    [Union(1, typeof(RelaySuccess))]
    [Union(2, typeof(RelayNewClient))]
    [Union(3, typeof(RelayedMessage))]
    [Union(4, typeof(RelayClientDisconnect))]
    public interface IFromRelay {
    }

    [MessagePackObject]
    public class RelaySuccess : IFromRelay {
        [Key(0)]
        public bool Success { get; set; }

        [Key(1)]
        public string? Info { get; set; }
    }

    [MessagePackObject]
    public class RelayNewClient : IFromRelay {
        [Key(0)]
        public List<byte> PublicKey { get; set; }

        [Key(1)]
        public string Address { get; set; }
    }

    [MessagePackObject]
    public class RelayedMessage : IFromRelay, IToRelay {
        [Key(0)]
        public List<byte> PublicKey { get; set; }

        [Key(1)]
        public List<byte> Message { get; set; }
    }

    [MessagePackObject]
    public class RelayClientDisconnect : IFromRelay, IToRelay {
        [Key(0)]
        public List<byte> PublicKey { get; set; }
    }
}
