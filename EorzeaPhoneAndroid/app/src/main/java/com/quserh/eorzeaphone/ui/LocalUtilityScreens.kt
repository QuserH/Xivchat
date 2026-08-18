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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    FeatureFrame("地图", state) {
        if (profile == null) EmptyFeature("连接游戏后显示当前位置") else Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(8.dp)).background(Color(0xFF344F48)), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("⌖", color = Color.White, fontSize = 44.sp); Text(profile.location, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold); Text("Territory ${profile.territoryId}", color = Color.White.copy(alpha = .65f), modifier = Modifier.padding(top = 5.dp)) }
            }
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(PhoneSurface).padding(16.dp)) { Text("当前世界", color = PhoneMuted, modifier = Modifier.weight(1f)); Text(profile.currentWorld, color = PhoneText) }
        }
    }
}

@Composable
fun HealthScreen(state: PhoneState) {
    val online = state.friends.count { it.online }
    FeatureFrame("健康", state) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFF47732E)).padding(20.dp)) { Text("游戏会话", color = Color.White.copy(alpha = .75f)); Text(if (state.connected) "状态良好" else "当前未连接", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold) }
            HealthRow("连接状态", if (state.connected) "已连接" else "离线")
            HealthRow("在线好友", "$online 人")
            HealthRow("消息缓存", "${state.chats.size} 条")
            HealthRow("数据模块", listOf(state.inventory.isNotEmpty(), state.wallet != null, state.weather != null, state.jobs.isNotEmpty()).count { it }.let { "$it / 4 正常" })
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
