package com.quserh.eorzeaphone.craft.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Apple-style tokens, aligned with the EorzeaPhone design system so the module
 * merges cleanly later: same type scale rules (WWDC 2020), same gold accent pair
 * (石之家金 #c4a86a fill + dark-gold ink), same neutral ramp.
 */

object CraftType {
    val Display = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, lineHeight = 38.sp)
    val Title = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, lineHeight = 27.sp)
    val Header = TextStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp, lineHeight = 24.sp)
    val Headline = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp, lineHeight = 21.sp)
    val Row = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp, lineHeight = 20.sp)
    val Body = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.sp, lineHeight = 21.sp)
    val Callout = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.1.sp, lineHeight = 17.sp)
    val Caption = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp, lineHeight = 15.sp)
    val Micro = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp, lineHeight = 13.sp)
    val SectionLabel = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.5.sp, lineHeight = 16.sp)
}

private val AccentFill = Color(0xFFC4A86A)
private val AccentInkLight = Color(0xFF7D6229)
private val AccentInkDark = Color(0xFFD8BE85)

val CraftAccent: Color @Composable get() =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) AccentInkLight else AccentInkDark

val CraftFill = AccentFill

private val DangerLight = Color(0xFFC2323D)
private val DangerDark = Color(0xFFFF8A93)
private val OkLight = Color(0xFF2F6B40)
private val OkDark = Color(0xFF7FC49A)

val CraftDanger: Color @Composable get() =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) DangerLight else DangerDark

val CraftOk: Color @Composable get() =
    if (MaterialTheme.colorScheme.background.luminance() > 0.5f) OkLight else OkDark

val CraftBackground: Color @Composable get() = MaterialTheme.colorScheme.background
val CraftSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
val CraftText: Color @Composable get() = MaterialTheme.colorScheme.onBackground
val CraftMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
val CraftLine: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
val CraftHq = Color(0xFF9A6B1F)

private fun lightColors() = lightColorScheme(
    primary = AccentInkLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF5EDDB),
    onPrimaryContainer = AccentInkLight,
    error = DangerLight,
    background = Color(0xFFF2F3F7),
    onBackground = Color(0xFF1E242C),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E242C),
    surfaceVariant = Color(0xFFEBEDF2),
    onSurfaceVariant = Color(0xFF5C6570),
    outline = Color(0xFF87929C),
    outlineVariant = Color(0xFFE3E6EC),
)

private fun darkColors() = darkColorScheme(
    primary = AccentInkDark,
    onPrimary = Color(0xFF241E10),
    primaryContainer = Color(0xFF33301F),
    onPrimaryContainer = AccentInkDark,
    error = DangerDark,
    background = Color(0xFF101216),
    onBackground = Color(0xFFE3E6E3),
    surface = Color(0xFF181B20),
    onSurface = Color(0xFFE3E6E3),
    surfaceVariant = Color(0xFF23262C),
    onSurfaceVariant = Color(0xFF969FA9),
    outline = Color(0xFF6E7C72),
    outlineVariant = Color(0xFF262B30),
)

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Grouped-list card radius used across the app (iOS inset group style). */
val CardShape = RoundedCornerShape(12.dp)

@Composable
fun CraftTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) darkColors() else lightColors(),
        shapes = Shapes,
        content = content,
    )
}
