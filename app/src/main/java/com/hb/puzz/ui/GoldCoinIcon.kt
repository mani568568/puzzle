package com.hb.puzz.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A minted gold coin with a star stamp, deliberately free of currency symbols. */
@Composable
internal fun GoldCoinIcon(modifier: Modifier = Modifier) {
    Canvas(modifier.size(24.dp)) {
        val radius = size.minDimension * 0.44f
        drawCircle(Color(0xFF80500C), radius, center + Offset(0f, radius * 0.10f))
        drawCircle(Brush.linearGradient(listOf(Color(0xFFFFF1A6), Color(0xFFF7C34A), Color(0xFFD38A17))), radius)
        drawCircle(Color(0xFFB97A14), radius * 0.77f, style = Stroke(radius * 0.085f))
        drawArc(Color(0xFFFFF5CD), 208f, 112f, false,
            topLeft = center - Offset(radius * 0.9f, radius * 0.9f),
            size = androidx.compose.ui.geometry.Size(radius * 1.8f, radius * 1.8f),
            style = Stroke(radius * 0.07f))
        val star = Path()
        repeat(10) { index ->
            val angle = -PI / 2 + index * PI / 5
            val r = radius * if (index % 2 == 0) 0.49f else 0.23f
            val x = center.x + cos(angle).toFloat() * r
            val y = center.y + sin(angle).toFloat() * r
            if (index == 0) star.moveTo(x, y) else star.lineTo(x, y)
        }
        star.close()
        drawPath(star, Color(0xFFAD7010))
    }
}
