using Dalamud.Hooking;
using System;
using System.Collections.Generic;
using Dalamud.Game.Text.SeStringHandling;
using Dalamud.Utility.Signatures;
using FFXIVClientStructs.FFXIV.Client.System.Framework;
using FFXIVClientStructs.FFXIV.Client.System.Memory;
using FFXIVClientStructs.FFXIV.Client.System.String;
using FFXIVClientStructs.FFXIV.Client.UI;
using FFXIVClientStructs.FFXIV.Client.UI.Agent;
using FFXIVClientStructs.FFXIV.Client.UI.Info;
using Lumina.Excel.Sheets;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Server;
using Dalamud.Game.Text;
using ClientGrandCompany = FFXIVClientStructs.FFXIV.Client.UI.Agent.GrandCompany;
using LuminaGrandCompany = Lumina.Excel.Sheets.GrandCompany;

namespace XIVChatPlugin {
    internal unsafe class GameFunctions : IDisposable {
        private static class Signatures {
            internal const string Input = "E8 ?? ?? ?? ?? ?? ?? ?? 84 C0 B9";
            internal const string InputAfk = "E8 ?? ?? ?? ?? 84 C0 74 ?? 66 83 3D";

            internal const string GetColour = "48 89 5C 24 ?? 48 89 6C 24 ?? 48 89 74 24 ?? 57 48 83 EC 20 8B F2 48 8D B9";

            internal const string Channel = "E8 ?? ?? ?? ?? 33 C0 EB ?? 85 D2";
            internal const string ChannelCommand = "E8 ?? ?? ?? ?? 0F B7 44 37";
            internal const string ChannelNameChange = "E8 ?? ?? ?? ?? BA ?? ?? ?? ?? 48 8D 4D B0 48 8B F8 E8 ?? ?? ?? ?? 41 8B D6";
            internal const string ColourLookup = "48 8D 0D ?? ?? ?? ?? ?? ?? ?? 85 D2 7E";
        }

        private Plugin Plugin { get; }

        #region Delegates

        private delegate byte IsInputDelegate(nint a1);

        private delegate byte IsInputAfkDelegate();

        private delegate void EndFriendListRequestDelegate(InfoProxyFriendList* proxy);

        private delegate nint GetColourInfoDelegate(nint handler, uint lookupResult);

        private delegate byte ChatChannelChangeDelegate(nint a1, uint channel);

        private delegate nint ChatChannelChangeNameDelegate(nint a1);

        private delegate nint ChannelChangeCommandDelegate(nint a1, int inputChannel, uint linkshellIdx, nint tellTarget, char canChangeChannel);

        #endregion

        #region Hooks

        [Signature(Signatures.Input, DetourName = nameof(IsInputDetour))]
        private readonly Hook<IsInputDelegate>? _isInputHook;

        [Signature(Signatures.InputAfk, DetourName = nameof(IsInputAfkDetour))]
        private readonly Hook<IsInputAfkDelegate>? _isInputAfkHook;

        [Signature(Signatures.Channel, DetourName = nameof(ChangeChatChannelDetour))]
        private readonly Hook<ChatChannelChangeDelegate>? _chatChannelChangeHook;

        [Signature(Signatures.ChannelNameChange, DetourName = nameof(ChangeChatChannelNameDetour))]
        private readonly Hook<ChatChannelChangeNameDelegate>? _chatChannelChangeNameHook;

        #endregion

        #region Functions

        [Signature(Signatures.GetColour)]
        private readonly GetColourInfoDelegate? _getColourInfo;

        [Signature(Signatures.ChannelCommand)]
        private readonly ChannelChangeCommandDelegate? _channelChangeCommand;

        #endregion

        #region Pointers

        [Signature(Signatures.ColourLookup, ScanType = ScanType.StaticAddress)]
        private nint ColourLookup { get; init; }

        #endregion

        public ServerHousingLocation HousingLocation {
            get {
                var info = XIVChatPlugin.HousingLocation.Current();
                if (info == null) {
                    return new ServerHousingLocation(null, null, false, null);
                }

                var ward = info.Ward;
                var plot = info.Plot ?? info.Yard ?? info.Apartment;
                var wing = (byte?) info.ApartmentWing;
                var exterior = info.Yard != null;

                return new ServerHousingLocation(ward, plot, exterior, wing);
            }
        }

