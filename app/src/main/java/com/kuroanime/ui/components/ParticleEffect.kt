package com.kuroanime.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.sin
import kotlin.random.Random

private data class Particle(
    val startX: Float,
    val startY: Float,
    val sizeDp: Float,
    val speed: Float,
    val alpha: Float,
    val color: Color,
    val phase: Float
)

@Composable
fun ParticleBackground(modifier: Modifier = Modifier, isVisible: Boolean = true) {
    if (!isVisible) return

    val density = LocalDensity.current.density

    val particles = remember {
        val palette = listOf(
            Color(0xFFEF5350),
            Color(0xFFFF8A65),
            Color(0xFFFFB74D)
        )
        List(18) {
            Particle(
                startX = Random.nextFloat(),
                startY = Random.nextFloat() * 1.1f,
                sizeDp = Random.nextFloat() * 3f + 1.5f,
                speed = Random.nextFloat() * 0.35f + 0.12f,
                alpha = Random.nextFloat() * 0.10f + 0.03f,
                color = palette[Random.nextInt(palette.size)],
                phase = Random.nextFloat() * 6.28f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        particles.forEach { p ->
            val raw = (p.startY + time * p.speed) / 1.1f
            val y = raw % 1f
            val fadeIn = (raw / 0.15f).coerceIn(0f, 1f)
            val fadeOut = (1f - ((raw - 0.85f) / 0.15f)).coerceIn(0f, 1f)
            val fade = fadeIn * fadeOut
            val xOsc = sin(raw * 10f + p.phase) * w * 0.015f
            drawCircle(
                color = p.color.copy(alpha = p.alpha * fade),
                radius = p.sizeDp * density,
                center = Offset(
                    (p.startX * w + xOsc).coerceIn(0f, w),
                    y * h
                )
            )
        }
    }
}
