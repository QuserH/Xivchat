package com.quserh.eorzeaphone.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ChatHeaderLayoutTest {
    @Test
    fun twoActionsReserveEqualSpaceOnBothSides() {
        assertEquals(196, chatHeaderTitleMaxWidth(360, 46, 76, 0, 6))
    }

    @Test
    fun localSettingsActionStaysSeparateFromTitle() {
        assertEquals(256, chatHeaderTitleMaxWidth(360, 46, 46, 0, 6))
    }

    @Test
    fun titleIconReservesSpaceWithoutShiftingTextCenter() {
        assertEquals(152, chatHeaderTitleMaxWidth(360, 46, 76, 16, 6))
    }

    @Test
    fun widerNavigationAlsoConstrainsBothSides() {
        assertEquals(188, chatHeaderTitleMaxWidth(360, 80, 46, 0, 6))
    }

    @Test
    fun narrowContainerNeverProducesNegativeConstraints() {
        assertEquals(0, chatHeaderTitleMaxWidth(120, 46, 60, 0, 6))
        assertEquals(0, chatHeaderTitleMaxWidth(0, 0, 0, 0, 6))
    }

    @Test
    fun longAndShortTitlesStayCenteredAndClearOfControls() {
        for (screenWidth in listOf(320, 360, 393, 412, 600, 840)) {
            for (margin in listOf(0, 14, 32, 58)) {
                for (actions in listOf(46, 76, 114)) {
                    for (icon in listOf(0, 16)) {
                        val width = screenWidth - margin * 2
                        val maxTitleWidth = chatHeaderTitleMaxWidth(width, 46, actions, icon, 6)
                        if (maxTitleWidth == 0) continue
                        for (naturalTitleWidth in listOf(40, 120, 280, 600)) {
                            val titleWidth = minOf(naturalTitleWidth, maxTitleWidth)
                            val titleX = (width - titleWidth) / 2
                            val titleRight = titleX + titleWidth
                            val contentRight = titleRight + if (icon > 0) icon + 6 else 0
                            val context = "screen=$screenWidth margin=$margin actions=$actions icon=$icon title=$titleWidth"
                            assertTrue(context, abs(margin + titleX + titleWidth / 2.0 - screenWidth / 2.0) <= 0.5)
                            assertTrue(context, titleX >= 46 + 6)
                            assertTrue(context, contentRight <= width - actions - 6)
                        }
                    }
                }
            }
        }
    }
}
