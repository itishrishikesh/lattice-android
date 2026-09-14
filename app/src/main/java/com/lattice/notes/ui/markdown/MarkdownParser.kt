package com.lattice.notes.ui.markdown

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String, val depth: Int) : MarkdownBlock
    data class Numbered(val number: String, val text: String, val depth: Int) : MarkdownBlock
    data class Task(val checked: Boolean, val text: String, val depth: Int) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val language: String?, val value: String) : MarkdownBlock
    data class FrontMatter(val properties: List<Pair<String, String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
}

object MarkdownParser {
    private val heading = Regex("^(#{1,6})\\s+(.+)$")
    private val task = Regex("^(\\s*)[-*+]\\s+\\[([ xX])]\\s+(.+)$")
    private val bullet = Regex("^(\\s*)[-*+]\\s+(.+)$")
    private val numbered = Regex("^(\\s*)(\\d+[.)])\\s+(.+)$")

    fun parse(markdown: String): List<MarkdownBlock> {
        if (markdown.isBlank()) return emptyList()
        val lines = markdown.replace("\r\n", "\n").lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var index = 0

        if (lines.firstOrNull()?.trim() == "---") {
            val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (end >= 0) {
                val properties = lines.subList(1, end + 1).mapNotNull { line ->
                    val key = line.substringBefore(':', "").trim()
                    val value = line.substringAfter(':', "").trim()
                    if (key.isNotBlank() && value.isNotBlank()) key to value else null
                }
                if (properties.isNotEmpty()) blocks += MarkdownBlock.FrontMatter(properties)
                index = end + 2
            }
        }

        val paragraph = mutableListOf<String>()
        fun flushParagraph() {
            if (paragraph.isNotEmpty()) {
                blocks += MarkdownBlock.Paragraph(paragraph.joinToString(" ") { it.trim() })
                paragraph.clear()
            }
        }

        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()
            if (trimmed.startsWith("```")) {
                flushParagraph()
                val language = trimmed.removePrefix("```").trim().takeIf(String::isNotBlank)
                index++
                val code = mutableListOf<String>()
                while (index < lines.size && !lines[index].trim().startsWith("```")) {
                    code += lines[index]
                    index++
                }
                blocks += MarkdownBlock.Code(language, code.joinToString("\n"))
            } else when {
                trimmed.isBlank() -> flushParagraph()
                trimmed.matches(Regex("^([-*_])\\1{2,}$")) -> {
                    flushParagraph(); blocks += MarkdownBlock.Divider
                }
                heading.matches(trimmed) -> {
                    flushParagraph()
                    val match = heading.matchEntire(trimmed)!!
                    blocks += MarkdownBlock.Heading(match.groupValues[1].length, match.groupValues[2])
                }
                task.matches(line) -> {
                    flushParagraph()
                    val match = task.matchEntire(line)!!
                    blocks += MarkdownBlock.Task(
                        checked = match.groupValues[2].equals("x", true),
                        text = match.groupValues[3],
                        depth = match.groupValues[1].length / 2
                    )
                }
                numbered.matches(line) -> {
                    flushParagraph()
                    val match = numbered.matchEntire(line)!!
                    blocks += MarkdownBlock.Numbered(match.groupValues[2], match.groupValues[3], match.groupValues[1].length / 2)
                }
                bullet.matches(line) -> {
                    flushParagraph()
                    val match = bullet.matchEntire(line)!!
                    blocks += MarkdownBlock.Bullet(match.groupValues[2], match.groupValues[1].length / 2)
                }
                trimmed.startsWith(">") -> {
                    flushParagraph(); blocks += MarkdownBlock.Quote(trimmed.removePrefix(">").trim())
                }
                else -> paragraph += line
            }
            index++
        }
        flushParagraph()
        return blocks
    }
}

fun resolveWikiTarget(currentPath: String, target: String, paths: List<String>): String? {
    val clean = target.substringBefore('#').trim().removeSuffix(".md")
    val parent = currentPath.substringBeforeLast('/', "")
    return paths.firstOrNull { it.removeSuffix(".md").equals(clean, true) }
        ?: paths.firstOrNull {
            it.substringBeforeLast('/', "") == parent &&
                it.substringAfterLast('/').removeSuffix(".md").equals(clean, true)
        }
        ?: paths.firstOrNull {
            it.substringAfterLast('/').removeSuffix(".md").equals(clean.substringAfterLast('/'), true)
        }
}
