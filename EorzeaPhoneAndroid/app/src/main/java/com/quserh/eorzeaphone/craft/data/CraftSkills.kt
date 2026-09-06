package com.quserh.eorzeaphone.craft.data

import com.quserh.eorzeaphone.data.GameCraftSkill

data class SkillDef(
    val id: Long,
    val cn: String,
    val en: String,
    val cp: Int,
    val durability: Int,
    val kind: Kind,
    val note: String,
    val icon: Int = 0,
    val desc: String = "",
    val canonicalId: Long = id,
    val specialist: Boolean = false,
) {
    enum class Kind { PROGRESS, QUALITY, PROGRESS_QUALITY, BUFF, REPAIR, OTHER }
}

/**
 * CN names and per-job rows: thewakingsands/ffxiv-datamining-cn CraftAction/Action.
 * Skill coverage and canonical IDs: Artisan RawInformation/Character/Skills.cs.
 * Keep icons and action IDs together; craft jobs are not all contiguous in the sheets.
 */
object CraftSkills {
    val ALL: List<SkillDef> = listOf(
        skill(100001, "制作", "Basic Synthesis", 0, 10, SkillDef.Kind.PROGRESS, 1501, "消耗耐久以推动作业进展。"),
        skill(100203, "模范制作", "Careful Synthesis", 7, 10, SkillDef.Kind.PROGRESS, 1986, "消耗耐久以推动作业进展。效率高于制作。"),
        skill(100363, "高速制作", "Rapid Synthesis", 0, 10, SkillDef.Kind.PROGRESS, 1988, "消耗耐久以推动作业进展。成功率50%。"),
        skill(100403, "坯料制作", "Groundwork", 18, 20, SkillDef.Kind.PROGRESS, 1518, "大幅推动作业进展。耐久不足消耗值时效率减半。"),
        skill(100315, "集中制作", "Intensive Synthesis", 6, 10, SkillDef.Kind.PROGRESS, 1514, "推动作业进展。高品质、最高品质或专心致志状态下可用。"),
        skill(100427, "俭约制作", "Prudent Synthesis", 18, 5, SkillDef.Kind.PROGRESS, 1520, "以较少耐久推动作业进展。俭约状态下无法使用。"),
        skill(100379, "坚信", "Muscle Memory", 6, 10, SkillDef.Kind.PROGRESS, 1994, "仅首步可用。推动作业进展，并使5次作业内的下一次制作效率提高100%。"),
        skill(100323, "精密制作", "Delicate Synthesis", 32, 10, SkillDef.Kind.PROGRESS_QUALITY, 1503, "同时推动作业进展并提高制品品质。"),
        skill(100002, "加工", "Basic Touch", 18, 10, SkillDef.Kind.QUALITY, 1502, "消耗耐久以提高制品品质。效率100。"),
        skill(100004, "中级加工", "Standard Touch", 32, 10, SkillDef.Kind.QUALITY, 1516, "提高制品品质。效率125。加工连击时制作力消耗降至18。"),
        skill(100411, "上级加工", "Advanced Touch", 46, 10, SkillDef.Kind.QUALITY, 1519, "提高制品品质。效率150。中级加工连击或观察后制作力消耗降至18。"),
        skill(100355, "仓促", "Hasty Touch", 0, 10, SkillDef.Kind.QUALITY, 1989, "提高制品品质。效率100，成功率60%。成功时附加工匠的良机，可使用冒进。"),
        skill(100299, "坯料加工", "Preparatory Touch", 40, 20, SkillDef.Kind.QUALITY, 1507, "提高制品品质。效率200。额外积累1档内静。"),
        skill(100128, "集中加工", "Precise Touch", 18, 10, SkillDef.Kind.QUALITY, 1524, "高品质、最高品质或专心致志状态下可用。效率150，额外积累1档内静。"),
        skill(100227, "俭约加工", "Prudent Touch", 25, 5, SkillDef.Kind.QUALITY, 1535, "以较少耐久提高制品品质。俭约状态下无法使用。"),
        skill(100435, "工匠的神技", "Trained Finesse", 32, 0, SkillDef.Kind.QUALITY, 1997, "内静达到10档时可用。不消耗耐久，提高制品品质。效率100。"),
        skill(100387, "闲静", "Reflect", 6, 10, SkillDef.Kind.QUALITY, 1982, "仅首步可用。提高制品品质并额外积累1档内静。"),
        skill(100443, "精炼加工", "Refined Touch", 24, 10, SkillDef.Kind.QUALITY, 1522, "提高制品品质。效率100。加工连击时额外积累1档内静。"),
        skill(100451, "冒进", "Daring Touch", 0, 10, SkillDef.Kind.QUALITY, 1998, "工匠的良机状态下可用。效率150，成功率60%。"),
        skill(100339, "比尔格的祝福", "Byregot's Blessing", 24, 10, SkillDef.Kind.QUALITY, 1975, "内静状态下可用。效率为100加上每档内静20。使用后内静消失。"),
        skill(100283, "工匠的神速技巧", "Trained Eye", 250, 0, SkillDef.Kind.QUALITY, 1981, "仅首步可用。制品品质提升至最大。职业等级须高于配方等级至少10级，高难度配方无法使用。"),
        skill(19297, "崇敬", "Veneration", 18, 0, SkillDef.Kind.BUFF, 1995, "4次作业内，推动作业进展的效率提高50%。"),
        skill(19004, "改革", "Innovation", 18, 0, SkillDef.Kind.BUFF, 1987, "4次作业内，提高制品品质的效率提高50%。"),
        skill(260, "阔步", "Great Strides", 32, 0, SkillDef.Kind.BUFF, 1955, "3次作业内，下一次提高制品品质的效率提高100%。"),
        skill(100371, "秘诀", "Tricks of the Trade", 0, 0, SkillDef.Kind.OTHER, 1990, "恢复20点制作力。高品质、最高品质或专心致志状态下可用。"),
        skill(100003, "精修", "Master's Mend", 88, 0, SkillDef.Kind.REPAIR, 1952, "恢复30点耐久。"),
        skill(4574, "掌握", "Manipulation", 96, 0, SkillDef.Kind.BUFF, 1985, "8次作业内，每次作业结束时恢复5点耐久。"),
        skill(4631, "俭约", "Waste Not", 56, 0, SkillDef.Kind.BUFF, 1992, "4次作业内，耐久消耗减半。"),
        skill(4639, "长期俭约", "Waste Not II", 98, 0, SkillDef.Kind.BUFF, 1993, "8次作业内，耐久消耗减半。"),
        skill(100010, "观察", "Observe", 7, 0, SkillDef.Kind.OTHER, 1954, "不进行任何操作，推进一次作业。"),
        skill(100395, "设计变动", "Careful Observation", 0, 0, SkillDef.Kind.OTHER, 1984, "消耗能工巧匠图纸改变制作状态，不推进作业。每次制作最多3次。", true),
        skill(19012, "最终确认", "Final Appraisal", 1, 0, SkillDef.Kind.BUFF, 1983, "5次作业内，下一次令作业完成的进展上升会停在完成前1点。使用本技能不推进作业。"),
        skill(100419, "专心致志", "Heart and Soul", 0, 0, SkillDef.Kind.BUFF, 1996, "消耗能工巧匠图纸，允许在普通状态下使用一次集中制作、集中加工或秘诀。不推进作业，每次制作限1次。", true),
        skill(100459, "快速改革", "Quick Innovation", 0, 0, SkillDef.Kind.BUFF, 1999, "消耗能工巧匠图纸，附加持续1次作业的改革。不推进作业，每次制作限1次。改革状态下无法使用。", true),
        skill(100467, "巧夺天工", "Immaculate Mend", 112, 0, SkillDef.Kind.REPAIR, 1950, "恢复全部耐久。"),
        skill(100475, "工匠的绝技", "Trained Perfection", 0, 0, SkillDef.Kind.BUFF, 1926, "令下一次消耗耐久的作业不消耗耐久。每次制作限1次。"),
    )

