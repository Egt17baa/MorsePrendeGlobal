package io.github.egt17baa.morseprende.game.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Valida los ficheros de contenido reales que se empaquetan en la app. */
object RealContent {
    val dir = File("../app/src/main/assets/content/es")
    val chapter1: Chapter by lazy { ContentParser.chapter(File(dir, "chapter1.json").readText()) }
}

class RealContentTest {

    private val chapter1 get() = RealContent.chapter1

    @Test fun real_chapter1_is_valid() {
        assertEquals(emptyList<String>(), ContentValidator.validate(listOf(chapter1), emptyList()))
        assertEquals(
            listOf("c1m1", "c1m2", "c1m3", "c1r1", "c1m4", "c1m5", "c1r2", "c1f"),
            chapter1.missions.map { it.id },
        )
        assertEquals(
            listOf(listOf('S', 'O'), listOf('E', 'T'), listOf('A', 'N'), listOf('I', 'M'), listOf('R', 'D')),
            chapter1.missions.filter { it.type == MissionType.LESSON }.map { it.newChars },
        )
        assertTrue("words=${chapter1.words.size}", chapter1.words.size >= 30)
        assertEquals("SOS", chapter1.missions.first().finalMessage.text)
        val finale = chapter1.missions.last()
        assertEquals(MissionType.FINALE, finale.type)
        assertTrue(finale.finalMessage.text.count { it != ' ' } >= 25)
    }
}