        [Flags]
        private enum InputSetters {
            None = 0,
            Normal = 1 << 0,
            Afk = 1 << 1,
        }

        private InputSetters HadInput { get; set; } = InputSetters.None;
        private Hook<EndFriendListRequestDelegate>? _friendListEndHook;
        private InfoProxyFriendList* _friendListProxy;
        private nint _chatManager = nint.Zero;
        private readonly nint _emptyXivString;

        internal bool RequestingFriendList { get; private set; }

        internal delegate void ReceiveFriendListHandler(List<Player> friends);

        internal event ReceiveFriendListHandler? ReceiveFriendList;

        internal GameFunctions(Plugin plugin) {
            this.Plugin = plugin;

            this.Plugin.GameInteropProvider.InitializeFromAttributes(this);

            this._chatChannelChangeHook?.Enable();
            this._chatChannelChangeNameHook?.Enable();
            this._isInputHook?.Enable();
            this._isInputAfkHook?.Enable();

            this._emptyXivString = (nint) Utf8String.CreateEmpty();
        }

        private byte IsInputDetour(nint a1) {
            if (!this.Plugin.Config.MessagesCountAsInput || this.HadInput == InputSetters.None) {
                return this._isInputHook!.Original(a1);
            }

            this.HadInput &= ~InputSetters.Normal;
            return 1;
        }

        private byte IsInputAfkDetour() {
            if (!this.Plugin.Config.MessagesCountAsInput || this.HadInput == InputSetters.None) {
                return this._isInputAfkHook!.Original();
            }

            this.HadInput &= ~InputSetters.Afk;
            return 1;
        }

        internal void ChangeChatChannel(InputChannel channel) {
            if (this._chatManager == nint.Zero || this._channelChangeCommand == null || this._emptyXivString == nint.Zero) {
                return;
            }

            this._channelChangeCommand(this._chatManager, (int) channel, channel.LinkshellIndex(), this._emptyXivString, '\x01');
        }

        // This function looks up a channel's user-defined colour.
        //
        // If this function would ever return 0, it returns null instead.
        internal uint? GetChannelColour(XivChatType channel) {
            if (this._getColourInfo == null || this.ColourLookup == nint.Zero) {
                return null;
            }

            // Colours are retrieved by looking up their code in a lookup table. Some codes share a colour, so they're lumped into a parent code here.
            // Only codes >= 10 (say) have configurable colours.
            // After getting the lookup value for the code, it is passed into a function with a handler which returns a pointer.
            // This pointer + 32 is the RGB value. This functions returns RGBA with A always max.

            var parent = channel.Parent();

            switch (parent) {
                case XivChatType.Debug:
                case XivChatType.Urgent:
                case XivChatType.Notice:
                    return channel.DefaultColour();
            }

            var framework = (nint) Framework.Instance();

            var lookupResult = *(uint*) (this.ColourLookup + (int) parent * 4);
            var info = this._getColourInfo(framework + 16, lookupResult);
            var rgb = *(uint*) (info + 32) & 0xFFFFFF;

            if (rgb == 0) {
                return null;
            }

            return 0xFF | (rgb << 8);
        }

        internal void ProcessChatBox(string message) {
            var uiModule = UIModule.Instance();
            if (uiModule == null) {
                return;
            }

            this.HadInput = InputSetters.Normal | InputSetters.Afk;

            var payload = Utf8String.FromString(message);
            try {
                uiModule->ProcessChatBoxEntry(payload, nint.Zero, false);
            } finally {
                payload->Dtor();
                IMemorySpace.Free(payload);
            }
        }

        internal bool RequestFriendList() {
            var proxy = InfoProxyFriendList.Instance();
            if (proxy == null) {
                return false;
            }

            this._friendListProxy = proxy;
            if (this._friendListEndHook == null) {
                var endRequest = (nint) proxy->VirtualTable->EndRequest;
                this._friendListEndHook = this.Plugin.GameInteropProvider.HookFromAddress<EndFriendListRequestDelegate>(
                    endRequest,
                    this.OnEndFriendListRequest
                );
                this._friendListEndHook.Enable();
            }

            this.RequestingFriendList = true;
            if (proxy->RequestData()) {
                return true;
            }

            if (proxy->EntryCount > 0) {
                this.CompleteFriendListRequest(proxy);
                return true;
            }

            this.RequestingFriendList = false;
            return false;
        }

