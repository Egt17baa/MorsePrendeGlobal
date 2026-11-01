package io.github.egt17baa.morseprende.game.publish

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** La ficha de Google Play (docs/publicacion) cumple los límites de Play y las reglas del proyecto. */
class StoreListingTest {

    /** Ruta que pasa Gradle (game/build.gradle.kts), que además la declara como entrada de la prueba. */
    private val listing: String = File(
        checkNotNull(System.getProperty("ficha.path")) { "falta la propiedad ficha.path (la pone Gradle)" },
    ).readText()

    /** Contenido de un campo `<!-- campo:nombre -->…<!-- /campo -->`, sin espacios de los extremos. */
    private fun field(name: String): String {
        val start = listing.indexOf("<!-- campo:$name -->")
        assertTrue("falta el campo $name", start >= 0)
        val from = start + "<!-- campo:$name -->".length
        val end = listing.indexOf("<!-- /campo -->", from)
        assertTrue("falta el cierre del campo $name", end >= 0)
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
        listOf(title, short, full).forEach { assertFalse(it, it.contains("gratis", ignoreCase = true) || it.contains("free", ignoreCase = true)) }
        // Ninguna referencia a otras apps ni marcas (spec §4.1), tampoco a la app de referencia.
        listOf("Morse Mania", "Morse Code Master", "Morsecode", "Duolingo", "Código Morse Completo", "Sparks Digital")
            .forEach { assertFalse(it, listing.contains(it, ignoreCase = true)) }
        // Sin mayúsculas «gritadas» (los títulos de sección cortos sí). Palabras por letras, también con tilde.
        val shouting = full.split(Regex("[^\\p{L}]+")).filter { w -> w.length >= 7 && w.all { it.isUpperCase() } }
            .filterNot { it in SECTION_WORDS }
        assertTrue("mayúsculas: $shouting", shouting.isEmpty())
        assertTrue(full.contains("sin anuncios", ignoreCase = true))
        assertTrue(full.contains("sin conexión", ignoreCase = true))
    }

    private companion object {
        /** Palabras de los encabezados de sección de la descripción completa. */
        val SECTION_WORDS = setOf("CAMPAÑA", "HISTORIA", "ESCUCHAR", "INTELIGENTE", "MINIJUEGOS", "BIBLIOTECA", "TRADUCTOR", "PRIVACIDAD", "RESPETA")
    }

    @Test fun privacy_policy_is_complete() {
        val policy = File(checkNotNull(System.getProperty("politica.path")) { "falta politica.path" }).readText()
        assertFalse("quedan huecos sin rellenar", policy.contains("‹") || listing.contains("‹"))
        assertTrue(policy.contains("https://github.com/egt17baa"))
        assertTrue(policy.contains("13 años o más"))
        // Público objetivo decidido: 13+ en la ficha, sin menores de 13.
        assertTrue(listing.contains("Decisión: **solo 13 años o más.**"))
    }
}
