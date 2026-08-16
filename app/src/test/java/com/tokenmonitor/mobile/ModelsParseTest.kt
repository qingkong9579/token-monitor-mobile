package com.tokenmonitor.mobile

import com.tokenmonitor.mobile.data.StatsResponse
import com.tokenmonitor.mobile.util.clientLabel
import com.tokenmonitor.mobile.util.compactTokens
import com.tokenmonitor.mobile.util.formatMoney
import com.tokenmonitor.mobile.util.modelColor
import com.tokenmonitor.mobile.util.vendorForModel
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsParseTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val payload = """
    {
      "updatedAt": "2026-08-14T13:11:49.819Z",
      "staleAfterMs": 600000,
      "periods": {
        "today": {
          "totalTokens": 0,
          "costUsd": 0,
          "clients": {},
          "models": {}
        },
        "month": {
          "totalTokens": 16383370,
          "costUsd": 0.447515,
          "clients": { "claude": 16383370 },
          "clientCosts": { "claude": 0.447515 },
          "models": { "claude-opus-4-5": 16383370 },
          "modelCosts": { "claude-opus-4-5": 0.447515 },
          "clientCacheReads": { "claude": 15200000 },
          "clientCacheWrites": { "claude": 100000 },
          "clientOutputs": { "claude": 400000 }
        },
        "allTime": {
          "totalTokens": 994771272,
          "costUsd": 121.93495,
          "clients": {
            "antigravity": 207030675,
            "codex": 50187768,
            "micode": 85784955,
            "pi": 319742528,
            "copilot": 682849,
            "claude": 331342497
          },
          "clientCosts": {
            "antigravity": 22.1,
            "codex": 6.2,
            "micode": 9.1,
            "pi": 40.0,
            "copilot": 0.05,
            "claude": 44.4
          },
          "models": { "gpt-5": 50187768, "claude-opus-4-5": 331342497, "pi-1.5": 319742528 }
        }
      },
      "limits": {
        "updatedAt": "2026-08-14T13:10:00.812Z",
        "providers": [
          {
            "provider": "deepseek",
            "accountKey": "sha256:abc",
            "status": "ok",
            "updatedAt": "2026-08-14T13:10:00.000Z",
            "windows": [
              { "kind": "credits", "metric": "credits", "currency": "CNY", "remaining": 12.34, "showMeter": false }
            ],
            "balance": { "amount": 12.34, "currency": "CNY", "monthSpend": 5.5 }
          },
          {
            "provider": "claude",
            "accountKey": "sha256:def",
            "planLabel": "Pro",
            "accountEmail": "user@example.com",
            "status": "ok",
            "windows": [
              { "kind": "session", "usedPercent": 42, "remainingPercent": 58, "resetsAt": "2026-08-14T21:00:00.000Z" },
              { "kind": "weekly", "usedPercent": 20, "remainingPercent": 80, "resetsAt": "2026-08-20T00:00:00.000Z" }
            ]
          }
        ]
      },
      "devices": [
        {
          "deviceId": "sunshine",
          "hostname": "Sunshine",
          "platform": "win32-x64",
          "osName": "Windows 11",
          "osVersion": "25H2",
          "agentVersion": "0.44.0",
          "agentRuntime": "electron-widget",
          "updatedAt": "2026-08-14T13:11:47.889Z",
          "receivedAt": "2026-08-14T13:11:48.235Z",
          "ageMs": 1584,
          "stale": false,
          "trackedClients": ["claude", "codex"],
          "periods": {
            "today": { "totalTokens": 0, "costUsd": 0, "clients": {}, "models": {} },
            "month": { "totalTokens": 16383370, "costUsd": 0.447515, "clients": { "claude": 16383370 }, "models": {} },
            "allTime": { "totalTokens": 994771272, "costUsd": 121.93495, "clients": { "claude": 331342497 }, "models": {} }
          }
        }
      ]
    }
    """.trimIndent()

    @Test
    fun `parses real hub stats payload`() {
        val stats = json.decodeFromString(StatsResponse.serializer(), payload)
        assertNotNull(stats.periods)
        assertEquals(0L, stats.periods!!.today!!.totalTokens)
        assertEquals(16_383_370L, stats.periods!!.month!!.totalTokens)
        assertEquals(994_771_272L, stats.periods!!.allTime!!.totalTokens)
        assertEquals(1, stats.devices!!.size)
        assertEquals("sunshine", stats.devices!![0].deviceId)
        assertFalse(stats.devices!![0].stale)
        assertEquals(2, stats.limits!!.providers.size)
    }

    @Test
    fun `credits window exposes remaining money`() {
        val stats = json.decodeFromString(StatsResponse.serializer(), payload)
        val deepseek = stats.limits!!.providers.first { it.provider == "deepseek" }
        val window = deepseek.windows.first()
        assertEquals("credits", window.metric)
        assertEquals(12.34, window.remaining!!, 0.001)
        assertTrue(window.usedPercent == null)
    }

    @Test
    fun `client labels and colors resolve`() {
        assertEquals("Claude Code", clientLabel("claude"))
        assertEquals("MiMo Code", clientLabel("micode"))
        assertEquals("GitHub Copilot", clientLabel("copilot"))
        assertEquals("unknown-tool", clientLabel("unknown-tool"))
        assertNotNull(modelColor("gpt-5"))
    }

    @Test
    fun `model vendor detection matches desktop rules`() {
        assertEquals("claude", vendorForModel("claude-opus-4-5"))
        assertEquals("codex", vendorForModel("gpt-5-thinking-medium"))
        assertEquals("codex", vendorForModel("o4-mini"))
        assertEquals("deepseek", vendorForModel("deepseek-chat"))
        assertEquals("qwen", vendorForModel("qwen3-coder"))
        assertEquals("gemini", vendorForModel("gemini-2.5-pro"))
        assertEquals(null, vendorForModel("pi-1.5"))
    }

    @Test
    fun `compact token formatting matches desktop`() {
        assertEquals("994.77M", compactTokens(994_771_272))
        assertEquals("1.00B", compactTokens(1_000_000_000))
        assertEquals("16.38M", compactTokens(16_383_370))
        assertEquals("682.8K", compactTokens(682_849))
        assertEquals("123", compactTokens(123))
    }

    @Test
    fun `money formatting`() {
        assertEquals("\$121.93", formatMoney(121.93495, "USD", null))
        assertEquals("\$0.4475", formatMoney(0.447515, "USD", null))
        assertEquals("¥877.93", formatMoney(121.93495, "CNY", null))
        assertEquals("¥1000.00", formatMoney(100.0, "CNY", 10.0))
    }

    @Test
    fun `decimal usedPercent from hub parses as Double`() {
        // Regression: the hub emits 0-100 percentages with float noise, e.g.
        // copilot's 0.7999999999999972 — previously declared Int, which crashed
        // the parser with "Unexpected symbol '.' in numeric literal".
        val payload = """
        {
          "limits": {
            "providers": [
              {
                "provider": "copilot",
                "status": "ok",
                "windows": [
                  { "kind": "billing", "usedPercent": 0.7999999999999972, "remainingPercent": 99.2, "showMeter": true }
                ]
              }
            ]
          }
        }
        """.trimIndent()
        val stats = json.decodeFromString(StatsResponse.serializer(), payload)
        val window = stats.limits!!.providers[0].windows[0]
        assertEquals(0.7999999999999972, window.usedPercent!!, 0.0)
        assertEquals(99.2, window.remainingPercent!!, 0.0)
        assertEquals("0.8%", com.tokenmonitor.mobile.util.pctDisplay(window.usedPercent))
        assertEquals("99.2%", com.tokenmonitor.mobile.util.pctDisplay(window.remainingPercent))
        assertEquals("42%", com.tokenmonitor.mobile.util.pctDisplay(42.0))
    }

    @Test
    fun `live hub response parses end-to-end`() {
        // The actual production response captured from
        // a live hub's /api/stats — must parse with the app's
        // exact Json config (ignoreUnknownKeys + coerceInputValues).
        val live = javaClass.classLoader!!.getResourceAsStream("live-stats.json")!!
            .bufferedReader().readText()
        val stats = json.decodeFromString(StatsResponse.serializer(), live)
        assertNotNull(stats.periods)
        assertTrue(stats.devices!!.isNotEmpty())
        assertTrue(stats.limits != null)
        assertTrue(stats.periods!!.allTime!!.totalTokens > 0)
    }

    @Test
    fun `missing and unknown fields are tolerated`() {
        val minimal = """{"periods":{"today":{}}}"""
        val stats = json.decodeFromString(StatsResponse.serializer(), minimal)
        assertEquals(0L, stats.periods!!.today!!.totalTokens)
        assertTrue(stats.devices.isNullOrEmpty())
        assertTrue(stats.periods!!.month == null)
    }
}
