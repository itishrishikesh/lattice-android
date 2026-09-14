package com.lattice.notes.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckBox
import androidx.compose.material.icons.rounded.CheckBoxOutlineBlank
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
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
    val listState = rememberLazyListState()
    val progress by remember(blocks.size) {
        derivedStateOf {
            if (blocks.isEmpty()) 0f
            else ((listState.firstVisibleItemIndex + 1f) / blocks.size).coerceIn(0f, 1f)
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
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 72.dp)
            ) {
                item {
                    Text(
                        file.path.substringBeforeLast('/', "Vault"),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(Modifier.height(12.dp))
                }
                items(blocks.size, key = { it }) { index ->
                    MarkdownBlockView(blocks[index], onWikiLink)
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

@Suppress("DEPRECATION")
@Composable
private fun InlineMarkdown(text: String, style: TextStyle, onWikiLink: (String) -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val surface = MaterialTheme.colorScheme.surfaceVariant
    val annotated = remember(text, primary, secondary, surface) { inlineAnnotated(text, primary, secondary, surface) }
    ClickableText(
        text = annotated,
        style = style,
        onClick = { offset ->
            annotated.getStringAnnotations("wiki", offset, offset).firstOrNull()?.let { onWikiLink(it.item) }
        }
    )
}

private fun inlineAnnotated(text: String, link: Color, tag: Color, codeBackground: Color): AnnotatedString = buildAnnotatedString {
    val token = Regex("(\\[\\[[^]]+]]|`[^`]+`|\\*\\*[^*]+\\*\\*|~~[^~]+~~|(?<![\\w/])#[\\p{L}\\p{N}_/-]+)")
    var cursor = 0
    token.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val value = match.value
        when {
            value.startsWith("[[") -> {
                val inside = value.removePrefix("[[").removeSuffix("]]"
                    )
                val target = inside.substringBefore('|')
                val label = inside.substringAfter('|', target).substringAfterLast('/')
                pushStringAnnotation("wiki", target)
                pushStyle(SpanStyle(color = link, fontWeight = FontWeight.SemiBold))
                append(label)
                pop(); pop()
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
            value.startsWith("#") -> {
                pushStyle(SpanStyle(color = tag, fontWeight = FontWeight.Medium)); append(value); pop()
            }
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}
