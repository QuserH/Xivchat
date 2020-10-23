using MessagePack;
using MessagePack.Formatters;
using System;
using System.Collections.Generic;

namespace XIVChatCommon {
    [MessagePackObject]
    public class ServerMessage : IEncodable {
        [MessagePackFormatter(typeof(MillisecondsDateTimeFormatter))]
        [Key(0)]
        public DateTime Timestamp { get; set; }
        [Key(1)]
        public ChatType Channel { get; set; }
        [Key(2)]
        public byte[] Sender { get; set; }
        [Key(3)]
        public byte[] Content { get; set; }
        [Key(4)]
        public List<Chunk> Chunks { get; set; }

        [IgnoreMember]
        public string ContentText => XivString.GetText(this.Content);
        [IgnoreMember]
        public string SenderText => XivString.GetText(this.Sender);

        [IgnoreMember]
        protected override byte Code => (byte)ServerOperation.Message;

        public static ServerMessage Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerMessage>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }

    [Union(1, typeof(TextChunk))]
    [Union(2, typeof(IconChunk))]
    [MessagePackObject]
    public abstract class Chunk {
    }

    [MessagePackObject]
    public class TextChunk : Chunk {
        [Key(0)]
        public uint? FallbackColour { get; set; }
        [Key(1)]
        public uint? Foreground { get; set; }
        [Key(2)]
        public uint? Glow { get; set; }
        [Key(3)]
        public bool Italic { get; set; }
        [Key(4)]
        public string Content { get; set; }
    }

    [MessagePackObject]
    public class IconChunk : Chunk {
        [Key(0)]
        public byte Index;
    }

    public class NameFormatting {
        public string Before { get; private set; } = string.Empty;
        public string After { get; private set; } = string.Empty;
        public bool IsPresent { get; private set; } = true;

        public static NameFormatting Empty() {
            return new NameFormatting {
                IsPresent = false,
            };
        }

        public static NameFormatting Of(string before, string after) {
            return new NameFormatting {
                Before = before,
                After = after,
            };
        }

        public static NameFormatting Basic() {
            return new NameFormatting {
                Before = "",
                After = ": ",
            };
        }
    }

    public class ChatCode {
        private const ushort CLEAR_7 = ~(~0 << 7);

        private readonly ushort code;

        public ChatType Type => (ChatType)(this.code & CLEAR_7);
        public ChatSource Source => this.SourceFrom(11);
        public ChatSource Target => this.SourceFrom(7);
        private ChatSource SourceFrom(ushort shift) => (ChatSource)(1 << ((this.code >> shift) & 0xF));

        public ChatCode(ushort code) {
            this.code = code;
        }

