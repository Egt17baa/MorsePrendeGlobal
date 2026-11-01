package io.github.egt17baa.morseprende.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R

@Composable
fun InterceptScreen(viewModel: InterceptViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GameLifecycle(state.phase == GamePhase.PLAYING, viewModel::abandon)
    InterceptContent(state, viewModel::start, viewModel::repeat, viewModel::answer, viewModel::next, onBack)
}

@Composable
fun InterceptContent(
    state: InterceptUiState,
    onStart: () -> Unit,
    onRepeat: () -> Unit,
    onAnswer: (String) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit,
) {
    GameScaffold(stringResource(R.string.game_intercept), onBack, scrollable = true) {
        when (state.phase) {
            GamePhase.LOADING -> Unit
            GamePhase.LOCKED -> LockedPanel()
            GamePhase.READY -> StartPanel(stringResource(R.string.game_intercept_desc), onStart)
            GamePhase.OVER -> GameOverPanel(
                state.score, state.isNewRecord, onStart, onBack,
                detail = stringResource(R.string.game_intercept_max, state.maxWpm),
                reward = state.reward,
            )
            GamePhase.PLAYING -> {
                var text by rememberSaveable(state.score, state.strikes) { mutableStateOf("") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(stringResource(R.string.game_wpm, state.wpm), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.game_strikes, state.strikes, 3), style = MaterialTheme.typography.titleMedium)
                }
                Text(stringResource(R.string.game_score, state.score), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                val feedback = state.feedback
                if (feedback == null) {
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
