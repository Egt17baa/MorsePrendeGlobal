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
import androidx.compose.ui.graphics.Path
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
        val h = minOf(size.height, size.width * 1.4f)
        val cx = size.width / 2
        val bottom = size.height * 0.95f
        val top = bottom - h * 0.55f
        val halfBottom = h * 0.11f
        val halfTop = h * 0.07f
        val tower = Path().apply {
            moveTo(cx - halfBottom, bottom); lineTo(cx - halfTop, top); lineTo(cx + halfTop, top); lineTo(cx + halfBottom, bottom); close()
        }
        drawPath(tower, Sand)
        val stripeTop = top + (bottom - top) * 0.35f
        val stripeBottom = stripeTop + (bottom - top) * 0.14f
        fun halfAt(y: Float) = halfTop + (halfBottom - halfTop) * ((y - top) / (bottom - top))
        drawPath(
            Path().apply {
                moveTo(cx - halfAt(stripeTop), stripeTop); lineTo(cx + halfAt(stripeTop), stripeTop)
                lineTo(cx + halfAt(stripeBottom), stripeBottom); lineTo(cx - halfAt(stripeBottom), stripeBottom); close()
            },
            Coral,
        )
        val lanternHeight = h * 0.12f
        val lanternTop = top - lanternHeight
        val lampCenter = Offset(cx, lanternTop + lanternHeight / 2)
        if (lampOn) {
            // Halo contenido: el destello ocupa una zona pequeña de la pantalla (fotosensibilidad).
            val glow = h * 0.2f
            drawCircle(Brush.radialGradient(listOf(BeamCream, Color.Transparent), center = lampCenter, radius = glow), radius = glow, center = lampCenter)
        }
        drawRect(if (lampOn) BeamCream else SeaMist, Offset(cx - halfTop * 0.9f, lanternTop), Size(halfTop * 1.8f, lanternHeight))
        drawPath(
            Path().apply { moveTo(cx - halfTop * 1.3f, lanternTop); lineTo(cx, lanternTop - h * 0.09f); lineTo(cx + halfTop * 1.3f, lanternTop); close() },
            Coral,
        )
    }
}
