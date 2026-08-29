package com.quserh.eorzeaphone.data.wiki

/**
 * 主类型 / 类型 / 品质 / 职业 的 ID→名映射。
 *
 * 全部取自站点 Lua 源码，已逐条与站点检索结果核对：
 *   - 主类型、职业：Module:Base 的 ITEM_KIND_ORDER / CLASSJOB
 *   - 类型 1..112：Module:Item/Data 的 name_map
 *   - 品质：Module:Item/ItemSearch 的 ITEM_RARITY_NAME_MAP
 */
object WikiDicts {

    val kinds = listOf(
        1 to "武器", 2 to "工具", 3 to "防具", 4 to "饰品",
        5 to "药品食品", 6 to "素材", 7 to "其他",
    )

    fun kindName(id: Int): String = kinds.firstOrNull { it.first == id }?.second ?: ""

    /** 品质：1白 2绿 3蓝 4紫 7以太。5/6 在站点不存在，别补。 */
    val rarities = listOf(1 to "白", 2 to "绿", 3 to "蓝", 4 to "紫", 7 to "以太")

    fun rarityName(id: Int): String = rarities.firstOrNull { it.first == id }?.second ?: ""

    /**
     * 染色槽数。库里存的就是槽数本身（0/1/2）。
     * 站点检索器的 dye 参数是槽数+1，做 UI 时别照搬站点的值。
     */
    val dyeOptions = listOf(0 to "不可染色", 1 to "单个染色槽", 2 to "两个染色槽")

    /** 类型 ID 1..112，下标 = ID-1。 */
    private val categoryNames = (
        "格斗武器,单手剑,大斧,弓,长枪,单手咒杖,双手咒杖,单手幻杖,双手幻杖,魔导书," +
            "盾,刻木工具（主工具）,刻木工具（副工具）,锻铁工具（主工具）,锻铁工具（副工具）," +
            "铸甲工具（主工具）,铸甲工具（副工具）,雕金工具（主工具）,雕金工具（副工具）," +
            "制革工具（主工具）,制革工具（副工具）,裁衣工具（主工具）,裁衣工具（副工具）," +
            "炼金工具（主工具）,炼金工具（副工具）,烹调工具（主工具）,烹调工具（副工具）," +
            "采矿工具（主工具）,采矿工具（副工具）,园艺工具（主工具）,园艺工具（副工具）," +
            "捕鱼用具（主工具）,钓饵,头部防具,身体防具,腿部防具,手部防具,脚部防具," +
            "停止流通道具,项链,耳饰,手镯,戒指,药品,食材,食品,水产品,石材,金属,木材," +
            "布料,皮革,骨材,炼金原料,染料,部件,家具,魔晶石,水晶,触媒,杂货,灵魂水晶,其他," +
            "房产证书,房顶,外墙,窗户,房门,房顶装饰,外墙装饰,门牌,院墙,内墙,地板,屋顶照明," +
            "庭具,桌台,桌上,壁挂,地毯,宠物,栽培用品,半魔晶石,双剑,杂货（季节活动）,九宫幻卡," +
            "双手剑,火枪,天球仪,飞空艇部件（船体）,飞空艇部件（舾装）,飞空艇部件（船尾）," +
            "飞空艇部件（船首）,管弦乐琴乐谱,绘画作品,武士刀,刺剑,魔导书（学者专用）," +
            "捕鱼用具（副工具）,货币,潜水艇部件（船体）,潜水艇部件（船尾）,潜水艇部件（船首）," +
            "潜水艇部件（舰桥）,青魔杖,枪刃,投掷武器,双手镰刀,贤具,蝰蛇对剑,画笔,套装"
        ).split(",")

    fun categoryName(id: Int): String = categoryNames.getOrNull(id - 1).orEmpty()

