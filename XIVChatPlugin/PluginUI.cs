using Dalamud.Bindings.ImGui;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Net;
using System.Numerics;
using System.Threading.Channels;
using System.Threading.Tasks;

namespace XIVChatPlugin {
    internal class PluginUi {
        private Plugin Plugin { get; }

        private bool _showSettings;

        private bool ShowSettings {
            get => this._showSettings;
            set => this._showSettings = value;
        }

        private readonly Dictionary<Guid, Tuple<BaseClient, Channel<bool>>> _pending = new();
        private readonly Dictionary<Guid, string> _pendingNames = new(0);

        internal PluginUi(Plugin plugin) {
            this.Plugin = plugin ?? throw new ArgumentNullException(nameof(plugin), "Plugin cannot be null");
        }

        private static class Colours {
            internal static readonly Vector4 Primary = new(2 / 255f, 204 / 255f, 238 / 255f, 1.0f);
            internal static readonly Vector4 PrimaryDark = new(2 / 255f, 180 / 255f, 211 / 255f, 1.0f);
            internal static readonly Vector4 Background = new(46 / 255f, 46 / 255f, 46 / 255f, 1.0f);
            internal static readonly Vector4 Text = new(190 / 255f, 190 / 255f, 190 / 255f, 1.0f);
            internal static readonly Vector4 Button = new(90 / 255f, 89 / 255f, 90 / 255f, 1.0f);
            internal static readonly Vector4 ButtonActive = new(123 / 255f, 122 / 255f, 124 / 255f, 1.0f);
            internal static readonly Vector4 ButtonHovered = new(108 / 255f, 107 / 255f, 109 / 255f, 1.0f);

            internal static readonly Vector4 White = new(1f, 1f, 1f, 1f);
        }

        internal void Draw() {
            ImGui.PushStyleColor(ImGuiCol.TitleBg, Colours.PrimaryDark);
            ImGui.PushStyleColor(ImGuiCol.TitleBgActive, Colours.Primary);
            ImGui.PushStyleColor(ImGuiCol.TitleBgCollapsed, Colours.PrimaryDark);
            ImGui.PushStyleColor(ImGuiCol.WindowBg, Colours.Background);
            ImGui.PushStyleColor(ImGuiCol.Text, Colours.Text);
            ImGui.PushStyleColor(ImGuiCol.Button, Colours.Button);
            ImGui.PushStyleColor(ImGuiCol.ButtonActive, Colours.ButtonActive);
            ImGui.PushStyleColor(ImGuiCol.ButtonHovered, Colours.ButtonHovered);

            try {
                this.DrawInner();
            } finally {
                ImGui.PopStyleColor(8);
            }
        }

