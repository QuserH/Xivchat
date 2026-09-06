package com.quserh.eorzeaphone.craft.data

data class CraftEffects(
    val innerQuiet: Int = 0,
    val veneration: Int = 0,
    val innovation: Int = 0,
    val greatStrides: Int = 0,
    val muscleMemory: Int = 0,
    val wasteNot: Int = 0,
    val manipulation: Int = 0,
    val finalAppraisal: Int = 0,
    val heartAndSoul: Boolean = false,
    val heartAndSoulUsed: Boolean = false,
    val quickInnovationUsed: Boolean = false,
    val trainedPerfection: Boolean = false,
    val trainedPerfectionUsed: Boolean = false,
    val carefulObservations: Int = 0,
    val expedience: Boolean = false,
    val previousAction: Long = 0,
    val standardTouchCombo: Boolean = false,
)

/** Local practice rules; progress/quality still use the existing estimated base stats. */
internal object CraftSimulation {
    private fun good(state: CraftState) = state.condition == "高品质" || state.condition == "最高品质"

    fun cpCost(state: CraftState, skill: SkillDef): Int = when (skill.canonicalId) {
        100004L -> if (state.effects.previousAction == 100002L) 18 else skill.cp
        100411L -> if (state.effects.standardTouchCombo || state.effects.previousAction == 100010L) 18 else skill.cp
        else -> skill.cp
    }

    fun durabilityCost(state: CraftState, skill: SkillDef): Int = when {
        state.effects.trainedPerfection -> 0
        state.effects.wasteNot > 0 -> (skill.durability + 1) / 2
        else -> skill.durability
    }

    fun canUse(state: CraftState, skill: SkillDef): Boolean {
        if (state.finished || state.cp < cpCost(state, skill)) return false
        val effects = state.effects
        return when (skill.canonicalId) {
            100379L, 100387L -> state.step == 0
            100283L -> state.step == 0 && state.recipe.craftLv <= 90 && state.recipe.stars == 0
            100315L, 100128L, 100371L -> good(state) || effects.heartAndSoul
            100227L, 100427L -> effects.wasteNot == 0
            100435L -> effects.innerQuiet == 10
            100339L -> effects.innerQuiet > 0
            100451L -> effects.expedience
            100395L -> effects.carefulObservations < 3
            100419L -> !effects.heartAndSoulUsed
            100459L -> !effects.quickInnovationUsed && effects.innovation == 0
            100475L -> !effects.trainedPerfectionUsed
            else -> true
        }
    }

