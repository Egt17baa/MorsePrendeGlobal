package io.github.egt17baa.morseprende.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.Coral
import io.github.egt17baa.morseprende.ui.theme.Sand
import io.github.egt17baa.morseprende.ui.theme.SeaMist

@Composable
fun FlashScreen(viewModel: FlashViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GameLifecycle(state.phase == GamePhase.PLAYING, viewModel::abandon)
    FlashContent(state, onStart = viewModel::start, onAnswer = viewModel::answer, onBack = onBack)
}

@Composable
fun FlashContent(state: FlashUiState, onStart: () -> Unit, onAnswer: (Char) -> Unit, onBack: () -> Unit) {
    GameScaffold(stringResource(R.string.game_flash), onBack) {
        when (state.phase) {
            GamePhase.LOCKED -> LockedPanel()
            GamePhase.READY -> StartPanel(stringResource(R.string.game_flash_desc), onStart)
            GamePhase.OVER -> GameOverPanel(
                state.score, state.isNewRecord, onStart, onBack,
                detail = if (state.averageReactionMs > 0) stringResource(R.string.game_flash_average, state.averageReactionMs) else null,
            )
            GamePhase.PLAYING -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.game_round, state.round, 10), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.game_score, state.score), style = MaterialTheme.typography.titleMedium)
                }
                FlashingLighthouse(state.lampOn, Modifier.fillMaxWidth().weight(1f).heightIn(min = 140.dp))
                Text(stringResource(R.string.game_flash_speed, state.flashWpm), style = MaterialTheme.typography.labelMedium)
                if (state.options.isEmpty()) {
                    Text(stringResource(R.string.game_flash_watch), style = MaterialTheme.typography.titleMedium)
                } else {
                    state.options.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { c ->
                                Button(onClick = { onAnswer(c) }, modifier = Modifier.weight(1f).height(64.dp)) {
                                    Text(c.toString(), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Faro cuya linterna parpadea al ritmo del Morse (zona pequeña de la pantalla, no la linterna real). */
@Composable
private fun FlashingLighthouse(lampOn: Boolean, modifier: Modifier) {
    Canvas(modifier) {
        val cx = size.width / 2
        val top = size.height * 0.3f
        val bottom = size.height * 0.95f
        val half = size.width * 0.06f
        drawRect(Sand, Offset(cx - half, top), Size(half * 2, bottom - top))
        drawRect(Coral, Offset(cx - half, top + (bottom - top) * 0.3f), Size(half * 2, (bottom - top) * 0.12f))
        val lampCenter = Offset(cx, top - size.height * 0.08f)
        if (lampOn) {
            drawCircle(Brush.radialGradient(listOf(BeamCream, Color.Transparent), center = lampCenter, radius = size.minDimension * 0.3f), radius = size.minDimension * 0.3f, center = lampCenter)
        }
        drawCircle(if (lampOn) BeamCream else SeaMist, radius = half * 1.2f, center = lampCenter)
    }
}
