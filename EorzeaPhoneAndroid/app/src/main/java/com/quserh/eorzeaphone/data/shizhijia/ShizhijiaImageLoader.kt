package com.quserh.eorzeaphone.data.shizhijia

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.quserh.eorzeaphone.data.CacheMaintenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
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

    private const val MEM_CACHE_BYTES = 32_000 // ~32MB of decoded bitmaps
    private const val MAX_IMAGE_DIMENSION = 2_048
    // A CDN response is compressed on the wire, so this is a guard against an
    // accidentally returned original/video or a maliciously large chunked body.  The
    // decoded bitmap is downsampled separately below; never let the response itself grow
    // without a bound first.
    private const val MAX_RESPONSE_BYTES = 16L * 1024L * 1024L
    // v2: v1 cached every image as JPEG which destroyed transparency (medal /
    // badge icons got black backgrounds). Bumping the dir abandons those files.
    private const val DISK_DIR = "shizhijia-img-v2"

    private val memCache = object : LruCache<String, Bitmap>(MEM_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    // url -> (width, height) so layouts can reserve the exact aspect ratio
    // before the full bitmap decodes (stable waterfall, no reflow while scrolling).
    private val sizeCache = java.util.concurrent.ConcurrentHashMap<String, Pair<Int, Int>>()

    private fun diskDir(app: Context): File = File(app.cacheDir, DISK_DIR).apply { mkdirs() }

    private fun cacheFileName(url: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-1")
            .digest(url.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) } + ".jpg"
    }

    /**
     * Synchronous bounds-only size read from the disk cache (fast, no full
     * decode). Falls back to the in-memory size cache. Returns null when the
     * image isn't downloaded yet.
     */
    fun peekSize(context: Context, url: String): Pair<Int, Int>? {
        if (url.isBlank()) return null
        sizeCache[url]?.let { return it }
        val file = File(diskDir(context), cacheFileName(url))
        if (!file.exists() || file.length() == 0L) return null
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, opts)
        val s = if (opts.outWidth > 0 && opts.outHeight > 0) (opts.outWidth to opts.outHeight) else null
        if (s != null) sizeCache[url] = s
        return s
    }

    private fun rememberSize(url: String, bmp: Bitmap) {
        sizeCache[url] = bmp.width to bmp.height
    }

    /** Synchronous memory-cache peek for UI thread reads; returns null on miss. */
    fun peek(url: String): Bitmap? {
        if (url.isBlank()) return null
        return memCache.get(url)
    }

    suspend fun load(context: Context, url: String): Bitmap? {
        if (url.isBlank()) return null
        return withContext(Dispatchers.IO) {
            memCache.get(url)?.let { rememberSize(url, it); return@withContext it }

            val file = File(diskDir(context), cacheFileName(url))
            if (file.exists() && file.length() > 0) {
                decodeFileSampled(file)?.let {
                    runCatching { file.setLastModified(System.currentTimeMillis()) }
                    memCache.put(url, it); rememberSize(url, it)
                    return@withContext it
                }
                runCatching { file.delete() }
            }

            val bmp = download(url) ?: return@withContext null
            rememberSize(url, bmp)
            runCatching {
                file.parentFile?.mkdirs()
                // Preserve alpha: JPEG has no transparency, so transparent PNGs
                // (medals, badges) would get black backgrounds. Store those as PNG.
                val fmt = if (bmp.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val tmp = File(file.parentFile, file.name + ".tmp")
                FileOutputStream(tmp).use { bmp.compress(fmt, 86, it) }
                if (file.exists()) file.delete()
                if (!tmp.renameTo(file)) tmp.delete()
            }
            CacheMaintenance.schedule(context)
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
                // Keep decodeByteArray (some CDN streams return null from decodeStream),
                // but read through a hard cap.  `InputStream.readBytes()` used to retain
                // an unbounded original in RAM before the bounds pass, which made one
                // oversized post image capable of spiking memory by hundreds of MiB.
                val bytes = connection.inputStream.use { readBounded(it, MAX_RESPONSE_BYTES) }
                if (bytes == null) {
                    android.util.Log.w("ShizhijiaImg", "response exceeds ${MAX_RESPONSE_BYTES} bytes for $url")
                    null
                } else if (bytes.isEmpty()) {
                    android.util.Log.w("ShizhijiaImg", "empty body for $url")
                    null
                } else {
                    // Phone layouts never display a multi-thousand-pixel source at native
                    // size. Downsample before decoding so one post photo cannot consume tens
                    // of MiB in RAM and then be written back at unnecessary resolution.
                    val bmp = decodeBytesSampled(bytes)
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

    /** Read a response without ever allocating beyond [limit] bytes. */
    private fun readBounded(input: InputStream, limit: Long): ByteArray? {
        val out = ByteArrayOutputStream(minOf(limit, 64L * 1024L).toInt())
        val buffer = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            total += count
            if (total > limit) return null
            out.write(buffer, 0, count)
        }
        return out.toByteArray()
    }

    private fun decodeFileSampled(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    private fun decodeBytesSampled(bytes: ByteArray): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight)
        }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    }

    private fun sampleSize(width: Int, height: Int): Int {
        var sample = 1
        while (width / (sample * 2) >= MAX_IMAGE_DIMENSION ||
            height / (sample * 2) >= MAX_IMAGE_DIMENSION
        ) {
            sample *= 2
        }
        return sample
    }
}
