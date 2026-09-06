package com.quserh.eorzeaphone.data

import org.junit.Assert.*
import org.junit.Test
import org.msgpack.core.MessagePack

class CraftSkillCodecTest {
    @Test
    fun startCarriesRecipeAndBothConsumablesAtomically() {
        MessagePack.newDefaultUnpacker(XivChatCodec.encodeCraftStart(500, 0, 1_000_100)).use { u ->
            assertEquals(3, u.unpackArrayHeader())
            assertEquals(500, u.unpackInt())
            assertEquals(0, u.unpackInt())
            assertEquals(1_000_100, u.unpackInt())
            assertFalse(u.hasNext())
        }
    }

    @Test
    fun readsFoodAndPotionIncludingHqAndFutureFields() {
        val bytes = MessagePack.newDefaultBufferPacker().use { p ->
            p.packArrayHeader(4).packLong(123).packArrayHeader(0)
            p.packArrayHeader(1).packArrayHeader(4).packInt(1_000_100).packString("料理").packInt(2).packString("future")
            p.packArrayHeader(1).packArrayHeader(3).packInt(101).packString("药剂").packInt(3)
            p.toByteArray()
        }
        MessagePack.newDefaultUnpacker(bytes).use { u ->
            val packet = XivChatCodec.readCraftSkillListPacket(u)
            assertEquals(listOf(GameCraftConsumable(1_000_100, "料理", 2)), packet.foods)
            assertEquals(listOf(GameCraftConsumable(101, "药剂", 3)), packet.pots)
            assertFalse(u.hasNext())
        }
    }

    @Test
    fun readsSkillsAndSkipsConsumablesAndFutureColumns() {
        val packet = MessagePack.newDefaultBufferPacker().use { packer ->
            packer.packArrayHeader(5).packLong(123).packArrayHeader(2)
            packer.packArrayHeader(7).packLong(100075).packString("制作").packInt(1651).packString("说明").packInt(0).packInt(0).packString("future")
            packer.packNil()
            packer.packArrayHeader(0).packArrayHeader(0).packString("futureRoot")
            packer.toByteArray()
        }
        MessagePack.newDefaultUnpacker(packet).use { unpacker ->
            assertEquals(listOf(GameCraftSkill(100075, "制作", 1651, "说明", 0, 0)), XivChatCodec.readCraftSkillList(unpacker))
            assertFalse(unpacker.hasNext())
        }
    }

    @Test
    fun optionalRowsAndMissingFieldsDoNotBreakConnection() {
        val packet = MessagePack.newDefaultBufferPacker().use { packer ->
            packer.packArrayHeader(2).packLong(123).packArrayHeader(1)
            packer.packArrayHeader(2).packLong(100001).packString("制作")
            packer.toByteArray()
        }
        MessagePack.newDefaultUnpacker(packet).use { unpacker ->
            val skill = XivChatCodec.readCraftSkillList(unpacker).single()
            assertEquals(100001L, skill.id)
            assertEquals(-1, skill.cp)
            assertFalse(unpacker.hasNext())
        }
    }
}
