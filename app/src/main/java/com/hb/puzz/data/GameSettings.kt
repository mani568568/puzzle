package com.hb.puzz.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.hb.puzz.data.images.ImageSourceMode
import com.hb.puzz.data.images.PexelsPhotoMeta
import com.hb.puzz.domain.CoinReward
import com.hb.puzz.domain.CoinRewards
import com.hb.puzz.domain.PuzzleLevel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject
import java.util.UUID

private val Context.picturePuzzleDataStore by preferencesDataStore(name = "cozy_blocks_settings")
enum class PuzzleClockMode { COUNTDOWN, STOPWATCH }

data class PuzzleSession(
    val levelId: Int,
    val gridSize: Int,
    val positions: IntArray,
    val moveCount: Int,
    val elapsedMillis: Long = 0,
    val sessionId: String = UUID.randomUUID().toString(),
    val imageSource: ImageSourceMode? = null,
    val started: Boolean = false,
    // Old saves never measured elapsed play reliably; they receive completion/move rewards only.
    val speedEligible: Boolean = true,
    val imageIdentity: String? = null
)

/**
 * A compact lifetime record for one Adventure. It is intentionally stored separately from the
 * current puzzle session so the Journey Journal can survive normal navigation and app restarts.
 */
data class AdventureHistoryEntry(
    val levelId: Int,
    val completions: Int = 0,
    val bestMoves: Int = 0,
    val bestElapsedMillis: Long = 0,
    val totalMoves: Int = 0,
    val totalElapsedMillis: Long = 0,
    val coinsEarned: Int = 0,
    val bestReward: Int = 0,
    val firstCompletedAt: Long = 0,
    val lastCompletedAt: Long = 0,
    val lastGridSize: Int = 0
)

data class HomeSnapshot(
    val highest: Int,
    val completed: Set<Int>,
    val saved: PuzzleSession?,
    val coins: Int,
    val sound: Boolean,
    val haptics: Boolean,
    val dark: Boolean,
    val source: ImageSourceMode,
    val history: List<AdventureHistoryEntry>
)

data class CompletionReceipt(val reward: CoinReward, val awarded: Int, val balance: Int)

class GameSettings(context: Context) {
    private val store = context.applicationContext.picturePuzzleDataStore
    private val sessionKey = stringPreferencesKey("session_v2")
    private val completedKey = stringSetPreferencesKey("completed_levels")
    private val highestKey = intPreferencesKey("highest_level")
    private val coinsKey = intPreferencesKey("coin_balance_v1")
    private val soundKey = booleanPreferencesKey("sound_enabled")
    private val hapticsKey = booleanPreferencesKey("haptics_enabled")
    private val darkKey = booleanPreferencesKey("dark_theme")
    private val sourceKey = stringPreferencesKey("image_source")

    private fun historyKey(levelId: Int) = stringPreferencesKey("adventure_history_$levelId")
    private fun bestCoinsKey(levelId: Int) = intPreferencesKey("best_coins_$levelId")

    val homeFlow = store.data.map { p ->
        val completed = (p[completedKey] ?: emptySet()).mapNotNull(String::toIntOrNull).toSet()
        HomeSnapshot(
            highest = (p[highestKey] ?: 1).coerceIn(1, PuzzleLevel.maxLevelId),
            completed = completed,
            saved = decodeSession(p),
            coins = p[coinsKey] ?: 0,
            sound = p[soundKey] ?: true,
            haptics = p[hapticsKey] ?: true,
            dark = p[darkKey] ?: false,
            source = p[sourceKey]?.let(ImageSourceMode::fromStored) ?: ImageSourceMode.PRELOADED,
            history = decodeHistory(p, completed)
        )
    }
    val completedLevelsFlow = store.data.map { (it[completedKey] ?: emptySet()).mapNotNull(String::toIntOrNull).toSet() }
    val highestLevelFlow = store.data.map { (it[highestKey] ?: 1).coerceIn(1, PuzzleLevel.maxLevelId) }
    val coinBalanceFlow = store.data.map { it[coinsKey] ?: 0 }
    val soundEnabledFlow = store.data.map { it[soundKey] ?: true }
    val hapticsEnabledFlow = store.data.map { it[hapticsKey] ?: true }
    val darkThemeFlow = store.data.map { it[darkKey] ?: false }
    val imageSourceFlow = store.data.map {
        it[sourceKey]?.let(ImageSourceMode::fromStored) ?: ImageSourceMode.PRELOADED
    }
    val savedSessionFlow = store.data.map(::decodeSession)
    suspend fun loadSession() = decodeSession(store.data.first())

