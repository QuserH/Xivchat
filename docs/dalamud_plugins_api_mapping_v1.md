# Dalamud 插件 — 游戏接口映射（V1 扩展版，中文）

> 文件位置：docs/dalamud_plugins_api_mapping_v1.md（api 分支）
>
> 本次为前一版的扩展与细化：把常用的 Dalamud / FFXIV 接口逐项写成“可直接复用”的规范说明（描述、可读取/监听的数据、常用属性/方法/事件、最小可运行示例、注意事项与风险），并补充了更详细的对外通信建议与线程/版本维护注意点。文中示例均为最小示例，适合直接拷贝到插件中做 PoC。

---

## 说明与目标

目标：为你要实现的“插件读取游戏信息并传回 App”场景提供一份详尽、可操作的参考：
- 明确每个常用 Dalamud/游戏服务能拿到哪些数据、如何拿（代码示例）
- 说明何时应使用高层服务，何时必须降级到 Hook/FFXIVClientStructs
- 给出安全、稳定的对外通信方案（优劣与实现要点）
- 给出版本兼容与维护建议，便于长期稳定运行

本文件为扩展版（详细实现导向）。我已经把这些内容写成可复用的代码片段与注意事项；如果你确认我将这些片段另外保存为独立示例文件（docs/snippets/*.cs），我可以分批提交。

---

## 一、使用约定（术语与前置）

- 高层 Dalamud 服务：通过依赖注入（[PluginService] 或构造器注入）获得，例如 IClientState、IObjectTable、IDataManager、IFramework 等。推荐优先使用。
- 低层访问：使用 FFXIVClientStructs（结构映射）、ISigScanner / IGameInteropProvider 做 Hook/签名扫描或直接读写内存。只有在高层不能满足需求时才降级。
- 线程约束：很多读取必须在 Framework 主线程或特定线程中执行（见 IFramework.IsInFrameworkUpdateThread）。跨线程 UI/游戏结构访问需用 RunOnFrameworkThread / 调度。
- 风险：Hook/签名/内存写入对游戏版本敏感并有封禁风险。尽量只“读取”而不“输入/写回”。

---

## 二、接口详解（按重要性排序，每项包含：说明 / 能拿到的数据 / 常用属性/方法/事件 / 最小示例 / 注意事项 / 仓库实例）

下面我把每个接口做成独立小节，便于复制到你的插件 README 或实现文档中。

### 1) IDalamudPluginInterface
- 说明：插件的主入口/生命周期管理接口。用于创建服务、读取/保存配置、注册 UI 生命周期回调（UiBuilder）和语言变更等。
- 能拿到的数据/能力：插件配置（GetPluginConfig/SavePluginConfig）、UiBuilder 事件（OpenConfigUi/Draw）、语言变化事件、依赖注入/创建服务（Create<T>() / Inject）。
- 常用方法/事件：GetPluginConfig(), SavePluginConfig(), Create<T>(), Inject(obj), UiBuilder.Draw, UiBuilder.OpenConfigUi, LanguageChanged
- 最小示例（构造器）:

```csharp
public sealed class Plugin : IDalamudPlugin {
  public Plugin(IDalamudPluginInterface pi) {
    // 注入/创建服务或配置
    var cfg = pi.GetPluginConfig() as Configuration ?? new Configuration();
    pi.Inject(cfg);
    // 注册 UI Draw
    pi.UiBuilder.Draw += OnDraw;
  }
}
```

- 注意事项：不要在构造器做大量阻塞工作（如长网络请求），应异步启动后台任务并用 Framework/Task 管理。
- 仓库实例：Meddle Plugin 构造（依赖注入示例）：https://github.com/PassiveModding/Meddle/blob/312ad2610b74083376838964f5aebe6b5886449b/Meddle/Meddle.Plugin/Plugin.cs#L1-L89

---

### 2) IClientState / IPlayerState
- 说明：客户端与玩家短期状态（登录、LocalPlayer、ContentId、TerritoryType、职业/等级变化等）。
- 能拿到的数据：IsLoggedIn、LocalPlayer 引用、ContentId、HomeWorld、CharacterName、事件（Login/Logout/LevelChanged/ClassJobChanged）
- 常用属性/事件：IsLoggedIn, LocalPlayer, Login, Logout, LevelChanged, ClassJobChanged
- 最小示例（读取玩家名）:

```csharp
if (clientState.IsLoggedIn && clientState.LocalPlayer != null) {
  var name = clientState.LocalPlayer.Name.TextValue;
}
```

- 注意事项：LocalPlayer 可能为 null；检查 IsLoggedIn。在 UI/Addon 读取中通常与 IFramework.Update 协作。
- 仓库实例：OwnCharacterDataProvider（HRT）周期性使用 IPlayerState / IClientState：https://github.com/Koenari/HimbeertoniRaidTool/blob/4f31b113692bf41fefcd8fdddf5709b48f4e7fae/HimbeertoniRaidTool/Services/OwnCharacterDataProvider.cs#L1-L91

---

### 3) IObjectTable
- 说明：场景对象表（Object Table），可以枚举世界中的对象（玩家、NPC、怪物等）。
- 能拿到的数据：对象枚举（IObject、ICharacter、IPlayerCharacter）、SearchById / SearchByEntityId / GetCharacters / LocalPlayer
- 典型用途：读取实体位置、名字、GameObjectId、判断对象可见性
- 最小示例（查找特定 EntityId）:

```csharp
var obj = objectTable.SearchByEntityId(entityId);
if (obj is IPlayerCharacter p) {
  var name = p.Name.TextValue;
}
```

- 注意：返回的接口可能是轻包装，若要读取更多内部字段可用 obj.Address / obj.Pointer 给 FFXIVClientStructs 进一步解析。
- 仓库实例：Meddle CommonUi 使用 objectTable.GetCharacters：https://github.com/PassiveModding/Meddle/blob/312ad2610b74083376838964f5aebe6b5886449b/Meddle/Meddle.Plugin/UI/CommonUI.cs#L1-L108

---

### 4) IFramework
- 说明：游戏框架的主循环钩子，可用于每帧或周期性任务（Update 事件）并检测当前线程。
- 能拿到的数据：Update 事件（UpdateDelta），IsInFrameworkUpdateThread
- 常用用法：framework.Update += OnFrameworkUpdate; 在回调里按时间间隔采样数据
- 最小示例（每 250ms 采样并发送）:

```csharp
private TimeSpan accum = TimeSpan.Zero;
private void OnFrameworkUpdate(IFramework f) {
  accum += f.UpdateDelta;
  if (accum.TotalMilliseconds >= 250) {
    accum = TimeSpan.Zero;
    // 读取 clientState.LocalPlayer 等并发送
  }
}
```

- 注意：某些游戏结构需在 Framework 线程访问；如果从其他线程需要调用 Framework 线程，使用 RunOnFrameworkThread。
- 仓库实例：OwnCharacterDataProvider 在 Framework.Update 中触发 UpdateChar/UpdateWallet：https://github.com/Koenari/HimbeertoniRaidTool/blob/4f31b113692bf41fefcd8fdddf5709b48f4e7fae/HimbeertoniRaidTool/Services/OwnCharacterDataProvider.cs#L1-L91

---

### 5) IDataManager (Lumina)
- 说明：读取静态 Excel 数据（动作表、物品表、地图、icon 名称等），不随场景变化。
- 能拿到的数据：通过 GetExcelSheet<T>() 读取各种 GeneratedSheets（Action, Item, TerritoryType 等）
- 最小示例：

```csharp
var actionSheet = dataManager.GetExcelSheet<Lumina.Excel.GeneratedSheets.Action>();
var row = actionSheet.GetRow(actionId);
var actionName = row?.Name.ToString();
```

- 注意：Lumina 表示静态数据，适合把 id 转为人类可读名称或图标 id。
- 仓库实例：OceanFishin 使用 DataManager.GetExcelSheet：https://github.com/markjsosnowski/OceanFishin/blob/038818ced30f79dc358c5aaf4fae126461eb10b9/MainWindow.cs#L1-L101

---

### 6) ICommandManager
- 说明：注册 /slash 命令（聊天命令）以便用户交互或调试。通常由 pluginInterface 或注入获得。
- 最小示例：

```csharp
commandManager.AddHandler("/myplugin", new CommandInfo( OnCommand ) { HelpMessage = "..." });
```

- 注意：命令会在玩家聊天框中触发，避免滥用自动发送行为。

---

### 7) IChatGui
- 说明：聊天发送/接受与 SeString 处理工具。可以添加聊天处理器或发送聊天。
- 能做的事：AddChatHandler/RemoveChatHandler、SendMessage (有时需小心)、SeString 解析
- 示例（解析 SeString）：RoleplayingVoice 的 TalkUtils 使用 SeString.Parse 将文本节点解析为可读字符串：https://github.com/Sebane1/RoleplayingVoiceDalamud/blob/c73282caf0151cead920db3ec1926d0605e614c5/ArtemisRoleplayingKit/Voice/TalkUtils.cs#L1-L96

注意：通过聊天发送可能会发送到服务器，具有风险；要避免自动发送到公共频道的行为以免触犯 ToS。

---

### 8) IGameGui（GetAddonByName）
- 说明：获取游戏 UI Addon 的内存地址，常配合 FFXIVClientStructs 的 AtkUnitBase/Addon 结构读取界面节点（文本、可见性、节点缓冲区）。
- 常见用途：读取对话框文本、战斗 UI add-on（如 `IKDFishingLog`）的内部节点。
- 最小示例：

```csharp
nint addr = gameGui.GetAddonByName("Talk");
if (addr != nint.Zero) {
  var addon = (AddonTalk*)addr.ToPointer();
  var speaker = SeString.Parse(addon->AtkTextNode220.NodeText.StringPtr, ...).TextValue;
}
```

- 注意：Addon 内部结构依赖客户端版本；仅用于只读。RoleplayingVoice/ArtemisRoleplayingKit 的实现是成熟样例：https://github.com/Sebane1/RoleplayingVoiceDalamud/blob/c73282caf0151cead920db3ec1926d0605e614c5/ArtemisRoleplayingKit/Voice/AddonTalkManager.cs#L1-L40

---

### 9) ISigScanner & IGameInteropProvider（签名扫描与 Hook）
- 说明：
  - ISigScanner：通过字节签名（signature）在进程内查找函数/数据地址。
  - IGameInteropProvider：在已知地址上创建 Hook 或直接以委托方式调用原始函数（HookFromSignature / HookFromAddress）。
- 能做的事：监听 ActionManager.UseAction、包解析函数或 VFX 回调，捕获客户端内事件以获得更细粒度的战斗/输入行为。
- 最小示例（Hook ActionManager.UseAction）：

```csharp
// 定义委托签名
private delegate bool UseActionDelegate(ActionManager* am, ActionType type, uint actionId, ulong target, uint extra, ActionManager.UseActionMode mode, uint comboId, bool* outOpt);

// 在构造里
try {
  _useActionHook = interopProvider.HookFromSignature<UseActionDelegate>("E8 ?? ?? ?? ?? B0 01 EB B6", UseActionDetour);
  _useActionHook.Enable();
} catch (Exception e) { log.Error(e, "hook fail"); }
```

- 风险与注意：Hook 与签名高度依赖客户端版本；签名失效后可能导致异常或 crash；Hook 可能被视作侵入操作，有封禁风险；在实现前评估风险并限制为只读监听。
- 仓库实例：ArtemisRoleplayingKit UseActionListener：HookFromSignature 示例：https://github.com/Sebane1/RoleplayingVoiceDalamud/blob/c73282caf0151cead920db3ec1926d0605e614c5/ArtemisRoleplayingKit/Voice/UseActionListener.cs#L1-L89

---

### 10) FFXIVClientStructs（低层结构）
- 说明：FFXIV 客户端内的数据结构映射，允许插件在 C# 中以 unsafe 方式直接读取游戏内存（Actor、DrawObject、Model、AtkUnitBase、TextNode 等）。
- 可读取/解析的数据：几乎任意游戏内结构（但需对结构偏移与大小有正确版本匹配）。示例：CharacterBase、Model、Attach、AtkTextNode、ActionManager 结构等。
- 最小示例（读取 DrawObject 指向的 Model）：见 Meddle 的 StructExtensions/GetMaterials：https://github.com/PassiveModding/Meddle/blob/312ad2610b74083376838964f5aebe6b5886449b/Meddle/Meddle.Plugin/Models/StructExtensions.cs#L1-L75

- 注意：使用前必须添加 AllowUnsafeBlocks，并确保 FFXIVClientStructs 的版本与游戏匹配；版本不一致可能导致内存读写错误与崩溃。

---

### 11) IPluginLog
- 说明：日志记录接口，推荐在插件中注入并把重要事件/错误记录到日志，便于本地调试与问题追踪。
- 用法：pluginLog.LogDebug/Information/Warning/Error

---

### 12) ITextureProvider / 图像操作
- 说明：将游戏内 icon/id 转为 ImGui 可用纹理（CreateFromRaw / GetFromGameIcon 等），用于本地 UI 显示。
- 仓库实例：Meddle 中将贴图做成 TextureWrap：https://github.com/PassiveModding/Meddle/blob/312ad2610b74083376838964f5aebe6b5886449b/Meddle/Meddle.Plugin/UI/ColorTableTester.cs#L1-L110

---

### 13) ICondition / Flag（状态查询）
- 说明：用来判断玩家是否处于战斗中、是否处于创建角色流程、是否被捆绑等条件。
- 使用示例：if (condition[ConditionFlag.InCombat]) { ... }

---

### 14) IPartyList / ITargetManager / IJobGauges（队伍/目标/职业资源）
- 用途：读取队伍成员 ContentId/名字/EntityId、获取目标的 HP/距离、读取职业特定资源（例如连击/调和等）。
- 仓库实例：HimbeertoniRaidTool 的 CharacterInfoService 使用 party/InfoProxy 来尝试解析 content id：https://github.com/Koenari/HimbeertoniRaidTool/blob/4f31b113692bf41fefcd8fdddf5709b48f4e7fae/HimbeertoniRaidTool/Services/CharacterInfoService.cs#L1-L93

---

### 15) 插件间 IPC（CallGate / Ipc）
- 说明：Dalamud 提供插件间调用接口（CallGate、ICallGateSubscriber），可用于插件间共享数据或暴露服务，但不能直接供外部 App 使用。
- 仓库实例：Brio 的 BrioAPI 展示如何暴露 IPC：https://github.com/Etheirys/Brio/blob/76254a165e839fc97e4edae065905d7c3976652e/BrioAPI_V2.cs

---

## 三、对外通信建议（把游戏数据传回你的 App）

优选方案（按推荐顺序）：

1) WebSocket（本地 loopback）
- 优点：双向、实时、便于保持连接并做握手/鉴权（本地可以简单 token），适合 UI/实时 telemetry。
- 缺点：需维护连接状态与重连逻辑。对实时性要求高时性能最好办。
- 最小实现（插件端推送示例）：

```csharp
using System.Net.WebSockets;
using System.Text;

async Task StartSender(IClientState clientState, CancellationToken ct) {
  var ws = new ClientWebSocket();
  await ws.ConnectAsync(new Uri("ws://127.0.0.1:42000"), ct);
  while (ws.State == WebSocketState.Open && !ct.IsCancellationRequested) {
    var payload = new { time = DateTime.UtcNow, player = clientState.LocalPlayer?.Name?.TextValue };
    var bytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(payload));
    await ws.SendAsync(bytes, WebSocketMessageType.Text, true, ct);
    await Task.Delay(250, ct);
  }
}
```

2) HTTP POST（本地）
- 优点：实现简单，易测试；适用于不需要持续双向连接的场景。
- 缺点：单向、频率高时需注意性能与请求队列。

3) 文件（JSON/SQLite）
- 优点：最简单（无网络），外部 App 轮询读取；兼容性最好。
- 缺点：延迟高、文件锁/并发需注意。

4) TCP/UDP Socket
- 优点：最高效率（UDP 非关键数据可丢）；适合高频 telemetry
- 缺点：需自己做包重组/心跳/鉴权/加密逻辑。

实现要点（通用）：
- 仅绑定 loopback（127.0.0.1），不要监听公网地址。
- 为高频数据做本地缓冲/去重（例如只有在数值变化或每 n ms 发送一次）。
- 对敏感数据做脱敏或避免上传。 

---

## 四、安全、合规与反作弊风险

1) 读取行为通常被社区接受（UI、对象表、DataManager）;
2) Hook / 修改内存 / 模拟输入 / 自动化玩家行为存在风险；尽量避免写入操作或自动化输入；如果必须实现，明确列出风险并要求用户知情同意；
3) 签名/Hook 依赖游戏版本，需有异常捕获与退路；
4) 强烈建议只绑定 loopback 地址，严格控制可序列化的数据与访问权限。

---

## 五、版本兼容与维护建议

- 签名/内存读写：每次游戏更新需验证签名与 ClientStructs 版本；建议：将签名集中维护在单独模块（sig table），便于热修复。
- 单元化：把“读取数据”与“对外通信”分成两个模块（Reader / Transport），便于调试与替换。
- 退化策略：若 Hook/签名失败，应降级到高层方法（尽可能），并在日志中说明。

---

## 六、下一步（我将如何继续并需要你确认的项）

我会把本文件中的“最小示例”拆成单独的代码片段文件：
- docs/snippets/read_player.cs (使用 IClientState / IObjectTable)
- docs/snippets/read_addon_text.cs (IGameGui + AtkTextNode + SeString)
- docs/snippets/hook_useaction.cs (ISigScanner + IGameInteropProvider 钩子)
- docs/snippets/websocket_sender.cs (插件端 WebSocket 客户端)

请确认你是否要我立即把这些示例文件也以提交的方式加入 api 分支？回复“是，提交示例”或“先不要”。

---

文件已更新为扩展版并保存到 api 分支（我已把详细接口描述写入该文件）。下一步我会按你的确认把独立示例片段提交为 docs/snippets/ 下的 .cs 文件，方便你直接编译/测试。

