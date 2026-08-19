package com.quserh.eorzeaphone.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.sin

internal enum class PhoneWeatherKind { Clear, Clouds, Fog, Rain, Thunder, Wind, Sand, Heat, Snow, Gloom }

internal data class PhoneWeatherVisual(val top: Color, val bottom: Color, val glow: Color, val ink: Color, val kind: PhoneWeatherKind)

internal fun phoneWeatherVisual(name: String, bell: Int): PhoneWeatherVisual {
    val lower = name.lowercase()
    val kind = when {
        "雷" in name || "thunder" in lower -> PhoneWeatherKind.Thunder
        "雪" in name || "冰" in name || "snow" in lower || "blizzard" in lower -> PhoneWeatherKind.Snow
        "沙" in name || "尘" in name || "dust" in lower || "sand" in lower -> PhoneWeatherKind.Sand
        "热" in name || "灼" in name || "heat" in lower -> PhoneWeatherKind.Heat
        "雨" in name || "rain" in lower || "shower" in lower -> PhoneWeatherKind.Rain
        "雾" in name || "fog" in lower -> PhoneWeatherKind.Fog
        "风" in name || "wind" in lower || "gale" in lower -> PhoneWeatherKind.Wind
        "妖" in name || "灵" in name || "gloom" in lower || "umbral" in lower -> PhoneWeatherKind.Gloom
        "晴" in name || "碧" in name || "clear" in lower || "fair" in lower -> PhoneWeatherKind.Clear
        else -> PhoneWeatherKind.Clouds
    }
    val day = bell in 6..18
    val colors = when (kind) {
        PhoneWeatherKind.Clear -> if (day) Triple(Color(0xFF1760C4), Color(0xFF70B3EC), Color(0xFFFFD96A)) else Triple(Color(0xFF09102E), Color(0xFF192C59), Color(0xFFC7D6FF))
        PhoneWeatherKind.Clouds -> if (day) Triple(Color(0xFF607586), Color(0xFFA2AFB9), Color(0xFFF1F5F8)) else Triple(Color(0xFF171C29), Color(0xFF303A49), Color(0xFF8793A6))
        PhoneWeatherKind.Fog -> Triple(Color(0xFF747A82), Color(0xFFB1B5B9), Color(0xFFF2F4F5))
        PhoneWeatherKind.Rain -> Triple(Color(0xFF25384D), Color(0xFF536A80), Color(0xFF87B5E8))
        PhoneWeatherKind.Thunder -> Triple(Color(0xFF242337), Color(0xFF534D65), Color(0xFFFFE16D))
        PhoneWeatherKind.Wind -> Triple(Color(0xFF34666A), Color(0xFF79A7A4), Color(0xFFD7F5F0))
        PhoneWeatherKind.Sand -> Triple(Color(0xFF795C32), Color(0xFFC69A5D), Color(0xFFFFD98A))
        PhoneWeatherKind.Heat -> Triple(Color(0xFF963F24), Color(0xFFE17C3B), Color(0xFFFFCE65))
        PhoneWeatherKind.Snow -> Triple(Color(0xFF738BA8), Color(0xFFCAD8E7), Color.White)
        PhoneWeatherKind.Gloom -> Triple(Color(0xFF2D243B), Color(0xFF554366), Color(0xFFC8A5E8))
    }
    val ink = if (day && kind in setOf(PhoneWeatherKind.Clear, PhoneWeatherKind.Clouds, PhoneWeatherKind.Fog, PhoneWeatherKind.Snow, PhoneWeatherKind.Wind)) Color(0xFF162235) else Color.White
    return PhoneWeatherVisual(colors.first, colors.second, colors.third, ink, kind)
}

@Composable
internal fun WeatherBackdrop(name: String, bell: Int, modifier: Modifier = Modifier) {
    val visual = phoneWeatherVisual(name, bell)
    val transition = rememberInfiniteTransition(label = "weather-ambience")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Restart), label = "weather-phase")
    Box(modifier.background(Brush.verticalGradient(listOf(visual.top, visual.bottom)))) {
        Canvas(Modifier.fillMaxSize()) {
            when (visual.kind) {
                PhoneWeatherKind.Rain, PhoneWeatherKind.Thunder -> repeat(22) { index ->
                    val x = ((index * 47f + phase * 180f) % (size.width + 60f)) - 30f
                    val y = ((index * 83f + phase * size.height * 1.7f) % (size.height + 80f)) - 40f
                    drawLine(visual.glow.copy(alpha = .45f), Offset(x, y), Offset(x - 9f, y + 29f), 2.2f, StrokeCap.Round)
                }
                PhoneWeatherKind.Snow -> repeat(26) { index ->
                    val x = ((index * 61f + sin(phase * 6.28f + index) * 20f) % size.width + size.width) % size.width
                    val y = (index * 71f + phase * size.height) % size.height
                    drawCircle(Color.White.copy(alpha = .62f), 2f + index % 3, Offset(x, y))
                }
                PhoneWeatherKind.Wind, PhoneWeatherKind.Fog -> repeat(6) { index ->
                    val y = size.height * (.18f + index * .14f)
                    val x = ((phase * size.width * 1.2f + index * 77f) % (size.width + 160f)) - 160f
                    drawLine(visual.glow.copy(alpha = .22f), Offset(x, y), Offset(x + 150f, y), 4f, StrokeCap.Round)
                }
                PhoneWeatherKind.Sand, PhoneWeatherKind.Heat -> repeat(18) { index ->
                    val x = (index * 53f + phase * size.width) % size.width
                    val y = (index * 41f + sin(phase * 6.28f + index) * 25f + size.height) % size.height
                    drawCircle(visual.glow.copy(alpha = .28f), 2.5f, Offset(x, y))
                }
                else -> {
                    drawCircle(visual.glow.copy(alpha = .32f), size.minDimension * .15f, Offset(size.width * .78f, size.height * .23f))
                    if (visual.kind == PhoneWeatherKind.Clear && bell !in 6..18) repeat(18) { index ->
                        drawCircle(Color.White.copy(alpha = .45f + (index % 3) * .15f), 1.4f, Offset((index * 79f) % size.width, (index * 43f) % (size.height * .7f)))
                    }
                }
            }
        }
    }
}
