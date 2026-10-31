package io.github.egt17baa.morseprende.playback

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SidetoneTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun sidetone_close_is_idempotent_and_safe_after_keys() {
        val tone = AudioSidetone(context, freqHz = 600, volume = 0.5f)
        tone.keyDown()
        assertTrue(tone.isKeyDown)
        tone.keyUp()
        assertFalse(tone.isKeyDown)
        tone.close()
        tone.close()
    }

    @Test fun sidetone_keys_after_close_do_nothing() {
        val tone = AudioSidetone(context, freqHz = 600, volume = 0.5f)
        tone.close()
        tone.keyDown()
        assertFalse(tone.isKeyDown)
    }
}