    private fun skill(id: Long, cn: String, en: String, cp: Int, durability: Int, kind: SkillDef.Kind, icon: Int, desc: String, specialist: Boolean = false) =
        SkillDef(id, cn, en, cp, durability, kind, "", icon, desc, specialist = specialist)

    private val actions = mapOf(
        100001L to listOf(100001L, 100015, 100030, 100075, 100045, 100060, 100090, 100105),
        100002L to listOf(100002L, 100016, 100031, 100076, 100046, 100061, 100091, 100106),
        100003L to listOf(100003L, 100017, 100032, 100077, 100047, 100062, 100092, 100107),
        100004L to listOf(100004L, 100018, 100034, 100078, 100048, 100064, 100093, 100109),
        100010L to listOf(100010L, 100023, 100040, 100082, 100053, 100070, 100099, 100113),
        100128L to listOf(100128L, 100129, 100130, 100131, 100132, 100133, 100134, 100135),
        100203L to listOf(100203L, 100204, 100205, 100206, 100207, 100208, 100209, 100210),
        100227L to listOf(100227L, 100228, 100229, 100230, 100231, 100232, 100233, 100234),
        100283L to listOf(100283L, 100284, 100285, 100286, 100287, 100288, 100289, 100290),
        100299L to listOf(100299L, 100300, 100301, 100302, 100303, 100304, 100305, 100306),
        100315L to listOf(100315L, 100316, 100317, 100318, 100319, 100320, 100321, 100322),
        100323L to listOf(100323L, 100324, 100325, 100326, 100327, 100328, 100329, 100330),
        100339L to listOf(100339L, 100340, 100341, 100342, 100343, 100344, 100345, 100346),
        100355L to listOf(100355L, 100356, 100357, 100358, 100359, 100360, 100361, 100362),
        100363L to listOf(100363L, 100364, 100365, 100366, 100367, 100368, 100369, 100370),
        100371L to listOf(100371L, 100372, 100373, 100374, 100375, 100376, 100377, 100378),
        100379L to listOf(100379L, 100380, 100381, 100382, 100383, 100384, 100385, 100386),
        100387L to listOf(100387L, 100388, 100389, 100390, 100391, 100392, 100393, 100394),
        100395L to listOf(100395L, 100396, 100397, 100398, 100399, 100400, 100401, 100402),
        100403L to listOf(100403L, 100404, 100405, 100406, 100407, 100408, 100409, 100410),
        100411L to listOf(100411L, 100412, 100413, 100414, 100415, 100416, 100417, 100418),
        100419L to listOf(100419L, 100420, 100421, 100422, 100423, 100424, 100425, 100426),
        100427L to listOf(100427L, 100428, 100429, 100430, 100431, 100432, 100433, 100434),
        100435L to listOf(100435L, 100436, 100437, 100438, 100439, 100440, 100441, 100442),
        100443L to listOf(100443L, 100444, 100445, 100446, 100447, 100448, 100449, 100450),
        100451L to listOf(100451L, 100452, 100453, 100454, 100455, 100456, 100457, 100458),
        100459L to listOf(100459L, 100460, 100461, 100462, 100463, 100464, 100465, 100466),
        100467L to listOf(100467L, 100468, 100469, 100470, 100471, 100472, 100473, 100474),
        100475L to listOf(100475L, 100476, 100477, 100478, 100479, 100480, 100481, 100482),
        260L to listOf(260L, 261, 262, 263, 265, 264, 266, 267),
        4574L to listOf(4574L, 4575, 4576, 4577, 4578, 4579, 4580, 4581),
        4631L to listOf(4631L, 4632, 4633, 4634, 4635, 4636, 4637, 4638),
        4639L to listOf(4639L, 4640, 4641, 4642, 4643, 4644, 19002, 19003),
        19004L to listOf(19004L, 19005, 19006, 19007, 19008, 19009, 19010, 19011),
        19012L to listOf(19012L, 19013, 19014, 19015, 19016, 19017, 19018, 19019),
        19297L to listOf(19297L, 19298, 19299, 19300, 19301, 19302, 19303, 19304),
    )

