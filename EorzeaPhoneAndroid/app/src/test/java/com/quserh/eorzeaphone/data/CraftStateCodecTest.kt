package com.quserh.eorzeaphone.data

import org.junit.Assert.*
import org.junit.Test
import org.msgpack.core.MessagePack

class CraftStateCodecTest {
    private fun frame(fields: Int) = MessagePack.newDefaultBufferPacker().use { p ->
        p.packArrayHeader(fields).packLong(123)
        listOf(35836, 2, 1200, 7500, 8250, 16500, 60, 70, 450, 600, 0).forEach { p.packInt(it) }
        p.packBoolean(false)
        if (fields > 13) p.packBoolean(true)
        if (fields > 14) p.packInt(15)
        if (fields > 15) p.packLong(638_980_000_000_000_000L)
        if (fields > 16) p.packString("future")
        p.toByteArray()
    }

    @Test fun olderFramesRemainCompatible() {
        for (fields in 13..14) MessagePack.newDefaultUnpacker(frame(fields)).use { u ->
            val state = XivChatCodec.readCraftState(u)
            assertEquals(16500, state.qualityMax)
            assertEquals(-1, state.hqChance)
            assertEquals(0L, state.craftInstanceId)
            assertEquals(fields == 14, state.canAct)
            assertFalse(u.hasNext())
        }
    }

    @Test fun readsLiveHqAndCraftIdentityAndSkipsFutureFields() {
        for (fields in 16..17) MessagePack.newDefaultUnpacker(frame(fields)).use { u ->
            val state = XivChatCodec.readCraftState(u)
            assertEquals(15, state.hqChance)
            assertEquals(638_980_000_000_000_000L, state.craftInstanceId)
            assertFalse(u.hasNext())
        }
    }

    @Test fun cancellationCannotBeConfusedWithObserverDisposal() {
        MessagePack.newDefaultUnpacker(XivChatCodec.encodeCraftStop()).use { u -> assertEquals(0, u.unpackArrayHeader()) }
        MessagePack.newDefaultUnpacker(XivChatCodec.encodeCraftCancel(35836, 77)).use { u ->
            assertEquals(2, u.unpackArrayHeader())
            assertEquals(35836, u.unpackInt())
            assertEquals(77L, u.unpackLong())
            assertFalse(u.hasNext())
        }
    }
}
