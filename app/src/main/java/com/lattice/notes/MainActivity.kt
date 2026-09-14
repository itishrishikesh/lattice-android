package com.lattice.notes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lattice.notes.ui.screens.ExplorerScreen
import com.lattice.notes.ui.screens.ReaderScreen
import com.lattice.notes.ui.screens.RepositoryScreen
import com.lattice.notes.ui.screens.SignInScreen
import com.lattice.notes.ui.theme.LatticeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel()
            val state by viewModel.state.collectAsState()
            val snackbar = remember { SnackbarHostState() }

            LatticeTheme(darkTheme = state.darkTheme) {
                LaunchedEffect(state.message) {
                    state.message?.let {
                        snackbar.showSnackbar(it)
                        viewModel.clearMessage()
                    }
                }
                BackHandler(enabled = state.screen == AppScreen.EXPLORER || state.screen == AppScreen.READER) {
                    viewModel.back()
                }
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = MaterialTheme.colorScheme.background,
                    snackbarHost = { SnackbarHost(snackbar) }
                ) { _ ->
                    Crossfade(targetState = state.screen, label = "screen") { screen ->
                        when (screen) {
                            AppScreen.SIGN_IN -> SignInScreen(
                                loading = state.loading,
                                deviceCode = state.deviceCode,
                                oauthAvailable = BuildConfig.GITHUB_CLIENT_ID.isNotBlank(),
                                onDeviceLogin = viewModel::startDeviceLogin,
                                onTokenLogin = viewModel::authenticate
                            )
                            AppScreen.REPOSITORIES -> RepositoryScreen(
                                user = state.user,
                                repositories = state.repositories,
                                recentNotes = state.recentNotes,
                                query = state.repositoryQuery,
                                loading = state.loading,
                                darkTheme = state.darkTheme,
                                onQueryChange = viewModel::setRepositoryQuery,
                                onSelect = viewModel::selectRepository,
                                onRecent = viewModel::openRecent,
                                onRefresh = viewModel::refresh,
                                onThemeChange = viewModel::setDarkTheme,
                                onLogout = viewModel::logout
                            )
                            AppScreen.EXPLORER -> state.selectedRepository?.let { repository ->
                                ExplorerScreen(
                                    repository = repository,
                                    files = state.files,
                                    currentFolder = state.currentFolder,
                                    query = state.fileQuery,
                                    loading = state.loading,
                                    onBack = { viewModel.back() },
                                    onQueryChange = viewModel::setFileQuery,
                                    onOpenFolder = viewModel::openFolder,
                                    onOpenFile = viewModel::openFile,
                                    onRefresh = viewModel::refresh
                                )
                            }
                            AppScreen.READER -> {
                                val repository = state.selectedRepository
                                val file = state.currentFile
                                if (repository != null && file != null) {
                                    ReaderScreen(
                                        repository = repository,
                                        file = file,
                                        content = state.noteContent,
                                        loading = state.loading,
                                        onBack = { viewModel.back() },
                                        onWikiLink = viewModel::followWikiLink
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
