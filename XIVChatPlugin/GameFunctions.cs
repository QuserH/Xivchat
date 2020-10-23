using Dalamud.Hooking;
using Lumina.Excel.GeneratedSheets;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.InteropServices;
using System.Text;
using XIVChatCommon;

namespace XIVChatPlugin {
    public class GameFunctions : IDisposable {
        private readonly Plugin plugin;

        private delegate IntPtr GetUIBaseDelegate();
        private delegate IntPtr GetUIModuleDelegate(IntPtr basePtr);
        private delegate void EasierProcessChatBoxDelegate(IntPtr uiModule, IntPtr message, IntPtr unused, byte a4);
        private delegate byte RequestFriendListDelegate(IntPtr manager);
        private delegate int FormatFriendListNameDelegate(long a1, long a2, long a3, int a4, IntPtr data, long a6);
        private delegate IntPtr OnReceiveFriendListChunkDelegate(IntPtr a1, IntPtr data);

        private readonly Hook<RequestFriendListDelegate> friendListHook;
        private readonly Hook<FormatFriendListNameDelegate> formatHook;
        private readonly Hook<OnReceiveFriendListChunkDelegate> receiveChunkHook;

        private readonly GetUIModuleDelegate GetUIModule;
        private readonly EasierProcessChatBoxDelegate _EasierProcessChatBox;

        private readonly IntPtr uiModulePtr;
        private IntPtr friendListManager = IntPtr.Zero;

        private bool requestingFriendList = false;
        public bool RequestingFriendList => this.requestingFriendList;
        private readonly List<Player> friends = new List<Player>();

        public delegate void ReceiveFriendListHandler(List<Player> friends);
        public event ReceiveFriendListHandler ReceiveFriendList;

        public GameFunctions(Plugin plugin) {
            this.plugin = plugin ?? throw new ArgumentNullException(nameof(plugin), "Plugin cannot be null");

            var getUIModulePtr = this.plugin.Interface.TargetModuleScanner.ScanText("E8 ?? ?? ?? ?? 48 83 7F ?? 00 48 8B F0");
            var easierProcessChatBoxPtr = this.plugin.Interface.TargetModuleScanner.ScanText("48 89 5C 24 ?? 57 48 83 EC 20 48 8B FA 48 8B D9 45 84 C9");
            var friendListPtr = this.plugin.Interface.TargetModuleScanner.ScanText("40 53 48 81 EC 80 0F 00 00 48 8B 05 ?? ?? ?? ?? 48 33 C4 48 89 84 24 ?? ?? ?? ?? 48 8B D9 48 8B 0D ?? ?? ?? ?? E8 ?? ?? ?? ?? 48 85 C0 0F 84 ?? ?? ?? ?? 44 0F B6 43 ?? 33 C9");
            var formatPtr = this.plugin.Interface.TargetModuleScanner.ScanText("48 89 5C 24 ?? 48 89 6C 24 ?? 48 89 74 24 ?? 41 56 48 83 EC 30 48 8B 6C 24 ??");
            var recvChunkPtr = this.plugin.Interface.TargetModuleScanner.ScanText("48 89 5C 24 ?? 56 48 83 EC 20 48 8B 0D ?? ?? ?? ?? 48 8B F2");
            this.uiModulePtr = this.plugin.Interface.TargetModuleScanner.GetStaticAddressFromSig("48 8B 0D ?? ?? ?? ?? 48 8D 54 24 ?? 48 83 C1 10 E8 ?? ?? ?? ??");

            this.GetUIModule = Marshal.GetDelegateForFunctionPointer<GetUIModuleDelegate>(getUIModulePtr);
            this._EasierProcessChatBox = Marshal.GetDelegateForFunctionPointer<EasierProcessChatBoxDelegate>(easierProcessChatBoxPtr);

            this.friendListHook = new Hook<RequestFriendListDelegate>(friendListPtr, new RequestFriendListDelegate(this.OnRequestFriendList));
            this.formatHook = new Hook<FormatFriendListNameDelegate>(formatPtr, new FormatFriendListNameDelegate(this.OnFormatFriendList));
            this.receiveChunkHook = new Hook<OnReceiveFriendListChunkDelegate>(recvChunkPtr, new OnReceiveFriendListChunkDelegate(this.OnReceiveFriendList));

            this.friendListHook.Enable();
            this.formatHook.Enable();
            this.receiveChunkHook.Enable();
        }

