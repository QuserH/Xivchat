package com.quserh.eorzeaphone.data

internal fun mergeSubmarineSnapshot(previous: GameSubmarine?, incoming: GameSubmarine): GameSubmarine? {
    val vessels = incoming.vessels.map { it.copy(name = it.name.trim().trimEnd('\u0000')) }.filter {
        it.name.isNotBlank() && !it.name.startsWith("System.Span<") && !it.name.startsWith("System.ReadOnlySpan<")
    }
    // Old plugins can send empty, unloaded slots or the byte span's type name.
    if (vessels.isEmpty() || (previous != null && incoming.updatedUnix < previous.updatedUnix)) return previous
    return incoming.copy(vessels = vessels)
}
