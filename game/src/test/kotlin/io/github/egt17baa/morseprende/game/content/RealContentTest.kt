package io.github.egt17baa.morseprende.game.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Valida los ficheros de contenido reales que se empaquetan en la app. */
object RealContent {
    val dir = File("../app/src/main/assets/content/es")
    val chapter1: Chapter by lazy { ContentParser.chapter(File(dir, "chapter1.json").readText()) }
    val chapter2: Chapter by lazy { ContentParser.chapter(File(dir, "chapter2.json").readText()) }
    val chapter3: Chapter by lazy { ContentParser.chapter(File(dir, "chapter3.json").readText()) }
    val library: List<LibraryCard> by lazy { ContentParser.libraryCards(File(dir, "library.json").readText()) }
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

    @Test fun real_library_is_valid() {
        val cards = RealContent.library
        assertEquals(emptyList<String>(), ContentValidator.validate(listOf(chapter1, RealContent.chapter2, RealContent.chapter3), cards))
        assertEquals(
            listOf(
                "lib-que-es", "lib-morse-vail", "lib-aprender", "lib-1844", "lib-americano", "lib-sos", "lib-mar",
                "lib-guerra", "lib-denton", "lib-numeros", "lib-q", "lib-prosignos", "lib-accesibilidad", "lib-hoy",
                // Etapa 8
                "lib-chappe", "lib-cables", "lib-luces", "lib-aviacion", "lib-marte", "lib-velocidad", "lib-sms", "lib-espana",
            ),
            cards.map { it.id },
        )
        assertEquals(
            listOf(
                null, null, null, "c1m2", "c1m3", "c1m5", "c1f", "c2m3", "c2m6", "c2n5", "c2f", "c3m3", "c3v", "c3f",
                "c1r2", "c2m5", "c1m4", "c3m1", "c3l", "c2m1", "c2m8", "c2x",
            ),
            cards.map { it.unlockedBy },
        )
        val icons = setOf("lighthouse", "telegraph", "ship", "radio", "book", "wave")
        cards.forEach { assertTrue("${it.id}: icono ${it.icon}", it.icon in icons) }
    }

    @Test fun real_chapter2_is_valid() {
        val ch2 = RealContent.chapter2
        assertEquals(emptyList<String>(), ContentValidator.validate(listOf(chapter1, ch2), emptyList()))
        assertEquals(
            listOf("c2m1", "c2m2", "c2m3", "c2r1", "c2m4", "c2m5", "c2m6", "c2r2", "c2m7", "c2m8", "c2r3",
                "c2n1", "c2n2", "c2n3", "c2n4", "c2n5", "c2r4", "c2x", "c2f"),
            ch2.missions.map { it.id },
        )
        assertEquals(
            listOf("UK", "WG", "HL", "FP", "CB", "JV", "XY", "ZQ", "12", "34", "56", "78", "90", "Ñ"),
            ch2.missions.filter { it.type == MissionType.LESSON }.map { it.newChars.joinToString("") },
        )
        val x = ch2.missions.single { it.id == "c2x" }
        assertTrue(x.optional)
        assertEquals(1, ch2.missions.count { it.optional })
        val noise = ch2.missions.associate { it.id to it.noise }
        listOf("c2m1", "c2m2").forEach { assertEquals(0f, noise.getValue(it), 1e-6f) }
        listOf("c2m3", "c2r1").forEach { assertEquals(0.15f, noise.getValue(it), 1e-6f) }
        listOf("c2m4", "c2m5", "c2m6", "c2r2").forEach { assertEquals(0.25f, noise.getValue(it), 1e-6f) }
        ch2.missions.dropWhile { it.id != "c2m7" }.forEach { assertEquals(0.35f, it.noise, 1e-6f) }
        val time = ch2.missions.associate { it.id to it.timeLimitSeconds }
        ch2.missions.takeWhile { it.id != "c2r2" }.forEach { assertEquals(null, it.timeLimitSeconds) }
        listOf("c2r2", "c2m7", "c2m8").forEach { assertEquals(15, time.getValue(it)) }
        ch2.missions.dropWhile { it.id != "c2r3" }.forEach { assertEquals(12, it.timeLimitSeconds) }
        ch2.missions.filter { it.id != "c2x" }.forEach { assertTrue("${it.id}", 'Ñ' !in it.finalMessage.text) }
        assertTrue("words=${ch2.words.size}", ch2.words.size >= 60)
        assertEquals(MissionType.FINALE, ch2.missions.last().type)
    }

