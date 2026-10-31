package io.github.egt17baa.morseprende.ui.mission

import io.github.egt17baa.morseprende.game.content.Chapter
import io.github.egt17baa.morseprende.game.content.Mission
import io.github.egt17baa.morseprende.game.content.MissionType
import io.github.egt17baa.morseprende.game.content.StoryMessage

object MissionTestContent {
    val chapter = Chapter(
        id = 1, title = "El Faro", intro = "Intro", words = listOf("SOS", "OSO"),
        missions = listOf(
            Mission("t1", MissionType.LESSON, "La primera luz", "Aprende S y O", listOf('S', 'O'), StoryMessage("Gaviota", "SOS"), "¡Bien!"),
        ),
    )
}