    private fun decodeSession(p: Preferences): PuzzleSession? = runCatching {
        val json = p[sessionKey]?.let(::JSONObject)
        val id = json?.getInt("level") ?: p[intPreferencesKey("saved_level")] ?: return null
        val level = PuzzleLevel.getLevel(id) ?: return null
        val raw = json?.getString("positions") ?: p[stringPreferencesKey("saved_positions")] ?: return null
        val positions = raw.split(',').map(String::toInt).toIntArray()
        val grid = json?.getInt("grid") ?: p[intPreferencesKey("saved_grid_size")]
            ?: kotlin.math.sqrt(positions.size.toDouble()).toInt()
        if (!level.acceptsGridSize(grid) || positions.size != grid * grid ||
            positions.toSet().size != positions.size || positions.any { it !in positions.indices }) return null
        PuzzleSession(id, grid, positions,
            (json?.optInt("moves") ?: p[intPreferencesKey("saved_moves")] ?: 0).coerceAtLeast(0),
            json?.optLong("elapsed")?.coerceAtLeast(0) ?: 0,
            json?.getString("id") ?: UUID.randomUUID().toString(),
            json?.optString("source")?.takeIf { it.isNotBlank() }?.let(ImageSourceMode::fromStored),
            json?.optBoolean("started") ?: false,
            json?.optBoolean("speedEligible", true) ?: false,
            json?.optString("imageIdentity")?.takeIf { it.isNotBlank() })
    }.getOrNull()

    private fun decodeHistory(p: Preferences, completed: Set<Int>): List<AdventureHistoryEntry> =
        PuzzleLevel.ALL_LEVELS.mapNotNull { level ->
            val stored = p[historyKey(level.id)]?.let { raw ->
                runCatching { decodeHistoryEntry(level.id, JSONObject(raw)) }.getOrNull()
            }
            // Players who completed Adventures before the Journal was introduced still see them in
            // history. Time/move detail begins being tracked on their next completion.
            stored ?: if (level.id in completed) {
                AdventureHistoryEntry(
                    levelId = level.id,
                    completions = 1,
                    bestReward = p[bestCoinsKey(level.id)] ?: 0
                )
            } else null
        }

    private fun decodeHistoryEntry(levelId: Int, json: JSONObject) = AdventureHistoryEntry(
        levelId = levelId,
        completions = json.optInt("completions", 0).coerceAtLeast(0),
        bestMoves = json.optInt("bestMoves", 0).coerceAtLeast(0),
        bestElapsedMillis = json.optLong("bestElapsed", 0).coerceAtLeast(0),
        totalMoves = json.optInt("totalMoves", 0).coerceAtLeast(0),
        totalElapsedMillis = json.optLong("totalElapsed", 0).coerceAtLeast(0),
        coinsEarned = json.optInt("coinsEarned", 0).coerceAtLeast(0),
        bestReward = json.optInt("bestReward", 0).coerceAtLeast(0),
        firstCompletedAt = json.optLong("firstCompletedAt", 0).coerceAtLeast(0),
        lastCompletedAt = json.optLong("lastCompletedAt", 0).coerceAtLeast(0),
        lastGridSize = json.optInt("lastGridSize", 0).coerceAtLeast(0)
    )

    private fun encodeHistoryEntry(entry: AdventureHistoryEntry) = JSONObject().apply {
        put("completions", entry.completions)
        put("bestMoves", entry.bestMoves)
        put("bestElapsed", entry.bestElapsedMillis)
        put("totalMoves", entry.totalMoves)
        put("totalElapsed", entry.totalElapsedMillis)
        put("coinsEarned", entry.coinsEarned)
        put("bestReward", entry.bestReward)
        put("firstCompletedAt", entry.firstCompletedAt)
        put("lastCompletedAt", entry.lastCompletedAt)
        put("lastGridSize", entry.lastGridSize)
    }.toString()

    suspend fun saveSession(s: PuzzleSession) {
        require(PuzzleLevel.requireLevel(s.levelId).acceptsGridSize(s.gridSize))
        require(s.positions.size == s.gridSize * s.gridSize && s.positions.toSet().size == s.positions.size)
        require(s.positions.all { it in s.positions.indices })
        store.edit { p -> p[sessionKey] = encode(s); removeLegacySession(p) }
    }
    private fun encode(s: PuzzleSession) = JSONObject().apply {
        put("level", s.levelId); put("grid", s.gridSize); put("positions", s.positions.joinToString(","))
        put("moves", s.moveCount); put("elapsed", s.elapsedMillis); put("id", s.sessionId)
        put("source", s.imageSource?.storedValue ?: ""); put("started", s.started)
        put("speedEligible", s.speedEligible); put("imageIdentity", s.imageIdentity ?: "")
    }.toString()

    private fun removeLegacySession(p: MutablePreferences) {
        p.asMap().keys.filter { it.name.startsWith("saved_") }.forEach { p.remove(it) }
    }
    suspend fun clearSession() { store.edit { it.remove(sessionKey); removeLegacySession(it) } }

