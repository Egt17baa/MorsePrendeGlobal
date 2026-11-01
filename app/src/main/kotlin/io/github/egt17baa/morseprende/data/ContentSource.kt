package io.github.egt17baa.morseprende.data

import android.content.Context
import io.github.egt17baa.morseprende.game.content.Chapter
import io.github.egt17baa.morseprende.game.content.ContentParser
import io.github.egt17baa.morseprende.game.content.LibraryCard

/** Contenido narrativo del juego (capítulos y fichas de la biblioteca). */
interface ContentSource {
    /** @throws ContentLoadException si el contenido falta o está roto. */
    fun chapters(): List<Chapter>

    /** @throws ContentLoadException si el contenido falta o está roto. */
    fun libraryCards(): List<LibraryCard>
}

class ContentLoadException(message: String, cause: Throwable?) : Exception(message, cause)

/** Lee el contenido de `assets/content/es/`. Se guarda en caché tras la primera lectura. */
class AssetContentSource internal constructor(private val read: (String) -> String) : ContentSource {

    constructor(context: Context) : this({ name ->
        context.applicationContext.assets.open("$DIR/$name").bufferedReader().use { it.readText() }
    })

    private val chapters by lazy { load(CHAPTER_FILES) { files -> files.map { ContentParser.chapter(read(it)) } } }
    private val cards by lazy { load(listOf(LIBRARY_FILE)) { ContentParser.libraryCards(read(it.single())) } }

    override fun chapters(): List<Chapter> = chapters.getOrThrow()

    override fun libraryCards(): List<LibraryCard> = cards.getOrThrow()

    private fun <T> load(files: List<String>, parse: (List<String>) -> T): Result<T> =
        runCatching { parse(files) }.recoverCatching { e ->
            throw ContentLoadException("No se pudo cargar el contenido ${files.joinToString()}: ${e.message}", e)
        }

    private companion object {
        const val DIR = "content/es"
        val CHAPTER_FILES = listOf("chapter1.json", "chapter2.json")
        const val LIBRARY_FILE = "library.json"
    }
}
