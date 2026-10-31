package io.github.egt17baa.morseprende.game.games

import io.github.egt17baa.morseprende.game.review.CharOutcome
import io.github.egt17baa.morseprende.game.review.Leitner
import kotlin.random.Random

/** Una señal Morse que cae del cielo al mar. */
data class FallingSignal(val id: Int, val char: Char, val spawnedAt: Long, val fallMs: Long) {
    /** 0 = acaba de aparecer, 1 = toca el mar. */
    fun progress(now: Long): Float = ((now - spawnedAt).toFloat() / fallMs).coerceAtLeast(0f)
}

sealed interface StormEvent {
    data class Spawned(val char: Char) : StormEvent
    data class Hit(val char: Char) : StormEvent
    data class Splash(val char: Char) : StormEvent
    data object GameOver : StormEvent
}

/**
 * Tormenta de señales: caen señales Morse y hay que escribir su letra antes de que toquen el mar.
 * Cada señal nueva cae un 3 % más rápido (mínimo 3,5 s) y aparece un 3 % antes (mínimo 0,9 s).
 * Tres señales en el mar terminan la partida. El reloj lo aporta quien llama (`now` en ms).
 */
class StormGame(
    private val chars: Collection<Char>,
    private val boxes: Map<Char, Int>,
    private val random: Random,
    startAt: Long,
) {
    private val live = mutableListOf<FallingSignal>()
    private val charOutcomes = mutableListOf<CharOutcome>()
    private var nextId = 1
    private var nextSpawnAt = startAt
    private var interval = START_INTERVAL_MS.toDouble()
    private var fall = START_FALL_MS.toDouble()

    val signals: List<FallingSignal> get() = live.toList()
    var score: Int = 0
        private set
    var lives: Int = LIVES
        private set
    val isOver: Boolean get() = lives <= 0
    val outcomes: List<CharOutcome> get() = charOutcomes.toList()

    init {
        require(chars.isNotEmpty()) { "La tormenta necesita al menos una letra" }
    }

    fun tick(now: Long): List<StormEvent> {
        if (isOver) return emptyList()
        val events = mutableListOf<StormEvent>()
        while (now >= nextSpawnAt) {
            val c = Leitner.pick(chars, boxes, random)
            live += FallingSignal(nextId++, c, nextSpawnAt, fall.toLong())
            events += StormEvent.Spawned(c)
            nextSpawnAt += interval.toLong()
            interval = maxOf(MIN_INTERVAL_MS.toDouble(), interval * SPEEDUP)
            fall = maxOf(MIN_FALL_MS.toDouble(), fall * SPEEDUP)
        }
        val landed = live.filter { it.progress(now) >= 1f }.sortedBy { it.spawnedAt + it.fallMs }
        for (signal in landed) {
            if (isOver) break
            live -= signal
            lives--
            charOutcomes += CharOutcome(signal.char, false)
            events += StormEvent.Splash(signal.char)
            if (isOver) events += StormEvent.GameOver
        }
        return events
    }

    fun input(c: Char, now: Long): List<StormEvent> {
        if (isOver) return emptyList()
        val target = live.filter { it.char == c }.maxByOrNull { it.progress(now) } ?: return emptyList()
        live -= target
        score++
        charOutcomes += CharOutcome(c, true)
        return listOf(StormEvent.Hit(c))
    }

    companion object {
        const val LIVES = 3
        private const val START_INTERVAL_MS = 2500L
        private const val MIN_INTERVAL_MS = 900L
        private const val START_FALL_MS = 7000L
        private const val MIN_FALL_MS = 3500L
        private const val SPEEDUP = 0.97
    }
}