    /** Wallet, Adventure progress, history, best reward, receipt and session clearing commit together. */
    suspend fun completeSession(s: PuzzleSession): CompletionReceipt {
        require(s.positions.indices.all { s.positions[it] == it })
        val calculated = CoinRewards.calculate(s.gridSize, s.elapsedMillis, s.moveCount)
        val reward = if (s.speedEligible) calculated else calculated.copy(speed = 0)
        var receipt: CompletionReceipt? = null
        store.edit { p ->
            val receiptKey = stringPreferencesKey("coin_receipt_${s.levelId}")
            val previous = p[receiptKey]?.let(::JSONObject)
            if (previous?.optString("session") == s.sessionId) {
                receipt = CompletionReceipt(reward, previous.getInt("awarded"), p[coinsKey] ?: 0)
                if (decodeSession(p)?.sessionId == s.sessionId) p.remove(sessionKey)
                return@edit
            }

            val bestKey = bestCoinsKey(s.levelId)
            val award = CoinRewards.improvement(p[bestKey] ?: 0, reward)
            val balance = (p[coinsKey] ?: 0) + award
            p[bestKey] = maxOf(p[bestKey] ?: 0, reward.total)
            p[coinsKey] = balance
            p[receiptKey] = JSONObject().put("session", s.sessionId).put("awarded", award).toString()
            p[completedKey] = (p[completedKey] ?: emptySet()) + s.levelId.toString()
            p[highestKey] = maxOf(p[highestKey] ?: 1, (s.levelId + 1).coerceAtMost(PuzzleLevel.maxLevelId))

            val now = System.currentTimeMillis()
            val oldHistory = p[historyKey(s.levelId)]?.let { raw ->
                runCatching { decodeHistoryEntry(s.levelId, JSONObject(raw)) }.getOrNull()
            } ?: AdventureHistoryEntry(levelId = s.levelId)
            val updatedHistory = oldHistory.copy(
                completions = oldHistory.completions + 1,
                bestMoves = when {
                    s.moveCount <= 0 -> oldHistory.bestMoves
                    oldHistory.bestMoves <= 0 -> s.moveCount
                    else -> minOf(oldHistory.bestMoves, s.moveCount)
                },
                bestElapsedMillis = when {
                    s.elapsedMillis <= 0 -> oldHistory.bestElapsedMillis
                    oldHistory.bestElapsedMillis <= 0 -> s.elapsedMillis
                    else -> minOf(oldHistory.bestElapsedMillis, s.elapsedMillis)
                },
                totalMoves = oldHistory.totalMoves + s.moveCount.coerceAtLeast(0),
                totalElapsedMillis = oldHistory.totalElapsedMillis + s.elapsedMillis.coerceAtLeast(0),
                coinsEarned = oldHistory.coinsEarned + award.coerceAtLeast(0),
                bestReward = maxOf(oldHistory.bestReward, reward.total),
                firstCompletedAt = oldHistory.firstCompletedAt.takeIf { it > 0 } ?: now,
                lastCompletedAt = now,
                lastGridSize = s.gridSize
            )
            p[historyKey(s.levelId)] = encodeHistoryEntry(updatedHistory)

            // Never clear an unrelated newer session.
            if (decodeSession(p)?.sessionId == s.sessionId) p.remove(sessionKey)
            removeLegacySession(p)
            receipt = CompletionReceipt(reward, award, balance)
        }
        return checkNotNull(receipt)
    }

    suspend fun updateSoundEnabled(v: Boolean) { store.edit { it[soundKey] = v } }
    suspend fun updateHapticsEnabled(v: Boolean) { store.edit { it[hapticsKey] = v } }
    suspend fun updateDarkTheme(v: Boolean) { store.edit { it[darkKey] = v } }
    suspend fun updateImageSource(v: ImageSourceMode) { store.edit { it[sourceKey] = v.storedValue } }
    suspend fun resetProgress() {
        store.edit { p ->
            p.remove(completedKey)
            p.remove(highestKey)
            p.remove(sessionKey)
            p.asMap().keys.filter { it.name.startsWith("adventure_history_") }.forEach { p.remove(it) }
            removeLegacySession(p)
        }
    }
    // Wallet and personal bests intentionally survive journey reset, preventing duplicate rewards.
    suspend fun loadPexelsPhoto(levelId: Int): PexelsPhotoMeta? {
        val p = store.data.first()
        fun value(suffix: String) = p[stringPreferencesKey("pexels_${levelId}_$suffix")]
        return PexelsPhotoMeta(value("id")?.toLongOrNull() ?: return null,
            value("image_url") ?: return null, value("photo_url") ?: return null,
            value("photographer") ?: "Pexels photographer", value("photographer_url") ?: "")
    }
    suspend fun savePexelsPhoto(levelId: Int, photo: PexelsPhotoMeta) {
        store.edit { p ->
            mapOf("id" to photo.id.toString(), "image_url" to photo.imageUrl, "photo_url" to photo.photoUrl,
                "photographer" to photo.photographer, "photographer_url" to photo.photographerUrl).forEach { (suffix, value) ->
                p[stringPreferencesKey("pexels_${levelId}_$suffix")] = value
            }
        }
    }
}
