package io.github.egt17baa.morseprende.game.content

import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import kotlin.test.assertFailsWith

class ContentParserTest {

    private val chapterJson = """
        {"id":1,"title":"El Faro","intro":"Hola","words":["SOS"],
         "missions":[{"id":"c1m1","type":"LESSON","title":"La primera luz","briefing":"b",
           "newChars":["S","O"],"finalMessage":{"from":"Gaviota","text":"SOS"},"debriefing":"d"}]}
    """.trimIndent()

    @Test fun parses_chapter() {
        val ch = ContentParser.chapter(chapterJson)
        assertEquals("El Faro", ch.title)
        val m = ch.missions.single()
        assertEquals(MissionType.LESSON, m.type)
        assertEquals(listOf('S', 'O'), m.newChars)
        assertEquals(StoryMessage("Gaviota", "SOS"), m.finalMessage)
    }

    @Test fun rejects_unknown_key() {
        assertFailsWith<SerializationException> {
            ContentParser.chapter(chapterJson.replace("\"title\":\"El Faro\"", "\"tittle\":\"El Faro\""))
        }
    }

    @Test fun parses_cards() {
        val cards = ContentParser.libraryCards(
            """[{"id":"a","title":"A","body":"x","didYouKnow":"y","icon":"book"},
                {"id":"b","title":"B","body":"x","didYouKnow":"y","unlockedBy":"c1m2","icon":"ship"}]""",
        )
        assertEquals(2, cards.size)
        assertNull(cards[0].unlockedBy)
        assertEquals("c1m2", cards[1].unlockedBy)
    }

    @Test fun channel_parsed() {
        val ch = ContentParser.chapter(chapterJson.replace("\"debriefing\":\"d\"", "\"debriefing\":\"d\",\"channel\":\"LIGHT\",\"wpm\":20"))
        assertEquals(Channel.LIGHT, ch.missions.single().channel)
        assertEquals(20, ch.missions.single().wpm)
        assertEquals(Channel.SOUND, ContentParser.chapter(chapterJson).missions.single().channel)
    }
}
