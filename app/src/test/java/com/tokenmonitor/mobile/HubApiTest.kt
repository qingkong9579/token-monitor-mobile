package com.tokenmonitor.mobile

import com.tokenmonitor.mobile.data.HubApi
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class HubApiTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun api(): HubApi = HubApi(OkHttpClient.Builder().build())

    private fun enqueueStats() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("content-type", "application/json")
                .setBody("""{"periods":{"today":{"totalTokens":1}}}""")
        )
    }

    @Test
    fun `secret with trailing newline still authenticates`() = runBlocking {
        enqueueStats()
        val url = server.url("/").toString().trimEnd('/')
        val stats = api().fetchStats(url, "abc123\n").stats
        assertEquals(1L, stats.periods!!.today!!.totalTokens)
        val recorded = server.takeRequest()
        assertEquals("Bearer abc123", recorded.getHeader("Authorization"))
    }

    @Test
    fun `secret with internal spaces and tabs is cleaned`() = runBlocking {
        enqueueStats()
        val url = server.url("/").toString().trimEnd('/')
        api().fetchStats(url, "ab c\t123\n")
        val recorded = server.takeRequest()
        assertEquals("Bearer abc123", recorded.getHeader("Authorization"))
    }

    @Test
    fun `blank secret hits public endpoint without auth header`() = runBlocking {
        enqueueStats()
        val url = server.url("/").toString().trimEnd('/')
        api().fetchStats(url, "  \n ")
        val recorded = server.takeRequest()
        assertEquals("/api/public/stats", recorded.path)
        assertEquals(null, recorded.getHeader("Authorization"))
    }

    @Test
    fun `secret hits authenticated stats endpoint`() = runBlocking {
        enqueueStats()
        val url = server.url("/").toString().trimEnd('/')
        api().fetchStats(url, "secret123")
        val recorded = server.takeRequest()
        assertEquals("/api/stats", recorded.path)
        assertEquals("Bearer secret123", recorded.getHeader("Authorization"))
    }

    @Test
    fun `401 surfaces auth error message`() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(401)
                .setBody("""{"error":"unauthorized"}""")
        )
        val url = server.url("/").toString().trimEnd('/')
        try {
            api().fetchStats(url, "wrong")
            throw AssertionError("expected ApiException")
        } catch (e: com.tokenmonitor.mobile.data.ApiException) {
            assertEquals(401, e.statusCode)
            assertEquals(true, e.message!!.contains("Secret"))
        }
    }
}
