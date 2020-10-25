using Dalamud.Game.Command;
using Dalamud.Hooking;
using Dalamud.Plugin;
using System;
using System.Collections.Generic;
#if DEBUG
using System.IO;
#endif
using System.Reflection;

// TODO: hostable relay server (run one but have option to run your own)?

namespace XIVChatPlugin {
    public class Plugin : IDalamudPlugin {
        private bool disposedValue;

        public string Name => "XIVChat";

        internal string Location { get; private set; } = Assembly.GetExecutingAssembly().Location;

        [System.Diagnostics.CodeAnalysis.SuppressMessage("CodeQuality", "IDE0051:Remove unused private members", Justification = "LivePluginLoader")]
        private void SetLocation(string path) {
            this.Location = path;
        }

        public DalamudPluginInterface Interface { get; private set; }
        public Configuration Config { get; private set; }
        public PluginUI Ui { get; private set; }
        public Server Server { get; private set; }
        public GameFunctions Functions { get; private set; }

        private delegate byte ChatChannelChangeDelegate(IntPtr a1, uint channel);
        private Hook<ChatChannelChangeDelegate> chatChannelChangeHook;

        public void Initialize(DalamudPluginInterface pluginInterface) {
            this.Interface = pluginInterface ?? throw new ArgumentNullException(nameof(pluginInterface), "DalamudPluginInterface cannot be null");

            // load libsodium.so from debug location if in debug mode
#if DEBUG
            string path = Environment.GetEnvironmentVariable("PATH");
            string newPath = Path.GetDirectoryName(this.Location);
            Environment.SetEnvironmentVariable("PATH", $"{path};{newPath}");
#endif

            this.Config = (Configuration)this.Interface.GetPluginConfig() ?? new Configuration();
            this.Config.Initialise(this);

            this.Functions = new GameFunctions(this);
            try {
                var funcPtr = this.Interface.TargetModuleScanner.ScanText("40 55 48 8D 6C 24 ?? 48 81 EC A0 00 00 00 48 8B 05 ?? ?? ?? ?? 48 33 C4 48 89 45 ?? 48 8B 0D ?? ?? ?? ?? 33 C0 48 83 C1 10 89 45 ?? C7 45 ?? 01 00 00 00");
                this.chatChannelChangeHook = new Hook<ChatChannelChangeDelegate>(funcPtr, new ChatChannelChangeDelegate(this.ChangeChatChannelDetour));
                this.chatChannelChangeHook.Enable();
            } catch (KeyNotFoundException) {
                PluginLog.LogError("Could not sig chat channel change function");
            }

            this.Ui = new PluginUI(this);

            this.Server = new Server(this);
            this.Server.Spawn();

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

        private byte ChangeChatChannelDetour(IntPtr a1, uint channel) {
            // a1 + 0xfd0 is the chat channel byte (including for when clicking on shout)
            this.Server.OnChatChannelChange(channel);
            return this.chatChannelChangeHook.Original(a1, channel);
        }

        private void OnCommand(string command, string args) {
            this.Ui.OpenSettings(null, null);
        }

        protected virtual void Dispose(bool disposing) {
            if (!this.disposedValue) {
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

                    this.chatChannelChangeHook?.Dispose();
                }

                this.disposedValue = true;
            }
        }

        public void Dispose() {
            // Do not change this code. Put cleanup code in 'Dispose(bool disposing)' method
            this.Dispose(disposing: true);
            GC.SuppressFinalize(this);
        }
    }
}
