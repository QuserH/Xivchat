package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Lightweight HTML -> Compose renderer for 石之家 article/comment bodies.
 *
 * The backend returns full HTML (paragraphs, bold, colored spans, blockquotes,
 * links and <img>). We deliberately render a faithful *subset* in native Compose
 * instead of embedding a WebView:
 *
 *   - block-level tags (<p> <br> <div> <li> <hr>) become paragraph breaks
 *   - inline styling (<strong> <b> <i> <em> <u> <span color>) becomes spans
 *   - <a href> becomes an accent link that opens the system browser on tap
 *   - <img src> becomes a [ShizhijiaRemoteImage] block (async, cached)
 *
 * Any unknown tag is skipped but its text is kept, and all HTML entities are
 * decoded, so the parser degrades gracefully on unexpected markup.
 */

private data class RichRun(val text: String, val bold: Boolean, val italic: Boolean, val underline: Boolean, val color: Color?, val link: String?)

// Comment emoji: the backend sends placeholders like <span class="at-emo">[emo2]</span>
// which must become a small emoji image on the official CDN.
private const val EMO_BASE = "https://static.web.sdo.com/jijiamobile/pic/ff14/2023ffstone/emo"
private val EMO_RE = Regex("\\[(emo\\d+)\\]")

private class RichParagraphBuilder {
    val runs = mutableListOf<RichRun>()
    var align: TextAlign = TextAlign.Start
    private var bold = false
    private var italic = false
    private var underline = false
    private var color: Color? = null
    private var link: String? = null

    private val styleStack = ArrayDeque<Pair<String, Any?>>()

    fun push(tag: String, value: Any?) { styleStack.addLast(tag to value) }
    fun pop(tag: String) {
        var i = styleStack.size - 1
        while (i >= 0) {
            if (styleStack[i].first == tag) { styleStack.removeAt(i); break }
            i--
        }
        recompute()
    }

    private fun recompute() {
        bold = false; italic = false; underline = false; color = null; link = null
        for ((t, v) in styleStack) {
            when (t) {
                "b" -> bold = true
                "i" -> italic = true
                "u" -> underline = true
                "color" -> color = v as? Color
                "a" -> link = v as? String
            }
        }
    }

    fun text(segment: String) {
        val decoded = decodeEntities(segment)
        if (decoded.isEmpty()) return
        val last = runs.lastOrNull()
        if (last != null && last.bold == bold && last.italic == italic && last.underline == underline && last.color == color && last.link == link) {
            runs[runs.lastIndex] = last.copy(text = last.text + decoded)
        } else {
            runs.add(RichRun(decoded, bold, italic, underline, color, link))
        }
    }

    /** True when the paragraph contains any non-whitespace text (skips <p>&nbsp;</p> spacers). */
    fun hasVisibleText(): Boolean = runs.any { it.text.any { c -> !c.isWhitespace() } }

    fun isEmpty() = runs.isEmpty()
}

private fun decodeEntities(s: String): String {
    if (!s.contains('&')) return s
    val out = StringBuilder(s.length)
    var i = 0
    while (i < s.length) {
        val amp = s.indexOf('&', i)
        if (amp < 0) { out.append(s, i, s.length); break }
        out.append(s, i, amp)
        val semi = s.indexOf(';', amp)
        if (semi < 0) { out.append(s, amp, s.length); break }
        val ent = s.substring(amp + 1, semi)
        val ch = when (ent) {
            "amp" -> '&'; "lt" -> '<'; "gt" -> '>'; "quot" -> '"'; "apos" -> '\''
            "nbsp" -> ' '; "ldquo" -> '\u201C'; "rdquo" -> '\u201D'; "lsquo" -> '\u2018'; "rsquo" -> '\u2019'
            "middot" -> '\u00B7'; "mdash" -> '\u2014'; "ndash" -> '\u2013'; "hellip" -> '\u2026'
            else -> null
        }
        if (ch != null) {
            out.append(ch); i = semi + 1; continue
        }
        // Numeric entities (hex or decimal) decode to a single code point.
        var cp: Int? = null
        if (ent.startsWith("#x") || ent.startsWith("#X")) cp = ent.substring(2).toIntOrNull(16)
        else if (ent.startsWith("#")) cp = ent.substring(1).toIntOrNull()
        if (cp != null) {
            out.append(cp.toChar()); i = semi + 1; continue
        }
        // Unknown entity: keep the raw text verbatim.
        out.append(s, amp, semi + 1)
        i = semi + 1
    }
    return out.toString()
}

