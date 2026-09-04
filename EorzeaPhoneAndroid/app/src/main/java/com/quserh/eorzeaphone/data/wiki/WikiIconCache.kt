package com.quserh.eorzeaphone.data.wiki

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import com.quserh.eorzeaphone.data.CacheMaintenance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * 灰机图床的图标缓存，用来兜 xivapi 的缺图。
 *
 * 结构照 [com.quserh.eorzeaphone.data.ItemIconLoader]：内存 LRU → 磁盘 → CDN。
 * 只在 xivapi 那条路 404 时才会走到这里，所以量很小
 * （实测约 474 件 / 51120，都是 7.41 以后的新装备）。
 */
object WikiIconCache {
    private val mem = object : LruCache<String, Bitmap>(16_000) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount / 1024
    }

    private const val UA = "EorzeaPhone/0.7.225 (+https://ff14.huijiwiki.com/)"
    private const val TIMEOUT_MS = 12_000

    private fun dir(context: Context) =
        File(context.cacheDir, "wiki-icons").apply { mkdirs() }

    fun peek(iconHash: String): Bitmap? =
        if (iconHash.isBlank()) null else mem.get(iconHash)

    /**
     * 按**任意 URL** 取图并缓存，给 wiki 条目页的配图用
     * （`pageimages` 回来的是完整 URL，不是 icon hash）。
     *
     * 缓存键是 URL 的 MD5 —— URL 里有 `/`、`:`，不能直接当文件名。
     * 复用 [load] 的锁和临时文件那套，不另写一个下载器。
     */
    suspend fun loadUrl(context: Context, url: String): Bitmap? {
        if (url.isBlank()) return null
        val key = java.security.MessageDigest.getInstance("MD5")
            .digest(url.toByteArray()).joinToString("") { "%02x".format(it) }
        return loadKeyed(context, key) { url }
    }

    suspend fun load(context: Context, iconHash: String): Bitmap? {
        if (iconHash.isBlank()) return null
        return loadKeyed(context, iconHash) { WikiRemote.huijiIconUrl(iconHash) }
    }

    /**
     * [key] 既是内存/磁盘缓存键，也是那把锁的键。[urlOf] 延迟求值 ——
     * 命中缓存时不必算 URL。
     */
    private suspend fun loadKeyed(
        context: Context,
        key: String,
        urlOf: () -> String,
    ): Bitmap? {
        mem.get(key)?.let { return it }
        // 同一个 hash 只允许一个协程去下。
        // 不加这道锁会有竞态：列表里两行用同一个图标时两边都看到 file.exists()==false，
        // 于是各下一份、各写同一个文件；后到的读到半截文件，decodeFile 返回 null，
        // 那一行就一直空着。真机上撞到过 —— 同为 icon 30704 的「幻境利剑·秘影」
        // 显示了，「幻境利剑·蚀影」没显示。
        val lock = locks.getOrPut(key) { Mutex() }
        return lock.withLock {
            mem.get(key)?.let { return@withLock it }
            withContext(Dispatchers.IO) {
                val file = File(dir(context), "$key.png")
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath)?.let {
                        runCatching { file.setLastModified(System.currentTimeMillis()) }
                        mem.put(key, it)
                        return@withContext it
                    }
                    // 解不出来说明是上一轮写坏的半截文件，删掉重下
                    runCatching { file.delete() }
                }
                val bmp = download(urlOf()) ?: return@withContext null
                // 先写临时文件再改名，避免别的协程读到写一半的内容
                runCatching {
                    val tmp = File(file.parentFile, "$key.tmp")
                    FileOutputStream(tmp).use {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                    if (!tmp.renameTo(file)) tmp.delete()
                }
                CacheMaintenance.schedule(context)
                mem.put(key, bmp)
                bmp
            }
        }
    }

    /** 每个 hash 一把锁。图标数有限（约 870 个走兜底），不用清理。 */
    private val locks = java.util.concurrent.ConcurrentHashMap<String, Mutex>()

    private fun download(url: String): Bitmap? {
        if (url.isBlank()) return null
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("User-Agent", UA)
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.use { BitmapFactory.decodeStream(it) }
        } catch (_: Exception) {
            null
        } finally {
            runCatching { conn?.disconnect() }
        }
    }
}
