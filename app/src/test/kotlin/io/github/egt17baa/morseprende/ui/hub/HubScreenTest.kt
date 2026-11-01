package io.github.egt17baa.morseprende.ui.hub

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.content.MissionType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HubScreenTest {

    @get:Rule val compose = createComposeRule()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private val state = HubUiState(
        chapterTitle = "El Faro",
        nodes = listOf(
            MissionNode("c1m1", "La primera luz", MissionType.LESSON, 2, unlocked = true, isNext = false),
            MissionNode("c1m2", "Niebla en la bocana", MissionType.LESSON, 0, unlocked = true, isNext = true),
            MissionNode("c1m3", "El pesquero Gaviota", MissionType.LESSON, 0, unlocked = false, isNext = false),
        ),
        glow = 0.25f,
    )

    @Test fun tapping_unlocked_node_navigates() {
        val opened = mutableListOf<String>()
        compose.setContent { HubContent(state, HubCallbacks(onMission = { opened += it })) }
        compose.onNodeWithText("Niebla en la bocana").performScrollTo().performClick()
        assertEquals(listOf("c1m2"), opened)
    }

    @Test fun locked_node_not_clickable() {
        val opened = mutableListOf<String>()
        compose.setContent { HubContent(state, HubCallbacks(onMission = { opened += it })) }
        compose.onNodeWithText("El pesquero Gaviota").performScrollTo().performClick()
        assertEquals(emptyList<String>(), opened)
    }

    @Test fun bottom_bar_opens_library() {
        var library = 0
        compose.setContent { HubContent(state.copy(newCards = 2), HubCallbacks(onLibrary = { library++ })) }
        compose.onNodeWithText(context.getString(R.string.hub_library)).performClick()
        assertEquals(1, library)
    }

    @Test fun error_shows_message() {
        compose.setContent { HubContent(HubUiState(error = true), HubCallbacks()) }
        compose.onNodeWithText(context.getString(R.string.hub_error)).performScrollTo()
    }

    @Test fun balance_chip_names_destellos_and_opens_shop() {
        var shop = 0
        compose.setContent { HubContent(state.copy(balance = 120), HubCallbacks(onShop = { shop++ })) }
        compose.onNodeWithText(context.resources.getQuantityString(R.plurals.hub_balance, 120, 120)).performClick()
        assertEquals(1, shop)
    }

    @Test fun coming_soon_hidden_when_chapter3_exists() {
        fun section(id: Int) = ChapterSection(id, "Capítulo $id", state.nodes.map { it.copy(id = "c${id}_" + it.id) }, unlocked = id == 1, progress = 0f)
        compose.setContent { HubContent(state.copy(chapters = listOf(section(1), section(2), section(3))), HubCallbacks()) }
        compose.onAllNodesWithText(context.getString(R.string.hub_coming_soon)).assertCountEquals(0)
    }

    @Test fun coming_soon_shown_without_chapter3() {
        fun section(id: Int) = ChapterSection(id, "Capítulo $id", state.nodes.map { it.copy(id = "c${id}_" + it.id) }, unlocked = id == 1, progress = 0f)
        compose.setContent { HubContent(state.copy(chapters = listOf(section(1), section(2))), HubCallbacks()) }
        compose.onNodeWithText(context.getString(R.string.hub_coming_soon)).performScrollTo()
    }

    @Test fun locked_chapter_title_shown_once() {
        fun section(id: Int) = ChapterSection(id, "Título $id", state.nodes.map { it.copy(id = "c${id}_" + it.id) }, unlocked = id == 1, progress = 0f)
        compose.setContent { HubContent(state.copy(chapters = listOf(section(1), section(2), section(3))), HubCallbacks()) }
        compose.onAllNodesWithText(context.getString(R.string.hub_chapter, 2, "Título 2")).assertCountEquals(1)
        compose.onAllNodesWithText(context.getString(R.string.hub_chapter, 1, "Título 1")).assertCountEquals(1)
    }

    @Test fun hub_shows_daily_card() {
        var opened = 0
        var current by androidx.compose.runtime.mutableStateOf(state.copy(daily = DailyCard(doneToday = false)))
        compose.setContent { HubContent(current, HubCallbacks(onDaily = { opened++ })) }
        compose.onNodeWithText(context.getString(R.string.hub_daily_pending)).performScrollTo().performClick()
        assertEquals(1, opened)
        current = state.copy(daily = DailyCard(doneToday = true))
        compose.onNodeWithText(context.getString(R.string.hub_daily_done)).performScrollTo()
    }
}
