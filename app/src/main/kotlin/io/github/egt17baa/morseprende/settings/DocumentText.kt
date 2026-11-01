package io.github.egt17baa.morseprende.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Un bloque de un documento de texto (política de privacidad o licencia). */
sealed interface DocBlock {
    data class Heading(val text: String, val level: Int) : DocBlock
    data class Paragraph(val text: String) : DocBlock
    data class Bullet(val text: String) : DocBlock
    data object Rule : DocBlock
}

/**
 * Convierte texto (Markdown sencillo o licencias de texto plano) en bloques legibles en el móvil:
 * títulos con `#`, viñetas con `- `, separadores `---`, y párrafos con las líneas partidas a 78
 * columnas unidas (con letra grande, el corte original quedaría a trompicones). Se quitan `**` y `` ` ``.
 * Sin expresiones regulares: carácter a carácter y línea a línea.
 */
fun documentBlocks(text: String): List<DocBlock> {
    val blocks = mutableListOf<DocBlock>()
    val paragraph = mutableListOf<String>()
    fun flush() {
        if (paragraph.isNotEmpty()) blocks += DocBlock.Paragraph(clean(paragraph.joinToString(" ")))
        paragraph.clear()
    }
    for (raw in text.lines()) {
        val line = raw.trim()
        when {
            line.isEmpty() -> flush()
            line == "---" -> { flush(); blocks += DocBlock.Rule }
            line.startsWith("#") -> {
                flush()
                val level = line.takeWhile { it == '#' }.length
                blocks += DocBlock.Heading(clean(line.drop(level).trim()), level)
            }
            line.startsWith("- ") -> { flush(); blocks += DocBlock.Bullet(clean(line.drop(2))) }
            else -> paragraph += line
        }
    }
    flush()
    return blocks
}

private fun clean(text: String): String = text.replace("**", "").replace("`", "")

@Composable
fun DocumentText(text: String) {
    documentBlocks(text).forEach { block ->
        when (block) {
            is DocBlock.Heading -> Text(
                block.text,
                style = if (block.level <= 1) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.semantics { heading() },
            )
            is DocBlock.Paragraph -> Text(block.text, style = MaterialTheme.typography.bodyMedium)
            is DocBlock.Bullet -> Row(Modifier.padding(start = 8.dp)) {
                Text("•  ", style = MaterialTheme.typography.bodyMedium)
                Text(block.text, style = MaterialTheme.typography.bodyMedium)
            }
            DocBlock.Rule -> HorizontalDivider()
        }
    }
}
