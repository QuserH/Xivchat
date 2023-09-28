using Dalamud.Hooking;
using Lumina.Excel.GeneratedSheets;
using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using Dalamud.Game.Text.SeStringHandling;
using Dalamud.Memory;
using Dalamud.Utility.Signatures;
using FFXIVClientStructs.FFXIV.Client.System.Framework;
using XIVChatCommon.Message;
using XIVChatCommon.Message.Server;

namespace XIVChatPlugin {
    internal unsafe class GameFunctions : IDisposable {
        private static class Signatures {
            internal const string ProcessChat = "48 89 5C 24 ?? 57 48 83 EC 20 48 8B FA 48 8B D9 45 84 C9";
            internal const string Input = "80 B9 ?? ?? ?? ?? ?? 0F 9C C0";
            internal const string InputAfk = "E8 ?? ?? ?? ?? 0F 28 74 24 ?? 0F B6 F0";
            internal const string FriendList = "40 53 48 81 EC 80 0F 00 00 48 8B 05 ?? ?? ?? ?? 48 33 C4 48 89 84 24 ?? ?? ?? ?? 48 8B D9 48 8B 0D ?? ?? ?? ?? E8 ?? ?? ?? ?? 48 85 C0 0F 84 ?? ?? ?? ?? 44 0F B6 43 ?? 33 C9";
            internal const string Format = "48 89 5C 24 ?? 48 89 6C 24 ?? 48 89 7C 24 ?? 41 56 48 83 EC 30 48 8B 6C 24";
            internal const string ReceiveChunk = "48 89 5C 24 ?? 56 48 83 EC 20 48 8B 0D ?? ?? ?? ?? 48 8B F2";

            internal const string GetColour = "48 89 5C 24 ?? 48 89 6C 24 ?? 48 89 74 24 ?? 57 48 83 EC 20 8B F2 48 8D B9";

            internal const string Channel = "E8 ?? ?? ?? ?? E9 ?? ?? ?? ?? 85 D2 BB";
            internal const string ChannelCommand = "E8 ?? ?? ?? ?? 0F B7 44 37";
            internal const string ChannelNameChange = "E8 ?? ?? ?? ?? BA ?? ?? ?? ?? 48 8D 4D B0 48 8B F8 E8 ?? ?? ?? ?? 41 8B D6";
            internal const string XivStringCtor = "E8 ?? ?? ?? ?? 44 2B F7";
            internal const string XivStringDtor = "E8 ?? ?? ?? ?? B0 6E";
            internal const string ColourLookup = "48 8D 0D ?? ?? ?? ?? 8B 14 ?? 85 D2 7E ?? 48 8B 0D ?? ?? ?? ?? 48 83 C1 10 E8 ?? ?? ?? ?? 8B 70 ?? 41 8D 4D";
        }

        private Plugin Plugin { get; }

        #region Delegates

        private delegate void EasierProcessChatBoxDelegate(IntPtr uiModule, IntPtr message, IntPtr unused, byte a4);

        private delegate byte IsInputDelegate(IntPtr a1);

        private delegate byte IsInputAfkDelegate();

        private delegate byte RequestFriendListDelegate(IntPtr manager);

        private delegate int FormatFriendListNameDelegate(long a1, long a2, long a3, int a4, IntPtr data, long a6);

        private delegate IntPtr OnReceiveFriendListChunkDelegate(IntPtr a1, IntPtr data);

        private delegate IntPtr GetColourInfoDelegate(IntPtr handler, uint lookupResult);

        private delegate byte ChatChannelChangeDelegate(IntPtr a1, uint channel);

        private delegate IntPtr ChatChannelChangeNameDelegate(IntPtr a1);

        private delegate IntPtr ChannelChangeCommandDelegate(IntPtr a1, int inputChannel, uint linkshellIdx, IntPtr tellTarget, char canChangeChannel);

        private delegate IntPtr XivStringCtorDelegate(IntPtr memory);

        private delegate IntPtr XivStringDtorDelegate(IntPtr memory);

        #endregion

        #region Hooks

        [Signature(Signatures.Input, DetourName = nameof(IsInputDetour))]
        private readonly Hook<IsInputDelegate>? _isInputHook;

