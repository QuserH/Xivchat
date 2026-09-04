package com.quserh.eorzeaphone.data.market

import android.content.Context
import com.quserh.eorzeaphone.data.CacheMaintenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream

/**
 * Universalis market board client. Same source as the ffxiv-priceinsight plugin.
 *
 * Verified against the live API (see `开发/WIKI/wiki-feature/_probe_universalis*.py`):
 * - CN is fully covered: 4 DCs (陆行鸟/莫古力/猫小胖/豆豆柴) under region 中国, 28 worlds
 * - `/marketable` returns 16843 ids, and **all 16843 exist in our local items.db**,
 *   so local item search can drive price lookups with no id mapping.
 *
 * Gotcha that shaped this file: CJK DC/world names must be percent-encoded in
 * the path. Raw bytes throw.
 *
 * (An earlier version of this header claimed history retains only ~7 days.
 * That was a measurement error -- see [HISTORY_DAYS_MAX].)
 */
object MarketApi {
    private const val BASE = "https://universalis.app/api/v2"
    private const val UA = "EorzeaPhone/0.7.289 (FFXIV companion; market board lookup)"
    private const val TIMEOUT_MS = 15_000
    private const val CACHE_DIR = "market-cache"

    // ---- request gate + short-lived response cache ----

    /**
     * Caps concurrency and spaces requests out.
     *
     * Needed because the 全部 tab fans out 4 DC calls at once and the watchlist
     * checker walks every saved item; without a gate those arrive as a burst and
     * Universalis starts 429ing. 4-in-flight / 100ms apart mirrors what the
     * Aetherphone plugin settled on.
     */
    private val gate = Semaphore(4)
    private val spacingLock = Mutex()
    private var lastRequestAt = 0L
    private const val MIN_SPACING_MS = 100L

    private suspend fun <T> gated(block: suspend () -> T): T = gate.withPermit {
        spacingLock.withLock {
            val wait = MIN_SPACING_MS - (System.currentTimeMillis() - lastRequestAt)
            if (wait > 0) delay(wait)
            lastRequestAt = System.currentTimeMillis()
        }
        block()
    }

    private class Cached(val body: String, val atMs: Long)

    private val memCache = ConcurrentHashMap<String, Cached>()

    /**
     * Freshness windows. Listings move constantly, aggregated summaries are
     * derived and change slower, tax rates only shift on the weekly reset.
     *
     * The point is not saving bandwidth -- it is that flipping between the scope
     * tabs used to refetch everything on every tap.
     */
    private const val TTL_LISTINGS_MS = 45_000L
    private const val TTL_AGGREGATED_MS = 120_000L
    private const val TTL_TAX_MS = 3_600_000L

    /** GET with a TTL cache. [ttlMs] = 0 bypasses the cache entirely. */
    private suspend fun cachedRemote(path: String, ttlMs: Long): String? {
        if (ttlMs > 0) {
            memCache[path]?.let { hit ->
                if (System.currentTimeMillis() - hit.atMs < ttlMs) return hit.body
            }
        }
        val body = gated { get(path) } ?: return null
        if (ttlMs > 0) {
            memCache[path] = Cached(body, System.currentTimeMillis())
            // Keep the map from growing without bound over a long session.
            if (memCache.size > 240) {
                val cutoff = System.currentTimeMillis() - maxOf(ttlMs, TTL_AGGREGATED_MS)
                memCache.entries.removeAll { it.value.atMs < cutoff }
            }
        }
        return body
    }

    /** Drops cached bodies so an explicit pull-to-refresh really refetches. */
    fun invalidateCache() = memCache.clear()

    /**
     * The API *can* serve at least 90 days -- `entriesWithin` works.
     *
     * An earlier version of this file claimed 7 days. That was a measurement
     * mistake: the probe passed `entriesToReturn=1800` alongside `entriesWithin`,
     * and on a busy item 1800 sales only reach back **1.8 days**, so the row cap
     * hit first and looked like a time wall. With `entriesToReturn=99999` the same
     * item returns 29568 sales spanning a full 30 days.
     *
     * Cost is the real constraint, not availability:
     *   7d  ->    1800 sales,  19 KB gzip, 1.1s
     *   30d ->   29568 sales, 305 KB gzip, 3.7s
     * So 30d is fetched only when the user asks for it, then cached locally.
     */
    const val HISTORY_DAYS_MAX = 90

