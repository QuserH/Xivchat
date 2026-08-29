package com.quserh.eorzeaphone.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * 把 Bitmap 存进系统相册。
 *
 * **公共件**：石之家的图片查看器在用，wiki 的道具图、任务树的图要存也走这里，
 * 别各写一套（写相册这件事在两个 Android 版本上做法完全不同，写两遍必然有一边错）。
 *
 * 两条路：
 * - **API 29+**：MediaStore 插一条记录拿到 uri 直接写。写自己的
 *   `Pictures/<子目录>` 不需要任何权限。写的过程中 `IS_PENDING=1`，
 *   写完再清掉——不这么做的话相册可能扫到一个写了一半的文件。
 * - **API 24~28**：只能往公共 Pictures 目录写文件，需要
 *   WRITE_EXTERNAL_STORAGE（manifest 里那条带 maxSdkVersion=28 的），
 *   写完还要主动通知媒体库扫描，否则相册里看不到。
 *
 * 返回 null 表示成功，否则返回给人看的失败原因（调用方直接 Toast）。
 */
object SaveImage {

    private const val ALBUM = "艾欧泽亚终端"

    /**
     * @param name 文件名（不含扩展名）。会被清掉文件系统不允许的字符。
     * @return null = 成功；非 null = 失败原因
     */
    fun toGallery(context: Context, bitmap: Bitmap, name: String): String? {
        val safe = sanitize(name)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(context, bitmap, safe)
            } else {
                saveLegacy(context, bitmap, safe)
            }
        } catch (e: SecurityException) {
            // API 28 及以下没给存储权限时会走到这儿。
            "没有存储权限，无法保存到相册"
        } catch (e: Exception) {
            e.message?.take(60) ?: "保存失败"
        }
    }

    private fun saveViaMediaStore(context: Context, bitmap: Bitmap, name: String): String? {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
            // 写完之前标成 pending，避免相册扫到半个文件。
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: return "系统不允许写入相册"
        var ok = false
        try {
            resolver.openOutputStream(uri)?.use { out ->
                ok = bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            } ?: return "打不开相册的写入流"
        } finally {
            if (ok) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                // 压缩失败就把那条空记录删掉，不在相册里留一个坏文件。
                runCatching { resolver.delete(uri, null, null) }
            }
        }
        return if (ok) null else "图片编码失败"
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(context: Context, bitmap: Bitmap, name: String): String? {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), ALBUM)
        if (!dir.exists() && !dir.mkdirs()) return "建不了相册目录"
        val file = File(dir, "$name.png")
        FileOutputStream(file).use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) return "图片编码失败"
        }
        // 老系统要主动通知媒体库，否则文件在那儿但相册里看不到。
        runCatching {
            MediaStore.Images.Media.insertImage(context.contentResolver, file.absolutePath, "$name.png", null)
        }
        return null
    }

    /**
     * 文件名清洗。URL 里的路径分隔符、问号这些进文件名会直接失败，
     * 中文和常规字符保留。太长也截掉——有些图片 URL 的文件名部分很长。
     */
    private fun sanitize(raw: String): String {
        val cleaned = raw.trim()
            .replace(Regex("""[\\/:*?"<>|\r\n\t]"""), "_")
            .replace(Regex("_{2,}"), "_")
            .trim('_', '.')
        val base = cleaned.ifBlank { "image" }.take(60)
        // 加时间戳：同一张图存两次不该互相覆盖，也不该失败。
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.CHINA)
            .format(java.util.Date())
        return "${base}_$stamp"
    }

    /** 从 URL 猜一个文件名。取最后一段去掉查询串，没有就用 "image"。 */
    fun nameFromUrl(url: String): String {
        val path = url.substringBefore('?').substringBefore('#')
        val last = path.substringAfterLast('/')
        return last.substringBeforeLast('.').ifBlank { "image" }
    }
}
