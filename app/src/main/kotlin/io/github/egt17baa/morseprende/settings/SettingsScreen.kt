package io.github.egt17baa.morseprende.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.core.MorseTiming
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onBack: () -> Unit, onLicenses: () -> Unit = {}) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionTitle(stringResource(R.string.settings_speed_section))
            Text(stringResource(R.string.settings_char_wpm, s.charWpm))
            Slider(
                value = s.charWpm.toFloat(),
                onValueChange = { viewModel.setCharWpm(it.roundToInt()) },
                valueRange = MorseTiming.MIN_WPM.toFloat()..MorseTiming.MAX_WPM.toFloat(),
                steps = MorseTiming.MAX_WPM - MorseTiming.MIN_WPM - 1,
            )
            Help(stringResource(R.string.settings_char_wpm_help))
            Text(stringResource(R.string.settings_effective_wpm, s.effectiveWpm))
            if (s.charWpm > MorseTiming.MIN_WPM) {
                Slider(
                    value = s.effectiveWpm.toFloat(),
                    onValueChange = { viewModel.setEffectiveWpm(it.roundToInt()) },
                    valueRange = MorseTiming.MIN_WPM.toFloat()..s.charWpm.toFloat(),
                    steps = (s.charWpm - MorseTiming.MIN_WPM - 1).coerceAtLeast(0),
                )
            }
            Help(stringResource(R.string.settings_effective_wpm_help))

            SectionTitle(stringResource(R.string.settings_sound_section))
            Text(stringResource(R.string.settings_tone, s.toneHz))
            Slider(
                value = s.toneHz.toFloat(),
                onValueChange = { viewModel.setToneHz((it / 10).roundToInt() * 10) },
                valueRange = AppSettings.MIN_TONE_HZ.toFloat()..AppSettings.MAX_TONE_HZ.toFloat(),
            )
            Text(stringResource(R.string.settings_volume, (s.volume * 100).roundToInt()))
            Slider(value = s.volume, onValueChange = viewModel::setVolume)
            Button(onClick = viewModel::testSound) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.settings_test_sound), modifier = Modifier.padding(start = 8.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_vibration), modifier = Modifier.weight(1f))
                Switch(checked = s.vibration, onCheckedChange = viewModel::setVibration)
            }
            Help(stringResource(R.string.settings_vibration_help))

            SectionTitle(stringResource(R.string.settings_keyer_section))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = s.keyerMode == KeyerMode.ONE_BUTTON,
                    onClick = { viewModel.setKeyerMode(KeyerMode.ONE_BUTTON) },
                    label = { Text(stringResource(R.string.settings_keyer_one)) },
                )
                FilterChip(
                    selected = s.keyerMode == KeyerMode.TWO_BUTTONS,
                    onClick = { viewModel.setKeyerMode(KeyerMode.TWO_BUTTONS) },
                    label = { Text(stringResource(R.string.settings_keyer_two)) },
                )
            }
            Help(stringResource(R.string.settings_keyer_help))

            SectionTitle(stringResource(R.string.settings_accessibility_section))
            Text(stringResource(R.string.settings_text_size))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val labels = listOf(R.string.settings_text_normal, R.string.settings_text_large, R.string.settings_text_huge)
                AppSettings.TEXT_SCALES.zip(labels).forEach { (scale, label) ->
                    FilterChip(
                        selected = kotlin.math.abs(s.textScale - scale) < 0.01f,
                        onClick = { viewModel.setTextScale(scale) },
                        label = { Text(stringResource(label)) },
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_high_contrast), modifier = Modifier.weight(1f))
                Switch(
                    checked = s.highContrast,
                    onCheckedChange = viewModel::setHighContrast,
                    modifier = Modifier.testTag("settings_high_contrast"),
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.settings_limit_flashes), modifier = Modifier.weight(1f))
                Switch(checked = s.limitFlashes, onCheckedChange = viewModel::setLimitFlashes)
            }
            Help(stringResource(R.string.settings_limit_flashes_help))

            SectionTitle(stringResource(R.string.settings_progress_section))
            val resetState by viewModel.resetState.collectAsStateWithLifecycle()
            ResetProgressButton(enabled = resetState != SettingsViewModel.ResetState.RUNNING, onConfirm = viewModel::resetProgress)
            if (resetState == SettingsViewModel.ResetState.DONE) {
                Text(stringResource(R.string.settings_reset_done), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
            }

            SectionTitle(stringResource(R.string.settings_about_section))
            val context = androidx.compose.ui.platform.LocalContext.current
            val version = remember {
                runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
            }
            Help(stringResource(R.string.settings_about_version, version))
            Help(stringResource(R.string.settings_about_font))
            Help(stringResource(R.string.settings_about_privacy))
            androidx.compose.material3.TextButton(onClick = onLicenses) { Text(stringResource(R.string.settings_licenses)) }
        }
    }
}

/** «Borrar progreso» siempre pide confirmación. */
@Composable
private fun ResetProgressButton(enabled: Boolean, onConfirm: () -> Unit) {
    var asking by remember { mutableStateOf(false) }
    androidx.compose.material3.OutlinedButton(onClick = { asking = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.settings_reset), color = MaterialTheme.colorScheme.error)
    }
    Help(stringResource(R.string.settings_reset_help))
    if (asking) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { asking = false },
            title = { io.github.egt17baa.morseprende.ui.theme.DialogText(stringResource(R.string.settings_reset_title)) },
            text = { io.github.egt17baa.morseprende.ui.theme.DialogText(stringResource(R.string.settings_reset_body)) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { asking = false; onConfirm() }) {
                    Text(stringResource(R.string.settings_reset_confirm), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { asking = false }) { Text(stringResource(R.string.settings_reset_cancel)) }
            },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 12.dp),
    )
}

@Composable
private fun Help(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
