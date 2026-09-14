package com.lattice.notes.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lattice.notes.data.RepoFile
import com.lattice.notes.data.Repository
import com.lattice.notes.ui.components.EmptyState
import com.lattice.notes.ui.components.LoadingPane
import com.lattice.notes.ui.components.SearchField
import com.lattice.notes.ui.components.SectionLabel

private data class ExplorerEntry(val path: String, val folder: Boolean, val file: RepoFile? = null) {
    val name get() = path.substringAfterLast('/')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    repository: Repository,
    files: List<RepoFile>,
    currentFolder: String,
    query: String,
    loading: Boolean,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onOpenFolder: (String) -> Unit,
    onOpenFile: (RepoFile) -> Unit,
    onRefresh: () -> Unit
) {
    val entries = remember(files, currentFolder, query) { explorerEntries(files, currentFolder, query) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(repository.name, style = MaterialTheme.typography.titleMedium)
                        Text(repository.defaultBranch, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                actions = { IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Refresh") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (loading && files.isEmpty()) {
            Box(Modifier.padding(padding)) { LoadingPane("Opening the vault…") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 36.dp)
            ) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(8.dp))
                        Breadcrumb(currentFolder, onOpenFolder)
                        Spacer(Modifier.height(16.dp))
                        SearchField(query, onQueryChange, "Search every note")
                        SectionLabel(
                            if (query.isBlank()) "${entries.size} items" else "${entries.size} matches",
                            Modifier.padding(top = 22.dp, bottom = 8.dp)
                        )
                    }
                }
                if (entries.isEmpty()) {
                    item {
                        EmptyState(
                            if (query.isBlank()) "This folder is empty" else "No notes found",
                            if (query.isBlank()) "There are no files to show here." else "Try a title or folder name.",
                            if (query.isBlank()) "◇" else "⌕"
                        )
                    }
                } else {
                    items(entries, key = ExplorerEntry::path) { entry ->
                        ExplorerRow(entry, query.isNotBlank(), onOpenFolder, onOpenFile)
                    }
                }
            }
        }
    }
}

@Composable
private fun Breadcrumb(folder: String, onOpenFolder: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = { onOpenFolder("") }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
            Text("Vault", style = MaterialTheme.typography.titleLarge)
        }
        var built = ""
        folder.split('/').filter(String::isNotBlank).forEach { segment ->
            built = if (built.isBlank()) segment else "$built/$segment"
            val destination = built
            Icon(Icons.Rounded.ChevronRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.outline)
            TextButton(onClick = { onOpenFolder(destination) }, contentPadding = PaddingValues(horizontal = 3.dp, vertical = 0.dp)) {
                Text(segment, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun ExplorerRow(
    entry: ExplorerEntry,
    showPath: Boolean,
    onOpenFolder: (String) -> Unit,
    onOpenFile: (RepoFile) -> Unit
) {
    val enabled = entry.folder || entry.file?.isMarkdown == true
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(enabled = enabled) { if (entry.folder) onOpenFolder(entry.path) else entry.file?.let(onOpenFile) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(11.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
                Icon(
                    when { entry.folder -> Icons.Rounded.Folder; entry.file?.isMarkdown == true -> Icons.Rounded.Description; else -> Icons.AutoMirrored.Rounded.InsertDriveFile },
                    null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(Modifier.weight(1f)) {
            Text(
                entry.name.removeSuffix(".md"),
                style = MaterialTheme.typography.titleMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (showPath || (!entry.folder && entry.file?.isMarkdown != true)) {
                Text(
                    if (showPath) entry.path.substringBeforeLast('/', "Root") else "Preview not available",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (entry.folder) Icon(Icons.Rounded.ChevronRight, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.outline)
    }
}

private fun explorerEntries(files: List<RepoFile>, folder: String, query: String): List<ExplorerEntry> {
    if (query.isNotBlank()) {
        return files.asSequence()
            .filter(RepoFile::isMarkdown)
            .filter { it.path.contains(query.trim(), ignoreCase = true) }
            .map { ExplorerEntry(it.path, false, it) }
            .sortedBy { it.path.lowercase() }
            .toList()
    }
    val prefix = folder.takeIf(String::isNotBlank)?.plus("/").orEmpty()
    val folders = linkedSetOf<String>()
    val directFiles = mutableListOf<RepoFile>()
    files.forEach { file ->
        if (!file.path.startsWith(prefix) || file.path == folder) return@forEach
        val relative = file.path.removePrefix(prefix)
        if ('/' in relative) folders += prefix + relative.substringBefore('/')
        else if (file.type == "blob") directFiles += file
    }
    return folders.sortedBy(String::lowercase).map { ExplorerEntry(it, true) } +
        directFiles.sortedWith(compareByDescending<RepoFile> { it.isMarkdown }.thenBy { it.name.lowercase() })
            .map { ExplorerEntry(it.path, false, it) }
}
