package com.quserh.eorzeaphone.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicInteger

/**
 * Keeps downloaded/cache data bounded without touching user-owned files.
 *
 * Only directories below [Context.getCacheDir] (plus the explicitly named crash-log folder)
 * are considered. Photos, avatars, exported files, databases in filesDir, and preferences are
 * intentionally out of scope. Each cache has both a byte ceiling and an age ceiling; the oldest
 * entries are removed first when either limit is exceeded.
 */
object CacheMaintenance {
    data class Report(
        val deletedFiles: Int,
        val deletedBytes: Long,
        val beforeBytes: Long,
        val afterBytes: Long,
    )

    /** A user-facing breakdown of the app's private data directory. */
    data class StorageEntry(
        val id: String,
        val label: String,
        val bytes: Long,
        /** True when the entry can be recreated without losing user content/session. */
        val reclaimable: Boolean,
    )

    data class StorageReport(
        val totalBytes: Long,
        val entries: List<StorageEntry>,
        val generatedAt: Long = System.currentTimeMillis(),
    ) {
        val reclaimableBytes: Long
            get() = entries.filter { it.reclaimable }.sumOf { it.bytes }
    }

    data class ImageCompactionReport(
        val scannedFiles: Int,
        val compactedFiles: Int,
        val savedBytes: Long,
    )

    private data class Policy(
        val relativePath: String,
        val maxBytes: Long,
        val maxAgeMs: Long,
        val maxFiles: Int,
    )

    private val worker = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val gate = Mutex()
    private val lastScheduledAt = AtomicLong(0L)
    // WebView owns its cache files and can be writing them while a login page is
    // visible.  File-level trimming during that window can leave a half-written
    // cache entry (or, on older WebView builds, a locked SQLite journal).  The
    // login screen marks its lifetime here; a trim is deferred until disposal.
    private val activeWebViews = AtomicInteger(0)

    // These limits keep the normal cache footprint comfortably below the hundreds of MiB
    // seen on long-lived installs while leaving enough room for offline revisits.
    private val policies = listOf(
        Policy("icons", 48L * MiB, 45L * DAY, 12_000),
        Policy("wiki-icons", 24L * MiB, 45L * DAY, 8_000),
        Policy("shizhijia-img-v2", 64L * MiB, 30L * DAY, 2_000),
        // v1 used this directory before transparent PNGs were handled correctly.
        // It is no longer read by the loader, so retain no orphaned copies after an
        // upgrade.  A zero budget/age/file count is an explicit "delete everything"
        // policy; it is intentionally not applied to any active cache directory.
        Policy("shizhijia-img", 0L, 0L, 0),
        Policy("maps", 32L * MiB, 90L * DAY, 1_000),
        Policy("market-cache", 8L * MiB, 30L * DAY, 1_000),
        // wiki-detail also contains page/Quest/Instance/Shop subdirectories; trimming the
        // root recursively gives one shared budget instead of an unbounded budget per kind.
        Policy("wiki-detail", 24L * MiB, 60L * DAY, 3_000),
        Policy("share", 8L * MiB, DAY, 100),
    )

    private val crashPolicy = Policy("crash", 2L * MiB, 14L * DAY, 50)

    // WebView keeps these outside Context.cacheDir (usually under
    // dataDir/app_webview/Default), so Android's "clear cache" button does not
    // touch them and our normal cache policies cannot see them.  Never include
    // Cookies, Web Data, Local Storage, or databases: those contain the SDO
    // session and clearing them would log the user out.
    private val webViewCacheNames = setOf(
        "Cache", "Code Cache", "GPUCache", "Media Cache",
    )
    private const val WEBVIEW_MAX_BYTES = 32L * 1024L * 1024L
    private const val WEBVIEW_MAX_FILES = 2_000

