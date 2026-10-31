package io.github.egt17baa.morseprende.core

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

/**
 * Las pruebas corren en el Java de escritorio, pero la app usa el motor de expresiones regulares de
 * Android (ICU), que no admite algunas construcciones. Una de ellas, `(?U)`, cerraba la app al usar
 * el códec Morse. Esta prueba impide que vuelvan a colarse en el código de producción.
 */
class AndroidRegexCompatibilityTest {

    private val forbidden = listOf("(?U)", "UNICODE_CHARACTER_CLASS", "\\\\p{")

    @Test fun production_code_avoids_regex_features_unsupported_on_android() {
        val root = File("..").canonicalFile
        val offenders = listOf("morse-core", "game", "app")
            .map { File(root, "$it/src/main") }
            .filter { it.exists() }
            .flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .flatMap { file ->
                file.readLines().mapIndexedNotNull { i, line ->
                    val code = line.substringBefore("//")
                    forbidden.firstOrNull { it in code }?.let { "${file.relativeTo(root)}:${i + 1}: $it" }
                }
            }
        assertEquals(emptyList<String>(), offenders)
    }
}
