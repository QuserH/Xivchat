using Dalamud.Hooking;
using Lumina.Excel.GeneratedSheets;
using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using Dalamud.Plugin;
using XIVChatCommon.Message;

namespace XIVChatPlugin {
    public class GameFunctions : IDisposable {
        private Plugin Plugin { get; }

        private delegate IntPtr GetUiModuleDelegate(IntPtr basePtr);

        private delegate void EasierProcessChatBoxDelegate(IntPtr uiModule, IntPtr message, IntPtr unused, byte a4);

        private delegate byte RequestFriendListDelegate(IntPtr manager);

        private delegate int FormatFriendListNameDelegate(long a1, long a2, long a3, int a4, IntPtr data, long a6);

        private delegate IntPtr OnReceiveFriendListChunkDelegate(IntPtr a1, IntPtr data);

        private delegate IntPtr GetColourInfoDelegate(IntPtr handler, uint lookupResult);

        private delegate byte ChatChannelChangeDelegate(IntPtr a1, uint channel);

        private delegate IntPtr ChannelChangeCommandDelegate(IntPtr a1, int inputChannel, uint linkshellIdx, IntPtr tellTarget, char canChangeChannel);

        private delegate IntPtr XivStringCtorDelegate(IntPtr memory);

        private delegate IntPtr XivStringDtorDelegate(IntPtr memory);

        private readonly Hook<RequestFriendListDelegate>? _friendListHook;
        private readonly Hook<FormatFriendListNameDelegate>? _formatHook;
        private readonly Hook<OnReceiveFriendListChunkDelegate>? _receiveChunkHook;
        private readonly Hook<ChatChannelChangeDelegate>? _chatChannelChangeHook;

        private readonly GetUiModuleDelegate? _getUiModule;
        private readonly EasierProcessChatBoxDelegate? _easierProcessChatBox;
        private readonly GetColourInfoDelegate? _getColourInfo;
        private readonly ChannelChangeCommandDelegate? _channelChangeCommand;
        private readonly XivStringCtorDelegate? _xivStringCtor;
        private readonly XivStringDtorDelegate? _xivStringDtor;

        private IntPtr UiModulePtr { get; }
        private IntPtr ColourHandler { get; }
        private IntPtr ColourLookup { get; }
        private IntPtr _friendListManager = IntPtr.Zero;
        private IntPtr _chatManager = IntPtr.Zero;
        private IntPtr _emptyXivString = IntPtr.Zero;

        public bool RequestingFriendList { get; private set; }

        private readonly List<Player> _friends = new List<Player>();

        public delegate void ReceiveFriendListHandler(List<Player> friends);

        public event ReceiveFriendListHandler? ReceiveFriendList;

