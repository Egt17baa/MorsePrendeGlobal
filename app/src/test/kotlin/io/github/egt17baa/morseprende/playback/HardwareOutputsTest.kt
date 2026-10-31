package io.github.egt17baa.morseprende.playback

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Vibrator
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.core.Signal
import io.github.egt17baa.morseprende.settings.AppSettings
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowCameraCharacteristics
import kotlin.test.assertFailsWith

@RunWith(RobolectricTestRunner::class)
class HardwareOutputsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun addFlashCamera() {
        val characteristics = ShadowCameraCharacteristics.newCameraCharacteristics()
        shadowOf(characteristics).set(CameraCharacteristics.FLASH_INFO_AVAILABLE, true)
        val cameraManager = context.getSystemService(CameraManager::class.java)
        shadowOf(cameraManager).addCamera("0", characteristics)
    }

    @Test fun torch_unavailable_without_flash() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA_FLASH, false)
        assertFalse(TorchOutput.isAvailable(context))
    }

    @Test fun torch_unavailable_with_feature_but_no_camera() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA_FLASH, true)
        assertFalse(TorchOutput.isAvailable(context))
    }

    @Test fun torch_available_with_flash_camera() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_CAMERA_FLASH, true)
        addFlashCamera()
        assertTrue(TorchOutput.isAvailable(context))
    }

    @Test fun torch_play_survives_camera_exception() = runTest {
        // Sin cámaras: setTorchMode no tiene id válido y no debe lanzar nada.
        TorchOutput(context).play(listOf(Signal(true, 60)))
    }

    @Test fun torch_toggles_flash_camera() = runTest {
        addFlashCamera()
        val torch = TorchOutput(context)
        val cameraManager = context.getSystemService(CameraManager::class.java)
        val job = launch { torch.play(listOf(Signal(true, 100))) }
        runCurrent()
        assertTrue(shadowOf(cameraManager).getTorchMode("0"))
        advanceTimeBy(200)
        job.join()
        assertFalse(shadowOf(cameraManager).getTorchMode("0"))
    }

    @Suppress("DEPRECATION")
    @Test fun vibration_starts_and_stops() = runTest {
        val vibrator = context.getSystemService(Vibrator::class.java)
        val output = VibrationOutput(context)
        val job = launch { output.play(listOf(Signal(true, 60), Signal(false, 60), Signal(true, 180))) }
        runCurrent()
        assertTrue(shadowOf(vibrator).isVibrating)
        job.cancel()
        job.join()
        assertFalse(shadowOf(vibrator).isVibrating)
    }

    @Test fun audio_play_completes() = runBlocking {
        withTimeout(5_000) {
            AudioOutput(context, freqHz = 600, volume = 0.5f).play(listOf(Signal(true, 50), Signal(false, 50)))
        }
    }

    @Test fun audio_stop_without_play_is_safe() {
        val audio = AudioOutput(context, freqHz = 600, volume = 0.5f)
        audio.stop()
        audio.stop()
    }

    @Test fun factory_creates_hardware_outputs() {
        val factory = AndroidOutputFactory(context)
        val settings = AppSettings()
        assertTrue(factory.create(OutputKind.SOUND, settings) is AudioOutput)
        assertTrue(factory.create(OutputKind.VIBRATION, settings) is VibrationOutput)
        assertTrue(factory.create(OutputKind.TORCH, settings) is TorchOutput)
        assertFailsWith<IllegalArgumentException> { factory.create(OutputKind.SCREEN, settings) }
    }
}