    /** Schedule a background trim. Calls inside image loaders are throttled to one/15 min. */
    fun schedule(context: Context, force: Boolean = false) {
        val now = System.currentTimeMillis()
        while (true) {
            val previous = lastScheduledAt.get()
            if (!force && now - previous < SCHEDULE_INTERVAL_MS) return
            if (lastScheduledAt.compareAndSet(previous, now)) break
        }
        val app = context.applicationContext
        worker.launch {
            runCatching { trimNow(app) }
                .onFailure { Log.w(TAG, "cache trim failed", it) }
        }
    }

    /** Run synchronously from a background coroutine when a caller needs a fresh report. */
    suspend fun trimNow(context: Context): Report = gate.withLock {
        val app = context.applicationContext
        var deletedFiles = 0
        var deletedBytes = 0L
        var beforeBytes = 0L
        var afterBytes = 0L

        for (policy in policies) {
            val result = trimDirectory(File(app.cacheDir, policy.relativePath), policy)
            deletedFiles += result.deletedFiles
            deletedBytes += result.deletedBytes
            beforeBytes += result.beforeBytes
            afterBytes += result.afterBytes
        }
        // Crash logs live in filesDir for post-mortem access, but are still disposable and
        // must not be allowed to grow forever.
        val crash = trimDirectory(File(app.filesDir, crashPolicy.relativePath), crashPolicy)
        deletedFiles += crash.deletedFiles
        deletedBytes += crash.deletedBytes
        beforeBytes += crash.beforeBytes
        afterBytes += crash.afterBytes

        // app_webview is deliberately handled after the ordinary cache roots and
        // only while no WebView is alive.  `clearCache(true)` on disposal normally
        // does the first pass; this is the repair path for old installs that grew
        // before the bounded cache policy existed.
        if (activeWebViews.get() == 0) {
            val web = trimWebViewCaches(app)
            deletedFiles += web.deletedFiles
            deletedBytes += web.deletedBytes
            beforeBytes += web.beforeBytes
            afterBytes += web.afterBytes
        }

        val report = Report(deletedFiles, deletedBytes, beforeBytes, afterBytes)
        if (deletedFiles > 0) {
            Log.i(TAG, "trimmed ${deletedFiles} files / ${deletedBytes} bytes; " +
                "${beforeBytes} -> ${afterBytes} bytes")
        }
        report
    }

    /** Approximate current usage of the managed directories; intended for diagnostics UI. */
    suspend fun managedBytes(context: Context): Long = gate.withLock {
        val app = context.applicationContext
        val ordinary = (policies.map { bytesIn(File(app.cacheDir, it.relativePath)) } +
            bytesIn(File(app.filesDir, crashPolicy.relativePath))).sum()
        ordinary + webViewCacheBytes(app)
    }

    /**
     * Return a bounded, mutually-exclusive breakdown of private app data.
     *
     * This deliberately reports sizes rather than reading any large preference value.  A
     * previous diagnostic used `SharedPreferences.all`, which materialised the entire legacy
     * chat transcript in memory just to measure it.  Directory walking is slower but safe and
     * makes the 600-MB cases (WebView cache, old avatars, or legacy XML) visible to the user.
     */
    suspend fun storageReport(context: Context): StorageReport = gate.withLock {
        storageReportLocked(context.applicationContext)
    }

