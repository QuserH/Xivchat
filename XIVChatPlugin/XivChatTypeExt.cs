using Dalamud.Game.Text;

namespace XIVChatPlugin;

public static class XivChatTypeExt {
    public static XivChatType Parent(this XivChatType type) => type switch {
        XivChatType.Say => XivChatType.Say,
        XivChatType.GmSay => XivChatType.Say,
        XivChatType.Shout => XivChatType.Shout,
        XivChatType.GmShout => XivChatType.Shout,
        XivChatType.TellOutgoing => XivChatType.TellOutgoing,
        XivChatType.TellIncoming => XivChatType.TellOutgoing,
        XivChatType.GmTell => XivChatType.TellOutgoing,
        XivChatType.Party => XivChatType.Party,
        XivChatType.CrossParty => XivChatType.Party,
        XivChatType.GmParty => XivChatType.Party,
        XivChatType.Ls1 => XivChatType.Ls1,
        XivChatType.GmLinkshell1 => XivChatType.Ls1,
        XivChatType.Ls2 => XivChatType.Ls2,
        XivChatType.GmLinkshell2 => XivChatType.Ls2,
        XivChatType.Ls3 => XivChatType.Ls3,
        XivChatType.GmLinkshell3 => XivChatType.Ls3,
        XivChatType.Ls4 => XivChatType.Ls4,
        XivChatType.GmLinkshell4 => XivChatType.Ls4,
        XivChatType.Ls5 => XivChatType.Ls5,
        XivChatType.GmLinkshell5 => XivChatType.Ls5,
        XivChatType.Ls6 => XivChatType.Ls6,
        XivChatType.GmLinkshell6 => XivChatType.Ls6,
        XivChatType.Ls7 => XivChatType.Ls7,
        XivChatType.GmLinkshell7 => XivChatType.Ls7,
        XivChatType.Ls8 => XivChatType.Ls8,
        XivChatType.GmLinkshell8 => XivChatType.Ls8,
        XivChatType.FreeCompany => XivChatType.FreeCompany,
        XivChatType.GmFreeCompany => XivChatType.FreeCompany,
        XivChatType.NoviceNetwork => XivChatType.NoviceNetwork,
        XivChatType.GmNoviceNetwork => XivChatType.NoviceNetwork,
        XivChatType.CustomEmote => XivChatType.CustomEmote,
        XivChatType.StandardEmote => XivChatType.StandardEmote,
        XivChatType.Yell => XivChatType.Yell,
        XivChatType.GmYell => XivChatType.Yell,
        XivChatType.GainBuff => XivChatType.GainBuff,
        XivChatType.LoseBuff => XivChatType.GainBuff,
        XivChatType.GainDebuff => XivChatType.GainDebuff,
        XivChatType.LoseDebuff => XivChatType.GainDebuff,
        XivChatType.SystemMessage => XivChatType.SystemMessage,
        XivChatType.Alarm => XivChatType.SystemMessage,
        XivChatType.RetainerSale => XivChatType.SystemMessage,
        XivChatType.PeriodicRecruitmentNotification => XivChatType.SystemMessage,
        XivChatType.Sign => XivChatType.SystemMessage,
        XivChatType.Orchestrion => XivChatType.SystemMessage,
        XivChatType.MessageBook => XivChatType.SystemMessage,
        XivChatType.NPCDialogue => XivChatType.NPCDialogue,
        XivChatType.NPCDialogueAnnouncements => XivChatType.NPCDialogueAnnouncements,
        XivChatType.LootRoll => XivChatType.LootRoll,
        XivChatType.RandomNumber => XivChatType.LootRoll,
        XivChatType.FreeCompanyAnnouncement => XivChatType.FreeCompanyAnnouncement,
        XivChatType.FreeCompanyLoginLogout => XivChatType.FreeCompanyAnnouncement,
        XivChatType.PvpTeamAnnouncement => XivChatType.PvpTeamAnnouncement,
        XivChatType.PvpTeamLoginLogout => XivChatType.PvpTeamAnnouncement,
        _ => type,
    };

