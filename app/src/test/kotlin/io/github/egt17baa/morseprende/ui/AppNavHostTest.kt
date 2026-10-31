package io.github.egt17baa.morseprende.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
        compose.onNodeWithText(context.getString(R.string.home_settings)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.settings_title)).assertIsDisplayed()
    }

    @Test fun home_navigates_to_translator() {
        compose.setContent { AppNavHost(AppContainer(context)) }
        compose.onNodeWithText(context.getString(R.string.home_translator)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.translator_title)).assertIsDisplayed()
    }
}
