using Dalamud.Game.Command;
using Dalamud.Plugin;
using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Reflection;
using Dalamud.Data;
using Dalamud.Game;
using Dalamud.Game.ClientState;
using Dalamud.Game.ClientState.Objects;
using Dalamud.Game.Gui;
using Dalamud.IoC;
#if DEBUG
using System.IO;
#endif

namespace XIVChatPlugin {
    internal class Plugin : IDalamudPlugin {
        public string Name => "XIVChat";

        private bool _disposedValue;

        [PluginService]
        internal DalamudPluginInterface Interface { get; private init; } = null!;

        [PluginService]
        internal ChatGui ChatGui { get; private init; } = null!;

        [PluginService]
        internal ClientState ClientState { get; private init; } = null!;

        [PluginService]
        private CommandManager CommandManager { get; init; } = null!;

        [PluginService]
        internal DataManager DataManager { get; private init; } = null!;

        [PluginService]
        private Framework Framework { get; init; } = null!;

        [PluginService]
        internal ObjectTable ObjectTable { get; private init; } = null!;

        [PluginService]
        private SigScanner SigScanner { get; init; } = null!;

        internal Configuration Config { get; }
        private PluginUi Ui { get; }
        internal Server Server { get; private set; }
        internal Relay? Relay { get; private set; }
        internal GameFunctions Functions { get; }
        internal InternalEvents Events { get; }
        private List<IDisposable> Ipcs { get; } = new();

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

            this.Interface.UiBuilder.Draw += this.Ui.Draw;
            this.Interface.UiBuilder.OpenConfigUi += this.Ui.OpenSettings;
            this.Framework.Update += this.Server!.OnFrameworkUpdate;
            this.ChatGui.ChatMessage += this.Server.OnChat;
            this.ClientState.Login += this.Server.OnLogIn;
            this.ClientState.Logout += this.Server.OnLogOut;
            this.ClientState.TerritoryChanged += this.Server.OnTerritoryChange;
            this.CommandManager.AddHandler("/xivchat", new CommandInfo(this.OnCommand) {
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

            this.Interface.UiBuilder.Draw -= this.Ui.Draw;
            this.Interface.UiBuilder.OpenConfigUi -= this.Ui.OpenSettings;
            this.Framework.Update -= this.Server.OnFrameworkUpdate;
            this.ChatGui.ChatMessage -= this.Server.OnChat;
            this.ClientState.Login -= this.Server.OnLogIn;
            this.ClientState.Logout -= this.Server.OnLogOut;
            this.ClientState.TerritoryChanged -= this.Server.OnTerritoryChange;
            this.CommandManager.RemoveHandler("/xivchat");

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
                return this.SigScanner.ScanText(sig);
            } catch (KeyNotFoundException) {
                return IntPtr.Zero;
            }
        }

        internal IntPtr GetStaticAddressFromSig(string sig) {
            try {
                return this.SigScanner.GetStaticAddressFromSig(sig);
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
            this.Ui.OpenSettings();
        }
    }
}
