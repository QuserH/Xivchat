package com.quserh.eorzeaphone.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.floor

data class FishingItemRef(val id: Int, val name: String, val icon: Int)

data class FishingAetheryte(val id: Int, val x: Int, val y: Int, val name: String)

data class FishingSpot(
    val id: Int,
    val name: String,
    val zone: String,
    val region: String,
    val territory: Int,
    val weatherRate: Int,
    val mapFile: String,
    val mapSizeFactor: Int,
    val mapOffsetX: Int,
    val mapOffsetY: Int,
    val x: Int,
    val y: Int,
    val radius: Int,
    val aetherytes: List<FishingAetheryte>,
)

data class FishingPredator(val id: Int, val name: String, val icon: Int, val count: Int)

data class FishingFish(
    val id: Int,
    val logId: Int,
    val name: String,
    val icon: Int,
    val version: Double,
    val method: String,
    val tier: String,
    val startHour: Double,
    val endHour: Double,
    val startText: String,
    val endText: String,
    val previousWeather: List<Int>,
    val weather: List<Int>,
    val tug: String,
    val hook: String,
    val intuitionSeconds: Int,
    val predators: List<FishingPredator>,
    val bait: List<FishingItemRef>,
    val mooch: List<FishingItemRef>,
    val path: List<FishingItemRef>,
    val spots: List<FishingSpot>,
    val snagging: Boolean,
    val folkloreId: Int,
    val lure: String,
    val lureStacks: Int,
    val collectableInfo: String,
    val quest: String,
    val gathering: Int,
    val perception: Int,
    val size: String,
    val speed: String,
    val guide: String,
    val guidePath: String,
    val guideAuthor: String,
) {
    val isBigFish: Boolean get() = tier != "normal" && tier != "ikdNormalFish"
    val restricted: Boolean get() = startHour != 0.0 || endHour != 24.0 || weather.isNotEmpty() || previousWeather.isNotEmpty()
}

data class FishingWeather(val id: Int, val name: String, val icon: Int)
data class FishingWeatherRate(val id: Int, val weatherIds: List<Int>, val rates: List<Int>)

data class FishingCatalog(
    val fish: List<FishingFish>,
    val weather: Map<Int, FishingWeather>,
    val weatherRates: Map<Int, FishingWeatherRate>,
)

object FishingCatalogRepository {
    @Volatile private var cached: FishingCatalog? = null

    suspend fun load(context: Context): FishingCatalog = cached ?: withContext(Dispatchers.IO) {
        cached ?: parse(context.assets.open("fishing/catalog.json").bufferedReader().use { it.readText() }).also { cached = it }
    }

    private fun parse(text: String): FishingCatalog {
        val root = JSONObject(text)
        val weather = root.getJSONArray("weather").objects().associate { row ->
            val value = FishingWeather(row.getInt("id"), row.optString("name"), row.optInt("icon"))
            value.id to value
        }
        val rates = root.getJSONArray("weatherRates").objects().associate { row ->
            val value = FishingWeatherRate(row.getInt("id"), row.getJSONArray("weatherIds").ints(), row.getJSONArray("rates").ints())
            value.id to value
        }
        val fish = root.getJSONArray("fish").objects().map(::parseFish)
        return FishingCatalog(fish, weather, rates)
    }

    private fun parseFish(row: JSONObject): FishingFish = FishingFish(
        id = row.getInt("id"),
        logId = row.getInt("logId"),
        name = row.optString("name"),
        icon = row.optInt("icon"),
        version = row.optDouble("version"),
        method = row.optString("method", "rod"),
        tier = row.optString("tier", "normal"),
        startHour = row.optDouble("start", 0.0),
        endHour = row.optDouble("end", 24.0),
        startText = row.optString("startText", "0"),
        endText = row.optString("endText", "24"),
        previousWeather = row.optJSONArray("previousWeather")?.ints().orEmpty(),
        weather = row.optJSONArray("weather")?.ints().orEmpty(),
        tug = row.optString("tug"),
        hook = row.optString("hook"),
        intuitionSeconds = row.optInt("intuition"),
        predators = row.optJSONArray("predators")?.objects()?.map {
            FishingPredator(it.optInt("id"), it.optString("name"), it.optInt("icon"), it.optInt("count"))
        }.orEmpty(),
        bait = row.optJSONArray("bait")?.itemRefs().orEmpty(),
        mooch = row.optJSONArray("mooch")?.itemRefs().orEmpty(),
        path = row.optJSONArray("path")?.itemRefs().orEmpty(),
        spots = row.optJSONArray("spots")?.objects()?.map {
            val crystals = it.optJSONArray("aetherytes")?.objects()?.map { crystal ->
                FishingAetheryte(crystal.optInt("id"), crystal.optInt("x"), crystal.optInt("y"), crystal.optString("name"))
            }.orEmpty()
            FishingSpot(it.optInt("id"), it.optString("name"), it.optString("zone"), it.optString("region"), it.optInt("territory"), it.optInt("weatherRate"), it.optString("mapFile"), it.optInt("mapSizeFactor", 100), it.optInt("mapOffsetX"), it.optInt("mapOffsetY"), it.optInt("x"), it.optInt("y"), it.optInt("radius"), crystals)
        }.orEmpty(),
        snagging = row.optBoolean("snagging"),
        folkloreId = row.optInt("folklore"),
        lure = row.optString("lure"),
        lureStacks = row.optInt("lureStacks"),
        collectableInfo = row.optString("collectableInfo"),
        quest = row.optString("quest"),
        gathering = row.optInt("gathering"),
        perception = row.optInt("perception"),
        size = row.optString("size"),
        speed = row.optString("speed"),
        guide = row.optString("guide"),
        guidePath = row.optString("guidePath"),
        guideAuthor = row.optString("guideAuthor"),
    )

