package io.github.egt17baa.morseprende.translator

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.playback.OutputKind
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TranslatorScreen(viewModel: TranslatorViewModel, onBack: () -> Unit) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    DisposableEffect(Unit) { onDispose { viewModel.stop() } }
    // Salir de la app (botón de inicio, bloquear el móvil) también detiene la linterna y la vibración.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.stop() }
    // Mientras suena, la pantalla no se apaga (el destello de pantalla dejaría de verse).
    val view = LocalView.current
    DisposableEffect(view, s.isPlaying) {
        view.keepScreenOn = s.isPlaying
        onDispose { view.keepScreenOn = false }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.translator_title)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                InputCard(s, viewModel::onInputChange)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    FilledTonalButton(onClick = viewModel::swapDirection) {
                        Text("⇅  " + stringResource(R.string.translator_swap))
                    }
                }
                OutputCard(s)
                if (s.unsupported.isNotEmpty()) {
                    Notice(stringResource(R.string.translator_unsupported, s.unsupported.joinToString(" ")))
                }
                if (s.speedLimitedForFlashes) {
                    Notice(stringResource(R.string.translator_speed_limited))
                }
                s.failedOutputs.forEach { kind ->
                    Notice(stringResource(R.string.output_failed, stringResource(kind.label)))
                }
                SpeedControl(s.charWpm, viewModel::setSpeed)
                OutputChips(s, viewModel::toggleOutput)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = if (s.isPlaying) viewModel::stop else viewModel::play,
                        enabled = s.isPlaying || s.canPlay,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(if (s.isPlaying) R.string.translator_stop else R.string.translator_play))
                    }
                    OutlinedButton(onClick = viewModel::clear, modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.translator_clear))
                    }
                }
            }
        }

        if (s.screenLit) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(BeamCream),
            )
        }
    }

    if (s.showFlashWarning) {
        AlertDialog(
            onDismissRequest = viewModel::dismissFlashWarning,
            title = { Text(stringResource(R.string.flash_warning_title)) },
            text = { Text(stringResource(R.string.flash_warning_text)) },
            confirmButton = {
                TextButton(onClick = viewModel::acceptFlashWarning) { Text(stringResource(R.string.flash_warning_accept)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissFlashWarning) { Text(stringResource(R.string.flash_warning_cancel)) }
            },
        )
    }
}

@Composable
private fun InputCard(s: TranslatorState, onInputChange: (String) -> Unit) {
    val isText = s.direction == Direction.TEXT_TO_MORSE
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            OutlinedTextField(
                value = s.input,
                onValueChange = onInputChange,
                label = {
                    Text(stringResource(if (isText) R.string.translator_input_text else R.string.translator_input_morse))
                },
                supportingText = {
                    Text(
                        if (isText) {
                            stringResource(R.string.translator_counter, s.input.length, TranslatorViewModel.MAX_INPUT)
                        } else {
                            stringResource(R.string.translator_morse_hint)
                        },
                    )
                },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun OutputCard(s: TranslatorState) {
    val isMorse = s.direction == Direction.TEXT_TO_MORSE
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(if (isMorse) R.string.translator_output_morse else R.string.translator_output_text),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        scope.launch { clipboard.setClipEntry(ClipEntry(ClipData.newPlainText("morse", s.output))) }
                    },
                    enabled = s.output.isNotEmpty(),
                ) { Text(stringResource(R.string.translator_copy)) }
            }
            Text(
                if (isMorse) MorseDisplay.pretty(s.output) else s.output,
                style = if (isMorse) {
                    MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OutputChips(s: TranslatorState, onToggle: (OutputKind) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.translator_outputs), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutputKind.entries
                .filter { it != OutputKind.TORCH || s.torchAvailable }
                .forEach { kind ->
                    FilterChip(
                        selected = kind in s.outputs,
                        onClick = { onToggle(kind) },
                        label = { Text(stringResource(kind.label)) },
                    )
                }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
}

private val OutputKind.label: Int
    get() = when (this) {
        OutputKind.SOUND -> R.string.output_sound
        OutputKind.VIBRATION -> R.string.output_vibration
        OutputKind.TORCH -> R.string.output_torch
        OutputKind.SCREEN -> R.string.output_screen
    }

@Composable
private fun SpeedControl(charWpm: Int, onSpeedChange: (Int) -> Unit) {
    Column {
        Text(stringResource(R.string.translator_speed, charWpm), style = MaterialTheme.typography.titleSmall)
        Slider(
            value = charWpm.toFloat(),
            onValueChange = { onSpeedChange(it.roundToInt()) },
            valueRange = MorseTiming.MIN_WPM.toFloat()..MorseTiming.MAX_WPM.toFloat(),
            steps = MorseTiming.MAX_WPM - MorseTiming.MIN_WPM - 1,
        )
    }
}
