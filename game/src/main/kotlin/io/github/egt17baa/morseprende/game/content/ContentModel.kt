package io.github.egt17baa.morseprende.game.content

import kotlinx.serialization.Serializable

@Serializable
enum class MissionType { LESSON, REVIEW, FINALE }

/** Un mensaje de la historia: quién lo envía y qué dice (solo caracteres ya aprendidos). */
@Serializable
data class StoryMessage(val from: String, val text: String)

@Serializable
data class Mission(
    val id: String,
    val type: MissionType,
    val title: String,
    val briefing: String,
    val newChars: List<Char> = emptyList(),
    val finalMessage: StoryMessage,
    val debriefing: String,
)

@Serializable
data class Chapter(
    val id: Int,
    val title: String,
    val intro: String,
    /** Banco de palabras del capítulo; cada misión usa solo las que contienen letras ya aprendidas. */
    val words: List<String>,
    val missions: List<Mission>,
)

@Serializable
data class LibraryCard(
    val id: String,
    val title: String,
    val body: String,
    val didYouKnow: String,
    /** Id de la misión que la desbloquea; null = disponible desde el principio. */
    val unlockedBy: String? = null,
    /** "lighthouse" | "telegraph" | "ship" | "radio" | "book" | "wave". */
    val icon: String,
)
