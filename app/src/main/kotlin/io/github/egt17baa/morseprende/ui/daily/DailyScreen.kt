package io.github.egt17baa.morseprende.ui.daily

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.translator.MorseDisplay
import io.github.egt17baa.morseprende.ui.rewards.RewardPanel

@Composable
fun DailyScreen(viewModel: DailyViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { viewModel.abandon() } }
    DailyContent(state, onPlay = viewModel::play, onAnswer = viewModel::answer, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyContent(state: DailyUiState, onPlay: () -> Unit, onAnswer: (String) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.daily_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (state.phase) {
                DailyPhase.LOADING -> Unit
                DailyPhase.LOCKED -> Text(stringResource(R.string.daily_locked), style = MaterialTheme.typography.bodyLarge)
                DailyPhase.READY -> ReadyPanel(state, onPlay, onAnswer)
                DailyPhase.DONE -> DonePanel(state)
            }
        }
    }
}

@Composable
private fun ReadyPanel(state: DailyUiState, onPlay: () -> Unit, onAnswer: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Text(stringResource(R.string.daily_intro), style = MaterialTheme.typography.bodyLarge)
    OutlinedButton(onClick = onPlay, enabled = !state.isPlaying, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.daily_listen))
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.mission_type_hint)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done, autoCorrectEnabled = false),
        keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onAnswer(text) }),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = { onAnswer(text) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.mission_check))
    }
    if (state.lastWrong) {
        Text(stringResource(R.string.daily_wrong), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.bodyMedium)
    }
    state.hint?.let {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.daily_hint), style = MaterialTheme.typography.labelLarge)
                Text(MorseDisplay.pretty(it), style = MaterialTheme.typography.headlineSmall)
            }
        }
    }
}

@Composable
private fun DonePanel(state: DailyUiState) {
    Text(stringResource(R.string.daily_done), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
    state.message?.let {
        Card(Modifier.fillMaxWidth()) {
            Text(it, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(20.dp))
        }
    }
    state.reward?.let { RewardPanel(it) }
    Text(stringResource(R.string.daily_come_back), style = MaterialTheme.typography.bodyLarge)
    Text(
        androidx.compose.ui.platform.LocalContext.current.resources.getQuantityString(R.plurals.daily_total, state.totalDone, state.totalDone),
        style = MaterialTheme.typography.labelLarge,
    )
}
