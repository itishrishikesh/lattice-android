package com.lattice.notes.data

data class GitHubUser(
    val login: String,
    val name: String?,
    val avatarUrl: String?
)

data class Repository(
    val id: Long,
    val name: String,
    val fullName: String,
    val owner: String,
    val description: String?,
    val defaultBranch: String,
    val isPrivate: Boolean,
    val updatedAt: String,
    val language: String?,
    val stargazers: Int
)

data class RepoFile(
    val path: String,
    val type: String,
    val size: Long,
    val sha: String
) {
    val name: String get() = path.substringAfterLast('/')
    val parent: String get() = path.substringBeforeLast('/', "")
    val isMarkdown: Boolean get() = type == "blob" && name.substringAfterLast('.', "").lowercase() in setOf("md", "markdown")
}

data class DeviceCode(
    val deviceCode: String,
    val userCode: String,
    val verificationUri: String,
    val expiresIn: Int,
    val interval: Int
)

sealed interface TokenPollResult {
    data class Success(val token: String) : TokenPollResult
    data object Pending : TokenPollResult
    data class SlowDown(val seconds: Int) : TokenPollResult
    data class Error(val message: String) : TokenPollResult
}

data class RecentNote(
    val repoFullName: String,
    val path: String,
    val openedAt: Long
)
