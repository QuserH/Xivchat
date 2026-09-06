package com.quserh.eorzeaphone.data.wiki

import org.junit.Assert.*
import org.junit.Test

class GatherClockTest {
    private fun node(kind: Int, type: String = "") = GatherNode(
        1, kind, 100, 0, "地图", "地区", "地域", 20f, 20f,
        listOf(0, 12), 120, "", emptyList(), kindName = type,
    )

    @Test fun bothMiningAndBothBotanyNodeShapesHaveTheirJob() {
        for (kind in 0..1) assertEquals(GatherJobCategory.MINING, node(kind).jobCategory)
        for (kind in 2..3) assertEquals(GatherJobCategory.BOTANY, node(kind).jobCategory)
        assertEquals(GatherJobCategory.FISHING, node(5).jobCategory)
        assertEquals(GatherJobCategory.OTHER, node(99).jobCategory)
    }

    @Test fun nodeTypesFollowActualDatabaseLabels() {
        assertEquals(GatherNodeType.TIMED, node(0, "限时").typeCategory)
        assertEquals(GatherNodeType.LEGENDARY, node(2, "传说").typeCategory)
        assertEquals(GatherNodeType.UNKNOWN, node(1, "未知").typeCategory)
        assertEquals(GatherNodeType.OTHER, node(5, "鱼影").typeCategory)
    }

    @Test fun levelRangeBoundariesDoNotOverlap() {
        for (level in 1..100) assertEquals(1, GatherLevelRange.entries.drop(1).count { it.includes(level) })
        assertTrue(GatherLevelRange.LV50.includes(50))
        assertFalse(GatherLevelRange.LV50.includes(51))
        assertTrue(GatherLevelRange.ALL.includes(110))
    }

    @Test fun activeWindowCrossesMidnight() {
        val now = (24.5 * 3_600_000 / EorzeaTime.FACTOR).toLong()
        val remaining = EorzeaTime.nextWindowMs(listOf(23), 120, now)
        assertTrue(remaining < 0)
        assertEquals(EorzeaTime.etMinutesToRealMs(30).toDouble(), -remaining.toDouble(), 2.0)
    }

    @Test fun permanentOrInvalidWindowDoesNotEnterClockRanking() {
        assertEquals(Long.MAX_VALUE, EorzeaTime.nextWindowMs(emptyList(), 120, 0))
        assertEquals(Long.MAX_VALUE, EorzeaTime.nextWindowMs(listOf(0), 0, 0))
    }

    @Test fun closedWindowFindsNextOccurrence() {
        val now = (3 * 3_600_000L / EorzeaTime.FACTOR).toLong()
        val wait = EorzeaTime.nextWindowMs(listOf(0, 12), 120, now)
        assertEquals(EorzeaTime.etMinutesToRealMs(9 * 60).toDouble(), wait.toDouble(), 2.0)
    }
}
