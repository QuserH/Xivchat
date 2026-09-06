package com.quserh.eorzeaphone.craft.data

import com.quserh.eorzeaphone.data.GameChatMessage
import com.quserh.eorzeaphone.data.GameCraftState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

class RemoteCraftSessionTest {
    private val recipe = CraftRecipe(500, 0, 100, 1, 90, 0, 1, true, false, 2000, 4000)
    private fun frame(recipeId: Int = 500, step: Int = 2) =
        GameCraftState(recipeId, step, 200, 0, 300, 0, 40, 60, 300, 500, 0, false, true)

    private class Bridge : CraftRemote {
        override var lastState: GameCraftState? = null
        override val chat = MutableSharedFlow<GameChatMessage>(extraBufferCapacity = 1)
        var starts = 0
        var stops = 0
        val actions = mutableListOf<Long>()
        val cancellations = mutableListOf<Pair<Int, Long>>()
        override fun craftStart(recipeId: Int) { starts++ }
        override fun craftSkill(actionId: Long) { actions += actionId }
        override fun craftStop() { stops++ }
        override fun craftCancel(recipeId: Int, craftInstanceId: Long) { cancellations += recipeId to craftInstanceId }
        override fun craftFood(itemId: Int) = Unit
    }

    @Test fun skillsFollowPluginCanActAndDoNotUseLocalPracticeRules() = runBlocking {
        val bridge = Bridge().also { it.lastState = frame().copy(canAct = false, cp = 0) }
        val session = RemoteCraftSession(this, recipe, "道具", bridge, false)
        val skill = CraftSkills.forJob(recipe.job).first { it.canonicalId == 100002L }
        try {
            withTimeout(3000) { session.state.filterNotNull().first { it.step == 2 } }
            session.useSkill(skill)
            assertTrue(bridge.actions.isEmpty())

            // The plugin owns remote availability, even when local CP rules would reject it.
            bridge.lastState = frame(step = 3).copy(canAct = true, cp = 0)
            withTimeout(3000) { session.state.filterNotNull().first { it.step == 3 && it.canAct } }
            session.useSkill(skill)
            session.useSkill(skill)
            assertEquals(listOf(skill.id), bridge.actions)
            assertFalse(session.state.value!!.canAct)

            bridge.lastState = frame(step = 4).copy(finished = true)
            withTimeout(3000) { session.state.filterNotNull().first { it.finished } }
            session.useSkill(skill)
            assertEquals(listOf(skill.id), bridge.actions)
        } finally { session.stop() }
    }

    @Test fun adoptsWithoutStartingAnotherGameCraftAndUsesRecipeCaps() = runBlocking {
        val bridge = Bridge().also { it.lastState = frame() }
        val session = RemoteCraftSession(this, recipe, "道具", bridge, false)
        try {
            val current = withTimeout(3000) { session.state.filterNotNull().first { it.step == 2 } }
            assertEquals(0, bridge.starts)
            assertEquals(2000, current.progressMax)
            assertEquals(4000, current.qualityMax)
        } finally { session.stop() }
    }

    @Test fun ignoresAnotherRecipeUntilMatchingStateArrives() = runBlocking {
        val bridge = Bridge().also { it.lastState = frame(recipeId = 999) }
        val session = RemoteCraftSession(this, recipe, "道具", bridge, true)
        try {
            delay(180)
            assertEquals(0, session.state.value!!.step)
            bridge.lastState = frame(step = 3)
            withTimeout(3000) { session.state.filterNotNull().first { it.step == 3 } }
            assertEquals(1, bridge.starts)
        } finally { session.stop() }
    }

    @Test fun stoppingSessionRemovesItsChatCollector() = runBlocking {
        val bridge = Bridge()
        val session = RemoteCraftSession(this, recipe, "道具", bridge, true)
        try {
            withTimeout(3000) { bridge.chat.subscriptionCount.first { it == 1 } }
        } finally { session.stop() }
        withTimeout(3000) { bridge.chat.subscriptionCount.first { it == 0 } }
        assertEquals(1, bridge.stops)
    }

    @Test fun liveGameCapsAndHqChanceOverrideTheRecipeFallback() = runBlocking {
        val bridge = Bridge().also { it.lastState = frame().copy(qualityMax = 16500, hqChance = 47, craftInstanceId = 77) }
        val session = RemoteCraftSession(this, recipe, "道具", bridge, false)
        try {
            val current = withTimeout(3000) { session.state.filterNotNull().first { it.step == 2 } }
            assertEquals(16500, current.qualityMax)
            assertEquals(47, current.hqChance)
        } finally { session.stop() }
        assertTrue(bridge.cancellations.isEmpty())
        assertEquals(0, bridge.stops)
    }

    @Test fun confirmedCancellationIsScopedAndWaitsForGameAcknowledgement() = runBlocking {
        val bridge = Bridge().also { it.lastState = frame().copy(craftInstanceId = 77) }
        val session = RemoteCraftSession(this, recipe, "道具", bridge, false)
        try {
            withTimeout(3000) { session.state.filterNotNull().first { it.step == 2 } }
            session.cancelCraft()
            session.cancelCraft()
            assertEquals(listOf(500 to 77L), bridge.cancellations)
            assertTrue(session.state.value!!.cancelPending)
            assertFalse(session.state.value!!.finished)
            assertFalse(session.state.value!!.canAct)
            bridge.lastState = bridge.lastState!!.copy(finished = true)
            val ended = withTimeout(3000) { session.state.filterNotNull().first { it.finished } }
            assertTrue(ended.cancelled)
            assertFalse(ended.cancelPending)
        } finally { session.stop() }
    }

    @Test fun legacyPluginCannotAccidentallyCancelAnotherCraft() = runBlocking {
        val bridge = Bridge().also { it.lastState = frame() }
        val session = RemoteCraftSession(this, recipe, "道具", bridge, false)
        try {
            withTimeout(3000) { session.state.filterNotNull().first { it.step == 2 } }
            session.cancelCraft()
            assertTrue(bridge.cancellations.isEmpty())
            assertNotNull(session.state.value!!.cancelError)
        } finally { session.stop() }
    }

    @Test fun aLaterCraftOfTheSameRecipeIsNotCancelledByAnOldConfirmation() = runBlocking<Unit> {
        val bridge = Bridge().also { it.lastState = frame().copy(craftInstanceId = 77) }
        val session = RemoteCraftSession(this, recipe, "道具", bridge, false)
        try {
            withTimeout(3000) { session.state.filterNotNull().first { it.step == 2 } }
            bridge.lastState = frame().copy(craftInstanceId = 78)
            session.cancelCraft()
            assertTrue(bridge.cancellations.isEmpty())
            withTimeout(3000) { session.state.filterNotNull().first { it.finished } }
        } finally { session.stop() }
    }
}
