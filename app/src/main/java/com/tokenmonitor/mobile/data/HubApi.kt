package com.tokenmonitor.mobile.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String, val statusCode: Int? = null) : IOException(message)

/** Shared lenient JSON config: unknown keys tolerated, nulls coerced. */
val ApiJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
}

/** A parsed stats payload together with its raw JSON (for offline caching). */
data class FetchedStats(val stats: StatsResponse, val rawJson: String)

/**
 * Thin HTTP client for the Token Monitor hub API.
 * Authenticated mode uses `Authorization: Bearer <secret>`; when the secret is
 * blank the app falls back to the unauthenticated `/api/public/stats` endpoint
 * (requires PUBLIC_STATS_ENABLED=1 on the hub).
 */
class HubApi(private val client: OkHttpClient) {

    suspend fun fetchStats(baseUrl: String, secret: String): FetchedStats {
        val root = baseUrl.trim().trimEnd('/')
        // Paste safety: strip ALL whitespace from the secret (a trailing newline
        // copied from a chat/code block would otherwise break the Authorization
        // header value — OkHttp rejects control chars in header values).
        val key = secret.filterNot { it.isWhitespace() }
        val url = if (key.isBlank()) "$root/api/public/stats" else "$root/api/stats"
        val builder = Request.Builder().url(url)
        if (key.isNotBlank()) builder.header("Authorization", "Bearer $key")
        val response = executeRaw(builder.build())
        val body = response.body?.string().orEmpty()
        if (!response.isSuccessful) {
            throw ApiException(
                when (response.code) {
                    401 -> "认证失败:Secret 不正确"
                    503 -> "Hub 未配置密钥或公开端点未开启"
                    404 -> "Hub 端点不存在,请检查 Hub URL"
                    else -> "HTTP ${response.code}: ${body.take(200)}"
                },
                response.code
            )
        }
        return try {
            FetchedStats(ApiJson.decodeFromString(StatsResponse.serializer(), body), body)
        } catch (e: Exception) {
            throw ApiException("响应解析失败: ${e.message}", response.code)
        }
    }

    /** Lightweight reachability probe used by the Settings screen. */
    suspend fun checkHealth(baseUrl: String): Boolean {
        val root = baseUrl.trim().trimEnd('/')
        val request = Request.Builder().url("$root/api/health").build()
        return try {
            executeRaw(request).use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun executeRaw(request: Request): Response =
        withContext(Dispatchers.IO) { client.newCall(request).execute() }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
