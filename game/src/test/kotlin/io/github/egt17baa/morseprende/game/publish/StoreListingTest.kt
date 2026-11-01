package io.github.egt17baa.morseprende.game.publish

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** La ficha de Google Play (docs/publicacion) cumple los límites de Play y las reglas del proyecto. */
class StoreListingTest {

    private val listing: String = listOf(File("../docs/publicacion/ficha-google-play.md"), File("docs/publicacion/ficha-google-play.md"))
        .first { it.exists() }.readText()

    /** Contenido de un campo `<!-- campo:nombre -->…<!-- /campo -->`, sin espacios de los extremos. */
    private fun field(name: String): String {
        val start = listing.indexOf("<!-- campo:$name -->")
        assertTrue("falta el campo $name", start >= 0)
        val from = start + "<!-- campo:$name -->".length
        val end = listing.indexOf("<!-- /campo -->", from)
        return listing.substring(from, end).trim()
    }

    /** Longitud como la cuenta Play (puntos de código). */
    private fun String.playLength() = codePointCount(0, length)

    @Test fun store_listing_within_limits() {
        val title = field("titulo")
        val short = field("breve")
        val full = field("completa")
        val news = field("novedades")
        assertTrue("título: ${title.playLength()}", title.playLength() in 1..30)
        assertTrue("breve: ${short.playLength()}", short.playLength() in 1..80)
        assertTrue("completa: ${full.playLength()}", full.playLength() in 1..4000)
        assertTrue("novedades: ${news.playLength()}", news.playLength() in 1..500)
        assertTrue(title == "MorsePrende: El Faro")
        // Política de metadatos de Play: nada de precio ni promociones en el nombre ni en la breve.
        listOf(title, short).forEach { assertFalse(it, it.contains("gratis", ignoreCase = true) || it.contains("free", ignoreCase = true)) }
        // Ninguna referencia a otras apps ni marcas (spec §4.1).
        listOf("Morse Mania", "Morse Code Master", "Morsecode", "Duolingo").forEach { assertFalse(it, listing.contains(it, ignoreCase = true)) }
        // Sin mayúsculas «gritadas» en el texto (los títulos de sección cortos sí pueden ir en mayúsculas).
        val shouting = Regex("\\b[A-ZÁÉÍÓÚÑ]{7,}\\b").findAll(full).map { it.value }.filterNot { it in SECTION_WORDS }.toList()
        assertTrue("mayúsculas: $shouting", shouting.isEmpty())
        assertTrue(full.contains("sin anuncios", ignoreCase = true))
        assertTrue(full.contains("sin conexión", ignoreCase = true))
    }

    private companion object {
        /** Palabras de los encabezados de sección de la descripción completa. */
        val SECTION_WORDS = setOf("CAMPAÑA", "HISTORIA", "ESCUCHAR", "INTELIGENTE", "MINIJUEGOS", "BIBLIOTECA", "TRADUCTOR", "PRIVACIDAD", "RESPETA")
    }
}