    //public string ConfigKey() {
    //    switch (this.Type) {
    //        case XivChatType.Say:
    //        case XivChatType.GmSay:
    //            return "ColorSay";
    //        case XivChatType.Shout:
    //        case XivChatType.GmShout:
    //            return "ColorShout";
    //        case XivChatType.TellOutgoing:
    //        case XivChatType.TellIncoming:
    //        case XivChatType.GmTell:
    //            return "ColorTell";
    //        case XivChatType.Party:
    //        case XivChatType.CrossParty:
    //        case XivChatType.GmParty:
    //            return "ColorParty";
    //        case XivChatType.Alliance:
    //            return "ColorAlliance";
    //        case XivChatType.Ls1:
    //        case XivChatType.GmLinkshell1:
    //            return "ColorLS1";
    //        case XivChatType.Ls2:
    //        case XivChatType.GmLinkshell2:
    //            return "ColorLS2";
    //        case XivChatType.Ls3:
    //        case XivChatType.GmLinkshell3:
    //            return "ColorLS3";
    //        case XivChatType.Ls4:
    //        case XivChatType.GmLinkshell4:
    //            return "ColorLS4";
    //        case XivChatType.Ls5:
    //        case XivChatType.GmLinkshell5:
    //            return "ColorLS5";
    //        case XivChatType.Ls6:
    //        case XivChatType.GmLinkshell6:
    //            return "ColorLS6";
    //        case XivChatType.Ls7:
    //        case XivChatType.GmLinkshell7:
    //            return "ColorLS7";
    //        case XivChatType.Ls8:
    //        case XivChatType.GmLinkshell8:
    //            return "ColorLS8";
    //        case XivChatType.FreeCompany:
    //        case XivChatType.GmFreeCompany:
    //            return "ColorFCompany";
    //        case XivChatType.NoviceNetwork:
    //        case XivChatType.GmNoviceNetwork:
    //            return "ColorBeginner";
    //        case XivChatType.CustomEmote:
    //            return "ColorEmoteUser";
    //        case XivChatType.StandardEmote:
    //            return "ColorEmote";
    //        case XivChatType.Yell:
    //        case XivChatType.GmYell:
    //            return "ColorYell";
    //        case XivChatType.PvpTeam:
    //            return "ColorPvPGroup";
    //        case XivChatType.CrossLinkShell1:
    //            return "ColorCWLS";
    //        case XivChatType.Damage:
    //            return "ColorAttackSuccess";
    //        case XivChatType.Miss:
    //            return "ColorAttackFailure";
    //        case XivChatType.Action:
    //            return "ColorAction";
    //        case XivChatType.Item:
    //            return "ColorItem";
    //        case XivChatType.Healing:
    //            return "ColorCureGive";
    //        case XivChatType.GainBuff:
    //        case XivChatType.GainDebuff:
    //            return "ColorBuffGive";
    //        case XivChatType.LoseBuff:
    //        case XivChatType.LoseDebuff:
    //            return "ColorDebuffGive";
    //        case XivChatType.Echo:
    //            return "ColorEcho";
    //        case XivChatType.System:
    //        case XivChatType.Alarm:
    //        case XivChatType.RetainerSale:
    //        case XivChatType.PeriodicRecruitmentNotification:
    //        case XivChatType.Sign:
    //        case XivChatType.Orchestrion:
    //        case XivChatType.MessageBook:
    //            return "ColorSysMsg";
    //        case XivChatType.BattleSystem:
    //            return "ColorSysBattle";
    //        case XivChatType.GatheringSystem:
    //            return "ColorSysGathering";
    //        case XivChatType.Error:
    //            return "ColorSysError";
    //        case XivChatType.NpcDialogue:
    //        case XivChatType.NpcAnnouncement:
    //            return "ColorNpcSay";
    //        case XivChatType.LootNotice:
    //            return "ColorItemNotice";
    //        case XivChatType.Progress:
    //            return "ColorGrowup";
    //        case XivChatType.LootRoll:
    //        case XivChatType.RandomNumber:
    //            return "ColorLoot";
    //        case XivChatType.Crafting:
    //            return "ColorCraft";
    //        case XivChatType.Gathering:
    //            return "ColorGathering";
    //        case XivChatType.FreeCompanyAnnouncement:
    //        case XivChatType.FreeCompanyLoginLogout:
    //            return "ColorFCAnnounce";
    //        case XivChatType.NoviceNetworkSystem:
    //            return "ColorBeginnerAnnounce";
    //        case XivChatType.PvpTeamAnnouncement:
    //        case XivChatType.PvpTeamLoginLogout:
    //            return "ColorPvPGroupAnnounce";
    //        case XivChatType.CrossLinkShell2:
    //            return "ColorCWLS2";
    //        case XivChatType.CrossLinkShell3:
    //            return "ColorCWLS3";
    //        case XivChatType.CrossLinkShell4:
    //            return "ColorCWLS4";
    //        case XivChatType.CrossLinkShell5:
    //            return "ColorCWLS5";
    //        case XivChatType.CrossLinkShell6:
    //            return "ColorCWLS6";
    //        case XivChatType.CrossLinkShell7:
    //            return "ColorCWLS7";
    //        case XivChatType.CrossLinkShell8:
    //            return "ColorCWLS8";
    //        default:
    //            return null;
    //    }
    //}

