package io.github.egt17baa.morseprende.ui.mission

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
    }
    MissionContent(state, actions)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissionContent(state: MissionUiState, actions: MissionActions) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.phase == MissionPhase.EXERCISE && state.exerciseCount > 0) {
                LinearProgressIndicator(
                    progress = { state.exerciseIndex.toFloat() / state.exerciseCount },
                    modifier = Modifier.fillMaxWidth(),
                    strokeCap = StrokeCap.Round,
                )
                Text(
                    stringResource(R.string.mission_progress, state.exerciseIndex + 1, state.exerciseCount),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            when (state.phase) {
                MissionPhase.BRIEFING -> LogbookPanel(state.briefing, stringResource(R.string.mission_start), actions::start)
                MissionPhase.EXERCISE -> ExercisePanel(state, actions)
                MissionPhase.DEBRIEFING -> DebriefingPanel(state, actions)
                MissionPhase.ERROR -> ErrorPanel(actions)
            }
        }
    }
}