    /**
     * Remove only data that is explicitly disposable: app cache files, WebView cache files,
     * and crash logs.  Cookies, local storage, chat/market databases, photos and chosen
     * avatars are never touched.  The operation is idempotent and safe to run while the app
     * is open; a WebView cache is deferred when a login page is alive.
     */
    suspend fun clearTemporaryCaches(context: Context): Report = gate.withLock {
        val app = context.applicationContext
        val before = storageReportLocked(app).totalBytes
        var deletedFiles = 0
        var deletedBytes = 0L

        fun clearTree(root: File) {
            if (!root.exists() || !root.isDirectory) return
            val files = runCatching { root.walkTopDown().filter { it.isFile }.toList() }
                .getOrDefault(emptyList())
            files.forEach { file ->
                val size = file.length().coerceAtLeast(0L)
                if (runCatching { file.delete() }.getOrDefault(false)) {
                    deletedFiles++
                    deletedBytes += size
                }
            }
            runCatching {
                root.walkBottomUp().filter { it.isDirectory && it != root && it.listFiles().isNullOrEmpty() }
                    .forEach { it.delete() }
            }
        }

        // Everything below cacheDir is, by Android's contract, disposable.  Keep the root
        // itself so libraries which hold its File reference continue to work.
        clearTree(app.cacheDir)
        clearTree(File(app.filesDir, crashPolicy.relativePath))

        if (activeWebViews.get() == 0) {
            val files = webViewFiles(app)
            files.forEach { file ->
                val size = file.length().coerceAtLeast(0L)
                if (runCatching { file.delete() }.getOrDefault(false)) {
                    deletedFiles++
                    deletedBytes += size
                }
            }
            webViewRoots(app).forEach { root ->
                runCatching {
                    webViewCacheDirs(root).forEach { cache ->
                        cache.walkBottomUp()
                            .filter { it.isDirectory && it != cache && it.listFiles().isNullOrEmpty() }
                            .forEach { it.delete() }
                        if (cache.listFiles().isNullOrEmpty()) cache.delete()
                    }
                }
            }
        }

        val after = storageReportLocked(app).totalBytes
        Report(deletedFiles, (before - after).coerceAtLeast(deletedBytes), before, after)
    }

    /**
     * One-time repair for images copied by pre-0.7.3 builds.  Those builds copied the source
     * JPEG/PNG byte-for-byte, so a single modern camera photo could occupy 10–20 MB in the
     * app's private directory.  New writes are already bounded in PhoneState.savePickedImage;
     * this pass shrinks old files to at most 768 px and atomically replaces them only after a
     * successful encode.  If decoding/replacement fails, the original is left untouched.
     */
    suspend fun compactLegacyImages(context: Context): ImageCompactionReport = gate.withLock {
        val app = context.applicationContext
        val marker = app.getSharedPreferences("eorzea_phone_maintenance", Context.MODE_PRIVATE)
        if (marker.getBoolean("legacyImagesCompactedV1", false)) {
            return@withLock ImageCompactionReport(0, 0, 0L)
        }
        val roots = listOf("avatars", "conv-icons", "friend-avatars")
            .map { File(app.filesDir, it) }
        val files = roots.flatMap { root ->
            runCatching { root.walkTopDown().filter { it.isFile && !it.name.endsWith(".tmp") }.toList() }
                .getOrDefault(emptyList())
        }
        var compacted = 0
        var saved = 0L
        files.forEach { file ->
            val oldBytes = file.length().coerceAtLeast(0L)
            val result = compactImage(file)
            if (result > 0L) {
                compacted++
                saved += (oldBytes - file.length()).coerceAtLeast(0L)
            }
        }
        // Set the marker only after the complete pass.  A crash or a transient file lock will
        // therefore retry on the next launch instead of permanently preserving a large file.
        marker.edit().putBoolean("legacyImagesCompactedV1", true).apply()
        ImageCompactionReport(files.size, compacted, saved)
    }

