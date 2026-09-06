package com.quserh.eorzeaphone.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryMenuPositionTest {
    @Test
    fun rightEdgeKeepsWholeMenuOnScreen() {
        val position = InventoryMenuPositionProvider(Offset(390f, 200f), 8)
            .calculatePosition(IntRect.Zero, IntSize(400, 800), LayoutDirection.Ltr, IntSize(200, 112))
        assertEquals(IntOffset(192, 200), position)
    }

    @Test
    fun bottomEdgeOpensAboveTouchAndAvoidsSystemBar() {
        val position = InventoryMenuPositionProvider(Offset(390f, 765f), 8, 24, 32)
            .calculatePosition(IntRect.Zero, IntSize(400, 800), LayoutDirection.Ltr, IntSize(200, 112))
        assertEquals(IntOffset(192, 648), position)
    }

    @Test
    fun measuredSizeHandlesLargeFontsAndBothDirections() {
        for (width in listOf(320, 393, 600)) {
            for (height in listOf(480, 800)) {
                for (menuHeight in listOf(112, 180, 280)) {
                    for (direction in LayoutDirection.entries) {
                        for (touch in listOf(Offset.Zero, Offset(width - 1f, height - 1f))) {
                            val position = InventoryMenuPositionProvider(touch, 8, 24, 32)
                                .calculatePosition(IntRect.Zero, IntSize(width, height), direction, IntSize(200, menuHeight))
                            assertTrue(position.x >= 8 && position.x + 200 <= width - 8)
                            assertTrue(position.y >= 32 && position.y + menuHeight <= height - 40)
                        }
                    }
                }
            }
        }
    }
}
