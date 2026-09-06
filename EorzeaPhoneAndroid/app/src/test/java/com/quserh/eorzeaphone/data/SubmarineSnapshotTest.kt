package com.quserh.eorzeaphone.data

import org.junit.Assert.*
import org.junit.Test
import org.msgpack.core.MessagePack

class SubmarineSnapshotTest {
    private val vessel = GameSubmarineVessel("Test", 12345, 30, 50, 100)
    private val cached = GameSubmarine(100, listOf(vessel))

    @Test
    fun emptyWorkshopDoesNotEraseLastKnownFleet() {
        assertSame(cached, mergeSubmarineSnapshot(cached, GameSubmarine(200, emptyList())))
        assertNull(mergeSubmarineSnapshot(null, GameSubmarine(200, emptyList())))
    }

    @Test
    fun oldPluginSpanNamesAreNotVessels() {
        val invalid = listOf("", "  ", "System.Span<Byte>[20]", "System.ReadOnlySpan<Byte>[20]")
        assertNull(mergeSubmarineSnapshot(null, GameSubmarine(200, invalid.map { vessel.copy(name = it) })))
        assertEquals(listOf(vessel), mergeSubmarineSnapshot(null,
            GameSubmarine(200, listOf(vessel.copy(name = " Test "), vessel.copy(name = invalid[2]))))?.vessels)
    }

    @Test
    fun delayedSnapshotCannotUndoNewerVoyage() {
        assertSame(cached, mergeSubmarineSnapshot(cached, GameSubmarine(90, listOf(vessel.copy(returnUnix = 0)))))
        assertEquals(0L, mergeSubmarineSnapshot(cached,
            GameSubmarine(200, listOf(vessel.copy(returnUnix = 0))))?.vessels?.single()?.returnUnix)
    }

    @Test
    fun missingCharacterCacheDoesNotBorrowAnotherCharacter() {
        assertNull(mergeSubmarineSnapshot(null, GameSubmarine(200, emptyList())))
        assertEquals(cached, mergeSubmarineSnapshot(null, cached))
    }

    @Test
    fun codecReadsVesselsAndSkipsFutureFields() {
        val bytes = MessagePack.newDefaultBufferPacker().use { p ->
            p.packArrayHeader(3).packLong(100).packArrayHeader(2)
            p.packArrayHeader(6).packString("Test").packLong(12345).packInt(30).packLong(50).packLong(100).packString("future")
            p.packNil().packString("futureRoot")
            p.toByteArray()
        }
        MessagePack.newDefaultUnpacker(bytes).use { p ->
            assertEquals(cached, XivChatCodec.readSubmarine(p))
            assertFalse(p.hasNext())
        }
    }

    @Test
    fun codecAcceptsUnloadedAndOlderRows() {
        val bytes = MessagePack.newDefaultBufferPacker().use { p ->
            p.packArrayHeader(2).packLong(100).packArrayHeader(2)
            p.packArrayHeader(2).packString("Test").packNil()
            p.packArrayHeader(0)
            p.toByteArray()
        }
        MessagePack.newDefaultUnpacker(bytes).use { p ->
            val result = mergeSubmarineSnapshot(null, XivChatCodec.readSubmarine(p))!!
            assertEquals(listOf(GameSubmarineVessel("Test", 0, 0, 0, 0)), result.vessels)
            assertFalse(p.hasNext())
        }
        val nilFleet = byteArrayOf(0x92.toByte(), 100, 0xc0.toByte())
        MessagePack.newDefaultUnpacker(nilFleet).use { p ->
            assertTrue(XivChatCodec.readSubmarine(p).vessels.isEmpty())
            assertFalse(p.hasNext())
        }
    }

    @Test
    fun handshakeAdvertisesSubmarineSupport() {
        MessagePack.newDefaultUnpacker(XivChatCodec.encodePreferences()).use { p ->
            assertEquals(1, p.unpackArrayHeader())
            val preferences = buildMap { repeat(p.unpackMapHeader()) { put(p.unpackInt(), p.unpackBoolean()) } }
            assertEquals(true, preferences[12])
            assertFalse(p.hasNext())
        }
    }
}