        public void ProcessChatBox(string message) {
            IntPtr uiModule = this.GetUIModule(Marshal.ReadIntPtr(this.uiModulePtr));

            if (uiModule == IntPtr.Zero) {
                throw new ApplicationException("uiModule was null");
            }

            using (var payload = new ChatPayload(message)) {
                IntPtr mem1 = Marshal.AllocHGlobal(400);
                Marshal.StructureToPtr(payload, mem1, false);

                this._EasierProcessChatBox(uiModule, mem1, IntPtr.Zero, 0);

                Marshal.FreeHGlobal(mem1);
            }
        }

        public bool RequestFriendList() {
            if (this.friendListManager == IntPtr.Zero || this.friendListHook == null) {
                return false;
            }

            this.requestingFriendList = true;
            this.friendListHook.Original(this.friendListManager);
            return true;
        }

        private byte OnRequestFriendList(IntPtr manager) {
            this.friendListManager = manager;
            return this.friendListHook.Original(manager);
        }

        private int OnFormatFriendList(long a1, long a2, long a3, int a4, IntPtr data, long a6) {
            // have to call this first to populate cross-world info
            var ret = this.formatHook.Original(a1, a2, a3, a4, data, a6);

            if (!this.RequestingFriendList) {
                return ret;
            }

            var entry = Marshal.PtrToStructure<FriendListEntryRaw>(data);

            string jobName = null;
            if (entry.job > 0) {
                jobName = this.plugin.Interface.Data.GetExcelSheet<ClassJob>().GetRow(entry.job)?.Name;
            }

            var player = new Player {
                Name = entry.Name(),
                FreeCompany = entry.FreeCompany(),
                Status = entry.flags,

                CurrentWorld = entry.currentWorldId,
                CurrentWorldName = this.plugin.Interface.Data.GetExcelSheet<World>().GetRow(entry.currentWorldId)?.Name,
                HomeWorld = entry.homeWorldId,
                HomeWorldName = this.plugin.Interface.Data.GetExcelSheet<World>().GetRow(entry.homeWorldId)?.Name,

                Territory = entry.territoryId,
                TerritoryName = this.plugin.Interface.Data.GetExcelSheet<TerritoryType>().GetRow(entry.territoryId)?.PlaceName?.Value?.Name,

                Job = entry.job,
                JobName = jobName,

                GrandCompany = entry.grandCompany,
                GrandCompanyName = this.plugin.Interface.Data.GetExcelSheet<GrandCompany>().GetRow(entry.grandCompany)?.Name,

                Languages = entry.langsEnabled,
                MainLanguage = entry.mainLanguage,
            };
            this.friends.Add(player);

            return ret;
        }

        private IntPtr OnReceiveFriendList(IntPtr a1, IntPtr data) {
            var ret = this.receiveChunkHook.Original(a1, data);

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

            this.ReceiveFriendList(this.friends);
            this.friends.Clear();
            this.requestingFriendList = false;

        Return:
            return ret;
        }

        public void Dispose() {
            this.friendListHook?.Dispose();
            this.formatHook?.Dispose();
            this.receiveChunkHook?.Dispose();
        }
    }

    [StructLayout(LayoutKind.Explicit)]
    struct ChatPayload : IDisposable {
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

            this.textLen = (ulong)(stringBytes.Length + 1);

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
        internal ulong flags;
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

        private static string HandleString(byte[] bytes) {
            byte[] nonNull = bytes.TakeWhile(b => b != 0).ToArray();
            if (nonNull.Length == 0) {
                return null;
            }

            return Encoding.UTF8.GetString(nonNull);
        }

        public string Name() => HandleString(this.name);
        public string FreeCompany() => HandleString(this.fc);
    }
}
