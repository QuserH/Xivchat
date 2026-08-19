package com.quserh.eorzeaphone.ui

private val itemLinkPattern = Regex("([\\u201c\\u201d\\\"\\u300c\\u300d])([^\\u201c\\u201d\\\"\\u300c\\u300d]{1,96})([\\u201d\\\"\\u300d])(?=\\s*[\\u00d7xX]\\s*\\d+)")

private fun displayWidth(codePoint: Int): Int {
    return if (Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN) 2 else 1
}

private fun displayWidth(value: String): Int = value.codePoints().toArray().sumOf(::displayWidth)

internal fun wrapChatTextByUnits(text: String, unitsPerLine: Int): String {
    if (unitsPerLine <= 0) return text
    val result = StringBuilder()
    var used = 0
    var cursor = 0

    fun appendUnit(value: String, width: Int, atomic: Boolean = false) {
        if (value.isEmpty()) return
        if (used > 0 && used + width > unitsPerLine) {
            result.append('\n')
            used = 0
        }
        // Item links are kept together. If one is wider than the configured
        // line, it starts a fresh line instead of being split at a separator.
        result.append(value)
        used += width
        if (atomic && used > unitsPerLine) {
            result.append('\n')
            used = 0
        }
    }

    while (cursor < text.length) {
        val match = itemLinkPattern.find(text, cursor)
        if (match == null) {
            val tail = text.substring(cursor)
            tail.codePoints().forEach { codePoint ->
                if (codePoint == '\n'.code) {
                    result.append('\n')
                    used = 0
                } else {
                    appendUnit(String(Character.toChars(codePoint)), displayWidth(codePoint))
                }
            }
            break
        }
        val prefix = text.substring(cursor, match.range.first)
        prefix.codePoints().forEach { codePoint ->
            if (codePoint == '\n'.code) {
                result.append('\n')
                used = 0
            } else {
                appendUnit(String(Character.toChars(codePoint)), displayWidth(codePoint))
            }
        }
        val item = "◆${match.value}"
        appendUnit(item, displayWidth(item), atomic = true)
        cursor = match.range.last + 1
    }
    return result.toString().trimEnd('\n')
}
