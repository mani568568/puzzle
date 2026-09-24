package com.hb.puzz.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.material3.LocalContentColor

val VibrantOrangeBrush = Brush.linearGradient(
    listOf(Color(0xFFFF7A18), Color(0xFFFFB52E), Color(0xFFFF4F8B))
)

val VibrantBlueBrush = Brush.linearGradient(
    listOf(Color(0xFF39C6FF), Color(0xFF5B7CFA), Color(0xFF9B5CF6))
)

val VibrantMintBrush = Brush.linearGradient(
    listOf(Color(0xFF2DD4BF), Color(0xFF22C55E), Color(0xFFFFD43B))
)

val VibrantPinkBrush = Brush.linearGradient(
    listOf(Color(0xFFFF5C8A), Color(0xFFFF8A4C), Color(0xFFFFD43B))
)

enum class ActionMotion {
    SHRINK,
    BOUNCE_UP,
    DEPTH_PRESS,
    TILT,
    PULSE,
    SHAKE
}

@Composable
fun VibrantAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(22.dp),
    brush: Brush = VibrantOrangeBrush,
    contentColor: Color = Color.White,
    contentAlignment: Alignment = Alignment.Center,
    motion: ActionMotion = ActionMotion.SHRINK,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    val targetScale = if (!pressed) 1f else when (motion) {
        ActionMotion.SHRINK -> 0.94f
        ActionMotion.BOUNCE_UP -> 0.97f
        ActionMotion.DEPTH_PRESS -> 0.95f
        ActionMotion.TILT -> 0.95f
        ActionMotion.PULSE -> 0.91f
        ActionMotion.SHAKE -> 0.96f
    }
    val targetRotation = if (!pressed) 0f else when (motion) {
        ActionMotion.TILT -> -2.4f
        ActionMotion.SHAKE -> 3.1f
        else -> 0f
    }
    val targetY = if (!pressed) 0f else when (motion) {
        ActionMotion.BOUNCE_UP -> -4.2f
        ActionMotion.DEPTH_PRESS -> 2.8f
        else -> 0f
    }
    val targetX = if (!pressed) 0f else when (motion) {
        ActionMotion.SHAKE -> 2.8f
        else -> 0f
    }

    val scale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = spring(dampingRatio = 0.48f, stiffness = 520f),
        label = "vibrant-scale"
    )
    val rotation by animateFloatAsState(
        targetValue = targetRotation,
        animationSpec = spring(dampingRatio = 0.40f, stiffness = 620f),
        label = "vibrant-rotation"
    )
    val translateY by animateFloatAsState(
        targetValue = targetY,
        animationSpec = spring(dampingRatio = 0.46f, stiffness = 560f),
        label = "vibrant-y"
    )
    val translateX by animateFloatAsState(
        targetValue = targetX,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = 760f),
        label = "vibrant-x"
    )
    val glow by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(160),
        label = "vibrant-glow"
    )
    val rippleProgress by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = tween(if (pressed) 240 else 180, easing = FastOutSlowInEasing),
        label = "vibrant-ripple"
    )
    val infinite = rememberInfiniteTransition(label = "vibrant-highlight")
    val sweep by infinite.animateFloat(
        initialValue = -0.55f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "highlight-sweep"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationZ = rotation
                translationY = translateY.dp.toPx()
                translationX = translateX.dp.toPx()
                shadowElevation = when {
                    pressed && motion == ActionMotion.DEPTH_PRESS -> 2.dp.toPx()
                    else -> 7.dp.toPx() + 7.dp.toPx() * glow
                }
                this.shape = shape
                clip = false
                alpha = if (enabled) 1f else 0.52f
            }
            .clip(shape)
            .background(brush)
            .border(1.1.dp, Color.White.copy(alpha = 0.18f), shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = contentAlignment
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f + glow * 0.09f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
            )
            val startX = size.width * sweep
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.18f + glow * 0.12f),
                        Color.Transparent
                    ),
                    start = Offset(startX - size.width * 0.35f, 0f),
                    end = Offset(startX, size.height)
                ),
                topLeft = Offset.Zero,
                size = Size(size.width, size.height)
            )
            if (rippleProgress > 0.001f) {
                val rippleRadius = size.maxDimension * 0.72f * rippleProgress
                val rippleAlpha = (0.20f * (1f - rippleProgress * 0.35f)).coerceAtLeast(0f)
                drawCircle(
                    color = Color.White.copy(alpha = rippleAlpha),
                    radius = rippleRadius,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
            if (pressed && motion == ActionMotion.DEPTH_PRESS) {
                drawRoundRect(
                    color = Color.Black.copy(alpha = 0.10f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
            }
        }

        CompositionLocalProvider(LocalContentColor provides contentColor) {
            content()
        }

        val showParticles = motion == ActionMotion.PULSE || motion == ActionMotion.BOUNCE_UP
        if (pressed && showParticles) {
            Canvas(Modifier.fillMaxSize()) {
                val points = listOf(
                    0.12f to 0.18f,
                    0.87f to 0.20f,
                    0.19f to 0.78f,
                    0.82f to 0.74f,
                    0.50f to 0.10f,
                    0.52f to 0.88f
                )
                points.forEachIndexed { index, pair ->
                    val radius = (2.0f + (index % 3)) * density
                    val center = Offset(size.width * pair.first, size.height * pair.second)
                    val sparkle = if (index % 2 == 0) Color.White else Color(0xFFFFF176)
                    drawCircle(sparkle.copy(alpha = 0.90f), radius, center)
                    drawCircle(sparkle.copy(alpha = 0.22f), radius * 2.6f, center)
                    rotate(index * 19f, center) {
                        drawLine(
                            sparkle.copy(alpha = 0.88f),
                            Offset(center.x - radius * 2f, center.y),
                            Offset(center.x + radius * 2f, center.y),
                            strokeWidth = density
                        )
                        drawLine(
                            sparkle.copy(alpha = 0.88f),
                            Offset(center.x, center.y - radius * 2f),
                            Offset(center.x, center.y + radius * 2f),
                            strokeWidth = density
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VibrantCircleAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    brush: Brush = VibrantBlueBrush,
    motion: ActionMotion = ActionMotion.SHRINK,
    content: @Composable BoxScope.() -> Unit
) = VibrantAction(
    onClick = onClick,
    modifier = modifier,
    shape = CircleShape,
    brush = brush,
    motion = motion,
    content = content
)
