package com.hb.puzz.ui.feedback

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.hb.puzz.R

/** Small, low-latency feedback layer for rewarding correct puzzle connections. */
class PuzzleFeedbackPlayer(context: Context) {
    private val appContext = context.applicationContext
    private val soundPool: SoundPool
    private val loadedSounds = mutableSetOf<Int>()
    private val connectionSoundId: Int
    private val solvedSoundId: Int

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(3)
            .setAudioAttributes(attributes)
            .build()

        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSounds += sampleId
        }

        connectionSoundId = soundPool.load(appContext, R.raw.piece_connect, 1)
        solvedSoundId = soundPool.load(appContext, R.raw.puzzle_solved, 1)
    }

    fun playConnectionSound() {
        if (connectionSoundId in loadedSounds) {
            soundPool.play(connectionSoundId, 0.82f, 0.82f, 1, 0, 1f)
        }
    }

    fun playSolvedSound() {
        if (solvedSoundId in loadedSounds) {
            soundPool.play(solvedSoundId, 0.9f, 0.9f, 2, 0, 1f)
        }
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
        soundPool.release()
    }
}
