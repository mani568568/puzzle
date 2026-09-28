package com.hb.puzz.ui.images

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Loads only a small sampled bitmap for lists/history instead of decoding a full-size JPEG. */
@Composable
fun LevelArtworkThumbnail(
    levelId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    alpha: Float = 1f
) {
    val appContext = LocalContext.current.applicationContext
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, levelId) {
        value = withContext(Dispatchers.IO) {
            runCatching { ImageAssets.loadBitmap(appContext, levelId, targetEdgePx = 320) }.getOrNull()
        }
    }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(listOf(Color(0xFFEAF8FF), Color(0xFFF6EEFF), Color(0xFFFFF3D8)))
        ),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null && !loaded.isRecycled) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = alpha
            )
        } else {
            CircularProgressIndicator()
        }
    }
}
