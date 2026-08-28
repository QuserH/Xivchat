package com.quserh.eorzeaphone.ui

/**
 * App Store 的文案与分类。
 *
 * **单独一个文件，不并进 AppCatalog.kt**：那份表是"桌面上有哪些格子"，
 * 由别处维护（加应用的人改那里）；这份是"商店怎么介绍它们"。
 * 两件事分开，加应用时不需要动商店，写文案时不会碰坏桌面布局。
 *
 * [implemented] 记的是**有没有自己的界面**——这是从 EorzeaPhoneApp.kt 的
 * 路由表逐条对出来的事实（`when (state.selectedApp?.id)` 里有分支的就有，
 * 落到 `GenericAppScreen` 的就没有）。商店里如实标出来：
 * 一个只有图标和一句话的占位，装上去显示"等待游戏数据"，
 * 商店要是也说得像做好了，那是骗人。
 */
data class AppStoreEntry(
    val id: String,
    /** 一句话，说它做什么，不吹。 */
    val blurb: String,
    val category: AppStoreCategory,
    /** 有独立界面 = true；落到通用占位页 = false。 */
    val implemented: Boolean,
)

enum class AppStoreCategory(val label: String, val hint: String) {
    Tools("工具", "算数、记事、计时这类不依赖游戏也能用的"),
    GameData("游戏数据", "要连上插件才有内容，数据来自当前角色"),
    Social("社交", "和别人打交道的"),
    Fun("娱乐", "打发时间的"),
}

object AppStoreCatalog {
    /**
     * 按 id 查。表里没有的应用不会因此消失——商店照样列出来，
     * 只是没有介绍文案（见 AppStoreScreen 里的兜底）。
     * 这样别人往 AppCatalog 里加应用时，商店不会崩、也不用同步改这里。
     */
    private val entries: List<AppStoreEntry> = listOf(
        // ---- 游戏数据：连上插件才有内容 ----
        AppStoreEntry("shizhijia", "石之家：帖子、攻略、招募、幻化、部队，还有登录后的收藏和签到", AppStoreCategory.Social, true),
        AppStoreEntry("wiki", "查道具：名字、图标、能不能交易、从哪来", AppStoreCategory.GameData, true),
        AppStoreEntry("gatherclock", "采集时钟：稀有采集点的刷新时间，按艾欧泽亚时间走", AppStoreCategory.GameData, true),
        AppStoreEntry("inventory", "看当前角色的包裹和雇员仓库", AppStoreCategory.GameData, true),
        AppStoreEntry("wallet", "金币和各种代币的余额", AppStoreCategory.GameData, true),
        AppStoreEntry("character", "活跃度：在线时长、去过哪些副本", AppStoreCategory.GameData, true),
        AppStoreEntry("jobs", "所有职业的等级和进度", AppStoreCategory.GameData, true),
        AppStoreEntry("collections", "坐骑、宠物、发型这些收集进度", AppStoreCategory.GameData, true),
        AppStoreEntry("skywatcher", "当前区域天气，以及接下来几个时段的预报", AppStoreCategory.GameData, true),
        AppStoreEntry("fishing", "钓鱼记录：钓上过什么、在哪钓的", AppStoreCategory.GameData, true),
        AppStoreEntry("maps", "地图：当前坐标和采集点标记", AppStoreCategory.GameData, true),
        AppStoreEntry("dailies", "每日和每周重置的项目还剩多久", AppStoreCategory.GameData, true),
        AppStoreEntry("submarine", "潜水艇的航行状态和归来时间", AppStoreCategory.GameData, true),
        AppStoreEntry("housing", "房屋位置，以及庭院的状态", AppStoreCategory.GameData, true),
        AppStoreEntry("health", "健康：把游戏里的连续在线时长摆出来看", AppStoreCategory.GameData, true),
        AppStoreEntry("calendar", "日历：活动和版本节点", AppStoreCategory.GameData, true),

        // ---- 工具：不连游戏也能用 ----
        AppStoreEntry("calculator", "计算器", AppStoreCategory.Tools, true),
        AppStoreEntry("notes", "备忘录，写在手机里，不上传", AppStoreCategory.Tools, true),
        AppStoreEntry("clock", "时钟：艾欧泽亚时间和现实时间对照", AppStoreCategory.Tools, true),
        AppStoreEntry("timers", "计时器：能同时跑好几个", AppStoreCategory.Tools, true),
        AppStoreEntry("shortcuts", "快捷指令：把常用的游戏指令存起来一键发", AppStoreCategory.Tools, true),
        AppStoreEntry("camera", "相机：拍下当前画面存进照片", AppStoreCategory.Tools, true),
        AppStoreEntry("photos", "照片：相机拍的和游戏截图都在这儿", AppStoreCategory.Tools, true),
        AppStoreEntry("notifications", "通知：把插件推来的消息集中在一处", AppStoreCategory.Tools, true),
        AppStoreEntry("appstore", "就是这儿。装应用、卸应用、看桌面还剩多少位置", AppStoreCategory.Tools, true),

        // ---- 还没做界面的：装上去只有一句占位 ----
        AppStoreEntry("chirper", "叽叽：短消息，界面还没做", AppStoreCategory.Social, false),
        AppStoreEntry("aethergram", "以太图集：图片流，界面还没做", AppStoreCategory.Social, false),
        AppStoreEntry("velvet", "Velvet，界面还没做", AppStoreCategory.Social, false),
        AppStoreEntry("polls", "投票，界面还没做", AppStoreCategory.Social, false),
        AppStoreEntry("announcements", "公告，界面还没做", AppStoreCategory.Social, false),
        AppStoreEntry("feedback", "反馈，界面还没做", AppStoreCategory.Social, false),
        AppStoreEntry("yellowpages", "黄页，界面还没做", AppStoreCategory.Social, false),
        AppStoreEntry("muster", "Muster：集合点名，界面还没做", AppStoreCategory.Social, false),
        AppStoreEntry("venues", "场馆，界面还没做", AppStoreCategory.Fun, false),
        AppStoreEntry("games", "游戏，界面还没做", AppStoreCategory.Fun, false),
        AppStoreEntry("gamba", "Gamba，界面还没做", AppStoreCategory.Fun, false),
        AppStoreEntry("coin", "Aether Coin，界面还没做", AppStoreCategory.Fun, false),
        AppStoreEntry("market", "市场板：查价格，界面还没做", AppStoreCategory.GameData, false),
    )

    private val byId = entries.associateBy { it.id }

    fun entry(id: String): AppStoreEntry? = byId[id]

    /** 表里没有这个 id 时给一句中性的说明，别让卡片空着。 */
    fun blurbOf(id: String): String = byId[id]?.blurb ?: "还没有介绍"

    fun categoryOf(id: String): AppStoreCategory = byId[id]?.category ?: AppStoreCategory.Tools

    /** 不确定的按"没做"算：宁可少说，别把占位说成功能。 */
    fun isImplemented(id: String): Boolean = byId[id]?.implemented ?: false
}
