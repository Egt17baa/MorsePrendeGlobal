package io.github.egt17baa.morseprende.translator

/** Presentación del Morse en pantalla: puntos y rayas gruesos con espaciado amplio. */
object MorseDisplay {
    fun pretty(morse: String): String = morse
        .split(" / ")
        .joinToString("   /   ") { word ->
            word.split(' ').joinToString("  ") { letter ->
                // Espacio estrecho sin salto de línea: "---" no parece una barra y la letra no se parte.
                letter.map { if (it == '.') '•' else if (it == '-') '▬' else it }.joinToString("\u202F")
            }
        }
}
