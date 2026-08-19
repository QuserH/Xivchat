package com.quserh.eorzeaphone.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Local-first icon loader: memory LRU -> disk cache (cacheDir/icons) -> CDN download,
 * writing each downloaded icon to disk so later loads are instant and work offline.
 */
object ItemIconLoader {
    private val memCache = object : LruCache<String, Bitmap>(96_000) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    fun diskCache(app: Context): File = File(app.cacheDir, "icons").apply { mkdirs() }

    fun urlFor(iconId: Int): String {
        if (iconId <= 0) return ""
        val digits = "%06d".format(iconId)
        val folder = "%06d".format((iconId / 1000) * 1000)
        return "https://xivapi.com/i/$folder/$digits.png"
    }

    suspend fun load(app: Context, iconId: Int): Bitmap? {
        if (iconId <= 0) return null
        val url = urlFor(iconId)
        return withContext(Dispatchers.IO) {
            memCache.get(url)?.let { return@withContext it }

            val file = File(diskCache(app), "$iconId.png")
            if (file.exists()) {
                BitmapFactory.decodeFile(file.absolutePath)?.let {
                    memCache.put(url, it)
                    return@withContext it
                }
            }

            val bmp = download(url) ?: return@withContext null
            try {
                file.parentFile?.mkdirs()
                FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } catch (_: Exception) { }
            memCache.put(url, bmp)
            bmp
        }
    }

    private fun download(url: String): Bitmap? = try {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 8000
            readTimeout = 8000
            setRequestProperty("User-Agent", "EorzeaPhone/0.3")
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
    } catch (_: Exception) {
        null
    }
}
