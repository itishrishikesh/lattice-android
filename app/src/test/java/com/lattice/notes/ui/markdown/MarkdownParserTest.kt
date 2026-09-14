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

    @Test
    fun `parses Obsidian callouts and tables without exposing syntax`() {
        val note = """
            > [!warning] Read this first
            > Links should stay legible.

            | Feature | Status |
            | --- | --- |
            | Selection | Done |

            Visible %%this comment is hidden%% text.
        """.trimIndent()

        val blocks = MarkdownParser.parse(note)

        assertTrue(blocks.any { it is MarkdownBlock.Callout && it.kind == "WARNING" && it.title == "Read this first" })
        assertTrue(blocks.any { it is MarkdownBlock.Table && it.headers == listOf("Feature", "Status") })
        assertTrue(blocks.filterIsInstance<MarkdownBlock.Paragraph>().none { "comment" in it.text })
    }

    @Test
    fun `resolves relative markdown links`() {
        val paths = listOf("notes/Index.md", "reference/Guide.md")
        assertEquals("reference/Guide.md", resolveWikiTarget("notes/Index.md", "../reference/Guide.md", paths))
    }
}