    public static bool IsBattle(this XivChatType type) {
        switch (type) {
            case XivChatType.Damage:
            case XivChatType.Miss:
            case XivChatType.Action:
            case XivChatType.Item:
            case XivChatType.Healing:
            case XivChatType.GainBuff:
            case XivChatType.LoseBuff:
            case XivChatType.GainDebuff:
            case XivChatType.LoseDebuff:
            case XivChatType.SystemError:
                return true;
            default:
                return false;
        }
    }

    public static uint? DefaultColour(this XivChatType type) {
        switch (type) {
            case XivChatType.Debug:
                return Rgba(204, 204, 204);
            case XivChatType.Urgent:
                return Rgba(255, 127, 127);
            case XivChatType.Notice:
                return Rgba(179, 140, 255);

            case XivChatType.Say:
                return Rgba(247, 247, 247);
            case XivChatType.Shout:
                return Rgba(255, 166, 102);
            case XivChatType.TellIncoming:
            case XivChatType.TellOutgoing:
            case XivChatType.GmTell:
                return Rgba(255, 184, 222);
            case XivChatType.Party:
            case XivChatType.CrossParty:
                return Rgba(102, 229, 255);
            case XivChatType.Alliance:
                return Rgba(255, 127, 0);
            case XivChatType.NoviceNetwork:
            case XivChatType.NoviceNetworkSystem:
                return Rgba(212, 255, 125);
            case XivChatType.Ls1:
            case XivChatType.Ls2:
            case XivChatType.Ls3:
            case XivChatType.Ls4:
            case XivChatType.Ls5:
            case XivChatType.Ls6:
            case XivChatType.Ls7:
            case XivChatType.Ls8:
            case XivChatType.CrossLinkShell1:
            case XivChatType.CrossLinkShell2:
            case XivChatType.CrossLinkShell3:
            case XivChatType.CrossLinkShell4:
            case XivChatType.CrossLinkShell5:
            case XivChatType.CrossLinkShell6:
            case XivChatType.CrossLinkShell7:
            case XivChatType.CrossLinkShell8:
                return Rgba(212, 255, 125);
            case XivChatType.StandardEmote:
                return Rgba(186, 255, 240);
            case XivChatType.CustomEmote:
                return Rgba(186, 255, 240);
            case XivChatType.Yell:
                return Rgba(255, 255, 0);
            case XivChatType.Echo:
                return Rgba(204, 204, 204);
            case XivChatType.SystemMessage:
            case XivChatType.GatheringSystemMessage:
            case XivChatType.PeriodicRecruitmentNotification:
            case XivChatType.Orchestrion:
            case XivChatType.Alarm:
            case XivChatType.RetainerSale:
            case XivChatType.Sign:
            case XivChatType.MessageBook:
                return Rgba(204, 204, 204);
            case XivChatType.NPCDialogueAnnouncements:
            case XivChatType.NPCDialogue:
                return Rgba(171, 214, 71);
            case XivChatType.ErrorMessage:
                return Rgba(255, 74, 74);
            case XivChatType.FreeCompany:
            case XivChatType.FreeCompanyAnnouncement:
            case XivChatType.FreeCompanyLoginLogout:
                return Rgba(171, 219, 229);
            case XivChatType.PvPTeam:
                return Rgba(171, 219, 229);
            case XivChatType.PvpTeamAnnouncement:
            case XivChatType.PvpTeamLoginLogout:
                return Rgba(171, 219, 229);
            case XivChatType.Action:
            case XivChatType.Item:
            case XivChatType.LootNotice:
                return Rgba(255, 255, 176);
            case XivChatType.Progress:
                return Rgba(255, 222, 115);
            case XivChatType.LootRoll:
            case XivChatType.RandomNumber:
                return Rgba(199, 191, 158);
            case XivChatType.Crafting:
            case XivChatType.Gathering:
                return Rgba(222, 191, 247);
            case XivChatType.Damage:
                return Rgba(255, 125, 125);
            case XivChatType.Miss:
                return Rgba(204, 204, 204);
            case XivChatType.Healing:
                return Rgba(212, 255, 125);
            case XivChatType.GainBuff:
            case XivChatType.LoseBuff:
                return Rgba(148, 191, 255);
            case XivChatType.GainDebuff:
            case XivChatType.LoseDebuff:
                return Rgba(255, 138, 196);
            case XivChatType.SystemError:
                return Rgba(204, 204, 204);
            default:
                return null;
        }
    }

