package com.quserh.eorzeaphone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quserh.eorzeaphone.ui.theme.PhoneAccent
import com.quserh.eorzeaphone.ui.theme.PhoneMuted
import com.quserh.eorzeaphone.ui.theme.PhoneSurface
import com.quserh.eorzeaphone.ui.theme.PhoneSurfaceRaised
import com.quserh.eorzeaphone.ui.theme.PhoneText

@Composable
fun AppStoreScreen(state: PhoneState) {
    ScreenFrame {
        ScreenHeader("App Store", state)
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(state.storeApps(), key = { it.id }) { app ->
                val systemApp = app.id == "appstore"
                val installed = state.isAppInstalled(app.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(PhoneSurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(9.dp)).background(app.color),
                        contentAlignment = Alignment.Center,
                    ) {
                        ImageGlyph(app.icon, Color.White, Modifier.fillMaxSize().padding(9.dp))
                    }
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(app.label, color = PhoneText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            if (systemApp) "系统应用，始终保留" else if (installed) "已安装" else "未安装",
                            color = PhoneMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Button(
                        enabled = !systemApp,
                        onClick = {
                            if (installed) state.uninstallApp(app.id) else state.installApp(app.id)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (installed) PhoneSurfaceRaised else PhoneAccent,
                            contentColor = if (installed) PhoneText else Color.White,
                            disabledContainerColor = PhoneSurfaceRaised,
                            disabledContentColor = PhoneMuted,
                        ),
                    ) {
                        Text(if (systemApp) "系统应用" else if (installed) "移除" else "安装", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
