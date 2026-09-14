package com.lattice.notes

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lattice.notes.data.DeviceCode
import com.lattice.notes.data.GitHubApi
import com.lattice.notes.data.GitHubUser
import com.lattice.notes.data.NoteCache
import com.lattice.notes.data.RecentNote
import com.lattice.notes.data.RecentStore
import com.lattice.notes.data.RepoFile
import com.lattice.notes.data.Repository
import com.lattice.notes.data.SecureTokenStore
import com.lattice.notes.data.TokenPollResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class AppScreen { SIGN_IN, REPOSITORIES, EXPLORER, READER }

data class AppState(
    val screen: AppScreen = AppScreen.SIGN_IN,
    val user: GitHubUser? = null,
    val repositories: List<Repository> = emptyList(),
    val selectedRepository: Repository? = null,
    val files: List<RepoFile> = emptyList(),
    val currentFolder: String = "",
    val currentFile: RepoFile? = null,
    val noteContent: String = "",
    val repositoryQuery: String = "",
    val fileQuery: String = "",
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val deviceCode: DeviceCode? = null,
    val message: String? = null,
    val darkTheme: Boolean = false,
    val recentNotes: List<RecentNote> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val api = GitHubApi()
    private val tokenStore = SecureTokenStore(application)
    private val recentStore = RecentStore(application)
    private val cache = NoteCache(application)
    private var token: String? = null
    private var authJob: Job? = null

    private val _state = MutableStateFlow(AppState(recentNotes = recentStore.load()))
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        tokenStore.read()?.let(::authenticate)
    }

    fun authenticate(candidate: String) {
        val cleaned = candidate.trim()
        if (cleaned.isBlank()) {
            showMessage("Enter a GitHub token to continue.")
            return
        }
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null, deviceCode = null) }
            runCatching {
                val user = api.currentUser(cleaned)
                val repos = api.repositories(cleaned)
                Triple(cleaned, user, repos)
            }.onSuccess { (validToken, user, repos) ->
                token = validToken
                tokenStore.save(validToken)
                _state.update {
                    it.copy(
                        screen = AppScreen.REPOSITORIES,
                        user = user,
                        repositories = repos,
                        loading = false
                    )
                }
            }.onFailure { failure ->
                tokenStore.clear()
                _state.update { it.copy(loading = false, message = friendlyError(failure)) }
            }
        }
    }

    fun startDeviceLogin() {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (clientId.isBlank()) {
            showMessage("GitHub OAuth is not configured in this build. Use a fine-grained token below.")
            return
        }
        authJob?.cancel()
        authJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            runCatching { api.requestDeviceCode(clientId) }
                .onSuccess { code ->
                    _state.update { it.copy(loading = false, deviceCode = code) }
                    pollForToken(clientId, code)
                }
                .onFailure { _state.update { state -> state.copy(loading = false, message = friendlyError(it)) } }
        }
    }

    private suspend fun pollForToken(clientId: String, code: DeviceCode) {
        var interval = code.interval.coerceAtLeast(5)
        val started = System.currentTimeMillis()
        while (viewModelScope.isActive && System.currentTimeMillis() - started < code.expiresIn * 1_000L) {
            delay(interval * 1_000L)
            when (val result = runCatching { api.pollDeviceToken(clientId, code.deviceCode) }
                .getOrElse { TokenPollResult.Error(friendlyError(it)) }) {
                is TokenPollResult.Success -> {
                    authenticate(result.token)
                    return
                }
                TokenPollResult.Pending -> Unit
                is TokenPollResult.SlowDown -> interval += result.seconds
                is TokenPollResult.Error -> {
                    _state.update { it.copy(deviceCode = null, message = result.message) }
                    return
                }
            }
        }
        _state.update { it.copy(deviceCode = null, message = "The sign-in code expired. Please try again.") }
    }

    fun selectRepository(repository: Repository) {
        val auth = token ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null, selectedRepository = repository) }
            runCatching { api.repositoryTree(auth, repository) }
                .onSuccess { files ->
                    _state.update {
                        it.copy(
                            screen = AppScreen.EXPLORER,
                            files = files,
                            currentFolder = "",
                            fileQuery = "",
                            loading = false
                        )
                    }
                }
                .onFailure { failure ->
                    _state.update { it.copy(loading = false, selectedRepository = null, message = friendlyError(failure)) }
                }
        }
    }

    fun openFolder(path: String) {
        _state.update { it.copy(currentFolder = path.trim('/'), fileQuery = "") }
    }

    fun openFile(file: RepoFile) {
        val auth = token ?: return
        val repo = _state.value.selectedRepository ?: return
        val cached = cache.read(repo.fullName, file.path)
        _state.update {
            it.copy(
                screen = AppScreen.READER,
                currentFile = file,
                noteContent = cached.orEmpty(),
                loading = cached == null,
                message = null
            )
        }
        viewModelScope.launch {
            runCatching { api.rawFile(auth, repo, file.path) }
                .onSuccess { content ->
                    cache.write(repo.fullName, file.path, content)
                    recentStore.add(repo.fullName, file.path)
                    _state.update {
                        it.copy(noteContent = content, loading = false, recentNotes = recentStore.load())
                    }
                }
                .onFailure { failure ->
                    _state.update {
                        it.copy(
                            loading = false,
                            message = if (cached != null) "Showing the saved copy. ${friendlyError(failure)}" else friendlyError(failure)
                        )
                    }
                }
        }
    }

    fun followWikiLink(target: String) {
        val normalized = target.substringBefore('#').trim().removeSuffix(".md")
        val currentParent = _state.value.currentFile?.parent.orEmpty()
        val candidates = _state.value.files.filter(RepoFile::isMarkdown)
        val match = candidates.firstOrNull {
            it.path.removeSuffix(".md").equals(normalized, ignoreCase = true)
        } ?: candidates.firstOrNull {
            it.name.removeSuffix(".md").equals(normalized, ignoreCase = true) && it.parent == currentParent
        } ?: candidates.firstOrNull {
            it.name.removeSuffix(".md").equals(normalized.substringAfterLast('/'), ignoreCase = true)
        }
        if (match != null) openFile(match) else showMessage("“$target” isn’t in this repository.")
    }

    fun openRecent(note: RecentNote) {
        val repository = _state.value.repositories.firstOrNull { it.fullName == note.repoFullName }
        if (repository == null) {
            showMessage("That repository is not available in the current account.")
            return
        }
        if (_state.value.selectedRepository?.fullName == repository.fullName) {
            _state.value.files.firstOrNull { it.path == note.path }?.let(::openFile)
                ?: showMessage("That note may have moved.")
        } else {
            val auth = token ?: return
            viewModelScope.launch {
                _state.update { it.copy(loading = true, selectedRepository = repository) }
                runCatching { api.repositoryTree(auth, repository) }
                    .onSuccess { files ->
                        _state.update { it.copy(files = files, loading = false) }
                        files.firstOrNull { it.path == note.path }?.let(::openFile)
                            ?: showMessage("That note may have moved.")
                    }
                    .onFailure { _state.update { state -> state.copy(loading = false, message = friendlyError(it)) } }
            }
        }
    }

    fun refresh() {
        val repo = _state.value.selectedRepository
        if (repo == null) {
            val auth = token ?: return
            viewModelScope.launch {
                _state.update { it.copy(refreshing = true) }
                runCatching { api.repositories(auth) }
                    .onSuccess { repos -> _state.update { it.copy(repositories = repos, refreshing = false) } }
                    .onFailure { _state.update { state -> state.copy(refreshing = false, message = friendlyError(it)) } }
            }
        } else selectRepository(repo)
    }

    fun back(): Boolean {
        val state = _state.value
        return when (state.screen) {
            AppScreen.READER -> {
                _state.update { it.copy(screen = AppScreen.EXPLORER, currentFile = null, noteContent = "") }
                true
            }
            AppScreen.EXPLORER -> if (state.currentFolder.isNotEmpty()) {
                openFolder(state.currentFolder.substringBeforeLast('/', ""))
                true
            } else {
                _state.update { it.copy(screen = AppScreen.REPOSITORIES, selectedRepository = null, files = emptyList()) }
                true
            }
            else -> false
        }
    }

    fun setRepositoryQuery(value: String) = _state.update { it.copy(repositoryQuery = value) }
    fun setFileQuery(value: String) = _state.update { it.copy(fileQuery = value) }
    fun setDarkTheme(value: Boolean) = _state.update { it.copy(darkTheme = value) }
    fun clearMessage() = _state.update { it.copy(message = null) }

    fun logout() {
        authJob?.cancel()
        token = null
        tokenStore.clear()
        _state.value = AppState(darkTheme = _state.value.darkTheme, recentNotes = recentStore.load())
    }

    private fun showMessage(message: String) = _state.update { it.copy(message = message) }

    private fun friendlyError(error: Throwable): String = when {
        error.message?.contains("401") == true -> "GitHub rejected those credentials. Check the token and try again."
        error.message?.contains("rate limit", ignoreCase = true) == true -> "GitHub’s request limit was reached. Try again shortly."
        else -> error.message?.takeIf(String::isNotBlank) ?: "Something went wrong. Check your connection and try again."
    }
}
