package com.hb.puzz.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hb.puzz.R

class PuzzleFeedbackPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build()
    private val cues = SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attributes).build()
    private val ready = mutableSetOf<Int>()
    private val themes = intArrayOf(R.raw.music_meadow, R.raw.music_lantern, R.raw.music_tide)
    private val variants: List<IntArray>
    private val victory: Int
    private val nextVariant = IntArray(3)
    private var cueStream = 0
    private var track = 0
    private var music: MediaPlayer? = null
    private var audible = false
    private var playMusic = false
    private var hasFocus = false
    private var requestedFocus = false
    private var released = false

    init {
        cues.setOnLoadCompleteListener { _, id, status -> if (!released && status == 0) ready.add(id) }
        fun load(id: Int) = cues.load(appContext, id, 1)
        variants = listOf(
            intArrayOf(load(R.raw.merge_pair_a), load(R.raw.merge_pair_b)),
            intArrayOf(load(R.raw.merge_cluster_a), load(R.raw.merge_cluster_b)),
            intArrayOf(load(R.raw.merge_large_a), load(R.raw.merge_large_b))
        )
        victory = load(R.raw.music_finish)
    }

    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (!released && audible && requestedFocus) {
            hasFocus = change == AudioManager.AUDIOFOCUS_GAIN
            if (!hasFocus) stopCue()
            if (change == AudioManager.AUDIOFOCUS_LOSS) abandonFocus()
            syncMusic()
        }
    }
    private val focusRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener(focusListener)
            .setWillPauseWhenDucked(true)
            .build()
    } else null

    fun updatePlayback(levelId: Int, enabled: Boolean, backing: Boolean) {
        if (released) return
        val newTrack = themes[(levelId - 1).coerceAtLeast(0) % themes.size]
        if (track != newTrack) {
            music?.release()
            music = null
            track = newTrack
        }
        val previouslyAudible = audible
        audible = enabled
        playMusic = backing
        if (enabled && !previouslyAudible) {
            @Suppress("DEPRECATION")
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                manager.requestAudioFocus(requireNotNull(focusRequest))
            } else manager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
            hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            requestedFocus = hasFocus
        } else if (!enabled) {
            stopCue()
            abandonFocus()
        }
        syncMusic()
    }

    private fun syncMusic() {
        if (!audible || !hasFocus || !playMusic) {
            music?.takeIf { it.isPlaying }?.pause()
            return
        }
        val player = music ?: MediaPlayer.create(
            appContext, track, attributes, AudioManager.AUDIO_SESSION_ID_GENERATE
        )?.also {
            it.isLooping = true
            it.setVolume(0.21f, 0.21f)
            music = it
        } ?: return
        if (!player.isPlaying) player.start()
    }

    private fun abandonFocus() {
        hasFocus = false
        if (!requestedFocus) return
        requestedFocus = false
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.abandonAudioFocusRequest(requireNotNull(focusRequest))
        } else manager.abandonAudioFocus(focusListener)
    }

    fun playMerge(tiles: Int) {
        if (tiles < 2) return
        val tier = when (tiles) {
            in 2..3 -> 0
            in 4..7 -> 1
            else -> 2
        }
        val choices = variants[tier]
        val index = nextVariant[tier]
        nextVariant[tier] = (index + 1) % choices.size
        playCue(choices[index])
    }

    fun playSolvedSound() = playCue(victory)

    private fun playCue(id: Int) {
        if (released || !audible || !hasFocus || id !in ready) return
        stopCue()
        cueStream = cues.play(id, 0.76f, 0.76f, 1, 0, 1f)
    }

    private fun stopCue() {
        if (cueStream != 0) cues.stop(cueStream)
        cueStream = 0
    }

    fun buzzForConnection() {
        vibrate(
            timings = longArrayOf(0, 18, 34, 24),
            amplitudes = intArrayOf(0, 110, 0, 180)
        )
    }

    fun buzzForSolved() {
        vibrate(
            timings = longArrayOf(0, 28, 38, 35, 45, 55),
            amplitudes = intArrayOf(0, 140, 0, 190, 0, 230)
        )
    }

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        } ?: return

        if (!vibrator.hasVibrator()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(timings, -1)
        }
    }

    fun release() {
        released = true
        abandonFocus()
        music?.release()
        music = null
        cues.release()
    }
}
