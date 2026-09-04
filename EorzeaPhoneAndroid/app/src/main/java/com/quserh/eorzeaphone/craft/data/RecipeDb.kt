package com.quserh.eorzeaphone.craft.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import java.util.zip.ZipFile

/**
 * Read-only accessor for the craft.db database (same schema as
 * tools/build_craft_db.py). Sources, in order:
 *  1. a previously built db in filesDir,
 *  2. the craft.db.gz bundled in assets (auto-repair re-extracts on corruption),
 *  3. a network rebuild from the live 5p statics pack ([rebuildFromNetwork]).
 */
class RecipeDb(private val context: Context) {

    private var db: SQLiteDatabase? = null

    @Volatile
    var craftableIds: Set<Int> = emptySet()
        private set

    @Volatile
    var version: String = ""
        private set

    /** Distinct craftable item ids, cached for canCraft() checks. */
    val isReady: Boolean get() = db != null

    fun close() {
        runCatching { db?.close() }
        db = null
    }

    /**
     * Open the db, repairing from the bundled asset when the local copy is
     * missing or corrupt. Reports the final outcome through the callbacks;
     * errors are never silently dropped.
     */
    suspend fun prepare(onReady: () -> Unit, onError: (String) -> Unit) = withContext(Dispatchers.IO) {
        val file = File(context.filesDir, DB_NAME)
        try {
            if (!file.exists() || file.length() < 4096) extractAsset(file)
            open(file)
            onReady()
        } catch (first: Throwable) {
            try {
                // A partial extract from an interrupted launch leaves a file that
                // exists but will not open: rebuild it from the asset once.
                file.delete()
                extractAsset(file)
                open(file)
                onReady()
            } catch (second: Throwable) {
                onError(second.message ?: "配方库不可用")
            }
        }
    }

    /**
     * Download the live statics pack (a zip disguised as statics.json) and rebuild
     * the local db from its item/recipe_ja tables. Used as the repair path when
     * the bundled asset will not open, and as a data update channel.
     */
    suspend fun rebuildFromNetwork(
        url: String = DEFAULT_URL,
        onProgress: (String) -> Unit,
    ): Unit = withContext(Dispatchers.IO) {
        onProgress("下载数据包…")
        val zipFile = File(context.cacheDir, "statics.pack")
        download(url, zipFile)

        val tmp = File(context.filesDir, "$DB_NAME.building")
        tmp.delete()
        onProgress("解析数据…")
        var itemCount = 0
        var recipeCount = 0
        ZipFile(zipFile).use { zip ->
            val dbb = SQLiteDatabase.openOrCreateDatabase(tmp, null)
            try {
                dbb.beginTransaction()
                try {
                    // execSQL compiles ONE statement per call; a joined script would
                    // silently create only the first table.
                    SCHEMA_STATEMENTS.forEach { dbb.execSQL(it) }
                    zip.getEntry("item")?.let { entry ->
                        zip.getInputStream(entry).use { stream ->
                            JsonReader(stream.reader().buffered(1 shl 16)).use { reader ->
                                itemCount = parseItems(reader, dbb, onProgress)
                            }
                        }
                    }
                    zip.getEntry("recipe_ja")?.let { entry ->
                        zip.getInputStream(entry).use { stream ->
                            JsonReader(stream.reader().buffered(1 shl 16)).use { reader ->
                                recipeCount = parseRecipes(reader, dbb, onProgress)
                            }
                        }
                    }
                    val meta = dbb.compileStatement("INSERT OR REPLACE INTO meta VALUES(?,?)")
                    meta.bindString(1, "source"); meta.bindString(2, "network"); meta.execute()
                    meta.bindString(1, "built_at"); meta.bindString(2, (System.currentTimeMillis() / 1000).toString()); meta.execute()
                    meta.bindString(1, "items"); meta.bindString(2, itemCount.toString()); meta.execute()
                    meta.bindString(1, "recipes"); meta.bindString(2, recipeCount.toString()); meta.execute()
                    dbb.setTransactionSuccessful()
                } finally {
                    dbb.endTransaction()
                }
            } finally {
                dbb.close()
            }
        }
        zipFile.delete()

        val file = File(context.filesDir, DB_NAME)
        close()
        file.delete()
        if (!tmp.renameTo(file)) {
            tmp.copyTo(file, overwrite = true)
            tmp.delete()
        }
        open(file)
    }

    // ---------------------------------------------------------------- queries