    /**
     * 主类型 → 细类，**顺序照抄站点 `Module:Item/Data` 的 kind_map**。
     *
     * 注意不是升序 —— 站点是按显示优先级排的（武器组第一个是"单手剑"而不是
     * "格斗武器"）。之前我按物品数量排，和网页对不上。
     */
    private val kindCategories: Map<Int, List<Int>> = mapOf(
        1 to listOf(2, 3, 87, 106, 5, 108, 1, 96, 84, 110, 4, 88, 107, 6, 7, 10,
                    97, 111, 8, 9, 98, 89, 109, 105),
        2 to listOf(12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
                    27, 28, 29, 30, 31, 32, 99),
        3 to listOf(11, 34, 35, 37, 36, 38),
        4 to listOf(41, 40, 42, 43, 62),
        5 to listOf(44, 46),
        6 to listOf(45, 47, 48, 49, 50, 51, 52, 53, 54, 56, 55, 90, 91, 93, 92,
                    101, 102, 103, 104),
        7 to listOf(58, 59, 60, 61, 85, 81, 63, 64, 65, 66, 67, 68, 69, 70, 71,
                    72, 73, 74, 75, 76, 57, 77, 78, 79, 80, 82, 83, 86, 94, 33,
                    95, 112, 39, 100),
    )

    /** 某主类型下的细类（站点顺序）。kindId=0 返回空 —— 站点也是先选主类型才有细类。 */
    fun categoriesOf(kindId: Int): List<Pair<Int, String>> =
        kindCategories[kindId].orEmpty().map { it to categoryName(it) }

    /** 可获得：站点是「全部 / 可获得 / 不可获得」。 */
    val obtainableOptions = listOf(1 to "可获得", 2 to "不可获得")

    /**
     * 版本。站点是一个 110 项的平铺下拉，chip 铺不下，
     * 所以先选资料片再选小版本（两级，和物品类型一致的交互）。
     */
    val expansions = listOf(
        2 to "重生之境", 3 to "苍穹之禁城", 4 to "红莲之狂潮", 5 to "暗影之逆焰",
        6 to "晓月之终途", 7 to "金曦之遗辉",
    )

    private val versionList = listOf(
        1.0, 1.23,
        2.0, 2.05, 2.1, 2.15, 2.16, 2.2, 2.3, 2.4, 2.45, 2.5, 2.51, 2.57,
        3.0, 3.01, 3.05, 3.1, 3.15, 3.2, 3.21, 3.25, 3.3, 3.35, 3.38, 3.4,
        3.41, 3.45, 3.5, 3.55, 3.57,
        4.0, 4.01, 4.05, 4.06, 4.1, 4.11, 4.15, 4.18, 4.2, 4.21, 4.25, 4.3,
        4.31, 4.35, 4.36, 4.4, 4.41, 4.45, 4.5, 4.55, 4.56, 4.57,
        5.0, 5.01, 5.05, 5.1, 5.11, 5.15, 5.18, 5.2, 5.21, 5.25, 5.3, 5.31,
        5.35, 5.4, 5.41, 5.45, 5.5, 5.55, 5.57, 5.58,
        6.0, 6.01, 6.05, 6.1, 6.11, 6.15, 6.18, 6.2, 6.25, 6.3, 6.31, 6.35,
        6.38, 6.4, 6.45, 6.5, 6.51, 6.55, 6.57, 6.58,
        7.0, 7.01, 7.05, 7.1, 7.11, 7.15, 7.2, 7.25, 7.3, 7.35, 7.4, 7.41,
        7.45, 7.5, 7.51, 7.55,
    )

    /** 某资料片下的小版本。站点显示成两位小数（7.5 -> "7.50"）。 */
    fun versionsOf(major: Int): List<Pair<Double, String>> =
        versionList.filter { it.toInt() == major }.map { it to "%.2f".format(it) }

    fun versionLabel(v: Double): String = "%.2f".format(v)

    /**
     * 职业 ID→名，0..43 连续无缺号。
     *
     * **别漏 1-7、26、29 这些基础/前置职业** —— 物品的"可使用职业ID"用的正是它们。
     * 例：暗黑斩(16069) 是 [3, 21] = 斧术师 + 战士，不是 [1, 21]。
     */
    private val jobNames = (
        "冒险者,剑术师,格斗家,斧术师,枪术师,弓箭手,幻术师,咒术师," +
            "刻木匠,锻铁匠,铸甲匠,雕金匠,制革匠,裁衣匠,炼金术士,烹调师," +
            "采矿工,园艺工,捕鱼人,骑士,武僧,战士,龙骑士,吟游诗人,白魔法师,黑魔法师," +
            "秘术师,召唤师,学者,双剑师,忍者,机工士,暗黑骑士,占星术士,武士,赤魔法师," +
            "青魔法师,绝枪战士,舞者,钐镰客,贤者,蝰蛇剑士,绘灵法师,驯兽师"
        ).split(",")

