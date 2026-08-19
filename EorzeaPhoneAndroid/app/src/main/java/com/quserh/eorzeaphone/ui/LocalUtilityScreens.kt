package com.quserh.eorzeaphone.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneText
import java.io.File
import java.io.FileOutputStream

@Composable
fun CameraScreen(state: PhoneState) {
    val context = LocalContext.current
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var status by remember { mutableStateOf("") }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        preview = bitmap
        status = if (bitmap == null) "已取消拍摄" else "照片尚未保存"
    }
    FeatureFrame("相机", state) {
        Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)).background(Color.Black), contentAlignment = Alignment.Center) {
                preview?.let { Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
                    ?: Text("取景器", color = Color.White.copy(alpha = .55f), fontSize = 18.sp)
            }
            if (status.isNotBlank()) Text(status, color = PhoneMuted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { launcher.launch(null) }, colors = ButtonDefaults.buttonColors(containerColor = PhoneAccent)) { Text(if (preview == null) "拍照" else "重拍") }
                if (preview != null) Button(onClick = {
                    val dir = File(context.filesDir, "photos").apply { mkdirs() }
                    val file = File(dir, "eorzea-${System.currentTimeMillis()}.jpg")
                    FileOutputStream(file).use { preview?.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                    status = "已保存到艾欧泽亚终端相册"
                }) { Text("保存") }
            }
        }
    }
}

private data class LocalPhoto(val file: File, val bitmap: Bitmap)

@Composable
fun PhotosScreen(state: PhoneState) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf<LocalPhoto?>(null) }
    val photos = remember {
        File(context.filesDir, "photos").listFiles().orEmpty().sortedByDescending { it.lastModified() }.mapNotNull { file ->
            BitmapFactory.decodeFile(file.absolutePath)?.let { LocalPhoto(file, it) }
        }
    }
    FeatureFrame(if (selected == null) "照片" else "照片详情", state) {
        if (selected != null) {
            Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Image(selected!!.bitmap.asImageBitmap(), null, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)), contentScale = ContentScale.Fit)
                TextButtonAction("返回相册") { selected = null }
            }
        } else if (photos.isEmpty()) {
            EmptyFeature("还没有照片\n使用相机拍摄并保存后会显示在这里")
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(photos, key = { it.file.name }) { photo ->
                    Row(Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(8.dp)).background(PhoneSurface).clickable { selected = photo }, verticalAlignment = Alignment.CenterVertically) {
                        Image(photo.bitmap.asImageBitmap(), null, Modifier.size(92.dp), contentScale = ContentScale.Crop)
                        Column(Modifier.weight(1f).padding(14.dp)) {
                            Text("艾欧泽亚照片", color = PhoneText, fontWeight = FontWeight.SemiBold)
                            Text(photo.file.nameWithoutExtension, color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Text("›", color = PhoneMuted, fontSize = 28.sp, modifier = Modifier.padding(end = 14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ShortcutsScreen(state: PhoneState) {
    var adding by remember { mutableStateOf(false) }
    var nameInput by remember { mutableStateOf("") }
    var commandInput by remember { mutableStateOf("") }
    FeatureFrame("快捷指令", state, trailing = { Text("＋", color = PhoneAccent, fontSize = 24.sp, modifier = Modifier.clickable { nameInput = ""; commandInput = ""; adding = true }.padding(horizontal = 8.dp)) }) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.customShortcuts, key = { it.command }) { shortcut ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).clickable(enabled = state.connected) { state.sendChat(shortcut.command); state.statusMessage = "已发送：${shortcut.name}" }.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(40.dp).clip(RoundedCornerShape(7.dp)).background(PhoneAccent), contentAlignment = Alignment.Center) { Text("⌁", color = Color.White, fontSize = 20.sp) }
                    Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(shortcut.name, color = PhoneText); Text(shortcut.command, color = PhoneMuted, fontSize = 11.sp) }
                    if (state.customShortcuts.count { it.command == shortcut.command } > 1 || defaultShortcuts.none { it.command == shortcut.command }) {
                        Text("✕", color = Color(0xFFE56B6F), fontSize = 16.sp, modifier = Modifier.clickable { state.removeShortcut(shortcut.command) }.padding(6.dp))
                    } else {
                        Text("›", color = PhoneMuted, fontSize = 24.sp)
                    }
                }
            }
            if (!state.connected) item { Text("连接游戏后可执行快捷指令", color = PhoneMuted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(14.dp)) }
            if (state.customShortcuts.size > defaultShortcuts.size) {
                item { Text("重置为默认", color = PhoneAccent, modifier = Modifier.clip(RoundedCornerShape(7.dp)).clickable { state.resetShortcuts() }.padding(horizontal = 10.dp, vertical = 8.dp)) }
            }
        }
    }
    if (adding) {
        AlertDialog(
            onDismissRequest = { adding = false },
            title = { Text("新增快捷指令") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(nameInput, { nameInput = it }, label = { Text("名称") }, singleLine = true)
                    OutlinedTextField(commandInput, { commandInput = it }, label = { Text("命令（如 /return）") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = { state.addShortcut(nameInput, commandInput); adding = false }) { Text("添加", color = PhoneAccent) }
            },
            dismissButton = { TextButton(onClick = { adding = false }) { Text("取消") } },
        )
    }
}

