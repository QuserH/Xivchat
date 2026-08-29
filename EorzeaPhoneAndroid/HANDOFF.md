# 交接文档 — 艾欧泽亚终端（EorzeaPhone Android）

写于 2026-08-29，对应 **0.7.241 / versionCode 263**（commit `ac465df`）。

> **先读这三节再动手**：[多会话共享工作目录](#0-警告多会话共享同一个工作目录)、
> [踩过的坑](#5-踩过的坑必读)、[未验证清单](#8-未验证清单诚实交代)。
> 前者会让你和别人互相覆盖，后两者能省掉我已经犯过的错。

---

## 0. 警告：多会话共享同一个工作目录

这个仓库同时被**多个 Claude Code 会话**操作过。接手时先确认还有没有别人在写：

```bash
git status --short                    # 有没有你不认识的未提交改动
ls -la --time-style=+%H:%M:%S app/src/main/java/com/quserh/eorzeaphone/ui/
```

`assembleDebug` 编译的是**整个 module**，会把别人写一半的代码打进你的包。
0.7.239 ~ 0.7.241 三个包里都含着另一个会话未完成的 wiki / 任务树 / 采集时钟。

**已约定的协议**（继续遵守）：

1. **出包前先问一句**"我要出包了，你手上的能编译吗"，等回复。对方没做完就等。
2. **版本号分段**，别撞。我用到 279 / 0.7.257，下一个从 280 起。改前 `git pull`。
3. **提交只 stage 自己的文件**，显式列路径，**绝不 `git add -A`**。
4. 对方长时间不回（我等过 4.5 小时 + 问两次）可以出包，但要在 commit 里
   写明包含了谁的未完成代码，且**不提交对方的文件**。
5. **⚠️ 显式列路径不等于只提交了自己的改动。** `git add <路径>` 提交的是
   那个文件**当前磁盘上的全部内容**，包括对方刚写进同一个文件的部分。
   0.7.257 就是这么把对方那一轮（幻化职业筛 + 热门排序）整批带进了我的
   提交，commit message 里一句没提 —— 557 行的 `ShizhijiaScreens.kt` 里
   我自己只写了一小半。

   **所以 stage 之后、commit 之前，必须 `git diff --cached` 逐个文件看一眼。**
   看到不是自己写的东西：要么撤出来（`git restore --staged`），要么在
   commit message 里写明"本次含另一个会话的 X"。文件级归属在两个会话
   同时写同一个文件时是失效的，只有看 diff 才靠得住。

**文件归属**（截至交接时）：

**文件归属是按"谁在设计这一块"分的，不是排他锁** —— 见上面第 5 条。
2026-08-29 之后两个会话都在改 `ShizhijiaScreens.kt`（一个做 UI 排版、
一个做幻化流的功能），所以这张表只说明**谁负责哪块的判断**，
不代表另一方不会动这个文件。

| 谁的 | 文件 |
|---|---|
| 我（UI 设计 + 石之家/壳层排版） | `ShizhijiaScreens.kt` 的排版部分、`Szj*` 视觉原语、`SubScreens.kt`、`ui/theme/*`、`AetherphoneParityScreens.kt`、`AppStore*`、`WikiScreens.kt`/`WikiLinkScreens.kt` 的排版 |
| 另一个会话（功能实现 + 数据层） | `data/wiki/`、`assets/wiki/`、`QuestTreeScreen.kt`、`GatherClockScreen.kt`、`GlamourPickerScreens.kt`、`ShizhijiaScreens.kt` 的幻化流功能、`ShizhijiaApi.kt` 的接口实测 |
| **共用，动前必须问** | `AppCatalog.kt`、`EorzeaPhoneApp.kt`（加应用格子 / 加路由都在这两个文件） |

分工是用户定的：我做 UI 设计并调度，另一个会话做功能实现。
视觉规格（列数、尺寸、用哪个卡片原语、选中态、空态文案）由我给，
它不自己定配色和排版；数据映射和接口实测归它。

`XIVChatPlugin/Resources/lib/xivchat_native_tools.dll` 那个删除**一直挂在
`git status` 里，别提交**。根因查清了（0.7.256）：**Windows Defender 在隔离它**，
报 `Trojan:Win32/Cobaltstrike.MKRT!MTB`，8/19 起吃了 6 次，每个出现过它的
位置都清过。不落盘做过 PE 分析（`git cat-file blob … | python`），
**证据指向误报**：导出只有 `wrap`/`wrap_free`/`rust_eh_personality`，
导入只有 KERNEL32 + VCRUNTIME140 + CRT —— 没有 WinSock、没有 WinHTTP、
没有 `VirtualAllocEx`/`WriteProcessMemory`/`CreateRemoteThread`、
没有注册表和服务 API。一个不能出网的二进制做不了 C2 beacon。
它是 Rust 写的文字折行库（带 `unicode-linebreak` 字样），
唯一调用点是 `Server.cs:2567`（发游戏内长消息时切成几条）。
要用它得给 Defender 加窄路径排除 —— **那是用户的安全决定，不替他做**。
git 里的 blob 完好，随时能取回。

---

## 1. 这是什么

Android Compose 应用，把 FF14 国服的一台"虚构手机"做出来：桌面 + 一堆内置
小应用 + **石之家（Rising Stones）客户端**。石之家那部分是主体，
`ShizhijiaScreens.kt` 一个文件 7500+ 行。

- 分支 `feature/aetherphone-shell`，远端 `QuserH/Xivchat`
- 目录 `E:\Game tools\Final Fantasy XIV plugin\Pliungs\XIVChat-source\EorzeaPhoneAndroid`
- compileSdk 36 / minSdk 24 / Compose BOM 2024.12.01（Compose 1.7.6）
- 用户是国服玩家，GitHub 账号 QuserH。**回复和文档都用中文。**

结构上分两层，理解这个区分很重要：

- **壳层**：桌面、设置、联系人、各个小应用。token 前缀 `Phone*`
- **石之家模块**：`ShizhijiaScreens.kt` 等。token 前缀 `Szj*`

两层的设计体系本来是脱节的（石之家做得很完整，壳层是 Material3 裸用），
0.7.241 这一批就是在补壳层。见 [第 4 节](#4-已完成的工作)。

---

## 2. 构建与发版

**没有 Gradle wrapper**，用仓库里的 `szj_build.sh`（它负责 `JAVA_HOME`、
AF_UNIX 补丁、把 `TMP`/`TEMP` 指到本地 `.buildtmp`）：

```bash
bash szj_build.sh :app:compileDebugKotlin     # 只编译，快
bash szj_build.sh :app:assembleDebug          # 出包
```

输出里可能混二进制导致 `grep` 报 "Binary file matches"，这样读：

```bash
bash szj_build.sh :app:assembleDebug > /tmp/b.txt 2>&1; echo "exit=$?"
tr -d '\000' < /tmp/b.txt | grep -E "^e:|BUILD" | head
```

**出包后必须用 aapt2 核对版本号**（我每次都核，别省）：

```bash
"$(ls -d /c/Users/Administrator/AppData/Local/Android/Sdk/build-tools/* | tail -1)/aapt2.exe" \
  dump badging app/build/outputs/apk/debug/app-debug.apk | head -1
```

APK 复制到 `E:/Game tools/Final Fantasy XIV plugin/Pliungs/艾欧泽亚终端包存储/`，
命名 `艾欧泽亚终端-<版本>.apk`。**别覆盖旧文件名**，用户靠这些文件回滚。

**本机网络怪癖**（都验证过）：

- `git push` 必须 `git -c http.sslBackend=openssl push`，否则失败
- `curl` 正常可用；**Python 的 urllib SSL 会失败**，要抓网络用 curl
- 没有 `gh` CLI
- Windows 原生 Python 看不到 Git Bash 的 `/tmp`，要 `cygpath -w /tmp`
  （实际是 `C:/Users/Administrator/AppData/Local/Temp`）

---

## 3. 安全约束（不可违反）

用户授权过用**他自己的 cookie** 抓石之家，但原话是：

> 你自己抓，此外我的 cookie 你别放进 app 里面就行

**cookie 绝不能进 app、进仓库、进对话。** 至今没有任何凭证值出现在任何源文件
或提交里，保持这样。

需要验证登录后才能读的接口时用 `docs/szj/verify_authed.sh`：

```bash
bash docs/szj/verify_authed.sh /c/Users/Administrator/szj_cookie.txt
```

那个 cookie 文件放**仓库外面**，脚本从文件读、全程不打印内容、只读不写。
JSON 落在 `/tmp/szj_authed/`（含账号数据，看完 `rm -rf`）。

**这个脚本还没跑过** —— 用户说了"cookie 拿我的试"但还没给。见
[第 8 节](#8-未验证清单诚实交代)。

---

## 4. 已完成的工作

### 0.7.255 ~ 0.7.257 这三版（含另一个会话的部分，补记归属）

| 版本 | 谁做的 | 内容 |
|---|---|---|
| 0.7.255 `481815d` | 我 | wiki 模块整批入库；wiki 排版重做（详情页分区顺序、名字重复、品级成列、首页两格）；`ScreenHeader` 加 `titleColor`；`.gitattributes` 补二进制豁免（顺带修好已损坏的 `wallpaper_dusk_dark.jpg`） |
| 0.7.255 | 另一个会话 | 装备选择器 `GlamourPickerScreens.kt`；楼中楼子评论；染剂 125 色调色板 |
| 0.7.256 `c7171e2` | 我 | 染色孔位画反（`dyes` 改成按孔位定长）；分享图补空孔；「无染色」→「无」 |
| 0.7.257 `02068b8` | 我 | 装备选择器提到屏幕层（连带修好"搜索看不见结果"）；分享图右栏不再裁掉配饰、改按类别分段；发幻化补封面图/细节图/分享到动态开关 |
| 0.7.257 `02068b8` | **另一个会话** | **幻化流：客户端职业筛（两级，`jobIds` + `universalJob`）、补「热门」排序（`order=hottest`）、修 `sort=1` 发假值 `"time"`、修筛选面板重复拉取。这一批被我的提交带进去了，commit message 里没写，在此补记。** |

**为什么会漏**：我 stage 时显式列了路径（符合协议第 3 条），但没有
`git diff --cached` 逐个看。`ShizhijiaScreens.kt` 那 557 行里我只写了一小半。
协议已补第 5 条。

另一个会话在 0.7.257 那轮实测出来的三条结论（影响文案，写 UI 时用得上）：

- **`job_ids` 空 = 没主手 = 通用款**。null 的 5 篇详情里 MAIN_HAND 全空，
  窄表的 4 篇全都有主手。但**42 项全勾的那种不能反推成没主手** ——
  那是 2018-19 老帖手动全勾的，取样 4 篇里 2 篇其实有主手，
  42 项只能当"作者声明谁都能穿"。
- **通用款占比按流差很多**：最新 71%、推荐 60%、热门 23%。所以职业筛
  **默认必须带上通用款**，否则最新流一筛几乎全空。勾了「只看专属」之后
  最新流 22 个职业里 9 个凑不出一条，热门流只有 2 个。
- **筛空了要主动翻页**，否则列表空着滚不动、分页永远不触发。
  套路和 `ShizhijiaScreens.kt` 2200 行附近（推荐流"这一页都被你屏蔽了"）
  一样，但**加了 5 轮上限** —— 那边不封顶没事，这边冷门职业会一路翻到尽头。

### 石之家：收藏页四类 + 幻化收藏修复

收藏页原来一直显示"没读取到 / Type不正确"。根因是 `userInfo/myStarPosts`
的 **`type` 是必填的**（1=帖子 / 2=攻略），我只传了 page/limit。
那句"Type不正确"是服务端原话，`SzjResState` 照实转出来的。

顺着官网 `MeCollections` 读下来发现收藏是**四类各走各的接口**：

| 标签 | 接口 | 参数 |
|---|---|---|
| 帖子 / 攻略 | `userInfo/myStarPosts` | `{type:1\|2, page, limit}` |
| RP | `recruit/homePageStarRecruitRp` | **无参数、无分页** |
| 幻化 | `glamour/myFavoriteItemsList` | `{favorite_id, page, limit}` |

**幻化收藏是两层**：收藏必须落在某个收藏夹里，先 `myFavoritesList` 拿夹子
再拿内容。我之前只把第一层当"收藏时选夹子"用过，第二层压根没写。

**幻化点不开的根因**：收藏行和幻化列表**不同构**。行里的 `id` 是收藏记录的
id，幻化 id 叫 **`glamour_id`**（官网详情链接就是 `/glamour/detail/{glamour_id}`）。
卡片只用 `title` + `main_image` 所以画得出来，点进去查的是不存在的幻化。
收藏行还**没有** likes / favorites / character_name / area_name / group_name，
以及带一个 `is_valid`（0 = 原作已删，官网不让点、标"已失效"）。
专用解析在 `ShizhijiaGlamourCard.fromFavoriteJson()`。

### 石之家：写操作

`likePost` / `starPost` / `commentPost` / `likeGlamour` / `favoriteGlamour` /
`cancelFavoriteGlamour` / `respondRecruit`。**点赞和收藏都是"切换"不是幂等 set**，
同一个接口再打一次就取消。POST body 是 **form-urlencoded**，且 body 里
**也要**一个 `tempsuid`（和 query 里的是两个不同 uuid）。

端点表见 `app/src/main/java/com/quserh/eorzeaphone/data/shizhijia/API_WRITE_ENDPOINTS.md`，
每条都注明了出处。**这七个写操作一次都没真正跑过**（需要登录态）。

### 主题色系统（`ui/theme/AccentPalette.kt`）

九套预设 + 自定义，设置里「外观 → 主题色」可选。**一套主题色是四个角色**，
不是一个颜色：

| 角色 | 用途 |
|---|---|
| `fill` | 实心面（按钮底、选中态），上面配白字 |
| `inkLight` / `inkDark` | 文字和图标，按明暗模式取，保证 ≥4.5:1 |
| `bubble` | 聊天气泡底，**必须深** |

**为什么 bubble 必须单独一支**：游戏自己的频道文字色落在气泡上，我们改不了。
情感动作色 `#BEFFF1` 落在石之家金 `#c4a86a` 上只有 **2.05:1**，等于看不见
（0.7.235 的真 bug）。每套预设的 bubble 都拿这个色算过，除以太紫（3.53，
历史原值）外都在 8:1 以上。`fromSeed()` 给任何种子色算出的 bubble 都是 5.21:1。

**默认必须是 `crystal_teal`，不能写 `presets.first()`**。石之家金排第一（那是
站点品牌色，官网 CSS 里 177 次），但**金和紫是用户明确说过不喜欢的两支**。
默认值是"还没选之前给什么"，不能给被否过的颜色。0.7.236 我写成
`presets.first()` 发了金色出去，0.7.239 改回来的。

### 壳层设计原语（0.7.241 这一批的主体）

**根因**：石之家有完整 token 体系 + `SzjCardSurface`，壳层**什么都没有** ——
数过 **148 处**就地 `clip(RoundedCornerShape(x)).background(y)`，零个卡片原语，
M3 组件裸用。这就是"石之家里像设计过、出去像默认长相"的来源。

补的东西：

- `ui/theme/Theme.kt`：`PhoneLine`（分割线）/ `PhoneHairline`（描边）/
  `PhoneEdge`（卡片顶边高光）/ `phoneLight`。取值和石之家同源
- `SubScreens.kt`：`PhoneCard`（构造照 `SzjCardSurface`：阴影撑厚度 → 顶边
  1dp 高光 → 浅色模式才加收边 → 按下下沉）、`PhoneButton`
  （`PhoneButtonKind` 三档 × `PhoneButtonSize` 两档）、
  `PhoneCardShape` 14dp / `PhoneInnerShape` 10dp
- `PhoneButtonSize.Wide` 的语义是"**填满给它的宽度**"而不是硬 `fillMaxWidth`，
  靠内层撑，所以既能单独占整宽、也能在 Row 里传 `weight` 各占一份

换过去的地方：`SettingsGroup`（**改一处带动 13 个分组**）、`SettingsDivider`、
连接/断开按钮、筛选面板的重置/应用、整个 `AppStoreScreen`。

`PhoneChipShape` **早就存在**（10dp，在筛选面板那节），别再定义一个 —— 我撞过。

### App Store 重做

原来是纯列表（图标 + 名字 + 一个 M3 Button），是个设置项不是商店。

**顶部刻意不做"编辑推荐/今日精选"**：那是照抄真商店的形，但这台手机上没有
"推荐"这回事，编出来只能是假内容。放的是**桌面实况地图** —— 两页各占多少格、
每格用应用图标自己的颜色、**下一个装的会落在哪一页**。最后这条是真信息：
`installApp()` 挑格子最少的那页，这规则以前藏在代码里。

**13 个应用如实标"占位"**（只有图标没界面）。这不是估的，是逐条对
`EorzeaPhoneApp.kt` 的路由表得出的：有分支的有界面，落到 `GenericAppScreen`
的没有。文案表在 `AppStoreCatalog.kt`，**单独一个文件不并进 `AppCatalog.kt`**
（后者是"桌面有哪些格子"、别人在维护）。

`GenericAppScreen` 原来说"应用已打开，等待游戏数据"—— **那是假话**，这些应用
没有界面，等下去不会有东西。现在如实说"还没做界面"并给真的下一步（从桌面移除）。

### 把两批固定配色收进主题体系

`AetherphoneParityScreens.kt` 的职业屏和活跃度屏原来是照原版 Aetherphone
复刻的固定深蓝底（`#12335E→#061423`、`#063454→#03111D`）+ 二十来个写死蓝灰字色。
`ClockAndTimersScreens.kt` 的时钟屏是写死近黑 `#111117`。

处理原则：**渐变这个形式留着**（数据屏该有仪表盘气质，去掉就成平底页了），
色标改成从 `PhoneBackground` 算（`ThemedDataFrame`）。两屏合计 **57 处**写死色
换成 token（Aether 39 + 时钟 18），用带精确计数断言的脚本改的（见 5.9）。

---

## 5. 踩过的坑（必读）

这一节是这份文档最有价值的部分。每条都是真犯过的错，不是理论风险。

### 5.1 `fillMaxSize()` 在 `Column` 里会吃掉全部剩余高度

**同一类错我犯过三次**：联系人页头重叠、提示框把图标顶走、**幻化点赞收藏条
整条看不见**。最后那个的代码是：

```kotlin
// 错：ScreenFrame 的 Column 里，这个 LazyColumn 吃光高度，
// 下面的动作条被压成 0 高度 —— 不是没画，是高度为 0
LazyColumn(Modifier.fillMaxSize()) { ... }
底部动作条()
```

**必须写 `.weight(1f)`**。`ScreenFrame { }` 的 content 是 `ColumnScope`，
里面任何"占满剩余"的滚动容器都用 `weight(1f)`，永远不用 `fillMaxSize()`。

### 5.2 白压白：屏幕从"永远深底"改成跟主题时

职业屏 / 活跃度屏 / 时钟屏原来永远是深底，所以里面写了
`Color.White` 当图标色、`Color.White.copy(alpha=.1f)` 当底。
收进主题后浅色模式下就是**白底上放白图标**，直接看不见。

这批一共抓到 **15 处** `Color.White`（职业/活跃度 4、时钟 11），其中 13 处是
真的白压白，另 2 处是 5.3 那种 `BrandFill` 上压白字。

改屏幕底色之前，先把这一屏所有 `Color.White` 和 `Color.White.copy(alpha=...)`
找出来一个个判断 —— 用 `git diff | grep -c "^-.*Color\.White"` 核对改了几处。

### 5.3 `Color.White` 落在 `BrandFill` 上是对比度 bug

`BrandOnFill` 就是为这件事算出来的（亮底给深墨、暗底给白）。
写死白字的话，浅色强调色（石之家金 `#C4A86A`）上就不够对比。
凡是 `background(BrandFill)` 的地方，字色一律 `BrandOnFill`。

### 5.4 一个颜色不能同时干三件事

填充要**鲜艳**（配白字）、文字要**够对比**（≥4.5:1）、气泡要**深**
（游戏频道色落上面）。把它们当一个颜色用就会出 5.2 那种 2.05:1。
这是 `AccentPalette` 拆四个角色的原因。

### 5.5 竞争的 `pointerInput` 会互相抢手势

一个节点上同时挂 `detectTapGestures` 和 `detectHorizontalDragGestures`，
谁先消费谁赢，症状是**偶发不响应**。修法是一个 `awaitPointerEventScope` 循环
自己处理（见 `SubScreens.kt` 的 `ColorSlider`）。

### 5.6 居中标题需要两侧对称

左 46dp、右 76dp（两个 38dp 按钮）会让"居中"标题偏 15dp —— 不重叠，
但看着就是没对齐。联系人页头为这个改了三轮。**最后改成左对齐**，
左对齐没有这个前提条件。别再试图靠调 padding 配平。

### 5.7 改了颜色语义就要改注释

我删掉固定深蓝底时，上面留着一段注释写"这两屏刻意不跟深浅主题、
那些写死的蓝灰是有意的"。**留一段和代码相反的说明比没有说明更坏**。
同类：`washing_num` 我一度标成"漂白次数"，实际是**幻想药使用次数**。

### 5.8 改之前先确认改的是不是渲染中的那份

联系人页头我改了三轮都没效果，因为真正渲染的是
`AetherphoneContactDetailScreen`，而 `SubScreens.kt` 里还躺着一整套
**死的**联系人实现，我一直在读那个。**顺着 `EorzeaPhoneApp.kt` 的路由表
找到真正的入口再动手。**

删死代码之前先读一遍：那次清掉 514 行时，从里面救回了两个丢掉的能力
（`FriendActionKind.SearchInfo` = 4，和基于 `contentId` 的禁用态）。

### 5.9 脚本化替换必须带精确计数断言

批量改颜色/文案时我用 Python 脚本，每条替换都写明期望出现次数，
**数量不符就整个中止、不做"尽力而为"的改写**（见
`.buildtmp/aether_theme.py`、`clock_theme.py`）。有一次
`#AAB6C4`（聊天 System 频道色，必须保留）和 `#AAB8C5`（活跃度标签，要换）
只差一位，靠这个断言才没改错。

### 5.10 死代码扫描的假阳性

`deadscan.py` 漏掉了尾随 lambda 的调用（`SettingsGroup { }` 没有括号），
正则要写成 `\b<name>\s*[({]` 外加 `::name`。扫出来的每个名字
**再用独立 grep 交叉验证一遍**再删。

### 5.11 Kotlin KDoc 里的 `posts` + 斜杠星号会吞代码

Kotlin 的块注释**可嵌套**，KDoc 里写 `posts/*` 那个序列会开一层内嵌注释，
把后面整段代码吞掉。`ShizhijiaApi.kt` 里有注释专门警告这个。

### 5.12 导航没有 `SaveableStateHolder`

`AnimatedContent` + `when`，没有 per-screen 的状态保持器，所以
`rememberSaveable` **活不过屏幕切换**。状态要提到 `PhoneState` 里
（`accentId`、`accentCustom` 就是这么处理的）。

### 5.13 `check_imports.py` 曾把 URL 里的 `//` 当行注释（已修）

原来的删除顺序是「块注释 → 行注释 → 字符串」，于是 `"https://…"` 里的
`//` 被当成行注释开头，**把后半行连右引号一起删掉**；剩一个落单的左引号，
字符串正则接着从它一路匹配到很远处的下一个引号，把中间的真代码吞了。

实测 `ShizhijiaScreens.kt` 全文只有 4 个带 `//` 的 URL 字符串，
却骗走 226 个引号、吞掉约 74KB 正文 —— 报的 13 条"多余 import"里
**10 条是假阳性**（`WikiDicts`、`WebView`、`slideOutVertically` 都在用）。

已改成**字符串先删**（三引号也要先处理，它跨行）。13 条降到 3 条。
发现它的线索是"gradle 过了但脚本说 `WikiDicts` 没用"这个矛盾 ——
**脚本和编译器打架时，信编译器**。

还剩两类已知假阳性，别照着改：

- **「可能缺 import」里的枚举成员**。`Glamour`/`Posts`/`Rp`/`Strats` 其实是
  `SzjFavTab.Glamour` 这样的限定名，脚本只看裸标识符。
- **不带参数跑等于什么都没查**。它从 `sys.argv[1:]` 取文件，
  不给路径时输出「多余 0 条」，那是空集不是干净。要显式传文件：
  `python check_imports.py <那些 .kt>`。

---

## 6. 设计约束（用户明确表达过的）

设计体系是**板岩 + 水晶青**（取材莫杜纳的冷调岩色、银泪湖水晶光）。

**用户否掉过两版配色**，原话"不好看"：

1. 深色 = 近黑底 + 单一金色点缀
2. 浅色 = 暖米白 + 暗金

这两种正好是 `frontend-design` skill 点名的 **AI 默认审美**。那个 skill 列了
三种要避开的：暖米白 `#F4F1EA` + 高对比衬线 + 陶土色；近黑底 + 单一亮酸绿/朱红；
报纸式细线分栏 + 零圆角。**换配色时避开这三个方向。**

- **不喜欢紫色和金色**（所以默认主题色是水晶青，见第 4 节）
- 要**简约、卡片感、动效流畅**
- 签名元素 `SzjShard`（锥形水晶棱条）**只用作分区/选中标记**，别当图标用
  （用废过一次）
- 唯一卡片原语：石之家用 `SzjCardSurface`，壳层用 `PhoneCard`

**skill 装好了**：`~/.claude/settings.json` 的 `enabledPlugins` 加了
`frontend-design@claude-plugins-official`（marketplace 早就 clone 在本地，
`source` 是相对路径不用联网）。改前备份在
`settings.json.bak-20260829-043802`，`env` 块里的代理配置原样没动。
**要重启会话才注册**，但 skill 本体就一个 `SKILL.md`，急用直接读：
`~/.claude/plugins/marketplaces/claude-plugins-official/plugins/frontend-design/skills/frontend-design/SKILL.md`

### 哪些颜色**有意不跟主题**

这条容易被后人"顺手统一"掉，所以单列出来。这些是**各自代表的东西的身份**，
跟着主题变就认不出来了：

- 聊天频道色（`ChatCategory` 那一组）、情感动作色 `#BEFFF1`
- 活跃度三环的红 / 绿 / 青（哪环是哪项指标）
- 计时器每条的颜色（每日重置橙、部队红、每周蓝、时尚粉、仙人彩金、海钓青）
- 传送横幅的固定深底（它盖在任意界面上，包括浅色的桌面，
  只有自带深底 + 白字才能保证任何背景下都读得清）

代码里这些地方都写了注释说明原因。

---

## 7. 石之家接口：怎么查，不用抓包

站点是 Vite 分包的 Vue 应用，chunk 全是公开静态资源，**接口形状直接从 JS 里读**。
脚本和用法在 `docs/szj/README.md`，工具：`extract_api.py`、`find_callers.py`、
`find_bodies.py`、`fetch_all.sh`、`fetch_chunks.sh`。

流程：拉入口页 → 取 entry bundle 文件名 → entry 里以字符串列着全部 chunk 名
（155 个约 2.4MB）→ 正则扫 `url:...method:...data/params` 三元组。
请求体是变量时顺着 `export{p as P}` 的别名找到消费方再看它构造的对象。

移动端是 webpack 打的，需要先从 `index.html` 的内联 runtime 里取
chunk→hash 映射表才能拉到具体分包。

**读出来的关键结论**（都记在 `API_WRITE_ENDPOINTS.md`）：

- POST body 是 form-urlencoded，且 body 里也要 `tempsuid`
- 幻化分享**没有服务端接口** —— 移动端是 `html2canvas` 在客户端出图
  （scale 1.75）。我们的实现是 `rememberGraphicsLayer()` + `toImageBitmap()`，
  见 `SzjShareImage.kt`（FileProvider authority `${applicationId}.share`，
  只暴露 `cacheDir/share`）
- `userRelation/fansList` 确实存在（原来代码里是照 `followList` 对称猜的）

### `ip_location`：是省级地区名，不是 IP 地址

实测值 `中国上海市` / `中国浙江省` / `中国广西壮族自治区`。

| 接口 | 有无 |
|---|---|
| `posts/postsDetail` | 有 |
| `posts/postsCommentDetail` | 有，每条评论各一个 |
| `posts/postsList` | 无 |
| 招募详情 | 有 |
| `userInfo/getUserInfo` | 需登录，**未验证** |

**不存在"常驻 IP"这种字段。** 帖子详情内嵌的 `userInfo` 里一个 IP 相关字段
都没有，只有一堆 `*_publish` 可见性开关（`washing_num_publish`、
`house_info_publish`…）。

有些记录该字段是空字符串 —— 这就是"有些招募能看见有些看不见"的原因。

**官网 PC 端 155 个 chunk 里 `ip_location` 一次都没出现**，移动端只在
"发完评论本地先插一条"的占位对象里写了空串。两端都拿到了但都不显示，
**我们显示它是主动选择**，不是跟着官网做。用户要是不想显示，去掉很容易。

---

## 8. 未验证清单（诚实交代）

**这批工作里，十一个版本没有任何一个上机验证过，全部只过了编译。**
接手第一件事应该是装上 0.7.241 逐条看。

数字类的结论可信（对比度是算出来的、接口形状是从 JS 读出来或真打过接口的），
但下面这些在开发机上试不了：

### 需要装机才能确认

- **左滑跳评论**是否和 `LazyColumn` 的竖向滚动抢手势
  （判定在 `dx < -56dp`，`detectHorizontalDragGestures` 的方向锁应该能挡住，
  但没在真机验证）
- 主题色九套预设**好不好看**、自定义 HSV 三条滑条**跟不跟手**
- `ThemedDataFrame` 算出来的**渐变落差**够不够（深色 0.045 / 浅色 0.02 是估的）
- 浅色模式下**表盘**清不清楚（盘面 `PhoneSurfaceRaised` + 指针 `PhoneText`）
- App Store 那张**桌面地图**好不好看、按钮大小合不合适
- 分享出图那条路（`FileProvider` + `content://`）能不能真跑通
- 石之家金主题下 `BrandOnFill` 的实际观感

### 需要登录态才能验证（我没有 cookie）

- **主页有没有 `ip_location`** —— 用户说"cookie 拿我的试"但还没给。
  脚本 `docs/szj/verify_authed.sh` 已经写好，一趟能查四件事
- 四类收藏的**参数对不对**（RP 和幻化这两类我连一行真实返回都没见过）
- 七个写操作（帖子点赞/收藏/评论、评论点赞、幻化点赞/收藏、招募响应）
  **一次都没跑过**
- 别人的 `uuid` 能不能拿来看他的关系列表（官网只暴露看自己的）

### 已知还没做的

- 壳层其余小应用还有约 **40 处写死色**
  （`CollectionsScreen` 8、`LocalFeatureScreens` 11、`LocalUtilityScreens` 8、
  `FishingScreen` 3、`CalculatorScreen` 2 等），和主题体系冲突
- 时钟屏内部用的是 8dp 圆角自成一套，**没有**强行改成 `PhoneCard` 的 14dp
  （混着两种半径会更难看，要改就整屏一起）
- **字号阶梯 39 个值、圆角阶梯 21 个值**，一直没统一过
- 会话列表可能有和联系人页一样的滚动位置丢失问题
  （根治方式是给返回栈套 `rememberSaveableStateHolder`）
- `sysMsg/*` 整块站内消息没做；帖子投票 `posts/vote` 没做；
  幻化收藏夹的建/删（`glamour/deleteFavorites`）没做

---

## 9. 提交历史（这一批）

```
ac465df  时钟屏收进主题体系（0.7.241 出包）
7ea2a0c  加交接文档 HANDOFF.md
0e21313  职业屏和活跃度屏收进主题体系（原来是写死的深蓝底）
45327ef  壳层补上卡片和按钮原语、设置页和商店换过去（未出包）
edf198b  App Store 重做、占位应用不再说假话（0.7.240）
c41cbf1  默认主题色从金改回水晶青、元信息行加省略号（0.7.239 出包）
d144a0f  IP 属地并进元信息行、时间砍掉秒（0.7.239）
3f575d5  收藏的幻化点不开修好（主键是 glamour_id）、帖子和评论显示 IP 属地（0.7.238）
c59d5b4  收藏页改成四类、补上漏掉的 type、横滑改成左滑（0.7.237）
e78a2f1  主题色做成可选（九套预设 + 自定义）、气泡底改用专挑的深色（0.7.236）
18da5ff  全局改用石之家的金、右滑改成定位到评论、幻化动作条修好、分享出图（0.7.235）
5f925b8  幻想药标签、帖子详情横滑到评论、联系人详情重做、接通四个写操作（0.7.234）
```

commit message 写得很长是有意的：每条都记了**为什么**和**验证到哪一步**，
比 diff 有用。接手时 `git log` 读一遍能省很多来回。

---

## 10. 用户沟通习惯

- **用中文**。用户表达直接，会明确指出做错的地方
- **说清楚验证到哪一步**：算出来的数字和上机验证过的要分开讲，别把
  "编译通过"说成"做好了"
- 做错了**直接改并说明根因**，不要绕。用户在意的是"为什么会错"
- 需求反复是正常的（左滑/右滑来回过一次）—— 按最新的做，
  但如果新要求会破坏已经修好的东西，先说明再动

---

*文档结束。有疑问优先读 [第 5 节](#5-踩过的坑必读) 和
[第 8 节](#8-未验证清单诚实交代)。*
