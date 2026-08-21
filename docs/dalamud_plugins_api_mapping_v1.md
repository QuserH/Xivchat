# Dalamud 插件 — 游戏接口映射（V1 初稿，中文）

> 文件位置：docs/dalamud_plugins_api_mapping_v1.md（api 分支）

说明：这是第一版中文初稿。我已将初稿提交到你仓库的 api 分支。本稿为工作文档，后续我会把前 100 个最相关的仓库逐一做静态检索并把每个插件调用的游戏接口、代码片段与 permalink 补充到本文件中。

---

## 概要

目标：在公开仓库中找出 Dalamud（“卫月”）插件，识别它们读取/使用游戏状态时调用的 Dalamud / FFXIV 接口，以便你实现一个插件，从游戏中读取信息并将其传回你自己的 app（手机/服务端）。

本次交付（初稿 v1）包含：
- 一批代表性插件示例及它们使用的主要 Dalamud / 游戏接口（示例源码行链接）
- 常用的 Dalamud / 游戏接口汇总，便于直接引用
- 将游戏信息传回外部 App 的推荐方式（优缺点）
- 后续工作计划（我将如何产出完整的“前 100”映射）

注意：我使用的自动检索会分页限制结果。接下来的完整 Top-100 扫描会把更多源码 permalinks 补齐并以中文写出每个插件的调用点说明。

---

## 代表性插件示例（快速索引与发现）

以下是我已抽取并确认的若干热门或典型插件，列出仓库与主要调用的接口，并附 1–2 个源码 permalink 作为示例：

1) TPie — 圆形热键环插件
   - 仓库：https://github.com/Tischel/TPie
   - 常用接口：IClientState、IObjectTable、ICommandManager、IDalamudPluginInterface、IDataManager、IFramework、IGameGui、ISigScanner、IGameInteropProvider、IKeyState、IPluginLog、ITextureProvider
   - 示例源码：Plugin.cs（服务注入）
     https://github.com/Tischel/TPie/blob/a861c2132284430e6b2d4666d6fc708051d9b115/TPie/Plugin.cs
     CooldownHelper（使用 ActionManager / FFXIVClientStructs）：
     https://github.com/Tischel/TPie/blob/a861c2132284430e6b2d4666d6fc708051d9b115/TPie/Helpers/CooldownHelper.cs

2) DelvUI — 完整 UI 替换插件
   - 仓库：https://github.com/DelvUI/DelvUI
   - 常用接口：大量 Dalamud 服务（IPluginLog、IChatGui、IGameGui、IFramework、ISigScanner、IObjectTable、IClientState、ITextureProvider、INotificationManager、IPartyList 等），并使用 Hook 读取/拦截游戏内部行为
   - 示例源码：PullTimerHelper（Hook 游戏 Agent）：
     https://github.com/DelvUI/DelvUI/blob/edc6ec52620ddf3feaa28115e69329a09b865f4a/DelvUI/Helpers/PullTimerHelper.cs

3) Brio — GPose / Actor API 与插件间 IPC
   - 仓库：https://github.com/Etheirys/Brio
   - 常用接口：通过 PluginService 注入的 Banyak Dalamud 服务（IFramework、IClientState、IObjectTable、IDataManager 等），并提供自己的 IPC（BrioAPI，使用 ICallGateSubscriber）
   - 示例源码：DalamudService（服务注入）与 BrioAPI
     https://github.com/Etheirys/Brio/blob/76254a165e839fc97e4edae065905d7c3976652e/Brio/Game/Core/DalamudService.cs
     https://github.com/Etheirys/Brio/blob/76254a165e839fc97e4edae065905d7c3976652e/BrioAPI_V2.cs

4) Triad Buddy — 读 UI 与内存（Triple Triad）
   - 仓库：https://github.com/MgAl2O4/FFTriadBuddyDalamud
   - 常用接口：IDataManager、ICommandManager、IFramework、IGameGui、ISigScanner、ITextureProvider；并使用不安全内存读取解析游戏 Agent/UI
   - 示例源码：Service.cs 和 UIReaderScheduler
     https://github.com/MgAl2O4/FFTriadBuddyDalamud/blob/1bffa57921d3eedbb201c227b6ea05f35a0ee75e/plugin/Service.cs
     https://github.com/MgAl2O4/FFTriadBuddyDalamud/blob/1bffa57921d3eedbb201c227b6ea05f35a0ee75e/utils/UIReaderScheduler.cs

