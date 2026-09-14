package com.lattice.notes.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.lattice.notes.data.GitHubUser
import com.lattice.notes.data.RecentNote
import com.lattice.notes.data.Repository
import com.lattice.notes.ui.components.EmptyState
import com.lattice.notes.ui.components.LoadingPane
import com.lattice.notes.ui.components.SearchField
import com.lattice.notes.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryScreen(
    user: GitHubUser?,
    repositories: List<Repository>,
    recentNotes: List<RecentNote>,
    query: String,
    loading: Boolean,
    darkTheme: Boolean,
    onQueryChange: (String) -> Unit,
    onSelect: (Repository) -> Unit,
    onRecent: (RecentNote) -> Unit,
    onRefresh: () -> Unit,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    val filtered = remember(repositories, query) {
        if (query.isBlank()) repositories else repositories.filter {
            it.fullName.contains(query, true) || it.description?.contains(query, true) == true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Lattice", style = MaterialTheme.typography.titleLarge)
                        Text(
                            user?.name ?: user?.login.orEmpty(),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    Surface(
                        modifier = Modifier.padding(start = 16.dp, end = 8.dp).size(38.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                (user?.name ?: user?.login ?: "L").take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) { Icon(Icons.Rounded.Refresh, "Refresh repositories") }
                    Box {
                        IconButton(onClick = { menuOpen = true }) { Icon(Icons.Rounded.MoreVert, "Account menu") }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(if (darkTheme) "Use light theme" else "Use dark theme") },
                                leadingIcon = { Icon(if (darkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode, null) },
                                onClick = { onThemeChange(!darkTheme); menuOpen = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Sign out") },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Logout, null) },
                                onClick = { onLogout(); menuOpen = false }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        if (loading && repositories.isEmpty()) {
            Box(Modifier.padding(padding)) { LoadingPane("Finding your repositories…") }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 36.dp)
            ) {
                item {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        Spacer(Modifier.height(10.dp))
                        Text("Choose your vault", style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Select the repository where your Obsidian notes live.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(22.dp))
                        SearchField(query, onQueryChange, "Search repositories")
                    }
                }

                if (recentNotes.isNotEmpty() && query.isBlank()) {
                    item {
                        SectionLabel("Recently read", Modifier.padding(start = 20.dp, top = 28.dp, bottom = 12.dp))
                        LazyRow(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(recentNotes.take(8), key = { "${it.repoFullName}:${it.path}" }) { recent ->
                                RecentNoteCard(recent, onRecent)
                            }
                        }
                    }
                }

                item { SectionLabel("Repositories · ${filtered.size}", Modifier.padding(start = 20.dp, top = 28.dp, bottom = 10.dp)) }
                if (filtered.isEmpty()) {
                    item { EmptyState("No repositories found", "Try a different search or refresh your account.", "⌕") }
                } else {
                    items(filtered, key = Repository::id) { repository ->
                        RepositoryRow(repository, onSelect)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentNoteCard(note: RecentNote, onClick: (RecentNote) -> Unit) {
    Surface(
        modifier = Modifier.width(210.dp).clickable { onClick(note) },
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(Modifier.padding(15.dp)) {
            Text(
                note.path.substringAfterLast('/').removeSuffix(".md"),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(5.dp))
            Text(note.repoFullName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RepositoryRow(repository: Repository, onClick: (Repository) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick(repository) }.padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Storage, null, tint = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(repository.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (repository.isPrivate) {
                    Spacer(Modifier.width(6.dp)); Icon(Icons.Rounded.Lock, "Private", Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                repository.description ?: repository.fullName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (repository.stargazers > 0) {
            Icon(Icons.Rounded.Star, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(3.dp))
            Text(repository.stargazers.toString(), style = MaterialTheme.typography.labelMedium)
        }
    }
}
