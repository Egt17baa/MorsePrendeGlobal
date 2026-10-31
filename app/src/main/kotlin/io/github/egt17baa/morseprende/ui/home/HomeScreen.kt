package io.github.egt17baa.morseprende.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.egt17baa.morseprende.R
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.Coral
import io.github.egt17baa.morseprende.ui.theme.Sand
import io.github.egt17baa.morseprende.ui.theme.SeaDeep
import io.github.egt17baa.morseprende.ui.theme.SeaMist
import io.github.egt17baa.morseprende.ui.theme.SeaNight

/** Pantalla principal provisional: el faro, el traductor y los ajustes. */
@Composable
fun HomeScreen(onTranslator: () -> Unit, onSettings: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.home_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
            LighthouseScene(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp),
            )
            Button(onClick = onTranslator, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_translator))
            }
            OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.home_settings))
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(stringResource(R.string.home_campaign_soon), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.home_campaign_soon_body), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** Faro dibujado con un haz de luz que barre el mar lentamente. */
@Composable
private fun LighthouseScene(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "beam")
    val angle by transition.animateFloat(
        initialValue = -28f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beamAngle",
    )
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(SeaNight, SeaDeep)))
        drawSea()
        val towerBottom = size.height * 0.82f
        val towerTop = size.height * 0.36f
        val cx = size.width / 2
        val lantern = Offset(cx, towerTop - size.height * 0.05f)
        rotate(angle, pivot = lantern) { drawBeam(lantern) }
        drawTower(cx, towerTop, towerBottom)
    }
}

private fun DrawScope.drawSea() {
    val seaTop = size.height * 0.78f
    drawRect(SeaMist, topLeft = Offset(0f, seaTop), size = androidx.compose.ui.geometry.Size(size.width, size.height - seaTop))
    val wave = Path().apply {
        moveTo(0f, seaTop)
        var x = 0f
        val step = size.width / 8
        while (x < size.width) {
            quadraticTo(x + step / 2, seaTop - 8f, x + step, seaTop)
            x += step
        }
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(wave, SeaMist)
}

private fun DrawScope.drawBeam(lantern: Offset) {
    val length = size.width * 0.48f
    val spread = size.height * 0.16f
    fun beam(direction: Float) = Path().apply {
        moveTo(lantern.x, lantern.y - 4f)
        lineTo(lantern.x + direction * length, lantern.y - spread)
        lineTo(lantern.x + direction * length, lantern.y + spread)
        lineTo(lantern.x, lantern.y + 4f)
        close()
    }
    val brush = { direction: Float ->
        Brush.horizontalGradient(
            listOf(BeamCream.copy(alpha = 0.55f), Color.Transparent),
            startX = lantern.x,
            endX = lantern.x + direction * length,
        )
    }
    drawPath(beam(1f), brush(1f))
    drawPath(beam(-1f), brush(-1f))
    // Un punto y una raya viajando por el haz: la seña de identidad del juego.
    drawCircle(BeamCream, radius = 5f, center = Offset(lantern.x + length * 0.35f, lantern.y))
    drawRect(
        BeamCream,
        topLeft = Offset(lantern.x + length * 0.45f, lantern.y - 3.5f),
        size = androidx.compose.ui.geometry.Size(length * 0.18f, 7f),
    )
}

private fun DrawScope.drawTower(cx: Float, top: Float, bottom: Float) {
    val halfBottom = size.width * 0.07f
    val halfTop = size.width * 0.045f
    val tower = Path().apply {
        moveTo(cx - halfBottom, bottom)
        lineTo(cx - halfTop, top)
        lineTo(cx + halfTop, top)
        lineTo(cx + halfBottom, bottom)
        close()
    }
    drawPath(tower, Sand)
    // Franjas de coral.
    val height = bottom - top
    for (i in listOf(0.25f, 0.6f)) {
        val y1 = top + height * i
        val y2 = y1 + height * 0.12f
        fun halfAt(y: Float) = halfTop + (halfBottom - halfTop) * ((y - top) / height)
        val stripe = Path().apply {
            moveTo(cx - halfAt(y1), y1)
            lineTo(cx + halfAt(y1), y1)
            lineTo(cx + halfAt(y2), y2)
            lineTo(cx - halfAt(y2), y2)
            close()
        }
        drawPath(stripe, Coral)
    }
    // Linterna y tejado.
    val lanternHeight = size.height * 0.08f
    drawRect(
        BeamCream,
        topLeft = Offset(cx - halfTop * 0.9f, top - lanternHeight),
        size = androidx.compose.ui.geometry.Size(halfTop * 1.8f, lanternHeight),
    )
    val roof = Path().apply {
        moveTo(cx - halfTop * 1.3f, top - lanternHeight)
        lineTo(cx, top - lanternHeight - size.height * 0.07f)
        lineTo(cx + halfTop * 1.3f, top - lanternHeight)
        close()
    }
    drawPath(roof, Coral)
}
