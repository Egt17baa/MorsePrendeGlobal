package io.github.egt17baa.morseprende.ui.mission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.settings.KeyerMode

/** Manipulador: un botón grande que se mantiene pulsado, o dos botones punto | raya. */
@Composable
fun KeyerPad(mode: KeyerMode, actions: MissionActions) {
    when (mode) {
        KeyerMode.ONE_BUTTON -> StraightKey(actions)
        KeyerMode.TWO_BUTTONS -> TwoButtonKeys(actions)
    }
}

@Composable
private fun StraightKey(actions: MissionActions) {
    val haptics = LocalHapticFeedback.current
    var pressed by remember { mutableStateOf(false) }
    val description = stringResource(R.string.mission_key)
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .background(if (pressed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface, CircleShape)
                .border(6.dp, MaterialTheme.colorScheme.secondary, CircleShape)
                .semantics { contentDescription = description }
                .pointerInput(Unit) {
                    // Se sigue el dedo hasta que se levanta, aunque se mueva o salga del círculo, y se
                    // consumen los eventos para que la pantalla no se desplace mientras se mantiene
                    // pulsado. Los tiempos salen del propio evento (más precisos que leer el reloj).
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()
                        pressed = true
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        actions.keyDown(down.uptimeMillis)
                        var upTime = down.uptimeMillis
                        try {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                event.changes.forEach { it.consume() }
                                if (change == null || !change.pressed) {
                                    upTime = change?.uptimeMillis ?: upTime
                                    break
                                }
                                upTime = change.uptimeMillis
                            }
                        } finally {
                            pressed = false
                            actions.keyUp(upTime)
                        }
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                description,
                style = MaterialTheme.typography.titleMedium,
                color = if (pressed) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun TwoButtonKeys(actions: MissionActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = actions::dot, modifier = Modifier.weight(1f).height(72.dp)) {
                Text("•", fontSize = 28.sp)
                Text(stringResource(R.string.mission_dot), modifier = Modifier.padding(start = 12.dp))
            }
            Button(onClick = actions::dash, modifier = Modifier.weight(1f).height(72.dp)) {
                Text("▬", fontSize = 28.sp)
                Text(stringResource(R.string.mission_dash), modifier = Modifier.padding(start = 12.dp))
            }
        }
        FilledTonalButton(onClick = actions::nextLetter, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.mission_next_letter))
        }
    }
}
