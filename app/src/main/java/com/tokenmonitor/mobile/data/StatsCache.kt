package com.tokenmonitor.mobile.data

import android.content.Context
import java.io.File

/**
 * On-disk cache of the last successful `/api/stats` response (raw JSON + save
 * time), so a later launch can show data immediately even when the hub is
 * unreachable. Written on successful fetches, read at startup.
 */
class StatsCache(context: Context) {

    private val dir = File(context.filesDir, "stats_cache")
    private val jsonFile = File(dir, "stats.json")
    private val metaFile = File(dir, "saved_at_ms")

    fun save(rawJson: String, savedAtMs: Long) {
        try {
            dir.mkdirs()
            jsonFile.writeText(rawJson)
            metaFile.writeText(savedAtMs.toString())
        } catch (_: Exception) {
            // Cache is best-effort; a failed write never breaks the UI.
        }
    }

    fun load(): Pair<String, Long>? = try {
        if (!jsonFile.exists()) return null
        val savedAt = metaFile.readText().toLongOrNull() ?: 0L
        val json = jsonFile.readText()
        if (json.isBlank()) null else json to savedAt
    } catch (_: Exception) {
        null
    }
}
