package com.quserh.eorzeaphone.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import org.json.JSONArray
import org.json.JSONObject

/**
 * 聊天档案的读写。消息内容只追加；存储维护可以按用户设置裁掉最旧记录。
 *
 * `chatCache`（SharedPreferences）只承担最近热窗口；SQLite 负责较完整的历史
 * 搜索/回溯。这样不会把整份聊天正文同时塞进 XML 和数据库。基本聊天功能仍可
 * 在档案读写失败时依靠热窗口继续工作。所以：
 *
 * - **写**（[appendNewer]）内部不抛异常。调用点在 `saveChats()` 的协程里，而那个
 *   scope 没装 CoroutineExceptionHandler，未捕获异常会走到
 *   MainActivity 的 default handler 并**结束进程**（已核实 MainActivity.kt:25-37）。
 *   档案是附加能力，不能连坐主存储，更不能崩 App。
 * - 但失败**必须留痕**（[lastError]），不能让"写失败"伪装成"没有记录"。
 * - **读**允许抛。读失败就该被看见，静默返回空表会让用户无法区分"真的没有"和"读挂了"。
 */
internal class ChatStore private constructor(context: Context) {
    private val helper = ChatArchive(context)

    /** 最近一次写入失败的信息，null 表示正常。给设置页/诊断用。 */
    @Volatile
    var lastError: String? = null
        private set