private fun parseColor(spec: String): Color? {
    val rgb = Regex("rgb\\s*\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)").find(spec)
    if (rgb != null) {
        val (r, g, b) = rgb.destructured
        val rr = r.toLongOrNull() ?: return null
        val gg = g.toLongOrNull() ?: return null
        val bb = b.toLongOrNull() ?: return null
        return Color(0xFF000000L or (rr shl 16) or (gg shl 8) or bb)
    }
    val hex = Regex("#([0-9a-fA-F]{6})").find(spec)
    if (hex != null) return hex.groupValues[1].toLongOrNull(16)?.let { Color(0xFF000000L or it) }
    return null
}

/**
 * A parsed unit of content. Either a text paragraph (`runs` + alignment) or a
 * remote image (`url` set). The renderer switches on which field is populated.
 */
private data class RichBlock(val runs: List<RichRun>, val align: TextAlign, val url: String?)

/**
 * Parse raw HTML into an ordered list of blocks: text paragraphs and images.
 * A `url`-bearing block is an image; all other blocks are text runs.
 */
private fun parseRichHtml(html: String): List<RichBlock> {
    val blocks = mutableListOf<RichBlock>()
    val p = RichParagraphBuilder()
    val seenImgs = mutableSetOf<String>()
    fun flush() {
        // Drop paragraphs that are only whitespace (e.g. <p>&nbsp;</p> spacers)
        // so the article keeps tight, natural paragraph spacing.
        if (!p.isEmpty() && p.hasVisibleText()) blocks.add(RichBlock(p.runs.toList(), p.align, null))
        p.runs.clear(); p.align = TextAlign.Start
    }
    // Single pass over the markup: text between tags goes to the current
    // paragraph, tags mutate style or emit paragraph/image breaks.
    val tagRe = Regex("<([^>]+)>")
    var last = 0
    // Emit a run of plain text, but split out any [emoN] placeholder into its
    // own image block so the backend emoji shows as a picture, not literal text.
    fun emitText(seg: String) {
        var idx = 0
        for (em in EMO_RE.findAll(seg)) {
            if (em.range.first > idx) p.text(seg.substring(idx, em.range.first))
            flush()
            blocks.add(RichBlock(emptyList(), TextAlign.Start, EMO_BASE + em.groupValues[1].removePrefix("emo") + ".png"))
            idx = em.range.last + 1
        }
        if (idx < seg.length) p.text(seg.substring(idx))
    }
    for (m in tagRe.findAll(html)) {
        if (m.range.first > last) emitText(html.substring(last, m.range.first))
        handleTag(m.groupValues[1], p, ::flush, blocks, seenImgs)
        last = m.range.last + 1
    }
    if (last < html.length) emitText(html.substring(last))
    flush()
    return blocks
}

