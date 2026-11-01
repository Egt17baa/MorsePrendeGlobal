package io.github.egt17baa.morseprende.ui.theme

import androidx.compose.ui.graphics.Color

/** Colores de los cosméticos de la tienda. */
object Cosmetics {
    fun lampColor(id: String): Color = when (id) {
        "lamp_emerald" -> Color(0xFF7FE0B0)
        "lamp_harbor" -> Color(0xFFFF8A80)
        "lamp_polar" -> Color(0xFF9AD7FF)
        "lamp_amber" -> Color(0xFFFFC266)
        else -> BeamCream
    }

    fun keyerRing(id: String): Color = when (id) {
        "key_brass" -> Color(0xFFD4A84B)
        "key_silver" -> Color(0xFFC9D3DA)
        else -> Coral
    }
}
