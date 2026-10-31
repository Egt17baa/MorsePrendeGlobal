package io.github.egt17baa.morseprende.playback

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import io.github.egt17baa.morseprende.core.Signal

/**
 * Morse con la linterna del móvil. No necesita permiso de cámara. Si la linterna falla (no hay
 * flash o la cámara la está usando) deja de intentarlo durante esa reproducción sin cerrar la app.
 */
class TorchOutput(context: Context, private val onFailure: () -> Unit = {}) : SignalOutput {

    private val cameraManager: CameraManager? = context.getSystemService(CameraManager::class.java)
    private val cameraId: String? by lazy { cameraManager?.let(::findFlashCamera) }

    @Volatile private var failed = false
    private val stepped = SteppedOutput(::setTorch)

    override suspend fun play(signals: List<Signal>) {
        failed = false
        stepped.play(signals)
    }

    override fun stop() = stepped.stop()

    private fun setTorch(on: Boolean) {
        if (failed && on) return
        val id = cameraId ?: run { markFailed(); return }
        try {
            cameraManager?.setTorchMode(id, on)
        } catch (e: Exception) {
            // CameraAccessException (cámara ocupada), IllegalArgumentException (id no válido)…
            markFailed()
        }
    }

    /** Avisa una sola vez por reproducción. */
    private fun markFailed() {
        if (!failed) {
            failed = true
            onFailure()
        }
    }

    companion object {
        fun isAvailable(context: Context): Boolean {
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) return false
            val manager = context.getSystemService(CameraManager::class.java) ?: return false
            return findFlashCamera(manager) != null
        }

        private fun findFlashCamera(manager: CameraManager): String? = runCatching {
            manager.cameraIdList.firstOrNull { id ->
                manager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrNull()
    }
}
