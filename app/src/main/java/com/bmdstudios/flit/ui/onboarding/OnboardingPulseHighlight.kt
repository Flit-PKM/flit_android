package com.bmdstudios.flit.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Applies a subtle pulsing highlight used by onboarding coachmarks.
 */
fun Modifier.onboardingPulseHighlight(
    enabled: Boolean,
    shape: Shape,
    color: Color,
    minScale: Float = 1f,
    maxScale: Float = 1.05f,
    minBorderWidth: Dp = 2.dp,
    maxBorderWidth: Dp = 3.dp,
    minBackgroundAlpha: Float = 0.10f,
    maxBackgroundAlpha: Float = 0.24f
): Modifier = composed {
    if (!enabled) return@composed this

    val transition = rememberInfiniteTransition(label = "onboardingPulse")
    val scale by transition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val borderWidth by transition.animateFloat(
        initialValue = minBorderWidth.value,
        targetValue = maxBorderWidth.value,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBorderWidth"
    )
    val backgroundAlpha by transition.animateFloat(
        initialValue = minBackgroundAlpha,
        targetValue = maxBackgroundAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseBackgroundAlpha"
    )

    this
        .scale(scale)
        .background(color = color.copy(alpha = backgroundAlpha), shape = shape)
        .border(width = borderWidth.dp, color = color, shape = shape)
}