    private fun JSONArray.objects(): List<JSONObject> = buildList(length()) { for (index in 0 until length()) add(getJSONObject(index)) }
    private fun JSONArray.ints(): List<Int> = buildList(length()) { for (index in 0 until length()) add(optInt(index)) }
    private fun JSONArray.itemRefs(): List<FishingItemRef> = objects().map { FishingItemRef(it.optInt("id"), it.optString("name"), it.optInt("icon")) }
}

data class FishingWindow(val startMillis: Long, val endMillis: Long, val spot: FishingSpot?)

object FishingWindowCalculator {
    private const val EORZEA_HOUR_MILLIS = 175_000.0
    private const val WEATHER_BLOCK_MILLIS = 1_400_000L

    fun nextWindow(fish: FishingFish, catalog: FishingCatalog, nowMillis: Long = System.currentTimeMillis()): FishingWindow? {
        val spots = fish.spots.ifEmpty { listOf(null) }
        return spots.mapNotNull { spot -> nextForSpot(fish, spot, catalog, nowMillis) }.minByOrNull { it.startMillis }
    }

    fun availableNow(fish: FishingFish, catalog: FishingCatalog, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val window = nextWindow(fish, catalog, nowMillis - 1_000L) ?: return false
        return window.startMillis <= nowMillis && window.endMillis > nowMillis
    }

    private fun nextForSpot(fish: FishingFish, spot: FishingSpot?, catalog: FishingCatalog, nowMillis: Long): FishingWindow? {
        val firstBlock = Math.floorDiv(nowMillis, WEATHER_BLOCK_MILLIS)
        for (offset in 0..1_000) {
            val block = firstBlock + offset
            val blockStart = block * WEATHER_BLOCK_MILLIS
            val blockEnd = blockStart + WEATHER_BLOCK_MILLIS
            if (!weatherMatches(fish, spot, catalog, blockStart)) continue
            val blockStartHour = blockStart / EORZEA_HOUR_MILLIS
            val blockEndHour = blockEnd / EORZEA_HOUR_MILLIS
            val day = floor(blockStartHour / 24.0).toLong()
            for (candidateDay in day - 1..day + 1) {
                val (timeStart, timeEnd) = if (fish.startHour == 0.0 && fish.endHour == 24.0) {
                    candidateDay * 24.0 to (candidateDay + 1) * 24.0
                } else if (fish.startHour < fish.endHour) {
                    candidateDay * 24.0 + fish.startHour to candidateDay * 24.0 + fish.endHour
                } else {
                    candidateDay * 24.0 + fish.startHour to (candidateDay + 1) * 24.0 + fish.endHour
                }
                val start = maxOf(blockStartHour, timeStart, nowMillis / EORZEA_HOUR_MILLIS)
                val end = minOf(blockEndHour, timeEnd)
                if (end > start) return FishingWindow((start * EORZEA_HOUR_MILLIS).toLong(), (end * EORZEA_HOUR_MILLIS).toLong(), spot)
            }
        }
        return null
    }

    private fun weatherMatches(fish: FishingFish, spot: FishingSpot?, catalog: FishingCatalog, blockStart: Long): Boolean {
        if (fish.weather.isEmpty() && fish.previousWeather.isEmpty()) return true
        val rate = spot?.weatherRate?.let(catalog.weatherRates::get) ?: return false
        val current = weatherAt(blockStart, rate)
        val previous = weatherAt(blockStart - WEATHER_BLOCK_MILLIS, rate)
        return (fish.weather.isEmpty() || current in fish.weather) && (fish.previousWeather.isEmpty() || previous in fish.previousWeather)
    }

