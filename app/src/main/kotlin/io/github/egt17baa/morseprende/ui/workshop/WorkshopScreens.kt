package io.github.egt17baa.morseprende.ui.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.translator.MorseDisplay
import io.github.egt17baa.morseprende.ui.mission.KeyerActions
import io.github.egt17baa.morseprende.ui.mission.KeyerPad
import io.github.egt17baa.morseprende.ui.theme.Cosmetics
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkshopScaffold(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

// ---- Envío libre ----

@Composable
fun FreeSendScreen(viewModel: FreeSendViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // El tono se abre al entrar (o al volver a la app; ON_START llega también al entrar) y se cierra
    // al salir o pasar a segundo plano: nunca queda sonando.
    DisposableEffect(Unit) { onDispose { viewModel.leave() } }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.enter() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.leave() }
    LaunchedEffect(Unit) {
        while (true) {
            delay(TICK_MS)
            viewModel.tick()
        }
    }
    FreeSendContent(state, viewModel, viewModel::deleteLast, viewModel::clear, onBack)
}

@Composable
fun FreeSendContent(state: FreeSendUiState, keyer: KeyerActions, onDelete: () -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    WorkshopScaffold(stringResource(R.string.workshop_send), onBack) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().heightIn(min = 120.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.text.isEmpty()) {
                    Text(stringResource(R.string.workshop_send_hint), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(state.text, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Text(
            MorseDisplay.pretty(state.pending).ifEmpty { " " },
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.wpm > 0) {
            Text(stringResource(R.string.workshop_speed, state.wpm), style = MaterialTheme.typography.labelLarge, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        KeyerPad(state.keyerMode, keyer, Cosmetics.keyerRing(state.keyerSkin), haptics = state.haptics)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.mission_delete)) }
            TextButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.workshop_clear)) }
        }
    }
}

// ---- Letras a la carta ----

@Composable
fun LetterDrillScreen(viewModel: LetterDrillViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { viewModel.abandon() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stop() }
    LetterDrillContent(state, viewModel::toggle, viewModel::start, viewModel::replay, viewModel::answer, viewModel::stop, onBack)
}

@Composable
fun LetterDrillContent(
    state: LetterDrillUiState,
    onToggle: (Char) -> Unit,
    onStart: () -> Unit,
    onReplay: () -> Unit,
    onAnswer: (Char) -> Unit,
    onChange: () -> Unit,
    onBack: () -> Unit,
) {
    WorkshopScaffold(stringResource(R.string.workshop_letters), onBack) {
        when (state.phase) {
            DrillPhase.LOADING -> Unit
            DrillPhase.LOCKED -> Text(stringResource(R.string.workshop_letters_locked), style = MaterialTheme.typography.bodyLarge)
            DrillPhase.SETUP -> {
                Text(stringResource(R.string.workshop_letters_desc), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.workshop_letters_pick), style = MaterialTheme.typography.titleMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.available.forEach { c ->
                        FilterChip(selected = c in state.selected, onClick = { onToggle(c) }, label = { Text(c.toString(), style = MaterialTheme.typography.titleMedium) })
                    }
                }
                Button(onClick = onStart, enabled = state.selected.size >= 2, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.workshop_letters_start))
                }
            }
            DrillPhase.PLAYING -> {
                val feedback = state.feedback
                Text(
                    when {
                        feedback == null -> stringResource(R.string.workshop_letters_which)
                        feedback.correct -> stringResource(R.string.mission_correct)
                        else -> stringResource(R.string.mission_wrong, feedback.expected.toString())
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = when {
                        feedback == null -> MaterialTheme.colorScheme.onSurface
                        feedback.correct -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.secondary
                    },
                )
                OutlinedButton(onClick = onReplay, enabled = feedback == null, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.mission_listen_again))
                }
                // Teclado solo con las letras elegidas, grande para tocarlo sin mirar.
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.available.filter { it in state.selected }.forEach { c ->
                        FilledTonalButton(onClick = { onAnswer(c) }, enabled = feedback == null, modifier = Modifier.sizeIn(minWidth = 72.dp, minHeight = 64.dp)) {
                            Text(c.toString(), fontSize = 28.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                state.stats.forEach { (c, score) ->
                    Text(stringResource(R.string.workshop_letters_score, c.toString(), score.right, score.total), style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = onChange, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.workshop_letters_change)) }
            }
        }
    }
}

private const val TICK_MS = 20L
