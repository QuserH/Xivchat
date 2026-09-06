package com.quserh.eorzeaphone.craft.data

import org.junit.Assert.*
import org.junit.Test

class CraftSimulationTest {
    private fun initial(job: Int = 0) = CraftState(
        CraftRecipe(1, job, 1, 1, 80, 0, 1, true, true, 10000, 10000),
        "Item", 0, 0, 10000, 0, 10000, 60, 60, 600, 600, "稳定", false, 1,
    )
    private fun skill(id: Long, job: Int = 0) = CraftSkills.forJob(job).first { it.canonicalId == id }
    private fun use(state: CraftState, id: Long) = CraftSimulation.apply(state, skill(id, state.recipe.job), 0.0, 0.8)!!

    @Test
    fun nonCarpenterSkillsUseSameEffects() {
        assertEquals(use(initial(), 100203).progress, use(initial(3), 100203).progress)
        assertEquals(use(initial(), 100467).durability, use(initial(7), 100467).durability)
    }

    @Test
    fun touchComboUsesDiscountOnlyInCorrectSequence() {
        val base = initial()
        assertEquals(32, CraftSimulation.cpCost(base, skill(100004)))
        assertEquals(46, CraftSimulation.cpCost(base, skill(100411)))
        val basic = use(base, 100002)
        assertEquals(18, CraftSimulation.cpCost(basic, skill(100004)))
        val standard = use(basic, 100004)
        assertEquals(18, CraftSimulation.cpCost(standard, skill(100411)))
        assertEquals(46, CraftSimulation.cpCost(use(base, 100004), skill(100411)))
        assertEquals(18, CraftSimulation.cpCost(use(base, 100010), skill(100411)))
    }

    @Test
    fun wasteNotAndManipulationHaveRealEffects() {
        val wasteNot = use(initial(), 4631)
        assertEquals(4, wasteNot.effects.wasteNot)
        assertEquals(55, use(wasteNot, 100002).durability)
        assertFalse(CraftSimulation.canUse(wasteNot, skill(100427)))
        val manipulation = use(initial(), 4574)
        assertEquals(8, manipulation.effects.manipulation)
        assertEquals(55, use(manipulation, 100002).durability)
    }

    @Test
    fun trainedPerfectionAndRepairWorkOnceOrRestoreFully() {
        val perfection = use(initial(), 100475)
        assertFalse(CraftSimulation.canUse(perfection, skill(100475)))
        val ground = use(perfection, 100403)
        assertEquals(60, ground.durability)
        assertFalse(ground.effects.trainedPerfection)
        assertEquals(60, use(initial().copy(durability = 5), 100467).durability)
        assertEquals(35, use(initial().copy(durability = 5), 100003).durability)
    }

    @Test
    fun specialistActionsCheckLimitsWithoutConsumingSteps() {
        val soul = use(initial(), 100419)
        assertEquals(0, soul.step)
        assertFalse(CraftSimulation.canUse(soul, skill(100419)))
        val intensive = use(soul, 100315)
        assertTrue(intensive.progress > 0)
        assertFalse(intensive.effects.heartAndSoul)
        val quick = use(initial(), 100459)
        assertEquals(0, quick.step)
        assertEquals(1, quick.effects.innovation)
        assertFalse(CraftSimulation.canUse(quick, skill(100459)))
        var observed = initial()
        repeat(3) { observed = use(observed, 100395) }
        assertEquals(0, observed.step)
        assertFalse(CraftSimulation.canUse(observed, skill(100395)))
    }

    @Test
    fun innerQuietAndConditionalSkillsAreEnforced() {
        assertFalse(CraftSimulation.canUse(initial(), skill(100435)))
        assertFalse(CraftSimulation.canUse(initial(), skill(100339)))
        val refined = use(use(initial(), 100002), 100443)
        assertEquals(3, refined.effects.innerQuiet)
        assertEquals(0, use(refined, 100339).effects.innerQuiet)
        assertFalse(CraftSimulation.canUse(initial(), skill(100451)))
        assertTrue(CraftSimulation.canUse(use(initial(), 100355), skill(100451)))
        assertFalse(CraftSimulation.canUse(use(initial(), 100002), skill(100387)))
    }

    @Test
    fun finalAppraisalPreventsCompletionAndCpRecoveryIsCapped() {
        val appraised = use(initial().copy(progress = 9999), 19012)
        val crafted = use(appraised, 100001)
        assertEquals(9999, crafted.progress)
        assertEquals(0, crafted.effects.finalAppraisal)
        assertFalse(crafted.finished)
        assertEquals(600, use(initial().copy(cp = 595, condition = "高品质"), 100371).cp)
    }

    @Test
    fun failedHastyTouchDoesNotGrantStacksOrExpedience() {
        val next = CraftSimulation.apply(initial(), skill(100355), 0.99, 0.8)!!
        assertEquals(0, next.quality)
        assertEquals(0, next.effects.innerQuiet)
        assertFalse(next.effects.expedience)
        assertEquals(50, next.durability)
    }

    @Test
    fun progressOnLastDurabilityCanCompleteSuccessfully() {
        val next = use(initial().copy(progress = 9999, durability = 10), 100001)
        assertTrue(next.finished)
        assertEquals(10000, next.progress)
        assertEquals(0, next.durability)
    }
}
