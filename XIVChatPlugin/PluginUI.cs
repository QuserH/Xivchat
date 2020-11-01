using ImGuiNET;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Numerics;
using System.Threading.Channels;

namespace XIVChatPlugin {
    public class PluginUI {
        private readonly Plugin plugin;

        private bool showSettings;
        private bool ShowSettings { get => this.showSettings; set => this.showSettings = value; }

        private readonly Dictionary<Guid, Tuple<Client, Channel<bool>>> pending = new Dictionary<Guid, Tuple<Client, Channel<bool>>>();
        private readonly Dictionary<Guid, string> pendingNames = new Dictionary<Guid, string>(0);

        public PluginUI(Plugin plugin) {
            this.plugin = plugin ?? throw new ArgumentNullException(nameof(plugin), "Plugin cannot be null");
        }

        private static class Colours {
            public static readonly Vector4 Primary = new Vector4(2 / 255f, 204 / 255f, 238 / 255f, 1.0f);
            public static readonly Vector4 PrimaryDark = new Vector4(2 / 255f, 180 / 255f, 211 / 255f, 1.0f);
            public static readonly Vector4 Background = new Vector4(46 / 255f, 46 / 255f, 46 / 255f, 1.0f);
            public static readonly Vector4 Text = new Vector4(190 / 255f, 190 / 255f, 190 / 255f, 1.0f);
            public static readonly Vector4 Button = new Vector4(90 / 255f, 89 / 255f, 90 / 255f, 1.0f);
            public static readonly Vector4 ButtonActive = new Vector4(123 / 255f, 122 / 255f, 124 / 255f, 1.0f);
            public static readonly Vector4 ButtonHovered = new Vector4(108 / 255f, 107 / 255f, 109 / 255f, 1.0f);

            public static readonly Vector4 White = new Vector4(1f, 1f, 1f, 1f);
        }

        public void Draw() {
            ImGui.PushStyleColor(ImGuiCol.TitleBg, Colours.PrimaryDark);
            ImGui.PushStyleColor(ImGuiCol.TitleBgActive, Colours.Primary);
            ImGui.PushStyleColor(ImGuiCol.TitleBgCollapsed, Colours.PrimaryDark);
            ImGui.PushStyleColor(ImGuiCol.WindowBg, Colours.Background);
            ImGui.PushStyleColor(ImGuiCol.Text, Colours.Text);
            ImGui.PushStyleColor(ImGuiCol.Button, Colours.Button);
            ImGui.PushStyleColor(ImGuiCol.ButtonActive, Colours.ButtonActive);
            ImGui.PushStyleColor(ImGuiCol.ButtonHovered, Colours.ButtonHovered);

            this.DrawInner();

            ImGui.PopStyleColor(8);
        }

        private static V WithWhiteText<V>(Func<V> func) {
            ImGui.PushStyleColor(ImGuiCol.Text, Colours.White);
            var ret = func();
            ImGui.PopStyleColor();
            return ret;
        }

        private static void WithWhiteText(Action func) {
            ImGui.PushStyleColor(ImGuiCol.Text, Colours.White);
            func();
            ImGui.PopStyleColor();
        }

        private static bool Begin(string name, ImGuiWindowFlags flags) {
            return WithWhiteText(() => ImGui.Begin(name, flags));
        }

        private static bool Begin(string name, ref bool showSettings, ImGuiWindowFlags flags) {
            ImGui.PushStyleColor(ImGuiCol.Text, Colours.White);
            var result = ImGui.Begin(name, ref showSettings, flags);
            ImGui.PopStyleColor();
            return result;
        }

        private static void TextWhite(string text) => WithWhiteText(() => ImGui.TextUnformatted(text));

        private static void HelpMarker(string text) {
            ImGui.TextDisabled("(?)");
            if (!ImGui.IsItemHovered()) {
                return;
            }

            ImGui.BeginTooltip();
            ImGui.PushTextWrapPos(ImGui.GetFontSize() * 20f);
            ImGui.TextUnformatted(text);
            ImGui.PopTextWrapPos();
            ImGui.EndTooltip();
        }

