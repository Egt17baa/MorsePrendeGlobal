package io.github.egt17baa.morseprende.ui.mission

import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.layout.height
import io.github.egt17baa.morseprende.game.content.Channel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.mission.Exercise

/** Acciones que la pantalla de misión puede pedir. */
interface MissionActions {
    fun start()
    fun replay()
    fun continueListen()
    fun answer(text: String)
    fun keyDown(atMs: Long)
    fun keyUp(atMs: Long)
    fun tick()
    fun dot()
    fun dash()
    fun nextLetter()
    fun deleteLast()
    fun submitSend()
    fun next()
    fun exit()
    fun useSound()
    fun resume()
}

@Composable
fun MissionScreen(viewModel: MissionViewModel, onExit: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Salir de la pantalla o de la app detiene el sonido y el tono del manipulador (no se guarda nada).
    DisposableEffect(Unit) { onDispose { viewModel.abandon() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.abandon() }
    val view = LocalView.current
    val keepOn = state.isPlaying || state.exercise is Exercise.Send
    DisposableEffect(view, keepOn) {
        view.keepScreenOn = keepOn
        onDispose { view.keepScreenOn = false }
    }
    val actions = object : MissionActions {
        override fun start() = viewModel.start()
        override fun replay() = viewModel.replay()
        override fun continueListen() = viewModel.continueListen()
        override fun answer(text: String) = viewModel.answer(text)
        override fun keyDown(atMs: Long) = viewModel.keyDown(atMs)
        override fun keyUp(atMs: Long) = viewModel.keyUp(atMs)
        override fun tick() = viewModel.tick()
        override fun dot() = viewModel.dot()
        override fun dash() = viewModel.dash()
        override fun nextLetter() = viewModel.nextLetter()
        override fun deleteLast() = viewModel.deleteLast()
        override fun submitSend() = viewModel.submitSend()
        override fun next() = viewModel.next()
        override fun exit() = onExit()
        override fun useSound() = viewModel.useSound()
        override fun resume() = viewModel.resume()
    }
    MissionContent(state, actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionContent(state: MissionUiState, actions: MissionActions) {
    // Cada capítulo tiene su paleta (Sombra: oliva y ámbar; Estelar: violeta y magenta).
    io.github.egt17baa.morseprende.ui.theme.ChapterTheme(state.chapterId) { MissionScaffold(state, actions) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MissionScaffold(state: MissionUiState, actions: MissionActions) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.title) },
                navigationIcon = {
                    IconButton(onClick = actions::exit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        // La cabecera (progreso, reloj y canal, con el faro pequeño) queda fija y solo se desplaza
        // el panel de abajo: al abrir el teclado el faro y el contrarreloj siguen a la vista.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.phase == MissionPhase.EXERCISE && state.exerciseCount > 0) {
                Column(Modifier.fillMaxWidth().testTag("mission_header"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(
                        progress = { state.exerciseIndex.toFloat() / state.exerciseCount },
                        modifier = Modifier.fillMaxWidth(),
                        strokeCap = StrokeCap.Round,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            stringResource(R.string.mission_progress, state.exerciseIndex + 1, state.exerciseCount),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.weight(1f),
                        )
                        val receiving = state.exercise is Exercise.ReceiveChoice || state.exercise is Exercise.ReceiveTyped || state.exercise is Exercise.FinalMessage
                        if (state.noisy && receiving) {
                            Text(stringResource(R.string.mission_noise), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        }
                        state.secondsLeft?.let {
                            Text(stringResource(R.string.mission_seconds_left, it), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                ChannelBanner(state, actions)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("mission_scroll")
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (state.phase) {
                    MissionPhase.BRIEFING -> {
                        val resumeAt = state.resumeAt
                        if (resumeAt != null && state.exerciseCount > 0) {
                            // Partida a medias: se sigue por donde se dejó, o se empieza de nuevo.
                            LogbookPanel(state.briefing, stringResource(R.string.mission_resume, resumeAt + 1, state.exerciseCount), actions::resume)
                            androidx.compose.material3.OutlinedButton(onClick = actions::start, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.mission_start_over))
                            }
                        } else {
                            LogbookPanel(state.briefing, stringResource(R.string.mission_start), actions::start)
                        }
                    }
                    MissionPhase.EXERCISE -> ExercisePanel(state, actions)
                    MissionPhase.DEBRIEFING -> DebriefingPanel(state, actions)
                    MissionPhase.ERROR -> ErrorPanel(actions)
                }
            }
        }
    }
}

/** Canal de la misión: el faro pequeño para la luz y avisos para la vibración. */
@Composable
private fun ChannelBanner(state: MissionUiState, actions: MissionActions) {
    val receiving = state.exercise is Exercise.ReceiveChoice || state.exercise is Exercise.ReceiveTyped || state.exercise is Exercise.FinalMessage
    if (!receiving) return
    when (state.channel) {
        Channel.LIGHT -> {
            Text(stringResource(R.string.mission_channel_light), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            io.github.egt17baa.morseprende.ui.games.FlashingLighthouse(
                state.lampOn,
                io.github.egt17baa.morseprende.ui.theme.Cosmetics.lampColor(state.lampColor),
                Modifier.fillMaxWidth().height(120.dp).testTag("mission_lamp"),
            )
        }
        Channel.VIBRATION -> {
            Text(
                stringResource(if (state.channelFallback) R.string.mission_channel_vibration_fallback else R.string.mission_channel_vibration),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            // Si el móvil vibra «en teoría» pero no se nota (ahorro de batería, vibración
            // desactivada…), el jugador puede pasar a sonido: la misión nunca se queda imposible.
            if (!state.channelFallback) {
                androidx.compose.material3.TextButton(onClick = actions::useSound) {
                    Text(stringResource(R.string.mission_vibration_use_sound))
                }
            }
        }
        Channel.SOUND -> Unit
    }
}
