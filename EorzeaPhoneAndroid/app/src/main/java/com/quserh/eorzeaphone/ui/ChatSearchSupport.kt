package com.quserh.eorzeaphone.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.quserh.eorzeaphone.data.GameChatMessage

internal fun chatSearchKey(message: GameChatMessage): String = buildString {
    append(message.timestamp).append('\u0000')
    append(message.channel).append('\u0000')
    append(message.sender).append('\u0000')
    append(message.text).append('\u0000')
    append(message.self).append('\u0000')
    append(message.senderName.orEmpty()).append('\u0000')
    append(message.senderWorld.orEmpty()).append('\u0000')
    append(message.senderStatusName.orEmpty()).append('\u0000')
    append(message.senderStatusIcon ?: -1).append('\u0000')
    append(message.senderWorldIcon ?: -1).append('\u0000')
    append(message.characterTag.orEmpty()).append('\u0000')
    append(message.targetName.orEmpty()).append('\u0000')
    append(message.targetWorld.orEmpty()).append('\u0000')
    append(message.selfFlag).append('\u0000')
    message.chunks.forEach { chunk ->
        append(chunk.text.orEmpty()).append('\u0001')
        append(chunk.icon ?: -1).append('\u0001')
        append(chunk.italic).append('\u0001')
        append(chunk.foreground ?: Long.MIN_VALUE).append('\u0002')
    }
}

/** Cheap row identity used only by LazyColumn; search/dedup keeps the full fingerprint. */
private fun chatComposeFingerprint(message: GameChatMessage): Long {
    var h = -0x61c8864680b583ebL
    fun mix(value: Long) {
        h = (h xor value) * -0x40a7b892e31b1a47L
        h = h xor (h ushr 29)
    }
    mix(message.timestamp)
    mix(message.channel.toLong())
    mix(message.sender.hashCode().toLong())
    mix(message.text.hashCode().toLong())
    mix(if (message.self) 1L else 0L)
    mix(message.senderName?.hashCode()?.toLong() ?: 0L)
    mix(message.senderWorld?.hashCode()?.toLong() ?: 0L)
    mix(message.targetName?.hashCode()?.toLong() ?: 0L)
    mix(message.targetWorld?.hashCode()?.toLong() ?: 0L)
    mix(message.chunks.size.toLong())
    // Chunk list sizes are normally tiny.  Include icon/foreground and text hashes so
    // two links with the same plain message still don't recycle the wrong inline item.
    message.chunks.forEach { chunk ->
        mix((chunk.icon ?: -1).toLong())
        mix(chunk.text?.hashCode()?.toLong() ?: 0L)
        mix(chunk.foreground ?: Long.MIN_VALUE)
        mix(if (chunk.italic) 1L else 0L)
    }
    return h
}

/**
 * Reuse the key list while the user drags a transcript.  SnapshotStateList keeps its
 * identity while messages are appended, so the cache also checks size and a few element
 * references; this catches normal append/clear/replace operations without an O(n)
 * equality/hash pass on every frame.
 */
private class MessageOccurrenceKeyCache {
    private var size = -1
    private var source: List<GameChatMessage>? = null
    private var sampleIndexes: IntArray = IntArray(0)
    private var samples: Array<GameChatMessage?> = emptyArray()
    private var cached: List<String> = emptyList()
    private val occurrences = HashMap<Long, Int>()
    private var revision = 0L
    private var result = MessageOccurrenceKeys(cached, revision)

    fun get(messages: List<GameChatMessage>): MessageOccurrenceKeys {
        val n = messages.size
        val previousSource = source
        val prefixStillMatches = when {
            size < 0 || n < size -> false
            size == 0 -> true
            previousSource === messages -> sampleIndexes.indices.all { sample ->
                val index = sampleIndexes[sample]
                index < messages.size && messages[index] === samples[sample]
            }
            previousSource == null || previousSource.size != size -> false
            else -> (0 until size).all { index -> previousSource[index] === messages[index] }
        }
        val appendOnly = n > size && prefixStillMatches
        val unchanged = n == size && prefixStillMatches

        if (appendOnly) {
            val next = ArrayList<String>(n)
            next.addAll(cached)
            for (index in size until n) {
                val fingerprint = chatComposeFingerprint(messages[index])
                val ordinal = occurrences[fingerprint] ?: 0
                occurrences[fingerprint] = ordinal + 1
                next += "message:${fingerprint.toString(16)}\u0003$ordinal"
            }
            cached = next
            revision++
            result = MessageOccurrenceKeys(cached, revision)
        } else if (!unchanged) {
            // Compose asks for keys while a LazyColumn is being re-laid out. Do not build
            // the full search fingerprint here: it contains every chunk/string and creates
            // substantial garbage. Rebuild compact hashes only after an actual replace,
            // front trim or filter change; normal appends extend the prior key list above.
            occurrences.clear()
            cached = messages.map { message ->
                val fingerprint = chatComposeFingerprint(message)
                val ordinal = occurrences[fingerprint] ?: 0
                occurrences[fingerprint] = ordinal + 1
                "message:${fingerprint.toString(16)}\u0003$ordinal"
            }
            revision++
            result = MessageOccurrenceKeys(cached, revision)
        }

        size = n
        source = messages
        sampleIndexes = if (n == 0) IntArray(0) else intArrayOf(
            0,
            (n - 1) / 3,
            (n - 1) / 2,
            ((n - 1) * 2) / 3,
            n - 1,
        )
        val nextSamples: Array<GameChatMessage?> = if (n == 0) {
            emptyArray()
        } else {
            Array(sampleIndexes.size) { sample -> messages[sampleIndexes[sample]] }
        }
        samples = nextSamples
        return result
    }
}

internal data class MessageOccurrenceKeys(
    val keys: List<String>,
    val revision: Long,
)

@Composable
internal fun rememberMessageOccurrenceKeys(messages: List<GameChatMessage>): MessageOccurrenceKeys {
    val cache = remember { MessageOccurrenceKeyCache() }
    return cache.get(messages)
}
