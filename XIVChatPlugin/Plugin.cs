using Dalamud.Game.Command;
using Dalamud.Plugin;
using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Reflection;
using Dalamud.IoC;
using Dalamud.Plugin.Services;
#if DEBUG
using System.IO;
#endif

namespace XIVChatPlugin {
    internal class Plugin : IDalamudPlugin {
        internal static string Name => "艾欧泽亚终端";

        private bool _disposedValue;

        [PluginService]
        internal static IPluginLog Log { get; private set; } = null!;

        [PluginService]
        internal static IDalamudPluginInterface Interface { get; private set; } = null!;

        [PluginService]
        internal static IChatGui ChatGui { get; private set; } = null!;

        [PluginService]
        internal static IClientState ClientState { get; private set; } = null!;

        [PluginService]
        private static ICommandManager CommandManager { get; set; } = null!;

        [PluginService]
        internal static IDataManager DataManager { get; private set; } = null!;

        [PluginService]
        private static IFramework Framework { get; set; } = null!;

        [PluginService]
        internal static IObjectTable ObjectTable { get; private set; } = null!;
        [PluginService]
        internal static IPartyList PartyList { get; private set; } = null!;

        [PluginService]
        internal static IUnlockState UnlockState { get; private set; } = null!;

        [PluginService]
        internal static IGameInventory GameInventory { get; private set; } = null!;

        [PluginService]
        internal static IDutyState DutyState { get; private set; } = null!;

        [PluginService]
        internal static IGameInteropProvider GameInteropProvider { get; private set; } = null!;

        [PluginService]
        private static ISigScanner SigScanner { get; set; } = null!;

        internal Configuration Config { get; }
        private PluginUi Ui { get; }
        internal Server Server { get; private set; }
        internal Relay? Relay { get; private set; }
        internal GameFunctions Functions { get; }
        internal InternalEvents Events { get; }
        private List<IDisposable> Ipcs { get; } = [];

        // ReSharper disable once UnusedMember.Global
        // ReSharper disable once AutoPropertyCanBeMadeGetOnly.Local
        // ReSharper disable once UnusedAutoPropertyAccessor.Global
        // ReSharper disable once MemberCanBePrivate.Global
        internal string Location { get; private set; } = Assembly.GetExecutingAssembly().Location;

        [SuppressMessage("ReSharper", "UnusedMember.Local")]
        private void SetLocation(string path) {
            this.Location = path;
        }

        public Plugin() {
            this.Events = new InternalEvents();

            // load libsodium.so from debug location if in debug mode
            #if DEBUG
            string path = Environment.GetEnvironmentVariable("PATH")!;
            string newPath = Path.GetDirectoryName(this.Location)!;
            Environment.SetEnvironmentVariable("PATH", $"{path};{newPath}");
            #endif

            this.Config = Interface.GetPluginConfig() as Configuration ?? new Configuration();
            this.Config.Initialise(this);

            this.Functions = new GameFunctions(this);

            this.Ui = new PluginUi(this);

            this.LaunchServer();

            if (this.Config.AllowRelayConnections) {
                this.StartRelay();
            }

            Interface.UiBuilder.Draw += this.Ui.Draw;
            Interface.UiBuilder.OpenConfigUi += this.Ui.OpenSettings;
            Framework.Update += this.Server!.OnFrameworkUpdate;
            ChatGui.ChatMessage += this.Server.OnChat;
            ClientState.Login += this.Server.OnLogIn;
            ClientState.Logout += this.Server.OnLogOut;
            ClientState.TerritoryChanged += this.Server.OnTerritoryChange;
            CommandManager.AddHandler("/eorzeaphone", new CommandInfo(this.OnCommand) {
                HelpMessage = "打开艾欧泽亚终端插件设置",
            });

            this.Ipcs.Add(new Ipc.PeepingTom(this));
        }

        public void Dispose() {
            if (this._disposedValue) {
                return;
            }

            this._disposedValue = true;

            this.Relay?.Dispose();
            this.Server.Dispose();

            Interface.UiBuilder.Draw -= this.Ui.Draw;
            Interface.UiBuilder.OpenConfigUi -= this.Ui.OpenSettings;
            Framework.Update -= this.Server.OnFrameworkUpdate;
            ChatGui.ChatMessage -= this.Server.OnChat;
            ClientState.Login -= this.Server.OnLogIn;
            ClientState.Logout -= this.Server.OnLogOut;
            ClientState.TerritoryChanged -= this.Server.OnTerritoryChange;
            CommandManager.RemoveHandler("/eorzeaphone");
            this.Functions.Dispose();

            foreach (var ipc in this.Ipcs) {
                ipc.Dispose();
            }
        }

        internal void StartRelay() {
            if (this.Relay != null) {
                return;
            }

            this.Relay = new Relay(this);
            this.Relay.Start();
        }

        internal void StopRelay() {
            if (this.Relay == null) {
                return;
            }

            this.Relay.Dispose();
            this.Relay = null;
        }

        internal nint ScanText(string sig) {
            try {
                return SigScanner.ScanText(sig);
            } catch (KeyNotFoundException) {
                return nint.Zero;
            }
        }

        internal nint GetStaticAddressFromSig(string sig) {
            try {
                return SigScanner.GetStaticAddressFromSig(sig);
            } catch (KeyNotFoundException) {
                return nint.Zero;
            }
        }

        private void LaunchServer() {
            this.Server = new Server(this);
            this.Server.Spawn();
        }

        internal void RelaunchServer() {
            this.Server.Dispose();
            this.LaunchServer();
        }

        private void OnCommand(string command, string args) {
            this.Ui.OpenSettings();
        }
    }
}
