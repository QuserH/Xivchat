package com.quserh.eorzeaphone.data.shizhijia

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
 * Local-first loader for remote 石之家 images (avatars, post covers, comment
 * pictures on the ff14risingstones.gcloud.com.cn CDN).
 *
 * Order: memory LRU -> disk cache -> network download. Every downloaded bitmap
 * is written to disk so revisits are instant and work offline, mirroring the
 * pattern already used by [com.quserh.eorzeaphone.data.ItemIconLoader] for game
 * icons. Because arbitrary URLs can be long, the disk file name is a SHA-1 hash
 * of the URL rather than the URL itself.
 */
object ShizhijiaImageLoader {

    private const val MEM_CACHE_BYTES = 64_000 // ~64MB of decoded bitmaps
    // v2: v1 cached every image as JPEG which destroyed transparency (medal /
    // badge icons got black backgrounds). Bumping the dir abandons those files.
    private const val DISK_DIR = "shizhijia-img-v2"

    private val memCache = object : LruCache<String, Bitmap>(MEM_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    private fun diskDir(app: Context): File = File(app.cacheDir, DISK_DIR).apply { mkdirs() }

    private fun cacheFileName(url: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-1")
            .digest(url.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) } + ".jpg"
    }

    /** Synchronous memory-cache peek for UI thread reads; returns null on miss. */
    fun peek(url: String): Bitmap? {
        if (url.isBlank()) return null
        return memCache.get(url)
    }

    suspend fun load(context: Context, url: String): Bitmap? {
        if (url.isBlank()) return null
        return withContext(Dispatchers.IO) {
            memCache.get(url)?.let { return@withContext it }

            val file = File(diskDir(context), cacheFileName(url))
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)?.let {
                    memCache.put(url, it)
                    return@withContext it
                }
            }

            val bmp = download(url) ?: return@withContext null
            runCatching {
                file.parentFile?.mkdirs()
                // Preserve alpha: JPEG has no transparency, so transparent PNGs
                // (medals, badges) would get black backgrounds. Store those as PNG.
                val fmt = if (bmp.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                FileOutputStream(file).use { bmp.compress(fmt, 90, it) }
            }
            memCache.put(url, bmp)
            bmp
        }
    }

    private fun download(url: String): Bitmap? {
        // A single retry absorbs transient timeouts/TLS hiccups on slow links.
        repeat(2) { attempt ->
            val bmp = downloadOnce(url)
            if (bmp != null) return bmp
            if (attempt == 0) Thread.sleep(400)
        }
        return null
    }

    private fun downloadOnce(url: String): Bitmap? = try {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", "EorzeaPhone/0.3")
            setRequestProperty("Accept", "image/*")
            setRequestProperty("Referer", "https://ff14risingstones.web.sdo.com/")
        }
        try {
            if (connection.responseCode in 200..299) {
                // Read the whole body then decode: decodeStream on an
                // HttpURLConnection stream returns null for some CDN responses,
                // while decodeByteArray is reliable.
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isEmpty()) {
                    android.util.Log.w("ShizhijiaImg", "empty body for $url")
                    null
                } else {
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bmp == null) android.util.Log.w("ShizhijiaImg", "decode null (${bytes.size} bytes) for $url")
                    bmp
                }
            } else {
                android.util.Log.w("ShizhijiaImg", "HTTP ${connection.responseCode} for $url")
                null
            }
        } finally {
            connection.disconnect()
        }
    } catch (e: Exception) {
        // Surface the real reason in logcat so device-side failures are debuggable.
        android.util.Log.w("ShizhijiaImg", "download failed for $url: ${e::class.simpleName}: ${e.message}")
        null
    }
}