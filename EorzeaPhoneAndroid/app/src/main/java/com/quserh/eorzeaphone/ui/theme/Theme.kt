package com.quserh.eorzeaphone.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Adjustable global horizontal inset for feature screens (外观 -> 内容边距). */
val LocalContentMargin = staticCompositionLocalOf { 16 }

// Material 3 baseline palette. The whole app resolves its surfaces, text and
// controls through these roles, so switching the scheme re-themes everything at once.
private val M3Light = Color(0xFF6750A4)
private val M3OnLight = Color.White
private val M3ContainerLight = Color(0xFFEADDFF)
private val M3OnContainerLight = Color(0xFF21005D)
private val M3Dark = Color(0xFFD0BCFF)
private val M3OnDark = Color(0xFF381E72)
private val M3ContainerDark = Color(0xFF4F378B)
private val M3OnContainerDark = Color(0xFFEADDFF)

// Accent must be usable inside Canvas DrawScope (non-composable), so keep it a constant.
val PhoneAccent = M3Light
val PhoneAccentContainer = M3ContainerLight
val PhoneOnAccentContainer = M3OnContainerLight
val PhoneGreen = Color(0xFF27C66A)

val PhoneBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val PhoneSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val PhoneSurfaceRaised: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val PhoneText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val PhoneMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val PhoneOutline = Color(0xFF79747E)

private val LightPhoneColors = lightColorScheme(
    primary = M3Light,
    onPrimary = M3OnLight,
    primaryContainer = M3ContainerLight,
    onPrimaryContainer = M3OnContainerLight,
    secondary = Color(0xFF625B71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFFDF7FF),
    onBackground = Color(0xFF1D1B20),
    surface = Color(0xFFFDF7FF),
    onSurface = Color(0xFF1D1B20),
    surfaceVariant = Color(0xFFE7E0EC),
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF79747E),
    outlineVariant = Color(0xFFCAC4D0),
    scrim = Color(0xFF000000),
)

private val DarkPhoneColors = darkColorScheme(
    primary = M3Dark,
    onPrimary = M3OnDark,
    primaryContainer = M3ContainerDark,
    onPrimaryContainer = M3OnContainerDark,
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = Color(0xFFF2B8B5),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF141218),
    onBackground = Color(0xFFE6E0E9),
    surface = Color(0xFF141218),
    onSurface = Color(0xFFE6E0E9),
    surfaceVariant = Color(0xFF49454F),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF938F99),
    outlineVariant = Color(0xFF49454F),
    scrim = Color(0xFF000000),
)

private val Md3Shapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun EorzeaPhoneTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkPhoneColors else LightPhoneColors,
        typography = Typography(),
        shapes = Md3Shapes,
        content = content,
    )
}
