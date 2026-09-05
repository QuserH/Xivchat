package com.quserh.eorzeaphone.craft.data

import com.quserh.eorzeaphone.data.GameCraftState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * One crafting skill usable from the workbench. Ids follow Artisan's Skills enum
 * (CraftActions base ids >= 100000 / Action ids otherwise) — the plugin side will
 * resolve per-job action ids exactly like Artisan.SkillActionMap does.
 */
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
) {
    enum class Kind { PROGRESS, QUALITY, PROGRESS_QUALITY, BUFF, REPAIR, OTHER }
}

/** Standard endgame crafting skill set (PunishXIV/Artisan RawInformation Skills.cs). */
object CraftSkills {
    val ALL: List<SkillDef> = listOf(
        SkillDef(100001, "基本制作", "Basic Synthesis", 0, 10, SkillDef.Kind.PROGRESS, "120p 进展", 1501, "对进展上升 120% 效率。成功率 100%。耐久消耗 10。"),
        SkillDef(100203, "慎重制作", "Careful Synthesis", 7, 10, SkillDef.Kind.PROGRESS, "180p 进展", 1986, "对进展上升 180% 效率。成功率 100%。耐久消耗 10。"),
        SkillDef(100363, "快速制作", "Rapid Synthesis", 0, 10, SkillDef.Kind.PROGRESS, "500p 进展 / 50% 成功", 1988, "对进展上升 500% 效率。成功率 50%。耐久消耗 10。"),
        SkillDef(100403, "扎实制作", "Groundwork", 18, 20, SkillDef.Kind.PROGRESS, "360p 进展", 1518, "对进展上升 360% 效率。耐久剩余不足 20 时效率减半。耐久消耗 20。"),
        SkillDef(100315, "集中制作", "Intensive Synthesis", 6, 10, SkillDef.Kind.PROGRESS, "400p 进展 / 需良好以上", 1514, "状态为高品质或最高品质时可用。对进展上升 400% 效率。成功率 100%。"),
        SkillDef(100379, "专心致志", "Muscle Memory", 6, 10, SkillDef.Kind.PROGRESS, "300p 进展 / 仅首步", 1994, "仅限初次操作。对进展上升 300% 效率，并获得『坚信』效果。成功率 100%。"),
        SkillDef(100323, "精细制作", "Delicate Synthesis", 32, 10, SkillDef.Kind.PROGRESS_QUALITY, "进展+加工 各100p", 1503, "对进展与品质均上升 100% 效率。成功率 100%。耐久消耗 10。"),
        SkillDef(100002, "基本加工", "Basic Touch", 18, 10, SkillDef.Kind.QUALITY, "100p 品质", 1502, "对品质上升 100% 效率。成功率 100%。耐久消耗 10。"),
        SkillDef(100004, "中级加工", "Standard Touch", 18, 10, SkillDef.Kind.QUALITY, "125p 品质(连击)", 1516, "对品质上升 125% 效率。接在基本加工之后为连击。耐久消耗 10。"),
        SkillDef(100411, "上级加工", "Advanced Touch", 18, 10, SkillDef.Kind.QUALITY, "150p 品质(连击)", 1519, "对品质上升 150% 效率。接在中级加工之后为连击。耐久消耗 10。"),
        SkillDef(100227, "节约加工", "Prudent Touch", 25, 5, SkillDef.Kind.QUALITY, "100p 品质 / 耐久-5", 1535, "对品质上升 100% 效率。耐久消耗减半(5)。『短期节约』中无法使用。"),
        SkillDef(100299, "准备加工", "Preparatory Touch", 40, 20, SkillDef.Kind.QUALITY, "200p 品质 / +1内静", 1507, "对品质上升 200% 效率。内静层数 +1。耐久消耗 20。"),
        SkillDef(100128, "集中加工", "Precise Touch", 18, 10, SkillDef.Kind.QUALITY, "150p 品质 / 需良好以上", 1524, "状态为高品质或最高品质时可用。对品质上升 150% 效率。内静层数 +1。"),
        SkillDef(100339, "比尔格的祝福", "Byregot's Blessing", 24, 10, SkillDef.Kind.QUALITY, "品质+20×内静层数", 1975, "对品质上升 100%+20%×内静层数 效率。使用后内静清零。"),
        SkillDef(100003, "工匠的妙计", "Master's Mend", 88, 0, SkillDef.Kind.REPAIR, "耐久 +30", 1952, "恢复 30 点耐久。"),
        SkillDef(100467, "完美修复", "Immaculate Mend", 112, 0, SkillDef.Kind.REPAIR, "耐久回满", 1950, "将耐久恢复至最大值。"),
        SkillDef(19297, "虔敬", "Veneration", 18, 0, SkillDef.Kind.BUFF, "4 步内进展效果 +50%", 1995, "接下来的 4 步内，进展类效率上升 50%。"),
        SkillDef(19004, "改革", "Innovation", 18, 0, SkillDef.Kind.BUFF, "4 步内加工效果 +50%", 1987, "接下来的 4 步内，品质类效率上升 50%。"),
        SkillDef(260, "阔步", "Great Strides", 32, 0, SkillDef.Kind.BUFF, "下次加工效果翻倍", 1955, "下一次品质类效率翻倍。"),
        SkillDef(100010, "观察", "Observe", 7, 0, SkillDef.Kind.OTHER, "原地不动一步", 1954, "不进行任何操作，静待一步。"),
    )

