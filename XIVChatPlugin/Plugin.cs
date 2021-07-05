using Dalamud.Game.Command;
using Dalamud.Plugin;
using System;
using System.Collections.Generic;
#if DEBUG
using System.IO;
#endif

namespace XIVChatPlugin {
    internal class Plugin : IDisposable {
        private bool _disposedValue;

        internal DalamudPluginInterface Interface { get; }
        internal DalamudPlugin DalamudPlugin { get; }
        internal Configuration Config { get; }
        private PluginUi Ui { get; }
        internal Server Server { get; private set; } = null!;
        internal Relay? Relay { get; private set; }
        internal GameFunctions Functions { get; }
        private List<IDisposable> Ipcs { get; } = new();

        internal Plugin(DalamudPlugin dalamudPlugin, DalamudPluginInterface pluginInterface) {
            this.Interface = pluginInterface;
            this.DalamudPlugin = dalamudPlugin;

            // load libsodium.so from debug location if in debug mode
            #if DEBUG
            string path = Environment.GetEnvironmentVariable("PATH")!;
            string newPath = Path.GetDirectoryName(this.DalamudPlugin.Location)!;
            Environment.SetEnvironmentVariable("PATH", $"{path};{newPath}");
            #endif

            this.Config = this.Interface.GetPluginConfig() as Configuration ?? new Configuration();
            this.Config.Initialise(this);

            this.Functions = new GameFunctions(this);

            this.Ui = new PluginUi(this);

            this.LaunchServer();

            if (this.Config.AllowRelayConnections) {
                this.StartRelay();
            }

            this.Interface.UiBuilder.OnBuildUi += this.Ui.Draw;
            this.Interface.UiBuilder.OnOpenConfigUi += this.Ui.OpenSettings;
            this.Interface.Framework.OnUpdateEvent += this.Server.OnFrameworkUpdate;
            this.Interface.Framework.Gui.Chat.OnChatMessage += this.Server.OnChat;
            this.Interface.ClientState.OnLogin += this.Server.OnLogIn;
            this.Interface.ClientState.OnLogout += this.Server.OnLogOut;
            this.Interface.ClientState.TerritoryChanged += this.Server.OnTerritoryChange;
            this.Interface.CommandManager.AddHandler("/xivchat", new CommandInfo(this.OnCommand) {
                HelpMessage = "Opens the config for the XIVChat plugin",
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

            this.Interface.UiBuilder.OnBuildUi -= this.Ui.Draw;
            this.Interface.UiBuilder.OnOpenConfigUi -= this.Ui.OpenSettings;
            this.Interface.Framework.OnUpdateEvent -= this.Server.OnFrameworkUpdate;
            this.Interface.Framework.Gui.Chat.OnChatMessage -= this.Server.OnChat;
            this.Interface.ClientState.OnLogin -= this.Server.OnLogIn;
            this.Interface.ClientState.OnLogout -= this.Server.OnLogOut;
            this.Interface.ClientState.TerritoryChanged -= this.Server.OnTerritoryChange;
            this.Interface.CommandManager.RemoveHandler("/xivchat");

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

        internal IntPtr ScanText(string sig) {
            try {
                return this.Interface.TargetModuleScanner.ScanText(sig);
            } catch (KeyNotFoundException) {
                return IntPtr.Zero;
            }
        }

        internal IntPtr GetStaticAddressFromSig(string sig) {
            try {
                return this.Interface.TargetModuleScanner.GetStaticAddressFromSig(sig);
            } catch (KeyNotFoundException) {
                return IntPtr.Zero;
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
            this.Ui.OpenSettings(null, null);
        }
    }
}