    @Test fun chapter2_unlocks_after_chapter1_finale() {
        val chapters = listOf(chapter1, RealContent.chapter2)
        val ch1 = chapter1.missions.map { it.id }.toSet()
        assertFalse(io.github.egt17baa.morseprende.game.progress.ProgressRules.isUnlocked("c2m1", chapters, ch1 - "c1f"))
        assertTrue(io.github.egt17baa.morseprende.game.progress.ProgressRules.isUnlocked("c2m1", chapters, ch1))
        // La final del capítulo 2 no exige la Ñ opcional.
        val ch2required = RealContent.chapter2.missions.filter { !it.optional && it.id != "c2f" }.map { it.id }
        assertTrue(io.github.egt17baa.morseprende.game.progress.ProgressRules.isUnlocked("c2f", chapters, ch1 + ch2required))
    }

    @Test fun planner_works_for_every_chapter2_mission() {
        val chapters = listOf(chapter1, RealContent.chapter2)
        for (m in RealContent.chapter2.missions) {
            val learned = ContentValidator.learnedCharsUpTo(chapters, m.id)
            repeat(10) { seed ->
                val ex = io.github.egt17baa.morseprende.game.mission.MissionPlanner.plan(m, RealContent.chapter2, kotlin.random.Random(seed), chapters = chapters)
                ex.forEach { e ->
                    val texts = when (e) {
                        is io.github.egt17baa.morseprende.game.mission.Exercise.ReceiveChoice -> e.options + e.answer
                        is io.github.egt17baa.morseprende.game.mission.Exercise.ReceiveTyped -> listOf(e.answer)
                        is io.github.egt17baa.morseprende.game.mission.Exercise.Send -> listOf(e.target)
                        else -> emptyList()
                    }
                    texts.forEach { t -> assertTrue("${m.id}: $t", t.all { it in learned }) }
                }
            }
        }
    }

    @Test fun real_chapter3_is_valid() {
        val ch3 = RealContent.chapter3
        assertEquals(emptyList<String>(), ContentValidator.validate(listOf(chapter1, RealContent.chapter2, ch3), emptyList()))
        assertEquals(listOf("c3m1", "c3m2", "c3r1", "c3m3", "c3m4", "c3r2", "c3l", "c3v", "c3s", "c3f"), ch3.missions.map { it.id })
        assertEquals(listOf(".,", "?/", "=+", "'!"), ch3.missions.filter { it.type == MissionType.LESSON }.map { it.newChars.joinToString("") })
        assertEquals(Channel.LIGHT, ch3.missions.single { it.id == "c3l" }.channel)
        assertEquals(Channel.VIBRATION, ch3.missions.single { it.id == "c3v" }.channel)
        assertEquals(20, ch3.missions.single { it.id == "c3s" }.wpm)
        assertEquals(18, ch3.missions.single { it.id == "c3f" }.wpm)
        assertEquals(MissionType.FINALE, ch3.missions.last().type)
        assertTrue("words=${ch3.words.size}", ch3.words.size >= 40)
        assertTrue(ch3.missions.last().finalMessage.text.any { it in ".,?!" })
    }

    @Test fun chapter3_unlocks_after_c2f_without_enye() {
        val chapters = listOf(chapter1, RealContent.chapter2, RealContent.chapter3)
        val done = (chapter1.missions + RealContent.chapter2.missions).filter { !it.optional }.map { it.id }.toSet()
        assertTrue(io.github.egt17baa.morseprende.game.progress.ProgressRules.isUnlocked("c3m1", chapters, done))
        assertFalse(io.github.egt17baa.morseprende.game.progress.ProgressRules.isUnlocked("c3m1", chapters, done - "c2f"))
    }

    @Test fun planner_works_for_every_chapter3_mission() {
        val chapters = listOf(chapter1, RealContent.chapter2, RealContent.chapter3)
        for (m in RealContent.chapter3.missions) {
            val learned = ContentValidator.learnedCharsUpTo(chapters, m.id)
            assertFalse("${m.id}: Ñ", 'Ñ' in learned)
            repeat(10) { seed ->
                io.github.egt17baa.morseprende.game.mission.MissionPlanner.plan(m, RealContent.chapter3, kotlin.random.Random(seed), chapters = chapters).forEach { e ->
                    val texts = when (e) {
                        is io.github.egt17baa.morseprende.game.mission.Exercise.ReceiveChoice -> e.options + e.answer
                        is io.github.egt17baa.morseprende.game.mission.Exercise.ReceiveTyped -> listOf(e.answer)
                        is io.github.egt17baa.morseprende.game.mission.Exercise.Send -> listOf(e.target)
                        else -> emptyList()
                    }
                    texts.forEach { t -> assertTrue("${m.id}: $t", t.all { it in learned }) }
                }
            }
        }
    }
}