    fun byId(id: Long): SkillDef? = ALL.firstOrNull { it.id == id }
}

/** Live state of one craft, published while a session runs. */
data class CraftState(
    val recipe: CraftRecipe,
    val itemName: String,
    val step: Int,
    val progress: Int,
    val progressMax: Int,
    val quality: Int,
    val qualityMax: Int,
    val durability: Int,
    val durabilityMax: Int,
    val cp: Int,
    val cpMax: Int,
    val condition: String,
    val finished: Boolean,
    val hqChance: Int,
    val remote: Boolean = false,
    /** 远程模式:插件推的角色当前可否行动(动画锁/可用性)。模拟模式忽略。 */
    val canAct: Boolean = false,
)

/**
 * Drives one manual crafting session.
 *
 * [MockCraftEngine] simulates everything client-side (clearly labelled 模拟 in the
 * UI). The remote engine that talks to the game through new plugin ops replaces
 * it after the plugin side lands — same [CraftSession] interface, see
 * docs/集成方案.md for the op layout.
 */
interface CraftSession {
    val state: StateFlow<CraftState?>
    val log: StateFlow<List<String>>
    /** Seconds until the next skill is allowed (the in-game 2.5s GCD). */
    val cooldown: StateFlow<Int>
    fun useSkill(skill: SkillDef)
    fun stop()
}

/**
 * Bridge the workbench uses to drive a real in-game craft through the plugin
 * (ops 30/31/32) and to observe its state pushes (op 40).
 */
interface CraftRemote {
    val lastState: GameCraftState?
    fun craftStart(recipeId: Int)
    fun craftSkill(actionId: Long)
    fun craftStop()
    fun craftFood(itemId: Int)
}

private fun mapCondition(id: Int): String = when (id) {
    0 -> "稳定"; 1 -> "高品质"; 2 -> "最高品质"; 3 -> "低品质"; else -> "特殊"
}

