package com.quserh.eorzeaphone

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import java.io.File
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.quserh.eorzeaphone.ui.EorzeaPhoneApp
import com.quserh.eorzeaphone.data.KeepAliveService

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, t ->
            try {
                val dir = File(filesDir, "crash")
                dir.mkdirs()
                val sw = java.io.StringWriter()
                t.printStackTrace(java.io.PrintWriter(sw))
                val crashName = "crash-" + System.currentTimeMillis() + ".txt"
                File(dir, crashName).writeText(t.javaClass.name + ": " + t.message + "\n" + sw)
            } catch (_: Throwable) {
            }
            defaultHandler?.uncaughtException(thread, t)
        }
        KeepAliveService.start(this)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.Transparent.value.toInt()),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(8, 7, 13)),
        )
        // Fill the whole window (including the transparent status-bar strip) with the
        // phone background so the content color extends up onto the system status bar
        // instead of a black bar.
        window.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent { EorzeaPhoneApp() }
    }
}
