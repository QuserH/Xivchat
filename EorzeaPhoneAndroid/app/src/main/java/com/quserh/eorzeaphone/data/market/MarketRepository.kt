package com.quserh.eorzeaphone.data.market

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.quserh.eorzeaphone.data.MarketMonitorRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Local state for the market feature: search history, watchlist, alert rules,
 * and an accumulating price history.
 *
 * Separate DB from `items.db` on purpose — that one ships read-only in assets and
 * gets replaced wholesale when the stamp changes, which would wipe user data.
 *
 * ## Why we store history at all
 *
 * Universalis only retains ~7 days of sales (measured across 6 items; neither
 * `entriesWithin` nor `statsWithin` extends it). The 1-month chart therefore has
 * to be built up locally: every time an item is opened we fold its sales into
 * `price_point`, so the month view fills in as the app gets used. Until then the
 * UI must say so rather than draw a stub line.
 */
object MarketRepository {
    private const val DB_NAME = "market.db"
    private const val SEARCH_HISTORY_MAX = 30

    /** Daily buckets kept per item. ~13 months, enough for the month view plus slack. */
    private const val POINT_RETENTION_DAYS = 400
    // A user can open thousands of different items over the life of an install.
    // The per-day retention above alone would still let the database grow without
    // bound (one row per item/day/quality/scope).  Keep a hard global ceiling too;
    // evict the oldest buckets first so recently visited items remain useful.
    private const val MAX_PRICE_POINT_ROWS = 50_000

    @Volatile private var db: SQLiteDatabase? = null

