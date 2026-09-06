package com.quserh.eorzeaphone.craft.data

import org.junit.Assert.*
import org.junit.Test

class CraftQualityTest {
    @Test fun archeoKingdomScepterUsesItsRecipeFactor() {
        assertEquals(16500, recipeCap(15000, 110))
        assertEquals(7500, recipeCap(7500, 100))
        assertEquals(50, recipeCap(75, 67))
        assertEquals(0, recipeCap(15000, 0))
    }

    @Test fun hqChanceIsNotTheQualityPercentage() {
        assertEquals(1, CraftQuality.hqChance(0, 16500, true))
        assertEquals(15, CraftQuality.hqChance(8250, 16500, true))
        assertEquals(47, CraftQuality.hqChance(12375, 16500, true))
        assertEquals(100, CraftQuality.hqChance(16500, 16500, true))
    }

    @Test fun handlesUnknownNonHqAndOutOfRangeValues() {
        assertEquals(-1, CraftQuality.hqChance(0, 0, true))
        assertEquals(0, CraftQuality.hqChance(100, 100, false))
        assertEquals(1, CraftQuality.hqChance(-5, 100, true))
        assertEquals(100, CraftQuality.hqChance(Int.MAX_VALUE, 16500, true))
        val chances = (0..100).map { CraftQuality.hqChance(it, 100, true) }
        assertTrue(chances.zipWithNext().all { (a, b) -> a <= b })
    }
}
