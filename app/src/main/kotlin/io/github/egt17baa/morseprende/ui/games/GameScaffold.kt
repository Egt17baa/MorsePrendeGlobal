package io.github.egt17baa.morseprende.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.egt17baa.morseprende.R

/**
 * Marco común de los minijuegos. [scrollable]: para los que se juegan con teclado; la columna queda
 * por encima del teclado y se desplaza (no admite hijos con `weight`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScaffold(title: String, onBack: () -> Unit, scrollable: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
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
            Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (scrollable) Modifier.imePadding().verticalScroll(rememberScrollState()) else Modifier)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

/** Al salir de la pantalla o de la app se abandona la partida; mientras se juega, la pantalla no se apaga. */
@Composable
fun GameLifecycle(playing: Boolean, abandon: () -> Unit) {
    DisposableEffect(Unit) { onDispose { abandon() } }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { abandon() }
    val view = LocalView.current
    DisposableEffect(view, playing) {
        view.keepScreenOn = playing
        onDispose { view.keepScreenOn = false }
    }
}

@Composable
fun StartPanel(description: String, onStart: () -> Unit) {
    Text(description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.game_start)) }
}

@Composable
fun LockedPanel() {
    Text(stringResource(R.string.game_locked), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
}

@Composable
fun GameOverPanel(
    score: Int,
    isNewRecord: Boolean,
    onAgain: () -> Unit,
    onBack: () -> Unit,
    detail: String? = null,
    reward: io.github.egt17baa.morseprende.rewards.RewardSummary? = null,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.game_over), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.game_score, score), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                detail?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                if (isNewRecord) Text(stringResource(R.string.game_new_record), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
        io.github.egt17baa.morseprende.ui.rewards.RewardPanel(reward)
        Button(onClick = onAgain, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.game_again)) }
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.back)) }
    }
}