    /** Search across zh/jp/en names; prefix hits rank before substring hits. */
    fun search(query: String, limit: Int = 80): List<CraftItem> {
        val database = db ?: return emptyList()
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val like = "%${escapeLike(q)}%"
        // ESCAPE '\' is rejected by SQLite ("must be a single character" — the
        // backslash swallows the quote during literal parsing); char(92) is the
        // same character as an expression and parses cleanly.
        val esc = "ESCAPE char(92)"
        val rows = mutableListOf<Pair<Int, CraftItem>>()
        val sql = """
            SELECT id, name_cn, name_jp, name_en, icon, ilv, hq, uicat, jobs,
                CASE
                    WHEN name_cn = ? OR name_jp = ? OR name_en = ? THEN 0
                    WHEN name_cn LIKE ? $esc OR name_jp LIKE ? $esc OR name_en LIKE ? $esc THEN 1
                    ELSE 2
                END AS rank_val,
                EXISTS(SELECT 1 FROM recipes r WHERE r.item_id = items.id) AS craft
            FROM items
            WHERE name_cn LIKE ? $esc OR name_jp LIKE ? $esc OR name_en LIKE ? $esc
            ORDER BY craft DESC, rank_val ASC, id ASC
            LIMIT $limit
        """.trimIndent()
        database.rawQuery(sql, arrayOf(q, q, q, "$q%", "$q%", "$q%", like, like, like)).use { cur ->
            while (cur.moveToNext()) {
                rows += cur.getInt(8) to CraftItem(
                    cur.getInt(0), cur.getString(1) ?: "", cur.getString(2) ?: "", cur.getString(3) ?: "",
                    cur.getInt(4), cur.getInt(5), cur.getInt(6) != 0, cur.getInt(7), cur.getInt(9),
                )
            }
        }
        return rows.sortedWith(compareByDescending<Pair<Int, CraftItem>> { it.first }.thenBy { it.second.id })
            .map { it.second }
    }

    fun item(id: Int): CraftItem? {
        val database = db ?: return null
        return database.rawQuery(
            "SELECT name_cn, name_jp, name_en, icon, ilv, hq, uicat, jobs FROM items WHERE id=?",
            arrayOf(id.toString()),
        ).use { cur ->
            if (cur.moveToFirst()) CraftItem(
                id, cur.getString(0) ?: "", cur.getString(1) ?: "", cur.getString(2) ?: "",
                cur.getInt(3), cur.getInt(4), cur.getInt(5) != 0, cur.getInt(6), cur.getInt(7),
            ) else null
        }
    }

    fun jobCatOf(categoryId: Int): JobCat? = jobcats[categoryId]

    fun canCraft(itemId: Int): Boolean = craftableIds.contains(itemId)

    fun recipesFor(itemId: Int): List<CraftRecipe> {
        val database = db ?: return emptyList()
        return database.rawQuery(
            "SELECT id, job, yield, craft_lv, stars, rlv, hq, qs FROM recipes WHERE item_id=? ORDER BY job, craft_lv",
            arrayOf(itemId.toString()),
        ).use { cur ->
            buildList {
                while (cur.moveToNext()) {
                    add(CraftRecipe(
                        id = cur.getInt(0), job = cur.getInt(1), itemId = itemId,
                        yield = cur.getInt(2), craftLv = cur.getInt(3), stars = cur.getInt(4),
                        rlv = cur.getInt(5), hq = cur.getInt(6) != 0, qs = cur.getInt(7) != 0,
                    ))
                }
            }
        }
    }

    fun materialsFor(recipeId: Int): List<MaterialLine> {
        val database = db ?: return emptyList()
        return database.rawQuery(
            "SELECT item_id, qty, is_crystal FROM recipe_mats WHERE recipe_id=? ORDER BY is_crystal DESC, item_id",
            arrayOf(recipeId.toString()),
        ).use { cur ->
            buildList {
                while (cur.moveToNext()) add(MaterialLine(cur.getInt(0), cur.getInt(1), cur.getInt(2) != 0))
            }
        }
    }

    private fun escapeLike(s: String): String =
        s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

    // ---------------------------------------------------------------- internals

    /** MuMu/emulator filesystems intermittently fail writes with EAGAIN; retry. */
    private fun <T> retryIo(times: Int = 4, block: () -> T): T {
        var last: Throwable? = null
        repeat(times) { attempt ->
            try {
                return block()
            } catch (e: java.io.IOException) {
                last = e
                Thread.sleep(150L * (attempt + 1))
            }
        }
        throw last ?: java.io.IOException("IO failed")
    }