5) DeathRecap — 抓包 Hook 捕获战斗事件
   - 仓库：https://github.com/Kouzukii/ffxiv-deathrecap
   - 常用接口：IDalamudPluginInterface 注入、多种 Dalamud 服务、GameInteropProvider HookFromSignature、FFXIVClientStructs（解析包 / 内存）
   - 示例源码：Service.cs 与 CombatEventCapture（Hook）
     https://github.com/Kouzukii/ffxiv-deathrecap/blob/658ec3a19614f225e354b207ebba87aaf64943c7/Service.cs
     https://github.com/Kouzukii/ffxiv-deathrecap/blob/658ec3a19614f225e354b207ebba87aaf64943c7/Events/CombatEventCapture.cs

6) MidiBard — MIDI 演奏与插件内部 IPC
   - 仓库：https://github.com/akira0245/MidiBard
   - 常用接口：通过注入获得的多项 Dalamud 服务（ClientState、ChatGui、DataManager、GameGui、KeyState 等），内部使用 IPC/本地通信实现合奏与控制
   - 示例源码：Midibard/DalamudApi 和 IPCHandlers
     https://github.com/akira0245/MidiBard/blob/976b78b801f1ad84ace1ab86f91c31fe9fe5e769/Midibard/DalamudApi/api.cs
     https://github.com/akira0245/MidiBard/blob/976b78b801f1ad84ace1ab86f91c31fe9fe5e769/Midibard/IPC/IPCHandles.cs

7) LMeter — ACT 集成叠加器
   - 仓库：https://github.com/lichie567/LMeter
   - 常用接口：IClientState、ICommandManager、ICondition、IDalamudPluginInterface、IDataManager、IFramework、IGameGui、IJobGauges、IObjectTable、IPartyList、ITargetManager 等；与 ACT 通过 WebSocket 或 IINACT IPC 连接
   - 示例源码：Plugin.cs（服务注册）
     https://github.com/lichie567/LMeter/blob/7921aeb703321be2feb28f147c3be2b7a9649c71/LMeter/Plugin.cs

8) BossMod — 深度集成、Hook 与 FFXIVClientStructs 应用
   - 仓库：https://github.com/awgil/ffxiv_bossmod
   - 常用接口：大量 FFXIVClientStructs 使用、ActionManager Hook、SigScanner 与 HookAddress、IClientState、IObjectTable、IFramework 等
   - 示例源码：Framework/Service.cs、Data/ClientState.cs
     https://github.com/awgil/ffxiv_bossmod/blob/162fde51b5e56ca133cbc13502ae03548a23f461/BossMod/Framework/Service.cs
     https://github.com/awgil/ffxiv_bossmod/blob/162fde51b5e56ca133cbc13502ae03548a23f461/BossMod/Data/ClientState.cs

9) AutoDuty — 导航/自动化插件（内存读写与 Hook）
   - 仓库：https://github.com/ffxivcode/AutoDuty
   - 常用接口：多项 Dalamud 服务、SigScanner、Hook、FFXIVClientStructs，用以执行移动/交互自动化
   - 示例源码：AutoDuty.cs
     https://github.com/ffxivcode/AutoDuty/blob/53f4e7400f9791f4534f803a2ec2d2ff709375ba/AutoDuty/AutoDuty.cs

10) VFXEditor — VFX / 动画 / 音效 编辑器
    - 仓库：https://github.com/0ceal0t/Dalamud-VFXEditor
    - 常用接口：IClientState、IFramework、ICommandManager、IObjectTable、ISigScanner、IDataManager、ITargetManager、IGameInteropProvider，以及 FFXIVClientStructs 的 VFX/动画 互操作
    - 示例源码：Dalamud 服务注册
      https://github.com/0ceal0t/Dalamud-VFXEditor/blob/d29a10b7ab9013d274cca7e9b7298e69678348fc/VFXEditor/Dalamud.cs

---

## 常用的 Dalamud / 游戏接口（汇总）

下面这些接口在多数插件中经常出现，建议你的插件按需注入并以此读取所需信息：