        public NameFormatting NameFormat() {
            switch (this.Type) {
                case ChatType.Say:
                case ChatType.Shout:
                case ChatType.Yell:
                case ChatType.NpcAnnouncement:
                case ChatType.NpcDialogue:
                    return NameFormatting.Of("", ": ");
                case ChatType.TellOutgoing:
                    return NameFormatting.Of(">> ", ": ");
                case ChatType.TellIncoming:
                    return NameFormatting.Of("", " >> ");
                case ChatType.GmTell:
                    return NameFormatting.Of("[GM]", " >> ");
                case ChatType.GmSay:
                case ChatType.GmShout:
                case ChatType.GmYell:
                    return NameFormatting.Of("[GM]", ": ");
                case ChatType.GmParty:
                    return NameFormatting.Of("([GM]", ") ");
                case ChatType.GmFreeCompany:
                    return NameFormatting.Of("[FC]<[GM]", "> ");
                case ChatType.GmLinkshell1:
                    return NameFormatting.Of("[1]<[GM]", "> ");
                case ChatType.GmLinkshell2:
                    return NameFormatting.Of("[2]<[GM]", "> ");
                case ChatType.GmLinkshell3:
                    return NameFormatting.Of("[3]<[GM]", "> ");
                case ChatType.GmLinkshell4:
                    return NameFormatting.Of("[4]<[GM]", "> ");
                case ChatType.GmLinkshell5:
                    return NameFormatting.Of("[5]<[GM]", "> ");
                case ChatType.GmLinkshell6:
                    return NameFormatting.Of("[6]<[GM]", "> ");
                case ChatType.GmLinkshell7:
                    return NameFormatting.Of("[7]<[GM]", "> ");
                case ChatType.GmLinkshell8:
                    return NameFormatting.Of("[8]<[GM]", "> ");
                case ChatType.GmNoviceNetwork:
                    return NameFormatting.Of("[NOVICE][GM]", ": ");
                case ChatType.Party:
                case ChatType.CrossParty:
                    return NameFormatting.Of("(", ") ");
                case ChatType.Alliance:
                    return NameFormatting.Of("((", ")) ");
                case ChatType.PvpTeam:
                    return NameFormatting.Of("[PVP]<", "> ");
                case ChatType.FreeCompany:
                    return NameFormatting.Of("[FC]<", "> ");
                case ChatType.Linkshell1:
                    return NameFormatting.Of("[1]<", "> ");
                case ChatType.Linkshell2:
                    return NameFormatting.Of("[2]<", "> ");
                case ChatType.Linkshell3:
                    return NameFormatting.Of("[3]<", "> ");
                case ChatType.Linkshell4:
                    return NameFormatting.Of("[4]<", "> ");
                case ChatType.Linkshell5:
                    return NameFormatting.Of("[5]<", "> ");
                case ChatType.Linkshell6:
                    return NameFormatting.Of("[6]<", "> ");
                case ChatType.Linkshell7:
                    return NameFormatting.Of("[7]<", "> ");
                case ChatType.Linkshell8:
                    return NameFormatting.Of("[8]<", "> ");
                case ChatType.StandardEmote:
                    return NameFormatting.Empty();
                case ChatType.CustomEmote:
                    return NameFormatting.Of("", "");
                case ChatType.CrossLinkshell1:
                    return NameFormatting.Of("[CWLS1]<", "> ");
                case ChatType.CrossLinkshell2:
                    return NameFormatting.Of("[CWLS2]<", "> ");
                case ChatType.CrossLinkshell3:
                    return NameFormatting.Of("[CWLS3]<", "> ");
                case ChatType.CrossLinkshell4:
                    return NameFormatting.Of("[CWLS4]<", "> ");
                case ChatType.CrossLinkshell5:
                    return NameFormatting.Of("[CWLS5]<", "> ");
                case ChatType.CrossLinkshell6:
                    return NameFormatting.Of("[CWLS6]<", "> ");
                case ChatType.CrossLinkshell7:
                    return NameFormatting.Of("[CWLS7]<", "> ");
                case ChatType.CrossLinkshell8:
                    return NameFormatting.Of("[CWLS8]<", "> ");
                case ChatType.NoviceNetwork:
                    return NameFormatting.Of("[NOVICE]", ": ");
                default:
                    return null;
            }
        }

        public bool IsBattle() {
            switch (this.Type) {
                case ChatType.Damage:
                case ChatType.Miss:
                case ChatType.Action:
                case ChatType.Item:
                case ChatType.Healing:
                case ChatType.GainBuff:
                case ChatType.LoseBuff:
                case ChatType.GainDebuff:
                case ChatType.LoseDebuff:
                case ChatType.BattleSystem:
                    return true;
                default:
                    return false;
            }
        }

