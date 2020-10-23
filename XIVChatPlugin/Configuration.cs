using Dalamud.Configuration;
using Sodium;
using System;
using System.Collections.Generic;

namespace XIVChatPlugin {
    public class Configuration : IPluginConfiguration {
        private Plugin plugin;

        public int Version { get; set; } = 1;
        public ushort Port { get; set; } = 14777;

        public bool BacklogEnabled { get; set; } = true;
        public ushort BacklogCount { get; set; } = 100;

        public bool SendBattle { get; set; } = true;

        public Dictionary<Guid, Tuple<string, byte[]>> TrustedKeys { get; set; } = new Dictionary<Guid, Tuple<string, byte[]>>();
        public KeyPair KeyPair { get; set; } = null;

        public void Initialise(Plugin plugin) {
            this.plugin = plugin ?? throw new ArgumentNullException(nameof(plugin), "Plugin cannot be null");
        }

        public void Save() {
            this.plugin.Interface.SavePluginConfig(this);
        }
    }
}