- IDalamudPluginInterface（插件生命周期、UiBuilder、GetIpcSubscriber / CallGate）
- IClientState（LocalPlayer、IsPvP、TerritoryType 等）
- IObjectTable（场景对象检索、LocalPlayer、SearchById 等）
- IFramework（RunOnTick、Update 事件 —— 每帧/定时读取）
- IDataManager / Lumina（GetExcelSheet<T>，读取静态游戏数据表）
- ICommandManager（注册 /slash 命令）
- IChatGui（发送/接收聊天、AddChatLinkHandler）
- IGameGui（GetAddonByName —— 读取游戏界面 Addon 指针）
- IKeyState（键盘状态）
- ISigScanner（签名扫描：定位游戏函数地址）
- IGameInteropProvider（HookFromSignature / HookFromAddress）
- IPluginLog（日志）
- ITextureProvider（图标/贴图查找）
- ICondition（ConditionFlag，例如 BoundByDuty / InCombat / WatchingCutscene）
- IPartyList / ITargetManager / IJobGauges / IToastGui / INotificationManager —— 常用但按需使用
- FFXIVClientStructs —— 当高层 API 不够时用于直接读取/写入游戏内存结构

建议优先使用高层 Dalamud 服务（IClientState、IObjectTable、IDataManager 等），仅在必要时使用 FFXIVClientStructs / Hook（因为直接内存读取或 Hook 风险更高且对游戏更新敏感）。

---

## 把游戏数据传回外部 App 的实现方式（推荐与权衡）

1) WebSocket（本地）—— 推荐用于实时双向通信。插件作为 WebSocket 客户端连接到你的本地服务端（或反向）。优点：实时、双向；缺点：需要本地端口与防火墙设置。
2) HTTP POST（推送）—— 插件向本地服务发 JSON POST。优点：实现简单、易调试；缺点：为单向，若需实时性需配合轮询或长连接。
3) TCP/UDP Socket —— 低层协议，UDP 适合丢包可容忍的遥测数据，TCP 保证可靠性。优点：高效；缺点：需自行实现协议、重连与分包处理。
4) 写文件（JSON/SQLite）—— 最简单，外部 App 定期轮询读取。优点：实现最容易，兼容性好；缺点：延迟高、文件锁与并发问题需考量。
5) Dalamud 插件内 IPC —— 只能在 Dalamud 插件间使用，不适合对接外部 App。

安全与合规提示：
- 强烈建议只使用 loopback（127.0.0.1）地址绑定，避免暴露到网络。不要上传敏感数据到第三方。 
- 尽量避免自动化玩家行为（例如模拟输入、自动操作）以降低封禁风险——读取和导出信息通常是可接受的，但直接替玩家下指令或注入包存在风险。

示例（C# + WebSocket 推送，伪代码）：

```csharp
// 插件内后台任务示例
var ws = new ClientWebSocket();
await ws.ConnectAsync(new Uri("ws://127.0.0.1:42000"), CancellationToken.None);
while (ws.State == WebSocketState.Open) {
    var payload = new {
        time = DateTime.UtcNow,
        player = clientState.LocalPlayer?.Name?.TextValue,
        hp = clientState.LocalPlayer?.CurrentHp
    };
    var bytes = Encoding.UTF8.GetBytes(JsonConvert.SerializeObject(payload));
    await ws.SendAsync(bytes, WebSocketMessageType.Text, true, CancellationToken.None);
    await Task.Delay(250); // 每 250ms 发送一次
}
```

---

## 后续步骤（我会如何推进 Top-100 的完整映射）

- 生成一个优先排序的 Top-100 插件候选列表（按匹配度 / star / 最近提交活跃度排序），并提交为索引文件（CSV/JSON）到 docs/ 下。
- 对这 100 个仓库逐一运行静态搜索与抽取：定位服务注入点（Plugin 构造/PluginService）、FFXIVClientStructs 调用、SigScanner/Hook 调用、以及任何网络/文件/IPC 导出实现。
- 为每个确认为 Dalamud 插件的仓库在文档中增加小节，包含：
  - 仓库链接 + 简短说明
  - 调用的主要 Dalamud / 游戏接口清单（按服务名列出）
  - 关键调用或导出实现的源码 permalink（1–3 个精确行范围）
  - 是否对外通信（HTTP/WS/TCP/File/IPC）及位置
- 我会按每 10–20 个仓库分批推送到 api 分支，方便你审阅；最终把 Top-100 的完整初稿合并成 docs/dalamud_plugins_api_mapping_v1.md（中文）。

预计时间：Complete Top-100 初稿约 6–12 小时（会分批提交，先提交前 20 个结果）。

---

现在我继续对 Top-100 候选做索引与代码检索，并且所有后续文档都会以中文写出并提交到 api 分支。有任何优先仓库或额外格式（例如要 Excel/CSV 输出）请告诉我，我会一并输出。