    fun apply(state: CraftState, skill: SkillDef, successRoll: Double, conditionRoll: Double): CraftState? {
        if (!canUse(state, skill)) return null
        val id = skill.canonicalId
        val before = state.effects
        val consumesStep = id !in setOf(100395L, 19012L, 100419L, 100459L)
        val success = successRoll < when (id) {
            100363L -> 0.5
            100355L, 100451L -> 0.6
            else -> 1.0
        }
        val progressAction = skill.kind == SkillDef.Kind.PROGRESS || skill.kind == SkillDef.Kind.PROGRESS_QUALITY
        val qualityAction = skill.kind == SkillDef.Kind.QUALITY || skill.kind == SkillDef.Kind.PROGRESS_QUALITY
        val qualityFactor = when (state.condition) {
            "高品质" -> 1.5
            "最高品质" -> 4.0
            "低品质" -> 0.5
            else -> 1.0
        } * (1.0 + before.innerQuiet * 0.1) *
            (1.0 + (if (before.innovation > 0) 0.5 else 0.0) + (if (before.greatStrides > 0) 1.0 else 0.0))
        val progressFactor = 1.0 + (if (before.veneration > 0) 0.5 else 0.0) +
            (if (before.muscleMemory > 0) 1.0 else 0.0)
        val progressPotency = when (id) {
            100001L -> 120
            100203L, 100427L -> 180
            100363L -> 500
            100403L -> if (state.durability < durabilityCost(state, skill)) 180 else 360
            100315L -> 400
            100379L -> 300
            100323L -> 150
            else -> 0
        }
        val qualityPotency = when (id) {
            100004L -> 125
            100411L, 100128L, 100451L -> 150
            100299L -> 200
            100387L -> 300
            100339L -> 100 + 20 * before.innerQuiet
            else -> if (qualityAction) 100 else 0
        }
        var progress = state.progress + if (success && progressAction) (progressPotency * progressFactor).toInt() else 0
        val quality = when {
            id == 100283L -> state.qualityMax
            success && qualityAction -> state.quality + (qualityPotency * qualityFactor).toInt()
            else -> state.quality
        }.coerceIn(0, state.qualityMax)
        val preventedCompletion = before.finalAppraisal > 0 && progress >= state.progressMax
        if (preventedCompletion) progress = (state.progressMax - 1).coerceAtLeast(0)

        var durability = state.durability - durabilityCost(state, skill)
        if (durability > 0) {
            if (id == 100003L) durability += 30
            if (id == 100467L) durability = state.durabilityMax
            if (before.manipulation > 0 && id != 4574L && consumesStep && progress < state.progressMax) durability += 5
        }
        durability = durability.coerceIn(0, state.durabilityMax)
        val cp = (state.cp - cpCost(state, skill) + if (id == 100371L) 20 else 0).coerceIn(0, state.cpMax)
        var innerQuiet = before.innerQuiet
        if (success && qualityAction && id != 100283L) {
            val extra = id in setOf(100128L, 100299L, 100387L) || id == 100443L && before.previousAction == 100002L
            innerQuiet = if (id == 100339L) 0 else (innerQuiet + if (extra) 2 else 1).coerceAtMost(10)
        }
        fun remaining(turns: Int) = (turns - if (consumesStep) 1 else 0).coerceAtLeast(0)
        val effects = before.copy(
            innerQuiet = innerQuiet,
            veneration = if (id == 19297L) 4 else remaining(before.veneration),
            innovation = when (id) { 19004L -> 4; 100459L -> 1; else -> remaining(before.innovation) },
            greatStrides = when { id == 260L -> 3; qualityAction -> 0; else -> remaining(before.greatStrides) },
            muscleMemory = when { id == 100379L -> 5; progressAction -> 0; else -> remaining(before.muscleMemory) },
            wasteNot = when (id) { 4631L -> 4; 4639L -> 8; else -> remaining(before.wasteNot) },
            manipulation = if (id == 4574L) 8 else remaining(before.manipulation),
            finalAppraisal = when { id == 19012L -> 5; preventedCompletion -> 0; else -> remaining(before.finalAppraisal) },
            heartAndSoul = id == 100419L || before.heartAndSoul && (good(state) || id !in setOf(100315L, 100128L, 100371L)),
            heartAndSoulUsed = before.heartAndSoulUsed || id == 100419L,
            quickInnovationUsed = before.quickInnovationUsed || id == 100459L,
            trainedPerfection = id == 100475L || before.trainedPerfection && skill.durability == 0,
            trainedPerfectionUsed = before.trainedPerfectionUsed || id == 100475L,
            carefulObservations = before.carefulObservations + if (id == 100395L) 1 else 0,
            expedience = if (consumesStep) id == 100355L && success else before.expedience,
            previousAction = id,
            standardTouchCombo = id == 100004L && before.previousAction == 100002L,
        )
        val condition = when {
            !consumesStep && id != 100395L -> state.condition
            state.condition == "最高品质" -> "低品质"
            state.condition == "高品质" || state.condition == "低品质" -> "稳定"
            conditionRoll < 0.04 -> "最高品质"
            conditionRoll < 0.24 -> "高品质"
            else -> "稳定"
        }
        return state.copy(
            step = state.step + if (consumesStep) 1 else 0,
            progress = progress.coerceIn(0, state.progressMax), quality = quality,
            durability = durability, cp = cp, condition = condition, effects = effects,
            finished = progress >= state.progressMax || durability == 0,
            hqChance = CraftQuality.hqChance(quality, state.qualityMax, state.recipe.hq),
        )
    }
}
