package io.github.egt17baa.morseprende.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.AppContainer
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.ui.navigation.AppNavHost
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppNavHostTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun home_navigates_to_settings() {
        compose.setContent { AppNavHost(AppContainer(context)) }
        compose.onNodeWithText(context.getString(R.string.hub_settings)).performClick()
        compose.onNodeWithText(context.getString(R.string.settings_title)).assertIsDisplayed()
    }

    @Test fun home_navigates_to_translator() {
        compose.setContent { AppNavHost(AppContainer(context)) }
        compose.onNodeWithText(context.getString(R.string.hub_translator)).performClick()
        compose.onNodeWithText(context.getString(R.string.translator_title)).assertIsDisplayed()
    }

    @Test fun double_back_returns_home_not_blank() {
        compose.setContent { AppNavHost(AppContainer(context)) }
        compose.onNodeWithText(context.getString(R.string.hub_settings)).performClick()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        val back = compose.onNodeWithContentDescription(context.getString(R.string.back))
        back.performClick()
        compose.mainClock.advanceTimeByFrame()
        back.performClick()
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithText(context.getString(R.string.hub_translator)).assertIsDisplayed()
    }

    @Test fun app_starts_on_hub() {
        compose.setContent { AppNavHost(AppContainer(context)) }
        compose.onNodeWithText(context.getString(R.string.hub_library)).assertIsDisplayed()
        compose.onNodeWithText("La primera luz", useUnmergedTree = true).assertExists()
    }
}
