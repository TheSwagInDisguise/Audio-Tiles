package com.sony.xm6controller.domain

import android.content.Context
import android.database.ContentObserver
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Visual Theme Skins matching Sony official hardware finishes.
 */
enum class ThemeSkin(
    val displayName: String,
    val subtitle: String,
    val primaryGold: Color,
    val accentCopper: Color,
    val surfaceDark: Color,
    val backgroundObsidian: Color,
    val cardBorder: Color
) {
    BLACK_COPPER(
        displayName = "Black & Brushed Copper",
        subtitle = "Signature Sony 1000X flagship look",
        primaryGold = Color(0xFFC6A87C),
        accentCopper = Color(0xFFD79E66),
        surfaceDark = Color(0xFF141721),
        backgroundObsidian = Color(0xFF090B10),
        cardBorder = Color(0xFF222938)
    ),
    PLATINUM_SILVER(
        displayName = "Platinum Silver & Ice",
        subtitle = "Frosted matte aluminum & cool accents",
        primaryGold = Color(0xFFE2E8F0),
        accentCopper = Color(0xFF38BDF8),
        surfaceDark = Color(0xFF131A26),
        backgroundObsidian = Color(0xFF0A0F19),
        cardBorder = Color(0xFF253347)
    ),
    SMOKY_PINK(
        displayName = "Smoky Pink & Rose",
        subtitle = "Muted blush inspired by LinkBuds S & WF-XM5",
        primaryGold = Color(0xFFF9A8D4),
        accentCopper = Color(0xFFFB7185),
        surfaceDark = Color(0xFF1C141E),
        backgroundObsidian = Color(0xFF0F0A11),
        cardBorder = Color(0xFF332038)
    ),
    MIDNIGHT_NAVY(
        displayName = "Midnight Navy & Brass",
        subtitle = "Deep oceanic cobalt with brass highlights",
        primaryGold = Color(0xFFFCD34D),
        accentCopper = Color(0xFF60A5FA),
        surfaceDark = Color(0xFF0F172A),
        backgroundObsidian = Color(0xFF070B14),
        cardBorder = Color(0xFF1E293B)
    )
}

/**
 * ISO 226 Dynamic Bass Equal-Loudness Manager.
 *
 * Based on the Fletcher-Munson equal-loudness contours:
 * Human ears lose sensitivity to frequencies below 100Hz at lower listening volumes.
 * This manager dynamically boosts Clear Bass at low volume levels (10% - 40%)
 * and eases back to reference flat as volume increases to 70%+, preventing distortion
 * while preserving punch during quiet listening sessions.
 */
class DynamicBassManager(
    private val context: Context,
    private val repository: HeadphoneRepository
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observer: ContentObserver? = null

    private val _isEnabled = MutableStateFlow(false)
    val isEnabled: StateFlow<Boolean> = _isEnabled

    private val _currentCompensationOffset = MutableStateFlow(0)
    val currentCompensationOffset: StateFlow<Int> = _currentCompensationOffset

    private var baseClearBass: Int = 0

    init {
        val prefs = context.getSharedPreferences("smart_audio_prefs", Context.MODE_PRIVATE)
        _isEnabled.value = prefs.getBoolean("iso226_enabled", false)
        if (_isEnabled.value) {
            startObserving()
        }
    }

    fun setEnabled(enabled: Boolean) {
        _isEnabled.value = enabled
        context.getSharedPreferences("smart_audio_prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("iso226_enabled", enabled)
            .apply()

        if (enabled) {
            baseClearBass = repository.equalizerState.value.clearBass
            startObserving()
            recalculateAndApply()
        } else {
            stopObserving()
            _currentCompensationOffset.value = 0
            // Restore base Clear Bass
            scope.launch {
                val bands = repository.equalizerState.value.bands
                repository.setEqualizerBands(EqPreset.CUSTOM_1, baseClearBass, bands)
            }
        }
    }

    private fun startObserving() {
        stopObserving()
        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                if (_isEnabled.value) {
                    recalculateAndApply()
                }
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.System.CONTENT_URI,
            true,
            observer!!
        )
    }

    private fun stopObserving() {
        observer?.let {
            try {
                context.contentResolver.unregisterContentObserver(it)
            } catch (_: Exception) {}
            observer = null
        }
    }

    private fun recalculateAndApply() {
        val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val fraction = currentVol.toFloat() / maxVol.toFloat()

        // ISO 226 curve approximation for sub-bass compensation
        val offset = when {
            fraction <= 0.20f -> 4
            fraction <= 0.40f -> 3
            fraction <= 0.60f -> 1
            else -> 0
        }

        if (offset != _currentCompensationOffset.value) {
            _currentCompensationOffset.value = offset
            val targetCb = (baseClearBass + offset).coerceIn(-10, 10)
            scope.launch {
                val bands = repository.equalizerState.value.bands
                repository.setEqualizerBands(EqPreset.CUSTOM_1, targetCb, bands)
            }
        }
    }

    companion object {
        fun calculateCompensation(volumeRatio: Float): Int {
            return when {
                volumeRatio <= 0.20f -> 4
                volumeRatio <= 0.40f -> 3
                volumeRatio <= 0.60f -> 1
                else -> 0
            }
        }
    }
}

/**
 * Diagnostic Tone Generator for Earbud Acoustic Seal & Sub-Bass Leakage Verification.
 */
object AcousticSealTester {
    private var activeTrack: AudioTrack? = null

    operator fun invoke(context: Context? = null) = this

    fun generateSineWave(frequencyHz: Float, sampleRate: Int = 44100, durationMs: Int = 1800): ShortArray {
        val totalSamples = (sampleRate * (durationMs / 1000.0)).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            // Smooth 50ms fade-in and fade-out envelope to avoid audible clicks
            val fadeIn = (i.toFloat() / (sampleRate * 0.05f)).coerceIn(0f, 1f)
            val fadeOut = ((totalSamples - i).toFloat() / (sampleRate * 0.05f)).coerceIn(0f, 1f)
            val envelope = fadeIn * fadeOut

            val angle = 2.0 * Math.PI * i * frequencyHz / sampleRate
            buffer[i] = (Math.sin(angle) * Short.MAX_VALUE * 0.40f * envelope).toInt().toShort()
        }
        return buffer
    }

    fun playTone(frequencyHz: Int, durationMs: Int = 2500, onFinished: () -> Unit = {}) {
        playTestTone(frequencyHz.toFloat(), durationMs, onFinished)
    }

    fun stop() {
        stopTone()
    }

    fun release() {
        stopTone()
    }

    fun playTestTone(frequencyHz: Float, durationMs: Int = 1800, onFinished: () -> Unit = {}) {
        stopTone()
        Thread {
            try {
                val sampleRate = 44100
                val buffer = generateSineWave(frequencyHz, sampleRate, durationMs)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(buffer.size * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                activeTrack = track
                track.write(buffer, 0, buffer.size)
                track.play()

                Thread.sleep(durationMs.toLong() + 50)
                track.stop()
                track.release()
                if (activeTrack == track) activeTrack = null
                Handler(Looper.getMainLooper()).post { onFinished() }
            } catch (_: Exception) {
                Handler(Looper.getMainLooper()).post { onFinished() }
            }
        }.start()
    }

    fun stopTone() {
        try {
            activeTrack?.stop()
            activeTrack?.release()
        } catch (_: Exception) {}
        activeTrack = null
    }
}
