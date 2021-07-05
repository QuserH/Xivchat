using System.Diagnostics.CodeAnalysis;
using System.Reflection;
using Dalamud.Plugin;

namespace XIVChatPlugin {
    // ReSharper disable once ClassNeverInstantiated.Global
    public class DalamudPlugin : IDalamudPlugin {
        public string Name => "XIVChat";

        private Plugin? Plugin { get; set; }

        // ReSharper disable once UnusedMember.Global
        // ReSharper disable once AutoPropertyCanBeMadeGetOnly.Local
        internal string Location { get; private set; } = Assembly.GetExecutingAssembly().Location;

        public void Initialize(DalamudPluginInterface pluginInterface) {
            this.Plugin = new Plugin(this, pluginInterface);
        }

        public void Dispose() {
            this.Plugin?.Dispose();
        }

        [SuppressMessage("ReSharper", "UnusedMember.Local")]
        private void SetLocation(string path) {
            this.Location = path;
        }
    }
}
