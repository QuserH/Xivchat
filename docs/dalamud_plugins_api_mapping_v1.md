
## 第一批详表：前 20 个候选（已初步抽取）

下面是对 index_top20.csv 中前 20 个候选仓库的初步逐仓验证与接口调用点摘录（中文）。我已尽量从源码中摘取明确的调用点 permalink；未提供 permalink 的条目我会在下一批检索中补齐。每个条目包含：仓库、简要说明、已发现的主要 Dalamud/游戏接口（或低层结构）、以及示例源码链接（若有）。

---

1) goatcorp/Dalamud (框架库)
   - 仓库：https://github.com/goatcorp/Dalamud
   - 说明：Dalamud 框架本身，插件通过其注入服务访问游戏。
   - 关键接口：IDalamudPluginInterface、ISigScanner 等（在库内定义）。
   - 示例源码（接口定义）：
     https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/IDalamudPluginInterface.cs#L1-L106
     https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/Services/ISigScanner.cs#L1-L90

2) buttplugio/awesome-buttplug
   - 仓库：https://github.com/buttplugio/awesome-buttplug
   - 说明：与 FFXIV/Dalamud 无直接关系（检索结果里被 star 排高），标注为非 Dalamud 插件候选。

3) superiorlu/AITreasureBox
   - 仓库：https://github.com/superiorlu/AITreasureBox
   - 说明：非 Dalamud 插件集合（与本次目标关联度低），暂不深入。

4) karashiiro/xiv-resources
   - 仓库：https://github.com/karashiiro/xiv-resources
   - 说明：FFXIV 资源/工具索引集合（参考目录），可用于找更多插件或外围工具。

5) gamous/DalamudPluginsCN-Dev
   - 仓库：https://github.com/gamous/DalamudPluginsCN-Dev
   - 说明：中文社区的 Dalamud 插件集合/示例仓库，可能包含多个插件源码或发布清单。

6) NightlyRevenger/TataruHelper
   - 仓库：https://github.com/NightlyRevenger/TataruHelper
   - 说明：用来读取游戏文本 / 直接内存读取字幕/对话的工具（使用 Sharlayan + FFXIVClientStructs）。适合参考“内存读取字幕/聊天”实现。
   - 已发现/使用的低层：FFXIVClientStructs（Sharlayan），直接内存读取、TalkAddon 实时读取。
   - 示例源码（读取字幕/内存相关）：
     https://github.com/NightlyRevenger/TataruHelper/blob/a4def730c9f51ae2a32aefef615a75d8e69d0e30/TataruHelper/Services/GameMemory/TalkAddonRealtimeReader.cs#L1491-L1559
     https://github.com/NightlyRevenger/TataruHelper/blob/a4def730c9f51ae2a32aefef615a75d8e69d0e30/TataruHelper/Services/GameMemory/SharlayanGameMemoryGateway.cs#L26-L111

7) aers/FFXIVClientStructs
   - 仓库：https://github.com/aers/FFXIVClientStructs
   - 说明：反向工程出的游戏内结构集合（许多插件直接使用）。这是低层结构定义库，插件通过它读取/解析游戏内存结构。
   - 示例源码（项目片段）：
     https://github.com/aers/FFXIVClientStructs/blob/8c9ef2876f2d50190bba094b875add984ea88f55/FFXIVClientStructs/ThisAssembly.cs#L1-L4
     （库还包含大量结构定义与生成器，可用于构造内存读取/写入逻辑）

8) awgil/ffxiv_bossmod
   - 仓库：https://github.com/awgil/ffxiv_bossmod
   - 说明：高复杂度的 BossMod，广泛使用 FFXIVClientStructs、Hook、SigScanner 与直接内存访问 —— 适合参考战斗事件、VFX、Actor/Combatant 数据提取方式。
   - 常见接口/技术：FFXIVClientStructs、ActionManager Hook、SigScanner、ObjectTable、Framework。
   - 示例源码（VFX / 插件启动 / 内存工具）：
     https://github.com/awgil/ffxiv_bossmod/blob/162fde51b5e56ca133cbc13502ae03548a23f461/BossMod/Debug/DebugVfx.cs#L1-L62
     https://github.com/awgil/ffxiv_bossmod/blob/162fde51b5e56ca133cbc13502ae03548a23f461/BossMod/Framework/Plugin.cs#L1-L29
     https://github.com/awgil/ffxiv_bossmod/blob/162fde51b5e56ca133cbc13502ae03548a23f461/BossMod/Network/IDScramble.cs#L1-L19

9) marzent/IINACT
   - 仓库：https://github.com/marzent/IINACT
   - 说明：包含 OverlayPlugin 移植/集成，读取内存并暴露事件给 overlay。示例中有基于 FFXIVClientStructs 的内存解析与 packet dispatcher 封装。
   - 已发现技术点：AtkStage/Addon 读取、Party 内存结构、PacketDispatcher（网络包相关）
   - 示例源码：
     https://github.com/marzent/IINACT/blob/c9d51b295fe694805237b2b8ad0c99ce4cc67f1e/OverlayPlugin.Core/EventSources/FFXIVClientStructsEventSource.cs#L1-L101
     https://github.com/marzent/IINACT/blob/c9d51b295fe694805237b2b8ad0c99ce4cc67f1e/IINACT/Network/PacketDispatcher.cs#L1-L33

