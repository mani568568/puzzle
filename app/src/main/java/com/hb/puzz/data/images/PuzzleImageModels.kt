package com.hb.puzz.data.images

import android.graphics.Bitmap

enum class ImageSourceMode(val storedValue: String) {
    PEXELS("pexels"),
    PRELOADED("preloaded");

    companion object {
        fun fromStored(value: String?): ImageSourceMode =
            entries.firstOrNull { it.storedValue == value } ?: PEXELS
    }
}

data class PexelsPhotoMeta(
    val id: Long,
    val imageUrl: String,
    val photoUrl: String,
    val photographer: String,
    val photographerUrl: String
)

data class PuzzleImage(
    val bitmap: Bitmap,
    val sourceMode: ImageSourceMode,
    val attribution: PexelsPhotoMeta? = null,
    val usedFallback: Boolean = false,
    val message: String? = null
)
