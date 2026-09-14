package com.hb.puzz.data.images

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Rect
import com.hb.puzz.BuildConfig
import com.hb.puzz.data.GameSettings
import com.hb.puzz.domain.PuzzleLevel
import com.hb.puzz.ui.images.ImageAssets
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

class PuzzleImageRepository(
    private val context: Context,
    private val settings: GameSettings,
    apiKey: String = BuildConfig.PEXELS_API_KEY
) {
    private val appContext = context.applicationContext
    private val pexelsApi = PexelsApiClient(apiKey)
    private val cacheDirectory = File(appContext.filesDir, "pexels_puzzle_images").apply { mkdirs() }

    fun isPexelsConfigured(): Boolean = pexelsApi.isConfigured()

    suspend fun loadLevelImage(
        level: PuzzleLevel,
        sourceMode: ImageSourceMode,
        forceRefresh: Boolean = false
    ): PuzzleImage = withContext(Dispatchers.IO) {
        if (sourceMode == ImageSourceMode.PRELOADED) {
            return@withContext PuzzleImage(
                bitmap = ImageAssets.loadBitmap(appContext, level.id),
                sourceMode = ImageSourceMode.PRELOADED
            )
        }

        if (!pexelsApi.isConfigured()) {
            return@withContext fallback(
                level,
                "Pexels is selected, but no API key is configured. Using the preloaded image."
            )
        }

        try {
            val savedMeta = if (forceRefresh) null else settings.loadPexelsPhoto(level.id)
            val resolvedMeta = savedMeta ?: run {
                val query = pexelsQueryFor(level)
                val page = if (forceRefresh) Random.nextInt(1, 8) else 1
                val results = pexelsApi.searchSquarePhoto(query = query, page = page, perPage = 15)
                if (results.isEmpty()) {
                    return@withContext fallback(level, "Pexels returned no images for this level. Using the preloaded image.")
                }
                val selectedIndex = if (forceRefresh) Random.nextInt(results.size) else ((level.id - 1) * 7) % results.size
                results[selectedIndex].also { selected ->
                    settings.savePexelsPhoto(level.id, selected)
                }
            }
            val bitmap = loadCachedOrDownload(level.id, resolvedMeta)
            PuzzleImage(
                bitmap = bitmap,
                sourceMode = ImageSourceMode.PEXELS,
                attribution = resolvedMeta
            )
        } catch (error: Exception) {
            fallback(level, "Unable to load Pexels right now. Using the preloaded image.")
        }
    }

    suspend fun refreshLevelImage(level: PuzzleLevel): PuzzleImage = withContext(Dispatchers.IO) {
        settings.clearPexelsPhoto(level.id)
        deleteLevelCache(level.id)
        loadLevelImage(level, ImageSourceMode.PEXELS, forceRefresh = true)
    }

    private fun fallback(level: PuzzleLevel, message: String): PuzzleImage = PuzzleImage(
        bitmap = ImageAssets.loadBitmap(appContext, level.id),
        sourceMode = ImageSourceMode.PRELOADED,
        usedFallback = true,
        message = message
    )

    private fun loadCachedOrDownload(levelId: Int, meta: PexelsPhotoMeta): Bitmap {
        val file = File(cacheDirectory, "level_${levelId}_${meta.id}.jpg")
        if (file.exists()) {
            BitmapFactory.decodeFile(file.absolutePath)?.let { return centerCropSquare(it, 1000) }
            file.delete()
        }

        val connection = (URI(meta.imageUrl).toURL().openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 20_000
            requestMethod = "GET"
            setRequestProperty("User-Agent", "CozyPictureBlocks/1.0")
        }

        val downloaded = try {
            if (connection.responseCode !in 200..299) {
                throw PexelsApiException("Image download failed (${connection.responseCode})")
            }
            BitmapFactory.decodeStream(connection.inputStream)
                ?: throw PexelsApiException("Pexels image could not be decoded")
        } finally {
            connection.disconnect()
        }

        val square = centerCropSquare(downloaded, 1000)
        FileOutputStream(file).use { output ->
            square.compress(Bitmap.CompressFormat.JPEG, 90, output)
        }
        return square
    }

    private fun centerCropSquare(source: Bitmap, size: Int): Bitmap {
        val edge = minOf(source.width, source.height)
        val left = (source.width - edge) / 2
        val top = (source.height - edge) / 2
        val cropped = Bitmap.createBitmap(source, left, top, edge, edge)
        if (cropped.width == size && cropped.height == size) return cropped

        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(output).drawBitmap(
            cropped,
            null,
            Rect(0, 0, size, size),
            null
        )
        if (cropped !== source && !cropped.isRecycled) cropped.recycle()
        if (source !== cropped && !source.isRecycled) source.recycle()
        return output
    }

    private fun deleteLevelCache(levelId: Int) {
        cacheDirectory.listFiles()?.forEach { file ->
            if (file.name.startsWith("level_${levelId}_")) file.delete()
        }
    }

    private fun pexelsQueryFor(level: PuzzleLevel): String = when (level.id) {
        1 -> "sunrise warm landscape"
        2 -> "cozy tea cup"
        3 -> "sleeping cat cozy"
        4 -> "garden cottage flowers"
        5 -> "cozy reading nook"
        6 -> "cozy chair interior"
        7 -> "flower pot garden"
        8 -> "mountain lake landscape"
        9 -> "seaside village"
        10 -> "rainy cafe window"
        11 -> "warm cozy kitchen"
        12 -> "small library books"
        13 -> "window garden plants"
        14 -> "autumn walking path"
        15 -> "cozy balcony plants"
        16 -> "evening city street lights"
        17 -> "forest cabin"
        18 -> "snowy village winter"
        19 -> "moonlit lake night"
        20 -> "cozy home evening"
        else -> level.title
    }
}