10) LeonBlade/BDTHPlugin
    - 仓库：https://github.com/LeonBlade/BDTHPlugin
    - 说明：与房屋摆放有关的插件；代码常用 Dalamud 服务（IGameGui、IPluginLog 等）。我会在下一步抓取具体调用点并补 permalink。

11) ktisis-tools/Ktisis
    - 仓库：https://github.com/ktisis-tools/Ktisis
    - 说明：截图/GPose / posing 工具，通常使用 IClientState、IDataManager、IGameGui、IObjectTable 等以读取角色/相机状态。

12) ffxivcode/AutoDuty
    - 仓库：https://github.com/ffxivcode/AutoDuty
    - 说明：自动化/导航插件，示例展示了广泛使用 FFXIVClientStructs、SigScanner、Agent/UI 操作、TaskManager 与 Dalamud 服务（PluginInterface、IFramework 等）。
    - 示例源码（多处）:
      https://github.com/ffxivcode/AutoDuty/blob/53f4e7400f9791f4534f803a2ec2d2ff709375ba/AutoDuty/AutoDuty.cs#L1-L81
      https://github.com/ffxivcode/AutoDuty/blob/53f4e7400f9791f4534f803a2ec2d2ff709375ba/AutoDuty/Helpers/InputHelper.cs#L1-L15
      https://github.com/ffxivcode/AutoDuty/blob/53f4e7400f9791f4534f803a2ec2d2ff709375ba/AutoDuty/Helpers/PronounHelper.cs#L1-L71

13) jayotterbein/FFXIV-Zoom-Hack
    - 仓库：https://github.com/jayotterbein/FFXIV-Zoom-Hack
    - 说明：相机/镜头修改类插件，使用较低层 Hook/签名扫描技术以修改摄像机行为（风险较高）。

14) FFXIV-CombatReborn/RotationSolverReborn
    - 仓库：https://github.com/FFXIV-CombatReborn/RotationSolverReborn
    - 说明：战斗帧级数据与动作选择逻辑，通常读取战斗状态（ClientState/ObjectTable/ActionManager）并分析行为。

15) KazWolfe/XIVDeck
    - 仓库：https://github.com/KazWolfe/XIVDeck
    - 说明：Stream Deck 集成插件，示例中会读取 IClientState、IObjectTable、IChatGui 等以驱动外部硬件/按钮。

16) jawslouis/MakePlacePlugin
    - 仓库：https://github.com/jawslouis/MakePlacePlugin
    - 说明：制作房屋摆放辅助插件，典型接口：IGameGui、IObjectTable、IClientState。

17) lokinmodar/Echoglossian
    - 仓库：https://github.com/lokinmodar/Echoglossian
    - 说明：对话翻译工具，依赖内存/Addon 读取或 Sharlayan/FFXIVClientStructs 实现字幕/对话抓取（可参考 TataruHelper 的实现）。

18) NightmareXIV/AntiAfkKick
    - 仓库：https://github.com/NightmareXIV/AntiAfkKick
    - 说明：防挂机踢插件，通常读写输入/键盘状态或直接模拟轻量活动；请注意合规/封禁风险。

19) Etheirys/Brio
    - 仓库：https://github.com/Etheirys/Brio
    - 说明：GPose/姿势工具，且提供插件 IPC（BrioAPI）供其他插件调用。建议参考其 IPC 实现来了解插件间数据共享；也大量使用 IFramework、IDataManager、IObjectTable 等。
    - 示例源码：
      https://github.com/Etheirys/Brio/blob/76254a165e839fc97e4edae065905d7c3976652e/Brio/Game/Core/DalamudService.cs
      https://github.com/Etheirys/Brio/blob/76254a165e839fc97e4edae065905d7c3976652e/BrioAPI_V2.cs

20) Ottermandias/SeaOfStars
    - 仓库：https://github.com/Ottermandias/SeaOfStars
    - 说明：FFXIV Modding/posing 套件（JS 工具链），与纯 Dalamud 插件不同，但在 Mod/提取资产方面有参考价值。

---

说明与下一步
- 上述为“第一批（前 20）”的初步逐仓摘要。对于第 6、8、9、12、19 等仓库我已抓取并记录了源码片段与 permalink（见上文）。
- 我接下来会把每个条目扩充：精确列出该仓库内使用到的具体 Dalamud 接口（类名/属性/方法）、以及“从游戏读取到哪些数据（玩家名/位置/HP/队伍信息/动作/包）”和“是否有对外通信实现（HTTP/WS/TCP/文件/IPC）”并把 1–3 个关键代码行范围的 permalink 写进每个条目。该工作我会分批（每批 10–20）提交到 api 分支。
- 我将在约 1–3 小时内把本批（20 个）的完整条目（每个包含 1–3 个 permalink + 简短解释）提交到 docs，并把提交链接贴给你。随后继续处理 21–100。

如果没问题，我继续按此计划处理并很快提交首批详表。