    /**
     * 把 [snapshot] 里比档案已有记录更新的消息追加进去。**从不删除。**
     *
     * 去重的难点：聊天消息**没有稳定 id**，所以任何基于内容的判重都是启发式。
     * 试过两种，都实测丢数据（脚本在 开发/WIKI/）：
     *
     *  1. UNIQUE 索引 + INSERT OR IGNORE —— 丢同毫秒的多条相同消息。
     *     战斗日志（AoE/持续伤害，sender 空、文本相同）和刷屏发言稳定触发。
     *     `_probe_chat_dedupe.py`：10 条里丢 4 条。
     *  2. 边界毫秒整体重写（DELETE 再重插）—— 丢**已归档但已离开内存**的同毫秒消息。
     *     入口是真实的：清空会话只从内存移除、档案照留（有意设计），若被清的消息
     *     和留下的同毫秒，下次追加就会连坐删掉它。
     *     `_probe_chat_boundary_gap.py`：档案 2 → 应为 3，实为 2。
     *
     * 所以这里用第三种：**不判断"某条在不在"，改成比较数量。** 对边界毫秒里每个
     * (timestamp, channel, sender, text) 组，档案已有 n 条、snapshot 有 m 条，
     * 只插 max(0, m - n) 条。删除永不发生，已归档的不可能被连坐；差额补齐，
     * 同毫秒的多条留得住、重复存也不增长。`_probe_chat_surplus.py` 8 个场景全过。
     *
     * 组内取**后** (m - n) 条，不是前几条：snapshot 保持到达顺序，已归档的对应组内
     * 最早出现的位置，所以已存过的是前 n 条。取前几条等于把存过的又存一遍、同时
     * 把真正新的丢掉——条数一样，挑的行不同，光比数量测不出来。
     * `_probe_chat_kotlin_transcript.py` 就是靠附属字段（chunks 等）差异查出这个的。
     *
     * 那个边界查询走 `idx_char_ts (charKey, timestamp)`。v1 曾另有一个把 text 也
     * 包进去的 `idx_dedupe`，v2 删了——正文双份存磁盘换 0.0025ms 不值，
     * 见 [ChatArchive.onCreate]。
     *
     * 空 charKey 直接返回：`charPrefs()` 在空 key 时回退到全局 prefs，若档案照样按
     * 空串建分区，多个未识别角色的记录会混进同一分区。宁可少存，不要混。
     *
     * **读水位线 / 算差额 / 插入必须在同一个事务里。** 三步各自进出锁的话，两个线程
     * 会都读到旧水位线、各自算出同一份差额、然后都插进去。这不是上面那条幂等性——
     * 串行重复调用时第二次能看见第一次的插入，并发时看不见。
     * `_probe_chat_surplus_race.py`：8 线程同存一份快照，应有 2 实存 8。
     * 真机可达：`saveChats()` 13 个调用点全都 `scope.launch(Dispatchers.IO)`，
     * 而 IO 是多线程池；工程没开 WAL，所以是单连接 + 锁，单条语句串行但序列不串行。
     *
     * 已知边界，**故意不管**：`fresh` 按 `timestamp >= since` 过滤，所以时间戳早于
     * 水位线且从未归档过的消息会被丢掉。稳态不可能触发（消息按时间到达）；入口是
     * PC 时钟被往回拨。要管就得放弃水位线改全量比对，代价不成比例。
     *
     * 不抛异常（见类注释），失败写进 [lastError]。
     *
     * @return 实际写入的条数；-1 表示失败。
     */
    fun appendNewer(charKey: String, snapshot: List<GameChatMessage>): Int {
        if (charKey.isBlank() || snapshot.isEmpty()) return 0
        return try {
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                val since = latestTimestamp(db, charKey)
                // 只看不早于水位线的，避免每次全量试插。
                val fresh = if (since <= 0L) snapshot else snapshot.filter { it.timestamp >= since }
                // 空手而归也要 setTransactionSuccessful：没东西写不代表要回滚。
                // 这是最常见的路径（多数 saveChats 没有新消息），不标成功的话
                // 每次都留一条回滚警告日志。
                if (fresh.isEmpty()) { db.setTransactionSuccessful(); return 0 }

                // 严格更新的直接插；边界那一毫秒补差额。
                val toInsert = ArrayList<GameChatMessage>(fresh.size)
                if (since <= 0L) {
                    toInsert.addAll(fresh)
                } else {
                    val boundary = ArrayList<GameChatMessage>()
                    for (m in fresh) {
                        if (m.timestamp > since) toInsert.add(m) else boundary.add(m)
                    }
                    if (boundary.isNotEmpty()) {
                        val have = HashMap<Triple<Int, String, String>, Int>()
                        db.rawQuery(
                            "SELECT channel, sender, text FROM ${ChatArchive.TABLE} " +
                                "WHERE charKey = ? AND timestamp = ?",
                            arrayOf(charKey, since.toString()),
                        ).use { c ->
                            while (c.moveToNext()) {
                                val k = Triple(c.getInt(0), c.getString(1) ?: "", c.getString(2) ?: "")
                                have[k] = (have[k] ?: 0) + 1
                            }
                        }
                        // 按 snapshot 的出现顺序取差额，保持到达顺序。
                        val seen = HashMap<Triple<Int, String, String>, Int>()
                        for (m in boundary) {
                            val k = Triple(m.channel, m.sender, m.text)
                            val n = (seen[k] ?: 0) + 1
                            seen[k] = n
                            if (n > (have[k] ?: 0)) toInsert.add(m)
                        }
                    }
                }

                if (toInsert.isEmpty()) { db.setTransactionSuccessful(); return 0 }
                for (m in toInsert) db.insert(ChatArchive.TABLE, null, valuesOf(charKey, m))
                db.setTransactionSuccessful()
                lastError = null
                toInsert.size
            } finally {
                // return 走到这里也会跑，事务不会悬着。
                db.endTransaction()
            }
        } catch (t: Throwable) {
            lastError = "${t.javaClass.simpleName}: ${t.message}"
            -1
        }
    }

    /**
     * 档案里该角色最新一条的时间戳，0 表示空。内部用，异常由 [appendNewer] 兜。
     *
     * 收 [db] 参数而不是自己取 `readableDatabase`：调用点在事务里，必须用同一个
     * 连接。不开 WAL 时两者恰好是同一个连接，但那是巧合，一开 WAL 就变真竞态。
     */
    private fun latestTimestamp(db: SQLiteDatabase, charKey: String): Long =
        db.rawQuery(
            "SELECT MAX(timestamp) FROM ${ChatArchive.TABLE} WHERE charKey = ?", arrayOf(charKey),
        ).use { if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else 0L }

    /** 该角色的档案条数。读路径，允许抛。 */
    fun count(charKey: String): Int {
        if (charKey.isBlank()) return 0
        return helper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM ${ChatArchive.TABLE} WHERE charKey = ?", arrayOf(charKey),
        ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
    }

    /**
     * Apply the same per-character retention limit used by the in-memory/chatCache path.
     *
     * The archive originally only appended, so enabling it silently created a second,
     * unbounded copy of chat history. A positive [limit] now has one meaning everywhere:
     * keep the newest N messages for this character. `0` remains the explicit "unlimited"
     * choice from settings.
     *
     * @return rows removed, or -1 when maintenance failed.
     */
    fun trimToLimit(charKey: String, limit: Int): Int {
        if (charKey.isBlank() || limit <= 0) return 0
        return try {
            val db = helper.writableDatabase
            db.beginTransaction()
            try {
                val before = db.rawQuery(
                    "SELECT COUNT(*) FROM ${ChatArchive.TABLE} WHERE charKey = ?",
                    arrayOf(charKey),
                ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
                if (before > limit) {
                    db.execSQL(
                        "DELETE FROM ${ChatArchive.TABLE} WHERE id IN (" +
                            "SELECT id FROM ${ChatArchive.TABLE} WHERE charKey = ? " +
                            "ORDER BY timestamp ASC, id ASC LIMIT ?)",
                        arrayOf(charKey, before - limit),
                    )
                }
                val after = if (before > limit) {
                    db.rawQuery(
                        "SELECT COUNT(*) FROM ${ChatArchive.TABLE} WHERE charKey = ?",
                        arrayOf(charKey),
                    ).use { if (it.moveToFirst()) it.getInt(0) else before }
                } else before
                db.setTransactionSuccessful()
                lastError = null
                (before - after).coerceAtLeast(0)
            } finally {
                db.endTransaction()
            }
        } catch (t: Throwable) {
            lastError = "${t.javaClass.simpleName}: ${t.message}"
            -1
        }
    }

    /**
     * Remove exactly the archived rows represented by [messages].  Game chat
     * packets do not carry a stable id, so match by the same tuple used by the
     * append surplus algorithm and delete only the requested multiplicity.  A
     * duplicate message that was not part of the clear operation is therefore
     * left intact.
     *
     * This is intentionally a hard delete: once a user clears a conversation,
     * later hydration/search must not resurrect it from the SQLite archive.
     */
    fun deleteMessages(charKey: String, messages: Collection<GameChatMessage>): Int {
        if (charKey.isBlank() || messages.isEmpty()) return 0
        return try {
            val db = helper.writableDatabase
            data class Fingerprint(val timestamp: Long, val channel: Int, val sender: String, val text: String)
            val wanted = messages.groupingBy {
                Fingerprint(it.timestamp, it.channel, it.sender, it.text)
            }.eachCount()
            var removed = 0
            db.beginTransaction()
            try {
                for ((key, count) in wanted) {
                    val ids = db.rawQuery(
                        "SELECT id FROM ${ChatArchive.TABLE} WHERE charKey=? AND timestamp=? " +
                            "AND channel=? AND sender=? AND text=? ORDER BY id ASC LIMIT ?",
                        arrayOf(
                            charKey, key.timestamp.toString(), key.channel.toString(),
                            key.sender, key.text, count.toString(),
                        ),
                    ).use { c ->
                        buildList { while (c.moveToNext()) add(c.getLong(0)) }
                    }
                    // Do not mutate the database while the cursor is open; some
                    // OEM SQLite builds invalidate the cursor in that situation.
                    ids.forEach { id ->
                        if (db.delete(
                                ChatArchive.TABLE, "id = ?",
                                arrayOf(id.toString()),
                            ) > 0
                        ) removed++
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            lastError = null
            removed
        } catch (t: Throwable) {
            lastError = "${t.javaClass.simpleName}: ${t.message}"
            -1
        }
    }

    /** Delete every archived message for a character (used by an explicit "clear all"). */
    fun deleteCharacter(charKey: String): Int {
        if (charKey.isBlank()) return 0
        return try {
            val n = helper.writableDatabase.delete(
                ChatArchive.TABLE, "charKey = ?", arrayOf(charKey),
            )
            lastError = null
            n
        } catch (t: Throwable) {
            lastError = "${t.javaClass.simpleName}: ${t.message}"
            -1
        }
    }

    /** Latest rows for cache recovery, oldest first. */
    fun latest(charKey: String, limit: Int = 400): List<GameChatMessage> {
        if (charKey.isBlank() || limit <= 0) return emptyList()
        return helper.readableDatabase.rawQuery(
            "SELECT * FROM ${ChatArchive.TABLE} WHERE charKey = ? " +
                "ORDER BY timestamp DESC, id DESC LIMIT ?",
            arrayOf(charKey, limit.toString()),
        ).use { readAll(it).asReversed() }
    }

    /**
     * Reclaim a materially sparse archive file after a large one-time trim.
     * VACUUM is deliberately gated: it is expensive, so ordinary message writes only reuse
     * free pages and startup maintenance compacts when at least 16 MiB and 25% are free.
     */
    fun compactIfWasteful(): Boolean {
        return try {
            val db = helper.writableDatabase
            fun pragma(name: String): Long = db.rawQuery("PRAGMA $name", null)
                .use { if (it.moveToFirst()) it.getLong(0) else 0L }
            val pages = pragma("page_count")
            val free = pragma("freelist_count")
            val pageSize = pragma("page_size")
            val freeBytes = free * pageSize
            if (pages > 0 && freeBytes >= 16L * 1024L * 1024L && free * 4 >= pages) {
                db.execSQL("VACUUM")
                lastError = null
                true
            } else {
                lastError = null
                false
            }
        } catch (t: Throwable) {
            lastError = "${t.javaClass.simpleName}: ${t.message}"
            false
        }
    }

    /**
     * 档案概览：条数 + 最早/最新时间戳。一次查询拿齐，给搜索页顶部的
     * 「N 条，X 到 Y」用。空档案返回 [Overview.EMPTY]（count = 0，两个时间戳都是 0）。
     *
     * 读路径，允许抛。
     */
    fun overview(charKey: String): Overview {
        if (charKey.isBlank()) return Overview.EMPTY
        return helper.readableDatabase.rawQuery(
            "SELECT COUNT(*), MIN(timestamp), MAX(timestamp) FROM ${ChatArchive.TABLE} " +
                "WHERE charKey = ?",
            arrayOf(charKey),
        ).use {
            if (!it.moveToFirst() || it.getInt(0) == 0) Overview.EMPTY
            else Overview(it.getInt(0), it.getLong(1), it.getLong(2))
        }
    }

    /** [overview] 的返回值。 */
    data class Overview(val count: Int, val earliestMs: Long, val latestMs: Long) {
        companion object { val EMPTY = Overview(0, 0L, 0L) }
    }

    /**
     * 档案里实际出现过的频道及各自条数，条数多的在前。
     *
     * 给筛选 chip 用：只列档案里真有的频道，而不是把游戏所有频道号都摆出来——
     * 大部分用户的档案里只有小队/部队/密语几个。
     *
     * 读路径，允许抛。
     */
    fun channelCounts(charKey: String): List<Pair<Int, Int>> {
        if (charKey.isBlank()) return emptyList()
        return helper.readableDatabase.rawQuery(
            "SELECT channel, COUNT(*) FROM ${ChatArchive.TABLE} WHERE charKey = ? " +
                "GROUP BY channel ORDER BY 2 DESC",
            arrayOf(charKey),
        ).use {
            val out = ArrayList<Pair<Int, Int>>(it.count)
            while (it.moveToNext()) out.add(it.getInt(0) to it.getInt(1))
            out
        }
    }

    /**
     * 全文检索。这是换库真正换来的东西——以前只能翻内存里那 5000 条。
     *
     * 用 `LIKE` 而不是 FTS：FTS5 需要编译期开启、Android 各版本不保证；FTS4 要额外
     * 维护一张影子表。当前量级 `LIKE` + `(charKey, timestamp)` 索引够用，
     * 真到瓶颈再上 FTS4。
     *
     * [channels] 空 = 不限频道；[sinceMs]/[untilMs] 为 0 = 不限时间。
     * 读路径，允许抛。
     */
    fun search(
        charKey: String,
        query: String,
        channels: Set<Int> = emptySet(),
        sinceMs: Long = 0,
        untilMs: Long = 0,
        limit: Int = 500,
        offset: Int = 0,
    ): List<GameChatMessage> {
        if (charKey.isBlank() || limit <= 0 || offset < 0) return emptyList()
        val where = StringBuilder("charKey = ?")
        val args = mutableListOf(charKey)
        if (query.isNotBlank()) {
            // ESCAPE 让 % _ \ 按字面匹配，否则用户搜 "100%" 会命中一切。
            where.append(" AND (text LIKE ? ESCAPE '\\' OR sender LIKE ? ESCAPE '\\')")
            val like = "%" + query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%"
            args.add(like); args.add(like)
        }
        if (channels.isNotEmpty()) {
            where.append(" AND channel IN (${channels.joinToString(",") { "?" }})")
            channels.forEach { args.add(it.toString()) }
        }
        if (sinceMs > 0) { where.append(" AND timestamp >= ?"); args.add(sinceMs.toString()) }
        if (untilMs > 0) { where.append(" AND timestamp <= ?"); args.add(untilMs.toString()) }
        return helper.readableDatabase.rawQuery(
            "SELECT * FROM ${ChatArchive.TABLE} WHERE $where ORDER BY timestamp DESC, id DESC LIMIT ? OFFSET ?",
            (args + limit.toString() + offset.toString()).toTypedArray(),
        ).use { readAll(it) }
    }

    /** 时间区间内的消息，正序。用于"去年今天"这类回溯。读路径，允许抛。 */
    fun between(charKey: String, sinceMs: Long, untilMs: Long, limit: Int = 5000): List<GameChatMessage> {
        if (charKey.isBlank()) return emptyList()
        return helper.readableDatabase.rawQuery(
            "SELECT * FROM ${ChatArchive.TABLE} WHERE charKey = ? AND timestamp >= ? " +
                "AND timestamp <= ? ORDER BY timestamp ASC, id ASC LIMIT ?",
            arrayOf(charKey, sinceMs.toString(), untilMs.toString(), limit.toString()),
        ).use { readAll(it) }
    }

    // ---- 映射：字段与 chatCache 那套 JSON 逐字对应，换介质不换数据形状 ----

    private fun valuesOf(charKey: String, m: GameChatMessage) = ContentValues().apply {
        put("charKey", charKey)
        put("timestamp", m.timestamp)
        put("sender", m.sender)
        put("text", m.text)
        put("channel", m.channel)
        put("self", if (m.self) 1 else 0)
        // sendState 不入档，见 ChatArchive.onCreate 里的说明。
        put("chunks", encodeChunks(m.chunks))
        put("senderName", m.senderName)
        put("senderWorld", m.senderWorld)
        put("senderStatusName", m.senderStatusName)
        put("senderStatusIcon", m.senderStatusIcon)
        put("senderWorldIcon", m.senderWorldIcon)
        put("characterTag", m.characterTag)
        put("targetName", m.targetName)
        put("targetWorld", m.targetWorld)
        put("selfFlag", if (m.selfFlag) 1 else 0)
    }

    private fun readAll(c: Cursor): List<GameChatMessage> {
        val out = ArrayList<GameChatMessage>(c.count)
        while (c.moveToNext()) out.add(readOne(c))
        return out
    }

    private fun readOne(c: Cursor): GameChatMessage {
        // 旧 JSON 用"空串视为 null"，这里保持同样行为，免得同一条消息经不同
        // 路径读出来不一样。
        fun str(name: String): String? =
            c.getColumnIndex(name).takeIf { it >= 0 && !c.isNull(it) }
                ?.let { c.getString(it) }?.takeIf { it.isNotBlank() }

        fun intOrNull(name: String): Int? =
            c.getColumnIndex(name).takeIf { it >= 0 && !c.isNull(it) }?.let { c.getInt(it) }

        return GameChatMessage(
            timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp")),
            sender = c.getString(c.getColumnIndexOrThrow("sender")) ?: "",
            text = c.getString(c.getColumnIndexOrThrow("text")) ?: "",
            channel = c.getInt(c.getColumnIndexOrThrow("channel")),
            self = c.getInt(c.getColumnIndexOrThrow("self")) != 0,
            // sendState 不入档，取 data class 默认值 0。
            chunks = decodeChunks(
                c.getColumnIndex("chunks").takeIf { it >= 0 && !c.isNull(it) }?.let { c.getString(it) }
            ),
            senderName = str("senderName"),
            senderWorld = str("senderWorld"),
            senderStatusName = str("senderStatusName"),
            senderStatusIcon = intOrNull("senderStatusIcon"),
            senderWorldIcon = intOrNull("senderWorldIcon"),
            characterTag = str("characterTag"),
            targetName = str("targetName"),
            targetWorld = str("targetWorld"),
            selfFlag = c.getInt(c.getColumnIndexOrThrow("selfFlag")) != 0,
        )
    }

    private fun encodeChunks(chunks: List<GameChatChunk>): String? {
        if (chunks.isEmpty()) return null
        val arr = JSONArray()
        for (ch in chunks) {
            arr.put(JSONObject().apply {
                if (ch.text != null) put("text", ch.text)
                if (ch.icon != null) put("icon", ch.icon)
                put("italic", ch.italic)
                if (ch.foreground != null) put("foreground", ch.foreground)
            })
        }
        return arr.toString()
    }

    private fun decodeChunks(raw: String?): List<GameChatChunk> {
        if (raw.isNullOrBlank()) return emptyList()
        val arr = JSONArray(raw)
        return buildList(arr.length()) {
            for (j in 0 until arr.length()) {
                val o = arr.getJSONObject(j)
                add(GameChatChunk(
                    o.optString("text").takeIf { o.has("text") && !o.isNull("text") },
                    o.optInt("icon", -1).takeIf { it >= 0 },
                    o.optBoolean("italic"),
                    o.optLong("foreground", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE },
                ))
            }
        }
    }

    companion object {
        @Volatile
        private var instance: ChatStore? = null

        /** 单例：SQLiteOpenHelper 自带连接池，不该每次调用都新建。 */
        fun of(context: Context): ChatStore =
            instance ?: synchronized(this) {
                instance ?: ChatStore(context.applicationContext).also { instance = it }
            }
    }
}
