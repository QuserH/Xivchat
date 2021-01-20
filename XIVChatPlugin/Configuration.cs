using Dalamud.Configuration;
using Sodium;
using System;
using System.Collections.Generic;

namespace XIVChatPlugin {
    public class Configuration : IPluginConfiguration {
        private Plugin? _plugin;

        public int Version { get; set; } = 1;
        public ushort Port { get; set; } = 14777;

        public bool BacklogEnabled { get; set; } = true;
        public ushort BacklogCount { get; set; } = 100;

        public bool SendBattle { get; set; } = true;

        public bool PairingMode { get; set; } = true;

        public bool AcceptNewClients { get; set; } = true;

        public bool AllowRelayConnections { get; set; }
        public string? RelayAuth { get; set; }

        public Dictionary<Guid, Tuple<string, byte[]>> TrustedKeys { get; set; } = new Dictionary<Guid, Tuple<string, byte[]>>();
        public KeyPair? KeyPair { get; set; }

        public void Initialise(Plugin plugin) {
            this._plugin = plugin ?? throw new ArgumentNullException(nameof(plugin), "Plugin cannot be null");
        }

        public void Save() {
            this._plugin?.Interface.SavePluginConfig(this);
        }
    }
}
