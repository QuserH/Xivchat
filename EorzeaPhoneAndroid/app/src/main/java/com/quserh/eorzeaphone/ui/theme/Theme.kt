package com.quserh.eorzeaphone.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PhoneBackground = Color(0xFF09080E)
val PhoneSurface = Color(0xFF1C1B20)
val PhoneSurfaceRaised = Color(0xFF242329)
val PhoneAccent = Color(0xFF8669F2)
val PhoneText = Color(0xFFF4F1FA)
val PhoneMuted = Color(0xFFA9A5B3)
val PhoneGreen = Color(0xFF27C66A)

private val PhoneColors = darkColorScheme(
    primary = PhoneAccent,
    onPrimary = Color.White,
    background = PhoneBackground,
    onBackground = PhoneText,
    surface = PhoneSurface,
    onSurface = PhoneText,
    surfaceVariant = PhoneSurfaceRaised,
    onSurfaceVariant = PhoneMuted,
)

@Composable
fun EorzeaPhoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PhoneColors, content = content)
}
