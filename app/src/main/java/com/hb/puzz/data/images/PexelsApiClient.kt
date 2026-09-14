package com.hb.puzz.data.images

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class PexelsApiException(message: String) : Exception(message)

class PexelsApiClient(private val apiKey: String) {
    fun isConfigured(): Boolean = apiKey.isNotBlank()

    fun searchSquarePhoto(query: String, page: Int = 1, perPage: Int = 15): List<PexelsPhotoMeta> {
        if (apiKey.isBlank()) throw PexelsApiException("Pexels API key is not configured")

        val encodedQuery = URLEncoder.encode(query.trim(), StandardCharsets.UTF_8.toString())
        val safePage = page.coerceAtLeast(1)
        val safePerPage = perPage.coerceIn(1, 80)
        val url = URI(
            "https://api.pexels.com/v1/search?query=$encodedQuery&orientation=square&page=$safePage&per_page=$safePerPage"
        ).toURL()

        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Authorization", apiKey)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "CozyPictureBlocks/1.0")
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val body = runCatching {
                    BufferedReader(InputStreamReader(connection.errorStream ?: connection.inputStream)).use { it.readText() }
                }.getOrDefault("")
                throw PexelsApiException("Pexels request failed ($responseCode)${if (body.isBlank()) "" else ": $body"}")
            }

            val body = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            parsePhotos(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parsePhotos(json: String): List<PexelsPhotoMeta> {
        val root = JSONObject(json)
        val photos = root.optJSONArray("photos") ?: return emptyList()
        return buildList {
            for (index in 0 until photos.length()) {
                val photo = photos.optJSONObject(index) ?: continue
                val src = photo.optJSONObject("src") ?: continue
                val imageUrl = sequenceOf("large2x", "large", "medium", "original")
                    .mapNotNull { key -> src.optString(key).takeIf { it.isNotBlank() } }
                    .firstOrNull()
                    ?: continue

                add(
                    PexelsPhotoMeta(
                        id = photo.optLong("id", -1L),
                        imageUrl = imageUrl,
                        photoUrl = photo.optString("url"),
                        photographer = photo.optString("photographer", "Pexels photographer"),
                        photographerUrl = photo.optString("photographer_url")
                    )
                )
            }
        }
    }
}
