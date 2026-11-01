package io.github.egt17baa.morseprende.ui.practice

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.data.games.GameId
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.Coral

@Composable
fun PracticeScreen(viewModel: PracticeViewModel, onGame: (GameId) -> Unit, onBack: () -> Unit, onWorkshop: (WorkshopTool) -> Unit = {}) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PracticeContent(state, onGame, onBack, onWorkshop)
}

/** Herramientas del «Taller del farero»: práctica libre, sin puntos. */
enum class WorkshopTool { SEND, LETTERS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeContent(state: PracticeUiState, onGame: (GameId) -> Unit, onBack: () -> Unit, onWorkshop: (WorkshopTool) -> Unit = {}) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.practice_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.workshop_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.workshop_intro), style = MaterialTheme.typography.bodyMedium)
            WorkshopCard(stringResource(R.string.workshop_send), stringResource(R.string.workshop_send_desc), GameId.INTERCEPT) { onWorkshop(WorkshopTool.SEND) }
            WorkshopCard(stringResource(R.string.workshop_letters), stringResource(R.string.workshop_letters_desc), GameId.STORM) { onWorkshop(WorkshopTool.LETTERS) }
            Text(stringResource(R.string.practice_title), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.practice_intro), style = MaterialTheme.typography.bodyMedium)
            state.games.forEach { GameCard(it, onGame) }
        }
    }
}

@Composable
private fun WorkshopCard(title: String, description: String, icon: GameId, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onOpen)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            GameIcon(icon)
            Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun GameCard(game: PracticeGame, onGame: (GameId) -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = game.unlocked) { onGame(game.id) },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (game.unlocked) GameIcon(game.id) else Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.hub_locked), modifier = Modifier.size(48.dp))
            Column(Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(game.id.titleRes), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(game.id.descriptionRes), style = MaterialTheme.typography.bodySmall)
                when {
                    !game.unlocked -> Text(stringResource(R.string.library_unlock_with, game.unlockMissionTitle.orEmpty()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                    game.record != null -> Text(
                        stringResource(R.string.practice_record, game.record.bestScore, game.record.plays),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

val GameId.titleRes: Int
    get() = when (this) {
        GameId.STORM -> R.string.game_storm
        GameId.FLASH -> R.string.game_flash
        GameId.INTERCEPT -> R.string.game_intercept
        GameId.NIGHT_WATCH -> R.string.game_night
    }

private val GameId.descriptionRes: Int
    get() = when (this) {
        GameId.STORM -> R.string.game_storm_desc
        GameId.FLASH -> R.string.game_flash_desc
        GameId.INTERCEPT -> R.string.game_intercept_desc
        GameId.NIGHT_WATCH -> R.string.game_night_desc
    }

/** Iconos propios de cada minijuego. */
@Composable
private fun GameIcon(id: GameId) {
    Canvas(Modifier.size(48.dp)) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w / 14)
        when (id) {
            GameId.STORM -> {
                drawLine(BeamCream, Offset(w * 0.3f, h * 0.15f), Offset(w * 0.3f, h * 0.45f), strokeWidth = stroke.width)
                drawCircle(BeamCream, radius = w * 0.06f, center = Offset(w * 0.6f, h * 0.35f))
                drawLine(Coral, Offset(0f, h * 0.8f), Offset(w, h * 0.8f), strokeWidth = stroke.width)
            }
            GameId.FLASH -> {
                drawRect(BeamCream, Offset(w * 0.4f, h * 0.35f), Size(w * 0.2f, h * 0.55f), style = stroke)
                drawCircle(Coral, radius = w * 0.14f, center = Offset(w * 0.5f, h * 0.22f))
            }
            GameId.NIGHT_WATCH -> {
                // Luna creciente y estrellas.
                drawCircle(BeamCream, radius = w * 0.28f, center = Offset(w * 0.45f, h * 0.5f))
                drawCircle(androidx.compose.ui.graphics.Color(0xFF0E2A2F), radius = w * 0.24f, center = Offset(w * 0.58f, h * 0.42f))
                drawCircle(Coral, radius = w * 0.05f, center = Offset(w * 0.82f, h * 0.2f))
                drawCircle(BeamCream, radius = w * 0.035f, center = Offset(w * 0.8f, h * 0.75f))
            }
            GameId.INTERCEPT -> {
                drawRect(BeamCream, Offset(w * 0.15f, h * 0.35f), Size(w * 0.7f, h * 0.5f), style = stroke)
                drawLine(Coral, Offset(w * 0.5f, h * 0.35f), Offset(w * 0.75f, h * 0.05f), strokeWidth = stroke.width)
                drawCircle(Coral, radius = w * 0.08f, center = Offset(w * 0.35f, h * 0.6f))
            }
        }
    }
}