    private static uint Rgba(byte red, byte green, byte blue, byte alpha = 0xFF) => alpha
                                                                                    | (uint) (red << 24)
                                                                                    | (uint) (green << 16)
                                                                                    | (uint) (blue << 8);

    public static string? Name(this XivChatType type) {
        return type switch {
            XivChatType.Debug => "Debug",
            XivChatType.Urgent => "Urgent",
            XivChatType.Notice => "Notice",
            XivChatType.Say => "Say",
            XivChatType.Shout => "Shout",
            XivChatType.TellOutgoing => "Tell (Outgoing)",
            XivChatType.TellIncoming => "Tell (Incoming)",
            XivChatType.Party => "Party",
            XivChatType.Alliance => "Alliance",
            XivChatType.Ls1 => "Linkshell [1]",
            XivChatType.Ls2 => "Linkshell [2]",
            XivChatType.Ls3 => "Linkshell [3]",
            XivChatType.Ls4 => "Linkshell [4]",
            XivChatType.Ls5 => "Linkshell [5]",
            XivChatType.Ls6 => "Linkshell [6]",
            XivChatType.Ls7 => "Linkshell [7]",
            XivChatType.Ls8 => "Linkshell [8]",
            XivChatType.FreeCompany => "Free Company",
            XivChatType.NoviceNetwork => "Novice Network",
            XivChatType.CustomEmote => "Custom Emotes",
            XivChatType.StandardEmote => "Standard Emotes",
            XivChatType.Yell => "Yell",
            XivChatType.CrossParty => "Cross-world Party",
            XivChatType.PvPTeam => "PvP Team",
            XivChatType.CrossLinkShell1 => "Cross-world Linkshell [1]",
            XivChatType.Damage => "Damage dealt",
            XivChatType.Miss => "Failed attacks",
            XivChatType.Action => "Actions used",
            XivChatType.Item => "Items used",
            XivChatType.Healing => "Healing",
            XivChatType.GainBuff => "Beneficial effects granted",
            XivChatType.GainDebuff => "Detrimental effects inflicted",
            XivChatType.LoseBuff => "Beneficial effects lost",
            XivChatType.LoseDebuff => "Detrimental effects cured",
            XivChatType.Alarm => "Alarm Notifications",
            XivChatType.Echo => "Echo",
            XivChatType.SystemMessage => "System Messages",
            XivChatType.SystemError => "Battle System Messages",
            XivChatType.GatheringSystemMessage => "Gathering System Messages",
            XivChatType.ErrorMessage => "Error Messages",
            XivChatType.NPCDialogue => "NPC Dialogue",
            XivChatType.LootNotice => "Loot Notices",
            XivChatType.Progress => "Progression Messages",
            XivChatType.LootRoll => "Loot Messages",
            XivChatType.Crafting => "Synthesis Messages",
            XivChatType.Gathering => "Gathering Messages",
            XivChatType.NPCDialogueAnnouncements => "NPC Dialogue (Announcements)",
            XivChatType.FreeCompanyAnnouncement => "Free Company Announcements",
            XivChatType.FreeCompanyLoginLogout => "Free Company Member Login Notifications",
            XivChatType.RetainerSale => "Retainer Sale Notifications",
            XivChatType.PeriodicRecruitmentNotification => "Periodic Recruitment Notifications",
            XivChatType.Sign => "Sign Messages for PC Targets",
            XivChatType.RandomNumber => "Random Number Messages",
            XivChatType.NoviceNetworkSystem => "Novice Network Notifications",
            XivChatType.Orchestrion => "Current Orchestrion Track Messages",
            XivChatType.PvpTeamAnnouncement => "PvP Team Announcements",
            XivChatType.PvpTeamLoginLogout => "PvP Team Member Login Notifications",
            XivChatType.MessageBook => "Message Book Alert",
            XivChatType.GmTell => "Tell (GM)",
            XivChatType.GmSay => "Say (GM)",
            XivChatType.GmShout => "Shout (GM)",
            XivChatType.GmYell => "Yell (GM)",
            XivChatType.GmParty => "Party (GM)",
            XivChatType.GmFreeCompany => "Free Company (GM)",
            XivChatType.GmLinkshell1 => "Linkshell [1] (GM)",
            XivChatType.GmLinkshell2 => "Linkshell [2] (GM)",
            XivChatType.GmLinkshell3 => "Linkshell [3] (GM)",
            XivChatType.GmLinkshell4 => "Linkshell [4] (GM)",
            XivChatType.GmLinkshell5 => "Linkshell [5] (GM)",
            XivChatType.GmLinkshell6 => "Linkshell [6] (GM)",
            XivChatType.GmLinkshell7 => "Linkshell [7] (GM)",
            XivChatType.GmLinkshell8 => "Linkshell [8] (GM)",
            XivChatType.GmNoviceNetwork => "Novice Network (GM)",
            XivChatType.CrossLinkShell2 => "Cross-world Linkshell [2]",
            XivChatType.CrossLinkShell3 => "Cross-world Linkshell [3]",
            XivChatType.CrossLinkShell4 => "Cross-world Linkshell [4]",
            XivChatType.CrossLinkShell5 => "Cross-world Linkshell [5]",
            XivChatType.CrossLinkShell6 => "Cross-world Linkshell [6]",
            XivChatType.CrossLinkShell7 => "Cross-world Linkshell [7]",
            XivChatType.CrossLinkShell8 => "Cross-world Linkshell [8]",
            _ => type.ToString(),
        };
    }
}
