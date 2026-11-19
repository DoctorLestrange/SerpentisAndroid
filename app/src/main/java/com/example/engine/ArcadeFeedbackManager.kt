package com.example.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.PI
import kotlin.math.sin

class ArcadeFeedbackManager(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val audioTrackSemaphore = Semaphore(4)

    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }.getOrNull()

    private data class ToneStep(
        val freqHz: Double,
        val durationMs: Int,
        val volume: Float
    )

    private data class CachedSound(
        val pcm: ShortArray,
        val totalDurationMs: Long
    )

    // Pre-synthesized PCM buffers (lazy, zero-allocation during active gameplay)
    private val turnSound: CachedSound by lazy {
        synthesizePcm(listOf(ToneStep(540.0, 22, 0.16f)))
    }

    private val eatAppleSoundsByCombo: List<CachedSound> by lazy {
        (1..8).map { combo ->
            val baseFreq = 520.0 + (combo * 55.0)
            synthesizePcm(
                listOf(
                    ToneStep(baseFreq, 42, 0.30f),
                    ToneStep(baseFreq * 1.25, 62, 0.34f)
                )
            )
        }
    }

    private val goldenSound: CachedSound by lazy {
        synthesizePcm(
            listOf(
                ToneStep(587.33, 45, 0.32f),
                ToneStep(783.99, 45, 0.34f),
                ToneStep(1046.50, 85, 0.38f)
            )
        )
    }

    private val portalSound: CachedSound by lazy {
        synthesizePcm(
            listOf(
                ToneStep(380.0, 35, 0.28f),
                ToneStep(620.0, 35, 0.30f),
                ToneStep(920.0, 45, 0.30f)
            )
        )
    }

    private val gameOverSound: CachedSound by lazy {
        synthesizePcm(
            listOf(
                ToneStep(440.0, 90, 0.36f),
                ToneStep(370.0, 90, 0.34f),
                ToneStep(311.13, 100, 0.32f),
                ToneStep(220.0, 180, 0.35f)
            )
        )
    }

    private val victorySound: CachedSound by lazy {
        synthesizePcm(
            listOf(
                ToneStep(523.25, 65, 0.34f),
                ToneStep(659.25, 65, 0.34f),
                ToneStep(783.99, 65, 0.36f),
                ToneStep(1046.50, 150, 0.40f)
            )
        )
    }

    fun onTurn(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) {
            vibrateTick(12L, 45)
        }
        if (soundEnabled) {
            playCachedSound(turnSound)
        }
    }

    fun onEatApple(combo: Int, soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) {
            vibrateTick(24L, 120)
        }
        if (soundEnabled) {
            val idx = (combo.coerceIn(1, 8) - 1)
            playCachedSound(eatAppleSoundsByCombo[idx])
        }
    }

    fun onEatGoldenOrPowerUp(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) {
            vibratePattern(longArrayOf(0, 25, 30, 40), intArrayOf(0, 160, 0, 220))
        }
        if (soundEnabled) {
            playCachedSound(goldenSound)
        }
    }

    fun onPortalWarp(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) {
            vibrateTick(32L, 150)
        }
        if (soundEnabled) {
            playCachedSound(portalSound)
        }
    }

    fun onGameOver(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) {
            vibratePattern(longArrayOf(0, 60, 40, 110), intArrayOf(0, 200, 0, 255))
        }
        if (soundEnabled) {
            playCachedSound(gameOverSound)
        }
    }

    fun onVictory(soundEnabled: Boolean, hapticsEnabled: Boolean) {
        if (hapticsEnabled) {
            vibratePattern(longArrayOf(0, 40, 30, 40, 30, 90), intArrayOf(0, 180, 0, 200, 0, 255))
        }
        if (soundEnabled) {
            playCachedSound(victorySound)
        }
    }

    private fun vibrateTick(durationMs: Long, amplitude: Int) {
        runCatching {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, amplitude.coerceIn(1, 255)))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        }
    }

    private fun vibratePattern(timings: LongArray, amplitudes: IntArray) {
        runCatching {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(timings, -1)
            }
        }
    }

    private fun synthesizePcm(steps: List<ToneStep>): CachedSound {
        val sampleRate = SAMPLE_RATE
        val totalDurationMs = steps.sumOf { it.durationMs }
        val totalSamples = ((sampleRate * totalDurationMs) / 1000).coerceAtLeast(1)
        val pcm = ShortArray(totalSamples)
        var sampleIndex = 0

        for (step in steps) {
            val stepSamples = (sampleRate * step.durationMs) / 1000
            val fadeSamples = (stepSamples / 6).coerceAtLeast(1)
            for (i in 0 until stepSamples) {
                if (sampleIndex >= totalSamples) break
                val env = when {
                    i < fadeSamples -> i.toFloat() / fadeSamples
                    i > stepSamples - fadeSamples -> (stepSamples - i).toFloat() / fadeSamples
                    else -> 1f
                }
                val angle = 2.0 * PI * i * step.freqHz / sampleRate
                val rawSine = sin(angle)
                val wave = (rawSine * 0.7 + (if (rawSine >= 0) 0.3 else -0.3))
                val sampleValue = (wave * step.volume * env * Short.MAX_VALUE).toInt()
                    .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    .toShort()
                pcm[sampleIndex++] = sampleValue
            }
        }
        return CachedSound(pcm = pcm, totalDurationMs = totalDurationMs.toLong())
    }

    private fun playCachedSound(cached: CachedSound) {
        if (!audioTrackSemaphore.tryAcquire()) return
        scope.launch {
            var track: AudioTrack? = null
            try {
                runCatching {
                    val totalSamples = cached.pcm.size
                    val byteCount = totalSamples * 2
                    val minBuf = AudioTrack.getMinBufferSize(
                        SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT
                    )
                    val bufferSize = maxOf(byteCount, minBuf)
                    if (bufferSize <= 0) return@runCatching

                    track = AudioTrack.Builder()
                        .setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_GAME)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                                .build()
                        )
                        .setAudioFormat(
                            AudioFormat.Builder()
                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setSampleRate(SAMPLE_RATE)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                .build()
                        )
                        .setBufferSizeInBytes(bufferSize)
                        .setTransferMode(AudioTrack.MODE_STATIC)
                        .build()

                    track?.write(cached.pcm, 0, totalSamples)
                    track?.play()
                    delay(cached.totalDurationMs + 35L)
                }
            } finally {
                runCatching {
                    track?.stop()
                }
                runCatching {
                    track?.release()
                }
                audioTrackSemaphore.release()
            }
        }
    }

    companion object {
        private const val SAMPLE_RATE = 22050
    }
}