        private void DrawInner() {
            this.AcceptPending();

            foreach (var item in this.pending.ToList()) {
                if (this.DrawPending(item.Key, item.Value.Item1, item.Value.Item2)) {
                    this.pending.Remove(item.Key);
                }
            }

            if (!this.ShowSettings || !Begin(this.plugin.Name, ref this.showSettings, ImGuiWindowFlags.AlwaysAutoResize)) {
                return;
            }

            if (WithWhiteText(() => ImGui.CollapsingHeader("Server public key"))) {
                string serverPublic = this.plugin.Config.KeyPair!.PublicKey.ToHexString(upper: true);
                ImGui.TextUnformatted(serverPublic);
                this.DrawColours(this.plugin.Config.KeyPair.PublicKey, serverPublic);

                if (WithWhiteText(() => ImGui.Button("Regenerate"))) {
                    this.plugin.Server.RegenerateKeyPair();
                }
            }

            if (WithWhiteText(() => ImGui.CollapsingHeader("Settings", ImGuiTreeNodeFlags.DefaultOpen))) {
                TextWhite("Port");

                int port = this.plugin.Config.Port;
                if (WithWhiteText(() => ImGui.InputInt("##port", ref port))) {
                    ushort realPort = (ushort)Math.Min(ushort.MaxValue, Math.Max(1, port));
                    this.plugin.Config.Port = realPort;
                    this.plugin.Config.Save();
                }

                ImGui.Spacing();

                bool backlogEnabled = this.plugin.Config.BacklogEnabled;
                if (WithWhiteText(() => ImGui.Checkbox("Enable backlog", ref backlogEnabled))) {
                    this.plugin.Config.BacklogEnabled = backlogEnabled;
                    this.plugin.Config.Save();
                }

                int backlogCount = this.plugin.Config.BacklogCount;
                if (WithWhiteText(() => ImGui.DragInt("Backlog messages", ref backlogCount, 1f, 0, ushort.MaxValue))) {
                    this.plugin.Config.BacklogCount = (ushort)Math.Max(0, Math.Min(ushort.MaxValue, backlogCount));
                    this.plugin.Config.Save();
                }

                ImGui.Spacing();

                bool sendBattle = this.plugin.Config.SendBattle;
                if (WithWhiteText(() => ImGui.Checkbox("Send battle messages", ref sendBattle))) {
                    this.plugin.Config.SendBattle = sendBattle;
                    this.plugin.Config.Save();
                }

                ImGui.SameLine();
                HelpMarker("Changing this setting will not affect messages already in the backlog.");

                ImGui.Spacing();

                bool pairingMode = this.plugin.Config.PairingMode;
                if (WithWhiteText(() => ImGui.Checkbox("Pairing mode", ref pairingMode))) {
                    this.plugin.Config.PairingMode = pairingMode;
                    this.plugin.Config.Save();
                }

                ImGui.SameLine();
                HelpMarker("While in pairing mode, XIVChat Server will listen for information requests from clients broadcast on your local network and respond with information about the server. This will make it easier to add your server to a client, but this should be turned off when not actively adding new devices.");

                ImGui.Spacing();

                bool acceptNew = this.plugin.Config.AcceptNewClients;
                if (WithWhiteText(() => ImGui.Checkbox("Accept new clients", ref acceptNew))) {
                    this.plugin.Config.AcceptNewClients = acceptNew;
                    this.plugin.Config.Save();
                }

                ImGui.SameLine();
                HelpMarker("If this is disabled, XIVChat Server will only allow clients with already-trusted keys to connect.");
            }

            if (WithWhiteText(() => ImGui.CollapsingHeader("Trusted keys"))) {
                if (this.plugin.Config.TrustedKeys.Count == 0) {
                    ImGui.TextUnformatted("None");
                }

                ImGui.Columns(2);
                var maxKeyLength = 0f;
                foreach (var entry in this.plugin.Config.TrustedKeys.ToList()) {
                    var name = entry.Value.Item1;

                    var key = entry.Value.Item2;
                    var hex = key.ToHexString(true);

                    maxKeyLength = Math.Max(maxKeyLength, ImGui.CalcTextSize(name).X);

                    ImGui.TextUnformatted(name);
                    if (ImGui.IsItemHovered()) {
                        ImGui.BeginTooltip();
                        ImGui.TextUnformatted(hex);
                        this.DrawColours(key, hex);
                        ImGui.EndTooltip();
                    }

                    ImGui.NextColumn();

                    if (WithWhiteText(() => ImGui.Button($"Untrust##{entry.Key}"))) {
                        this.plugin.Config.TrustedKeys.Remove(entry.Key);
                        this.plugin.Config.Save();
                    }

                    ImGui.NextColumn();
                }

                ImGui.SetColumnWidth(0, maxKeyLength + ImGui.GetStyle().ItemSpacing.X * 2);
                ImGui.Columns(1);
            }


            if (WithWhiteText(() => ImGui.CollapsingHeader("Connected clients"))) {
                if (this.plugin.Server.Clients.Count == 0) {
                    ImGui.TextUnformatted("None");
                } else {
                    ImGui.Columns(3);

                    TextWhite("IP");
                    ImGui.NextColumn();
                    TextWhite("Key");
                    ImGui.NextColumn();
                    ImGui.NextColumn();

                    foreach (var client in this.plugin.Server.Clients) {
                        EndPoint remote;
                        try {
                            remote = client.Value.Conn.Client.RemoteEndPoint;
                        } catch (ObjectDisposedException) {
                            continue;
                        }

                        string ipAddress;
                        if (remote is IPEndPoint ip) {
                            ipAddress = ip.Address.ToString();
                        } else {
                            ipAddress = "Unknown";
                        }

                        ImGui.TextUnformatted(ipAddress);

                        ImGui.NextColumn();

                        var trustedKey = this.plugin.Config.TrustedKeys.Values.FirstOrDefault(entry => entry.Item2.SequenceEqual(client.Value.Handshake!.RemotePublicKey));
                        if (trustedKey != null && !trustedKey.Equals(default(Tuple<string, byte[]>))) {
                            ImGui.TextUnformatted(trustedKey!.Item1);
                            if (ImGui.IsItemHovered()) {
                                ImGui.BeginTooltip();

                                var hex = trustedKey.Item2.ToHexString(true);
                                ImGui.TextUnformatted(hex);
                                this.DrawColours(trustedKey.Item2, hex);

                                ImGui.EndTooltip();
                            }
                        }

                        ImGui.NextColumn();

                        if (WithWhiteText(() => ImGui.Button($"Disconnect##{client.Key}"))) {
                            client.Value.Disconnect();
                        }

                        ImGui.NextColumn();
                    }

                    ImGui.Columns(1);
                }
            }

            if (WithWhiteText(() => ImGui.CollapsingHeader("ACT/Teamcraft issues?"))) {
                ImGui.PushTextWrapPos(ImGui.GetFontSize() * 20);
                ImGui.TextUnformatted("Click on the button below to visit a website showing a workaround for ACT and Teamcraft having issues.");
                ImGui.PopTextWrapPos();

                if (WithWhiteText(() => ImGui.Button("Open website"))) {
                    System.Diagnostics.Process.Start("https://xiv.chat/server/workaround");
                }
            }

            ImGui.End();
        }