    // ---- worlds / data centers ----

    data class World(val id: Int, val name: String, val dc: String)
    data class DataCenter(val name: String, val region: String, val worldIds: List<Int>)

    /**
     * World + DC tables. Cached 30 days — new worlds appear maybe once a year.
     * [cnOnly] keeps just region 中国, which is what this app needs.
     */
    suspend fun worlds(context: Context, cnOnly: Boolean = true): Pair<List<DataCenter>, List<World>> =
        withContext(Dispatchers.IO) {
            val dcRaw = cachedGet(context, "data-centers", "/data-centers", 30L * 86400_000)
                ?: return@withContext emptyList<DataCenter>() to emptyList()
            val wRaw = cachedGet(context, "worlds", "/worlds", 30L * 86400_000)
                ?: return@withContext emptyList<DataCenter>() to emptyList()

            val dcs = buildList {
                val arr = runCatching { JSONArray(dcRaw) }.getOrNull() ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val region = o.optString("region")
                    if (cnOnly && region != "中国") continue
                    val ids = buildList {
                        val wa = o.optJSONArray("worlds") ?: JSONArray()
                        for (j in 0 until wa.length()) add(wa.optInt(j))
                    }
                    add(DataCenter(o.optString("name"), region, ids))
                }
            }
            val idToDc = HashMap<Int, String>()
            dcs.forEach { dc -> dc.worldIds.forEach { idToDc[it] = dc.name } }

