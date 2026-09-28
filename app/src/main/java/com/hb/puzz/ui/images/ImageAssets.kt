package com.hb.puzz.ui.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlin.math.roundToInt

/**
 * Memory-efficient loader for drawable-nodpi/level_001.jpeg ... level_999.jpeg.
 *
 * Design goals:
 *  - resolve resources lazily instead of maintaining a 999-entry R.drawable table;
 *  - decode only near the pixel size the current screen needs;
 *  - keep a byte-bounded LRU rather than retaining every level bitmap;
 *  - keep thumbnail and gameplay resolutions in independent cache buckets;
 *  - never perform disk/resource decoding on the main thread (callers use Dispatchers.IO).
 */
object ImageAssets {
    private const val MIN_DECODE_EDGE = 320
    private const val MAX_DECODE_EDGE = 2048

    private val resourceIds = HashMap<Int, Int>(64)
    private val resourceLock = Any()

    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val runtimeLimit = Runtime.getRuntime().maxMemory()
        val cacheBytes = minOf(runtimeLimit / 8L, 96L * 1024L * 1024L)
            .coerceAtLeast(16L * 1024L * 1024L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
        object : LruCache<String, Bitmap>(cacheBytes) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
        }
    }

    fun resourceName(levelId: Int): String =
        "level_${levelId.coerceIn(1, 999).toString().padStart(3, '0')}"

    /**
     * Android resource names must be lower-case. Files named Level_001.jpeg must be renamed to
     * level_001.jpeg before AAPT runs. See app/normalize_level_artwork.cmd.
     */
    @Suppress("DiscouragedApi")
    fun getLevelImage(context: Context, levelId: Int): Int {
        require(levelId in 1..999) { "Level id must be between 1 and 999." }
        synchronized(resourceLock) {
            resourceIds[levelId]?.let { return it }
        }

        val name = resourceName(levelId)
        val id = context.resources.getIdentifier(name, "drawable", context.packageName)
        require(id != 0) {
            "Missing drawable-nodpi/$name.jpeg. Android resource filenames must use lower-case names."
        }
        synchronized(resourceLock) { resourceIds[levelId] = id }
        return id
    }

    fun recommendedGameplayEdgePx(context: Context): Int {
        val width = context.resources.displayMetrics.widthPixels
        return (width * 1.5f).roundToInt().coerceIn(768, MAX_DECODE_EDGE)
    }

    fun loadBitmap(
        context: Context,
        levelId: Int,
        targetEdgePx: Int = recommendedGameplayEdgePx(context)
    ): Bitmap {
        val bucket = decodeBucket(targetEdgePx)
        val cacheKey = "$levelId@$bucket"
        memoryCache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { return it }

        val resId = getLevelImage(context.applicationContext, levelId)
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            inScaled = false
        }
        BitmapFactory.decodeResource(context.resources, resId, options)
        check(options.outWidth > 0 && options.outHeight > 0) {
            "Could not read image bounds for ${resourceName(levelId)}.jpeg"
        }

        val sample = calculateInSampleSize(options.outWidth, options.outHeight, bucket)
        val decoded = BitmapFactory.decodeResource(
            context.resources,
            resId,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inDither = false
            }
        ) ?: error("Could not decode ${resourceName(levelId)}.jpeg")

        // The puzzle renderer expects a square source. Avoid a copy when the artwork is already square.
        val square = if (decoded.width == decoded.height) {
            decoded
        } else {
            val edge = minOf(decoded.width, decoded.height)
            val left = (decoded.width - edge) / 2
            val top = (decoded.height - edge) / 2
            Bitmap.createBitmap(decoded, left, top, edge, edge).also {
                if (it !== decoded && !decoded.isRecycled) decoded.recycle()
            }
        }

        memoryCache.put(cacheKey, square)
        return square
    }

    fun evictLevel(levelId: Int) {
        synchronized(memoryCache) {
            listOf(320, 768, 1280, 2048).forEach { memoryCache.remove("$levelId@$it") }
        }
    }

    fun clearMemoryCache() = memoryCache.evictAll()

    private fun decodeBucket(targetEdgePx: Int): Int = when {
        targetEdgePx <= 320 -> 320
        targetEdgePx <= 768 -> 768
        targetEdgePx <= 1280 -> 1280
        else -> 2048
    }

    private fun calculateInSampleSize(width: Int, height: Int, targetEdge: Int): Int {
        var sample = 1
        val shortest = minOf(width, height)
        while (shortest / (sample * 2) >= targetEdge) sample *= 2
        return sample
    }
}
