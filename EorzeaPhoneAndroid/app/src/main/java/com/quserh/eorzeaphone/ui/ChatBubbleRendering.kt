package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.data.ChatCategory
import com.quserh.eorzeaphone.data.GameChatChunk
import com.quserh.eorzeaphone.data.GameChatMessage

internal class ChatInk(
    val annotated: AnnotatedString,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
)

internal data class ChatBubbleContentKey(
    val message: GameChatMessage,
    val author: String,
    val selfEmoteFull: Boolean,
    val color: Color,
    val forceColor: Boolean,
    val highlight: String,
    val light: Boolean,
    val fontSizeSp: Int,
)

internal class ChatBubbleContent(val ink: ChatInk, val inline: Map<String, InlineTextContent>)

internal class ChatBubbleContentCache(capacity: Int = CHAT_TEXT_LAYOUT_CACHE_SIZE) :
    androidx.collection.LruCache<ChatBubbleContentKey, ChatBubbleContent>(capacity) {
    override fun create(key: ChatBubbleContentKey): ChatBubbleContent {
        // Row-local remember is discarded offscreen. Retain parsed spans and inline
        // icon lambdas alongside the bounded paragraph cache for reverse scrolling.
        val message = key.message
        val rawText = if (key.selfEmoteFull) key.author + message.text else message.text
        val cleaned = cleanChatText(rawText, if (key.selfEmoteFull) "" else key.author).ifBlank { " " }
        val chunks = if (message.category == ChatCategory.Emote) message.chunks.map { it.copy(italic = false) } else message.chunks
        val renderChunks = cleanItemLinkChunks(chunks)
        val fontUnit = key.fontSizeSp.sp
        val lineUnit = (key.fontSizeSp + 5).sp
        return ChatBubbleContent(
            chatBubbleInk(renderChunks, cleaned, key.color, key.forceColor, key.highlight, key.light, fontUnit, lineUnit, ChatAxisFont, alreadyCleaned = true),
            chatBubbleInline(renderChunks, cleaned, fontUnit, lineUnit, alreadyCleaned = true),
        )
    }
}

internal fun cleanItemLinkChunks(chunks: List<GameChatChunk>): List<GameChatChunk> {
    // 只重建“图标后连续含 PUA/� 的道具链接簇”：拿到完整名 + HQ，其余句子按原顺序保留，避免把长句误当道具名打乱顺序。
    val iconIdx = chunks.indexOfFirst { it.icon == 0xE0BB }
    if (iconIdx < 0) return chunks
    val head = chunks.take(iconIdx)
    val icon = chunks[iconIdx]
    val rest = chunks.drop(iconIdx + 1)
    fun isMarker(t: String) = t.any { it.code in 0xE000..0xF8FF || it.code == 0xFFFD || it.code == 0xE0BB } || t.isBlank()
    var clusterEnd = 0
    for (i in rest.indices) { val t = rest[i].text.orEmpty(); if (i == 0 || isMarker(t)) clusterEnd = i + 1 else break }
    val cluster = rest.take(clusterEnd)
    val tail = rest.drop(clusterEnd)
    fun clean(s: String?) = (s ?: "").filter { it.code !in 0xE000..0xF8FF && it.code != 0xFFFD }
    val itemName = cluster.map { clean(it.text) }.maxByOrNull { it.length } ?: ""
    val glyphBuilder = StringBuilder()
    for (c in cluster) glyphBuilder.append((c.text.orEmpty()).filter { it.code in 0xE000..0xF8FF && it.code != 0xE0BB })
    val out = ArrayList<GameChatChunk>()
    out.addAll(head)
    out.add(icon)
    if (itemName.isNotEmpty()) {
        val ref = cluster.firstOrNull { it.text != null }
        out.add(GameChatChunk(text = itemName, italic = ref?.italic ?: false, foreground = ref?.foreground))
    }
    if (glyphBuilder.isNotEmpty()) out.add(GameChatChunk(text = glyphBuilder.toString()))
    out.addAll(tail)
    return out
}

