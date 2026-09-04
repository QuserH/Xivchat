using Dalamud.Configuration;
using Sodium;
using System;
using System.Collections.Generic;

namespace XIVChatPlugin {
    [Serializable]
    internal class Configuration : IPluginConfiguration {
        private Plugin? _plugin;

        public int Version { get; set; } = 1;
        public ushort Port { get; set; } = 14777;

        public bool BacklogEnabled { get; set; } = true;
        public ushort BacklogCount { get; set; } = 500;
        public string? BacklogPath { get; set; }

        public bool SendBattle { get; set; } = true;

        public bool MessagesCountAsInput { get; set; } = true;

        public bool PairingMode { get; set; } = true;

        public bool AcceptNewClients { get; set; } = true;

        /// <summary>
        /// Whether paired phones may buy market board listings. Unlike every other
        /// remote action this one spends gil irreversibly, so it gets its own switch
        /// that does not also turn off chat. Off by default: spending real gil is
        /// something the player opts into, never something a pairing enables for them.
        /// </summary>
        public bool AllowMarketPurchase { get; set; }

        /// <summary>
        /// Whether the price monitor may buy listings on its own when one drops
        /// below a rule's threshold. The phone pushes the rules; this switch is the
        /// master arm for anything automatic. On by default because turning auto-buy
        /// on for a rule is already an explicit act in the app.
        /// </summary>
        public bool AllowMonitorAutoBuy { get; set; } = true;

        /// <summary>
        /// Price-monitor rules pushed by the phone. Persisted here (not in a separate
        /// file) so monitoring survives a restart exactly like every other setting.
        /// </summary>
        public List<MarketMonitorConfig> MarketMonitors { get; set; } = new();

        public bool AllowRelayConnections { get; set; }
        public string? RelayAuth { get; set; }

        public Dictionary<Guid, Tuple<string, byte[]>> TrustedKeys { get; set; } = new();
        public KeyPair? KeyPair { get; set; }

        internal void Initialise(Plugin plugin) {
            this._plugin = plugin;
        }

        internal void Save() {
            XIVChatPlugin.Plugin.Interface.SavePluginConfig(this);
        }
    }

    /// <summary>
    /// One persisted price-monitor rule. Mirrors
    /// <c>XIVChatCommon.Message.Client.ClientMarketMonitorEntry</c> on the wire plus
    /// the bookkeeping the plugin keeps between sessions.
    /// </summary>
    [Serializable]
    internal class MarketMonitorConfig {
        public uint ItemId { get; set; }

        /// <summary>Trigger (and auto-buy) when a listing's unit price is at or below this.</summary>
        public uint PriceThreshold { get; set; }

        public bool HqOnly { get; set; }
        public bool AutoBuy { get; set; }

        /// <summary>Total quantity this rule may ever auto-buy; 0 means unlimited.</summary>
        public uint BuyCap { get; set; }

        /// <summary>Quantity already auto-bought against <see cref="BuyCap"/>.</summary>
        public uint BoughtQty { get; set; }

        /// <summary>Last time a notification/auto-buy fired for this rule, and at what
        /// price. A standing cheap listing must not re-notify every poll.</summary>
        public long LastFireMs { get; set; }
        public uint LastFirePrice { get; set; }

        public MarketMonitorConfig Clone() => (MarketMonitorConfig) this.MemberwiseClone();
    }
}
