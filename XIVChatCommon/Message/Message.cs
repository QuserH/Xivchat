using System;
using MessagePack;
using MessagePack.Formatters;

namespace XIVChatCommon.Message {
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

        public TextChunk(string content) {
            this.Content = content;
        }

        public TextChunk(uint? fallbackColour, uint? foreground, uint? glow, bool italic, string content) {
            this.FallbackColour = fallbackColour;
            this.Foreground = foreground;
            this.Glow = glow;
            this.Italic = italic;
            this.Content = content;
        }
    }

    [MessagePackObject]
    public class IconChunk : Chunk {
        [Key(0)]
        public byte index;
    }

    public enum InputChannel : uint {
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

    public static class InputChannelExt {
        public static uint LinkshellIndex(this InputChannel channel) => channel switch {
            InputChannel.Linkshell1 => 0,
            InputChannel.Linkshell2 => 1,
            InputChannel.Linkshell3 => 2,
            InputChannel.Linkshell4 => 3,
            InputChannel.Linkshell5 => 4,
            InputChannel.Linkshell6 => 5,
            InputChannel.Linkshell7 => 6,
            InputChannel.Linkshell8 => 7,
            InputChannel.CrossLinkshell1 => 0,
            InputChannel.CrossLinkshell2 => 1,
            InputChannel.CrossLinkshell3 => 2,
            InputChannel.CrossLinkshell4 => 3,
            InputChannel.CrossLinkshell5 => 4,
            InputChannel.CrossLinkshell6 => 5,
            InputChannel.CrossLinkshell7 => 6,
            InputChannel.CrossLinkshell8 => 7,
            _ => 0,
        };
    }

    public enum PlayerListType : byte {
        Party = 1,
        Friend = 2,
        Linkshell = 3,
        CrossLinkshell = 4,
        Targeting = 5,
    }

    [MessagePackObject]
    public class Player {
        [Key(0)]
        public string? Name { get; set; }

        [Key(1)]
        public string? FreeCompany { get; set; }

        [Key(2)]
        public ulong Status { get; set; }

        [Key(3)]
        public ushort CurrentWorld { get; set; }

        [Key(4)]
        public string? CurrentWorldName { get; set; }

        [Key(5)]
        public ushort HomeWorld { get; set; }

        [Key(6)]
        public string? HomeWorldName { get; set; }

        [Key(7)]
        public uint Territory { get; set; }

        [Key(8)]
        public string? TerritoryName { get; set; }

        [Key(9)]
        public byte Job { get; set; }

        [Key(10)]
        public string? JobName { get; set; }

        [Key(11)]
        public byte GrandCompany { get; set; }

        [Key(12)]
        public string? GrandCompanyName { get; set; }

        [Key(13)]
        public byte Languages { get; set; }

        [Key(14)]
        public byte MainLanguage { get; set; }

        [Key(15)]
        public ulong ContentId { get; set; }

        public bool HasStatus(PlayerStatus status) => (this.Status & ((ulong) 1 << (int) status)) > 0;
    }

    public enum PlayerStatus {
        GameQa = 1,
        GameMaster1 = 2,
        GameMaster2 = 3,
        EventParticipant = 4,
        Disconnected = 5,
        WaitingForFriendListApproval = 6,
        WaitingForLinkshellApproval = 7,
        WaitingForFreeCompanyApproval = 8,
        NotFound = 9,
        Offline = 10,
        BattleMentor = 11,
        Busy = 12,
        Pvp = 13,
        PlayingTripleTriad = 14,
        ViewingCutscene = 15,
        UsingAChocoboPorter = 16,
        AwayFromKeyboard = 17,
        CameraMode = 18,
        LookingForRepairs = 19,
        LookingToRepair = 20,
        LookingToMeldMateria = 21,
        RolePlaying = 22,
        LookingForParty = 23,
        SwordForHire = 24,
        WaitingForDutyFinder = 25,
        RecruitingPartyMembers = 26,
        Mentor = 27,
        PveMentor = 28,
        TradeMentor = 29,
        PvpMentor = 30,
        Returner = 31,
        NewAdventurer = 32,
        AllianceLeader = 33,
        AlliancePartyLeader = 34,
        AlliancePartyMember = 35,
        PartyLeader = 36,
        PartyMember = 37,
        PartyLeaderCrossWorld = 38,
        PartyMemberCrossWorld = 39,
        AnotherWorld = 40,
        SharingDuty = 41,
        SimilarDuty = 42,
        InDuty = 43,
        TrialAdventurer = 44,
        FreeCompany = 45,
        GrandCompany = 46,
        Online = 47,
    }

    // ReSharper disable once IdentifierTypo
    public abstract class Encodable {
        protected abstract byte Code { get; }
        protected abstract byte[] PayloadEncode();

        public byte[] Encode() {
            byte[] payload = this.PayloadEncode();

            if (payload.Length == 0) {
                return [
                    this.Code,
                ];
            }

            byte[] bytes = new byte[1 + payload.Length];
            bytes[0] = this.Code;
            Array.Copy(payload, 0, bytes, 1, payload.Length);
            return bytes;
        }
    }

    public class MillisecondsDateTimeFormatter : IMessagePackFormatter<DateTime> {
        private static readonly DateTime Epoch = new(1970, 1, 1, 0, 0, 0, 0, DateTimeKind.Utc);

        public DateTime Deserialize(ref MessagePackReader reader, MessagePackSerializerOptions options) {
            var millis = reader.ReadInt64();
            return Epoch.AddMilliseconds(millis);
        }

        public void Serialize(ref MessagePackWriter writer, DateTime value, MessagePackSerializerOptions options) {
            var millis = (long) (value.ToUniversalTime() - Epoch).TotalMilliseconds;
            writer.WriteInt64(millis);
        }
    }
}
