package io.github.egt17baa.morseprende.game.games

import io.github.egt17baa.morseprende.core.MorseTiming
import io.github.egt17baa.morseprende.game.mission.AnswerResult
import io.github.egt17baa.morseprende.game.mission.MissionSession
import io.github.egt17baa.morseprende.game.review.CharOutcome
import io.github.egt17baa.morseprende.game.review.Leitner
import kotlin.random.Random

enum class NightChannel { SOUND, LIGHT, VIBRATION }

data class NightMessage(val text: String, val channel: NightChannel)

/**
 * «Guardia nocturna»: modo infinito tras la campaña. Llegan mensajes sin parar (letras, cifras o
 * palabras) por sonido, luz o vibración y hay que copiarlos. Cada 5 aciertos pasa una hora de la
 * noche (desde las 22:00): +2 PPM y algo más de ruido. 3 fallos terminan la guardia.
 */
class NightWatchGame(
    words: List<String>,
    chars: Collection<Char>,
    startWpm: Int,
    private val random: Random,
    private val vibrationAllowed: Boolean,
) {
    private val letters = chars.distinct()
    private val digits = letters.filter { it.isDigit() }
    private val alpha = letters.filter { it.isLetter() }
    private val pool = words.filter { w -> w.length > 1 && w.all { it == ' ' || it in letters } }.distinct()
    private val charOutcomes = mutableListOf<CharOutcome>()
    private var served = 0

    var current: NightMessage = NightMessage("", NightChannel.SOUND)
        private set
    var wpm: Int = startWpm.coerceIn(MorseTiming.MIN_WPM, MorseTiming.MAX_WPM)
        private set
    var noise: Float = START_NOISE
        private set
    var hour: Int = 0
        private set
    var score: Int = 0
        private set
    var strikes: Int = 0
        private set

    val isOver: Boolean get() = strikes >= STRIKES
    val outcomes: List<CharOutcome> get() = charOutcomes.toList()

    init {
        require(letters.isNotEmpty()) { "La guardia necesita al menos una letra" }
        current = nextMessage()
    }

    fun answer(text: String): AnswerResult {
        check(!isOver) { "La guardia ha terminado" }
        val expected = current.text
        val ok = MissionSession.matches(text, expected)
        charOutcomes += Leitner.outcomesFor(expected, text)
        if (ok) {
            score++
            if (score % PER_HOUR == 0) {
                hour++
                wpm = minOf(MorseTiming.MAX_WPM, wpm + WPM_PER_HOUR)
                noise = minOf(MAX_NOISE, noise + NOISE_PER_HOUR)
            }
        } else {
            strikes++
        }
        if (!isOver) current = nextMessage()
        return AnswerResult(ok, expected, advanced = true, attemptsLeft = STRIKES - strikes)
    }

    private fun nextMessage(): NightMessage {
        val index = served++
        // 4.º, 8.º… mensaje por luz; 6.º, 12.º… por vibración (si se permite).
        val channel = when {
            index % 6 == 5 && vibrationAllowed -> NightChannel.VIBRATION
            index % 4 == 3 -> NightChannel.LIGHT
            else -> NightChannel.SOUND
        }
        val previous = current.text
        var text: String
        var tries = 0
        do {
            text = pickText()
        } while (text == previous && tries++ < 10)
        return NightMessage(text, channel)
    }

    private fun pickText(): String {
        val roll = random.nextInt(100)
        return when {
            roll < 60 && pool.isNotEmpty() -> pool.random(random)
            roll < 75 && digits.isNotEmpty() -> String(CharArray(random.nextInt(2, 5)) { digits.random(random) })
            alpha.isNotEmpty() -> alpha.random(random).toString()
            else -> letters.random(random).toString()
        }
    }

    companion object {
        const val STRIKES = 3
        const val PER_HOUR = 5
        const val WPM_PER_HOUR = 2
        const val START_NOISE = 0.1f
        const val NOISE_PER_HOUR = 0.05f
        const val MAX_NOISE = 0.35f

        /** La hora 8 son las 06:00: el amanecer. */
        const val DAWN_HOUR = 8

        /** Hora de la noche (0 = 22:00) en formato reloj. */
        fun clock(hour: Int): String = String.format(java.util.Locale.ROOT, "%02d:00", (22 + hour) % 24)
    }
}