        public uint? DefaultColour() {
            switch (this.Type) {
                case ChatType.Debug:
                    return Rgba(204, 204, 204);
                case ChatType.Urgent:
                    return Rgba(255, 127, 127);
                case ChatType.Notice:
                    return Rgba(179, 140, 255);

                case ChatType.Say:
                    return Rgba(247, 247, 247);
                case ChatType.Shout:
                    return Rgba(255, 166, 102);
                case ChatType.TellIncoming:
                case ChatType.TellOutgoing:
                case ChatType.GmTell:
                    return Rgba(255, 184, 222);
                case ChatType.Party:
                case ChatType.CrossParty:
                    return Rgba(102, 229, 255);
                case ChatType.Alliance:
                    return Rgba(255, 127, 0);
                case ChatType.NoviceNetwork:
                case ChatType.NoviceNetworkSystem:
                    return Rgba(212, 255, 125);
                case ChatType.Linkshell1:
                case ChatType.Linkshell2:
                case ChatType.Linkshell3:
                case ChatType.Linkshell4:
                case ChatType.Linkshell5:
                case ChatType.Linkshell6:
                case ChatType.Linkshell7:
                case ChatType.Linkshell8:
                case ChatType.CrossLinkshell1:
                case ChatType.CrossLinkshell2:
                case ChatType.CrossLinkshell3:
                case ChatType.CrossLinkshell4:
                case ChatType.CrossLinkshell5:
                case ChatType.CrossLinkshell6:
                case ChatType.CrossLinkshell7:
                case ChatType.CrossLinkshell8:
                    return Rgba(212, 255, 125);
                case ChatType.StandardEmote:
                    return Rgba(186, 255, 240);
                case ChatType.CustomEmote:
                    return Rgba(186, 255, 240);
                case ChatType.Yell:
                    return Rgba(255, 255, 0);
                case ChatType.Echo:
                    return Rgba(204, 204, 204);
                case ChatType.System:
                case ChatType.GatheringSystem:
                case ChatType.PeriodicRecruitmentNotification:
                case ChatType.Orchestrion:
                case ChatType.Alarm:
                case ChatType.RetainerSale:
                case ChatType.Sign:
                case ChatType.MessageBook:
                    return Rgba(204, 204, 204);
                case ChatType.NpcAnnouncement:
                case ChatType.NpcDialogue:
                    return Rgba(171, 214, 71);
                case ChatType.Error:
                    return Rgba(255, 74, 74);
                case ChatType.FreeCompany:
                case ChatType.FreeCompanyAnnouncement:
                case ChatType.FreeCompanyLoginLogout:
                    return Rgba(171, 219, 229);
                case ChatType.PvpTeam:
                    return Rgba(171, 219, 229);
                case ChatType.PvpTeamAnnouncement:
                case ChatType.PvpTeamLoginLogout:
                    return Rgba(171, 219, 229);
                case ChatType.Action:
                case ChatType.Item:
                case ChatType.LootNotice:
                    return Rgba(255, 255, 176);
                case ChatType.Progress:
                    return Rgba(255, 222, 115);
                case ChatType.LootRoll:
                case ChatType.RandomNumber:
                    return Rgba(199, 191, 158);
                case ChatType.Crafting:
                case ChatType.Gathering:
                    return Rgba(222, 191, 247);
                case ChatType.Damage:
                    return Rgba(255, 125, 125);
                case ChatType.Miss:
                    return Rgba(204, 204, 204);
                case ChatType.Healing:
                    return Rgba(212, 255, 125);
                case ChatType.GainBuff:
                case ChatType.LoseBuff:
                    return Rgba(148, 191, 255);
                case ChatType.GainDebuff:
                case ChatType.LoseDebuff:
                    return Rgba(255, 138, 196);
                case ChatType.BattleSystem:
                    return Rgba(204, 204, 204);
                default:
                    return null;
            }
        }

        private static uint Rgba(byte red, byte green, byte blue, byte alpha = 0xFF) => alpha
            | (uint)(red << 24)
            | (uint)(green << 16)
            | (uint)(blue << 8);
    }