        [Signature(Signatures.InputAfk, DetourName = nameof(IsInputAfkDetour))]
        private readonly Hook<IsInputAfkDelegate>? _isInputAfkHook;

        [Signature(Signatures.FriendList, DetourName = nameof(OnRequestFriendList))]
        private readonly Hook<RequestFriendListDelegate>? _friendListHook;

        [Signature(Signatures.Format, DetourName = nameof(OnFormatFriendList))]
        private readonly Hook<FormatFriendListNameDelegate>? _formatHook;

        [Signature(Signatures.ReceiveChunk, DetourName = nameof(OnReceiveFriendList))]
        private readonly Hook<OnReceiveFriendListChunkDelegate>? _receiveChunkHook;

        [Signature(Signatures.Channel, DetourName = nameof(ChangeChatChannelDetour))]
        private readonly Hook<ChatChannelChangeDelegate>? _chatChannelChangeHook;

        [Signature(Signatures.ChannelNameChange, DetourName = nameof(ChangeChatChannelNameDetour))]
        private readonly Hook<ChatChannelChangeNameDelegate>? _chatChannelChangeNameHook;

        #endregion

        #region Functions

        [Signature(Signatures.ProcessChat)]
        private readonly EasierProcessChatBoxDelegate? _easierProcessChatBox;

        [Signature(Signatures.GetColour)]
        private readonly GetColourInfoDelegate? _getColourInfo;

        [Signature(Signatures.ChannelCommand)]
        private readonly ChannelChangeCommandDelegate? _channelChangeCommand;

        [Signature(Signatures.XivStringCtor)]
        private readonly XivStringCtorDelegate? _xivStringCtor;

        [Signature(Signatures.XivStringDtor)]
        private readonly XivStringDtorDelegate? _xivStringDtor;

        #endregion

        #region Pointers

        [Signature(Signatures.ColourLookup, ScanType = ScanType.StaticAddress)]
        private IntPtr ColourLookup { get; init; }

        #endregion