    fun jobName(id: Int): String = jobNames.getOrNull(id).orEmpty()

    /**
     * 职业按定位分组。
     *
     * 站点是一个 33 项的 `<select>`，一个折叠控件装得下。chip 装不下 ——
     * 33 个 chip 每行 4 个要铺 9 行，把面板里其余七组条件全挤到屏幕外
     * （真机上验过，第一屏只看得到品级/等级/职业墙）。
     * 所以做成两级：先选定位（7 个 chip 一行半），再出该定位下的职业。
     *
     * 分组本身不是我编的 —— 原来就写在 filterJobs 的注释里，
     * 顺序照站点 CLASSJOB_ORDER，这里只是把注释提成了数据。
     */
    val jobRoles: List<Pair<String, List<Int>>> = listOf(
        "坦克" to listOf(19, 21, 32, 37),
        "治疗" to listOf(24, 28, 33, 40),
        "近战" to listOf(20, 22, 30, 34, 39, 41),
        "远敏" to listOf(23, 31, 38),
        "法系" to listOf(25, 27, 35, 42, 36),
        "生产" to listOf(8, 9, 10, 11, 12, 13, 14, 15),
        "采集" to listOf(16, 17, 18),
    )

    /** 某定位下的职业。定位名给空串则返回空表。 */
    fun jobsOfRole(role: String): List<Pair<Int, String>> =
        jobRoles.firstOrNull { it.first == role }?.second.orEmpty()
            .map { it to jobName(it) }

    /** 反查某职业属于哪个定位，用于面板回填。查不到返回空串。 */
    fun roleOfJob(jobId: Int): String =
        jobRoles.firstOrNull { jobId in it.second }?.first.orEmpty()

    /**
     * 全部可筛职业的平表，仍按站点 CLASSJOB_ORDER。
     * 基础职业（剑术师等）不列 —— 它们只在物品数据里出现，玩家不会按它筛。
     */
    val filterJobs: List<Pair<Int, String>> =
        jobRoles.flatMap { it.second }.map { it to jobName(it) }

    /**
     * 品质色，**取站点 `MediaWiki:Common.css` 的原值**，不是我按观感猜的。
     *
     * 站点 class 名是 `rarity-common/uncommon/rare/epic/magic`：
     *   .rarity-common   #f3f3f3
     *   .rarity-uncommon #c0ffc0
     *   .rarity-rare     #5990ff
     *   .rarity-epic     #b38cff
     *   .rarity-magic    #d789b6
     *
     * 白色（#f3f3f3）是给站点深底用的，手机浅色主题下会看不见 ——
     * 白品质返回 0 表示"不着色，用正文色"。绿色 #c0ffc0 同理偏浅，
     * 压暗一档到 #6FBF6F 保证浅底可读（4.5:1），其余三色原样用。
     *
     * **有意不跟主题**（HANDOFF.md §6 那一类）：品质色是游戏里的身份标识，
     * 蓝=稀有、紫=史诗、粉=魔法，和聊天频道色一个道理。跟着强调色变
     * 就认不出品质了。别顺手统一掉 —— 尤其注意这里的紫色不是"配色选择"，
     * 是 FF14 的史诗品质色，与"用户不喜欢紫色"那条约束无关。
     */
    fun rarityColorArgb(rarity: Int): Long = when (rarity) {
        2 -> 0xFF6FBF6FL   // 绿：站点 #c0ffc0 在浅底上不可读，压暗
        3 -> 0xFF5990FFL   // 蓝：站点原值
        4 -> 0xFFB38CFFL   // 紫：站点原值
        7 -> 0xFFD789B6L   // 以太：站点原值
        else -> 0x00000000L // 白：站点 #f3f3f3 是深底用的，这里用正文色
    }
}
