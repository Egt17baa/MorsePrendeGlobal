package io.github.egt17baa.morseprende.game.content

import io.github.egt17baa.morseprende.core.MorseAlphabet
import io.github.egt17baa.morseprende.core.MorseCodec
import io.github.egt17baa.morseprende.core.MorseTiming

/** Reglas de coherencia del contenido. Se ejecutan en las pruebas sobre los ficheros reales. */
object ContentValidator {

    const val MAX_NOISE = 0.5f
    const val MIN_TIME_LIMIT = 5
    const val MIN_CARD_WORDS = 150
    const val MAX_CARD_WORDS = 300

    /** Caracteres aprendidos hasta la misión dada, incluida, recorriendo los capítulos en orden. */
    fun learnedCharsUpTo(chapters: List<Chapter>, missionId: String): Set<Char> {
        val learned = linkedSetOf<Char>()
        for (mission in chapters.flatMap { it.missions }) {
            if (mission.id == missionId) return learned + mission.newChars
            // Las letras de una misión opcional no se dan por aprendidas en las demás.
            if (!mission.optional) learned += mission.newChars
        }
        throw IllegalArgumentException("Misión desconocida: $missionId")
    }

    fun validate(chapters: List<Chapter>, cards: List<LibraryCard>): List<String> {
        val errors = mutableListOf<String>()
        val missions = chapters.flatMap { it.missions }

        missions.groupBy { it.id }.filterValues { it.size > 1 }.keys
            .forEach { errors += "Id de misión duplicado: $it" }

        val learned = mutableSetOf<Char>()
        for (mission in missions) {
            when (mission.type) {
                MissionType.LESSON -> if (mission.newChars.size !in 1..3) {
                    errors += "${mission.id}: una lección necesita entre 1 y 3 letras nuevas"
                }
                MissionType.REVIEW, MissionType.FINALE -> if (mission.newChars.isNotEmpty()) {
                    errors += "${mission.id}: un repaso o final no puede introducir letras nuevas"
                }
            }
            mission.newChars.filter { MorseAlphabet.codeFor(it) == null }
                .forEach { errors += "${mission.id}: el carácter '$it' no existe en Morse" }
            if (mission.optional && mission.type != MissionType.LESSON) {
                errors += "${mission.id}: solo una lección puede ser opcional"
            }
            if (mission.noise !in 0f..MAX_NOISE) errors += "${mission.id}: el ruido debe estar entre 0 y $MAX_NOISE"
            mission.wpm?.let { if (it !in MorseTiming.MIN_WPM..MorseTiming.MAX_WPM) errors += "${mission.id}: la velocidad debe estar entre ${MorseTiming.MIN_WPM} y ${MorseTiming.MAX_WPM} PPM" }
            mission.timeLimitSeconds?.let { if (it < MIN_TIME_LIMIT) errors += "${mission.id}: el contrarreloj debe ser de al menos $MIN_TIME_LIMIT s" }
            val available = learned + mission.newChars
            if (!mission.optional) learned += mission.newChars

            val unknown = MorseCodec.normalizeText(mission.finalMessage.text)
                .filter { it != ' ' && it !in available }
                .toSet()
            if (unknown.isNotEmpty()) {
                errors += "${mission.id}: el mensaje final usa letras no aprendidas: ${unknown.joinToString(" ")}"
            }
        }

        for (chapter in chapters) {
            chapter.words.filter { word -> word.isEmpty() || word.any { MorseAlphabet.codes[it] == null } }
                .forEach { errors += "Capítulo ${chapter.id}: palabra no válida en el banco: $it" }
        }

        cards.groupBy { it.id }.filterValues { it.size > 1 }.keys
            .forEach { errors += "Id de ficha duplicado: $it" }
        val missionIds = missions.map { it.id }.toSet()
        for (card in cards) {
            val unlock = card.unlockedBy
            if (unlock != null && unlock !in missionIds) {
                errors += "${card.id}: se desbloquea con una misión que no existe: $unlock"
            }
            val words = card.body.split(Regex("\\s+")).count { it.isNotBlank() }
            if (words !in MIN_CARD_WORDS..MAX_CARD_WORDS) {
                errors += "${card.id}: el cuerpo tiene $words palabras (debe tener entre $MIN_CARD_WORDS y $MAX_CARD_WORDS)"
            }
        }
        return errors
    }
}