private fun handleTag(tag: String, p: RichParagraphBuilder, flush: () -> Unit, blocks: MutableList<RichBlock>, seenImgs: MutableSet<String>) {
    val lower = tag.lowercase()
    when {
        lower == "br" || lower == "br/" || lower == "hr" || lower == "hr/" || lower == "/p" ||
            lower == "/div" || lower == "/li" || lower == "/blockquote" -> flush()

        lower == "img" || lower.startsWith("img ") || lower == "img/" -> {
            val src = Regex("src\\s*=\\s*[\"']([^\"']+)[\"']").find(tag)?.groupValues?.get(1).orEmpty()
            if (src.isNotBlank() && seenImgs.add(src)) { flush(); blocks.add(RichBlock(emptyList(), TextAlign.Start, src)) }
        }

        lower == "p" || lower.startsWith("p ") -> {
            // Capture paragraph text alignment from the style attribute.
            val style = Regex("style\\s*=\\s*[\"'][^\"']*text-align\\s*:\\s*([a-z]+)").find(tag)
            p.align = when (style?.groupValues?.get(1)) { "center" -> TextAlign.Center; "right" -> TextAlign.End; else -> TextAlign.Start }
        }

        lower == "strong" || lower == "b" -> p.push("b", null)
        lower == "/strong" || lower == "/b" -> p.pop("b")
        lower == "em" || lower == "i" -> p.push("i", null)
        lower == "/em" || lower == "/i" -> p.pop("i")
        lower == "u" -> p.push("u", null)
        lower == "/u" -> p.pop("u")

        lower == "a" || lower.startsWith("a ") -> {
            val href = Regex("href\\s*=\\s*[\"']([^\"']+)[\"']").find(tag)?.groupValues?.get(1)
            p.push("a", href ?: "")
        }
        lower == "/a" -> p.pop("a")

        lower.startsWith("span ") -> {
            val style = Regex("style\\s*=\\s*[\"']([^\"']*)[\"']").find(tag)?.groupValues?.get(1).orEmpty()
            val color = Regex("color\\s*:\\s*([^;]+)").find(style)?.groupValues?.get(1)?.trim()
            p.push("color", color?.let { parseColor(it) })
        }
        lower == "/span" -> p.pop("color")
        // Everything else (font, td, th, div, ul...) contributes no style.
    }
}

/** Render parsed blocks as native Compose content (text + async images). */
@Composable
fun ShizhijiaRichContent(html: String, modifier: Modifier = Modifier, placeholder: Boolean = false, imgMaxWidth: androidx.compose.ui.unit.Dp? = null) {
    // 石之家专属正文色，跟随深/浅主题，避免富文本内容显示成全局主题色。
    // 与 ShizhijiaScreens.kt 的 SzjText 保持同值（板岩体系的正文色）。
    val textColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFF1B2129) else Color(0xFFE8EDF2)
    // Tight inter-block spacing keeps the article/comment body dense like a
    // forum thread; paragraphs already carry their own line structure.
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        parseRichHtml(html).forEach { block ->
            if (block.url != null) {
                val isEmo = block.url.contains("/2023ffstone/emo")
                ShizhijiaRemoteImage(
                    url = block.url,
                    // Emoji placeholders render small; article/comment photos
                    // scale to the container width (or a cap for comments) and
                    // collapse entirely if they fail to load.
                    modifier = if (isEmo) {
                        Modifier.padding(vertical = 2.dp).size(46.dp)
                    } else {
                        Modifier.padding(vertical = 2.dp)
                            .then(if (imgMaxWidth != null) Modifier.widthIn(max = imgMaxWidth) else Modifier.fillMaxWidth())
                    },
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    placeholderColor = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFE3E8ED) else Color(0xFF232932),
                    showPlaceholder = false,
                    collapseOnFail = true,
                    onClick = if (isEmo) null else { url -> SzjViewer.url = url },
                )
            } else if (block.runs.isNotEmpty()) {
                val annotated = buildAnnotatedString {
                    block.runs.forEach { run ->
                        val style = SpanStyle(
                            color = run.color ?: textColor,
                            fontWeight = if (run.bold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (run.italic) FontStyle.Italic else FontStyle.Normal,
                            textDecoration = when {
                                run.underline -> TextDecoration.Underline
                                run.link != null -> TextDecoration.Underline
                                else -> TextDecoration.None
                            },
                        )
                        if (run.link != null) {
                            val href = run.link
                            val start = length
                            withStyle(style) { append(run.text) }
                            addStringAnnotation("url", href, start, length)
                        } else {
                            withStyle(style) { append(run.text) }
                        }
                    }
                }
                Text(
                    text = annotated,
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 23.sp,
                    textAlign = block.align,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
