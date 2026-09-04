package com.quserh.eorzeaphone.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * 聊天长效档案的表结构。
 *
 * 为什么要它：原来聊天记录整份序列化成一个 JSON 字符串塞进 SharedPreferences，
 * 每来一条消息就重写全量（saveChats 有 13 处调用点），所以只能设 5000 条上限。
 * 而"游戏关掉之后还能翻聊天记录"是这个项目的存在理由，那个上限本身就是要拆的。
 *
 * 为什么用裸 SQLiteOpenHelper 而不是 Room：这个工程的依赖是刻意极简的
 * （裸 HttpURLConnection + org.json，没有 OkHttp/序列化库）。单表场景 Room
 * 只多带一套注解处理，不值。
 *
 * `chatCache`（SharedPreferences）现在只保留最近的热窗口（并压缩），而这里
 * 保存可回溯的会话历史。两者不再各自保存一整份 transcript；档案彻底失灵时，
 * 热窗口仍能让基本聊天功能正常工作。
 */
internal class ChatArchive(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DB_NAME, null, DB_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                charKey TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                sender TEXT NOT NULL,
                text TEXT NOT NULL,
                channel INTEGER NOT NULL,
                self INTEGER NOT NULL DEFAULT 0,
                -- sendState 故意不存：它在内存里会就地变（发送中 1 → 已送达 2），而档案
                -- 只增不改，存进来就永远停在首次写入的值。搜索用不到它，存一个永远
                -- 可能是错的字段不如不存。读出来的 GameChatMessage.sendState 取默认 0。
                chunks TEXT,
                senderName TEXT,
                senderWorld TEXT,
                senderStatusName TEXT,
                senderStatusIcon INTEGER,
                senderWorldIcon INTEGER,
                characterTag TEXT,
                targetName TEXT,
                targetWorld TEXT,
                selfFlag INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        // 搜索、回溯、去重补差额都按 (角色, 时间) 走，一个索引全覆盖。
        //
        // v1 还有第二个索引 idx_dedupe (charKey, timestamp, channel, sender, text)，
        // v2 删掉了：它把每条消息的正文在磁盘上又存了一份，实测占总体积 29%
        // （20 万条里 17 MB）。而它只服务补差额那一个查询，那个查询按
        // (charKey, timestamp) 精确定位一个毫秒——下面这个索引已经覆盖。
        // 删掉后实测仍走 SEARCH USING INDEX idx_char_ts，没有全表扫，
        // 单次 0.0357ms（原来 0.0332ms），而每次 saveChats 只跑一次。
        // 拿正文双份存储换 0.0025ms 不值。脚本：开发/WIKI/_probe_chat_slim.py
        db.execSQL("CREATE INDEX idx_char_ts ON $TABLE (charKey, timestamp)")
        // 提醒：这里**不要**建 UNIQUE 索引。曾经是 UNIQUE，实测丢真实消息——
        // 同一毫秒内多条「同频道 + 同发送者 + 同文本」的独立消息会被压成 1 条，
        // AoE 战斗日志（sender 为空、文本相同）和刷屏发言稳定触发。
        // 复现：开发/WIKI/_probe_chat_dedupe.py（10 条里丢 4 条）。
        // 去重由 ChatStore.appendNewer 的「水位线 + 边界毫秒补差额」负责。
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // 只增不改的表，迁移也守同一条规矩：只碰派生物（索引），不碰行。
        // 以后加列一律 ALTER TABLE ADD COLUMN，不要 DROP/重建。
        if (oldVersion < 2) {
            // 索引是派生数据，删它不动任何一行。实测 373 行（用户真机的量级）
            // 逐字段比对完全一致、323 个毫秒的补差额查询结果完全一致：
            // 开发/WIKI/_probe_chat_dropindex_migration.py
            //
            // 不在这里 VACUUM：SQLiteOpenHelper 把 onUpgrade 包在事务里，而
            // VACUUM 在事务内会抛 "cannot VACUUM from within a transaction"。
            // 腾出的页进 freelist 会被后续插入复用，所以不 VACUUM 也不浪费，
            // 只是文件不会立刻变小。真要缩文件得在事务外单独跑。
            db.execSQL("DROP INDEX IF EXISTS idx_dedupe")
        }
    }

    companion object {
        private const val DB_NAME = "chat_archive.db"
        // v2: 删掉 idx_dedupe（见 onUpgrade）。
        private const val DB_VERSION = 2
        internal const val TABLE = "messages"
    }
}
