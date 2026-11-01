package io.github.egt17baa.morseprende.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.games.NightChannel
import io.github.egt17baa.morseprende.game.games.NightWatchGame

@Composable
fun NightWatchScreen(viewModel: NightWatchViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GameLifecycle(state.phase == GamePhase.PLAYING, viewModel::abandon)
    NightWatchContent(state, viewModel::start, viewModel::repeat, viewModel::answer, viewModel::next, onBack)
}

@Composable
fun NightWatchContent(
    state: NightWatchUiState,
    onStart: () -> Unit,
    onRepeat: () -> Unit,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    GameScaffold(stringResource(R.string.game_night), onBack) {
        when (state.phase) {
            GamePhase.LOADING -> Unit
            GamePhase.LOCKED -> LockedPanel()
            GamePhase.READY -> StartPanel(stringResource(R.string.game_night_desc), onStart)
            GamePhase.OVER -> GameOverPanel(
                state.score, state.isNewRecord, onStart, onBack,
                detail = stringResource(R.string.game_night_reached, NightWatchGame.clock(state.maxHour)),
                reward = state.reward,
            )
            GamePhase.PLAYING -> {
                var text by rememberSaveable(state.score, state.strikes) { mutableStateOf("") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("⏱ " + NightWatchGame.clock(state.hour), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.game_wpm, state.wpm), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.game_strikes, state.strikes, NightWatchGame.STRIKES), style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(R.string.game_score, state.score), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                val feedback = state.feedback
                if (feedback == null) {
                    when (state.channel) {
                        NightChannel.LIGHT -> {
                            Text(stringResource(R.string.mission_channel_light), color = MaterialTheme.colorScheme.secondary)
                            FlashingLighthouse(state.lampOn, MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth().height(120.dp).testTag("night_lamp"))
                        }
                        NightChannel.VIBRATION -> Text(
                            stringResource(if (state.vibrationFallback) R.string.mission_channel_vibration_fallback else R.string.mission_channel_vibration),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        NightChannel.SOUND -> Unit
                    }
                    OutlinedButton(onClick = onRepeat, enabled = state.canRepeat, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.game_repeat))
                    }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text(stringResource(R.string.mission_type_hint)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (text.isNotBlank()) onAnswer(text) }),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(onClick = { onAnswer(text) }, enabled = text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.mission_check))
                    }
                } else {
                    Text(
                        if (feedback.correct) stringResource(R.string.mission_correct) else stringResource(R.string.mission_wrong, feedback.expected),
                        style = MaterialTheme.typography.titleLarge,
                        color = if (feedback.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    )
                    Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.mission_continue)) }
                }
            }
        }
    }
}
