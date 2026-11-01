package io.github.egt17baa.morseprende.ui.mission

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.core.MorseAlphabet
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.game.mission.Exercise
import io.github.egt17baa.morseprende.translator.MorseDisplay
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.SeaMist
import kotlinx.coroutines.delay

private const val TICK_MS = 20L

/** Tarjeta de "cuaderno de bitácora" para briefings y debriefings. */
@Composable
fun LogbookPanel(text: String, buttonLabel: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.mission_logbook), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
            Text(text, style = MaterialTheme.typography.bodyLarge)
        }
    }
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) { Text(buttonLabel) }
}

@Composable
fun ExercisePanel(state: MissionUiState, actions: MissionActions) {
    when (val e = state.exercise) {
        is Exercise.Listen -> ListenPanel(e, actions)
        is Exercise.ReceiveChoice -> ChoicePanel(e, state, actions)
        is Exercise.ReceiveTyped -> TypedPanel(state, actions)
        is Exercise.Send -> SendPanel(e, state, actions)
        is Exercise.FinalMessage -> FinalMessagePanel(e, state, actions)
        null -> Unit
    }
    state.feedback?.let { FeedbackPanel(it, actions) }
}

private fun prosignLabel(c: Char): Int? = when (c) {
    '=' -> R.string.prosign_bt
    '+' -> R.string.prosign_ar
    else -> null
}

@Composable
private fun ListenPanel(e: Exercise.Listen, actions: MissionActions) {
    Text(stringResource(R.string.mission_listen_title), style = MaterialTheme.typography.titleLarge)
    e.chars.forEach { c ->
        Card(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(c.toString(), fontSize = 56.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Column(Modifier.padding(start = 24.dp)) {
                    Text(MorseDisplay.pretty(MorseAlphabet.codeFor(c).orEmpty()), style = MaterialTheme.typography.headlineMedium)
                    // «=» y «+» se enseñan como los prosignos BT y AR.
                    prosignLabel(c)?.let { Text(stringResource(it), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary) }
                }
            }
        }
    }
    if ('Ñ' in e.chars) {
        Text(stringResource(R.string.mission_enye_notice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
    }
    OutlinedButton(onClick = actions::replay, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.mission_listen_again)) }
    Button(onClick = actions::continueListen, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.mission_continue)) }
}

@Composable
private fun ChoicePanel(e: Exercise.ReceiveChoice, state: MissionUiState, actions: MissionActions) {
    Text(stringResource(state.byChannel(R.string.mission_choose, R.string.mission_choose_light, R.string.mission_choose_vibration)), style = MaterialTheme.typography.titleLarge)
    ReplayButton(state, actions)
    e.options.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { option ->
                Button(
                    onClick = { actions.answer(option) },
                    enabled = state.feedback == null,
                    modifier = Modifier.weight(1f),
                ) { Text(option, fontSize = 28.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun TypedPanel(state: MissionUiState, actions: MissionActions) {
    var text by rememberSaveable(state.exerciseIndex) { mutableStateOf("") }
    Text(stringResource(state.byChannel(R.string.mission_type_title, R.string.mission_type_title_light, R.string.mission_type_title_vibration)), style = MaterialTheme.typography.titleLarge)
    ReplayButton(state, actions)
    AnswerField(text, { text = it }, enabled = state.feedback == null) { actions.answer(text) }
}

@Composable
private fun AnswerField(text: String, onText: (String) -> Unit, enabled: Boolean, onSubmit: () -> Unit) {
    OutlinedTextField(
        value = text,
        onValueChange = onText,
        label = { Text(stringResource(R.string.mission_type_hint)) },
        singleLine = true,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = onSubmit, enabled = enabled && text.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.mission_check))
    }
}

@Composable
private fun SendPanel(e: Exercise.Send, state: MissionUiState, actions: MissionActions) {
    LaunchedEffect(state.exerciseIndex) {
        while (true) {
            delay(TICK_MS)
            actions.tick()
        }
    }
    Text(stringResource(R.string.mission_send_title), style = MaterialTheme.typography.titleLarge)
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(e.target, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            val showHelp = state.showPattern && e.target.any { it in state.newChars }
            if (showHelp) {
                Text(MorseDisplay.pretty(MorseCodec.encode(e.target).morse), style = MaterialTheme.typography.titleLarge)
            }
        }
    }
    Text(
        MorseDisplay.pretty(state.keyerPending).ifEmpty { " " },
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(
        stringResource(R.string.mission_sent, state.sentText),
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.mission_attempts, state.attemptsLeft), style = MaterialTheme.typography.labelLarge)
    if (state.feedback == null) {
        KeyerPad(state.keyerMode, actions, io.github.egt17baa.morseprende.ui.theme.Cosmetics.keyerRing(state.keyerSkin), haptics = state.vibrationEnabled)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = actions::deleteLast, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.mission_delete)) }
            TextButton(onClick = actions::replay, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.mission_listen_again)) }
        }
        // Sin nada transmitido no se puede enviar: un toque accidental no gasta un intento.
        Button(
            onClick = actions::submitSend,
            enabled = state.sentText.isNotBlank() || state.keyerPending.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.mission_submit)) }
    }
}