    private fun storageReportLocked(app: Context): StorageReport {
        val dataRoot = app.applicationInfo?.dataDir?.let(::File)
            ?: app.filesDir.parentFile
            ?: app.filesDir
        val cacheBytes = bytesIn(app.cacheDir)
        val crashBytes = bytesIn(File(app.filesDir, crashPolicy.relativePath))
        val photosBytes = bytesIn(File(app.filesDir, "photos"))
        val avatarBytes = listOf("avatars", "conv-icons", "friend-avatars")
            .sumOf { bytesIn(File(app.filesDir, it)) }
        val marketBytes = listOf("market.db", "market.db-wal", "market.db-shm", "market.db-journal")
            .sumOf { File(app.filesDir, it).length().coerceAtLeast(0L) }
        val marketCategoryBytes = File(app.filesDir, "market_categories_cache.json")
            .length().coerceAtLeast(0L)
        val databaseRoot = File(dataRoot, "databases")
        val chatDbBytes = listOf("chat_archive.db", "chat_archive.db-wal", "chat_archive.db-shm", "chat_archive.db-journal")
            .sumOf { File(databaseRoot, it).length().coerceAtLeast(0L) }
        val databaseBytes = bytesIn(databaseRoot)
        val otherDatabaseBytes = (databaseBytes - chatDbBytes).coerceAtLeast(0L)
        val sharedPrefsBytes = bytesIn(File(dataRoot, "shared_prefs"))
        val webRoots = webViewRoots(app)
        val webTotalBytes = webRoots.sumOf { bytesIn(it) }
        val webCacheBytes = webViewCacheBytes(app).coerceAtMost(webTotalBytes)
        val webPersistentBytes = (webTotalBytes - webCacheBytes).coerceAtLeast(0L)
        val codeCacheBytes = bytesIn(File(dataRoot, "code_cache"))
        val noBackupBytes = bytesIn(File(dataRoot, "no_backup"))
        val filesRootBytes = bytesIn(app.filesDir)
        val knownFilesBytes = (photosBytes + avatarBytes + crashBytes + marketBytes + marketCategoryBytes)
            .coerceAtMost(filesRootBytes)
        val otherFilesBytes = (filesRootBytes - knownFilesBytes).coerceAtLeast(0L)

        // Entries are intentionally disjoint.  The residual category catches files created by
        // future features without making the report silently under-count total app data.
        val entries = mutableListOf(
            StorageEntry("cache", "可清理临时缓存", cacheBytes, reclaimable = true),
            StorageEntry("web-cache", "石之家网页缓存", webCacheBytes, reclaimable = true),
            StorageEntry("web-session", "石之家登录数据（保留）", webPersistentBytes, reclaimable = false),
            StorageEntry("chat-db", "聊天数据库", chatDbBytes, reclaimable = false),
            StorageEntry("market-db", "市场历史数据库", marketBytes, reclaimable = false),
            StorageEntry("database-other", "其他数据库", otherDatabaseBytes, reclaimable = false),
            StorageEntry("shared-prefs", "配置与热缓存", sharedPrefsBytes, reclaimable = false),
            StorageEntry("photos", "本地照片", photosBytes, reclaimable = false),
            StorageEntry("avatars", "自选头像/会话图标", avatarBytes, reclaimable = false),
            StorageEntry("market-catalog", "市场目录索引", marketCategoryBytes, reclaimable = false),
            StorageEntry("crash", "崩溃日志", crashBytes, reclaimable = true),
            StorageEntry("code-cache", "系统代码缓存", codeCacheBytes, reclaimable = true),
            StorageEntry("no-backup", "其他应用文件", noBackupBytes, reclaimable = false),
        )
        val accounted = entries.sumOf { it.bytes }
        val total = bytesIn(dataRoot)
        val residual = (total - accounted).coerceAtLeast(0L)
        if (residual > 0L) entries += StorageEntry("other", "其他应用数据", residual, reclaimable = false)
        return StorageReport(total, entries.filter { it.bytes > 0L })
    }

