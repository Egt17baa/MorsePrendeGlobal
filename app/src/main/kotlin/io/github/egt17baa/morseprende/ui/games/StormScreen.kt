package io.github.egt17baa.morseprende.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.Coral
import io.github.egt17baa.morseprende.ui.theme.SeaDeep
import io.github.egt17baa.morseprende.ui.theme.SeaMist
import io.github.egt17baa.morseprende.ui.theme.SeaNight

@Composable
fun StormScreen(viewModel: StormViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GameLifecycle(state.phase == GamePhase.PLAYING, viewModel::abandon)
    if (state.phase == GamePhase.PLAYING) {
        LaunchedEffect(Unit) {
            while (true) withFrameMillis { viewModel.frame() }
        }
    }
    StormContent(state, onStart = viewModel::start, onInput = viewModel::input, onBack = onBack)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StormContent(state: StormUiState, onStart: () -> Unit, onInput: (Char) -> Unit, onBack: () -> Unit) {
    GameScaffold(stringResource(R.string.game_storm), onBack) {
        when (state.phase) {
            GamePhase.LOADING -> Unit
            GamePhase.LOCKED -> LockedPanel()
            GamePhase.READY -> StartPanel(stringResource(R.string.game_storm_desc), onStart)
            GamePhase.OVER -> GameOverPanel(state.score, state.isNewRecord, onStart, onBack)
            GamePhase.PLAYING -> {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.game_score, state.score), style = MaterialTheme.typography.titleMedium)
                    Text("✦".repeat(state.lives), color = BeamCream, style = MaterialTheme.typography.titleMedium)
                }
                // El cielo ocupa el espacio libre: el teclado siempre queda visible, también en pantallas pequeñas.
                StormSky(state.signals, Modifier.fillMaxWidth().weight(1f).heightIn(min = 160.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.letters.forEach { c ->
                        Button(onClick = { onInput(c) }, modifier = Modifier.size(64.dp)) {
                            Text(c.toString(), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

/** Cielo de tormenta: cada señal es su patrón Morse cayendo hacia el mar. */
@Composable
private fun StormSky(signals: List<SignalView>, modifier: Modifier) {
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(SeaNight, SeaDeep)))
        val seaTop = size.height * 0.88f
        drawRect(SeaMist, Offset(0f, seaTop), Size(size.width, size.height - seaTop))
        val columns = 5
        signals.forEach { s ->
            val x = size.width * ((s.id % columns) + 0.5f) / columns
            val y = s.progress * seaTop
            val dot = size.width * 0.012f
            val dash = dot * 4
            val gap = dot * 1.6f
            val totalWidth = s.pattern.sumOf { (if (it == '.') dot * 2 else dash).toDouble() }.toFloat() + gap * (s.pattern.length - 1)
            // La señal nunca se sale por los lados.
            val margin = dot * 2
            var cursor = (x - totalWidth / 2).coerceIn(margin, (size.width - totalWidth - margin).coerceAtLeast(margin))
            val color = if (s.progress > 0.75f) Coral else BeamCream
            s.pattern.forEach { el ->
                if (el == '.') {
                    drawCircle(color, radius = dot, center = Offset(cursor + dot, y))
                    cursor += dot * 2 + gap
                } else {
                    drawRect(color, Offset(cursor, y - dot * 0.7f), Size(dash, dot * 1.4f))
                    cursor += dash + gap
                }
            }
        }
    }
}