    [System.Diagnostics.CodeAnalysis.SuppressMessage("Design", "CA1028:Enum Storage should be Int32")]
    public enum ChatType : ushort {
        Debug = 1,
        Urgent = 2,
        Notice = 3,
        Say = 10,
        Shout = 11,
        TellOutgoing = 12,
        TellIncoming = 13,
        Party = 14,
        Alliance = 15,
        Linkshell1 = 16,
        Linkshell2 = 17,
        Linkshell3 = 18,
        Linkshell4 = 19,
        Linkshell5 = 20,
        Linkshell6 = 21,
        Linkshell7 = 22,
        Linkshell8 = 23,
        FreeCompany = 24,
        NoviceNetwork = 27,
        CustomEmote = 28,
        StandardEmote = 29,
        Yell = 30,
        // 31 - also party?
        CrossParty = 32,
        PvpTeam = 36,
        CrossLinkshell1 = 37,
        Damage = 41,
        Miss = 42,
        Action = 43,
        Item = 44,
        Healing = 45,
        GainBuff = 46,
        GainDebuff = 47,
        LoseBuff = 48,
        LoseDebuff = 49,
        Alarm = 55,
        Echo = 56,
        System = 57,
        BattleSystem = 58,
        GatheringSystem = 59,
        Error = 60,
        NpcDialogue = 61,
        LootNotice = 62,
        Progress = 64,
        LootRoll = 65,
        Crafting = 66,
        Gathering = 67,
        NpcAnnouncement = 68,
        FreeCompanyAnnouncement = 69,
        FreeCompanyLoginLogout = 70,
        RetainerSale = 71,
        PeriodicRecruitmentNotification = 72,
        Sign = 73,
        RandomNumber = 74,
        NoviceNetworkSystem = 75,
        Orchestrion = 76,
        PvpTeamAnnouncement = 77,
        PvpTeamLoginLogout = 78,
        MessageBook = 79,
        GmTell = 80,
        GmSay = 81,
        GmShout = 82,
        GmYell = 83,
        GmParty = 84,
        GmFreeCompany = 85,
        GmLinkshell1 = 86,
        GmLinkshell2 = 87,
        GmLinkshell3 = 88,
        GmLinkshell4 = 89,
        GmLinkshell5 = 90,
        GmLinkshell6 = 91,
        GmLinkshell7 = 92,
        GmLinkshell8 = 93,
        GmNoviceNetwork = 94,
        CrossLinkshell2 = 101,
        CrossLinkshell3 = 102,
        CrossLinkshell4 = 103,
        CrossLinkshell5 = 104,
        CrossLinkshell6 = 105,
        CrossLinkshell7 = 106,
        CrossLinkshell8 = 107,
    }

    [System.Diagnostics.CodeAnalysis.SuppressMessage("Design", "CA1028:Enum Storage should be Int32")]
    public enum ChatSource : ushort {
        Self = 2,
        PartyMember = 4,
        AllianceMember = 8,
        Other = 16,
        EngagedEnemy = 32,
        UnengagedEnemy = 64,
        FriendlyNpc = 128,
        SelfPet = 256,
        PartyPet = 512,
        AlliancePet = 1024,
        OtherPet = 2048,
    }

    [MessagePackObject]
    public class ClientMessage : IEncodable {
        [Key(0)]
        public string Content { get; set; }

        [IgnoreMember]
        protected override byte Code => (byte)ClientOperation.Message;

        public static ClientMessage Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ClientMessage>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }

    public enum ServerOperation : byte {
        /// <summary>
        /// Sent in response to a client ping. Has no payload.
        /// </summary>
        Pong = 1,
        /// <summary>
        /// A message was sent in game and is being relayed to the client.
        /// </summary>
        Message = 2,
        /// <summary>
        /// The server is shutting down. Clients should send no response and close their sockets. Has no payload.
        /// </summary>
        Shutdown = 3,
        PlayerData = 4,
        Availability = 5,
        Channel = 6,
        Backlog = 7,
        PlayerList = 8,
        LinkshellList = 9,
    }

    [MessagePackObject]
    public class PlayerData : IEncodable {
        [Key(0)]
        public readonly string homeWorld;
        [Key(1)]
        public readonly string currentWorld;
        [Key(2)]
        public readonly string location;
        [Key(3)]
        public readonly string name;