    /**
     * DDL as separate statements, NOT one blob split on ';'.
     *
     * Splitting was the original approach and it silently broke: a `--` comment
     * containing a semicolon ("One row per item; alert config lives here too")
     * got cut mid-sentence, leaving `alert config lives here too...` as its own
     * statement. That threw, `runCatching` swallowed it, and the `watch` table was
     * never created -- the whole alert feature was dead with no error anywhere.
     * Keep one statement per string and no semicolons inside them.
     */
    private val DDL = listOf(
        """
        CREATE TABLE IF NOT EXISTS search_history (
          query      TEXT PRIMARY KEY,
          at_ms      INTEGER NOT NULL
        )
        """,
        // Watchlist plus its alert config. A watched item with no alert just has
        // null thresholds and mode = 0.
        """
        CREATE TABLE IF NOT EXISTS watch (
          item_id      INTEGER PRIMARY KEY,
          added_ms     INTEGER NOT NULL,
          scope        TEXT NOT NULL,
          hq_only      INTEGER NOT NULL DEFAULT 0,
          -- Alert mode: 0 none, 1 absolute (below/between), 2 ratio-of-average
          mode         INTEGER NOT NULL DEFAULT 0,
          -- mode 1: alert when price is inside [min,max]. min null = "below max".
          min_price    INTEGER,
          max_price    INTEGER,
          -- mode 2: alert when price <= average * ratio (0.1, 0.2, ...)
          ratio        REAL,
          -- Set after firing so we don't renotify every poll for the same price.
          last_fire_ms INTEGER,
          last_price   INTEGER
        )
        """,
        // Daily price buckets. One row per item/day/scope/quality.
        """
        CREATE TABLE IF NOT EXISTS price_point (
          item_id  INTEGER NOT NULL,
          day      INTEGER NOT NULL,
          scope    TEXT NOT NULL,
          hq       INTEGER NOT NULL,
          min_p    INTEGER NOT NULL,
          avg_p    REAL NOT NULL,
          max_p    INTEGER NOT NULL,
          qty      INTEGER NOT NULL,
          sales    INTEGER NOT NULL,
          PRIMARY KEY (item_id, day, scope, hq)
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_pp_item ON price_point(item_id, scope, day)",
        "CREATE INDEX IF NOT EXISTS idx_pp_day ON price_point(day)",
        "CREATE TABLE IF NOT EXISTS pref (k TEXT PRIMARY KEY, v TEXT)",
    )

    /** Columns added when price monitoring arrived. */
    private val MONITOR_COLUMNS = listOf(
        Triple("auto_buy", "INTEGER NOT NULL DEFAULT 0", "watch"),
        Triple("monitor_threshold", "INTEGER NOT NULL DEFAULT 0", "watch"),
        Triple("buy_cap", "INTEGER NOT NULL DEFAULT 0", "watch"),
        Triple("bought_qty", "INTEGER NOT NULL DEFAULT 0", "watch"),
        // Favourite is one of the reasons a row exists, not the row itself: a watch
        // row may carry only an alert or only a monitor rule, so un-favouriting an
        // item must not silently delete the notification the user configured.
        Triple("favorite", "INTEGER NOT NULL DEFAULT 1", "watch"),
    )

    private suspend fun open(context: Context): SQLiteDatabase = db ?: withContext(Dispatchers.IO) {
        db ?: synchronized(this) {
            // No `execSQL("PRAGMA journal_mode=WAL")` here: PRAGMA returns a row and
            // execSQL throws on any statement that does, which aborted this whole
            // chain before applyDdl() ever ran -- leaving a market.db containing
            // only android_metadata and a silently dead feature.
            // enableWriteAheadLogging() is the supported way, and WAL is not
            // important enough for this DB to risk it, so plain journal is fine.
            db ?: SQLiteDatabase.openOrCreateDatabase(
                File(context.filesDir, DB_NAME), null,
            ).also { it.applyDdl(); it.applyMonitorColumns(); db = it }
        }
    }

    /**
     * Run the DDL on every open, not just on create: installs that ran the broken
     * split-on-';' version already have a market.db missing the `watch` table, and
     * they must heal without a reinstall. Every statement is IF NOT EXISTS.
     *
     * Deliberately NOT runCatching per statement -- swallowing errors here is what
     * hid the missing table in the first place.
     */
    private fun SQLiteDatabase.applyDdl() = DDL.forEach { execSQL(it.trimIndent()) }

    private fun SQLiteDatabase.applyMonitorColumns() {
        // SQLite has no `ADD COLUMN IF NOT EXISTS`.  Trying every ALTER on every cold
        // start and swallowing the failures still makes SQLite emit five error-level log
        // entries, and pays for five avoidable exceptions.  Read the schema once per table
        // and execute only migrations that are genuinely missing.
        MONITOR_COLUMNS.groupBy { it.third }.forEach { (table, columns) ->
            val existing = buildSet {
                rawQuery("PRAGMA table_info($table)", null).use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    while (cursor.moveToNext()) add(cursor.getString(nameIndex))
                }
            }
            columns.forEach { (column, type, _) ->
                if (column !in existing) execSQL("ALTER TABLE $table ADD COLUMN $column $type")
            }
        }
    }

    // ---- preferences ----

    /**
     * Selected scope (world or DC name).
     *
     * [fallback] should be the logged-in character's own DC. It only applies when
     * the user has never picked a scope -- a stored choice always wins, so changing
     * DC by hand is not undone on the next visit.
     *
     * The hardcoded 陆行鸟 is the last resort for when the game is offline and there
     * is no character to derive a DC from. It used to be the unconditional default,
     * which showed 陆行鸟 prices to players logged in elsewhere.
     */
    suspend fun scope(context: Context, fallback: String? = null): String =
        withContext(Dispatchers.IO) {
            pref(context, "scope")
                ?: fallback?.takeIf { it.isNotBlank() }
                ?: "陆行鸟"
        }

    suspend fun setScope(context: Context, scope: String) = withContext(Dispatchers.IO) {
        setPref(context, "scope", scope)
    }

    /**
     * Last seen character world, remembered so the 本服 tab still works offline.
     *
     * `state.profile` only exists while the plugin is connected, so without this the
     * per-world view would be empty exactly when the app is most useful -- away from
     * the PC. Written whenever a profile arrives; read when there is none.
     */
    suspend fun lastWorld(context: Context): String? = withContext(Dispatchers.IO) {
        pref(context, "lastWorld")?.takeIf { it.isNotBlank() }
    }

    suspend fun setLastWorld(context: Context, world: String) = withContext(Dispatchers.IO) {
        if (world.isNotBlank()) setPref(context, "lastWorld", world)
    }

    private fun prefRead(d: SQLiteDatabase, k: String): String? =
        d.rawQuery("SELECT v FROM pref WHERE k = ?", arrayOf(k))
            .use { if (it.moveToFirst()) it.getString(0) else null }

    suspend fun pref(context: Context, k: String): String? = withContext(Dispatchers.IO) {
        runCatching { prefRead(open(context), k) }.getOrNull()
    }

    suspend fun setPref(context: Context, k: String, v: String) = withContext(Dispatchers.IO) {
        runCatching {
            open(context).execSQL(
                "INSERT OR REPLACE INTO pref(k, v) VALUES(?, ?)", arrayOf(k, v),
            )
        }
        Unit
    }

    // ---- search history ----

    /**
     * Recent queries, newest first. Shown when the field is focused and empty —
     * that is the requested behaviour: history first, then it gets out of the way
     * as soon as the user types.
     */
    suspend fun searchHistory(context: Context, limit: Int = 12): List<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                open(context).rawQuery(
                    "SELECT query FROM search_history ORDER BY at_ms DESC LIMIT ?",
                    arrayOf(limit.toString()),
                ).use { c -> buildList { while (c.moveToNext()) add(c.getString(0)) } }
            }.getOrDefault(emptyList())
        }

    /** Record a query. Re-searching an old term moves it to the top, not duplicates it. */
    suspend fun addSearch(context: Context, query: String) = withContext(Dispatchers.IO) {
        val q = query.trim()
        if (q.isBlank()) return@withContext
        runCatching {
            val d = open(context)
            d.execSQL(
                "INSERT OR REPLACE INTO search_history(query, at_ms) VALUES(?, ?)",
                arrayOf(q, System.currentTimeMillis()),
            )
            d.execSQL(
                "DELETE FROM search_history WHERE query NOT IN " +
                    "(SELECT query FROM search_history ORDER BY at_ms DESC LIMIT ?)",
                arrayOf(SEARCH_HISTORY_MAX),
            )
        }
        Unit
    }

    suspend fun removeSearch(context: Context, query: String) = withContext(Dispatchers.IO) {
        runCatching {
            open(context).execSQL("DELETE FROM search_history WHERE query = ?", arrayOf(query))
        }
        Unit
    }

    suspend fun clearSearchHistory(context: Context) = withContext(Dispatchers.IO) {
        runCatching { open(context).execSQL("DELETE FROM search_history") }
        Unit
    }

    // ---- watchlist + alerts ----

    /** Alert mode. The user picks absolute OR ratio, not both. */
    enum class AlertMode { None, Absolute, Ratio }

    data class Watch(
        val itemId: Int,
        val addedMs: Long,
        val scope: String,
        val hqOnly: Boolean,
        val mode: AlertMode,
        /** Absolute mode: alert when price falls inside [minPrice, maxPrice]. */
        val minPrice: Int?,
        val maxPrice: Int?,
        /** Ratio mode: alert when price <= average * ratio. */
        val ratio: Double?,
        val lastFireMs: Long?,
        val lastPrice: Int?,
        /** Price-monitor rule pushed to the plugin. Independent of [mode]. */
        val monitorOn: Boolean = false,
        val monitorThreshold: Int = 0,
        val autoBuy: Boolean = false,
        val buyCap: Int = 0,
        val boughtQty: Int = 0,
        /** Shown in the watchlist because the user favourited it. */
        val favorite: Boolean = true,
    ) {
        val hasAlert: Boolean get() = mode != AlertMode.None
        /** A row survives un-favouriting while any of these are still configured. */
        val hasPurpose: Boolean get() = favorite || hasAlert || monitorOn
    }

    suspend fun watchList(context: Context): List<Watch> = withContext(Dispatchers.IO) {
        runCatching {
            open(context).rawQuery(
                "SELECT item_id, added_ms, scope, hq_only, mode, min_price, max_price, " +
                    "ratio, last_fire_ms, last_price, auto_buy, monitor_threshold, " +
                    "buy_cap, bought_qty, favorite FROM watch ORDER BY added_ms DESC", null,
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(
                            Watch(
                                itemId = c.getInt(0),
                                addedMs = c.getLong(1),
                                scope = c.getString(2) ?: "",
                                hqOnly = c.getInt(3) != 0,
                                mode = AlertMode.entries.getOrElse(c.getInt(4)) { AlertMode.None },
                                minPrice = if (c.isNull(5)) null else c.getInt(5),
                                maxPrice = if (c.isNull(6)) null else c.getInt(6),
                                ratio = if (c.isNull(7)) null else c.getDouble(7),
                                lastFireMs = if (c.isNull(8)) null else c.getLong(8),
                                lastPrice = if (c.isNull(9)) null else c.getInt(9),
                                monitorOn = c.getInt(10) != 0,
                                monitorThreshold = c.getInt(11),
                                autoBuy = c.getInt(12) != 0,
                                buyCap = c.getInt(13),
                                boughtQty = c.getInt(14),
                                favorite = c.getInt(15) != 0,
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    /**
     * Favourite state of an item: true when the heart is on. Distinct from row
     * existence -- a row may exist only for its alert or monitor rule.
     */
    suspend fun isWatched(context: Context, itemId: Int): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            open(context).rawQuery(
                "SELECT favorite FROM watch WHERE item_id = ?", arrayOf(itemId.toString()),
            ).use { it.moveToFirst() && it.getInt(0) != 0 }
        }.getOrDefault(false)
    }

    suspend fun watch(context: Context, itemId: Int): Watch? = withContext(Dispatchers.IO) {
        watchList(context).firstOrNull { it.itemId == itemId }
    }

    /** Add to watchlist with no alert. Keeps existing config if already watched. */
    suspend fun addWatch(context: Context, itemId: Int, scope: String) =
        withContext(Dispatchers.IO) {
            runCatching {
                val d = open(context)
                d.execSQL(
                    "INSERT OR IGNORE INTO watch(item_id, added_ms, scope) VALUES(?, ?, ?)",
                    arrayOf(itemId, System.currentTimeMillis(), scope),
                )
                d.execSQL(
                    "UPDATE watch SET favorite = 1 WHERE item_id = ?", arrayOf(itemId),
                )
            }
            Unit
        }

    /**
     * Un-favourite. The row survives when a notification or monitor rule is still
     * configured on it -- the heart and the bell are independent controls.
     */
    suspend fun removeWatch(context: Context, itemId: Int) = withContext(Dispatchers.IO) {
        runCatching {
            val d = open(context)
            d.execSQL(
                "UPDATE watch SET favorite = 0 WHERE item_id = ?", arrayOf(itemId),
            )
            d.execSQL(
                "DELETE FROM watch WHERE item_id = ? AND mode = 0 AND monitor_on = 0",
                arrayOf(itemId),
            )
        }
        Unit
    }

    /**
     * Set the alert rule. Absolute and ratio are mutually exclusive — writing one
     * clears the other so a stale threshold can't fire later.
     */
    suspend fun setAlert(
        context: Context,
        itemId: Int,
        scope: String,
        mode: AlertMode,
        minPrice: Int? = null,
        maxPrice: Int? = null,
        ratio: Double? = null,
        hqOnly: Boolean = false,
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val d = open(context)
            d.execSQL(
                "INSERT OR IGNORE INTO watch(item_id, added_ms, scope) VALUES(?, ?, ?)",
                arrayOf(itemId, System.currentTimeMillis(), scope),
            )
            d.execSQL(
                "UPDATE watch SET scope = ?, hq_only = ?, mode = ?, min_price = ?, " +
                    "max_price = ?, ratio = ?, last_fire_ms = NULL, last_price = NULL " +
                    "WHERE item_id = ?",
                arrayOf(
                    scope,
                    if (hqOnly) 1 else 0,
                    mode.ordinal,
                    if (mode == AlertMode.Absolute) minPrice else null,
                    if (mode == AlertMode.Absolute) maxPrice else null,
                    if (mode == AlertMode.Ratio) ratio else null,
                    itemId,
                ),
            )
        }
        Unit
    }

    /**
     * Set the price-monitor rule on a watch row. Monitoring runs inside the plugin
     * against the *current world's* board, so it is independent of the phone-side
     * alert scope; the threshold is compared against live listing unit prices.
     */
    suspend fun setMonitor(
        context: Context,
        itemId: Int,
        on: Boolean,
        threshold: Int = 0,
        hqOnly: Boolean = false,
        autoBuy: Boolean = false,
        buyCap: Int = 0,
    ) = withContext(Dispatchers.IO) {
        runCatching {
            val d = open(context)
            if (on) {
                d.execSQL(
                    "INSERT OR IGNORE INTO watch(item_id, added_ms, scope) VALUES(?, ?, ?)",
                    arrayOf(itemId, System.currentTimeMillis(), "本服"),
                )
                d.execSQL(
                    "UPDATE watch SET monitor_on = 1, monitor_threshold = ?, hq_only = ?, " +
                        "auto_buy = ?, buy_cap = ? WHERE item_id = ?",
                    arrayOf(threshold, if (hqOnly) 1 else 0, if (autoBuy) 1 else 0,
                        buyCap, itemId),
                )
            } else {
                d.execSQL(
                    "UPDATE watch SET monitor_on = 0, auto_buy = 0, monitor_threshold = 0, " +
                        "buy_cap = 0, bought_qty = 0 WHERE item_id = ?",
                    arrayOf(itemId),
                )
            }
        }
        Unit
    }

    /** Rules to push to the plugin: every watch row with monitoring enabled. */
    suspend fun monitorRules(context: Context): List<MarketMonitorRule> =
        watchList(context)
            .filter { it.monitorOn && it.monitorThreshold > 0 }
            .map {
                MarketMonitorRule(
                    itemId = it.itemId,
                    threshold = it.monitorThreshold,
                    hqOnly = it.hqOnly,
                    autoBuy = it.autoBuy,
                    buyCap = it.buyCap,
                )
            }

    // ---- price history accumulation ----

    data class Point(
        val day: Long,
        val minPrice: Int,
        val avgPrice: Double,
        val maxPrice: Int,
        val quantity: Int,
        val sales: Int,
    )

    /**
     * Fold fetched sales into daily buckets.
     *
     * ## Replace, don't accumulate, for fully-covered days
     *
     * The API always returns the whole ~7 day window, so consecutive fetches
     * overlap heavily. Blindly accumulating double-counts: measured 250+250
     * entries with 100 shared sales producing 500 instead of the true union 400.
     *
     * A day is *fully covered* only when it lies **strictly between** the payload's
     * oldest and newest sale — then the payload holds all of that day and may
     * overwrite. Both edge days are clipped and must merge instead.
     *
     * Getting that wrong is subtle: an earlier version only checked the oldest
     * edge, so a day clipped at the *newest* edge got overwritten and a lower
     * `min_p` recorded by a previous fetch was silently lost (caught by
     * `validate_market_logic.py` §4 asserting per-day min/max stay exact).
     *
     * On merge we take the better-informed side per field rather than summing, so
     * repeated fetches converge instead of inflating.
     */
    suspend fun recordSales(
        context: Context,
        itemId: Int,
        scope: String,
        sales: List<MarketApi.Sale>,
    ) = withContext(Dispatchers.IO) {
        if (sales.isEmpty()) return@withContext
        runCatching {
            val d = open(context)
            val oldestDay = sales.minOf { it.atSec } / 86400
            val newestDay = sales.maxOf { it.atSec } / 86400
            val byDay = sales.groupBy { (it.atSec / 86400) to it.hq }
            d.beginTransaction()
            try {
                for ((key, group) in byDay) {
                    val (day, hq) = key
                    val prices = group.map { it.pricePerUnit }
                    val newMin = prices.min()
                    val newMax = prices.max()
                    val newQty = group.sumOf { it.quantity.coerceAtLeast(1) }
                    val newSales = group.size
                    // The API reports one row per transaction, not one row per
                    // item.  Weight the per-unit average by stack quantity so a
                    // 99-item sale has the influence it actually represents.
                    val newAvg = group.sumOf {
                        it.pricePerUnit.toDouble() * it.quantity.coerceAtLeast(1).toDouble()
                    } / newQty.toDouble()

                    val fullyCovered = day > oldestDay && day < newestDay
                    val cur = if (fullyCovered) null else d.rawQuery(
                        "SELECT min_p, avg_p, max_p, qty, sales FROM price_point " +
                            "WHERE item_id=? AND day=? AND scope=? AND hq=?",
                        arrayOf(itemId.toString(), day.toString(), scope,
                            (if (hq) 1 else 0).toString()),
                    ).use { c ->
                        if (c.moveToFirst()) {
                            Point(day, c.getInt(0), c.getDouble(1), c.getInt(2),
                                c.getInt(3), c.getInt(4))
                        } else null
                    }

                    // Partial day: take the better-informed side per field instead
                    // of summing, so repeated fetches converge instead of inflating.
                    val (mMin, mAvg, mMax, mQty, mSales) = if (cur == null) {
                        Quint(newMin, newAvg, newMax, newQty, newSales)
                    } else if (newSales >= cur.sales) {
                        Quint(minOf(cur.minPrice, newMin), newAvg,
                            maxOf(cur.maxPrice, newMax),
                            maxOf(cur.quantity, newQty), newSales)
                    } else {
                        Quint(minOf(cur.minPrice, newMin), cur.avgPrice,
                            maxOf(cur.maxPrice, newMax),
                            maxOf(cur.quantity, newQty), cur.sales)
                    }

                    d.execSQL(
                        "INSERT OR REPLACE INTO price_point" +
                            "(item_id, day, scope, hq, min_p, avg_p, max_p, qty, sales) " +
                            "VALUES(?,?,?,?,?,?,?,?,?)",
                        arrayOf(itemId, day, scope, if (hq) 1 else 0,
                            mMin, mAvg, mMax, mQty, mSales),
                    )
                }
                val cutoff = System.currentTimeMillis() / 86400_000 - POINT_RETENTION_DAYS
                d.execSQL("DELETE FROM price_point WHERE day < ?", arrayOf(cutoff))
                trimPricePointRows(d)
                d.setTransactionSuccessful()
            } finally {
                d.endTransaction()
            }
        }
        Unit
    }

    /**
     * Keep the history database bounded even when many different items/scopes
     * have been viewed.  This runs inside the caller's transaction.
     */
    private fun trimPricePointRows(d: SQLiteDatabase): Int {
        val count = d.rawQuery("SELECT COUNT(*) FROM price_point", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        val excess = count - MAX_PRICE_POINT_ROWS
        if (excess <= 0) return 0
        d.execSQL(
            "DELETE FROM price_point WHERE rowid IN (" +
                "SELECT rowid FROM price_point ORDER BY day ASC, rowid ASC LIMIT ?)",
            arrayOf(excess),
        )
        return excess
    }

    /** Best-effort startup repair for databases created before the row ceiling. */
    suspend fun trimHistory(context: Context): Int = withContext(Dispatchers.IO) {
        runCatching {
            val d = open(context)
            d.beginTransaction()
            try {
                val cutoff = System.currentTimeMillis() / 86400_000 - POINT_RETENTION_DAYS
                d.execSQL("DELETE FROM price_point WHERE day < ?", arrayOf(cutoff))
                val removed = trimPricePointRows(d)
                d.setTransactionSuccessful()
                removed
            } finally {
                d.endTransaction()
            }
        }.getOrDefault(0)
    }

    /** Local 5-tuple so the merge above reads as one assignment. */
    private data class Quint(
        val a: Int, val b: Double, val c: Int, val d: Int, val e: Int,
    )

    /** Stored daily points for a chart window, oldest first. */
    suspend fun points(
        context: Context,
        itemId: Int,
        scope: String,
        days: Int,
        hq: Boolean? = null,
    ): List<Point> = withContext(Dispatchers.IO) {
        val from = System.currentTimeMillis() / 86400_000 - days
        runCatching {
            val hqClause = when (hq) {
                null -> ""
                else -> " AND hq = ${if (hq) 1 else 0}"
            }
            // Collapse NQ+HQ when no quality filter: one visual series per day.
            open(context).rawQuery(
                "SELECT day, MIN(min_p), " +
                    // avg_p is already a per-item average for the bucket.  Use the
                    // stored quantity as the merge weight, not the transaction count:
                    // one 99-item stack must influence the result 99x as much as a
                    // single-item sale.  Older rows can have qty = 0, so fall back to
                    // sales for those rows while keeping the denominator non-zero.
                    "SUM(avg_p * CASE WHEN qty > 0 THEN qty ELSE sales END) / " +
                    "NULLIF(SUM(CASE WHEN qty > 0 THEN qty ELSE sales END), 0), " +
                    "MAX(max_p), SUM(qty), SUM(sales) FROM price_point " +
                    "WHERE item_id = ? AND scope = ? AND day >= ?$hqClause " +
                    "GROUP BY day ORDER BY day",
                arrayOf(itemId.toString(), scope, from.toString()),
            ).use { c ->
                buildList {
                    while (c.moveToNext()) {
                        add(
                            Point(
                                day = c.getLong(0),
                                minPrice = c.getInt(1),
                                avgPrice = if (c.isNull(2)) 0.0 else c.getDouble(2),
                                maxPrice = c.getInt(3),
                                quantity = c.getInt(4),
                                sales = c.getInt(5),
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    /** How many distinct days we hold. Used to tell the user the month view is still filling. */
    suspend fun coverageDays(context: Context, itemId: Int, scope: String): Int =
        withContext(Dispatchers.IO) {
            runCatching {
                open(context).rawQuery(
                    "SELECT COUNT(DISTINCT day) FROM price_point WHERE item_id=? AND scope=?",
                    arrayOf(itemId.toString(), scope),
                ).use { if (it.moveToFirst()) it.getInt(0) else 0 }
            }.getOrDefault(0)
        }

    // ---- alert evaluation ----

    /** A rule that matched, ready to notify. */
    data class AlertHit(
        val itemId: Int,
        val price: Int,
        val worldName: String,
        val scope: String,
        val hq: Boolean,
        val mode: AlertMode,
        /** For ratio mode: the average it was compared against. */
        val reference: Double?,
    )

    /**
     * Check every alert rule once. Network-bound; call from a background poll.
     *
     * Dedupe rule: a hit only fires if the price is **lower** than what we last
     * notified for, or 12h have passed. Without that, a standing cheap listing
     * would notify on every poll.
     */
    suspend fun checkAlerts(context: Context): List<AlertHit> = withContext(Dispatchers.IO) {
        val watches = watchList(context).filter { it.hasAlert }
        if (watches.isEmpty()) return@withContext emptyList()
        val marketable = MarketApi.marketableIds(context)
        val out = mutableListOf<AlertHit>()

        // Group by scope so one aggregated call covers many items.
        for ((scope, group) in watches.groupBy { it.scope }) {
            val ids = group.map { it.itemId }.filter { it in marketable }
            if (ids.isEmpty()) continue
            val aggs = MarketApi.aggregated(scope, ids).associateBy { it.itemId }
            for (w in group) {
                val a = aggs[w.itemId] ?: continue
                // Prefer the scope the user picked; dc covers the common case.
                val block = if (w.hqOnly) (a.hqDc ?: a.hqWorld ?: a.hqRegion)
                else (a.nqDc ?: a.nqWorld ?: a.nqRegion)
                val price = block?.minPrice ?: continue
                val avg = block.avgPrice

                val matched = when (w.mode) {
                    AlertMode.Absolute -> {
                        val lo = w.minPrice
                        val hi = w.maxPrice
                        when {
                            lo != null && hi != null -> price in lo..hi
                            hi != null -> price <= hi
                            lo != null -> price >= lo
                            else -> false
                        }
                    }
                    AlertMode.Ratio -> {
                        val r = w.ratio
                        r != null && avg != null && avg > 0 && price <= avg * r
                    }
                    AlertMode.None -> false
                }
                if (!matched) continue

                val now = System.currentTimeMillis()
                val staleEnough = (w.lastFireMs ?: 0) < now - 12 * 3600_000
                val cheaper = w.lastPrice == null || price < w.lastPrice
                if (!staleEnough && !cheaper) continue

                out += AlertHit(
                    itemId = w.itemId,
                    price = price,
                    worldName = "",   // filled by caller from world tables if needed
                    scope = scope,
                    hq = w.hqOnly,
                    mode = w.mode,
                    reference = avg,
                )
                runCatching {
                    open(context).execSQL(
                        "UPDATE watch SET last_fire_ms = ?, last_price = ? WHERE item_id = ?",
                        arrayOf(now, price, w.itemId),
                    )
                }
            }
        }
        out
    }
}