        public ServerHousingLocation HousingLocation {
            get {
                var info = this.Plugin.Common.Functions.Housing.Location;
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
        private IntPtr _friendListManager = IntPtr.Zero;
        private IntPtr _chatManager = IntPtr.Zero;
        private readonly IntPtr _emptyXivString = IntPtr.Zero;

        internal bool RequestingFriendList { get; private set; }

        private readonly List<Player> _friends = new();

        internal delegate void ReceiveFriendListHandler(List<Player> friends);

        internal event ReceiveFriendListHandler? ReceiveFriendList;

        internal GameFunctions(Plugin plugin) {
            this.Plugin = plugin;

            this.Plugin.GameInteropProvider.InitializeFromAttributes(this);

            this._friendListHook?.Enable();
            this._formatHook?.Enable();
            this._receiveChunkHook?.Enable();
            this._chatChannelChangeHook?.Enable();
            this._chatChannelChangeNameHook?.Enable();
            this._isInputHook?.Enable();
            this._isInputAfkHook?.Enable();

            if (this._xivStringCtor != null && this._xivStringDtor != null) {
                this._emptyXivString = Marshal.AllocHGlobal(0x68);
                this._xivStringCtor(this._emptyXivString);
            }
        }

        private byte IsInputDetour(IntPtr a1) {
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
            if (this._chatManager == IntPtr.Zero || this._channelChangeCommand == null || this._emptyXivString == IntPtr.Zero) {
                return;
            }

            this._channelChangeCommand(this._chatManager, (int) channel, channel.LinkshellIndex(), this._emptyXivString, '\x01');
        }

        // This function looks up a channel's user-defined colour.
        //
        // If this function would ever return 0, it returns null instead.
        internal uint? GetChannelColour(ChatCode channel) {
            if (this._getColourInfo == null || this.ColourLookup == IntPtr.Zero) {
                return null;
            }

            // Colours are retrieved by looking up their code in a lookup table. Some codes share a colour, so they're lumped into a parent code here.
            // Only codes >= 10 (say) have configurable colours.
            // After getting the lookup value for the code, it is passed into a function with a handler which returns a pointer.
            // This pointer + 32 is the RGB value. This functions returns RGBA with A always max.

            var parent = channel.Parent();

            switch (parent) {
                case ChatType.Debug:
                case ChatType.Urgent:
                case ChatType.Notice:
                    return channel.DefaultColour();
            }

            var framework = (IntPtr) Framework.Instance();

            var lookupResult = *(uint*) (this.ColourLookup + (int) parent * 4);
            var info = this._getColourInfo(framework + 16, lookupResult);
            var rgb = *(uint*) (info + 32) & 0xFFFFFF;

            if (rgb == 0) {
                return null;
            }

            return 0xFF | (rgb << 8);
        }

        internal void ProcessChatBox(string message) {
            if (this._easierProcessChatBox == null) {
                return;
            }

            this.HadInput = InputSetters.Normal | InputSetters.Afk;

            var uiModule = Framework.Instance()->GetUiModule();

            using var payload = new ChatPayload(message);
            var mem1 = Marshal.AllocHGlobal(400);
            Marshal.StructureToPtr(payload, mem1, false);

            this._easierProcessChatBox((IntPtr) uiModule, mem1, IntPtr.Zero, 0);

            Marshal.FreeHGlobal(mem1);
        }

        internal bool RequestFriendList() {
            if (this._friendListManager == IntPtr.Zero || this._friendListHook == null) {
                return false;
            }

            this.RequestingFriendList = true;
            this._friendListHook.Original(this._friendListManager);
            return true;
        }

        private byte ChangeChatChannelDetour(IntPtr a1, uint channel) {
            this._chatManager = a1;
            // Last ShB patch
            // a1 + 0xfd0 is the chat channel byte (including for when clicking on shout)
            return this._chatChannelChangeHook!.Original(a1, channel);
        }

        private IntPtr ChangeChatChannelNameDetour(IntPtr a1) {
            // Last ShB patch
            // +0x40 = chat channel (byte or uint?)
            //         channel is 17 (maybe 18?) for tells
            // +0x48 = pointer to channel name string
            var ret = this._chatChannelChangeNameHook!.Original(a1);
            if (a1 == IntPtr.Zero) {
                return ret;
            }

            var channel = *(uint*) (a1 + 0x40);
            if (channel is 17 or 18) {
                channel = 0;
            }

            SeString? name = null;
            var namePtrPtr = (byte**) (a1 + 0x48);
            if (namePtrPtr != null) {
                var namePtr = *namePtrPtr;
                name = MemoryHelper.ReadSeStringNullTerminated((IntPtr) namePtr);
                if (name.Payloads.Count == 0) {
                    name = null;
                }
            }

            if (name == null) {
                return ret;
            }

            this.Plugin.Server.OnChatChannelChange(channel, name);

            return ret;
        }

        private byte OnRequestFriendList(IntPtr manager) {
            this._friendListManager = manager;
            // NOTE: if this is being called, hook isn't null
            return this._friendListHook!.Original(manager);
        }

        private int OnFormatFriendList(long a1, long a2, long a3, int a4, IntPtr data, long a6) {
            // have to call this first to populate cross-world info
            // NOTE: if this is being called, hook isn't null
            var ret = this._formatHook!.Original(a1, a2, a3, a4, data, a6);

            if (!this.RequestingFriendList) {
                return ret;
            }

            var entry = Marshal.PtrToStructure<FriendListEntryRaw>(data);

            string? jobName = null;
            if (entry.job > 0) {
                jobName = this.Plugin.DataManager.GetExcelSheet<ClassJob>()!.GetRow(entry.job)?.Name?.ToString();
            }

            // FIXME: remove this try/catch when lumina fixes bug with .Value
            string? territoryName;
            try {
                territoryName = this.Plugin.DataManager.GetExcelSheet<TerritoryType>()!.GetRow(entry.territoryId)?.PlaceName?.Value?.Name?.ToString();
            } catch (NullReferenceException) {
                territoryName = null;
            }

            var player = new Player {
                Name = entry.Name(),
                FreeCompany = entry.FreeCompany(),
                Status = entry.flags,

                CurrentWorld = entry.currentWorldId,
                CurrentWorldName = this.Plugin.DataManager.GetExcelSheet<World>()!.GetRow(entry.currentWorldId)?.Name?.ToString(),
                HomeWorld = entry.homeWorldId,
                HomeWorldName = this.Plugin.DataManager.GetExcelSheet<World>()!.GetRow(entry.homeWorldId)?.Name?.ToString(),

                Territory = entry.territoryId,
                TerritoryName = territoryName,

                Job = entry.job,
                JobName = jobName,

                GrandCompany = entry.grandCompany,
                GrandCompanyName = this.Plugin.DataManager.GetExcelSheet<GrandCompany>()!.GetRow(entry.grandCompany)?.Name?.ToString(),

                Languages = entry.langsEnabled,
                MainLanguage = entry.mainLanguage,
            };
            this._friends.Add(player);

            return ret;
        }

        private IntPtr OnReceiveFriendList(IntPtr a1, IntPtr data) {
            // NOTE: if this is being called, hook isn't null
            var ret = this._receiveChunkHook!.Original(a1, data);

            // + 0xc
            // 1 = party
            // 2 = friends
            // 3 = linkshell
            // doesn't run (though same memory gets updated) for cwl or blacklist

            // + 0x8 is current number of results returned or 0 when end of list

            if (!this.RequestingFriendList) {
                goto Return;
            }

            if (*(byte*) (data + 0xc) != 2 || *(ushort*) (data + 0x8) != 0) {
                goto Return;
            }

            this.ReceiveFriendList?.Invoke(this._friends);
            this._friends.Clear();
            this.RequestingFriendList = false;

            Return:
            return ret;
        }

        public void Dispose() {
            this._friendListHook?.Dispose();
            this._formatHook?.Dispose();
            this._receiveChunkHook?.Dispose();
            this._chatChannelChangeHook?.Dispose();
            this._chatChannelChangeNameHook?.Dispose();
            this._isInputHook?.Dispose();
            this._isInputAfkHook?.Dispose();

            if (this._emptyXivString != IntPtr.Zero) {
                this._xivStringDtor?.Invoke(this._emptyXivString);
                Marshal.FreeHGlobal(this._emptyXivString);
            }
        }
    }

    [StructLayout(LayoutKind.Explicit)]
    [SuppressMessage("ReSharper", "PrivateFieldCanBeConvertedToLocalVariable")]
    internal readonly struct ChatPayload : IDisposable {
        [FieldOffset(0)]
        private readonly IntPtr textPtr;

        [FieldOffset(16)]
        private readonly ulong textLen;

        [FieldOffset(8)]
        private readonly ulong unk1;

        [FieldOffset(24)]
        private readonly ulong unk2;

        internal ChatPayload(string text) {
            var stringBytes = Encoding.UTF8.GetBytes(text);
            this.textPtr = Marshal.AllocHGlobal(stringBytes.Length + 30);
            Marshal.Copy(stringBytes, 0, this.textPtr, stringBytes.Length);
            Marshal.WriteByte(this.textPtr + stringBytes.Length, 0);

            this.textLen = (ulong) (stringBytes.Length + 1);

            this.unk1 = 64;
            this.unk2 = 0;
        }

        public void Dispose() {
            Marshal.FreeHGlobal(this.textPtr);
        }
    }

    [StructLayout(LayoutKind.Sequential)]
    internal struct FriendListEntryRaw {
        private readonly ulong unk1;
        internal readonly ulong flags;
        private readonly uint unk2;

        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 2)]
        private readonly byte[] unk3;

        internal readonly ushort currentWorldId;
        internal readonly ushort homeWorldId;
        internal readonly ushort territoryId;
        internal readonly byte grandCompany;
        internal readonly byte mainLanguage;
        internal readonly byte langsEnabled;

        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 2)]
        private readonly byte[] unk4;

        internal readonly byte job;

        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 32)]
        private readonly byte[] name;

        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 5)]
        private readonly byte[] fc;

        private static string? HandleString(IEnumerable<byte> bytes) {
            var nonNull = bytes.TakeWhile(b => b != 0).ToArray();
            return nonNull.Length == 0 ? null : Encoding.UTF8.GetString(nonNull);
        }

        internal string? Name() => HandleString(this.name);
        internal string? FreeCompany() => HandleString(this.fc);
    }
}
