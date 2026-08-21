# Dalamud 原生接口（导出与能力说明）

> 文件：docs/dalamud_native_interfaces.md（api 分支）
>
> 说明：本文件把 goatcorp/Dalamud 仓库中常用且对插件开发最有价值的“原生接口（public interface）”汇总为索引，并延展说明这些接口能做什么、典型用例与风险提示。检索基于仓库源码，本次导出覆盖核心接口并附上源码链接；由于代码搜索分页/返回限制，若需“仓库中所有 interface 的逐一列出（逐文件完全导出）”，我可以再做一次分页抓取并生成完整清单。

---

## 重要说明（必读）

- 我已从官方 Dalamud 仓库抓取并整理出核心接口索引与链接。搜索工具在单次响应中可能未返回仓库中全部的 interface 文件——因此本列表覆盖“核心与常用”接口，但不是绝对穷尽。若你要我把仓库中的所有 `public interface` 文件逐一导出为完整文档，请回复我将继续分页抓取并合并成单一文件。
- 你可以在 GitHub UI 上查看更多结果或手动检索：
  https://github.com/goatcorp/Dalamud/search?q=interface&type=code


---

## 一、已整理的核心接口索引（按用途分组，含源码链接）

A) 插件层 / 生命周期 / 主门面

- IDalamudPluginInterface — 插件入口与服务提供主接口，包含 UiBuilder、配置管理、IPC provider/subscriber、Create/Inject、版本信息等
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/IDalamudPluginInterface.cs

- IDalamudPlugin — 插件需实现的基础接口（Dispose）
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/IDalamudPlugin.cs

- IDalamudVersionInfo — Dalamud 版本/SCM 信息
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/VersionInfo/IDalamudVersionInfo.cs


B) 客户端 / 玩家 / 对象 / 状态

- IClientState — 客户端运行时状态、事件（登录/登出/地图/区服/是否 PvP/GPose 等）
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/Services/IClientState.cs

- IPlayerState — 本地玩家概览（名字、ContentId、等级、World、属性、职业等级等）
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/Services/IPlayerState.cs

- IObjectTable — 场景对象表（枚举对象、SearchById/SearchByEntityId、LocalPlayer 等）
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/Services/IObjectTable.cs

- IGameObject / ICharacter / INpc / IPlayerCharacter 等（对象子接口分散在 Game/ClientState/Objects 目录）
  - 例如 INpc： https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Game/ClientState/Objects/SubKinds/Npc.cs


C) 服务与实用工具（Services / SigScanner / Hook）

- ISigScanner — 签名扫描服务（查找函数/静态地址）
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/Services/ISigScanner.cs

- IDalamudHook — Hook 抽象描述（Address / IsEnabled / Dispose）
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Hooking/IDalamudHook.cs

- 其它工具/实用接口（如 IDebouncer、Texture 管理相关接口等）分布在 Utility / Interface 子目录，可按需导出。


D) 插件间通信（IPC）

- ICallGateSubscriber / ICallGateProvider（多泛型版本） — Dalamud 插件间 CallGate API，用于暴露或订阅函数/动作
  - 源码： https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Plugin/Ipc/ICallGateSubscriber.cs


E) UI / ImGui / 窗口 / 纹理 / 字体

- IUiBuilder（通过 IDalamudPluginInterface.UiBuilder 暴露） — 插件绘制 ImGui 的生命周期钩子与事件（见 IDalamudPluginInterface）
- IWindow / IWindowSystem — 内置窗口系统与窗口抽象
  - IWindow: https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Interface/Windowing/IWindow.cs
  - IWindowSystem: https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Interface/Windowing/IWindowSystem.cs

- IBitmapCodecInfo / 纹理/贴图相关接口
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Interface/Textures/IBitmapCodecInfo.cs

- IDragDropManager（外部拖放支持）
  - 源码：https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Interface/DragDrop/IDragDropManager.cs


F) 游戏网络结构与市场板相关

- IMarketBoardCurrentOfferings / IMarketBoardPurchase / IMarketBoardHistory — 市场板数据结构接口
  - CurrentOfferings: https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Game/Network/Structures/IMarketBoardCurrentOfferings.cs
  - Purchase: https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Game/Network/Structures/IMarketBoardPurchase.cs
  - History: https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Game/Network/Structures/IMarketBoardHistory.cs


G) 其他（字体、颜色工具、内部追溯接口等）

- IFontSpec 等字体接口： https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Interface/FontIdentifier/IFontSpec.cs
- Texture 管理内部接口（示例）： https://github.com/goatcorp/Dalamud/blob/83042016d0e9996dc44c9f7fd96a8d33a5e586f2/Dalamud/Interface/Textures/Internal/TextureManager.BlameTracker.cs


---

## 二、延展说明：这个 API 到底可以干什么（概述与典型用例）

