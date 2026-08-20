package com.quserh.eorzeaphone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Adjustable global horizontal inset for feature screens (外观 -> 内容边距). */
val LocalContentMargin = staticCompositionLocalOf { 16 }

val PhoneAccent = Color(0xFF1C8EFF)
val PhoneGreen = Color(0xFF27C66A)

val PhoneBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val PhoneSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val PhoneSurfaceRaised: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val PhoneText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val PhoneMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val DarkPhoneColors = darkColorScheme(
    primary = PhoneAccent,
    onPrimary = Color.White,
    background = Color(0xFF09080E),
    onBackground = Color(0xFFF4F1FA),
    surface = Color(0xFF1C1B20),
    onSurface = Color(0xFFF4F1FA),
    surfaceVariant = Color(0xFF242329),
    onSurfaceVariant = Color(0xFFA9A5B3),
)

private val LightPhoneColors = lightColorScheme(
    primary = PhoneAccent,
    onPrimary = Color.White,
    background = Color(0xFFF5F5FA),
    onBackground = Color(0xFF24232A),
    surface = Color.White,
    onSurface = Color(0xFF24232A),
    surfaceVariant = Color(0xFFE8E8ED),
    onSurfaceVariant = Color(0xFF696A76),
)

@Composable
fun EorzeaPhoneTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkPhoneColors else LightPhoneColors, content = content)
}
