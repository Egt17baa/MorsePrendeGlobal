package io.github.egt17baa.morseprende.playback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.core.Signal
import kotlinx.coroutines.delay

/**
 * Morse por vibración. El patrón completo se entrega al motor de vibración de una vez. Si el
 * móvil no tiene vibrador se avisa con [onFailure].
 */
class VibrationOutput(context: Context, private val onFailure: () -> Unit = {}) : SignalOutput {

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }

    override suspend fun play(signals: List<Signal>) {
        val device = vibrator?.takeIf { it.hasVibrator() }
        if (device == null) {
            onFailure()
            return
        }
        val pattern = vibrationPattern(signals)
        if (pattern.isEmpty()) return
        try {
            device.vibrate(VibrationEffect.createWaveform(pattern, NO_REPEAT))
            delay(MorseTiming.totalMs(signals))
        } finally {
            device.cancel()
        }
    }

    override fun stop() {
        vibrator?.cancel()
    }

    private companion object {
        const val NO_REPEAT = -1
    }
}
