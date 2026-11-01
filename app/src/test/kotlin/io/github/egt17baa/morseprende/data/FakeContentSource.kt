package io.github.egt17baa.morseprende.data

import io.github.egt17baa.morseprende.game.content.Chapter
import io.github.egt17baa.morseprende.game.content.ContentParser
import io.github.egt17baa.morseprende.game.content.LibraryCard
import java.io.File

class FakeContentSource(
    private val chapters: List<Chapter>,
    private val cards: List<LibraryCard> = emptyList(),
    private val fail: Boolean = false,
) : ContentSource {
    override fun chapters(): List<Chapter> = if (fail) throw ContentLoadException("roto", null) else chapters
    override fun libraryCards(): List<LibraryCard> = if (fail) throw ContentLoadException("roto", null) else cards

    companion object {
        /** El capítulo 1 real, leído del fichero de assets. */
        val realChapter1: Chapter by lazy {
            ContentParser.chapter(File("src/main/assets/content/es/chapter1.json").readText())
        }

        /** El capítulo 2 real. */
        val realChapter2: Chapter by lazy {
            ContentParser.chapter(File("src/main/assets/content/es/chapter2.json").readText())
        }
    }
}