class RemoteCraftSession(
    scope: CoroutineScope,
    recipe: CraftRecipe,
    itemName: String,
    private val bridge: CraftRemote,
) : CraftSession {
    override val state = MutableStateFlow<CraftState?>(null)
    override val log = MutableStateFlow(listOf("远程制作：已请求游戏打开配方…"))
    override val cooldown = MutableStateFlow(0)

    private var progressMaxSeen = 0
    private var qualityMaxSeen = 0

    private val worker = scope.launch {
        bridge.craftStart(recipe.id)
        state.value = CraftState(
            recipe = recipe, itemName = itemName, step = 0,
            progress = 0, progressMax = 0, quality = 0, qualityMax = 0,
            durability = 0, durabilityMax = 0, cp = 0, cpMax = 0,
            condition = "等待游戏", finished = false, hqChance = 0,
        )
        while (isActive && state.value?.finished != true) {
            delay(120)
            val s = bridge.lastState ?: continue
            if (s.progress > progressMaxSeen) progressMaxSeen = s.progress
            if (s.quality > qualityMaxSeen) qualityMaxSeen = s.quality
            state.value = CraftState(
                recipe = recipe, itemName = itemName, step = s.step,
                progress = s.progress, progressMax = if (s.progressMax > 0) s.progressMax else progressMaxSeen,
                quality = s.quality, qualityMax = if (s.qualityMax > 0) s.qualityMax else qualityMaxSeen,
                durability = s.durability, durabilityMax = s.durabilityMax,
                cp = s.cp, cpMax = s.cpMax,
                condition = mapCondition(s.conditionId),
                finished = s.finished, hqChance = 0, remote = true,
            ).copy(canAct = s.canAct && !s.finished)
            if (s.finished) {
                log.value = (log.value + "游戏内制作已结束").takeLast(40)
            }
        }
    }

    override fun useSkill(skill: SkillDef) {
        val current = state.value ?: return
        // Skill availability is driven by the plugin's animation-lock push (canAct):
        // buttons go bright exactly when the character can act again.
        if (current.finished || !current.canAct) return
        bridge.craftSkill(skill.id)
        // Optimistically dim until the next plugin frame reports canAct again.
        state.value = current.copy(canAct = false)
    }

    override fun stop() {
        bridge.craftStop()
        worker.cancel()
    }
}

class MockCraftEngine(private val scope: CoroutineScope) {

    private var session: CraftSession? = null

    fun current(): CraftSession? = session

    fun startMock(recipe: CraftRecipe, itemName: String): CraftSession {
        stop()
        val impl = MockSession(scope, recipe, itemName)
        session = impl
        return impl
    }

    fun stop() {
        session?.stop()
        session = null
    }

    fun startRemote(recipe: CraftRecipe, itemName: String, bridge: CraftRemote): CraftSession {
        stop()
        val impl = RemoteCraftSession(scope, recipe, itemName, bridge)
        session = impl
        return impl
    }
}

