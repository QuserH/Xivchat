package com.quserh.eorzeaphone.craft.data

import com.quserh.eorzeaphone.data.GameChatMessage
import com.quserh.eorzeaphone.data.GameCraftState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

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
    val effects: CraftEffects = CraftEffects(),
    val cancelPending: Boolean = false,
    val cancelError: String? = null,
    val cancelled: Boolean = false,
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
    /** Explicit user-confirmed cancellation; stop() only disposes the observer. */
    fun cancelCraft()
    fun stop()
}

/**
 * Bridge the workbench uses to drive a real in-game craft through the plugin
 * (ops 30/31/32) and to observe its state pushes (op 40).
 */
interface CraftRemote {
    val lastState: GameCraftState?
    val chat: kotlinx.coroutines.flow.MutableSharedFlow<GameChatMessage>
    fun craftStart(recipeId: Int)
    fun craftSkill(actionId: Long)
    fun craftStop()
    fun craftCancel(recipeId: Int, craftInstanceId: Long)
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
    private val startGame: Boolean,
) : CraftSession {
    override val state = MutableStateFlow<CraftState?>(null)
    override val log = MutableStateFlow(listOf(if (startGame) "正在准备远程制作…" else "已接管游戏内制作"))
    override val cooldown = MutableStateFlow(0)

    private var progressMaxSeen = 0
    private var qualityMaxSeen = 0
    private var lastSkillSentNs = 0L
    private var craftInstanceId = 0L
    private var cancelRequestedNs = 0L
    private val sessionJob = SupervisorJob(scope.coroutineContext[Job])
    private val sessionScope = CoroutineScope(scope.coroutineContext + sessionJob)

    private val worker = sessionScope.launch {
        if (startGame) bridge.craftStart(recipe.id)
        state.value = CraftState(
            recipe = recipe, itemName = itemName, step = 0,
            progress = 0, progressMax = 0, quality = 0, qualityMax = 0,
            durability = 0, durabilityMax = 0, cp = 0, cpMax = 0,
            condition = "等待游戏", finished = false, hqChance = 0,
            remote = true,
        )
        var lastApplied: GameCraftState? = null
        var waitTicks = 0
        while (isActive && state.value?.finished != true) {
            delay(120)
            if (cancelRequestedNs != 0L && System.nanoTime() - cancelRequestedNs > 8_000_000_000L) {
                cancelRequestedNs = 0L
                state.value = state.value?.copy(cancelPending = false, canAct = bridge.lastState?.canAct == true,
                    cancelError = "取消未完成，请检查游戏连接后重试")
            }
            val s = bridge.lastState
            if (s == null || s.recipeId != recipe.id) {
                if (++waitTicks >= 375) {
                    state.value = state.value?.copy(finished = true, canAct = false)
                    log.value = log.value + "未收到本次制作状态，请检查游戏连接"
                }
                continue
            }
            waitTicks = 0
            if (craftInstanceId != 0L && s.craftInstanceId != 0L && s.craftInstanceId != craftInstanceId) {
                state.value = state.value?.copy(finished = true, canAct = false, cancelPending = false)
                break
            }
            if (s.craftInstanceId > 0) craftInstanceId = s.craftInstanceId
            if (s == lastApplied) {
                // An unavailable skill may be rejected without changing game state.
                // Do not leave every control dimmed forever in that case.
                if (state.value?.cancelPending != true && s.canAct && lastSkillSentNs != 0L && System.nanoTime() - lastSkillSentNs > 2_000_000_000L) {
                    state.value = state.value?.copy(canAct = true)
                    lastSkillSentNs = 0L
                }
                continue
            }
            lastApplied = s
            if (s.progress > progressMaxSeen) progressMaxSeen = s.progress
            if (s.quality > qualityMaxSeen) qualityMaxSeen = s.quality
            val qualityMax = s.qualityMax.takeIf { it > 0 } ?: recipe.qmax.takeIf { it > 0 } ?: qualityMaxSeen
            val cancelling = state.value?.cancelPending == true
            val cancelError = state.value?.cancelError
            state.value = CraftState(
                recipe = recipe, itemName = itemName, step = s.step,
                progress = s.progress, progressMax = s.progressMax.takeIf { it > 0 } ?: recipe.pmax.takeIf { it > 0 } ?: progressMaxSeen,
                quality = s.quality, qualityMax = qualityMax,
                durability = s.durability, durabilityMax = s.durabilityMax,
                cp = s.cp, cpMax = s.cpMax,
                condition = mapCondition(s.conditionId),
                finished = s.finished,
                hqChance = if (!recipe.hq) 0 else s.hqChance.takeIf { it in 0..100 }
                    ?: CraftQuality.hqChance(s.quality, qualityMax, recipe.hq),
                remote = true,
                cancelPending = cancelling && !s.finished,
                cancelError = cancelError.takeUnless { s.finished },
                cancelled = cancelling && s.finished,
            ).copy(canAct = s.canAct && !s.finished && !cancelling)
            if (s.finished) {
                log.value = (log.value + "游戏内制作已结束").takeLast(40)
            }
        }
    }

    init {
        // 聊天判定：系统消息“你制作出了/制作失败 + 道具名”比界面轮询更即时可靠。
        sessionScope.launch {
            bridge.chat.collect { msg ->
                val s = state.value ?: return@collect
                if (s.finished) return@collect
                val text = msg.text
                val done = text.contains("制作出了") || text.contains("制作失败") || text.contains("制作中断")
                if (done && text.contains(itemName)) {
                    val success = text.contains("制作出了")
                    state.value = s.copy(finished = true, remote = true, canAct = false,
                        cancelled = s.cancelPending, cancelPending = false)
                    log.value = (log.value + if (success) "检测到完成消息：$text" else "检测到失败消息：$text").takeLast(40)
                }
            }
        }
    }

    override fun useSkill(skill: SkillDef) {
        val current = state.value ?: return
        // Skill availability is driven by the plugin's animation-lock push (canAct):
        // buttons go bright exactly when the character can act again.
        if (current.finished || current.cancelPending || !current.canAct) return
        bridge.craftSkill(skill.id)
        lastSkillSentNs = System.nanoTime()
        // Optimistically dim until the next plugin frame reports canAct again.
        state.value = current.copy(canAct = false)
    }

    override fun cancelCraft() {
        val current = state.value ?: return
        if (current.finished || current.cancelPending) return
        if (current.step > 0 && craftInstanceId == 0L) {
            state.value = current.copy(cancelError = "请先更新游戏插件，当前版本不支持安全取消制作")
            return
        }
        val live = bridge.lastState
        if (live != null && (live.finished || live.recipeId != current.recipe.id ||
                craftInstanceId > 0 && live.craftInstanceId != craftInstanceId)) return
        cancelRequestedNs = System.nanoTime()
        state.value = current.copy(cancelPending = true, cancelError = null, canAct = false)
        bridge.craftCancel(current.recipe.id, craftInstanceId)
    }

    override fun stop() {
        if (state.value?.let { it.step == 0 && !it.finished } == true) bridge.craftStop()
        sessionJob.cancel()
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

    fun startRemote(recipe: CraftRecipe, itemName: String, bridge: CraftRemote, startGame: Boolean = true): CraftSession {
        stop()
        val impl = RemoteCraftSession(scope, recipe, itemName, bridge, startGame)
        session = impl
        return impl
    }

    fun adoptRemote(recipe: CraftRecipe, itemName: String, bridge: CraftRemote): CraftSession =
        startRemote(recipe, itemName, bridge, startGame = false)
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
            condition = "稳定", finished = false, hqChance = CraftQuality.hqChance(0, qualityMax, recipe.hq),
        ),
    )
    override val state: StateFlow<CraftState?> = inner
    override val log = MutableStateFlow(listOf("开始制作：$itemName（模拟）"))
    override val cooldown = MutableStateFlow(0)

    private val sessionScope = scope
    private var ticker: Job? = null

    override fun useSkill(skill: SkillDef) {
        val current = inner.value ?: return
        if (cooldown.value > 0) return
        val nextState = CraftSimulation.apply(current, skill, Math.random(), Math.random()) ?: return
        inner.value = nextState
        cooldown.value = 3
        ticker = sessionScope.launch {
            delay(500)
            cooldown.value = 2
            delay(1000)
            cooldown.value = 1
            delay(1000)
            cooldown.value = 0
        }
        val line = when {
            nextState.finished && nextState.progress >= progressMax -> "第${nextState.step}步 ${skill.cn} → 制作完成！HQ 概率 ${nextState.hqChance}%"
            nextState.finished -> "第${nextState.step}步 ${skill.cn} → 耐久耗尽，制作失败"
            else -> "第${nextState.step}步 ${skill.cn}（进展 ${nextState.progress}/$progressMax）"
        }
        log.value = (log.value + line).takeLast(40)
    }

    override fun cancelCraft() = stop()

    override fun stop() {
        ticker?.cancel()
        inner.value?.let { if (!it.finished) inner.value = it.copy(finished = true) }
    }
}