        public PlayerData(string homeWorld, string currentWorld, string location, string name) {
            this.homeWorld = homeWorld;
            this.currentWorld = currentWorld;
            this.location = location;
            this.name = name;
        }

        [IgnoreMember]
        protected override byte Code => (byte)ServerOperation.PlayerData;

        public static PlayerData Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<PlayerData>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }

    public class EmptyPlayerData : IEncodable {
        public static EmptyPlayerData Instance { get; } = new EmptyPlayerData();

        [IgnoreMember]
        protected override byte Code => (byte)ServerOperation.PlayerData;

        protected override byte[] PayloadEncode() {
            return new byte[0];
        }
    }

    [MessagePackObject]
    public class Availability : IEncodable {
        [Key(0)]
        public readonly bool available;

        public Availability(bool available) {
            this.available = available;
        }

        [IgnoreMember]
        protected override byte Code => (byte)ServerOperation.Availability;

        public static Availability Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<Availability>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }

    [MessagePackObject]
    public class ServerChannel : IEncodable {
        [Key(0)]
        public readonly byte channel;
        [Key(1)]
        public readonly string name;

        [IgnoreMember]
        public InputChannel InputChannel => (InputChannel)this.channel;

        protected override byte Code => (byte)ServerOperation.Channel;

        public ServerChannel(InputChannel channel, string name) : this((byte)channel, name) { }

        public ServerChannel(byte channel, string name) {
            this.channel = channel;
            this.name = name;
        }

        public static ServerChannel Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerChannel>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }

    public enum InputChannel : byte {
        Tell = 0,
        Say = 1,
        Party = 2,
        Alliance = 3,
        Yell = 4,
        Shout = 5,
        FreeCompany = 6,
        PvpTeam = 7,
        NoviceNetwork = 8,
        CrossLinkshell1 = 9,
        CrossLinkshell2 = 10,
        CrossLinkshell3 = 11,
        CrossLinkshell4 = 12,
        CrossLinkshell5 = 13,
        CrossLinkshell6 = 14,
        CrossLinkshell7 = 15,
        CrossLinkshell8 = 16,
        // 17 - unused?
        // 18 - unused?
        Linkshell1 = 19,
        Linkshell2 = 20,
        Linkshell3 = 21,
        Linkshell4 = 22,
        Linkshell5 = 23,
        Linkshell6 = 24,
        Linkshell7 = 25,
        Linkshell8 = 26,
    }

    public class Pong : IEncodable {
        public static Pong Instance { get; } = new Pong();

        [IgnoreMember]
        protected override byte Code => (byte)ServerOperation.Pong;

        protected override byte[] PayloadEncode() {
            return new byte[0];
        }
    }

    public class Ping : IEncodable {
        public static Ping Instance { get; } = new Ping();

        [IgnoreMember]
        protected override byte Code => (byte)ClientOperation.Ping;

        protected override byte[] PayloadEncode() {
            return new byte[0];
        }
    }

    public class ServerShutdown : IEncodable {
        public static ServerShutdown Instance { get; } = new ServerShutdown();

        [IgnoreMember]
        protected override byte Code => (byte)ServerOperation.Shutdown;

        protected override byte[] PayloadEncode() {
            return new byte[0];
        }
    }

    public class ClientShutdown : IEncodable {
        public static ClientShutdown Instance { get; } = new ClientShutdown();

        [IgnoreMember]
        protected override byte Code => (byte)ClientOperation.Shutdown;

        protected override byte[] PayloadEncode() {
            return new byte[0];
        }
    }

    [MessagePackObject]
    public class ServerBacklog : IEncodable {
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

    [MessagePackObject]
    public class ClientBacklog : IEncodable {
        [Key(0)]
        public ushort Amount { get; set; }

        protected override byte Code => (byte)ClientOperation.Backlog;

        public static ClientBacklog Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ClientBacklog>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }

    [MessagePackObject]
    public class ClientCatchUp : IEncodable {
        [MessagePackFormatter(typeof(MillisecondsDateTimeFormatter))]
        [Key(0)]
        public DateTime After { get; set; }

        protected override byte Code => (byte)ClientOperation.CatchUp;

        public static ClientCatchUp Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ClientCatchUp>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }

    [MessagePackObject]
    public class ServerPlayerList : IEncodable {
        [Key(0)]
        public PlayerListType Type { get; set; }

        [Key(1)]
        public Player[] Players { get; set; }

        protected override byte Code => (byte)ServerOperation.PlayerList;

        public static ServerPlayerList Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ServerPlayerList>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }

    [MessagePackObject]
    public class ClientPlayerList : IEncodable {
        [Key(0)]
        public PlayerListType Type { get; set; }

        protected override byte Code => (byte)ClientOperation.PlayerList;

        public static ClientPlayerList Decode(byte[] bytes) {
            return MessagePackSerializer.Deserialize<ClientPlayerList>(bytes);
        }

        protected override byte[] PayloadEncode() {
            return MessagePackSerializer.Serialize(this);
        }
    }

    public enum PlayerListType : byte {
        Party = 1,
        Friend = 2,
        Linkshell = 3,
        CrossLinkshell = 4,
    }

    [MessagePackObject]
    public class Player {
        [Key(0)]
        public string Name { get; set; }
        [Key(1)]
        public string FreeCompany { get; set; }
        [Key(2)]
        public ulong Status { get; set; }
        [Key(3)]
        public ushort CurrentWorld { get; set; }
        [Key(4)]
        public string CurrentWorldName { get; set; }
        [Key(5)]
        public ushort HomeWorld { get; set; }
        [Key(6)]
        public string HomeWorldName { get; set; }
        [Key(7)]
        public ushort Territory { get; set; }
        [Key(8)]
        public string TerritoryName { get; set; }
        [Key(9)]
        public byte Job { get; set; }
        [Key(10)]
        public string JobName { get; set; }
        [Key(11)]
        public byte GrandCompany { get; set; }
        [Key(12)]
        public string GrandCompanyName { get; set; }
        [Key(13)]
        public byte Languages { get; set; }
        [Key(14)]
        public byte MainLanguage { get; set; }
    }

    public abstract class IEncodable {
        protected abstract byte Code { get; }
        protected abstract byte[] PayloadEncode();

        public byte[] Encode() {
            byte[] payload = this.PayloadEncode();

            if (payload.Length == 0) {
                return new byte[] { this.Code };
            }

            byte[] bytes = new byte[1 + payload.Length];
            bytes[0] = this.Code;
            Array.Copy(payload, 0, bytes, 1, payload.Length);
            return bytes;
        }
    }

    public enum ClientOperation : byte {
        /// <summary>
        /// The client is sending data to the server to keep the socket alive. Has no payload.
        /// </summary>
        Ping = 1,
        /// <summary>
        /// The client has a message to be sent in the game and is relaying it to the server.
        /// </summary>
        Message = 2,
        /// <summary>
        /// The client is shutting down. Clients should send this and close their socket for a clean shutdown.
        /// </summary>
        Shutdown = 3,
        Backlog = 4,
        CatchUp = 5,
        PlayerList = 6,
        LinkshellList = 7,
    }

    public class MillisecondsDateTimeFormatter : IMessagePackFormatter<DateTime> {
        private static readonly DateTime Epoch = new DateTime(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);

        public DateTime Deserialize(ref MessagePackReader reader, MessagePackSerializerOptions options) {
            var millis = reader.ReadInt64();
            return Epoch.AddMilliseconds(millis);
        }

        public void Serialize(ref MessagePackWriter writer, DateTime value, MessagePackSerializerOptions options) {
            var millis = (long)(value.ToUniversalTime() - Epoch).TotalMilliseconds;
            writer.WriteInt64(millis);
        }
    }
}
