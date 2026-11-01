package io.github.egt17baa.morseprende.ui.achievements

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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.data.achievements.AchievementsRepository
import io.github.egt17baa.morseprende.game.achievements.Achievements
import io.github.egt17baa.morseprende.ui.rewards.achievementDescription
import io.github.egt17baa.morseprende.ui.rewards.achievementTitle
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.text.DateFormat
import java.util.Date

data class AchievementItem(val id: String, val unlockedAt: Long?)

class AchievementsViewModel(repo: AchievementsRepository) : ViewModel() {
    val state: StateFlow<List<AchievementItem>> = repo.unlocked
        .map { unlocked -> Achievements.ids.map { AchievementItem(it, unlocked[it]) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Achievements.ids.map { AchievementItem(it, null) })
}

@Composable
fun AchievementsScreen(viewModel: AchievementsViewModel, onBack: () -> Unit) {
    val items by viewModel.state.collectAsStateWithLifecycle()
    AchievementsContent(items, onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsContent(items: List<AchievementItem>, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.achievements_title, items.count { it.unlockedAt != null }, items.size)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items.forEach { item ->
                val done = item.unlockedAt != null
                Card(Modifier.fillMaxWidth().alpha(if (done) 1f else 0.5f)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (done) "✦" else "✧", color = BeamCream, fontSize = 28.sp)
                        Column(Modifier.padding(start = 16.dp)) {
                            Text(stringResource(achievementTitle(item.id)), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(achievementDescription(item.id)), style = MaterialTheme.typography.bodySmall)
                            item.unlockedAt?.let {
                                Text(
                                    stringResource(R.string.achievements_unlocked_on, DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it))),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