        private byte ChangeChatChannelDetour(nint a1, uint channel) {
            this._chatManager = a1;
            // Last ShB patch
            // a1 + 0xfd0 is the chat channel byte (including for when clicking on shout)
            return this._chatChannelChangeHook!.Original(a1, channel);
        }

        private nint ChangeChatChannelNameDetour(nint a1) {
            // Last ShB patch
            // +0x40 = chat channel (byte or uint?)
            //         channel is 17 (maybe 18?) for tells
            // +0x48 = pointer to channel name string
            var ret = this._chatChannelChangeNameHook!.Original(a1);
            if (a1 == nint.Zero) {
                return ret;
            }

            var agent = AgentChatLog.Instance();
            if (agent == null) {
                return ret;
            }

            var channel = (uint) agent->CurrentChannel;

            // Tell channels use a different internal chat-log state. On current
            // game builds ChannelLabel may not be initialized for these channels;
            // reading it here can stall or crash the Dalamud framework thread.
            if (channel is 17 or 18) {
                return ret;
            }

            var label = SeString.Parse(agent->ChannelLabel.AsSpan());

            this.Plugin.Server.OnChatChannelChange(channel, label);

            return ret;
        }

        private void OnEndFriendListRequest(InfoProxyFriendList* proxy) {
            this._friendListEndHook!.Original(proxy);

            if (this.RequestingFriendList && proxy == this._friendListProxy) {
                this.CompleteFriendListRequest(proxy);
            }
        }

        private void CompleteFriendListRequest(InfoProxyFriendList* proxy) {
            var friends = new List<Player>();

            foreach (ref readonly var entry in proxy->CharDataSpan) {
                friends.Add(new Player {
                    Name = entry.NameString,
                    FreeCompany = entry.FCTagString,
                    Status = (ulong) entry.State,
                    CurrentWorld = entry.CurrentWorld,
                    CurrentWorldName = this.WorldName(entry.CurrentWorld),
                    HomeWorld = entry.HomeWorld,
                    HomeWorldName = this.WorldName(entry.HomeWorld),
                    Territory = entry.Location,
                    TerritoryName = this.TerritoryName(entry.Location),
                    Job = entry.Job,
                    JobName = this.JobName(entry.Job),
                    GrandCompany = (byte) entry.GrandCompany,
                    GrandCompanyName = this.GrandCompanyName(entry.GrandCompany),
                    Languages = (byte) entry.Languages,
                    MainLanguage = (byte) entry.ClientLanguage,
                });
            }

            this.RequestingFriendList = false;
            this.ReceiveFriendList?.Invoke(friends);
        }

        private string? WorldName(ushort id) {
            return this.Plugin.DataManager.GetExcelSheet<World>().GetRowOrDefault(id)?.Name.ExtractText();
        }

        private string? JobName(byte id) {
            return id == 0
                ? null
                : this.Plugin.DataManager.GetExcelSheet<ClassJob>().GetRowOrDefault(id)?.Name.ExtractText();
        }

        private string? TerritoryName(ushort id) {
            try {
                var row = this.Plugin.DataManager.GetExcelSheet<TerritoryType>().GetRowOrDefault(id);
                return row is { PlaceName.IsValid: true }
                    ? row.Value.PlaceName.Value.Name.ExtractText()
                    : null;
            } catch (NullReferenceException) {
                return null;
            }
        }

        private string? GrandCompanyName(ClientGrandCompany id) {
            return this.Plugin.DataManager.GetExcelSheet<LuminaGrandCompany>().GetRowOrDefault((uint) id)?.Name.ExtractText();
        }

        public void Dispose() {
            this._friendListEndHook?.Dispose();
            this._chatChannelChangeHook?.Dispose();
            this._chatChannelChangeNameHook?.Dispose();
            this._isInputHook?.Dispose();
            this._isInputAfkHook?.Dispose();

            if (this._emptyXivString != nint.Zero) {
                var str = (Utf8String*) this._emptyXivString;
                str->Dtor();
                IMemorySpace.Free(str);
            }
        }
    }

}