@Composable
private fun FinalMessagePanel(e: Exercise.FinalMessage, state: MissionUiState, actions: MissionActions) {
    var text by rememberSaveable(state.exerciseIndex) { mutableStateOf("") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.mission_incoming, e.message.from),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            when (state.hintLevel) {
                0 -> Unit
                1 -> Text(stringResource(R.string.mission_hint, e.message.text.map { if (it == ' ') ' ' else '_' }.joinToString(" ")))
                else -> Text(stringResource(R.string.mission_hint, MorseDisplay.pretty(MorseCodec.encode(e.message.text).morse)))
            }
        }
    }
    ReplayButton(state, actions)
    AnswerField(text, { text = it }, enabled = state.feedback == null) { actions.answer(text) }
}

@Composable
private fun FeedbackPanel(feedback: Feedback, actions: MissionActions) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (feedback.correct) stringResource(R.string.mission_correct) else stringResource(R.string.mission_wrong, feedback.expected),
                style = MaterialTheme.typography.titleLarge,
                color = if (feedback.correct) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            )
            Button(onClick = actions::next, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.mission_continue)) }
        }
    }
}

@Composable
fun DebriefingPanel(state: MissionUiState, actions: MissionActions) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(3) { i -> LighthouseFlash(lit = i < state.stars) }
    }
    if (state.stars > state.previousBest && state.previousBest > 0) {
        Text(
            stringResource(R.string.mission_new_best),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    io.github.egt17baa.morseprende.ui.rewards.RewardPanel(state.reward)
    LogbookPanel(state.debriefing, stringResource(R.string.mission_back_to_lighthouse), actions::exit)
}

/** Una "estrella" con forma de destello de faro. */
@Composable
private fun LighthouseFlash(lit: Boolean) {
    Canvas(Modifier.size(64.dp).padding(4.dp)) {
        val color = if (lit) BeamCream else SeaMist
        drawCircle(Brush.radialGradient(listOf(color, Color.Transparent)), radius = size.minDimension / 2)
        drawCircle(color, radius = size.minDimension / 6, center = Offset(size.width / 2, size.height / 2))
    }
}

@Composable
fun ErrorPanel(actions: MissionActions) {
    Text(stringResource(R.string.mission_error), style = MaterialTheme.typography.titleMedium)
    Button(onClick = actions::exit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.mission_back_to_lighthouse)) }
}

/** Texto según el canal por el que llega la señal (si el móvil no vibra, se oye). */
private fun MissionUiState.byChannel(sound: Int, light: Int, vibration: Int): Int = when (channel) {
    io.github.egt17baa.morseprende.game.content.Channel.LIGHT -> light
    io.github.egt17baa.morseprende.game.content.Channel.VIBRATION -> if (channelFallback || !vibrationEnabled) sound else vibration
    io.github.egt17baa.morseprende.game.content.Channel.SOUND -> sound
}

@Composable
private fun ReplayButton(state: MissionUiState, actions: MissionActions) {
    OutlinedButton(onClick = actions::replay, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(state.byChannel(R.string.mission_replay, R.string.mission_replay_light, R.string.mission_replay_vibration)))
    }
}
