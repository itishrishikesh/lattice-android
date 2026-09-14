package com.lattice.notes.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lattice.notes.data.RepoFile
import com.lattice.notes.data.Repository
import com.lattice.notes.ui.components.LoadingPane
import com.lattice.notes.ui.markdown.MarkdownBlock
import com.lattice.notes.ui.markdown.MarkdownParser

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    repository: Repository,
    file: RepoFile,
    content: String,
    loading: Boolean,
    onBack: () -> Unit,
    onWikiLink: (String) -> Unit
) {
    val context = LocalContext.current
    val blocks = remember(content) { MarkdownParser.parse(content) }
    val scrollState = rememberScrollState()
    val progress by remember {
        derivedStateOf {
            if (scrollState.maxValue == 0) 0f
            else (scrollState.value.toFloat() / scrollState.maxValue).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(file.name.removeSuffix(".md"), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                            Text(repository.name, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                    actions = {
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(file.name, content))
                            Toast.makeText(context, "Note copied", Toast.LENGTH_SHORT).show()
                        }) { Icon(Icons.Rounded.ContentCopy, "Copy entire note") }
                        IconButton(onClick = {
                            val share = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, file.name.removeSuffix(".md"))
                                putExtra(Intent.EXTRA_TEXT, content)
                            }
                            context.startActivity(Intent.createChooser(share, "Share note"))
                        }) { Icon(Icons.Rounded.Share, "Share note") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(2.dp),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = Color.Transparent
                )
            }
        }
    ) { padding ->
        when {
            loading && content.isBlank() -> Box(Modifier.padding(padding)) { LoadingPane("Opening note…") }
            content.isBlank() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("This note is empty.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> SelectionContainer {
                Column(
                    modifier = Modifier.fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scrollState)
                        .padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 72.dp)
                ) {
                    Text(
                        file.path.substringBeforeLast('/', "Vault"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(12.dp))
                    blocks.forEach { block -> MarkdownBlockView(block, onWikiLink) }
                }
            }
        }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock, onWikiLink: (String) -> Unit) {
    when (block) {
        is MarkdownBlock.Heading -> {
            val style = when (block.level) {
                1 -> MaterialTheme.typography.headlineMedium
                2 -> MaterialTheme.typography.headlineSmall
                3 -> MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Serif)
                else -> MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Serif)
            }
            Spacer(Modifier.height(if (block.level <= 2) 22.dp else 14.dp))
            InlineMarkdown(block.text, style, onWikiLink)
            Spacer(Modifier.height(8.dp))
        }
        is MarkdownBlock.Paragraph -> {
            InlineMarkdown(block.text, MaterialTheme.typography.bodyLarge, onWikiLink)
            Spacer(Modifier.height(15.dp))
        }
        is MarkdownBlock.Bullet -> ListLine("•", block.text, block.depth, onWikiLink)
        is MarkdownBlock.Numbered -> ListLine(block.number, block.text, block.depth, onWikiLink)
        is MarkdownBlock.Task -> {
            Row(
                Modifier.fillMaxWidth().padding(start = (block.depth * 16).dp, bottom = 9.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    if (block.checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                    null,
                    Modifier.padding(top = 3.dp).size(20.dp),
                    tint = if (block.checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.width(10.dp))
                InlineMarkdown(
                    block.text,
                    MaterialTheme.typography.bodyLarge.copy(
                        color = if (block.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (block.checked) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    onWikiLink
                )
            }
        }
        is MarkdownBlock.Quote -> {
            Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
                Box(Modifier.width(3.dp).height(48.dp).background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(2.dp)))
                Spacer(Modifier.width(15.dp))
                InlineMarkdown(
                    block.text,
                    MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant),
                    onWikiLink
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        is MarkdownBlock.Callout -> CalloutBlock(block, onWikiLink)
        is MarkdownBlock.Table -> TableBlock(block, onWikiLink)
        is MarkdownBlock.Code -> {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(Modifier.padding(16.dp)) {
                    block.language?.let {
                        Text(it.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(
                        block.value,
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        is MarkdownBlock.FrontMatter -> {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    block.properties.forEach { (key, value) ->
                        Row {
                            Text(key, Modifier.width(90.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        MarkdownBlock.Divider -> HorizontalDivider(Modifier.padding(vertical = 22.dp), color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
private fun ListLine(marker: String, text: String, depth: Int, onWikiLink: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = (depth * 16).dp, bottom = 8.dp), verticalAlignment = Alignment.Top) {
        Text(marker, Modifier.width(26.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        InlineMarkdown(text, MaterialTheme.typography.bodyLarge, onWikiLink)
    }
}

@Composable
private fun InlineMarkdown(text: String, style: TextStyle, onWikiLink: (String) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val annotated = remember(text, primary, secondary, surface, onWikiLink) {
        inlineAnnotated(text, primary, secondary, surface, onWikiLink)
    }
    Text(
        text = annotated,
        style = style
    )
}

@Composable
private fun CalloutBlock(block: MarkdownBlock.Callout, onWikiLink: (String) -> Unit) {
    val accent = when (block.kind) {
        "WARNING", "CAUTION", "ATTENTION" -> MaterialTheme.colorScheme.secondary
        "DANGER", "ERROR", "BUG" -> MaterialTheme.colorScheme.error
        "TIP", "SUCCESS", "DONE" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.primary
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        shape = RoundedCornerShape(14.dp),
        color = accent.copy(alpha = 0.10f)
    ) {
        Row {
            Box(Modifier.width(4.dp).height(88.dp).background(accent))
            Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp).weight(1f)) {
                Text(block.title, style = MaterialTheme.typography.titleMedium, color = accent)
                if (block.body.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    InlineMarkdown(block.body, MaterialTheme.typography.bodyMedium, onWikiLink)
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun TableBlock(block: MarkdownBlock.Table, onWikiLink: (String) -> Unit) {
    val columnCount = maxOf(block.headers.size, block.rows.maxOfOrNull { it.size } ?: 0)
    Surface(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.horizontalScroll(rememberScrollState())) {
            TableRow(block.headers, columnCount, header = true, onWikiLink)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            block.rows.forEachIndexed { index, cells ->
                TableRow(cells, columnCount, header = false, onWikiLink)
                if (index != block.rows.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun TableRow(cells: List<String>, count: Int, header: Boolean, onWikiLink: (String) -> Unit) {
    Row(Modifier.background(if (header) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)) {
        repeat(count) { index ->
            Box(Modifier.width(156.dp).padding(horizontal = 12.dp, vertical = 10.dp)) {
                InlineMarkdown(
                    cells.getOrElse(index) { "" },
                    MaterialTheme.typography.bodyMedium.copy(fontWeight = if (header) FontWeight.SemiBold else FontWeight.Normal),
                    onWikiLink
                )
            }
        }
    }
}

private fun inlineAnnotated(
    text: String,
    link: Color,
    tag: Color,
    codeBackground: Color,
    onWikiLink: (String) -> Unit
): AnnotatedString = buildAnnotatedString {
    val token = Regex(
        """(!?\[\[[^]]+]]|!\[[^]]*]\([^)]+\)|\[[^]]+]\([^)]+\)|<https?://[^>]+>|https?://[^\s<>()]+|==[^=\n]+==|\*\*\*[^*]+\*\*\*|\*\*[^*]+\*\*|~~[^~]+~~|`[^`]+`|(?<!\*)\*[^*\n]+\*(?!\*)|(?<!\w)_[^_\n]+_(?!\w)|(?<![\w/])#[\p{L}\p{N}_/-]+)"""
    )
    var cursor = 0
    token.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val value = match.value
        when {
            value.startsWith("[[") || value.startsWith("![[") -> {
                val embedded = value.startsWith("![[")
                val inside = value.removePrefix(if (embedded) "![[" else "[[").removeSuffix("]]" )
                val target = inside.substringBefore('|')
                val fallback = target.substringBefore('#').substringAfterLast('/').removeSuffix(".md")
                    .ifBlank { target.substringAfter('#', "This note") }
                val label = inside.substringAfter('|', fallback)
                withLink(
                    LinkAnnotation.Clickable(
                        tag = target,
                        styles = TextLinkStyles(
                            style = SpanStyle(color = link, background = link.copy(alpha = 0.10f), fontWeight = FontWeight.SemiBold),
                            pressedStyle = SpanStyle(color = link, background = link.copy(alpha = 0.22f))
                        ),
                        linkInteractionListener = LinkInteractionListener { onWikiLink(target) }
                    )
                ) { append(if (embedded) "Attachment · $label" else label) }
            }
            value.startsWith("![") -> {
                val alt = value.substringAfter("![").substringBefore(']')
                val url = value.substringAfter("](").removeSuffix(")")
                withLink(externalOrInternalLink(url, link, onWikiLink)) {
                    append("Image · ${alt.ifBlank { url.substringAfterLast('/') }}")
                }
            }
            value.startsWith("[") -> {
                val label = value.substringAfter('[').substringBefore(']')
                val url = value.substringAfter("](").removeSuffix(")")
                withLink(externalOrInternalLink(url, link, onWikiLink)) { append(label) }
            }
            value.startsWith("<http") -> {
                val url = value.removeSurrounding("<", ">")
                withLink(externalLink(url, link)) { append(url); append(" ↗") }
            }
            value.startsWith("http") -> {
                val url = value.trimEnd('.', ',', ';', ':')
                withLink(externalLink(url, link)) { append(url); append(" ↗") }
                append(value.removePrefix(url))
            }
            value.startsWith("==") -> {
                pushStyle(SpanStyle(background = tag.copy(alpha = 0.18f), fontWeight = FontWeight.Medium))
                append(value.removeSurrounding("==")); pop()
            }
            value.startsWith("***") -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                append(value.removeSurrounding("***")); pop()
            }
            value.startsWith("**") -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold)); append(value.removeSurrounding("**")); pop()
            }
            value.startsWith("~~") -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)); append(value.removeSurrounding("~~")); pop()
            }
            value.startsWith("`") -> {
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)); append(value.removeSurrounding("`")); pop()
            }
            value.startsWith("*") || value.startsWith("_") -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(value.substring(1, value.length - 1)); pop()
            }
            value.startsWith("#") -> {
                pushStyle(SpanStyle(color = tag, fontWeight = FontWeight.Medium)); append(value); pop()
            }
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}

private fun externalOrInternalLink(url: String, color: Color, onWikiLink: (String) -> Unit): LinkAnnotation =
    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("obsidian://") || url.startsWith("mailto:")) {
        externalLink(url, color)
    } else {
        LinkAnnotation.Clickable(
            tag = url,
            styles = linkStyles(color),
            linkInteractionListener = LinkInteractionListener { onWikiLink(url) }
        )
    }

private fun externalLink(url: String, color: Color): LinkAnnotation.Url =
    LinkAnnotation.Url(url = url, styles = linkStyles(color))

private fun linkStyles(color: Color) = TextLinkStyles(
    style = SpanStyle(color = color, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
    pressedStyle = SpanStyle(color = color, background = color.copy(alpha = 0.16f), textDecoration = TextDecoration.Underline)
)
