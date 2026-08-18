package com.quserh.eorzeaphone.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** Loads FFXIV item/currency icons from the public datamining mirror and caches them in memory. */
object ItemIconLoader {
    private val cache = object : LruCache<String, Bitmap>(96) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun urlFor(iconId: Int): String {
        if (iconId <= 0) return ""
        val digits = "%06d".format(iconId)
        val folder = "%06d".format((iconId / 1000) * 1000)
        return "https://raw.githubusercontent.com/xivapi/ffxiv-datamining/master/icons/$folder/$digits.png"
    }

    suspend fun load(iconId: Int): Bitmap? {
        if (iconId <= 0) return null
        val url = urlFor(iconId)
        cache.get(url)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    setRequestProperty("User-Agent", "EorzeaPhone/0.2")
                }
                try {
                    if (connection.responseCode == 200) {
                        connection.inputStream.use { BitmapFactory.decodeStream(it) }
                    } else {
                        null
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (_: IOException) {
                null
            } catch (_: Exception) {
                null
            }
        }
        if (bitmap != null) cache.put(url, bitmap)
        return bitmap
    }
}
