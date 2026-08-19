package com.quserh.eorzeaphone

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.quserh.eorzeaphone.ui.EorzeaPhoneApp
import com.quserh.eorzeaphone.ui.theme.EorzeaPhoneTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(9, 8, 14)),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(8, 7, 13)),
        )
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            EorzeaPhoneTheme {
                EorzeaPhoneApp()
            }
        }
    }
}
