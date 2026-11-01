package io.github.egt17baa.morseprende.ui.hub

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Surface
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.content.MissionType
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.Coral
import io.github.egt17baa.morseprende.ui.theme.SeaMist

data class HubCallbacks(
    val onMission: (String) -> Unit = {},
    val onLibrary: () -> Unit = {},
    val onPractice: () -> Unit = {},
    val onTranslator: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onShop: () -> Unit = {},
    val onAchievements: () -> Unit = {},
)

@Composable
fun HubScreen(viewModel: HubViewModel, callbacks: HubCallbacks) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    HubContent(state, callbacks)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HubContent(state: HubUiState, callbacks: HubCallbacks) {
    Scaffold(bottomBar = { HubBottomBar(state.newCards, callbacks) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            // Los chips se reparten en varias líneas con letra grande. La racha es solo informativa.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AssistChip(
                    onClick = callbacks.onShop,
                    label = { Text(pluralStringResource(R.plurals.hub_balance, state.balance, state.balance)) },
                )
                AssistChip(onClick = callbacks.onAchievements, label = { Text(stringResource(R.string.hub_achievements)) })
                Surface(shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)) {
                    Text(
                        stringResource(R.string.hub_streak, state.streak),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            LighthouseScene(
                state.glow,
                Modifier.fillMaxWidth().height(220.dp),
                lampColor = io.github.egt17baa.morseprende.ui.theme.Cosmetics.lampColor(state.lampColor),
                decorations = state.decorations,
            )
            if (state.error) {
                Text(stringResource(R.string.hub_error), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                return@Column
            }
            val sections = state.chapters.ifEmpty { listOf(ChapterSection(1, state.chapterTitle, state.nodes, true, state.glow)) }
            sections.forEach { section ->
                if (section.unlocked) {
                    Text(
                        stringResource(R.string.hub_chapter, section.id, section.title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    MissionPath(section.nodes, callbacks.onMission)
                } else {
                    // El título va dentro de la tarjeta del candado (una sola vez).
                    LockedChapter(stringResource(R.string.hub_chapter, section.id, section.title), stringResource(R.string.hub_unlock_after, section.id - 1))
                }
            }
            if (sections.none { it.id == 3 }) {
                LockedChapter(stringResource(R.string.hub_chapter, 3, stringResource(R.string.hub_chapter3_title)), stringResource(R.string.hub_coming_soon))
            }
        }
    }
}

/** Posición horizontal (0..1) de cada nodo: un camino que serpentea. */
private val pathPattern = listOf(0.2f, 0.5f, 0.8f, 0.5f)

@Composable
private fun MissionPath(nodes: List<MissionNode>, onMission: (String) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        nodes.forEachIndexed { i, node ->
            val x = pathPattern[i % pathPattern.size]
            val prevX = if (i == 0) null else pathPattern[(i - 1) % pathPattern.size]
            MissionRow(node, x, prevX, onMission)
        }
    }
}

@Composable
private fun MissionRow(node: MissionNode, x: Float, prevX: Float?, onMission: (String) -> Unit) {
    val nodeSize = 56.dp
    val rowHeight = 104.dp
    val trail = MaterialTheme.colorScheme.outline
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(rowHeight)
            .clickable(enabled = node.unlocked) { onMission(node.id) },
    ) {
        val width = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            if (prevX != null) {
                // El tramo empieza bajo el nodo anterior (fuera de esta fila) para que el camino sea continuo.
                val startY = -(rowHeight - 12.dp - nodeSize).toPx()
                val path = Path().apply {
                    moveTo(prevX * size.width, startY)
                    cubicTo(prevX * size.width, size.height * 0.3f, x * size.width, size.height * 0.2f, x * size.width, size.height * 0.45f)
                }
                drawPath(path, trail, style = Stroke(width = 6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f))))
            }
        }
        val left = (width * x - nodeSize / 2)
        Box(Modifier.offset(x = left, y = 12.dp)) { NodeBadge(node, nodeSize) }
        val textOnRight = x < 0.6f
        Column(
            modifier = Modifier
                .offset(x = if (textOnRight) left + nodeSize + 12.dp else 0.dp, y = 16.dp)
                .width(if (textOnRight) width - left - nodeSize - 12.dp else left - 12.dp),
            horizontalAlignment = if (textOnRight) Alignment.Start else Alignment.End,
        ) {
            Text(
                if (node.optional) stringResource(R.string.hub_extra, node.title) else node.title,
                style = MaterialTheme.typography.titleSmall,
                color = if (node.unlocked) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (textOnRight) TextAlign.Start else TextAlign.End,
            )
            if (node.unlocked) Text("✦".repeat(node.stars) + "✧".repeat(3 - node.stars), color = BeamCream)
        }
    }
}

@Composable
private fun NodeBadge(node: MissionNode, size: androidx.compose.ui.unit.Dp) {
    val fill = if (node.unlocked) BeamCream else SeaMist
    val badgeSize = if (node.optional) size * 0.7f else size
    val shape = if (node.type == MissionType.REVIEW) RoundedCornerShape(12.dp) else CircleShape
    val ring = if (node.isNext) Modifier.border(4.dp, Coral, shape) else Modifier
    Box(
        Modifier
            .size(badgeSize)
            .then(if (node.type == MissionType.REVIEW) Modifier.rotate(45f) else Modifier)
            .background(if (node.type == MissionType.FINALE && node.unlocked) Coral else fill, shape)
            .then(ring),
        contentAlignment = Alignment.Center,
    ) {
        if (!node.unlocked) {
            Icon(
                Icons.Filled.Lock,
                contentDescription = stringResource(R.string.hub_locked),
                modifier = if (node.type == MissionType.REVIEW) Modifier.rotate(-45f) else Modifier,
            )
        }
    }
}

@Composable
private fun LockedChapter(title: String, subtitle: String) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun HubBottomBar(newCards: Int, callbacks: HubCallbacks) {
    NavigationBar {
        NavigationBarItem(
            selected = false,
            onClick = callbacks.onLibrary,
            icon = {
                BadgedBox(badge = { if (newCards > 0) Badge(containerColor = Coral) { Text(newCards.toString()) } }) { Text("📖") }
            },
            label = { Text(stringResource(R.string.hub_library)) },
        )
        NavigationBarItem(selected = false, onClick = callbacks.onPractice, icon = { Text("⚡") }, label = { Text(stringResource(R.string.hub_practice)) })
        NavigationBarItem(selected = false, onClick = callbacks.onTranslator, icon = { Text("•▬") }, label = { Text(stringResource(R.string.hub_translator)) })
        NavigationBarItem(selected = false, onClick = callbacks.onSettings, icon = { Text("⚙") }, label = { Text(stringResource(R.string.hub_settings)) })
    }
}
