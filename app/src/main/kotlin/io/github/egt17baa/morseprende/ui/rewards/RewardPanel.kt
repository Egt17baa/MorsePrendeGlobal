package io.github.egt17baa.morseprende.ui.rewards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.rewards.RewardSummary

/** Lo ganado al terminar: Destellos, bono diario y logros nuevos. */
@Composable
fun RewardPanel(reward: RewardSummary?) {
    if (reward == null) return
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(pluralStringResource(R.plurals.reward_destellos, reward.destellos, reward.destellos), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            if (reward.dailyBonus) Text(stringResource(R.string.reward_daily), style = MaterialTheme.typography.bodyMedium)
            reward.newAchievements.forEach { id ->
                Text(stringResource(R.string.reward_achievement, stringResource(achievementTitle(id))), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

fun achievementTitle(id: String): Int = when (id) {
    "first_sos" -> R.string.ach_first_sos
    "three_flashes" -> R.string.ach_three_flashes
    "keeper" -> R.string.ach_keeper
    "perfectionist" -> R.string.ach_perfectionist
    "storm_rider" -> R.string.ach_storm_rider
    "quick_eye" -> R.string.ach_quick_eye
    "sharp_ear" -> R.string.ach_sharp_ear
    "speed_20" -> R.string.ach_speed_20
    "steady" -> R.string.ach_steady
    "collector" -> R.string.ach_collector
    "mole_hunter" -> R.string.ach_mole_hunter
    else -> R.string.ach_reader
}

fun achievementDescription(id: String): Int = when (id) {
    "first_sos" -> R.string.ach_first_sos_desc
    "three_flashes" -> R.string.ach_three_flashes_desc
    "keeper" -> R.string.ach_keeper_desc
    "perfectionist" -> R.string.ach_perfectionist_desc
    "storm_rider" -> R.string.ach_storm_rider_desc
    "quick_eye" -> R.string.ach_quick_eye_desc
    "sharp_ear" -> R.string.ach_sharp_ear_desc
    "speed_20" -> R.string.ach_speed_20_desc
    "steady" -> R.string.ach_steady_desc
    "collector" -> R.string.ach_collector_desc
    "mole_hunter" -> R.string.ach_mole_hunter_desc
    else -> R.string.ach_reader_desc
}
