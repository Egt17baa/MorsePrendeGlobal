package io.github.egt17baa.morseprende.settings

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.ui.theme.MorsePrendeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LicensesScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @After fun tearDown() = Dispatchers.resetMain()

    @Test fun licenses_screen_shows_all() {
        // Con el texto «Muy grande» todo sigue siendo alcanzable desplazando.
        compose.setContent { MorsePrendeTheme(textScale = 1.3f) { LicensesScreen(onBack = {}) } }
        compose.onNodeWithText(context.getString(R.string.licenses_font)).assertExists()
        compose.onNodeWithText("SIL OPEN FONT LICENSE", substring = true).performScrollTo()
        compose.onNodeWithText(context.getString(R.string.licenses_apache)).performScrollTo()
        compose.onNodeWithText("END OF TERMS AND CONDITIONS", substring = true).performScrollTo()
    }

    @Test fun about_opens_licenses() {
        var opened = 0
        compose.setContent { SettingsScreen(SettingsViewModel(FakeSettingsRepository()), onBack = {}, onLicenses = { opened++ }) }
        compose.onNodeWithText(context.getString(R.string.settings_licenses)).performScrollTo().performClick()
        assertEquals(1, opened)
    }
}