    private fun weatherAt(timeMillis: Long, rate: FishingWeatherRate): Int {
        val unix = Math.floorDiv(timeMillis, 1_000L)
        val bell = Math.floorDiv(unix, 175L)
        val increment = (bell + 8L - bell % 8L) % 24L
        val totalDays = Math.floorDiv(unix, 4_200L)
        val base = totalDays * 100L + increment
        val step1 = (base shl 11) xor base
        val target = (((step1 ushr 8) xor step1) % 100L).toInt()
        var cumulative = 0
        for (index in rate.weatherIds.indices) {
            cumulative += rate.rates.getOrElse(index) { 0 }
            if (target < cumulative) return rate.weatherIds[index]
        }
        return rate.weatherIds.lastOrNull() ?: 0
    }
}

object FishingAlarmStore {
    private const val PREFS = "fishing_alarms"
    private const val IDS = "fish_ids"
    const val DEFAULT_LEAD_MINUTES = 5
    private const val LEAD_MINUTES = "lead_minutes"

    fun leadMinutes(context: Context): Int = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getInt(LEAD_MINUTES, DEFAULT_LEAD_MINUTES).coerceIn(0, 10)

    fun enabledIds(context: Context): Set<Int> = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        .getStringSet(IDS, emptySet()).orEmpty().mapNotNull(String::toIntOrNull).toSet()

    fun isEnabled(context: Context, fishId: Int): Boolean = fishId in enabledIds(context)

    fun set(context: Context, fish: FishingFish, catalog: FishingCatalog, enabled: Boolean) {
        val ids = enabledIds(context).toMutableSet()
        if (enabled) ids += fish.id else ids -= fish.id
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putStringSet(IDS, ids.map(Int::toString).toSet()).apply()
        val alarmId = alarmId(fish.id)
        if (!enabled) {
            AlarmScheduler.cancel(context, alarmId)
            return
        }
        scheduleNext(context, fish, catalog)
    }

    fun updateLeadMinutes(context: Context, catalog: FishingCatalog, minutes: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putInt(LEAD_MINUTES, minutes.coerceIn(0, 10)).apply()
        enabledIds(context).forEach { id -> catalog.fish.firstOrNull { it.id == id }?.let { scheduleNext(context, it, catalog) } }
    }

    fun scheduleNext(context: Context, fish: FishingFish, catalog: FishingCatalog, afterMillis: Long = System.currentTimeMillis()) {
        val window = FishingWindowCalculator.nextWindow(fish, catalog, afterMillis) ?: return
        val leadMinutes = leadMinutes(context)
        val trigger = maxOf(System.currentTimeMillis() + 1_000L, window.startMillis - leadMinutes * 60_000L)
        val place = window.spot?.let { "${it.region} · ${it.name}" }.orEmpty()
        AlarmScheduler.scheduleAt(
            context,
            alarmId(fish.id),
            trigger,
            "${fish.name} 即将可以捕获",
            place.ifBlank { "捕鱼窗口即将开始" },
            fish.id,
            window.endMillis,
        )
    }

    fun refresh(context: Context, catalog: FishingCatalog) {
        val byId = catalog.fish.associateBy { it.id }
        enabledIds(context).forEach { id -> byId[id]?.let { set(context, it, catalog, true) } }
    }

    private fun alarmId(fishId: Int): Long = (0x46000000 xor fishId).toLong()
}

object FishingMapImageLoader {
    suspend fun load(context: Context, mapFile: String): Bitmap? = withContext(Dispatchers.IO) {
        if (mapFile.isBlank()) return@withContext null
        val safeName = mapFile.replace('/', '_')
        val file = java.io.File(context.cacheDir, "maps/$safeName.jpg")
        if (file.exists()) BitmapFactory.decodeFile(file.absolutePath)?.let {
            runCatching { file.setLastModified(System.currentTimeMillis()) }
            return@withContext it
        }
        val area = mapFile.substringBefore('/')
        val layer = mapFile.substringAfter('/', "00")
        val urls = listOf(
            "https://v2.xivapi.com/api/asset/map/$mapFile",
            "https://xivapi.com/m/$area/$area.$layer.jpg",
        )
        val bitmap = urls.firstNotNullOfOrNull { url ->
            runCatching {
                val connection = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                    connectTimeout = 8_000
                    readTimeout = 12_000
                    instanceFollowRedirects = true
                    setRequestProperty("User-Agent", "EorzeaPhone/0.4")
                }
                try { if (connection.responseCode in 200..299) connection.inputStream.use(BitmapFactory::decodeStream) else null }
                finally { connection.disconnect() }
            }.getOrNull()
        } ?: return@withContext null
        runCatching {
            file.parentFile?.mkdirs()
            val tmp = java.io.File(file.parentFile, file.name + ".tmp")
            java.io.FileOutputStream(tmp).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 88, it) }
            if (file.exists()) file.delete()
            if (!tmp.renameTo(file)) tmp.delete()
        }
        CacheMaintenance.schedule(context)
        bitmap
    }
}