    private fun extractAsset(file: File) {
        // AGP decompresses *.gz assets at packaging time (the APK would silently
        // carry a plain craft.db instead), so the shipped asset uses the neutral
        // .dbz extension; keep fallbacks for the names older packaging produced.
        for (name in listOf("craft.dbz", "craft.db", "craft.db.gz")) {
            try {
                retryIo {
                    context.assets.open(name).use { raw ->
                        file.outputStream().use { out ->
                            if (name != "craft.db") {
                                GZIPInputStream(raw, 1 shl 16).copyTo(out)
                            } else {
                                raw.copyTo(out)
                            }
                        }
                    }
                }
                return
            } catch (_: java.io.FileNotFoundException) {
                // Try the next candidate name.
            }
        }
        throw java.io.FileNotFoundException("craft.dbz/craft.db/craft.db.gz 均未打包进 APK")
    }

    /** jobcat 查询缓存：类别 id -> (标签, 角色)。 */
    @Volatile
    var jobcats: Map<Int, JobCat> = emptyMap()
        private set

    private fun open(file: File) {
        val database = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
        // Fail fast on a corrupt file: touch every table the app queries.
        database.rawQuery("SELECT v FROM meta WHERE k='built_at'", null).use { cur ->
            check(cur.moveToFirst()) { "meta 表为空" }
            version = cur.getString(0)
        }
        // Old-schema local copies are rejected here, which triggers the repair
        // path (delete + re-extract from the bundled asset).
        database.rawQuery("SELECT v FROM meta WHERE k='schema_version'", null).use { cur ->
            check(cur.moveToFirst() && cur.getString(0) == EXPECTED_SCHEMA) { "配方库版本过旧" }
        }
        jobcats = database.rawQuery("SELECT id, label, role FROM jobcat", null).use { cur ->
            buildMap { while (cur.moveToNext()) put(cur.getInt(0), JobCat(cur.getString(1), cur.getString(2))) }
        }
        craftableIds = database.rawQuery("SELECT DISTINCT item_id FROM recipes", null).use { cur ->
            buildSet { while (cur.moveToNext()) add(cur.getInt(0)) }
        }
        database.rawQuery("SELECT COUNT(*) FROM items", null).use { cur ->
            check(cur.moveToFirst() && cur.getInt(0) > 0) { "items 表为空" }
        }
        db?.close()
        db = database
    }

