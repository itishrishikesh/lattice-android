package com.lattice.notes.ui.markdown

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Bullet(val text: String, val depth: Int) : MarkdownBlock
    data class Numbered(val number: String, val text: String, val depth: Int) : MarkdownBlock
    data class Task(val checked: Boolean, val text: String, val depth: Int) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Callout(val kind: String, val title: String, val body: String) : MarkdownBlock
    data class Table(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock
    data class Code(val language: String?, val value: String) : MarkdownBlock
    data class FrontMatter(val properties: List<Pair<String, String>>) : MarkdownBlock
    data object Divider : MarkdownBlock
}

object MarkdownParser {
    private val heading = Regex("^(#{1,6})\\s+(.+)$")
    private val task = Regex("^(\\s*)[-*+]\\s+\\[([ xX])]\\s+(.+)$")
    private val bullet = Regex("^(\\s*)[-*+]\\s+(.+)$")
    private val numbered = Regex("^(\\s*)(\\d+[.)])\\s+(.+)$")
    private val callout = Regex("^\\[!([A-Za-z0-9_-]+)](?:[+-])?\\s*(.*)$")
    private val tableDivider = Regex("^:?-{3,}:?$")

    fun parse(markdown: String): List<MarkdownBlock> {
        if (markdown.isBlank()) return emptyList()
        val visibleMarkdown = markdown
            .replace("\r\n", "\n")
            .replace(Regex("(?s)%%.*?%%"), "")
        val lines = visibleMarkdown.lines()
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
            } else if (isTableStart(lines, index)) {
                flushParagraph()
                val headers = tableCells(lines[index])
                index += 2
                val rows = mutableListOf<List<String>>()
                while (index < lines.size && lines[index].contains('|') && lines[index].isNotBlank()) {
                    rows += tableCells(lines[index])
                    index++
                }
                blocks += MarkdownBlock.Table(headers, rows)
                continue
            } else if (trimmed.startsWith(">")) {
                flushParagraph()
                val quoted = mutableListOf<String>()
                while (index < lines.size && lines[index].trimStart().startsWith(">")) {
                    quoted += lines[index].trimStart().removePrefix(">").removePrefix(" ")
                    index++
                }
                val match = callout.matchEntire(quoted.firstOrNull().orEmpty().trim())
                if (match != null) {
                    val kind = match.groupValues[1].uppercase()
                    val title = match.groupValues[2].ifBlank {
                        kind.lowercase().replaceFirstChar { it.titlecase() }
                    }
                    blocks += MarkdownBlock.Callout(kind, title, quoted.drop(1).joinToString("\n").trim())
                } else {
                    blocks += MarkdownBlock.Quote(quoted.joinToString("\n").trim())
                }
                continue
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
                else -> paragraph += line
            }
            index++
        }
        flushParagraph()
        return blocks
    }

    private fun isTableStart(lines: List<String>, index: Int): Boolean {
        if (index + 1 >= lines.size || !lines[index].contains('|')) return false
        val divider = tableCells(lines[index + 1])
        return divider.isNotEmpty() && divider.all { tableDivider.matches(it.replace(" ", "")) }
    }

    private fun tableCells(line: String): List<String> = line.trim().trim('|').split('|').map(String::trim)
}

fun resolveWikiTarget(currentPath: String, target: String, paths: List<String>): String? {
    val clean = target.substringBefore('#').substringBefore('?').trim()
        .replace("%20", " ")
        .removePrefix("/")
        .removeSuffix(".md")
    val parent = currentPath.substringBeforeLast('/', "")
    val relative = normalizePath(if (parent.isBlank()) clean else "$parent/$clean")
    return paths.firstOrNull { it.removeSuffix(".md").equals(relative, true) }
        ?: paths.firstOrNull { it.removeSuffix(".md").equals(clean, true) }
        ?: paths.firstOrNull {
            it.substringBeforeLast('/', "") == parent &&
                it.substringAfterLast('/').removeSuffix(".md").equals(clean, true)
        }
        ?: paths.firstOrNull {
                it.substringAfterLast('/').removeSuffix(".md").equals(clean.substringAfterLast('/'), true)
        }
}

private fun normalizePath(path: String): String {
    val parts = mutableListOf<String>()
    path.split('/').forEach { part ->
        when (part) {
            "", "." -> Unit
            ".." -> if (parts.isNotEmpty()) parts.removeAt(parts.lastIndex)
            else -> parts += part
        }
    }
    return parts.joinToString("/")
}