@Composable
fun FishingScreen(state: PhoneState) {
    val weather = state.weather
    FeatureFrame("捕鱼", state) {
        if (weather == null) EmptyFeature(if (state.connected) "等待区域与天气数据…" else "连接游戏后显示钓场环境") else
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF286476)).padding(18.dp)) {
                        Text(weather.zone, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("当前天气：${weather.current}", color = Color.White.copy(alpha = .8f), modifier = Modifier.padding(top = 5.dp))
                    }
                }
                item { Text("天气窗口", color = PhoneMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp)) }
                items(weather.forecast) { window ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(14.dp)) {
                        Text(window.name, color = PhoneText, modifier = Modifier.weight(1f))
                        Text(if (window.minutesFromNow <= 0) "现在" else "${window.minutesFromNow} 分后", color = PhoneAccent)
                    }
                }
                item { Text("鱼类图鉴和咬钩记录需要游戏内捕获事件，后续快照到达后会出现在此页。", color = PhoneMuted, fontSize = 11.sp, modifier = Modifier.padding(8.dp)) }
            }
    }
}

@Composable
fun MapsScreen(state: PhoneState) {
    val profile = state.profile
    val maps = state.maps
    var query by remember { mutableStateOf("") }
    var expandedExpansion by remember { mutableStateOf<String?>(null) }
    var expandedRegion by remember { mutableStateOf<String?>(null) }
    val purple = Color(0xFF6651BE)
    val ink = Color(0xFF25252B)
    val muted = Color(0xFF6E6E77)
    val normalizedQuery = query.trim()
    val expansions = maps?.expansions.orEmpty().mapNotNull { expansion ->
        if (normalizedQuery.isBlank()) expansion else {
            val regions = expansion.regions.mapNotNull { region ->
                val destinations = region.destinations.filter { it.name.contains(normalizedQuery, true) }
                when {
                    expansion.name.contains(normalizedQuery, true) || region.name.contains(normalizedQuery, true) -> region
                    destinations.isNotEmpty() -> region.copy(destinations = destinations)
                    else -> null
                }
            }
            if (expansion.name.contains(normalizedQuery, true)) expansion else expansion.copy(regions = regions).takeIf { it.regions.isNotEmpty() }
        }
    }
    val favorites = maps?.expansions.orEmpty().flatMap { it.regions }.flatMap { it.destinations }.filter { state.isMapFavorite(it.rowId) }
    ScreenFrame(background = Color(0xFFF5F5FA)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 21.dp)) {
            Text("‹", color = purple, fontSize = 40.sp, modifier = Modifier.clickable { state.back() }.padding(end = 12.dp))
            Text("地图", color = ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Box(Modifier.size(40.dp))
        }
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = ink, fontSize = 14.sp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 43.dp).height(44.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFE7E7EC)),
            decorationBox = { field ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxSize().padding(horizontal = 13.dp)) {
                    Text("⌕", color = muted, fontSize = 21.sp, modifier = Modifier.padding(end = 10.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("搜索地点", color = muted, fontSize = 14.sp)
                        field()
                    }
                }
            },
        )
        LazyColumn(Modifier.fillMaxSize().padding(top = 22.dp)) {
            item("current-label") { Text("当前位置", color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 42.dp, vertical = 8.dp)) }
            item("current") {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp).clip(RoundedCornerShape(10.dp)).background(Color.White).padding(horizontal = 24.dp, vertical = 17.dp)) {
                    Text("●", color = purple, fontSize = 24.sp)
                    Column(Modifier.padding(start = 16.dp)) {
                        Text(maps?.currentZone?.ifBlank { profile?.location.orEmpty() }?.ifBlank { "尚未取得位置" } ?: "尚未连接游戏", color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(maps?.currentRegion?.ifBlank { profile?.currentWorld.orEmpty() }.orEmpty(), color = muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            if (normalizedQuery.isBlank() && favorites.isNotEmpty()) {
                item("favorites-label") { Text("收藏", color = muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 42.dp, vertical = 12.dp)) }
                items(favorites, key = { "favorite-${it.rowId}" }) { destination ->
                    MapDestinationRow(destination.name, true, purple, ink, muted) { state.toggleMapFavorite(destination.rowId) }
                }
            }
            if (maps == null) {
                item("empty") { Text(if (state.connected) "正在从游戏读取地图资料…" else "连接游戏后读取地图资料", color = muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(32.dp)) }
            }
            expansions.forEach { expansion ->
                item("expansion-${expansion.order}-${expansion.name}") {
                    val expanded = normalizedQuery.isNotBlank() || expandedExpansion == expansion.name
                    Column(Modifier.fillMaxWidth().clickable {
                        expandedExpansion = if (expandedExpansion == expansion.name) null else expansion.name
                        expandedRegion = null
                    }.padding(horizontal = 42.dp, vertical = 17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (expanded) "⌄" else "›", color = ink, fontSize = 28.sp, modifier = Modifier.width(28.dp))
                            Text(expansion.name, color = ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                val showExpansion = normalizedQuery.isNotBlank() || expandedExpansion == expansion.name
                if (showExpansion) expansion.regions.forEach { region ->
                    val regionKey = "${expansion.order}:${region.name}"
                    item("region-$regionKey") {
                        val expanded = normalizedQuery.isNotBlank() || expandedRegion == regionKey
                        Row(Modifier.fillMaxWidth().clickable { expandedRegion = if (expandedRegion == regionKey) null else regionKey }.padding(start = 66.dp, end = 42.dp, top = 11.dp, bottom = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (expanded) "⌄" else "›", color = muted, fontSize = 24.sp, modifier = Modifier.width(26.dp))
                            Text(region.name, color = ink, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text("${region.destinations.size}", color = muted, fontSize = 11.sp)
                        }
                    }
                    if (normalizedQuery.isNotBlank() || expandedRegion == regionKey) {
                        items(region.destinations, key = { "destination-${it.rowId}" }) { destination ->
                            MapDestinationRow(destination.name, state.isMapFavorite(destination.rowId), purple, ink, muted) { state.toggleMapFavorite(destination.rowId) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MapDestinationRow(name: String, favorite: Boolean, accent: Color, ink: Color, muted: Color, onFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 42.dp, vertical = 2.dp).clip(RoundedCornerShape(8.dp)).background(Color.White).padding(start = 48.dp, end = 12.dp, top = 12.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("●", color = accent, fontSize = 12.sp, modifier = Modifier.padding(end = 12.dp))
        Text(name, color = ink, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(if (favorite) "★" else "☆", color = if (favorite) accent else muted, fontSize = 20.sp, modifier = Modifier.clickable { onFavorite() }.padding(4.dp))
    }
}

@Composable
fun HealthScreen(state: PhoneState) {
    val online = state.friends.count { it.online }
    val p = state.profile
    FeatureFrame("健康", state) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF47732E)).padding(20.dp)) {
                Text("游戏会话", color = Color.White.copy(alpha = .75f))
                Text(if (state.connected) "状态良好" else "当前未连接", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                if (p != null) {
                    Text("${p.jobName.ifBlank { "冒险者" }} · Lv.${p.level}", color = Color.White.copy(alpha = .85f), fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                    if (p.itemLevel > 0) Text("平均品级 ${p.itemLevel}", color = Color.White.copy(alpha = .75f), fontSize = 13.sp)
                }
            }
            if (p != null && p.maxHp > 0) {
                HealthBar("HP", p.currentHp, p.maxHp, Color(0xFF3CB371))
                if (p.maxMp > 0) HealthBar("MP", p.currentMp, p.maxMp, Color(0xFF4F8DE8))
                if (p.maxCp > 0) HealthRow("制作力", "${p.currentCp} / ${p.maxCp}")
                if (p.maxGp > 0) HealthRow("采集力", "${p.currentGp} / ${p.maxGp}")
            } else {
                HealthRow("连接状态", if (state.connected) "已连接" else "离线")
            }
            HealthRow("在线好友", "$online 人")
            HealthRow("消息缓存", "${state.chats.size} 条")
            HealthRow("数据模块", listOf(state.inventory.isNotEmpty(), state.wallet != null, state.weather != null, state.jobs.isNotEmpty()).count { it }.let { "$it / 4 正常" })
        }
    }
}

@Composable
private fun HealthBar(label: String, current: Int, max: Int, color: Color) {
    val frac = if (max > 0) (current.toFloat() / max.toFloat()).coerceIn(0f, 1f) else 0f
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth()) { Text(label, color = PhoneMuted, modifier = Modifier.weight(1f)); Text("$current / $max", color = PhoneText, fontWeight = FontWeight.SemiBold) }
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.White.copy(alpha = .1f))) {
            Box(Modifier.fillMaxWidth(frac).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

@Composable
private fun HealthRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(16.dp)) { Text(label, color = PhoneMuted, modifier = Modifier.weight(1f)); Text(value, color = PhoneText, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun TextButtonAction(label: String, action: () -> Unit) {
    Text(label, color = PhoneAccent, modifier = Modifier.clip(RoundedCornerShape(7.dp)).clickable(onClick = action).padding(horizontal = 18.dp, vertical = 10.dp))
}
