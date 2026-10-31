package io.github.egt17baa.morseprende.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.game.content.LibraryCard
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.Coral
import io.github.egt17baa.morseprende.ui.theme.SeaNight

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    BackHandler(enabled = state.opened != null) { viewModel.close() }
    LibraryContent(state, onOpen = viewModel::open, onClose = viewModel::close, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(state: LibraryUiState, onOpen: (String) -> Unit, onClose: () -> Unit, onBack: () -> Unit) {
    val opened = state.opened
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(opened?.title ?: stringResource(R.string.library_title)) },
                navigationIcon = {
                    IconButton(onClick = if (opened != null) onClose else onBack) {
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when {
                state.error -> Text(stringResource(R.string.library_error))
                opened != null -> CardDetail(opened)
                else -> {
                    Text(stringResource(R.string.library_intro), style = MaterialTheme.typography.bodyMedium)
                    state.items.forEach { item -> CardRow(item, onOpen) }
                }
            }
        }
    }
}

@Composable
private fun CardRow(item: LibraryItem, onOpen: (String) -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = item.unlocked) { onOpen(item.id) },
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            if (item.unlocked) CardIcon(item.icon) else Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.hub_locked), modifier = Modifier.size(40.dp))
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                item.unlockMissionTitle?.let {
                    Text(stringResource(R.string.library_unlock_with, it), style = MaterialTheme.typography.bodySmall)
                }
            }
            if (item.isNew) Badge { Text(stringResource(R.string.library_new)) }
        }
    }
}

@Composable
private fun CardDetail(card: LibraryCard) {
    CardIcon(card.icon, Modifier.size(72.dp))
    Text(card.body, style = MaterialTheme.typography.bodyLarge)
    Card(colors = CardDefaults.cardColors(containerColor = Coral), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(stringResource(R.string.library_did_you_know), style = MaterialTheme.typography.titleSmall, color = SeaNight)
            Text(card.didYouKnow, style = MaterialTheme.typography.bodyMedium, color = SeaNight)
        }
    }
}

/** Iconos propios de las fichas, dibujados con trazos sencillos. */
@Composable
private fun CardIcon(icon: String, modifier: Modifier = Modifier.size(40.dp)) {
    Canvas(modifier) {
        val stroke = Stroke(width = size.minDimension / 14)
        when (icon) {
            "lighthouse" -> drawLighthouse(stroke)
            "telegraph" -> drawTelegraph(stroke)
            "ship" -> drawShip(stroke)
            "radio" -> drawRadio(stroke)
            "wave" -> drawWave(stroke)
            else -> drawBook(stroke)
        }
    }
}

private fun DrawScope.drawLighthouse(stroke: Stroke) {
    val w = size.width; val h = size.height
    val tower = Path().apply { moveTo(w * 0.38f, h * 0.9f); lineTo(w * 0.44f, h * 0.35f); lineTo(w * 0.56f, h * 0.35f); lineTo(w * 0.62f, h * 0.9f); close() }
    drawPath(tower, BeamCream, style = stroke)
    drawRect(Coral, Offset(w * 0.42f, h * 0.2f), Size(w * 0.16f, h * 0.15f))
    drawLine(BeamCream, Offset(w * 0.6f, h * 0.27f), Offset(w * 0.95f, h * 0.15f), strokeWidth = stroke.width)
}

private fun DrawScope.drawTelegraph(stroke: Stroke) {
    val w = size.width; val h = size.height
    drawRect(BeamCream, Offset(w * 0.1f, h * 0.7f), Size(w * 0.8f, h * 0.15f), style = stroke)
    drawLine(Coral, Offset(w * 0.25f, h * 0.55f), Offset(w * 0.8f, h * 0.45f), strokeWidth = stroke.width * 1.5f)
    drawCircle(BeamCream, radius = w * 0.08f, center = Offset(w * 0.8f, h * 0.42f))
}

private fun DrawScope.drawShip(stroke: Stroke) {
    val w = size.width; val h = size.height
    val hull = Path().apply { moveTo(w * 0.1f, h * 0.6f); lineTo(w * 0.9f, h * 0.6f); lineTo(w * 0.75f, h * 0.82f); lineTo(w * 0.25f, h * 0.82f); close() }
    drawPath(hull, BeamCream, style = stroke)
    drawRect(Coral, Offset(w * 0.35f, h * 0.35f), Size(w * 0.3f, h * 0.25f))
    drawLine(BeamCream, Offset(w * 0.5f, h * 0.35f), Offset(w * 0.5f, h * 0.12f), strokeWidth = stroke.width)
}

private fun DrawScope.drawRadio(stroke: Stroke) {
    val w = size.width; val h = size.height
    drawRect(BeamCream, Offset(w * 0.15f, h * 0.4f), Size(w * 0.7f, h * 0.45f), style = stroke)
    drawCircle(Coral, radius = w * 0.1f, center = Offset(w * 0.35f, h * 0.62f))
    drawLine(BeamCream, Offset(w * 0.6f, h * 0.4f), Offset(w * 0.8f, h * 0.1f), strokeWidth = stroke.width)
}

private fun DrawScope.drawWave(stroke: Stroke) {
    val w = size.width; val h = size.height
    val wave = Path().apply {
        moveTo(0f, h * 0.5f)
        quadraticTo(w * 0.125f, h * 0.2f, w * 0.25f, h * 0.5f)
        quadraticTo(w * 0.375f, h * 0.8f, w * 0.5f, h * 0.5f)
        quadraticTo(w * 0.625f, h * 0.2f, w * 0.75f, h * 0.5f)
        quadraticTo(w * 0.875f, h * 0.8f, w, h * 0.5f)
    }
    drawPath(wave, BeamCream, style = stroke)
    drawCircle(Coral, radius = w * 0.06f, center = Offset(w * 0.25f, h * 0.5f))
}

private fun DrawScope.drawBook(stroke: Stroke) {
    val w = size.width; val h = size.height
    drawRect(BeamCream, Offset(w * 0.12f, h * 0.2f), Size(w * 0.36f, h * 0.6f), style = stroke)
    drawRect(BeamCream, Offset(w * 0.52f, h * 0.2f), Size(w * 0.36f, h * 0.6f), style = stroke)
    drawLine(Coral, Offset(w * 0.2f, h * 0.4f), Offset(w * 0.4f, h * 0.4f), strokeWidth = stroke.width)
    drawCircle(Coral, radius = w * 0.03f, center = Offset(w * 0.62f, h * 0.4f), style = Stroke(stroke.width))
}