internal fun chatBubbleInk(
    chunks: List<GameChatChunk>, fallback: String, color: Color, forceColor: Boolean, highlight: String, light: Boolean,
    fontSize: TextUnit, lineHeight: TextUnit, axisFont: FontFamily, alreadyCleaned: Boolean = false,
): ChatInk {
    val useChunks = (if (alreadyCleaned) chunks else cleanItemLinkChunks(chunks)).ifEmpty { listOf(GameChatChunk(text = fallback)) }
    val builder = AnnotatedString.Builder()
    val placeholders = mutableListOf<AnnotatedString.Range<Placeholder>>()
    var len = 0
    useChunks.forEachIndexed { index, chunk ->
        if (chunk.icon != null) {
            val alt = "◆"
            builder.appendInlineContent("icon-$index", alt)
            placeholders.add(AnnotatedString.Range(Placeholder(fontSize, lineHeight, PlaceholderVerticalAlign.Center), len, len + alt.length))
            len += alt.length
        } else {
                                    val text = decodeChatEntities(chunk.text.orEmpty()).trimEnd('\n', '\r', ' ', '\u00A0')
            if (text.isEmpty()) return@forEachIndexed
            val chunkColor = if (forceColor) color else (chunk.foreground?.let { val c = chatChunkColor(it); if (light) blendColor(c, Color.Black, 0.30f) else blendColor(c, Color.White, 0.28f) } ?: color)
            val spanStyle = SpanStyle(color = chunkColor, fontStyle = if (chunk.italic) FontStyle.Italic else null)
            if (text.all { it.code in 0xE000..0xF8FF }) {
                val glyphKey = "glyph-$index"
                builder.appendInlineContent(glyphKey, " ")
                placeholders.add(AnnotatedString.Range(Placeholder(fontSize, lineHeight, PlaceholderVerticalAlign.Center), len, len + 1))
                len += 1
            } else {
                builder.appendPuaAware(text, highlight, spanStyle, Color(0x66FFEB3B), axisFont)
                len += text.length
            }
        }
    }
        return ChatInk(builder.toAnnotatedString(), placeholders)
}

internal fun chatBubbleInline(chunks: List<GameChatChunk>, fallback: String, fontSize: TextUnit, lineHeight: TextUnit, alreadyCleaned: Boolean = false): Map<String, InlineTextContent> {
    val useChunks = (if (alreadyCleaned) chunks else cleanItemLinkChunks(chunks)).ifEmpty { listOf(GameChatChunk(text = fallback)) }
    return buildMap {
        useChunks.forEachIndexed { index, chunk ->
            val icon = chunk.icon
            if (icon != null) {
                val linkColor = if (icon in 0xE000..0xF8FF) {
                    var prevFg: Long? = null
                    var p = index - 1
                    while (p >= 0 && useChunks[p].icon != null) p--
                    if (p >= 0) prevFg = useChunks[p].foreground
                    var nextFg: Long? = null
                    var q = index + 1
                    while (q < useChunks.size && useChunks[q].icon != null) q++
                    if (q < useChunks.size) nextFg = useChunks[q].foreground
                    (nextFg ?: prevFg)?.let { chatChunkColor(it) }
                } else null
                put("icon-$index", InlineTextContent(Placeholder(fontSize, lineHeight, PlaceholderVerticalAlign.Center)) {
                    ChatInlineIcon(icon, fontSize, linkColor)
                })
            } else {
                            val gt = chunk.text.orEmpty().trimEnd('\n', '\r', ' ', '\u00A0')
if (gt.isNotEmpty() && gt.all { it.code in 0xE000..0xF8FF }) {
                    put("glyph-$index", InlineTextContent(Placeholder(fontSize, lineHeight, PlaceholderVerticalAlign.Center)) {
                        ChatInlineIcon(gt.first().code, fontSize, null)
                    })
                }
            }
        }
    }
}
