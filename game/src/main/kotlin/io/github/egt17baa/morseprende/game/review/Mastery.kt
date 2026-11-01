package io.github.egt17baa.morseprende.game.review

/** Lo bien que se domina cada letra, a partir de su caja de [Leitner]. Solo informa; no castiga. */
object Mastery {

    /** [marks]: de 0 a 3 marcas, para que el nivel no dependa solo del color. */
    enum class Level(val marks: Int) { NO_DATA(0), LEARNING(1), GOOD(2), MASTERED(3) }

    private const val MAX_WEAKEST = 4

    fun level(box: Int?): Level = when {
        box == null -> Level.NO_DATA
        box >= Leitner.MAX_BOX -> Level.MASTERED
        box >= 3 -> Level.GOOD
        else -> Level.LEARNING
    }

    /**
     * Las letras más flojas para practicar: entre las aprendidas con datos y aún no dominadas, las de
     * caja más baja y, si empatan, las aprendidas más tarde. Como mucho 4; vacío si hay menos de 2.
     */
    fun weakest(learnedInOrder: List<Char>, boxes: Map<Char, Int>): List<Char> {
        val order = learnedInOrder.withIndex().associate { (i, c) -> c to i }
        val candidates = learnedInOrder
            .filter { c -> boxes[c]?.let { it < Leitner.MAX_BOX } == true }
            .sortedWith(compareBy<Char> { boxes.getValue(it) }.thenByDescending { order.getValue(it) })
            .take(MAX_WEAKEST)
        return if (candidates.size >= 2) candidates else emptyList()
    }
}
