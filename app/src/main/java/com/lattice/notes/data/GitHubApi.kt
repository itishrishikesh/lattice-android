package com.lattice.notes.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class GitHubApi {
    suspend fun currentUser(token: String): GitHubUser = withContext(Dispatchers.IO) {
        val json = JSONObject(request("https://api.github.com/user", token))
        GitHubUser(
            login = json.getString("login"),
            name = json.optString("name").takeIf { it.isNotBlank() && it != "null" },
            avatarUrl = json.optString("avatar_url").takeIf(String::isNotBlank)
        )
    }

    suspend fun repositories(token: String): List<Repository> = withContext(Dispatchers.IO) {
        val body = request(
            "https://api.github.com/user/repos?per_page=100&sort=pushed&affiliation=owner,collaborator,organization_member",
            token
        )
        val array = JSONArray(body)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    Repository(
                        id = item.getLong("id"),
                        name = item.getString("name"),
                        fullName = item.getString("full_name"),
                        owner = item.getJSONObject("owner").getString("login"),
                        description = item.optString("description").takeIf { it.isNotBlank() && it != "null" },
                        defaultBranch = item.getString("default_branch"),
                        isPrivate = item.getBoolean("private"),
                        updatedAt = item.optString("updated_at"),
                        language = item.optString("language").takeIf { it.isNotBlank() && it != "null" },
                        stargazers = item.optInt("stargazers_count")
                    )
                )
            }
        }
    }

    suspend fun repositoryTree(token: String, repository: Repository): List<RepoFile> = withContext(Dispatchers.IO) {
        val branch = encodePath(repository.defaultBranch)
        val body = request(
            "https://api.github.com/repos/${repository.fullName}/git/trees/$branch?recursive=1",
            token
        )
        val root = JSONObject(body)
        val array = root.getJSONArray("tree")
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RepoFile(
                        path = item.getString("path"),
                        type = item.getString("type"),
                        size = item.optLong("size", 0L),
                        sha = item.getString("sha")
                    )
                )
            }
        }
    }

    suspend fun rawFile(token: String, repository: Repository, path: String): String = withContext(Dispatchers.IO) {
        request(
            "https://api.github.com/repos/${repository.fullName}/contents/${encodePath(path)}?ref=${encode(repository.defaultBranch)}",
            token,
            accept = "application/vnd.github.raw+json"
        )
    }

    suspend fun requestDeviceCode(clientId: String): DeviceCode = withContext(Dispatchers.IO) {
        val response = formRequest(
            "https://github.com/login/device/code",
            mapOf("client_id" to clientId, "scope" to "repo read:user")
        )
        val json = JSONObject(response)
        DeviceCode(
            deviceCode = json.getString("device_code"),
            userCode = json.getString("user_code"),
            verificationUri = json.getString("verification_uri"),
            expiresIn = json.getInt("expires_in"),
            interval = json.optInt("interval", 5)
        )
    }

    suspend fun pollDeviceToken(clientId: String, deviceCode: String): TokenPollResult = withContext(Dispatchers.IO) {
        val json = JSONObject(
            formRequest(
                "https://github.com/login/oauth/access_token",
                mapOf(
                    "client_id" to clientId,
                    "device_code" to deviceCode,
                    "grant_type" to "urn:ietf:params:oauth:grant-type:device_code"
                )
            )
        )
        when (val error = json.optString("error")) {
            "" -> TokenPollResult.Success(json.getString("access_token"))
            "authorization_pending" -> TokenPollResult.Pending
            "slow_down" -> TokenPollResult.SlowDown(json.optInt("interval", 5))
            else -> TokenPollResult.Error(json.optString("error_description", error))
        }
    }

    private fun request(url: String, token: String, accept: String = "application/vnd.github+json"): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return connection.use {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", accept)
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            setRequestProperty("User-Agent", "Lattice-Android")
            readResponse()
        }
    }

    private fun formRequest(url: String, fields: Map<String, String>): String {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return connection.use {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            setRequestProperty("User-Agent", "Lattice-Android")
            outputStream.bufferedWriter().use { writer ->
                writer.write(fields.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" })
            }
            readResponse()
        }
    }

    private fun HttpURLConnection.readResponse(): String {
        val success = responseCode in 200..299
        val body = (if (success) inputStream else errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
        if (!success) {
            val message = runCatching { JSONObject(body).optString("message") }.getOrNull()
            throw GitHubException(responseCode, message?.takeIf(String::isNotBlank) ?: "GitHub request failed")
        }
        return body
    }

    private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun encodePath(path: String): String = path.split('/').joinToString("/") { encode(it).replace("+", "%20") }

    private inline fun <T : HttpURLConnection, R> T.use(block: T.() -> R): R = try {
        block()
    } finally {
        disconnect()
    }
}

class GitHubException(val statusCode: Int, override val message: String) : IOException(message)
