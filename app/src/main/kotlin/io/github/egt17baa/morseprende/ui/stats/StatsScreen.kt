package io.github.egt17baa.morseprende.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.review.Mastery
import io.github.egt17baa.morseprende.ui.practice.titleRes

@Composable
fun StatsScreen(viewModel: StatsViewModel, onPractice: (List<Char>) -> Unit, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    StatsContent(state, onPractice, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsContent(state: StatsUiState, onPractice: (List<Char>) -> Unit, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hub_stats)) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.loaded) return@Column
            if (state.error) {
                Text(stringResource(R.string.hub_error), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            Section(stringResource(R.string.stats_summary)) {
                Text(stringResource(R.string.stats_missions, state.missionsDone, state.missionsTotal))
                Text(stringResource(R.string.stats_stars, state.stars, state.starsMax))
                Text(stringResource(R.string.stats_daily, state.dailyDone))
                Text(stringResource(R.string.stats_days, state.daysPlayed))
                Text(stringResource(R.string.stats_streak, state.streak))
            }
            Section(stringResource(R.string.stats_letters)) {
                if (state.letters.isEmpty()) {
                    Text(stringResource(R.string.stats_letters_empty))
                } else {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.letters.forEach { LetterTile(it) }
                    }
                    Legend()
                    if (state.weakest.isNotEmpty()) {
                        Text(stringResource(R.string.stats_weakest, state.weakest.joinToString(" ")), style = MaterialTheme.typography.titleMedium)
                        Button(onClick = { onPractice(state.weakest) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.stats_practice)) }
                    }
                }
            }
            Section(stringResource(R.string.stats_records)) {
                if (state.records.isEmpty()) {
                    Text(stringResource(R.string.stats_no_records))
                } else {
                    state.records.forEach { r ->
                        Text(stringResource(R.string.stats_record_line, stringResource(r.game.titleRes), stringResource(R.string.practice_record, r.bestScore, r.plays)))
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            content()
        }
    }
}

private val Mastery.Level.labelRes: Int
    get() = when (this) {
        Mastery.Level.NO_DATA -> R.string.stats_level_no_data
        Mastery.Level.LEARNING -> R.string.stats_level_learning
        Mastery.Level.GOOD -> R.string.stats_level_good
        Mastery.Level.MASTERED -> R.string.stats_level_mastered
    }

@Composable
private fun Mastery.Level.colors(): Pair<Color, Color> = with(MaterialTheme.colorScheme) {
    when (this@colors) {
        // Colores sólidos: se distinguen del fondo de la tarjeta en claro, oscuro y alto contraste.
        // «Sin practicar» no tiene relleno, solo borde.
        Mastery.Level.NO_DATA -> Color.Transparent to onSurfaceVariant
        Mastery.Level.LEARNING -> secondary to onSecondary
        Mastery.Level.GOOD -> tertiary to onTertiary
        Mastery.Level.MASTERED -> primary to onPrimary
    }
}

/** Solo «sin practicar» lleva borde (no tiene relleno); del color del texto, para verse en cualquier tema. */
private fun Mastery.Level.border(fg: Color): Color = if (this == Mastery.Level.NO_DATA) fg.copy(alpha = 0.85f) else Color.Transparent

/** Una letra con su nivel: color y, además, de 0 a 3 marcas (el nivel no depende solo del color). */
@Composable
private fun LetterTile(stat: LetterStat) {
    val (bg, fg) = stat.level.colors()
    val description = stringResource(R.string.stats_letter_desc, stat.char.toString(), stringResource(stat.level.labelRes))
    Column(
        Modifier.sizeIn(minWidth = 48.dp).clip(RoundedCornerShape(10.dp)).background(bg)
            .border(1.dp, stat.level.border(fg), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp)
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stat.char.toString(), color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Marks(stat.level.marks, fg)
    }
}

@Composable
private fun Marks(filled: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(3) { i ->
            Box(Modifier.size(6.dp).clip(CircleShape).background(if (i < filled) color else color.copy(alpha = 0.25f)))
        }
    }
}

@Composable
private fun Legend() {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Mastery.Level.entries.forEach { level ->
            val (bg, fg) = level.colors()
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.clip(RoundedCornerShape(6.dp)).background(bg).border(1.dp, level.border(fg), RoundedCornerShape(6.dp)).padding(4.dp)) {
                    Marks(level.marks, fg)
                }
                Text(stringResource(level.labelRes), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
