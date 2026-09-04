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
    // Must match the plugin's SecretMessage.MaxMessageLen. 8 MB: the category tree
    // is a single ~2 MB frame, and the old 128 KB cap made both sides reject it
    // (the plugin refused to send; this side would have dropped the connection).
    const val MAX_FRAME = 8_000_000

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
        // Over-limit frames are SKIPPED, never fatal. The old behaviour (throw ->
        // drop the connection) made every app older than the 8 MB limit enter a
        // 30 s reconnect loop against the new plugin: connect, request categories,
        // hit the 2 MB tree frame, disconnect, repeat -- and every reconnect reset
        // the live board mid-read. Read the frame out and move on instead; the
        // caller's empty-frame branch already skips it.
        if (length <= 16 || length > MAX_FRAME) {
            if (length in (MAX_FRAME + 1) until (1 shl 30)) {
                input.readFully(ByteArray(length))
            }
            return ByteArray(0)
        }
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

    fun encodeCatchUp(afterMillis: Long): ByteArray = pack { packArrayHeader(1); packLong(afterMillis) }
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
        // Must equal the number of packInt/packBoolean pairs below. A short header
        // makes the server stop reading early, silently dropping the trailing
        // preferences -- this was already off by one before market was added.
        packMapHeader(12)
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
        packInt(13)
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
        val senderStatus = if (fields > 7) nullableString(unpacker) else null
        val senderIcon = if (fields > 8) unpacker.tryUnpackNil().let { if (it) null else unpacker.unpackInt() } else null
        val characterTag = if (fields > 9) nullableString(unpacker) else null
        val targetName = if (fields > 10) nullableString(unpacker) else null
        val targetWorld = if (fields > 11) nullableString(unpacker) else null
        val selfFlag = if (fields > 12) unpacker.unpackBoolean() else false
        val senderWorldIcon = if (fields > 13) unpacker.tryUnpackNil().let { if (it) null else unpacker.unpackInt() } else null
        return GameChatMessage(time, sender, text, channel, chunks = chunks, senderName = senderName, senderWorld = senderWorld, senderStatusName = senderStatus, senderStatusIcon = senderIcon, characterTag = characterTag, targetName = targetName, targetWorld = targetWorld, selfFlag = selfFlag, senderWorldIcon = senderWorldIcon)
    }
    // The chat line text can embed "[频道]<名字> content" / "<名字> content" / "名字：content".
    // Keep only the actual content; the app shows sender/channel separately.
    private fun stripChatPrefix(raw: String, sender: String): String {
        var t = raw.trim()
        if (t.startsWith('[')) {
            val close = t.indexOf(']')
            if (close >= 0) t = t.substring(close + 1).trim()
        }
        // Strip a leading "<PlayerName> content" prefix ONLY when the angle
        // bracket sits at the very start and is followed by a space. Sound
        // effects (<se.11>) and mid-text tags are content, not a name prefix -
        // stripping them would drop everything before the tag (e.g. a macro
        // "辅导员来查寝了 <se.11>" must keep the text).
        if (t.startsWith('<')) {
            val gt = t.indexOf('>')
            if (gt >= 0) {
                val after = t.substring(gt + 1)
                if (after.startsWith(" ") && !t.substring(0, gt).trimStart().startsWith("se.", ignoreCase = true)) {
                    return after.trim()
                }
            }
        }
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
            result += GameFriend(name ?: "未知玩家", currentWorld ?: "", homeWorld ?: "", freeCompany ?: "", territory ?: "", status and (1L shl 47) != 0L, job ?: "", contentId, currentWorldId, homeWorldId, classJob, status)
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

    fun encodeMarketSearch(itemId: Int, hqOnly: Boolean): ByteArray = pack {
        packArrayHeader(2)
        packInt(itemId)
        packBoolean(hqOnly)
    }

    /**
     * Buy one listing.
     *
     * Carries what the screen showed rather than a row index: the plugin re-reads the
     * board and refuses unless price/quantity/HQ still match, so a board that changed
     * between render and tap aborts instead of buying the wrong listing.
     */
    fun encodeMarketPurchase(
        itemId: Int,
        listingId: Long,
        expectedUnitPrice: Int,
        expectedQuantity: Int,
        expectedHq: Boolean,
    ): ByteArray = pack {
        packArrayHeader(5)
        packInt(itemId)
        packLong(listingId)
        packInt(expectedUnitPrice)
        packInt(expectedQuantity)
        packBoolean(expectedHq)
    }

    /** Request the market category/item tree. */
    fun encodeMarketCategories(): ByteArray = pack {
        packArrayHeader(0)
    }

    /**
     * Live market listings read from the game client.
     *
     * Status is read before the rows so an empty result can explain itself (board
     * open on the PC, in a duty, item untradable) instead of rendering a blank table.
     */
    fun readMarket(unpacker: MessageUnpacker): GameMarket {
        val fields = unpacker.unpackArrayHeader()
        val updatedUnix = unpacker.unpackLong()
        val itemId = unpacker.unpackInt()
        val status = unpacker.unpackInt()
        val count = unpacker.unpackArrayHeader()
        val rows = ArrayList<GameMarketListing>(count)
        repeat(count) {
            val listingFields = unpacker.unpackArrayHeader()
            val listingId = unpacker.unpackLong()
            val unitPrice = unpacker.unpackInt()
            val quantity = unpacker.unpackInt()
            val hq = unpacker.unpackBoolean()
            // Key(4) used to be RetainerName, which the game never fills for ordinary
            // listings; it now carries the retainer city. Older plugins sent only the
            // original four fields, so do not consume the next row/result field when
            // the optional town value is absent.
            val town = if (listingFields > 4) nullableString(unpacker) ?: "" else ""
            val tax = if (listingFields > 5) unpacker.unpackInt() else 0
            val isSet = if (listingFields > 6) unpacker.unpackBoolean() else false
            val materia = if (listingFields > 7) unpacker.unpackInt() else 0
            repeat((listingFields - 8).coerceAtLeast(0)) { unpacker.skipValue() }
            rows += GameMarketListing(
                listingId, unitPrice, quantity, hq, town, tax, isSet, materia,
            )
        }
        val world = if (fields > 4) nullableString(unpacker) ?: "" else ""
        // Key(5) NPC vendor price (Item.PriceMid), added when benchmark lines arrived.
        val npcPrice = if (fields > 5) unpacker.unpackInt() else 0
        return GameMarket(updatedUnix, itemId, status, rows, world, npcPrice)
    }

    /**
     * Opcode 22: outcome of a purchase attempt from the phone.
     *
     * Always sent, refusals included, so the app never has to infer the result of
     * a gil transaction from silence.
     */
    fun readMarketPurchase(unpacker: MessageUnpacker): GameMarketPurchase {
        unpacker.unpackArrayHeader()
        val updatedUnix = unpacker.unpackLong()
        val itemId = unpacker.unpackInt()
        val status = unpacker.unpackInt()
        val listingId = unpacker.unpackLong()
        val unitPrice = unpacker.unpackInt()
        val quantity = unpacker.unpackInt()
        val tax = unpacker.unpackInt()
        val errorId = unpacker.unpackInt()
        return GameMarketPurchase(updatedUnix, itemId, status, listingId, unitPrice, quantity, tax, errorId)
    }

    /**
     * Opcode 23: market category tree with all tradable items.
     *
     * The plugin serialises MessagePack array layout, so a category is
     * `[id, name, order, subcategories[], iconId]` and an item is
     * `[itemId, name, iconId, level, canBeHq, npcPrice]`. Trailing fields were added
     * later; the readers stay tolerant of older plugins that sent the original
     * three-field shape (`[id, name, items]`, items `[id, name]`), which is what
     * shipped before the wire ever decoded successfully.
     */
    fun readMarketCategories(unpacker: MessageUnpacker): GameMarketCategories {
        if (unpacker.tryUnpackNil()) return GameMarketCategories(emptyList())
        val topFields = unpacker.unpackArrayHeader()
        val categoryCount = unpacker.unpackArrayHeader()
        val categories = (0 until categoryCount).map {
            val catFields = unpacker.unpackArrayHeader()
            val id = unpacker.unpackInt()
            val name = unpacker.unpackString()
            if (catFields > 3) {
                // New: [id, name, order, subcategories[], iconId?]
                val order = unpacker.unpackInt()
                val subCount = unpacker.unpackArrayHeader()
                val subs = ArrayList<GameMarketSubcategory>()
                var iconId = 0
                repeat(subCount) {
                    if (unpacker.tryUnpackNil()) return@repeat
                    val subFields = unpacker.unpackArrayHeader()
                    val subId = unpacker.unpackInt()
                    val subName = unpacker.unpackString()
                    val subOrder = if (subFields > 2) unpacker.unpackInt() else 0
                    val subItemCount = unpacker.unpackArrayHeader()
                    val items = ArrayList<GameMarketItem>(subItemCount)
                    repeat(subItemCount) { readMarketItem(unpacker)?.let(items::add) }
                    val subIcon = if (subFields > 4) unpacker.unpackInt() else 0
                    subs += GameMarketSubcategory(subId, subName, subOrder, subIcon, items)
                    if (iconId == 0) iconId = subIcon
                }
                if (catFields > 4) iconId = unpacker.unpackInt()
                GameMarketCategory(id, name, order, iconId, subs)
            } else {
                // Old plugin: [id, name, items[]] with two-field items -- keep it
                // browsable as a single synthetic subcategory.
                val items = if (catFields == 3) {
                    val n = unpacker.unpackArrayHeader()
                    (0 until n).map {
                        unpacker.unpackArrayHeader()
                        GameMarketItem(unpacker.unpackInt(), unpacker.unpackString())
                    }
                } else emptyList()
                GameMarketCategory(
                    id, name, 0, 0,
                    if (items.isEmpty()) emptyList()
                    else listOf(GameMarketSubcategory(id, name, 0, 0, items)),
                )
            }
        }

        // Read optional timestamp (Key 1) and game version (Key 2) if present
        var timestampMs = 0L
        var gameVersion = ""
        if (topFields > 1) {
            timestampMs = unpacker.unpackLong()
        }
        if (topFields > 2) {
            gameVersion = unpacker.unpackString()
        }

        return GameMarketCategories(categories, timestampMs, gameVersion)
    }

    /** One `[itemId, name, iconId, level, canBeHq, npcPrice]` row; nil-tolerant for safety. */
    private fun readMarketItem(unpacker: MessageUnpacker): GameMarketItem? {
        if (unpacker.tryUnpackNil()) return null
        val fields = unpacker.unpackArrayHeader()
        val itemId = unpacker.unpackInt()
        val itemName = unpacker.unpackString()
        val iconId = if (fields > 2) unpacker.unpackInt() else 0
        val level = if (fields > 3) unpacker.unpackInt() else 0
        val hq = if (fields > 4) unpacker.unpackBoolean() else false
        val npcPrice = if (fields > 5) unpacker.unpackInt() else 0
        repeat((fields - 6).coerceAtLeast(0)) { unpacker.skipValue() }
        return GameMarketItem(itemId, itemName, iconId, level, hq, npcPrice)
    }

    /**
     * Opcode 24: unsolicited price-monitor event.
     *
     * `[updatedUnix, itemId, kind, price, quantity, detail]` -- kind indexes
     * [GameMonitorEventKind].
     */
    fun readMarketMonitorEvent(unpacker: MessageUnpacker): GameMarketMonitorEvent {
        unpacker.unpackArrayHeader()
        val updatedUnix = unpacker.unpackLong()
        val itemId = unpacker.unpackInt()
        val kind = unpacker.unpackInt()
        val price = unpacker.unpackInt()
        val quantity = unpacker.unpackInt()
        val detail = nullableString(unpacker) ?: ""
        return GameMarketMonitorEvent(updatedUnix, itemId, kind, price, quantity, detail)
    }

    /**
     * Replace the plugin's monitor rules. A full-list sync keeps the phone the
     * source of truth: the plugin keeps its bookkeeping (bought counts) for rules
     * whose item id survives the replace.
     */
    fun encodeMarketMonitorSync(entries: List<MarketMonitorRule>): ByteArray = pack {
        packArrayHeader(1)
        packArrayHeader(entries.size)
        entries.forEach { e ->
            packArrayHeader(5)
            packInt(e.itemId)
            packInt(e.threshold)
            packBoolean(e.hqOnly)
            packBoolean(e.autoBuy)
            packInt(e.buyCap)
        }
    }

    /**
     * Crafting-recipe request/response codec kept for compatibility with the
     * experimental recipe event. Current plugin builds do not emit opcode 25,
     * but older development APKs may still send the compact array described here.
     * Keeping the decoder tolerant means an optional plugin update cannot break
     * the whole encrypted stream just because a recipe frame is encountered.
     */
    fun encodeRecipeRequest(itemId: Int): ByteArray = pack {
        packArrayHeader(1)
        packInt(itemId)
    }

    fun readRecipe(unpacker: MessageUnpacker): GameRecipe {
        val root = unpacker.unpackValue()

        fun field(value: Value?, index: Int, vararg names: String): Value? = when {
            value == null -> null
            value.isArrayValue() -> value.asArrayValue().getOrNilValue(index)
            value.isMapValue() -> value.asMapValue().entrySet().firstOrNull { entry ->
                val key = entry.key
                (key.isIntegerValue() && key.asIntegerValue().asInt() == index) ||
                    (key.isStringValue() && key.asStringValue().asString() in names)
            }?.value
            else -> null
        }
        fun int(value: Value?): Int = when {
            value?.isIntegerValue() == true -> value.asIntegerValue().asInt()
            value?.isFloatValue() == true -> value.asFloatValue().toFloat().toInt()
            value?.isStringValue() == true -> value.asStringValue().asString().toIntOrNull() ?: 0
            else -> 0
        }
        fun text(value: Value?): String = when {
            value?.isStringValue() == true -> value.asStringValue().asString()
            else -> ""
        }
        fun values(value: Value?): List<Value> = when {
            value?.isArrayValue() == true -> value.asArrayValue().list()
            value?.isMapValue() == true -> value.asMapValue().values().toList()
            else -> emptyList()
        }

        val ingredients = values(field(root, 6, "ingredients", "Ingredients")).mapNotNull { raw ->
            val itemId = int(field(raw, 0, "itemId", "ItemId", "id", "Id"))
            val name = text(field(raw, 1, "name", "Name"))
            val amount = int(field(raw, 2, "amount", "Amount", "quantity", "Quantity"))
            val icon = int(field(raw, 3, "iconId", "IconId", "icon", "Icon"))
            if (itemId == 0 && name.isBlank() && amount == 0) null
            else GameRecipeIngredient(itemId, name, amount, icon)
        }
        return GameRecipe(
            recipeId = int(field(root, 0, "recipeId", "RecipeId", "id", "Id")),
            itemId = int(field(root, 1, "itemId", "ItemId")),
            itemName = text(field(root, 2, "itemName", "ItemName", "name", "Name")),
            jobId = int(field(root, 3, "jobId", "JobId")),
            jobName = text(field(root, 4, "jobName", "JobName")),
            recipeLevel = int(field(root, 5, "recipeLevel", "RecipeLevel", "level", "Level")),
            ingredients = ingredients,
        )
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
