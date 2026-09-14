package com.hb.puzz.ui.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.core.content.ContextCompat
import com.hb.puzz.R

/** Loads level artwork and converts vector or bitmap drawables to a square bitmap. */
object ImageAssets {
    private val LEVEL_IMAGES = mapOf(
        1 to R.drawable.level_1,
        2 to R.drawable.level_2,
        3 to R.drawable.level_3,
        4 to R.drawable.level_4,
        5 to R.drawable.level_5,
        6 to R.drawable.level_6,
        7 to R.drawable.level_7,
        8 to R.drawable.level_8,
        9 to R.drawable.level_9,
        10 to R.drawable.level_10,
        11 to R.drawable.level_11,
        12 to R.drawable.level_12,
        13 to R.drawable.level_13,
        14 to R.drawable.level_14,
        15 to R.drawable.level_15,
        16 to R.drawable.level_16,
        17 to R.drawable.level_17,
        18 to R.drawable.level_18,
        19 to R.drawable.level_19,
        20 to R.drawable.level_20
    )

    fun getLevelImage(levelId: Int): Int = LEVEL_IMAGES[levelId] ?: R.drawable.level_default

    /** Works for both vector XML drawables and raster resources. */
    fun loadBitmap(context: Context, levelId: Int, sizePx: Int = 800): Bitmap {
        val drawable = ContextCompat.getDrawable(context, getLevelImage(levelId))
            ?: return DefaultLevelArtwork.generatePlaceholderBitmap(sizePx, sizePx)

        return runCatching {
            Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).also { bitmap ->
                val canvas = Canvas(bitmap)
                drawable.setBounds(0, 0, sizePx, sizePx)
                drawable.draw(canvas)
            }
        }.getOrElse {
            DefaultLevelArtwork.generatePlaceholderBitmap(sizePx, sizePx)
        }
    }

    fun hasImage(levelId: Int): Boolean = levelId in LEVEL_IMAGES

    fun getAvailableLevels(): List<Int> = LEVEL_IMAGES.keys.sorted()
}

object DefaultLevelArtwork {
    fun generatePlaceholderBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val bandHeight = (height / 4).coerceAtLeast(1)

            for (y in 0 until height step bandHeight) {
                val bottom = (y + bandHeight).coerceAtMost(height)
                val color1 = Color.HSVToColor(floatArrayOf(y.toFloat() / height * 360f, 0.35f, 0.95f))
                val color2 = Color.HSVToColor(floatArrayOf(bottom.toFloat() / height * 360f, 0.35f, 0.85f))
                paint.shader = LinearGradient(
                    0f,
                    y.toFloat(),
                    width.toFloat(),
                    bottom.toFloat(),
                    color1,
                    color2,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(0f, y.toFloat(), width.toFloat(), bottom.toFloat(), paint)
            }

            paint.shader = null
            paint.color = Color.WHITE
            paint.alpha = 55
            val cellSize = (width / 3).coerceAtLeast(1)
            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    if ((row + col) % 2 == 0) {
                        canvas.drawCircle(
                            (col * cellSize + cellSize / 2).toFloat(),
                            (row * cellSize + cellSize / 2).toFloat(),
                            (cellSize / 3).toFloat(),
                            paint
                        )
                    }
                }
            }
        }
    }
}
