package com.quserh.eorzeaphone.craft.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.quserh.eorzeaphone.data.GameCraftStats

data class CraftSimulationStats(
    val cpMax: Int = 400,
    val craftsmanship: Int = 3000,
    val control: Int = 3000,
    val syncedAtUnix: Long = 0,
)

interface CraftStatsPersistence {
    fun load(): Map<Int, CraftSimulationStats>
    fun save(jobId: Int, stats: CraftSimulationStats)
}

/** Uses the phone's existing character partition; no separate copy of character state. */
class PreferenceCraftStatsPersistence(
    private val preferences: () -> SharedPreferences,
    private val unassignedDefaults: SharedPreferences? = null,
) : CraftStatsPersistence {
    override fun load(): Map<Int, CraftSimulationStats> {
        val prefs = preferences()
        return (8..15).mapNotNull { job ->
            val prefix = "craft_stats_job_$job"
            val source = if (prefs.contains("${prefix}_cp")) prefs else
                unassignedDefaults?.takeIf { it.contains("${prefix}_cp") }
            // Values entered before the first game connection remain a fallback;
            // each character's own saved values always win, including after restart.
            source?.let {
                runCatching {
                    job to CraftSimulationStats(
                        cpMax = it.getInt("${prefix}_cp", 400).coerceAtLeast(1),
                        craftsmanship = it.getInt("${prefix}_craftsmanship", 3000).coerceAtLeast(1),
                        control = it.getInt("${prefix}_control", 3000).coerceAtLeast(1),
                        syncedAtUnix = it.getLong("${prefix}_synced_at", 0),
                    )
                }.getOrNull()
            }
        }.toMap()
    }

    override fun save(jobId: Int, stats: CraftSimulationStats) {
        val prefix = "craft_stats_job_$jobId"
        preferences().edit()
            .putInt("${prefix}_cp", stats.cpMax)
            .putInt("${prefix}_craftsmanship", stats.craftsmanship)
            .putInt("${prefix}_control", stats.control)
            .putLong("${prefix}_synced_at", stats.syncedAtUnix)
            .apply()
    }
}

/** One observable source for settings, simulation and incoming game snapshots. */
class CraftStatsStore(
    private val persistence: CraftStatsPersistence,
    private val fallback: CraftSimulationStats = CraftSimulationStats(),
) {
    var byJob by mutableStateOf(persistence.load())
        private set

    fun forJob(jobId: Int): CraftSimulationStats = byJob[jobId] ?: fallback

    fun reload() { byJob = persistence.load() }

    fun saveManual(jobId: Int, cpMax: Int, craftsmanship: Int, control: Int) {
        if (jobId !in 8..15 || cpMax <= 0 || craftsmanship <= 0 || control <= 0) return
        save(jobId, CraftSimulationStats(cpMax, craftsmanship, control))
    }

    fun sync(stats: GameCraftStats, updatedUnix: Long) {
        if (!stats.valid) return
        val current = byJob[stats.jobId]
        val next = CraftSimulationStats(stats.cpMax, stats.craftsmanship, stats.control, updatedUnix.coerceAtLeast(1))
        // Repeated refreshes with unchanged game values must not keep writing preferences.
        if (current != null && current.syncedAtUnix > 0 && current.copy(syncedAtUnix = next.syncedAtUnix) == next) return
        save(stats.jobId, next)
    }

    private fun save(jobId: Int, stats: CraftSimulationStats) {
        if (byJob[jobId] == stats) return
        persistence.save(jobId, stats)
        byJob = byJob + (jobId to stats)
    }
}
