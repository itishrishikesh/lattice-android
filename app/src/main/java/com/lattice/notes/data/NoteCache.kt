package com.lattice.notes.data

import android.content.Context
import java.security.MessageDigest

class NoteCache(context: Context) {
    private val directory = context.cacheDir.resolve("notes").apply { mkdirs() }

    fun read(repo: String, path: String): String? = runCatching {
        fileFor(repo, path).takeIf { it.exists() }?.readText()
    }.getOrNull()

    fun write(repo: String, path: String, content: String) {
        runCatching { fileFor(repo, path).writeText(content) }
    }

    private fun fileFor(repo: String, path: String) = directory.resolve(
        MessageDigest.getInstance("SHA-256")
            .digest("$repo:$path".toByteArray())
            .joinToString("") { "%02x".format(it) }
    )
}
