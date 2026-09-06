package com.quserh.eorzeaphone.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.data.GameChatChunk
import com.quserh.eorzeaphone.data.GameChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class ChatBubbleContentCacheTest {
    private fun key(text: String = "Message") = ChatBubbleContentKey(
        message = GameChatMessage(1000L, "Sender", text, 27),
        author = "Sender",
        selfEmoteFull = false,
        color = Color.Black,
        forceColor = false,
        highlight = "",
        light = true,
        fontSizeSp = 14,
    )

    @Test
    fun returningToMessageReusesParsedContent() {
        val cache = ChatBubbleContentCache()
        val original = cache[key()]
        assertSame(original, cache[key()])
    }

    @Test
    fun appearanceChangesDoNotReuseStaleContent() {
        val cache = ChatBubbleContentCache()
        val key = key()
        val original = cache[key]
        for (changed in listOf(
            key.copy(color = Color.Red),
            key.copy(fontSizeSp = 20),
            key.copy(light = false),
            key.copy(forceColor = true),
            key.copy(highlight = "Message"),
            key.copy(author = "Other"),
            key.copy(selfEmoteFull = true),
        )) {
            assertNotSame(original, cache[changed])
        }
        assertSame(original, cache[key])
    }

    @Test
    fun messageReplacementRefreshesText() {
        val cache = ChatBubbleContentCache()
        assertEquals("Message", cache[key()]!!.ink.annotated.text)
        assertEquals("Updated", cache[key("Updated")]!!.ink.annotated.text)
    }

    @Test
    fun cacheIsBoundedAndKeepsRecentlyUsedEntries() {
        val cache = ChatBubbleContentCache(capacity = 2)
        val first = cache[key("First")]
        val second = cache[key("Second")]
        assertSame(first, cache[key("First")])
        cache[key("Third")]
        assertSame(first, cache[key("First")])
        assertNotSame(second, cache[key("Second")])
    }

    @Test
    fun inlineIconsAndFontSizeSurviveCaching() {
        val cache = ChatBubbleContentCache()
        val key = key().let {
            it.copy(message = it.message.copy(chunks = listOf(GameChatChunk(icon = 77), GameChatChunk(text = "Message"))))
        }
        val original = cache[key]!!
        assertFalse(original.inline.isEmpty())
        assertEquals(14.sp, original.ink.placeholders.single().item.width)
        assertEquals(20.sp, cache[key.copy(fontSizeSp = 20)]!!.ink.placeholders.single().item.width)
        assertSame(original, cache[key])
    }

    @Test
    fun searchHighlightIsNotLeftOnUnhighlightedMessages() {
        val cache = ChatBubbleContentCache()
        val key = key()
        val plain = cache[key]!!
        val highlighted = cache[key.copy(highlight = "Message")]!!
        assertFalse(plain.ink.annotated.spanStyles.any { it.item.background == Color(0x66FFEB3B) })
        assertEquals(true, highlighted.ink.annotated.spanStyles.any { it.item.background == Color(0x66FFEB3B) })
        assertSame(plain, cache[key])
    }
}
