package com.lattice.notes.ui.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {
    @Test
    fun `parses Obsidian style note blocks`() {
        val note = """
            ---
            tags: [work, android]
            status: active
            ---
            # Project note

            A link to [[Architecture|the architecture]].

            - [x] Pick a stack
            - [ ] Ship the app

            > A useful callout

            ```kotlin
            val ready = true
            ```
        """.trimIndent()

        val blocks = MarkdownParser.parse(note)

        assertTrue(blocks.first() is MarkdownBlock.FrontMatter)
        assertTrue(blocks.any { it is MarkdownBlock.Heading && it.text == "Project note" })
        assertTrue(blocks.any { it is MarkdownBlock.Task && it.checked })
        assertTrue(blocks.any { it is MarkdownBlock.Code && it.language == "kotlin" })
    }

    @Test
    fun `wiki target prefers a note beside the current note`() {
        val paths = listOf("archive/Plan.md", "projects/Plan.md", "projects/Index.md")
        assertEquals("projects/Plan.md", resolveWikiTarget("projects/Index.md", "Plan", paths))
    }
}
