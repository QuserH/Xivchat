package com.quserh.eorzeaphone.craft.data

import com.quserh.eorzeaphone.data.GameCraftStats
import org.junit.Assert.*
import org.junit.Test

class CraftStatsStoreTest {
    @Test fun recipeIndicesMapToStableGameJobIds() {
        (0..7).forEach { recipeJob ->
            assertEquals(recipeJob + 8, CraftJobs.gameJobId(recipeJob))
            assertEquals(recipeJob, CraftJobs.recipeJob(CraftJobs.gameJobId(recipeJob)))
        }
        assertEquals(0, CraftJobs.gameJobId(8))
        assertNull(CraftJobs.recipeJob(7))
        assertNull(CraftJobs.recipeJob(16))
    }

    private class MemoryPersistence : CraftStatsPersistence {
        var character = "A"
        val records = mutableMapOf<String, Map<Int, CraftSimulationStats>>()
        var writes = 0
        override fun load() = records[character].orEmpty()
        override fun save(jobId: Int, stats: CraftSimulationStats) {
            writes++
            records[character] = load() + (jobId to stats)
        }
    }

    @Test fun jobsKeepTheirOwnStatsAcrossReloads() {
        val disk = MemoryPersistence()
        val store = CraftStatsStore(disk)
        store.saveManual(8, 600, 4000, 3900)
        store.saveManual(15, 650, 4800, 4700)
        val restored = CraftStatsStore(disk)
        assertEquals(CraftSimulationStats(600, 4000, 3900), restored.forJob(8))
        assertEquals(CraftSimulationStats(650, 4800, 4700), restored.forJob(15))
        assertEquals(CraftSimulationStats(), restored.forJob(9))
    }

    @Test fun gameSyncPersistsOnlyTheReportedJobAndDeduplicates() {
        val disk = MemoryPersistence()
        val store = CraftStatsStore(disk)
        store.saveManual(8, 600, 4000, 3900)
        val game = GameCraftStats(15, 5100, 5000, 700)
        store.sync(game, 100)
        store.sync(game, 200)
        assertEquals(2, disk.writes)
        assertEquals(700, CraftStatsStore(disk).forJob(15).cpMax)
        assertEquals(100L, store.forJob(15).syncedAtUnix)
        assertEquals(600, store.forJob(8).cpMax)
        store.sync(game.copy(craftsmanship = 5200), 300)
        assertEquals(5200, store.forJob(15).craftsmanship)
        assertEquals(3, disk.writes)
    }

    @Test fun badPacketsAndWrongJobIdDoNotOverwriteSavedStats() {
        val disk = MemoryPersistence()
        val store = CraftStatsStore(disk)
        store.saveManual(8, 600, 4000, 3900)
        store.sync(GameCraftStats(8, 0, 5000, 700), 123)
        store.sync(GameCraftStats(7, 5000, 5000, 700), 123)
        store.saveManual(0, 700, 5000, 5000)
        store.saveManual(8, 0, 5000, 5000)
        assertEquals(1, disk.writes)
        assertEquals(600, store.forJob(8).cpMax)
    }

    @Test fun characterSwitchReloadsTheExistingPhonePartition() {
        val disk = MemoryPersistence()
        val store = CraftStatsStore(disk)
        store.saveManual(8, 600, 4000, 3900)
        disk.character = "B"
        store.reload()
        assertEquals(CraftSimulationStats(), store.forJob(8))
        store.sync(GameCraftStats(8, 5100, 5000, 700), 100)
        disk.character = "A"
        store.reload()
        assertEquals(600, store.forJob(8).cpMax)
    }

    @Test fun legacyValuesRemainFallbackUntilAJobIsConfigured() {
        val legacy = CraftSimulationStats(580, 3800, 3700)
        val store = CraftStatsStore(MemoryPersistence(), legacy)
        assertEquals(legacy, store.forJob(8))
        assertEquals(legacy, store.forJob(15))
        store.saveManual(8, 600, 4100, 4000)
        assertEquals(legacy, store.forJob(15))
    }
}
