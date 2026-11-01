package io.github.egt17baa.morseprende.game.progress

import io.github.egt17baa.morseprende.game.content.Chapter
import io.github.egt17baa.morseprende.game.content.LibraryCard
import io.github.egt17baa.morseprende.game.content.Mission

/** Qué misiones y fichas están disponibles según las misiones completadas. */
object ProgressRules {

    /**
     * Una misión se desbloquea cuando todas las anteriores (en orden de campaña) están completadas,
     * o si ya se completó. Así no se puede saltar misiones aunque los datos tengan huecos.
     */
    fun isUnlocked(missionId: String, chapters: List<Chapter>, completed: Set<String>): Boolean {
        val missions = chapters.flatMap { it.missions }
        val index = missions.indexOfFirst { it.id == missionId }
        if (index < 0) return false
        return missionId in completed || missions.take(index).filter { !it.optional }.all { it.id in completed }
    }

    fun nextMission(chapters: List<Chapter>, completed: Set<String>): Mission? =
        chapters.flatMap { it.missions }.firstOrNull { !it.optional && it.id !in completed }

    fun unlockedCards(cards: List<LibraryCard>, completed: Set<String>): List<LibraryCard> =
        cards.filter { it.unlockedBy == null || it.unlockedBy in completed }

    fun chapterProgress(chapter: Chapter, completed: Set<String>): Float {
        val required = chapter.missions.filter { !it.optional }
        if (required.isEmpty()) return 0f
        return required.count { it.id in completed }.toFloat() / required.size
    }

    /** Letras nuevas de las misiones completadas: las únicas que usan los minijuegos. */
    fun learnedChars(chapters: List<Chapter>, completed: Set<String>): Set<Char> =
        chapters.flatMap { it.missions }.filter { it.id in completed }.flatMap { it.newChars }.toSet()

    private val gameUnlocks = mapOf("STORM" to "c1m2", "FLASH" to "c1m3", "INTERCEPT" to "c1r1", "NIGHT_WATCH" to "c3f")

    /** Misión que desbloquea cada minijuego (spec §7.5: se desbloquean al avanzar). */
    fun gameUnlockMission(game: String): String? = gameUnlocks[game]

    fun isGameUnlocked(game: String, completed: Set<String>): Boolean =
        gameUnlockMission(game)?.let { it in completed } ?: false
}
