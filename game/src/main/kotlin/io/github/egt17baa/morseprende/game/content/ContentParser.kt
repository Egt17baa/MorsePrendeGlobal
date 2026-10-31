package io.github.egt17baa.morseprende.game.content

import kotlinx.serialization.json.Json

object ContentParser {

    /** Estricto a propósito: una clave mal escrita en el JSON falla en vez de pasar desapercibida. */
    private val json = Json { ignoreUnknownKeys = false }

    fun chapter(text: String): Chapter = json.decodeFromString(text)

    fun libraryCards(text: String): List<LibraryCard> = json.decodeFromString(text)
}
