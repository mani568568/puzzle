package com.hb.puzz.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hb.puzz.data.*
import com.hb.puzz.data.images.*
import com.hb.puzz.domain.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PicturePuzzleUiState(
    val loading: Boolean = true,
    val image: PuzzleImage? = null,
    val boardVersion: Int = 0,
    val moves: Int = 0,
    val elapsedMillis: Long = 0,
    val started: Boolean = false,
    val running: Boolean = false,
    val solved: Boolean = false,
    val connections: Int = 0,
    val celebration: Set<Int> = emptySet(),
    val celebrationVersion: Int = 0,
    val receipt: CompletionReceipt? = null,
    val error: String? = null,
    val saveError: Boolean = false,
    val speedEligible: Boolean = true
)

class PuzzleGameViewModel(
    private val levelId: Int,
    gridSize: Int,
    private val settings: GameSettings,
    private val repository: PuzzleImageRepository,
    private val preferredSource: ImageSourceMode
) : ViewModel() {
    val engine = PuzzleEngine(gridSize)
    private val clock = ActivePlayClock(SystemClock::elapsedRealtime)
    private var session = PuzzleSession(levelId, gridSize, engine.getCurrentPositions(), 0)
    private val mutable = MutableStateFlow(PicturePuzzleUiState())
    val state = mutable.asStateFlow()
    // Ordered writes survive screen disposal, then drain and release their scope.
    private val writerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val writer = writerScope.launch {
        for (write in writes) {
            try { write() } catch (e: CancellationException) { throw e }
            catch (_: Exception) { leaving = false; mutable.value = mutable.value.copy(saveError = true) }
        }
    }
    private var leaving = false
    private var loadJob: Job? = null
    private var celebrationJob: Job? = null

    init {
        load()
        viewModelScope.launch {
            var lastCheckpoint = 0L
            while (isActive) {
                delay(250)
                if (clock.running) {
                    val elapsed = clock.elapsedMillis()
                    mutable.value = mutable.value.copy(elapsedMillis = elapsed)
                    if (elapsed - lastCheckpoint >= 5_000) {
                        checkpoint(); lastCheckpoint = elapsed
                    }
                }
            }
        }
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutable.value = mutable.value.copy(loading = true, error = null)
            try {
                val saved = settings.loadSession()
                if (saved?.levelId == levelId && saved.gridSize == engine.gridSize && engine.restorePositions(saved.positions)) {
                    session = saved
                    clock.restore(saved.elapsedMillis)
                }
                val loaded = repository.loadLevelImage(PuzzleLevel.requireLevel(levelId), session.imageSource ?: preferredSource)
                val identity = loaded.attribution?.id?.let { "pexels-$it" } ?: "bundled-$levelId"
                if (session.imageIdentity != null && session.imageIdentity != identity) {
                    mutable.value = mutable.value.copy(loading = false,
                        error = "Your saved photo is unavailable. Reconnect and retry to keep the same puzzle.")
                    return@launch
                }
                session = session.copy(imageSource = loaded.sourceMode, imageIdentity = identity)
                mutable.value = PicturePuzzleUiState(loading = false, image = loaded,
                    boardVersion = mutable.value.boardVersion + 1, moves = session.moveCount,
                    elapsedMillis = session.elapsedMillis, started = session.started,
                    solved = engine.isSolved(), connections = engine.getCorrectConnections().size,
                    speedEligible = session.speedEligible)
                if (engine.isSolved()) finish() else checkpoint()
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                mutable.value = mutable.value.copy(loading = false, error = "We couldn’t open your puzzle. Please try again.")
            }
        }
    }

    fun resume() {
        val s = mutable.value
        if (s.loading || s.error != null || s.solved) return
        clock.resume()
        session = session.copy(started = true)
        mutable.value = s.copy(started = true, running = true)
        checkpoint()
    }
    fun pause() {
        clock.pause()
        mutable.value = mutable.value.copy(running = false, elapsedMillis = clock.elapsedMillis())
        if (!mutable.value.loading && mutable.value.error == null && !mutable.value.solved) checkpoint()
    }
    private fun snapshot() = session.copy(positions = engine.getCurrentPositions(),
        moveCount = mutable.value.moves, elapsedMillis = clock.elapsedMillis())

    private fun checkpoint() {
        val snap = snapshot()
        writes.trySend { settings.saveSession(snap); mutable.value = mutable.value.copy(saveError = false) }
    }
    fun retrySave() { if (mutable.value.solved) finish() else checkpoint() }

    /** Navigation waits for the latest snapshot; failure keeps the player on this screen. */
    fun saveAndLeave(onSaved: () -> Unit) {
        if (leaving) return
        leaving = true
        if (mutable.value.loading || mutable.value.error != null) {
            loadJob?.cancel()
            onSaved()
            return
        }
        pause()
        val snap = snapshot()
        val solved = mutable.value.solved
        writes.trySend {
            if (!solved) {
                settings.saveSession(snap)
                mutable.value = mutable.value.copy(saveError = false)
            }
            if (!mutable.value.saveError) onSaved()
        }
    }

    /** 0 = no new merge, 1 = merge, 2 = completed. */
    fun drop(anchor: Int, target: Int): Int {
        val current = mutable.value
        if (current.loading || current.error != null || current.solved) return 0
        // Before the first move the board is already interactive. The first actual drop
        // starts active play automatically. A deliberate Pause still disables movement.
        if (current.started && !current.running) return 0
        if (!current.started) {
            clock.resume()
            session = session.copy(started = true)
            mutable.value = current.copy(started = true, running = true)
        }
        val before = engine.getCorrectConnections()
        if (!engine.attemptMoveGroup(anchor, target)) return 0
        val after = engine.getCorrectConnections()
        val gained = after - before
        val ids = gained.flatMap { listOf(it.firstTileId, it.secondTileId) }.toSet()
        val celebrating = engine.getConnectedGroups().filter { group -> group.any { it in ids } }.flatten().toSet()
        val solved = engine.isSolved()
        if (solved) clock.pause()
        mutable.value = mutable.value.copy(moves = mutable.value.moves + 1,
            elapsedMillis = clock.elapsedMillis(), boardVersion = mutable.value.boardVersion + 1,
            connections = after.size, solved = solved, running = !solved,
            celebration = celebrating, celebrationVersion = mutable.value.celebrationVersion + 1)
        celebrationJob?.cancel()
        celebrationJob = viewModelScope.launch {
            delay(MergeMotion.CLEAR_DELAY_MILLIS); mutable.value = mutable.value.copy(celebration = emptySet())
        }
        if (solved) finish() else checkpoint()
        return if (solved) 2 else if (ids.isNotEmpty()) 1 else 0
    }

    private fun finish() {
        if (mutable.value.receipt != null) return
        clock.pause()
        val snap = snapshot()
        writes.trySend {
            // A crash between these transactions leaves a solved session that can finish on restore.
            settings.saveSession(snap)
            val receipt = settings.completeSession(snap)
            mutable.value = mutable.value.copy(receipt = receipt, saveError = false)
        }
    }

    fun restart() {
        if (mutable.value.loading || mutable.value.image == null) return
        celebrationJob?.cancel()
        clock.restore(0)
        engine.shuffle()
        session = session.copy(sessionId = UUID.randomUUID().toString(), positions = engine.getCurrentPositions(),
            moveCount = 0, elapsedMillis = 0, started = false, speedEligible = true)
        mutable.value = PicturePuzzleUiState(loading = false, image = mutable.value.image,
            boardVersion = mutable.value.boardVersion + 1, connections = engine.getCorrectConnections().size)
        checkpoint()
    }
    override fun onCleared() {
        clock.pause()
        writes.close()
        writer.invokeOnCompletion { writerScope.cancel() }
        super.onCleared()
    }
}