        private static T WithWhiteText<T>(Func<T> func) {
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

            foreach (var item in this._pending.ToList()) {
                if (this.DrawPending(item.Key, item.Value.Item1, item.Value.Item2)) {
                    this._pending.Remove(item.Key);
                }
            }

            if (!this.ShowSettings) {
                return;
            }

            if (!Begin(Plugin.Name, ref this._showSettings, ImGuiWindowFlags.AlwaysAutoResize)) {
                ImGui.End();
                return;
            }

            if (WithWhiteText(() => ImGui.CollapsingHeader("服务器公钥"))) {
                string serverPublic = this.Plugin.Config.KeyPair!.PublicKey.ToHexString(upper: true);
                ImGui.TextUnformatted(serverPublic);
                DrawColours(this.Plugin.Config.KeyPair.PublicKey, serverPublic);

                if (WithWhiteText(() => ImGui.Button("重新生成"))) {
                    this.Plugin.Server.RegenerateKeyPair();
                    this.Plugin.Relay?.ResendPublicKey();
                }

                ImGui.SameLine();

                if (WithWhiteText(() => ImGui.Button("复制"))) {
                    ImGui.SetClipboardText(serverPublic);
                }
            }

            if (WithWhiteText(() => ImGui.CollapsingHeader("设置", ImGuiTreeNodeFlags.DefaultOpen))) {
                TextWhite("端口");

                int port = this.Plugin.Config.Port;
                if (WithWhiteText(() => ImGui.InputInt("##port", ref port))) {
                    var realPort = (ushort) Math.Min(ushort.MaxValue, Math.Max(1, port));
                    this.Plugin.Config.Port = realPort;
                    this.Plugin.Config.Save();

                    this.Plugin.RelaunchServer();
                }

                ImGui.Spacing();

                var backlogEnabled = this.Plugin.Config.BacklogEnabled;
                if (WithWhiteText(() => ImGui.Checkbox("启用历史消息", ref backlogEnabled))) {
                    this.Plugin.Config.BacklogEnabled = backlogEnabled;
                    this.Plugin.Config.Save();
                }

                int backlogCount = this.Plugin.Config.BacklogCount;
                if (WithWhiteText(() => ImGui.DragInt("历史消息数量", ref backlogCount, 1f, 0, ushort.MaxValue))) {
                    this.Plugin.Config.BacklogCount = (ushort) Math.Max(0, Math.Min(ushort.MaxValue, backlogCount));
                    this.Plugin.Config.Save();
                }

                ImGui.Spacing();

                var sendBattle = this.Plugin.Config.SendBattle;
                if (WithWhiteText(() => ImGui.Checkbox("发送战斗消息", ref sendBattle))) {
                    this.Plugin.Config.SendBattle = sendBattle;
                    this.Plugin.Config.Save();
                }

                ImGui.SameLine();
                HelpMarker("更改此设置不会影响已经保存的历史消息。");

                ImGui.Spacing();

                var messagesCountAsInput = this.Plugin.Config.MessagesCountAsInput;
                if (WithWhiteText(() => ImGui.Checkbox("将远程消息计为用户输入", ref messagesCountAsInput))) {
                    this.Plugin.Config.MessagesCountAsInput = messagesCountAsInput;
                    this.Plugin.Config.Save();
                }

                ImGui.SameLine();
                HelpMarker("启用后，从任意客户端发送消息都会被视为用户输入，并重置离开计时器。");

                ImGui.Spacing();

                var pairingMode = this.Plugin.Config.PairingMode;
                if (WithWhiteText(() => ImGui.Checkbox("配对模式", ref pairingMode))) {
                    this.Plugin.Config.PairingMode = pairingMode;
                    this.Plugin.Config.Save();
                }

                ImGui.SameLine();
                HelpMarker("配对模式下，XIVChat 服务器会响应局域网内客户端的广播查询，方便客户端发现并添加服务器。不添加新设备时应关闭此选项。");

                ImGui.Spacing();

                var acceptNew = this.Plugin.Config.AcceptNewClients;
                if (WithWhiteText(() => ImGui.Checkbox("接受新客户端", ref acceptNew))) {
                    this.Plugin.Config.AcceptNewClients = acceptNew;
                    this.Plugin.Config.Save();
                }

                ImGui.SameLine();
                HelpMarker("关闭后，XIVChat 服务器只允许持有已信任密钥的客户端连接。");
            }

            if (WithWhiteText(() => ImGui.CollapsingHeader("中继"))) {
                var allowRelay = this.Plugin.Config.AllowRelayConnections;
                if (WithWhiteText(() => ImGui.Checkbox("允许中继连接", ref allowRelay))) {
                    if (allowRelay) {
                        this.Plugin.StartRelay();
                    } else {
                        this.Plugin.StopRelay();
                    }

                    this.Plugin.Config.AllowRelayConnections = allowRelay;
                    this.Plugin.Config.Save();
                }

                ImGui.SameLine();
                HelpMarker("启用后，将接受来自 XIVChat 中继服务器的连接。");

                ImGui.Spacing();

                var relayStatus = this.Plugin.Relay?.Status ?? ConnectionStatus.Disconnected;
                ImGui.TextUnformatted($"连接状态：{ConnectionStatusText(relayStatus)}");

                ImGui.Spacing();

                if ((this.Plugin.Relay?.Status ?? ConnectionStatus.Disconnected) == ConnectionStatus.Disconnected && Relay.ConnectionError != null) {
                    ImGui.TextUnformatted($"错误：{Relay.ConnectionError}");

                    ImGui.Spacing();
                }

                var relayAuth = this.Plugin.Config.RelayAuth ?? "";
                WithWhiteText(() => ImGui.TextUnformatted("中继验证码"));
                ImGui.PushItemWidth(-1f);
                if (ImGui.InputText("###relay-auth", ref relayAuth, 100, ImGuiInputTextFlags.Password)) {
                    relayAuth = relayAuth.Trim();
                    if (relayAuth.Length == 0) {
                        relayAuth = null;
                    }

                    this.Plugin.Config.RelayAuth = relayAuth;
                    this.Plugin.Config.Save();
                }

                ImGui.PopItemWidth();
            }

            if (WithWhiteText(() => ImGui.CollapsingHeader("已信任密钥"))) {
                if (this.Plugin.Config.TrustedKeys.Count == 0) {
                    ImGui.TextUnformatted("无");
                }

                ImGui.Columns(2);
                var maxKeyLength = 0f;
                foreach (var entry in this.Plugin.Config.TrustedKeys.ToList()) {
                    var name = entry.Value.Item1;

                    var key = entry.Value.Item2;
                    var hex = key.ToHexString(true);

                    maxKeyLength = Math.Max(maxKeyLength, ImGui.CalcTextSize(name).X);

                    ImGui.TextUnformatted(name);
                    if (ImGui.IsItemHovered()) {
                        ImGui.BeginTooltip();
                        ImGui.TextUnformatted(hex);
                        DrawColours(key, hex);
                        ImGui.EndTooltip();
                    }

                    ImGui.NextColumn();

                    if (WithWhiteText(() => ImGui.Button($"取消信任##{entry.Key}"))) {
                        this.Plugin.Config.TrustedKeys.Remove(entry.Key);
                        this.Plugin.Config.Save();
                    }

                    ImGui.NextColumn();
                }

                ImGui.SetColumnWidth(0, maxKeyLength + ImGui.GetStyle().ItemSpacing.X * 2);
                ImGui.Columns(1);
            }


            if (WithWhiteText(() => ImGui.CollapsingHeader("已连接客户端"))) {
                var clients = this.Plugin.Server.Clients
                    .Where(client => client.Value.Connected)
                    .ToList();
                if (clients.Count == 0) {
                    ImGui.TextUnformatted("无");
                } else {
                    ImGui.Columns(3);

                    TextWhite("IP");
                    ImGui.NextColumn();
                    TextWhite("密钥");
                    ImGui.NextColumn();
                    ImGui.NextColumn();

                    foreach (var client in clients) {
                        if (!client.Value.Connected) {
                            continue;
                        }

                        IPAddress? remote;
                        try {
                            remote = client.Value.Remote;
                        } catch (ObjectDisposedException) {
                            continue;
                        }

                        var ipAddress = remote?.ToString() ?? "未知";

                        if (client.Value is RelayConnected) {
                            ipAddress = "(R) " + ipAddress;
                        }

                        ImGui.TextUnformatted(ipAddress);

                        ImGui.NextColumn();

                        var trustedKey = this.Plugin.Config.TrustedKeys.Values.FirstOrDefault(entry => client.Value.Handshake != null && entry.Item2.SequenceEqual(client.Value.Handshake.RemotePublicKey));
                        if (trustedKey != null && !trustedKey.Equals(default(Tuple<string, byte[]>))) {
                            ImGui.TextUnformatted(trustedKey!.Item1);
                            if (ImGui.IsItemHovered()) {
                                ImGui.BeginTooltip();

                                var hex = trustedKey.Item2.ToHexString(true);
                                ImGui.TextUnformatted(hex);
                                DrawColours(trustedKey.Item2, hex);

                                ImGui.EndTooltip();
                            }
                        }

                        ImGui.NextColumn();

                        if (WithWhiteText(() => ImGui.Button($"断开连接##{client.Key}"))) {
                            if (client.Value is RelayConnected) {
                                Task.Run(() => this.Plugin.Relay?.DisconnectClient(client.Value.Handshake!.RemotePublicKey))
                                    .ContinueWith(_ => client.Value.Disconnect());
                            } else {
                                client.Value.Disconnect();
                            }
                        }

                        ImGui.NextColumn();
                    }

                    ImGui.Columns(1);
                }
            }

            ImGui.End();
        }