        public GameFunctions(Plugin plugin) {
            this.Plugin = plugin ?? throw new ArgumentNullException(nameof(plugin), "Plugin cannot be null");

            var getUiModulePtr = this.Plugin.ScanText("E8 ?? ?? ?? ?? 48 83 7F ?? 00 48 8B F0");
            var easierProcessChatBoxPtr = this.Plugin.ScanText("48 89 5C 24 ?? 57 48 83 EC 20 48 8B FA 48 8B D9 45 84 C9");
            var friendListPtr = this.Plugin.ScanText("40 53 48 81 EC 80 0F 00 00 48 8B 05 ?? ?? ?? ?? 48 33 C4 48 89 84 24 ?? ?? ?? ?? 48 8B D9 48 8B 0D ?? ?? ?? ?? E8 ?? ?? ?? ?? 48 85 C0 0F 84 ?? ?? ?? ?? 44 0F B6 43 ?? 33 C9");
            var formatPtr = this.Plugin.ScanText("48 89 5C 24 ?? 48 89 6C 24 ?? 48 89 74 24 ?? 41 56 48 83 EC 30 48 8B 6C 24 ??");
            var recvChunkPtr = this.Plugin.ScanText("48 89 5C 24 ?? 56 48 83 EC 20 48 8B 0D ?? ?? ?? ?? 48 8B F2");
            var getColourPtr = this.Plugin.ScanText("48 89 5C 24 ?? 48 89 6C 24 ?? 48 89 74 24 ?? 57 48 83 EC 20 8B F2 48 8D B9 ?? ?? ?? ??");
            var channelPtr = this.Plugin.ScanText("40 55 48 8D 6C 24 ?? 48 81 EC A0 00 00 00 48 8B 05 ?? ?? ?? ?? 48 33 C4 48 89 45 ?? 48 8B 0D ?? ?? ?? ?? 33 C0 48 83 C1 10 89 45 ?? C7 45 ?? 01 00 00 00");
            var channelCommandPtr = this.Plugin.ScanText("E8 ?? ?? ?? ?? 0F B7 44 37 ??");
            var xivStringCtorPtr = this.Plugin.ScanText("E8 ?? ?? ?? ?? 44 2B F7 ");
            var xivStringDtorPtr = this.Plugin.ScanText("");

            this.UiModulePtr = this.Plugin.GetStaticAddressFromSig("48 8B 0D ?? ?? ?? ?? 48 8D 54 24 ?? 48 83 C1 10 E8 ?? ?? ?? ??");
            if (this.UiModulePtr == IntPtr.Zero) {
                PluginLog.Warning("Static pointer was null: {}", nameof(this.UiModulePtr));
            }

            this.ColourHandler = this.Plugin.GetStaticAddressFromSig("48 8B 05 ?? ?? ?? ?? 48 8B A8 ?? ?? ?? ?? 48 85 ED 0F 84 ?? ?? ?? ??");
            if (this.ColourHandler == IntPtr.Zero) {
                PluginLog.Warning("Static pointer was null: {}", nameof(this.ColourHandler));
            }

            this.ColourLookup = this.Plugin.GetStaticAddressFromSig("48 8D 0D ?? ?? ?? ?? 8B 14 ?? 85 D2 7E ?? 48 8B 0D ?? ?? ?? ?? 48 83 C1 10 E8 ?? ?? ?? ?? 8B 70 ?? 41 8D 4D ??");
            if (this.ColourLookup == IntPtr.Zero) {
                PluginLog.Warning("Static pointer was null: {}", nameof(this.ColourLookup));
            }

            if (getUiModulePtr != IntPtr.Zero) {
                this._getUiModule = Marshal.GetDelegateForFunctionPointer<GetUiModuleDelegate>(getUiModulePtr);
            } else {
                PluginLog.Warning("Pointer was null, disabling function: {}", nameof(getUiModulePtr));
            }

            if (easierProcessChatBoxPtr != IntPtr.Zero) {
                this._easierProcessChatBox = Marshal.GetDelegateForFunctionPointer<EasierProcessChatBoxDelegate>(easierProcessChatBoxPtr);
            } else {
                PluginLog.Warning("Pointer was null, disabling function: {}", nameof(easierProcessChatBoxPtr));
            }

            if (getColourPtr != IntPtr.Zero) {
                this._getColourInfo = Marshal.GetDelegateForFunctionPointer<GetColourInfoDelegate>(getColourPtr);
            } else {
                PluginLog.Warning("Pointer was null, disabling function: {}", nameof(getColourPtr));
            }

            if (channelCommandPtr != IntPtr.Zero) {
                this._channelChangeCommand = Marshal.GetDelegateForFunctionPointer<ChannelChangeCommandDelegate>(channelCommandPtr);
            } else {
                PluginLog.Warning("Pointer was null, disabling function: {}", nameof(channelCommandPtr));
            }

            if (xivStringCtorPtr != IntPtr.Zero) {
                this._xivStringCtor = Marshal.GetDelegateForFunctionPointer<XivStringCtorDelegate>(xivStringCtorPtr);
            } else {
                PluginLog.Warning("Pointer was null, disabling function: {}", nameof(xivStringCtorPtr));
            }

            if (xivStringDtorPtr != IntPtr.Zero) {
                this._xivStringDtor = Marshal.GetDelegateForFunctionPointer<XivStringDtorDelegate>(xivStringDtorPtr);
            } else {
                PluginLog.Warning("Pointer was null, disabling function: {}", nameof(xivStringDtorPtr));
            }

            if (friendListPtr != IntPtr.Zero) {
                this._friendListHook = new Hook<RequestFriendListDelegate>(friendListPtr, new RequestFriendListDelegate(this.OnRequestFriendList));
            } else {
                PluginLog.Warning("Pointer was null, disabling hook: {}", nameof(friendListPtr));
            }

            if (formatPtr != IntPtr.Zero) {
                this._formatHook = new Hook<FormatFriendListNameDelegate>(formatPtr, new FormatFriendListNameDelegate(this.OnFormatFriendList));
            } else {
                PluginLog.Warning("Pointer was null, disabling hook: {}", nameof(formatPtr));
            }

            if (recvChunkPtr != IntPtr.Zero) {
                this._receiveChunkHook = new Hook<OnReceiveFriendListChunkDelegate>(recvChunkPtr, new OnReceiveFriendListChunkDelegate(this.OnReceiveFriendList));
            } else {
                PluginLog.Warning("Pointer was null, disabling hook: {}", nameof(recvChunkPtr));
            }

            if (channelPtr != IntPtr.Zero) {
                this._chatChannelChangeHook = new Hook<ChatChannelChangeDelegate>(channelPtr, new ChatChannelChangeDelegate(this.ChangeChatChannelDetour));
            } else {
                PluginLog.Warning("Pointer was null, disabling hook: {}", nameof(channelPtr));
            }

            this._friendListHook?.Enable();
            this._formatHook?.Enable();
            this._receiveChunkHook?.Enable();
            this._chatChannelChangeHook?.Enable();

            if (this._xivStringCtor != null && this._xivStringDtor != null) {
                this._emptyXivString = Marshal.AllocHGlobal(0x68);
                this._xivStringCtor(this._emptyXivString);
            }
        }