        private void DrawColours(byte[] bytes, string widthOf) {
            this.DrawColours(bytes, ImGui.CalcTextSize(widthOf).X);
        }

        private void DrawColours(byte[] bytes, float width = 0f) {
            var pos = ImGui.GetCursorScreenPos();
            var spacing = ImGui.GetStyle().ItemSpacing;

            var colours = bytes.ToColours();

            float sizeX = width == 0f ? 32f : width / colours.Count;

            for (int i = 0; i < colours.Count; i++) {
                var topLeft = new Vector2(
                    pos.X + (sizeX * i),
                    pos.Y + spacing.Y
                );
                var bottomRight = new Vector2(
                    pos.X + (sizeX * (i + 1)),
                    pos.Y + spacing.Y + 16
                );

                ImGui.GetWindowDrawList().AddRectFilled(
                    topLeft,
                    bottomRight,
                    ImGui.GetColorU32(colours[i])
                );
            }

            // create a spacing for 32px and spacing
            ImGui.Dummy(new Vector2(0, 16 + spacing.Y * 2));
        }

        public void OpenSettings(object? sender, EventArgs? args) {
            this.ShowSettings = true;
        }

        private void AcceptPending() {
            while (this.plugin.Server.pendingClients.Reader.TryRead(out var item)) {
                this.pending[Guid.NewGuid()] = item;
            }
        }

        private bool DrawPending(Guid id, Client client, Channel<bool, bool> accepted) {
            bool ret = false;

            var clientPublic = client.Handshake!.RemotePublicKey;
            var clientPublicHex = clientPublic.ToHexString(upper: true);
            var serverPublic = this.plugin.Config.KeyPair!.PublicKey;
            var serverPublicHex = serverPublic.ToHexString(upper: true);

            var width = Math.Max(ImGui.CalcTextSize(clientPublicHex).X, ImGui.CalcTextSize(serverPublicHex).X) + (ImGui.GetStyle().WindowPadding.X * 2);

            if (!Begin($"Incoming XIVChat connection##{clientPublic}", ImGuiWindowFlags.AlwaysAutoResize)) {
                return false;
            }

            ImGui.PushTextWrapPos(width);

            ImGui.TextUnformatted("A client that has not previously connected is attempting to connect to XIVChat. If this is you, please check the two keys below and make sure that they match what is displayed by the client.");

            ImGui.Separator();

            TextWhite("Server");
            ImGui.TextUnformatted(serverPublicHex);
            this.DrawColours(serverPublic, serverPublicHex);

            ImGui.Spacing();

            TextWhite("Client");
            ImGui.TextUnformatted(clientPublicHex);
            this.DrawColours(clientPublic, clientPublicHex);

            ImGui.Separator();

            ImGui.TextUnformatted("Give this client a name to remember it more easily if you trust it.");

            ImGui.PopTextWrapPos();

            if (!this.pendingNames.TryGetValue(id, out string name)) {
                name = "No name";
            }

            if (WithWhiteText(() => ImGui.InputText("Client name", ref name, 100, ImGuiInputTextFlags.AutoSelectAll))) {
                this.pendingNames[id] = name;
            }

            ImGui.Separator();

            ImGui.TextUnformatted("Do both keys match?");
            if (WithWhiteText(() => ImGui.Button("Yes"))) {
                accepted.Writer.TryWrite(true);
                this.plugin.Config.TrustedKeys[Guid.NewGuid()] = Tuple.Create(name, client.Handshake.RemotePublicKey);
                this.plugin.Config.Save();
                this.pendingNames.Remove(id);
                ret = true;
            }

            ImGui.SameLine();
            if (WithWhiteText(() => ImGui.Button("No"))) {
                accepted.Writer.TryWrite(false);
                this.pendingNames.Remove(id);
                ret = true;
            }

            ImGui.End();

            return ret;
        }
    }
}