    private fun download(url: String, target: File) {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.setRequestProperty("User-Agent", "craftlist-repair")
        try {
            if (conn.responseCode != 200) {
                throw java.io.IOException("HTTP ${conn.responseCode}")
            }
            conn.inputStream.use { input ->
                retryIo { target.outputStream().use { input.copyTo(it) } }
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseItems(reader: JsonReader, database: SQLiteDatabase, onProgress: (String) -> Unit): Int {
        val stmt = database.compileStatement(
            "INSERT OR REPLACE INTO items VALUES(?,?,?,?,?,?,?,?)",
        )
        reader.beginObject()
        var count = 0
        while (reader.hasNext()) {
            reader.nextName() // item id key
            reader.beginObject()
            var id = 0L
            var ja = ""
            var en = ""
            var cn = ""
            var icon = 0
            var ilv = 0
            var uc = 0
            var hq = false
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.nextLong()
                    "lang" -> {
                        val langs = mutableListOf<String>()
                        reader.beginArray()
                        while (reader.hasNext()) langs.add(reader.nextString())
                        reader.endArray()
                        ja = langs.getOrElse(0) { "" }
                        en = langs.getOrElse(1) { "" }
                        cn = langs.getOrElse(2) { "" }
                    }
                    "icon" -> icon = reader.nextInt()
                    "ilv" -> ilv = reader.nextInt()
                    "uc" -> uc = reader.nextInt()
                    "hq" -> hq = reader.nextBoolean()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            stmt.bindLong(1, id)
            stmt.bindString(2, cn)
            stmt.bindString(3, ja)
            stmt.bindString(4, en)
            stmt.bindLong(5, icon.toLong())
            stmt.bindLong(6, ilv.toLong())
            stmt.bindLong(7, if (hq) 1 else 0)
            stmt.bindLong(8, uc.toLong())
            stmt.execute()
            count++
            if (count % 10000 == 0) onProgress("解析道具 $count…")
        }
        reader.endObject()
        onProgress("道具 $count 条")
        return count
    }

    private fun parseRecipes(reader: JsonReader, database: SQLiteDatabase, onProgress: (String) -> Unit): Int {
        val recipeStmt = database.compileStatement(
            "INSERT OR REPLACE INTO recipes VALUES(?,?,?,?,?,?,?,?,?)",
        )
        val matStmt = database.compileStatement(
            "INSERT INTO recipe_mats VALUES(?,?,?,?)",
        )
        reader.beginObject()
        var count = 0
        while (reader.hasNext()) {
            reader.nextName() // recipe id key
            reader.beginObject()
            var id = 0L
            var job = 0
            var itemId = 0
            var yield = 1
            var craftLv = 0
            var stars = 0
            var rlv = 0
            var hq = false
            var qs = false
            var mats: List<Int> = emptyList()
            var crystals: List<Int> = emptyList()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "id" -> id = reader.nextLong()
                    "job" -> job = reader.nextInt()
                    "it" -> itemId = reader.nextInt()
                    "bp" -> {
                        val bp = readIntArray(reader)
                        yield = bp.getOrElse(1) { 1 }
                        craftLv = bp.getOrElse(2) { 0 }
                        stars = bp.getOrElse(3) { 0 }
                    }
                    "m" -> mats = readIntArray(reader)
                    "s" -> crystals = readIntArray(reader)
                    "rlv" -> rlv = reader.nextInt()
                    "hq" -> hq = reader.nextBoolean()
                    "qs" -> qs = reader.nextBoolean()
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            recipeStmt.bindLong(1, id)
            recipeStmt.bindLong(2, job.toLong())
            recipeStmt.bindLong(3, itemId.toLong())
            recipeStmt.bindLong(4, yield.toLong())
            recipeStmt.bindLong(5, craftLv.toLong())
            recipeStmt.bindLong(6, stars.toLong())
            recipeStmt.bindLong(7, rlv.toLong())
            recipeStmt.bindLong(8, if (hq) 1 else 0)
            recipeStmt.bindLong(9, if (qs) 1 else 0)
            recipeStmt.execute()
            for (i in mats.indices step 2) {
                if (mats[i] > 0 && mats.getOrElse(i + 1) { 0 } > 0) {
                    matStmt.bindLong(1, id)
                    matStmt.bindLong(2, mats[i].toLong())
                    matStmt.bindLong(3, mats[i + 1].toLong())
                    matStmt.bindLong(4, 0)
                    matStmt.execute()
                }
            }
            for (i in crystals.indices step 2) {
                if (crystals[i] > 0 && crystals.getOrElse(i + 1) { 0 } > 0) {
                    matStmt.bindLong(1, id)
                    matStmt.bindLong(2, crystals[i].toLong())
                    matStmt.bindLong(3, crystals[i + 1].toLong())
                    matStmt.bindLong(4, 1)
                    matStmt.execute()
                }
            }
            count++
            if (count % 5000 == 0) onProgress("解析配方 $count…")
        }
        reader.endObject()
        onProgress("配方 $count 条")
        return count
    }

    private fun readIntArray(reader: JsonReader): List<Int> {
        val out = mutableListOf<Int>()
        reader.beginArray()
        while (reader.hasNext()) out.add(reader.nextInt())
        reader.endArray()
        return out
    }

    companion object {
        const val DB_NAME = "craft.db"
        const val DEFAULT_URL = "https://5p.nbb.ffxiv.cn/statics/statics.json"
        const val EXPECTED_SCHEMA = "2"
        /** One statement per entry — SQLiteDatabase.execSQL compiles a single statement. */
        val SCHEMA_STATEMENTS = listOf(
            """
            CREATE TABLE items(
                id INTEGER PRIMARY KEY,
                name_cn TEXT NOT NULL DEFAULT '', name_jp TEXT NOT NULL DEFAULT '',
                name_en TEXT NOT NULL DEFAULT '',
                icon INTEGER NOT NULL DEFAULT 0, ilv INTEGER NOT NULL DEFAULT 0,
                hq INTEGER NOT NULL DEFAULT 0, uicat INTEGER NOT NULL DEFAULT 0)
            """.trimIndent(),
            """
            CREATE TABLE recipes(
                id INTEGER PRIMARY KEY,
                job INTEGER NOT NULL, item_id INTEGER NOT NULL,
                yield INTEGER NOT NULL DEFAULT 1, craft_lv INTEGER NOT NULL DEFAULT 0,
                stars INTEGER NOT NULL DEFAULT 0, rlv INTEGER NOT NULL DEFAULT 0,
                hq INTEGER NOT NULL DEFAULT 0, qs INTEGER NOT NULL DEFAULT 0)
            """.trimIndent(),
            "CREATE INDEX idx_recipes_item ON recipes(item_id)",
            """
            CREATE TABLE recipe_mats(
                recipe_id INTEGER NOT NULL, item_id INTEGER NOT NULL,
                qty INTEGER NOT NULL, is_crystal INTEGER NOT NULL DEFAULT 0)
            """.trimIndent(),
            "CREATE INDEX idx_mats_recipe ON recipe_mats(recipe_id)",
            "CREATE INDEX idx_mats_item ON recipe_mats(item_id)",
            "CREATE TABLE meta(k TEXT PRIMARY KEY, v TEXT NOT NULL)",
        )
    }
}