        public void ChangeChatChannel(InputChannel channel) {
            if (this._chatManager == IntPtr.Zero || this._channelChangeCommand == null || this._emptyXivString == IntPtr.Zero) {
                return;
            }

            this._channelChangeCommand(this._chatManager, (int) channel, channel.LinkshellIndex(), this._emptyXivString, '\x01');
        }

        // This function looks up a channel's user-defined colour.
        //
        // If this function would ever return 0, it returns null instead.
        public uint? GetChannelColour(ChatCode channel) {
            if (this.ColourLookup == IntPtr.Zero || this.ColourHandler == IntPtr.Zero) {
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

            var lookupResult = (uint) Marshal.ReadInt32(this.ColourLookup, (int) parent * 4);
            var info = this._getColourInfo(Marshal.ReadIntPtr(this.ColourHandler) + 16, lookupResult);
            var rgb = (uint) Marshal.ReadInt32(info, 32) & 0xFFFFFF;

            if (rgb == 0) {
                return null;
            }

            return 0xFF | (rgb << 8);
        }

        public void ProcessChatBox(string message) {
            if (this._easierProcessChatBox == null || this.UiModulePtr == IntPtr.Zero) {
                return;
            }

            var uiModule = this._getUiModule(Marshal.ReadIntPtr(this.UiModulePtr));

            if (uiModule == IntPtr.Zero) {
                throw new ArgumentException("pointer was null", nameof(uiModule));
            }

            using var payload = new ChatPayload(message);
            var mem1 = Marshal.AllocHGlobal(400);
            Marshal.StructureToPtr(payload, mem1, false);

            this._easierProcessChatBox(uiModule, mem1, IntPtr.Zero, 0);

            Marshal.FreeHGlobal(mem1);
        }

        public bool RequestFriendList() {
            if (this._friendListManager == IntPtr.Zero || this._friendListHook == null) {
                return false;
            }

            this.RequestingFriendList = true;
            this._friendListHook.Original(this._friendListManager);
            return true;
        }

        private byte ChangeChatChannelDetour(IntPtr a1, uint channel) {
            this._chatManager = a1;
            // a1 + 0xfd0 is the chat channel byte (including for when clicking on shout)
            this.Plugin.Server.OnChatChannelChange(channel);
            return this._chatChannelChangeHook!.Original(a1, channel);
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
                jobName = this.Plugin.Interface.Data.GetExcelSheet<ClassJob>().GetRow(entry.job)?.Name;
            }

            // FIXME: remove this try/catch when lumina fixes bug with .Value
            string? territoryName;
            try {
                territoryName = this.Plugin.Interface.Data.GetExcelSheet<TerritoryType>().GetRow(entry.territoryId)?.PlaceName?.Value?.Name;
            } catch (NullReferenceException) {
                territoryName = null;
            }

            var player = new Player {
                Name = entry.Name(),
                FreeCompany = entry.FreeCompany(),
                Status = entry.flags,

                CurrentWorld = entry.currentWorldId,
                CurrentWorldName = this.Plugin.Interface.Data.GetExcelSheet<World>().GetRow(entry.currentWorldId)?.Name,
                HomeWorld = entry.homeWorldId,
                HomeWorldName = this.Plugin.Interface.Data.GetExcelSheet<World>().GetRow(entry.homeWorldId)?.Name,

                Territory = entry.territoryId,
                TerritoryName = territoryName,

                Job = entry.job,
                JobName = jobName,

                GrandCompany = entry.grandCompany,
                GrandCompanyName = this.Plugin.Interface.Data.GetExcelSheet<GrandCompany>().GetRow(entry.grandCompany)?.Name,

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

            if (Marshal.ReadByte(data + 0xc) != 2 || Marshal.ReadInt16(data + 0x8) != 0) {
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

            if (this._emptyXivString != IntPtr.Zero) {
                this._xivStringDtor?.Invoke(this._emptyXivString);
                Marshal.FreeHGlobal(this._emptyXivString);
            }
        }
    }

    [StructLayout(LayoutKind.Explicit)]
    [SuppressMessage("ReSharper", "PrivateFieldCanBeConvertedToLocalVariable")]
    readonly struct ChatPayload : IDisposable {
        [FieldOffset(0)]
        readonly IntPtr textPtr;

        [FieldOffset(16)]
        readonly ulong textLen;

        [FieldOffset(8)]
        readonly ulong unk1;

        [FieldOffset(24)]
        readonly ulong unk2;

        internal ChatPayload(string text) {
            byte[] stringBytes = Encoding.UTF8.GetBytes(text);
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
    struct FriendListEntryRaw {
        readonly ulong unk1;
        internal readonly ulong flags;
        readonly uint unk2;

        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 2)]
        readonly byte[] unk3;

        internal readonly ushort currentWorldId;
        internal readonly ushort homeWorldId;
        internal readonly ushort territoryId;
        internal readonly byte grandCompany;
        internal readonly byte mainLanguage;
        internal readonly byte langsEnabled;

        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 2)]
        readonly byte[] unk4;

        internal readonly byte job;

        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 32)]
        readonly byte[] name;

        [MarshalAs(UnmanagedType.ByValArray, SizeConst = 5)]
        readonly byte[] fc;

        private static string? HandleString(IEnumerable<byte> bytes) {
            byte[] nonNull = bytes.TakeWhile(b => b != 0).ToArray();
            return nonNull.Length == 0 ? null : Encoding.UTF8.GetString(nonNull);
        }

        public string? Name() => HandleString(this.name);
        public string? FreeCompany() => HandleString(this.fc);
    }
}
