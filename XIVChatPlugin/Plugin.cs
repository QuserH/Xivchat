using Dalamud.Game.Command;
using Dalamud.Hooking;
using Dalamud.Plugin;
using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
#if DEBUG
using System.IO;
#endif
using System.Reflection;

// TODO: hostable relay server (run one but have option to run your own)?

namespace XIVChatPlugin {
    public class Plugin : IDalamudPlugin {
        private bool _disposedValue;

        public string Name => "XIVChat";

        // ReSharper disable once MemberCanBePrivate.Global
        internal string Location { get; private set; } = Assembly.GetExecutingAssembly().Location;

        [SuppressMessage("ReSharper", "UnusedMember.Local")]
        private void SetLocation(string path) {
            this.Location = path;
        }

        #pragma warning disable 8618
        public DalamudPluginInterface Interface { get; private set; }
        public Configuration Config { get; private set; }
        private PluginUi Ui { get; set; }
        public Server Server { get; private set; }
        public GameFunctions Functions { get; private set; }
        #pragma warning restore 8618

        public void Initialize(DalamudPluginInterface pluginInterface) {
            this.Interface = pluginInterface ?? throw new ArgumentNullException(nameof(pluginInterface), "DalamudPluginInterface cannot be null");

            // load libsodium.so from debug location if in debug mode
            #if DEBUG
            string path = Environment.GetEnvironmentVariable("PATH")!;
            string newPath = Path.GetDirectoryName(this.Location)!;
            Environment.SetEnvironmentVariable("PATH", $"{path};{newPath}");
            #endif

            this.Config = (Configuration?)this.Interface.GetPluginConfig() ?? new Configuration();
            this.Config.Initialise(this);

            this.Functions = new GameFunctions(this);

            this.Ui = new PluginUi(this);

            this.LaunchServer();

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

        public void RelaunchServer() {
            this.Server.Dispose();
            this.LaunchServer();
        }

        private void OnCommand(string command, string args) {
            this.Ui.OpenSettings(null, null);
        }

        [SuppressMessage("ReSharper", "DelegateSubtraction")]
        protected virtual void Dispose(bool disposing) {
            if (this._disposedValue) {
                return;
            }

            if (disposing) {
                this.Server.Dispose();

                this.Interface.UiBuilder.OnBuildUi -= this.Ui.Draw;
                this.Interface.UiBuilder.OnOpenConfigUi -= this.Ui.OpenSettings;
                this.Interface.Framework.OnUpdateEvent -= this.Server.OnFrameworkUpdate;
                this.Interface.Framework.Gui.Chat.OnChatMessage -= this.Server.OnChat;
                this.Interface.ClientState.OnLogin -= this.Server.OnLogIn;
                this.Interface.ClientState.OnLogout -= this.Server.OnLogOut;
                this.Interface.ClientState.TerritoryChanged -= this.Server.OnTerritoryChange;
                this.Interface.CommandManager.RemoveHandler("/xivchat");
            }

            this._disposedValue = true;
        }

        public void Dispose() {
            // Do not change this code. Put cleanup code in 'Dispose(bool disposing)' method
            this.Dispose(true);
            GC.SuppressFinalize(this);
        }
    }
}
