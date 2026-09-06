package com.quserh.eorzeaphone.craft.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class MockCraftSessionTest {
    private val recipe = CraftRecipe(1, 0, 1, 1, 80, 0, 1, true, true, 10000, 10000)
    private fun skill(id: Long) = CraftSkills.forJob(recipe.job).first { it.canonicalId == id }

    @Test
    fun consecutiveSkillsApplyImmediatelyWithoutCooldownOrRemoteCanAct() = runBlocking {
        val engine = MockCraftEngine(this)
        val session = engine.startMock(recipe, "道具", cpMax = 600)
        try {
            assertFalse(session.state.value!!.canAct)
            session.useSkill(skill(100001))
            val first = session.state.value!!
            assertEquals(1, first.step)
            assertEquals(0, session.cooldown.value)

            // Intentionally do not advance time: practice mode has no game animation lock.
            session.useSkill(skill(100001))
            val second = session.state.value!!
            assertEquals(2, second.step)
            assertEquals(first.progress * 2, second.progress)
            assertEquals(first.durability - 10, second.durability)
            assertEquals(0, session.cooldown.value)
        } finally { engine.stop() }
    }

    @Test
    fun unavailableSkillAndStoppedSessionDoNotAdvanceOrAppendLogs() = runBlocking {
        val engine = MockCraftEngine(this)
        val session = engine.startMock(recipe, "道具", cpMax = 1)
        try {
            val initial = session.state.value
            val initialLog = session.log.value
            session.useSkill(skill(100002))
            assertEquals(initial, session.state.value)
            assertEquals(initialLog, session.log.value)

            session.stop()
            val stopped = session.state.value
            session.useSkill(skill(100001))
            assertEquals(stopped, session.state.value)
            assertEquals(initialLog, session.log.value)
        } finally { engine.stop() }
    }
}