    /** Return the new file size, or 0 when the original was left untouched. */
    private fun compactImage(file: File): Long {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        if (runCatching { BitmapFactory.decodeFile(file.absolutePath, bounds) }.isFailure) return 0L
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return 0L
        val maxDimension = maxOf(bounds.outWidth, bounds.outHeight)
        // Small files are already cheaper than the decode/replace work.  The size guard also
        // avoids re-encoding tiny transparent icons where PNG metadata could grow the file.
        if (maxDimension <= IMAGE_MAX_DIMENSION && file.length() <= IMAGE_COMPACT_THRESHOLD) return 0L
        var sample = 1
        while (maxDimension / (sample * 2) >= IMAGE_MAX_DIMENSION) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = runCatching { BitmapFactory.decodeFile(file.absolutePath, options) }.getOrNull()
            ?: return 0L
        val bitmap = if (maxOf(decoded.width, decoded.height) > IMAGE_MAX_DIMENSION) {
            val scale = IMAGE_MAX_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height).toFloat()
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            ).also { if (it !== decoded) decoded.recycle() }
        } else decoded
        val format = if (bitmap.hasAlpha()) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
        val tmp = File(file.parentFile, file.name + ".compact.tmp")
        val encoded = runCatching {
            FileOutputStream(tmp).use { out ->
                if (!bitmap.compress(format, IMAGE_JPEG_QUALITY, out)) return@runCatching false
                runCatching { out.fd.sync() }
                true
            }
        }.getOrDefault(false)
        bitmap.recycle()
        if (!encoded || !tmp.exists()) {
            tmp.delete()
            return 0L
        }
        val compactedBytes = tmp.length()
        // Never replace a file with a larger encoding.  This is common for small transparent
        // PNGs and would make a maintenance pass counterproductive.
        if (compactedBytes <= 0L || compactedBytes >= file.length()) {
            tmp.delete()
            return 0L
        }
        val backup = File(file.parentFile, file.name + ".compact.bak")
        if (backup.exists()) backup.delete()
        val movedOriginal = file.renameTo(backup)
        if (!movedOriginal) {
            tmp.delete()
            return 0L
        }
        // The original was moved out of the way first, so this same-directory rename is
        // atomic on Android's app filesystems and works on API 24 without java.nio desugaring.
        val movedNew = tmp.renameTo(file)
        return if (movedNew) {
            backup.delete()
            compactedBytes
        } else {
            // Restore the original before returning.  A failed maintenance pass must not
            // make an otherwise valid avatar disappear.
            file.delete()
            backup.renameTo(file)
            tmp.delete()
            0L
        }
    }

    /** Mark a WebView as alive before it starts loading a page. */
    fun webViewStarted() { activeWebViews.incrementAndGet() }

    /**
     * Clear and trim WebView-owned cache data after the last WebView is disposed.
     * This does not touch cookies, Web SQL, or local-storage databases.
     */
    fun webViewStopped(context: Context) {
        activeWebViews.updateAndGet { (it - 1).coerceAtLeast(0) }
        if (activeWebViews.get() == 0) schedule(context, force = true)
    }

    private data class TrimResult(
        val deletedFiles: Int,
        val deletedBytes: Long,
        val beforeBytes: Long,
        val afterBytes: Long,
    )

    private fun trimDirectory(root: File, policy: Policy): TrimResult {
        if (!root.exists() || !root.isDirectory) return TrimResult(0, 0L, 0L, 0L)
        val all = runCatching { root.walkTopDown().filter { it.isFile }.toList() }
            .getOrDefault(emptyList())
        val before = all.sumOf { it.length().coerceAtLeast(0L) }
        if (all.isEmpty()) return TrimResult(0, 0L, 0L, 0L)

        val now = System.currentTimeMillis()
        val removable = all.filter { file ->
            val name = file.name.lowercase()
            val temporary = name.endsWith(".tmp") || name.endsWith(".part") ||
                name.endsWith(".download") || file.length() <= 0L
            val expired = file.lastModified() > 0L && now - file.lastModified() > policy.maxAgeMs
            temporary || expired
        }.toMutableSet()

        var bytes = before
        var count = all.size
        val oldestFirst = all.sortedWith(compareBy<File>({ it.lastModified() }, { it.length() }))
        for (file in oldestFirst) {
            if (file in removable || bytes > policy.maxBytes || count > policy.maxFiles) {
                val fileBytes = file.length().coerceAtLeast(0L)
                if (runCatching { file.delete() }.getOrDefault(false)) {
                    removable.add(file)
                    bytes -= fileBytes
                    count--
                }
            }
        }

        // Remove only empty directories that we created under this explicit cache root.
        runCatching {
            root.walkBottomUp().filter { it.isDirectory && it != root && it.listFiles().isNullOrEmpty() }
                .forEach { it.delete() }
        }
        val after = runCatching { root.walkTopDown().filter { it.isFile }.sumOf { it.length().coerceAtLeast(0L) } }
            .getOrDefault(0L)
        return TrimResult(all.size - count, before - after, before, after)
    }

    private fun bytesIn(root: File): Long = runCatching {
        if (!root.exists()) 0L else root.walkTopDown().filter { it.isFile }.sumOf { it.length().coerceAtLeast(0L) }
    }.getOrDefault(0L)

    private fun webViewRoots(app: Context): List<File> = runCatching {
        val data = app.applicationInfo?.dataDir?.let(::File) ?: return@runCatching emptyList()
        data.listFiles()
            .orEmpty()
            .filter { it.isDirectory && (it.name == "app_webview" || it.name.startsWith("app_webview_")) }
    }.getOrDefault(emptyList())

    private fun webViewCacheDirs(root: File): List<File> = runCatching {
        root.walkTopDown().filter { dir ->
            dir.isDirectory && (
                dir.name in webViewCacheNames ||
                    (dir.name == "CacheStorage" && dir.parentFile?.name == "Service Worker")
                )
        }.toList()
    }.getOrDefault(emptyList())

    private fun webViewFiles(app: Context): List<File> = webViewRoots(app).flatMap { root ->
        webViewCacheDirs(root).flatMap { cache ->
            runCatching { cache.walkTopDown().filter { it.isFile }.toList() }
                .getOrDefault(emptyList())
        }
    }.distinctBy { it.absolutePath }

    private fun webViewCacheBytes(app: Context): Long =
        webViewFiles(app).sumOf { it.length().coerceAtLeast(0L) }

    private fun trimWebViewCaches(app: Context): TrimResult {
        val all = webViewFiles(app)
        if (all.isEmpty()) return TrimResult(0, 0L, 0L, 0L)
        val before = all.sumOf { it.length().coerceAtLeast(0L) }
        var bytes = before
        var count = all.size
        val now = System.currentTimeMillis()
        // A WebView cache entry may have no useful extension, so use age and
        // aggregate size/file count rather than the ordinary *.tmp heuristic.
        val oldest = all.sortedWith(compareBy<File>({ it.lastModified() }, { it.length() }))
        for (file in oldest) {
            val expired = file.lastModified() > 0L && now - file.lastModified() > 30L * DAY
            if (!expired && bytes <= WEBVIEW_MAX_BYTES && count <= WEBVIEW_MAX_FILES) continue
            val n = file.length().coerceAtLeast(0L)
            if (runCatching { file.delete() }.getOrDefault(false)) {
                bytes -= n
                count--
            }
        }
        // Empty cache directories are safe to remove; parent app_webview and
        // profile/data directories are intentionally left intact.
        webViewRoots(app).forEach { root ->
            runCatching {
                webViewCacheDirs(root).forEach { cache ->
                    cache.walkBottomUp()
                        .filter { it.isDirectory && it != cache && it.listFiles().isNullOrEmpty() }
                        .forEach { it.delete() }
                    if (cache.listFiles().isNullOrEmpty()) cache.delete()
                }
            }
        }
        val after = webViewCacheBytes(app)
        return TrimResult(all.size - count, before - after, before, after)
    }

    private const val TAG = "CacheMaintenance"
    private const val MiB = 1024L * 1024L
    private const val DAY = 24L * 60L * 60L * 1000L
    private const val SCHEDULE_INTERVAL_MS = 15L * 60L * 1000L
    private const val IMAGE_MAX_DIMENSION = 768
    private const val IMAGE_COMPACT_THRESHOLD = 512L * 1024L
    private const val IMAGE_JPEG_QUALITY = 86
}