        private static void DrawColours(byte[] bytes, string widthOf) {
            DrawColours(bytes, ImGui.CalcTextSize(widthOf).X);
        }

        private static void DrawColours(byte[] bytes, float width = 0f) {
            var pos = ImGui.GetCursorScreenPos();
            var spacing = ImGui.GetStyle().ItemSpacing;

            var colours = bytes.ToColours();

            var sizeX = width == 0f ? 32f : width / colours.Count;

            for (var i = 0; i < colours.Count; i++) {
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

        public void OpenSettings() {
            this.ShowSettings = true;
        }

        private static string ConnectionStatusText(ConnectionStatus status) => status switch {
            ConnectionStatus.Connecting => "正在连接",
            ConnectionStatus.Negotiating => "正在协商",
            ConnectionStatus.Connected => "已连接",
            _ => "已断开",
        };

        private void AcceptPending() {
            while (this.Plugin.Server.PendingClients.Reader.TryRead(out var item)) {
                this._pending[Guid.NewGuid()] = item;
            }
        }

        private bool DrawPending(Guid id, BaseClient client, Channel<bool, bool> accepted) {
            var ret = false;

            var clientPublic = client.Handshake!.RemotePublicKey;
            var clientPublicHex = clientPublic.ToHexString(upper: true);
            var serverPublic = this.Plugin.Config.KeyPair!.PublicKey;
            var serverPublicHex = serverPublic.ToHexString(upper: true);

            var width = Math.Max(ImGui.CalcTextSize(clientPublicHex).X, ImGui.CalcTextSize(serverPublicHex).X) + (ImGui.GetStyle().WindowPadding.X * 2);

            if (!Begin($"收到 XIVChat 连接请求##{clientPublic}", ImGuiWindowFlags.AlwaysAutoResize)) {
                return false;
            }

            ImGui.PushTextWrapPos(width);

            ImGui.TextUnformatted("一个从未连接过的客户端正在尝试连接 XIVChat。若这是你的设备，请检查下方两个密钥，并确认它们与客户端显示的内容一致。");

            ImGui.Separator();

            TextWhite("服务器");
            ImGui.TextUnformatted(serverPublicHex);
            DrawColours(serverPublic, serverPublicHex);

            ImGui.Spacing();

            TextWhite("客户端");
            ImGui.TextUnformatted(clientPublicHex);
            DrawColours(clientPublic, clientPublicHex);

            ImGui.Separator();

            ImGui.TextUnformatted("如果信任此客户端，请为它命名，以便日后识别。");

            ImGui.PopTextWrapPos();

            if (!this._pendingNames.TryGetValue(id, out var name)) {
                name = "未命名";
            }

            if (WithWhiteText(() => ImGui.InputText("客户端名称", ref name, 100, ImGuiInputTextFlags.AutoSelectAll))) {
                this._pendingNames[id] = name;
            }

            ImGui.Separator();

            ImGui.TextUnformatted("两个密钥是否一致？");
            if (WithWhiteText(() => ImGui.Button("是"))) {
                accepted.Writer.TryWrite(true);
                this.Plugin.Config.TrustedKeys[Guid.NewGuid()] = Tuple.Create(name, client.Handshake.RemotePublicKey);
                this.Plugin.Config.Save();
                this._pendingNames.Remove(id);
                ret = true;
            }

            ImGui.SameLine();
            if (WithWhiteText(() => ImGui.Button("否"))) {
                accepted.Writer.TryWrite(false);
                this._pendingNames.Remove(id);
                ret = true;
            }

            ImGui.End();

            return ret;
        }
    }
}
