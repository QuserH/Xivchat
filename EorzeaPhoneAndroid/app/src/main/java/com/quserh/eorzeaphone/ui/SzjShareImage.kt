package com.quserh.eorzeaphone.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.core.content.FileProvider
import com.quserh.eorzeaphone.data.CacheMaintenance
import java.io.File
import java.io.FileOutputStream

/**
 * 把一段 Compose 内容渲成图片再分享出去。
 *
 * 石之家移动端的"分享"就是这么做的：它把详情页里那个
 * `#glamour__preview___main` 节点交给 html2canvas（scale 1.75、
 * `backgroundColor: null`、`useCORS`），拿到 PNG 的 dataURL 之后弹图片预览让人保存。
 * 也就是说**没有服务端接口**，是把已经排好版的那一块就地栅格化。
 *
 * Compose 这边的对应物是 GraphicsLayer：`rememberGraphicsLayer()` 录一层，
 * `toImageBitmap()` 拿位图。所以做法一致——渲的是**专门排的分享卡**，
 * 不是整屏截图，这样输出才是"设计过的图"而不是"截了个屏"。
 */
object SzjShareImage {

    /** 分享用的临时文件目录（和 res/xml/share_paths.xml 里的 path 对应）。 */
    private fun shareDir(context: Context): File =
        File(context.cacheDir, "share").apply { mkdirs() }

    /**
     * 录好的图层存成 PNG 并拉起系统分享。
     *
     * @param layer 已经 record 过的图层。没录过会抛，调用方保证先画过一帧。
     * @param name 文件名（不含扩展名）。
     * @param text 附带的文字。有些目标（微信朋友圈之类）只认图，文字会被丢掉，
     *   所以图里本身要有信息，不能指望这行字。
     */
    suspend fun shareLayer(
        context: Context,
        layer: GraphicsLayer,
        name: String,
        text: String = "",
    ): Result<Unit> = runCatching {
        val bitmap = layer.toImageBitmap().asAndroidBitmap()
        share(context, bitmap, name, text).getOrThrow()
    }

    /** 位图存盘 + 拉起分享。同步部分很短（一次 PNG 编码），由调用方放到 IO 线程。 */
    fun share(
        context: Context,
        bitmap: Bitmap,
        name: String,
        text: String = "",
    ): Result<Unit> = runCatching {
        // 每次先清一遍旧文件：这些图只在分享那一下有用，留着白占空间。
        shareDir(context).listFiles()?.forEach { runCatching { it.delete() } }
        val safe = name.replace(Regex("[^\\w\\u4e00-\\u9fa5-]"), "_").take(40).ifBlank { "share" }
        val file = File(shareDir(context), "$safe.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        CacheMaintenance.schedule(context)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (text.isNotBlank()) putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, "分享").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        })
    }
}