private class MockSession(
    scope: CoroutineScope,
    recipe: CraftRecipe,
    itemName: String,
) : CraftSession {

    // Real caps from the recipe level table (schema v3).
    private val progressMax = if (recipe.pmax > 0) recipe.pmax else (60 + recipe.craftLv * 22)
    private val qualityMax = if (recipe.qmax > 0) recipe.qmax else (400 + recipe.craftLv * 130)
    private val durabilityMax = if (recipe.stars > 0) 70 else 60

    private val inner = MutableStateFlow(
        CraftState(
            recipe = recipe, itemName = itemName, step = 0,
            progress = 0, progressMax = progressMax,
            quality = 0, qualityMax = qualityMax,
            durability = durabilityMax, durabilityMax = durabilityMax,
            cp = 400, cpMax = 400,
            condition = "稳定", finished = false, hqChance = 1,
        ),
    )
    override val state: StateFlow<CraftState?> = inner
    override val log = MutableStateFlow(listOf("开始制作：$itemName（模拟）"))
    override val cooldown = MutableStateFlow(0)

    private var venerationSteps = 0
    private var innovationSteps = 0
    private var greatStrides = false
    private var ticker: Job? = null

    private val pending = mutableListOf<Pair<SkillDef, Long>>()
    private var worker: Job? = scope.launch {
        var lastTick = System.currentTimeMillis()
        while (isActive && inner.value?.finished == false) {
            delay(100)
            val now = System.currentTimeMillis()
            val delta = now - lastTick
            lastTick = now
            val cd = cooldown.value
            if (cd > 0) cooldown.value = (cd - delta / 1000.0).toInt().coerceAtLeast(0)
            val next = synchronized(pending) { pending.removeFirstOrNull() }
            if (next != null && cd == 0) apply(next.first)
        }
    }

    override fun useSkill(skill: SkillDef) {
        val current = inner.value ?: return
        if (current.finished || cooldown.value > 0 || current.cp < skill.cp) return
        synchronized(pending) { pending.add(skill to System.currentTimeMillis()) }
    }

    private fun apply(skill: SkillDef) {
        val current = inner.value ?: return
        var progress = current.progress
        var quality = current.quality
        var durability = current.durability
        var cp = current.cp
        var condition = current.condition
        var hqChance = current.hqChance

        cooldown.value = 2

        // Conditions rotate randomly like the real thing; 良好 boosts quality.
        val good = condition == "高品质"
        val qualityBoost = (if (good) 1.5f else 1f) * (if (innovationSteps > 0) 1.5f else 1f) *
            (if (greatStrides) 2f else 1f)
        val progressBoost = if (venerationSteps > 0) 1.5f else 1f

        when (skill.kind) {
            SkillDef.Kind.PROGRESS, SkillDef.Kind.PROGRESS_QUALITY -> {
                val potency = when (skill.id) {
                    100001L -> 120f; 100203L -> 180f; 100363L -> if (Math.random() < 0.5) 500f else 0f
                    100403L -> 360f; 100315L -> 400f; 100379L -> 300f; else -> 100f
                }
                progress += (potency * progressBoost).toInt()
                if (skill.kind == SkillDef.Kind.PROGRESS_QUALITY) quality += (100 * qualityBoost).toInt()
                durability -= skill.durability
            }
            SkillDef.Kind.QUALITY -> {
                val potency = when (skill.id) {
                    100002L -> 100f; 100004L -> 125f; 100411L -> 150f
                    100227L -> 100f; 100299L -> 200f; 100128L -> 150f
                    100339L -> 100f + 20 * 3; else -> 100f
                }
                quality += (potency * qualityBoost).toInt()
                if (greatStrides) greatStrides = false
                durability -= skill.durability
            }
            SkillDef.Kind.REPAIR -> durability = when (skill.id) {
                100467L -> durabilityMax
                else -> (durability + 30).coerceAtMost(durabilityMax)
            }
            SkillDef.Kind.BUFF -> when (skill.id) {
                19297L -> venerationSteps = 4
                19004L -> innovationSteps = 4
                260L -> greatStrides = true
            }
            SkillDef.Kind.OTHER -> Unit
        }

        if (skill.id == 19297L) venerationSteps = 4
        if (skill.id == 19004L) innovationSteps = 4
        if (venerationSteps > 0) venerationSteps--
        if (innovationSteps > 0) innovationSteps--

        cp -= skill.cp
        condition = when {
            Math.random() < 0.12 -> "高品质"
            Math.random() < 0.12 -> "低品质"
            else -> "稳定"
        }

        val done = progress >= progressMax || durability <= 0
        if (progress >= progressMax && durability > 0) {
            hqChance = ((quality.toFloat() / qualityMax) * 100).toInt().coerceIn(1, 100)
        }
        val nextState = current.copy(
            step = current.step + 1,
            progress = progress.coerceAtLeast(0),
            quality = quality.coerceIn(0, qualityMax),
            durability = durability.coerceAtLeast(0),
            cp = cp.coerceAtLeast(0),
            condition = condition,
            finished = done,
            hqChance = hqChance,
        )
        inner.value = nextState
        val line = when {
            nextState.finished && progress >= progressMax -> "第${nextState.step}步 ${skill.cn} → 制作完成！HQ 概率 $hqChance%"
            nextState.finished -> "第${nextState.step}步 ${skill.cn} → 耐久耗尽，制作失败"
            else -> "第${nextState.step}步 ${skill.cn}（进展 $progress/$progressMax）"
        }
        log.value = (log.value + line).takeLast(40)
    }

    override fun stop() {
        worker?.cancel()
        ticker?.cancel()
        inner.value?.let { if (!it.finished) inner.value = it.copy(finished = true) }
    }
}