    private val icons = mapOf(
        100001L to listOf(1501, 1551, 1601, 1651, 1701, 1751, 1801, 1851),
        100002L to listOf(1502, 1552, 1602, 1652, 1702, 1752, 1802, 1852),
        100004L to listOf(1516, 1566, 1616, 1665, 1716, 1765, 1816, 1865),
        100128L to listOf(1524, 1574, 1625, 1676, 1724, 1774, 1825, 1875),
        100227L to listOf(1535, 1584, 1635, 1686, 1734, 1784, 1835, 1886),
        100299L to listOf(1507, 1557, 1607, 1657, 1707, 1757, 1807, 1857),
        100315L to listOf(1514, 1564, 1614, 1663, 1714, 1763, 1814, 1863),
        100323L to listOf(1503, 1553, 1603, 1653, 1703, 1753, 1803, 1853),
        100403L to listOf(1518, 1568, 1618, 1667, 1718, 1767, 1818, 1867),
        100411L to listOf(1519, 1569, 1620, 1669, 1719, 1769, 1820, 1869),
        100427L to listOf(1520, 1570, 1621, 1670, 1720, 1770, 1821, 1870),
        100443L to listOf(1522, 1572, 1623, 1674, 1722, 1772, 1823, 1873),
    )
    private val byActionId = ALL.flatMap { skill -> actions.getValue(skill.id).map { it to skill } }.toMap()

    fun byId(id: Long): SkillDef? = byActionId[id]

    fun forJob(job: Int, live: List<GameCraftSkill> = emptyList()): List<SkillDef> {
        require(job in 0..7)
        val gameRows = live.associateBy { it.id }
        return ALL.map { skill ->
            val actionId = actions.getValue(skill.id)[job]
            val jobIcon = icons[skill.id]?.get(job) ?: skill.icon
            val exact = gameRows[actionId]
            val game = exact ?: gameRows[skill.id]
            skill.copy(
                id = actionId,
                cn = game?.name?.takeIf { it.isNotBlank() } ?: skill.cn,
                icon = exact?.icon?.takeIf { it > 0 } ?: if (jobIcon == skill.icon) game?.icon?.takeIf { it > 0 } ?: jobIcon else jobIcon,
                cp = game?.cp?.takeIf { it >= 0 } ?: skill.cp,
                desc = game?.description?.takeIf { it.isNotBlank() } ?: skill.desc,
            )
        }
    }
}
