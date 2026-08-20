package com.quserh.eorzeaphone.data

import com.goterl.lazysodium.LazySodiumAndroid
import com.goterl.lazysodium.utils.Key
import org.msgpack.core.MessagePack
import org.msgpack.core.MessageUnpacker
import org.msgpack.value.Value
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.text.Charsets.UTF_8

internal object XivChatCodec {
    const val MAGIC_0 = 14
    const val MAGIC_1 = 20
    const val MAGIC_2 = 67
    private const val MAX_FRAME = 128_000

    fun deriveClientKeys(sodium: LazySodiumAndroid, publicKey: ByteArray, secretKey: ByteArray, serverPublic: ByteArray): Pair<ByteArray, ByteArray> {
        val shared = sodium.cryptoScalarMult(Key.fromBytes(secretKey), Key.fromBytes(serverPublic)).asBytes
        val input = shared + publicKey + serverPublic
        val hash = ByteArray(64)
        check(sodium.cryptoGenericHash(hash, hash.size, input, input.size.toLong())) { "无法计算握手密钥" }
        return hash.copyOfRange(0, 32) to hash.copyOfRange(32, 64)
    }

    fun readSecret(input: DataInputStream, sodium: LazySodiumAndroid, key: ByteArray): ByteArray {
        val header = ByteArray(28)
        input.readFully(header)
        val length = ByteBuffer.wrap(header, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        require(length in 16..MAX_FRAME) { "无效的加密帧长度: $length" }
        val nonce = header.copyOfRange(4, 28)
        val cipher = ByteArray(length)
        input.readFully(cipher)
        val plain = ByteArray(length - 16)
        check(sodium.cryptoSecretBoxOpenEasy(plain, cipher, cipher.size.toLong(), nonce, key)) { "解密游戏数据失败" }
        return plain
    }

    fun writeSecret(output: DataOutputStream, sodium: LazySodiumAndroid, key: ByteArray, plain: ByteArray) {
        require(plain.size <= MAX_FRAME - 16) { "消息过大" }
        val nonce = sodium.nonce(24)
        val cipher = ByteArray(plain.size + 16)
        check(sodium.cryptoSecretBoxEasy(cipher, plain, plain.size.toLong(), nonce, key)) { "加密消息失败" }
        val header = ByteBuffer.allocate(28).order(ByteOrder.LITTLE_ENDIAN).putInt(cipher.size).put(nonce).array()
        output.write(header)
        output.write(cipher)
        output.flush()
    }

    fun encodeBacklog(amount: Int): ByteArray = pack { packArrayHeader(1); packShort(amount.toShort()) }
    fun encodePlayerList(type: Int = 2): ByteArray = pack { packArrayHeader(1); packByte(type.toByte()) }
    fun encodePing(): ByteArray = byteArrayOf(1)
    fun encodeShutdown(): ByteArray = byteArrayOf(3)
    fun encodeMessage(text: String): ByteArray = pack { packArrayHeader(1); packString(text) }
    fun encodeChannel(channel: Int): ByteArray = pack { packArrayHeader(1); packInt(channel) }
    fun encodeFriendAction(action: Int, contentId: Long, worldId: Int): ByteArray = pack {
        packArrayHeader(3)
        packInt(action)
        packLong(contentId)
        packInt(worldId)
    }
    fun encodePreferences(): ByteArray = pack {
        packArrayHeader(1)
        packMapHeader(10)
        packInt(3)
        packBoolean(true)
        packInt(4)
        packBoolean(true)
        packInt(5)
        packBoolean(true)
        packInt(2)
        packBoolean(true)
        packInt(6)
        packBoolean(true)
        packInt(7)
        packBoolean(true)
        packInt(8)
        packBoolean(true)
        packInt(9)
        packBoolean(true)
        packInt(10)
        packBoolean(true)
        packInt(11)
        packBoolean(true)
        packInt(12)
        packBoolean(true)
    }

    private fun pack(block: org.msgpack.core.MessagePacker.() -> Unit): ByteArray {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.block()
        return packer.toByteArray()
    }

    fun readMessage(unpacker: MessageUnpacker): GameChatMessage {
        val fields = unpacker.unpackArrayHeader()
        val time = unpacker.unpackLong()
        val channel = unpacker.unpackShort().toInt()
        val sender = decodeXiv(unpacker.readPayload(unpacker.unpackBinaryHeader()))
        val text = stripChatPrefix(decodeXiv(unpacker.readPayload(unpacker.unpackBinaryHeader())), sender)
        val chunks = if (fields > 4) readChunks(unpacker, sender) else emptyList()
        val senderName = if (fields > 5) nullableString(unpacker) else null
        val senderWorld = if (fields > 6) nullableString(unpacker) else null
        return GameChatMessage(time, sender, text, channel, chunks = chunks, senderName = senderName, senderWorld = senderWorld)
    }
    // The chat line text can embed "[频道]<名字> content" / "<名字> content" / "名字：content".
    // Keep only the actual content; the app shows sender/channel separately.
    private fun stripChatPrefix(raw: String, sender: String): String {
        var t = raw.trim()
        if (t.startsWith('[')) {
            val close = t.indexOf(']')
            if (close >= 0) t = t.substring(close + 1).trim()
        }
        val lt = t.indexOf('<')
        val gt = t.indexOf('>', lt.coerceAtLeast(0))
        if (gt >= 0) return t.substring(gt + 1).trim()
        if (sender.isNotEmpty() && t.startsWith(sender)) {
            t = t.removePrefix(sender).trimStart('：', ':', ' ', '>', '<').trim()
        }
        return t.ifBlank { raw.trim() }
    }

    private fun readChunks(unpacker: MessageUnpacker, sender: String): List<GameChatChunk> {
        val count = unpacker.unpackArrayHeader()
        var firstText = true
        return buildList(count) {
            repeat(count) {
                val unionFields = unpacker.unpackArrayHeader()
                if (unionFields < 2) { repeat(unionFields) { unpacker.skipValue() }; return@repeat }
                when (unpacker.unpackInt()) {
                    1 -> {
                        val fields = unpacker.unpackArrayHeader()
                        var content = ""
                        var italic = false
                        var fallback: Long? = null
                        var foreground: Long? = null
                        repeat(fields) { index ->
                            when (index) {
                                0 -> fallback = if (unpacker.tryUnpackNil()) null else unpacker.unpackLong()
                                3 -> italic = if (unpacker.tryUnpackNil()) false else unpacker.unpackBoolean()
                                4 -> content = if (unpacker.tryUnpackNil()) "" else unpacker.unpackString()
                                1 -> foreground = if (unpacker.tryUnpackNil()) null else unpacker.unpackLong()
                                else -> unpacker.skipValue()
                            }
                        }
                        val cleaned = if (firstText) stripChatPrefix(content, sender) else content
                        firstText = false
                        if (cleaned.isNotEmpty()) add(GameChatChunk(text = cleaned, italic = italic, foreground = foreground ?: fallback))
                    }
                    2 -> {
                        val fields = unpacker.unpackArrayHeader()
                        var icon = 0
                        repeat(fields) { index ->
                            if (index == 0) icon = unpacker.unpackInt() else unpacker.skipValue()
                        }
                        add(GameChatChunk(icon = icon))
                    }
                    else -> repeat(unionFields - 1) { unpacker.skipValue() }
                }
            }
        }
    }

    private fun skipChunk(unpacker: MessageUnpacker) {
        val shape = unpacker.unpackArrayHeader()
        repeat(shape) { unpacker.skipValue() }
    }

    fun readBacklog(unpacker: MessageUnpacker): List<GameChatMessage> {
        unpacker.unpackArrayHeader()
        val count = unpacker.unpackArrayHeader()
        val result = ArrayList<GameChatMessage>(count)
        repeat(count) { result += readMessage(unpacker) }
        unpacker.skipValue()
        return result
    }

    fun readPlayerList(unpacker: MessageUnpacker): Pair<Int, List<GameFriend>> {
        unpacker.unpackArrayHeader()
        val type = unpacker.unpackByte().toInt()
        val count = unpacker.unpackArrayHeader()
        val result = ArrayList<GameFriend>(count)
        repeat(count) {
            val fields = unpacker.unpackArrayHeader()
            val name = nullableString(unpacker)
            val freeCompany = nullableString(unpacker)
            val status = unpacker.unpackLong()
            val currentWorldId = unpacker.unpackShort().toInt() and 0xffff
            val currentWorld = nullableString(unpacker)
            val homeWorldId = unpacker.unpackShort().toInt() and 0xffff
            val homeWorld = nullableString(unpacker)
            unpacker.unpackShort()
            val territory = nullableString(unpacker)
            val classJob = unpacker.unpackByte().toInt()
            val job = nullableString(unpacker)
            repeat(4) { unpacker.skipValue() }
            val contentId = if (fields > 15) unpacker.unpackLong() else 0L
            result += GameFriend(name ?: "未知玩家", currentWorld ?: "", homeWorld ?: "", freeCompany ?: "", territory ?: "", status and (1L shl 47) != 0L, job ?: "", contentId, currentWorldId, homeWorldId, classJob)
        }
        return type to result
    }

    fun readInventory(unpacker: MessageUnpacker): GameInventorySnapshot {
        val fields = unpacker.unpackArrayHeader()
        unpacker.skipValue()
        val count = unpacker.unpackArrayHeader()
        val result = ArrayList<GameInventoryItem>(count)
        repeat(count) {
            val itemFields = unpacker.unpackArrayHeader()
            val itemId = unpacker.unpackLong()
            unpacker.unpackLong()
            val quantity = unpacker.unpackInt()
            val container = unpacker.unpackLong()
            val slot = unpacker.unpackLong()
            val hq = unpacker.unpackBoolean()
            unpacker.skipValue()
            unpacker.skipValue()
            val name = nullableString(unpacker) ?: "物品 $itemId"
            val icon = if (itemFields > 9) unpacker.unpackInt() else 0
            val retainerId = if (itemFields > 10) unpacker.unpackLong() else 0L
            result += GameInventoryItem(itemId, name, quantity, container, slot, hq, icon, retainerId)
        }
        val containers = ArrayList<GameInventoryContainer>()
        if (fields > 2) {
            val containerCount = unpacker.unpackArrayHeader()
            repeat(containerCount) {
                unpacker.unpackArrayHeader()
                containers += GameInventoryContainer(unpacker.unpackLong(), unpacker.unpackInt())
            }
        }
        val retainers = ArrayList<GameRetainer>()
        if (fields > 3) {
            val retainerCount = unpacker.unpackArrayHeader()
            repeat(retainerCount) {
                val retainerFields = unpacker.unpackArrayHeader()
                val id = unpacker.unpackLong()
                val name = unpacker.unpackString()
                val active = unpacker.unpackBoolean()
                val itemCount = unpacker.unpackInt()
                val quantity = unpacker.unpackInt()
                val gil = if (retainerFields > 5) unpacker.unpackLong() else 0L
                val ventureId = if (retainerFields > 6) unpacker.unpackLong() else 0L
                val ventureCompleteUnix = if (retainerFields > 7) unpacker.unpackLong() else 0L
                repeat((retainerFields - 8).coerceAtLeast(0)) { unpacker.skipValue() }
                retainers += GameRetainer(id, name, active, itemCount, quantity, gil, ventureId, ventureCompleteUnix)
            }
        }
        return GameInventorySnapshot(result, containers, retainers)
    }

    fun readWallet(unpacker: MessageUnpacker): GameWallet {
        unpacker.unpackArrayHeader()
        unpacker.skipValue()
        val gil = unpacker.unpackLong()
        val count = unpacker.unpackArrayHeader()
        val entries = ArrayList<GameWalletEntry>(count)
        repeat(count) {
            unpacker.unpackArrayHeader()
            val itemId = unpacker.unpackLong()
            val iconId = unpacker.unpackInt()
            val name = nullableString(unpacker) ?: "货币 $itemId"
            val amount = unpacker.unpackLong()
            val cap = unpacker.unpackLong()
            val section = nullableString(unpacker) ?: "其他"
            entries += GameWalletEntry(itemId, name, amount, cap, section, iconId)
        }
        return GameWallet(gil, entries)
    }

    fun readProfile(unpacker: MessageUnpacker): PlayerProfile {
        val fields = unpacker.unpackArrayHeader()
        val homeWorld = unpacker.unpackString()
        val currentWorld = unpacker.unpackString()
        val location = unpacker.unpackString()
        val name = unpacker.unpackString()
        val classJobId = if (fields > 4) unpacker.unpackLong() else 0L
        val jobName = if (fields > 5) unpacker.unpackString() else ""
        val level = if (fields > 6) unpacker.unpackInt() else 0
        val territoryId = if (fields > 7) unpacker.unpackLong() else 0L
        val currentHp = if (fields > 8) unpacker.unpackInt() else 0
        val maxHp = if (fields > 9) unpacker.unpackInt() else 0
        val currentMp = if (fields > 10) unpacker.unpackInt() else 0
        val maxMp = if (fields > 11) unpacker.unpackInt() else 0
        val currentCp = if (fields > 12) unpacker.unpackInt() else 0
        val maxCp = if (fields > 13) unpacker.unpackInt() else 0
        val currentGp = if (fields > 14) unpacker.unpackInt() else 0
        val maxGp = if (fields > 15) unpacker.unpackInt() else 0
        val itemLevel = if (fields > 16) unpacker.unpackInt() else 0
        return PlayerProfile(name, homeWorld, currentWorld, location, classJobId, jobName, level, territoryId,
            currentHp, maxHp, currentMp, maxMp, currentCp, maxCp, currentGp, maxGp, itemLevel)
    }

    fun readAvailability(unpacker: MessageUnpacker): Boolean {
        unpacker.unpackArrayHeader()
        return unpacker.unpackBoolean()
    }

    fun readWeather(unpacker: MessageUnpacker): GameWeather {
        unpacker.unpackArrayHeader()
        unpacker.skipValue()
        val zone = unpacker.unpackString()
        val current = unpacker.unpackString()
        val count = unpacker.unpackArrayHeader()
        val windows = ArrayList<GameWeatherWindow>(count)
        repeat(count) {
            unpacker.unpackArrayHeader()
            windows += GameWeatherWindow(unpacker.unpackString(), unpacker.unpackInt(), unpacker.unpackInt())
        }
        return GameWeather(zone, current, windows)
    }

    fun readJobs(unpacker: MessageUnpacker): List<GameJob> {
        unpacker.unpackArrayHeader()
        unpacker.skipValue()
        val count = unpacker.unpackArrayHeader()
        return List(count) {
            val fields = unpacker.unpackArrayHeader()
            val job = GameJob(
                unpacker.unpackLong(), unpacker.unpackString(), unpacker.unpackString(), unpacker.unpackString(),
                unpacker.unpackInt(), unpacker.unpackBoolean(), unpacker.unpackInt(),
                if (fields > 7) unpacker.unpackInt() else 0,
                if (fields > 8) unpacker.unpackInt() else -1,
            )
            repeat((fields - 9).coerceAtLeast(0)) { unpacker.skipValue() }
            job
        }
    }

    fun encodeJobsAction(gearsetId: Int): ByteArray = pack {
        packArrayHeader(1)
        packInt(gearsetId)
    }

    fun encodeTeleport(name: String): ByteArray = pack {
        packArrayHeader(1)
        packString(name)
    }

    fun readHousing(unpacker: MessageUnpacker): GameHousingLocation {
        unpacker.unpackArrayHeader()
        val ward = if (unpacker.tryUnpackNil()) null else unpacker.unpackInt()
        val plot = if (unpacker.tryUnpackNil()) null else unpacker.unpackInt()
        val exterior = unpacker.unpackBoolean()
        val wing = if (unpacker.tryUnpackNil()) null else unpacker.unpackInt()
        return GameHousingLocation(ward, plot, exterior, wing)
    }

    fun readDailies(unpacker: MessageUnpacker): GameDailies {
        unpacker.unpackArrayHeader()
        unpacker.skipValue()
        val dailyReset = unpacker.unpackLong()
        val weeklyReset = unpacker.unpackLong()
        val count = unpacker.unpackArrayHeader()
        val entries = List(count) {
            unpacker.unpackArrayHeader()
            GameDailyEntry(
                unpacker.unpackString(), unpacker.unpackString(), unpacker.unpackBoolean(), unpacker.unpackBoolean(),
                unpacker.unpackBoolean(), unpacker.unpackBoolean(), unpacker.unpackInt(), unpacker.unpackInt(), unpacker.unpackString(),
            )
        }
        return GameDailies(dailyReset, weeklyReset, entries)
    }

    fun readActivity(unpacker: MessageUnpacker): GameActivity {
        unpacker.unpackArrayHeader()
        unpacker.skipValue()
        return GameActivity(
            unpacker.unpackLong(), unpacker.unpackLong(), unpacker.unpackLong(), unpacker.unpackInt(),
            unpacker.unpackLong(), unpacker.unpackInt(), unpacker.unpackLong(), unpacker.unpackLong(),
            unpacker.unpackInt(), unpacker.unpackLong(), unpacker.unpackInt(), unpacker.unpackInt(),
            unpacker.unpackInt(), unpacker.unpackInt(), unpacker.unpackInt(), unpacker.unpackInt(),
            unpacker.unpackInt(), unpacker.unpackInt(),
        )
    }

    fun readCollections(unpacker: MessageUnpacker): GameCollections {
        unpacker.unpackArrayHeader()
        val updated = unpacker.unpackLong()
        val categoryCount = unpacker.unpackArrayHeader()
        val categories = ArrayList<GameCollectionCategory>(categoryCount)
        repeat(categoryCount) {
            val shape = unpacker.unpackArrayHeader()
            val id = unpacker.unpackInt()
            val total = unpacker.unpackInt()
            val owned = unpacker.unpackInt()
            val itemCount = unpacker.unpackArrayHeader()
            val items = ArrayList<GameCollectionItem>(itemCount)
            repeat(itemCount) {
                val itemShape = unpacker.unpackArrayHeader()
                val itemId = unpacker.unpackLong()
                val name = nullableString(unpacker) ?: "收藏 $itemId"
                val iconId = unpacker.unpackInt()
                val isOwned = if (itemShape > 3) unpacker.unpackBoolean() else true
                items += GameCollectionItem(itemId, name, iconId, isOwned)
            }
            categories += GameCollectionCategory(id, total, owned, items)
        }
        return GameCollections(categories)
    }

    fun readMaps(unpacker: MessageUnpacker): GameMaps {
        // MessagePack-CSharp normally emits keyed objects as arrays, but older
        // plugin builds may emit maps. Decode the complete value so either
        // representation remains compatible during plugin updates.
        val root = unpacker.unpackValue()
        fun field(value: Value, index: Int): Value? = when {
            value.isArrayValue() -> value.asArrayValue().getOrNilValue(index)
            value.isMapValue() -> value.asMapValue().entrySet().firstOrNull {
                it.key.isIntegerValue() && it.key.asIntegerValue().asInt() == index
            }?.value
            else -> null
        }
        fun text(value: Value?): String = value?.takeIf { it.isStringValue() }?.asStringValue()?.asString().orEmpty()
        fun number(value: Value?): Long = value?.takeIf { it.isIntegerValue() }?.asIntegerValue()?.asLong() ?: 0L
        fun array(value: Value?): List<Value> = when {
            value?.isArrayValue() == true -> value.asArrayValue().list()
            value?.isMapValue() == true -> value.asMapValue().values().toList()
            else -> emptyList()
        }

        val expansions = array(field(root, 2)).map { expansion ->
            val regions = array(field(expansion, 2)).map { region ->
                val destinations = array(field(region, 2)).map { destination ->
                    GameMapDestination(number(field(destination, 0)), text(field(destination, 1)), number(field(destination, 2)).toInt())
                }
                GameMapRegion(text(field(region, 0)), number(field(region, 1)).toInt(), destinations)
            }
            GameMapExpansion(text(field(expansion, 0)), number(field(expansion, 1)).toInt(), regions)
        }
        return GameMaps(text(field(root, 0)), text(field(root, 1)), expansions)
    }

    fun readFishing(unpacker: MessageUnpacker): GameFishingLog {
        val fields = unpacker.unpackArrayHeader()
        val updated = unpacker.unpackLong()
        fun bits(): ByteArray = unpacker.readPayload(unpacker.unpackBinaryHeader())
        val fish = if (fields > 1) bits() else byteArrayOf()
        val spearfish = if (fields > 2) bits() else byteArrayOf()
        return GameFishingLog(updated, fish, spearfish)
    }

    fun readSubmarine(unpacker: MessageUnpacker): GameSubmarine {
        unpacker.unpackArrayHeader()
        val updated = unpacker.unpackLong()
        val count = unpacker.unpackArrayHeader()
        val vessels = ArrayList<GameSubmarineVessel>(count)
        repeat(count) {
            unpacker.unpackArrayHeader()
            vessels += GameSubmarineVessel(
                name = unpacker.unpackString(),
                returnUnix = unpacker.unpackLong(),
                rankId = unpacker.unpackInt(),
                currentExp = unpacker.unpackLong(),
                nextLevelExp = unpacker.unpackLong(),
            )
        }
        return GameSubmarine(updated, vessels)
    }

    fun readChannel(unpacker: MessageUnpacker): Pair<Int, String> {
        unpacker.unpackArrayHeader()
        return unpacker.unpackByte().toInt() to unpacker.unpackString()
    }

    private fun nullableString(unpacker: MessageUnpacker): String? = if (unpacker.tryUnpackNil()) null else unpacker.unpackString()

    private fun decodeXiv(bytes: ByteArray): String {
        val out = ByteArrayOutputStream(bytes.size)
        var i = 0
        while (i < bytes.size) {
            val b = bytes[i].toInt() and 0xff
            if (b == 2 && i + 1 < bytes.size) {
                i += 2
                val (length, next) = readXivInteger(bytes, i)
                i = (next + length + 1).coerceAtMost(bytes.size)
                continue
            }
            if (b >= 0x20 || b == 0x0a || b == 0x09) out.write(b)
            i++
        }
        return out.toByteArray().toString(UTF_8).trim()
    }

    private fun readXivInteger(bytes: ByteArray, offset: Int): Pair<Int, Int> {
        if (offset >= bytes.size) return 0 to bytes.size
        var marker = bytes[offset].toInt() and 0xff
        if (marker < 0xd0) return (marker - 1).coerceAtLeast(0) to (offset + 1)
        marker = (marker + 1) and 0x0f
        val valueBytes = ByteArray(4)
        var cursor = offset + 1
        for (index in 3 downTo 0) {
            if (marker and (1 shl index) != 0 && cursor < bytes.size) {
                valueBytes[index] = bytes[cursor++]
            }
        }
        return ByteBuffer.wrap(valueBytes).order(ByteOrder.LITTLE_ENDIAN).int.coerceAtLeast(0) to cursor
    }
}

internal class EncryptedFrameReader(private val input: DataInputStream, private val sodium: LazySodiumAndroid, private val key: ByteArray) {
    fun next(): ByteArray = XivChatCodec.readSecret(input, sodium, key)
}
