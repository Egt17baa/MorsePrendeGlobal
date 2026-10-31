package io.github.egt17baa.morseprende.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.ui.home.HomeScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun home_buttons_call_navigation() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            HomeScreen(onTranslator = { clicks += "translator" }, onSettings = { clicks += "settings" })
        }
        compose.onNodeWithText(context.getString(R.string.home_campaign_soon)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.home_translator)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.home_settings)).performScrollTo().performClick()
        assertEquals(listOf("translator", "settings"), clicks)
    }
}
