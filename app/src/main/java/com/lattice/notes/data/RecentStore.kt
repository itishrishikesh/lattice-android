package com.lattice.notes.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RecentStore(context: Context) {
    private val prefs = context.getSharedPreferences("lattice_preferences", Context.MODE_PRIVATE)

    fun add(repo: String, path: String) {
        val updated = listOf(RecentNote(repo, path, System.currentTimeMillis())) +
            load().filterNot { it.repoFullName == repo && it.path == path }
        val array = JSONArray()
        updated.take(20).forEach {
            array.put(JSONObject().put("repo", it.repoFullName).put("path", it.path).put("opened", it.openedAt))
        }
        prefs.edit().putString("recent", array.toString()).apply()
    }

    fun load(): List<RecentNote> = runCatching {
        val array = JSONArray(prefs.getString("recent", "[]"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(RecentNote(item.getString("repo"), item.getString("path"), item.getLong("opened")))
            }
        }
    }.getOrDefault(emptyList())
}
