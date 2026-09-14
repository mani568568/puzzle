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

/**
 * Image asset loader for puzzle illustrations.
 * 
 * To add your own artwork:
 * 1. Place PNG/JPG images in app/src/main/res/drawable/
 * 2. Add them to the LEVEL_IMAGES map below with appropriate keys
 * 3. Ensure each image is a square divisible by grid size (3, 4, or 5)
 */
object ImageAssets {
    
    // Placeholder level IDs - replace these with your actual image resource IDs
    private val LEVEL_IMAGES = mutableMapOf<Int, Int>().apply {
        put(1, R.drawable.level_1)
        put(2, R.drawable.level_2)
        put(3, R.drawable.level_3)
        put(4, R.drawable.level_4)
        put(5, R.drawable.level_5)
        put(6, R.drawable.level_6)
        put(7, R.drawable.level_7)
        put(8, R.drawable.level_8)
        put(9, R.drawable.level_9)
        put(10, R.drawable.level_10)
        put(11, R.drawable.level_11)
        put(12, R.drawable.level_12)
        put(13, R.drawable.level_13)
        put(14, R.drawable.level_14)
        put(15, R.drawable.level_15)
        put(16, R.drawable.level_16)
        put(17, R.drawable.level_17)
        put(18, R.drawable.level_18)
        put(19, R.drawable.level_19)
        put(20, R.drawable.level_20)
    }
    
    /**
     * Get image resource ID for a level.
     * Returns default placeholder if not found.
     */
    fun getLevelImage(levelId: Int): Int {
        return LEVEL_IMAGES[levelId] ?: R.drawable.level_default
    }
    
    /**
     * Load bitmap from resources (call from background thread).
     */
    fun loadBitmap(context: Context, levelId: Int): Bitmap? {
        val resourceId = getLevelImage(levelId)
        try {
            return ContextCompat.getBitmap(context.resources, resourceId)
        } catch (e: Exception) {
            // Return placeholder if resource not found
            return DefaultLevelArtwork.generatePlaceholderBitmap(400, 400)
        }
    }
    
    /**
     * Check if an image exists for a level.
     */
    fun hasImage(levelId: Int): Boolean {
        return LEVEL_IMAGES.containsKey(levelId)
    }
    
    /**
     * Get all available level IDs with images.
     */
    fun getAvailableLevels(): List<Int> {
        return LEVEL_IMAGES.keys.filter { it <= 20 }.sorted()
    }
}

/**
 * Default placeholder image - replace this with actual artwork
 */
object DefaultLevelArtwork {
    
    // Generates a simple gradient pattern as default
    fun generatePlaceholderBitmap(width: Int, height: Int): Bitmap {
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            
            // Create gradient background
            val paint = Paint()
            
            for (y in 0 until height step (height / 4)) {
                val color1 = Color.HSVToColor(floatArrayOf(y.toFloat() / height * 360, 0.5f, 0.9f))
                val color2 = Color.HSVToColor(floatArrayOf((y + height/4).toFloat() / height * 360, 0.5f, 0.8f))
                
                paint.shader = LinearGradient(
                    0f, y.toFloat(),
                    width.toFloat(), (y + height/4).toFloat(),
                    color1, color2,
                    Shader.TileMode.CLAMP
                )
                
                canvas.drawRect(0f, y.toFloat(), width.toFloat(), (y + height/4).toFloat(), paint)
            }
            
            // Add some shapes
            paint.shader = null
            paint.color = Color.WHITE
            paint.alpha = 50
            
            val cellSize = width / 3
            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    if ((row + col) % 2 == 0) {
                        canvas.drawCircle(
                            (col * cellSize + cellSize/2).toFloat(),
                            (row * cellSize + cellSize/2).toFloat(),
                            (cellSize/3).toFloat(),
                            paint
                        )
                    }
                }
            }
        }
    }
}
