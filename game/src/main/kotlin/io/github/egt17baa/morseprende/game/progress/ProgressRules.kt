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
        return missionId in completed || missions.take(index).all { it.id in completed }
    }

    fun nextMission(chapters: List<Chapter>, completed: Set<String>): Mission? =
        chapters.flatMap { it.missions }.firstOrNull { it.id !in completed }

    fun unlockedCards(cards: List<LibraryCard>, completed: Set<String>): List<LibraryCard> =
        cards.filter { it.unlockedBy == null || it.unlockedBy in completed }

    fun chapterProgress(chapter: Chapter, completed: Set<String>): Float {
        if (chapter.missions.isEmpty()) return 0f
        return chapter.missions.count { it.id in completed }.toFloat() / chapter.missions.size
    }
}