下面把 Dalamud/FFXIV 插件 API 的能力按“可做/不该做/风险/典型实现”做个实用说明，帮助你决定如何用这些接口来实现目标。

1) 读取并监控玩家/世界状态（安全、广泛使用）
- 能力：通过 IClientState / IPlayerState / IObjectTable 读取 LocalPlayer 信息（名字、ContentId、HP/MP/位置通过对象表）、队伍成员列表、地图/区服/实例信息、是否处于战斗、GPose 等。
- 典型用例：GUI 显示玩家信息、远程统计、HUD 增强、战斗提示器。
- 限制：LocalPlayer 可能为 null，需检查登录/加载状态。

2) 读取/解析游戏 UI（Addon）文本与节点（有侵入风险但常被使用）
- 能力：通过 IGameGui.GetAddonByName 拿到 Addon 地址，配合 FFXIVClientStructs 的 AtkTextNode 读取对话文本、战斗提示、Hud 文本等。
- 典型用例：对话文本抓取（自动翻译/文本转语音）、读取战斗 UI 文本做自定义提示。
- 风险：Addon 内部结构随客户端版本变化，需小心偏移与长度限制；优先只读。

3) 监听/拦截客户端函数（高权限/高风险）
- 能力：使用 ISigScanner 查找函数地址，然后用 IGameInteropProvider/IDalamudHook 创建 Hook 来拦截（例如 ActionManager.UseAction、包处理入口）。
- 典型用例：捕获施法/技能使用事件、实现高级 combat breakdown、捕获 VFX 触发时机。
- 风险：签名/Hook 对版本敏感且可能触发反作弊检测——只做监听、避免写回或模拟输入。

4) 访问静态游戏数据（Lumina / IDataManager）
- 能力：使用 IDataManager.GetExcelSheet<T>() 把动作/物品/地图/职业名称从 id 转为文本或图标 id。
- 典型用例：显示技能名、图标、解析动作 id 以友好显示。

5) 构建插件 UI / 覆盖 / ImGui 窗口
- 能力：通过 IUiBuilder、IWindowSystem、ITextureProvider 等创建 ImGui 窗口、绘制图标、渲染实时 HUD。
- 典型用例：插件配置窗口、实时 overlay、物品快速查看、截图工具。

6) 插件间集成（CallGate / IPC）
- 能力：通过 ICallGateProvider/Subscriber 暴露或调用其他插件提供的函数/服务（本地插件间 RPC）。
- 典型用例：Brio 暴露 API 让其他插件订阅 GPose 数据、MidiBard 插件间合奏协调。
- 限制：仅在 Dalamud 插件生态内可用，不能直接对外的 App 使用（需插件作网桥）。

7) 对外通信（由插件实现）
- 能力本身并不提供网络通信模块，但插件可以借助 .NET 网络库（ClientWebSocket/HttpClient/TcpClient）将收集到的信息发送到本地服务（强烈建议绑定 loopback）。
- 典型用例：把玩家坐标/HP/目标信息实时推送到本地 overlay application、TTS、语音助手或直播工具。

8) 配置/安装/更新管理
- 能力：IDalamudPluginInterface 提供保存配置、打开插件安装器/设置窗口、查询更新等功能，方便插件配置与自动更新整合。

9) 市场板与网络结构访问
- 能力：接口如 IMarketBoardCurrentOfferings 能暴露市场板查询结果（但通常由游戏内网络消息驱动），适合做市场工具或插件内价格显示。


---

## 三、风险与合规建议（务必阅读）

- 优先使用高层服务（IClientState、IObjectTable、IDataManager）——这是最安全且兼容性最好的方式。仅在确实需要更深层数据时再使用签名/Hook/FFXIVClientStructs。 
- Hook/签名/直接内存写入存在封禁风险，且对游戏更新脆弱；若使用请清晰记录并在插件配置中让用户确认并承担风险。
- 始终把对外通信限制为 loopback（127.0.0.1），不要把敏感或可识别的个人数据上传到第三方主机。


---

## 四、我接下来可以为你做的事（选项）

1) 把仓库中“所有 public interface”逐一分页抓取并把完整签名导出为 docs/dalamud_all_interfaces.md（包括每个方法/属性的源码行号 permalink）。回复“导出全部接口”。

2) 现在把上面文档转为 CSV / JSON 以便机器处理（例如供自动化脚本使用）。回复“导出为 CSV”。

3) 进一步为每个核心接口生成“最小可运行示例代码片段”（C#），放到 docs/snippets/ 下。回复“生成示例”。


---

已将本摘要写入 docs/dalamud_native_interfaces.md（api 分支）。如果你同意我继续把仓库中全部接口（穷尽）导出，请回复“导出全部接口”，我会开始分页抓取并把最终文档提交到 api 分支；若只需部分类别，请说明类别。
