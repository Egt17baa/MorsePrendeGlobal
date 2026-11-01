package io.github.egt17baa.morseprende.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentTextTest {
    @Test fun parses_markdown_and_unwraps_lines() {
        val blocks = documentBlocks("# Título\n\nUna línea\npartida en dos.\n\n## Sección\n\n- **uno**\n- `dos`\n\n---\n")
        assertEquals(
            listOf(
                DocBlock.Heading("Título", level = 1),
                DocBlock.Paragraph("Una línea partida en dos."),
                DocBlock.Heading("Sección", level = 2),
                DocBlock.Bullet("uno"),
                DocBlock.Bullet("dos"),
                DocBlock.Rule,
            ),
            blocks,
        )
    }

    @Test fun plain_license_paragraphs_are_unwrapped() {
        val blocks = documentBlocks("   TERMS AND CONDITIONS\n   FOR USE\n\n   1. Definitions.\n      \"License\" shall mean")
        assertEquals(DocBlock.Paragraph("TERMS AND CONDITIONS FOR USE"), blocks[0])
        assertEquals(DocBlock.Paragraph("1. Definitions. \"License\" shall mean"), blocks[1])
    }
}