            val ws = buildList {
                val arr = runCatching { JSONArray(wRaw) }.getOrNull() ?: JSONArray()
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optInt("id")
                    val dc = idToDc[id] ?: if (cnOnly) continue else ""
                    add(World(id, o.optString("name"), dc))
                }
            }
            dcs to ws
        }

    // ---- aggregated (cheap summary, what priceinsight uses) ----

    /** One scope's numbers. Any field may be null when nobody has listed/sold there. */
    data class Agg(
        val minPrice: Int?,
        val minWorldId: Int?,
        val recentPrice: Int?,
        val recentAtMs: Long?,
        val avgPrice: Double?,
        val dailyQty: Double?,
    ) {
        val isEmpty: Boolean get() = minPrice == null && recentPrice == null && avgPrice == null
    }

    /** Aggregated result for one item: NQ and HQ, each at world/dc/region scope. */
    data class ItemAgg(
        val itemId: Int,
        val nqWorld: Agg?, val nqDc: Agg?, val nqRegion: Agg?,
        val hqWorld: Agg?, val hqDc: Agg?, val hqRegion: Agg?,
        /** worldId -> last upload ms. Drives the "how stale is this" label. */
        val uploadTimes: Map<Int, Long>,
    )

    /**
     * Batch aggregated lookup. [scope] is a world name, DC name, or region.
     *
     * Verified 13 ids in one call; keep batches modest anyway. Unmarketable ids
     * make the whole call return **HTTP 400**, not a partial result — so callers
     * must filter to marketable ids first (see [marketableIds]).
     */
    suspend fun aggregated(scope: String, itemIds: List<Int>): List<ItemAgg> =
        withContext(Dispatchers.IO) {
            if (itemIds.isEmpty()) return@withContext emptyList()
            val body = cachedRemote(
                "/aggregated/${enc(scope)}/${itemIds.joinToString(",")}",
                TTL_AGGREGATED_MS,
            ) ?: return@withContext emptyList()
            val results = runCatching {
                JSONObject(body).optJSONArray("results")
            }.getOrNull() ?: return@withContext emptyList()

            buildList {
                for (i in 0 until results.length()) {
                    val r = results.optJSONObject(i) ?: continue
                    val ut = HashMap<Int, Long>()
                    r.optJSONArray("worldUploadTimes")?.let { a ->
                        for (j in 0 until a.length()) {
                            val o = a.optJSONObject(j) ?: continue
                            ut[o.optInt("worldId")] = o.optLong("timestamp")
                        }
                    }
                    val nq = r.optJSONObject("nq")
                    val hq = r.optJSONObject("hq")
                    add(
                        ItemAgg(
                            itemId = r.optInt("itemId"),
                            nqWorld = agg(nq, "world"), nqDc = agg(nq, "dc"),
                            nqRegion = agg(nq, "region"),
                            hqWorld = agg(hq, "world"), hqDc = agg(hq, "dc"),
                            hqRegion = agg(hq, "region"),
                            uploadTimes = ut,
                        )
                    )
                }
            }
        }

    /** Pull one scope out of an nq/hq block. Empty objects are normal, not errors. */
    private fun agg(block: JSONObject?, scope: String): Agg? {
        if (block == null) return null
        fun sub(key: String) = block.optJSONObject(key)?.optJSONObject(scope)
        val ml = sub("minListing")
        val rp = sub("recentPurchase")
        val av = sub("averageSalePrice")
        val dv = sub("dailySaleVelocity")
        val a = Agg(
            minPrice = ml?.optInt("price")?.takeIf { it > 0 },
            minWorldId = ml?.optInt("worldId")?.takeIf { it > 0 },
            recentPrice = rp?.optInt("price")?.takeIf { it > 0 },
            recentAtMs = rp?.optLong("timestamp")?.takeIf { it > 0 },
            avgPrice = av?.optDouble("price")?.takeIf { it > 0 },
            dailyQty = dv?.optDouble("quantity")?.takeIf { it > 0 },
        )
        return if (a.isEmpty) null else a
    }

    // ---- per-world listings (the "all servers" view) ----

    data class Listing(
        val worldId: Int,
        val worldName: String,
        val pricePerUnit: Int,
        val quantity: Int,
        val hq: Boolean,
        val retainerName: String,
        val lastReviewSec: Long,
        /**
         * Server-side listing id. Stable and unique -- probed 3036 CN listings
         * across 10 items x 4 DCs: zero empty, zero duplicates. Used as the
         * Compose list key, which replaced a hand-built composite key that had
         * to survive 815 collisions in the same sample.
         */
        val listingId: String,
        /**
         * Sales tax on this listing, in gil, as charged by the retainer's city.
         *
         * [total] does NOT include it -- probe: 27 x 300 = total 8100, tax 405,
         * so the buyer actually pays 8505. Present on every row sampled.
         */
        val tax: Int,
    ) {
        val total: Int get() = pricePerUnit * quantity

        /** What the buyer is actually charged. */
        val totalWithTax: Int get() = total + tax
    }

    data class Listings(
        val itemId: Int,
        val listings: List<Listing>,
        /** worldId -> last upload ms. A world absent here has never reported. */
        val uploadTimes: Map<Int, Long>,
        val currentAvg: Double?,
    ) {
        /**
         * Listing price spread, split by quality.
         *
         * Reports the **median**, not the mean. Players park unwanted items at
         * absurd prices to keep them off the board -- measured listings at
         * 999,999,999 and 30,303,030 gil. Across 6 sampled items the mean was
         * skewed >2x on 5 of them, by up to 294,000x. The mean is unusable here;
         * the median is barely affected.
         *
         * For a trustworthy "average", use the aggregated *sale* price instead of
         * anything derived from listings.
         */
        fun stats(hq: Boolean?): Stats? {
            val rows = when (hq) {
                null -> listings
                else -> listings.filter { it.hq == hq }
            }
            if (rows.isEmpty()) return null
            val p = rows.map { it.pricePerUnit }.sorted()
            val mid = if (p.size % 2 == 1) p[p.size / 2].toDouble()
            else (p[p.size / 2 - 1] + p[p.size / 2]) / 2.0
            return Stats(p.first(), mid, p.last(), rows.size)
        }
    }

    /** [median] is the middle listing price; see [Listings.stats] for why not mean. */
    data class Stats(val min: Int, val median: Double, val max: Int, val count: Int) {
        /**
         * True when the top listing is so far above the middle that showing it as
         * "highest price" misleads -- it is a park, not an offer.
         */
        val maxIsOutlier: Boolean get() = median > 0 && max > median * 20
    }

    /** Pseudo-scope for "every CN data centre at once". Not a Universalis scope. */
    const val SCOPE_ALL = "全部"

    /**
     * All CN data centres merged, for the 全部 tab.
     *
     * Deliberately fans out one call per DC instead of hitting the region scope:
     * `/中国/<id>?listings=100` caps at 100 listings *globally*, which came back
     * covering only 13 of 28 worlds. Four parallel DC calls returned 303 listings
     * across 27 worlds for the same item -- the region endpoint silently truncates.
     *
     * Runs concurrently, so wall time is one call not four (~2s vs ~7.4s measured).
     */
    suspend fun listingsAllCn(
        context: Context,
        itemId: Int,
        limitPerDc: Int = 100,
    ): Listings? = withContext(Dispatchers.IO) {
        val (dcs, _) = worlds(context)
        if (dcs.isEmpty()) return@withContext null
        val parts = coroutineScope {
            dcs.map { dc -> async { listings(dc.name, itemId, limitPerDc) } }.awaitAll()
        }.filterNotNull()
        if (parts.isEmpty()) return@withContext null
        // Dedupe at the merge, not at each call site: four DC responses are the one
        // place the same listing could arrive twice, and a repeated listingId would
        // crash the LazyColumn that keys on it.
        val merged = run {
            val seen = HashSet<String>()
            parts.flatMap { it.listings }.filter { seen.add(it.listingId) }
        }
        Listings(
            itemId = itemId,
            listings = merged,
            uploadTimes = buildMap { parts.forEach { putAll(it.uploadTimes) } },
            // Averaging DC averages would be wrong (unequal listing counts), so
            // recompute from the merged rows instead.
            currentAvg = merged.takeIf { it.isNotEmpty() }
                ?.let { l -> l.sumOf { it.pricePerUnit.toDouble() } / l.size },
        )
    }

    /** Aggregated summary for the whole CN region. One call; region scope is fine here. */
    suspend fun aggregatedAllCn(itemId: Int): ItemAgg? =
        aggregated("中国", listOf(itemId)).firstOrNull()

    /**
     * Live listings for one item across a whole DC (or one world).
     *
     * Measured: 100 listings for 土之碎晶 spread over 7 of 陆行鸟's 8 worlds, and
     * per-world upload staleness ranged 2h..60h. That spread is why the UI shows
     * an age per world instead of implying every number is current.
     */
    suspend fun listings(scope: String, itemId: Int, limit: Int = 100): Listings? =
        withContext(Dispatchers.IO) {
            val body = cachedRemote(
                "/${enc(scope)}/$itemId?listings=$limit&entries=0",
                TTL_LISTINGS_MS,
            ) ?: return@withContext null
            val o = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext null
            val ls = buildList {
                val arr = o.optJSONArray("listings") ?: JSONArray()
                // Guard against a repeated listingId. The CN probe found none, but a
                // duplicate would crash the LazyColumn ("key was already used") since
                // this feeds the list key, so drop rather than trust.
                val seen = HashSet<String>()
                for (i in 0 until arr.length()) {
                    val l = arr.optJSONObject(i) ?: continue
                    val lid = l.optString("listingID")
                    if (lid.isNotEmpty() && !seen.add(lid)) continue
                    val unit = l.optInt("pricePerUnit")
                    val qty = l.optInt("quantity").coerceAtLeast(1)
                    add(
                        Listing(
                            worldId = l.optInt("worldID"),
                            worldName = l.optString("worldName"),
                            pricePerUnit = unit,
                            quantity = qty,
                            hq = l.optBoolean("hq"),
                            retainerName = l.optString("retainerName"),
                            lastReviewSec = l.optLong("lastReviewTime"),
                            // Fall back to a positional id so a missing one cannot
                            // collapse two rows into one key.
                            listingId = lid.ifEmpty { "idx$i-${l.optInt("worldID")}-$unit-$qty" },
                            tax = l.optInt("tax"),
                        )
                    )
                }
            }
            Listings(
                itemId = o.optInt("itemID", itemId),
                listings = ls,
                uploadTimes = buildMap {
                    o.optJSONObject("worldUploadTimes")?.let { w ->
                        w.keys().forEach { k -> k.toIntOrNull()?.let { put(it, w.optLong(k)) } }
                    }
                },
                currentAvg = o.optDouble("currentAveragePrice").takeIf { it > 0 },
            )
        }

    // ---- history (charts) ----

    data class Sale(
        val atSec: Long,
        val pricePerUnit: Int,
        val quantity: Int,
        val hq: Boolean,
        val worldName: String,
    )

    /**
     * Completed sales. **Timestamps are seconds here** while aggregated uses
     * milliseconds — mixing them up silently puts every point in 1970.
     *
     * [days] sets the window via `entriesWithin`. [entries] must be large enough
     * or it truncates the *window* instead of the detail: on a busy item 1800 rows
     * only span 1.8 days, which is what made an earlier probe conclude the API
     * kept just 7 days. See [HISTORY_DAYS_MAX].
     *
     * Density varies wildly with how the item trades: a gear item gave 6812 sales
     * in 7 days, 土之碎晶 only 47. The chart buckets by day to cope with both.
     */
    suspend fun history(
        scope: String,
        itemId: Int,
        days: Int = 7,
        entries: Int = 99_999,
    ): List<Sale> =
        withContext(Dispatchers.IO) {
            val within = days.coerceAtLeast(1) * 86_400
            val body = cachedRemote(
                "/history/${enc(scope)}/$itemId" +
                    "?entriesWithin=$within&entriesToReturn=$entries",
                TTL_LISTINGS_MS,
            ) ?: return@withContext emptyList()
            val arr = runCatching {
                JSONObject(body).optJSONArray("entries")
            }.getOrNull() ?: return@withContext emptyList()
            buildList {
                for (i in 0 until arr.length()) {
                    val e = arr.optJSONObject(i) ?: continue
                    add(
                        Sale(
                            atSec = e.optLong("timestamp"),
                            pricePerUnit = e.optInt("pricePerUnit"),
                            quantity = e.optInt("quantity").coerceAtLeast(1),
                            hq = e.optBoolean("hq"),
                            worldName = e.optString("worldName"),
                        )
                    )
                }
            }
        }

    // ---- tax rates (seller side) ----

    /** Retainer city name (CN) and its sales tax percentage on one world. */
    data class CityTax(val city: String, val rate: Int)

    /** Universalis returns English city names; the app is CN-only. */
    private val CITY_CN = mapOf(
        "Limsa Lominsa" to "利姆萨·罗敏萨",
        "Gridania" to "格里达尼亚",
        "Ul'dah" to "乌尔达哈",
        "Ishgard" to "伊修加德",
        "Kugane" to "黄金港",
        "Crystarium" to "水晶都",
        "Old Sharlayan" to "旧萨雷安",
        "Tuliyollal" to "图莱尤拉",
    )

    /**
     * Sales tax per retainer city on one **world**, cheapest first.
     *
     * World-scoped on purpose: rates are not uniform inside a DC. Probed all 28
     * CN worlds -- 3 of the 4 DCs disagree internally, 莫古力 alone had 7 distinct
     * rate tables. So this cannot be shown for a DC-wide scope.
     *
     * Rates observed are only ever 3% or 5%, and they move on the weekly reset,
     * hence the 1h cache.
     *
     * This is seller-facing (where to park a retainer). Buyers do not need it --
     * each listing already carries its own [Listing.tax].
     */
    suspend fun taxRates(world: String): List<CityTax> = withContext(Dispatchers.IO) {
        if (world.isBlank()) return@withContext emptyList()
        val body = cachedRemote("/tax-rates?world=${enc(world)}", TTL_TAX_MS)
            ?: return@withContext emptyList()
        val o = runCatching { JSONObject(body) }.getOrNull() ?: return@withContext emptyList()
        buildList {
            o.keys().forEach { k ->
                val rate = o.optInt(k)
                if (rate > 0) add(CityTax(CITY_CN[k] ?: k, rate))
            }
        }.sortedBy { it.rate }
    }

    // ---- cross-scope comparison ----

    /**
     * A cheaper offer outside the scope the user is looking at.
     *
     * [savedPct] is what makes this actionable: travelling to another DC costs
     * real time, so a 2% saving is not worth surfacing (the UI applies a floor).
     */
    data class CheaperElsewhere(
        val price: Int,
        val worldId: Int,
        val worldName: String,
        val crossDc: Boolean,
        val savedPct: Double,
    )

    /**
     * Compares the price on the current scope against the DC and region minimums
     * from the same aggregated payload.
     *
     * Returns null when the current scope already is the cheapest, which is the
     * common case -- callers should treat null as "nothing to say" rather than an
     * error. HQ and NQ are compared separately since they are different goods.
     */
    fun cheaperElsewhere(
        agg: ItemAgg?,
        currentPrice: Int?,
        hqOnly: Boolean,
        worldNames: Map<Int, String>,
        dcOfWorld: Map<Int, String> = emptyMap(),
        currentDc: String? = null,
    ): CheaperElsewhere? {
        if (agg == null || currentPrice == null || currentPrice <= 0) return null
        val dc = if (hqOnly) agg.hqDc else agg.nqDc
        val region = if (hqOnly) agg.hqRegion else agg.nqRegion

        var best: Pair<Int, Int>? = null // price to worldId
        listOfNotNull(dc, region).forEach { a ->
            val p = a.minPrice ?: return@forEach
            val w = a.minWorldId ?: return@forEach
            if (p < currentPrice && (best == null || p < best!!.first)) best = p to w
        }
        val (price, worldId) = best ?: return null
        return CheaperElsewhere(
            price = price,
            worldId = worldId,
            worldName = worldNames[worldId] ?: "其他服务器",
            crossDc = currentDc != null && dcOfWorld[worldId] != null &&
                dcOfWorld[worldId] != currentDc,
            savedPct = (currentPrice - price) * 100.0 / currentPrice,
        )
    }

    // ---- marketable ids ----

    /**
     * Ids that have a market board at all. Cached 7 days.
     *
     * Must be consulted before [aggregated]: a single unmarketable id makes the
     * whole batch return HTTP 400 (measured with itemId 1 = gil).
     */
    suspend fun marketableIds(context: Context): Set<Int> = withContext(Dispatchers.IO) {
        val body = cachedGet(context, "marketable", "/marketable", 7L * 86400_000)
            ?: return@withContext emptySet()
        val arr = runCatching { JSONArray(body) }.getOrNull() ?: return@withContext emptySet()
        buildSet { for (i in 0 until arr.length()) add(arr.optInt(i)) }
    }

    // ---- plumbing ----

    private fun enc(s: String): String =
        URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    /** Disk-cached GET for the slow-changing tables. */
    private fun cachedGet(context: Context, key: String, path: String, maxAgeMs: Long): String? {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val f = File(dir, "$key.json")
        if (f.exists() && System.currentTimeMillis() - f.lastModified() < maxAgeMs) {
            runCatching { f.readText() }.getOrNull()?.takeIf { it.isNotBlank() }?.let {
                runCatching { f.setLastModified(System.currentTimeMillis()) }
                return it
            }
        }
        val body = get(path) ?: return runCatching { f.readText() }.getOrNull()
        runCatching {
            val tmp = File(dir, "$key.json.tmp")
            tmp.writeText(body)
            if (f.exists()) f.delete()
            if (!tmp.renameTo(f)) tmp.delete()
        }
        CacheMaintenance.schedule(context)
        return body
    }

    private fun get(path: String): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(BASE + path).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", UA)
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Accept-Encoding", "gzip")
            }
            if (conn.responseCode !in 200..299) return null
            val stream = if (conn.contentEncoding?.contains("gzip", true) == true) {
                GZIPInputStream(conn.inputStream)
            } else {
                conn.inputStream
            }
            stream.bufferedReader().use { it.readText() }.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }
}
