package io.github.egt17baa.morseprende.ui.hub

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import io.github.egt17baa.morseprende.ui.theme.BeamCream
import io.github.egt17baa.morseprende.ui.theme.Coral
import io.github.egt17baa.morseprende.ui.theme.Sand
import io.github.egt17baa.morseprende.ui.theme.SeaDeep
import io.github.egt17baa.morseprende.ui.theme.SeaMist
import io.github.egt17baa.morseprende.ui.theme.SeaNight

/**
 * Faro dibujado con un haz de luz que barre el mar lentamente.
 * @param glow progreso del capítulo (0..1): el haz gana brillo y alcance al avanzar.
 */
@Composable
fun LighthouseScene(
    glow: Float,
    modifier: Modifier = Modifier,
    lampColor: Color = BeamCream,
    decorations: Set<String> = emptySet(),
) {
    val strength = 0.3f + 0.7f * glow.coerceIn(0f, 1f)
    val transition = rememberInfiniteTransition(label = "beam")
    val angle by transition.animateFloat(
        initialValue = -28f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(tween(4_000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beamAngle",
    )
    Canvas(modifier) {
        drawRect(Brush.verticalGradient(listOf(SeaNight, SeaDeep)))
        drawSky(decorations)
        drawSea()
        val towerBottom = size.height * 0.82f
        val towerTop = size.height * 0.36f
        val cx = size.width / 2
        val lantern = Offset(cx, towerTop - size.height * 0.05f)
        rotate(angle, pivot = lantern) { drawBeam(lantern, strength, lampColor) }
        drawDecorations(decorations, cx, towerTop, lantern)
        drawTower(cx, towerTop, towerBottom, lampColor)
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

private fun DrawScope.drawBeam(lantern: Offset, strength: Float, lampColor: Color) {
    val length = size.width * (0.25f + 0.25f * strength)
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
            listOf(lampColor.copy(alpha = 0.6f * strength), Color.Transparent),
            startX = lantern.x,
            endX = lantern.x + direction * length,
        )
    }
    drawPath(beam(1f), brush(1f))
    drawPath(beam(-1f), brush(-1f))
    // Un punto y una raya viajando por el haz: la seña de identidad del juego.
    drawCircle(lampColor, radius = 5f, center = Offset(lantern.x + length * 0.35f, lantern.y))
    drawRect(
        lampColor,
        topLeft = Offset(lantern.x + length * 0.45f, lantern.y - 3.5f),
        size = androidx.compose.ui.geometry.Size(length * 0.18f, 7f),
    )
}

private fun DrawScope.drawTower(cx: Float, top: Float, bottom: Float, lampColor: Color) {
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
        lampColor,
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

/** Decoraciones del cielo, detrás de todo: aurora y estrellas fugaces. */
private fun DrawScope.drawSky(decorations: Set<String>) {
    if ("decor_aurora" in decorations) {
        val bands = listOf(Color(0xFF7FE0B0) to 0.10f, Color(0xFFC9A7FF) to 0.18f, Color(0xFF7FE0B0) to 0.26f)
        bands.forEachIndexed { i, (color, fy) ->
            val y = size.height * fy
            val path = Path().apply {
                moveTo(0f, y)
                cubicTo(size.width * 0.3f, y - size.height * 0.08f, size.width * 0.6f, y + size.height * 0.08f, size.width, y - size.height * 0.04f * (i + 1))
                lineTo(size.width, y + size.height * 0.05f)
                cubicTo(size.width * 0.6f, y + size.height * 0.12f, size.width * 0.3f, y - size.height * 0.02f, 0f, y + size.height * 0.06f)
                close()
            }
            drawPath(path, Brush.verticalGradient(listOf(color.copy(alpha = 0.25f), Color.Transparent), startY = y - size.height * 0.06f, endY = y + size.height * 0.12f))
        }
    }
    if ("decor_stars" in decorations) {
        listOf(0.08f to 0.08f, 0.35f to 0.05f, 0.55f to 0.12f, 0.88f to 0.07f, 0.66f to 0.28f, 0.12f to 0.33f).forEach { (fx, fy) ->
            drawCircle(Sand.copy(alpha = 0.85f), radius = size.width * 0.004f, center = Offset(size.width * fx, size.height * fy))
        }
        // Estrella fugaz con su estela.
        val head = Offset(size.width * 0.86f, size.height * 0.16f)
        val tail = Offset(size.width * 0.70f, size.height * 0.06f)
        drawLine(Brush.linearGradient(listOf(Color.Transparent, BeamCream), start = tail, end = head), tail, head, strokeWidth = size.width * 0.006f)
        drawCircle(BeamCream, radius = size.width * 0.007f, center = head)
    }
}

/** Decoraciones compradas en la tienda: gaviotas, barca, banderines, farolillos, casa y ballena. */
private fun DrawScope.drawDecorations(decorations: Set<String>, cx: Float, towerTop: Float, lantern: Offset) {
    val seaTop = size.height * 0.78f
    val stroke = size.width * 0.006f
    if ("decor_gulls" in decorations) {
        listOf(0.18f to 0.22f, 0.27f to 0.15f, 0.78f to 0.2f).forEach { (fx, fy) ->
            val x = size.width * fx
            val y = size.height * fy
            val w = size.width * 0.03f
            drawLine(Sand, Offset(x - w, y - w * 0.4f), Offset(x, y), strokeWidth = stroke)
            drawLine(Sand, Offset(x, y), Offset(x + w, y - w * 0.4f), strokeWidth = stroke)
        }
    }
    if ("decor_boat" in decorations) {
        val bx = size.width * 0.2f
        val hull = Path().apply {
            moveTo(bx - size.width * 0.06f, seaTop - size.height * 0.01f)
            lineTo(bx + size.width * 0.06f, seaTop - size.height * 0.01f)
            lineTo(bx + size.width * 0.04f, seaTop + size.height * 0.04f)
            lineTo(bx - size.width * 0.04f, seaTop + size.height * 0.04f)
            close()
        }
        drawPath(hull, Coral)
        drawLine(Sand, Offset(bx, seaTop - size.height * 0.01f), Offset(bx, seaTop - size.height * 0.12f), strokeWidth = stroke)
        drawPath(
            Path().apply {
                moveTo(bx, seaTop - size.height * 0.12f); lineTo(bx + size.width * 0.04f, seaTop - size.height * 0.03f); lineTo(bx, seaTop - size.height * 0.03f); close()
            },
            Sand,
        )
    }
    if ("decor_flags" in decorations) {
        val end = Offset(size.width * 0.92f, seaTop - size.height * 0.02f)
        drawLine(Sand.copy(alpha = 0.6f), lantern, end, strokeWidth = stroke * 0.6f)
        for (i in 1..6) {
            val t = i / 7f
            val p = Offset(lantern.x + (end.x - lantern.x) * t, lantern.y + (end.y - lantern.y) * t)
            val flag = Path().apply {
                moveTo(p.x - size.width * 0.012f, p.y); lineTo(p.x + size.width * 0.012f, p.y); lineTo(p.x, p.y + size.height * 0.04f); close()
            }
            drawPath(flag, if (i % 2 == 0) Coral else BeamCream)
        }
    }
    if ("decor_lanterns" in decorations) {
        for (i in 0 until 7) {
            val x = size.width * (0.08f + i * 0.14f)
            if (kotlin.math.abs(x - cx) < size.width * 0.08f) continue
            val c = Offset(x, seaTop - size.height * 0.015f)
            drawCircle(Brush.radialGradient(listOf(BeamCream.copy(alpha = 0.7f), Color.Transparent), center = c, radius = size.width * 0.03f), radius = size.width * 0.03f, center = c)
            drawCircle(BeamCream, radius = size.width * 0.008f, center = c)
        }
    }
    if ("decor_house" in decorations) {
        // Casa del farero sobre una roca, a la derecha del faro, con la ventana encendida.
        val left = cx + size.width * 0.12f
        val w = size.width * 0.12f
        val h = size.height * 0.10f
        val base = seaTop + size.height * 0.01f
        drawOval(SeaDeep, Offset(left - w * 0.25f, base - size.height * 0.015f), androidx.compose.ui.geometry.Size(w * 1.5f, size.height * 0.05f))
        drawRect(Sand, Offset(left, base - h), androidx.compose.ui.geometry.Size(w, h))
        drawPath(
            Path().apply { moveTo(left - w * 0.1f, base - h); lineTo(left + w / 2, base - h * 1.7f); lineTo(left + w * 1.1f, base - h); close() },
            Coral,
        )
        drawRect(BeamCream, Offset(left + w * 0.3f, base - h * 0.7f), androidx.compose.ui.geometry.Size(w * 0.25f, h * 0.3f))
    }
    if ("decor_whale" in decorations) {
        // Ballena asomando en el mar, con su chorro.
        val wx = size.width * 0.8f
        val wy = seaTop + size.height * 0.15f
        val body = Path().apply {
            moveTo(wx - size.width * 0.09f, wy)
            cubicTo(wx - size.width * 0.07f, wy - size.height * 0.07f, wx + size.width * 0.06f, wy - size.height * 0.07f, wx + size.width * 0.08f, wy)
            lineTo(wx + size.width * 0.13f, wy - size.height * 0.04f)
            lineTo(wx + size.width * 0.12f, wy + size.height * 0.005f)
            close()
        }
        drawPath(body, Color(0xFF123A40))
        drawCircle(Sand, radius = size.width * 0.005f, center = Offset(wx - size.width * 0.05f, wy - size.height * 0.025f))
        val spout = Offset(wx - size.width * 0.02f, wy - size.height * 0.06f)
        listOf(-0.025f, 0f, 0.025f).forEach { dx ->
            drawLine(Sand.copy(alpha = 0.8f), spout, Offset(spout.x + size.width * dx, spout.y - size.height * 0.06f), strokeWidth = stroke)
        }
    }
}
