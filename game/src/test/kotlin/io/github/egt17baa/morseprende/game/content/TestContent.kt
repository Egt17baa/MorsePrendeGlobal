package io.github.egt17baa.morseprende.game.content

/** Contenido pequeño y coherente para las pruebas. */
object TestContent {
    fun body(words: Int) = List(words) { "palabra" }.joinToString(" ")

    val chapter = Chapter(
        id = 1, title = "El Faro", intro = "Intro",
        words = listOf("SOS", "OSO", "ESTO", "ESE", "TESO", "SETO", "OTOS"),
        missions = listOf(
            Mission("m1", MissionType.LESSON, "Uno", "b", listOf('S', 'O'), StoryMessage("Gaviota", "SOS"), "d"),
            Mission("r1", MissionType.REVIEW, "Repaso", "b", emptyList(), StoryMessage("Remedios", "OSO"), "d"),
            Mission("m2", MissionType.LESSON, "Dos", "b", listOf('E', 'T'), StoryMessage("Gaviota", "ESTO ES SOS"), "d"),
        ),
    )

    val cards = listOf(
        LibraryCard("c1", "Qué es", body(160), "Dato", null, "book"),
        LibraryCard("c2", "SOS", body(200), "Dato", "m2", "ship"),
    )
}
