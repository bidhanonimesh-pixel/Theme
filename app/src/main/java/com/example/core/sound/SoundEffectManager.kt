package com.example.core.sound

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class SoundEffectManager(private val context: Context) {
    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_SYSTEM, 70)
        } catch (_: Exception) {
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
        }
    }

    fun playSciFiBeep(type: ToneType = ToneType.CLICK) {
        try {
            val tone = when (type) {
                ToneType.CLICK -> ToneGenerator.TONE_PROP_BEEP
                ToneType.CONFIRM -> ToneGenerator.TONE_PROP_ACK
                ToneType.ALERT -> ToneGenerator.TONE_PROP_BEEP2
                ToneType.DIAL_0 -> ToneGenerator.TONE_DTMF_0
                ToneType.DIAL_1 -> ToneGenerator.TONE_DTMF_1
                ToneType.DIAL_2 -> ToneGenerator.TONE_DTMF_2
                ToneType.DIAL_3 -> ToneGenerator.TONE_DTMF_3
                ToneType.DIAL_4 -> ToneGenerator.TONE_DTMF_4
                ToneType.DIAL_5 -> ToneGenerator.TONE_DTMF_5
                ToneType.DIAL_6 -> ToneGenerator.TONE_DTMF_6
                ToneType.DIAL_7 -> ToneGenerator.TONE_DTMF_7
                ToneType.DIAL_8 -> ToneGenerator.TONE_DTMF_8
                ToneType.DIAL_9 -> ToneGenerator.TONE_DTMF_9
                ToneType.DIAL_STAR -> ToneGenerator.TONE_DTMF_S
                ToneType.DIAL_POUND -> ToneGenerator.TONE_DTMF_P
            }
            toneGenerator?.startTone(tone, 70)
        } catch (_: Exception) {
        }
    }

    fun performHaptic(durationMs: Long = 25) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        toneGenerator?.release()
        toneGenerator = null
    }

    enum class ToneType {
        CLICK, CONFIRM, ALERT,
        DIAL_0, DIAL_1, DIAL_2, DIAL_3, DIAL_4, DIAL_5, DIAL_6, DIAL_7, DIAL_8, DIAL_9, DIAL_STAR, DIAL_POUND
    }
}
