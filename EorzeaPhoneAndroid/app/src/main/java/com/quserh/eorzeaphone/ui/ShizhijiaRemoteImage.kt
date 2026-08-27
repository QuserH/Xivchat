package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.quserh.eorzeaphone.data.shizhijia.ShizhijiaImageLoader
import android.graphics.BitmapFactory

/** Decode an inline `data:image/...;base64,....` URI into a bitmap. */
internal fun decodeDataUri(uri: String): android.graphics.Bitmap? = try {
    val b64 = uri.substringAfter(',')
    val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
} catch (_: Exception) {
    null
}

/**
 * Compose widget that asynchronously renders a remote 石之家 image (avatar,
 * cover, inline picture). Behavior knobs:
 *
 *  - showPlaceholder=false renders a *transparent* tile while loading.
 *  - collapseOnFail removes the tile entirely once the load settles with no
 *    bitmap, so a failed image never leaves an empty frame in the layout.
 *  - fitByAspect renders the bitmap with its own aspect ratio (full image,
 *    not cropped) - used for comment/inline pictures that must not stretch.
 *  - onClick lets callers jump to a full-screen viewer.
 */
@Composable
fun ShizhijiaRemoteImage(
    url: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    // 与石之家板岩体系的 SzjCardRaised 同值。
    placeholderColor: Color = if (MaterialTheme.colorScheme.background.luminance() > 0.5f) Color(0xFFE3E8ED) else Color(0xFF232932),
    showPlaceholder: Boolean = true,
    fitByAspect: Boolean = false,
    collapseOnFail: Boolean = false,
    onClick: ((String) -> Unit)? = null,
) {
    if (url.isBlank()) return
    val context = LocalContext.current
    var bitmap by remember(url) { mutableStateOf(if (url.startsWith("data:image")) decodeDataUri(url) else ShizhijiaImageLoader.peek(url)) }
    var loaded by remember(url) { mutableStateOf(bitmap != null) }
    LaunchedEffect(url) {
        if (!url.startsWith("data:image")) { bitmap = ShizhijiaImageLoader.load(context, url); loaded = true }
    }
    val clickMod = if (onClick != null) modifier.then(Modifier.clickable { onClick(url) }) else modifier

    val bmp = bitmap
    when {
        bmp != null -> Image(bmp.asImageBitmap(), contentDescription = null, contentScale = contentScale, modifier = clickMod.clip(RoundedCornerShape(10.dp)))
        // Load settled with no bitmap: drop the tile so a failed picture never
        // leaves a blank frame (unless a grey placeholder is explicitly wanted).
        loaded && collapseOnFail -> Unit
        showPlaceholder -> Box(clickMod.clip(RoundedCornerShape(10.dp)).background(placeholderColor))
        fitByAspect -> Box(clickMod.heightIn(min = 80.dp).clip(RoundedCornerShape(10.dp)))
        else -> Box(clickMod.clip(RoundedCornerShape(10.dp)))
    }
}
