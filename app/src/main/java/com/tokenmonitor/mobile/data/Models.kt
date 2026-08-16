package com.tokenmonitor.mobile.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire shapes mirroring the Token Monitor hub HTTP API (docs/API.md).
 * All fields are optional-with-defaults so the app tolerates older hubs
 * and partial payloads.
 */

@Serializable
data class StatsResponse(
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("staleAfterMs") val staleAfterMs: Long? = null,
    @SerialName("periods") val periods: Periods? = null,
    @SerialName("limits") val limits: LimitsAggregate? = null,
    @SerialName("devices") val devices: List<DeviceRecord>? = null,
    @SerialName("subscriptionsUpdatedAt") val subscriptionsUpdatedAt: String? = null,
    @SerialName("historyPreview") val historyPreview: HistoryPreview? = null
)

/** Retained history preview — the daily/monthly rows backing the trends view. */
@Serializable
data class HistoryPreview(
    @SerialName("daily") val daily: List<HistoryDay> = emptyList(),
    @SerialName("monthly") val monthly: List<HistoryMonth> = emptyList(),
    @SerialName("summary") val summary: HistorySummary = HistorySummary()
)

@Serializable
data class HistoryDay(
    @SerialName("date") val date: String = "",
    @SerialName("tokens") val tokens: Long = 0,
    @SerialName("cost") val cost: Double = 0.0,
    @SerialName("activeTimeMs") val activeTimeMs: Long? = null
)

@Serializable
data class HistoryMonth(
    @SerialName("key") val key: String = "",
    @SerialName("tokens") val tokens: Long = 0,
    @SerialName("cost") val cost: Double = 0.0
)

@Serializable
data class HistorySummary(
    @SerialName("totalTokens") val totalTokens: Long = 0,
    @SerialName("totalCost") val totalCost: Double = 0.0,
    @SerialName("activeDays") val activeDays: Long? = null,
    @SerialName("currentStreak") val currentStreak: Long? = null,
    @SerialName("longestStreak") val longestStreak: Long? = null,
    @SerialName("peakDayTokens") val peakDayTokens: Long? = null,
    @SerialName("favoriteModel") val favoriteModel: String? = null,
    @SerialName("messages") val messages: Long? = null,
    @SerialName("activeTimeMs") val activeTimeMs: Long? = null
)

@Serializable
data class Periods(
    @SerialName("today") val today: PeriodSummary? = null,
    @SerialName("month") val month: PeriodSummary? = null,
    @SerialName("allTime") val allTime: PeriodSummary? = null
)

@Serializable
data class PeriodSummary(
    @SerialName("totalTokens") val totalTokens: Long = 0,
    @SerialName("costUsd") val costUsd: Double = 0.0,
    @SerialName("cacheReadTokens") val cacheReadTokens: Long = 0,
    @SerialName("cacheWriteTokens") val cacheWriteTokens: Long = 0,
    @SerialName("outputTokens") val outputTokens: Long = 0,
    @SerialName("unclassifiedTokens") val unclassifiedTokens: Long = 0,
    @SerialName("clients") val clients: Map<String, Long> = emptyMap(),
    @SerialName("clientCosts") val clientCosts: Map<String, Double> = emptyMap(),
    @SerialName("clientCacheReads") val clientCacheReads: Map<String, Long> = emptyMap(),
    @SerialName("clientCacheWrites") val clientCacheWrites: Map<String, Long> = emptyMap(),
    @SerialName("clientOutputs") val clientOutputs: Map<String, Long> = emptyMap(),
    @SerialName("clientUnclassifiedTokens") val clientUnclassifiedTokens: Map<String, Long> = emptyMap(),
    @SerialName("models") val models: Map<String, Long> = emptyMap(),
    @SerialName("modelCosts") val modelCosts: Map<String, Double> = emptyMap(),
    @SerialName("modelCacheReads") val modelCacheReads: Map<String, Long> = emptyMap(),
    @SerialName("modelCacheWrites") val modelCacheWrites: Map<String, Long> = emptyMap(),
    @SerialName("modelOutputs") val modelOutputs: Map<String, Long> = emptyMap(),
    @SerialName("modelUnclassifiedTokens") val modelUnclassifiedTokens: Map<String, Long> = emptyMap(),
    @SerialName("clientModels") val clientModels: Map<String, Map<String, Long>> = emptyMap(),
    @SerialName("clientModelCosts") val clientModelCosts: Map<String, Map<String, Double>> = emptyMap(),
    @SerialName("projects") val projects: Map<String, ProjectEntry> = emptyMap(),
    @SerialName("sessions") val sessions: Map<String, SessionEntry> = emptyMap()
)

@Serializable
data class ProjectEntry(
    @SerialName("label") val label: String? = null,
    @SerialName("tokens") val tokens: Long = 0,
    @SerialName("costUsd") val costUsd: Double = 0.0,
    @SerialName("clients") val clients: Map<String, Long> = emptyMap()
)

@Serializable
data class SessionEntry(
    @SerialName("client") val client: String? = null,
    @SerialName("sessionId") val sessionId: String? = null,
    @SerialName("totalTokens") val totalTokens: Long = 0,
    @SerialName("costUsd") val costUsd: Double = 0.0,
    @SerialName("messageCount") val messageCount: Long? = null,
    @SerialName("inputTokens") val inputTokens: Long? = null,
    @SerialName("outputTokens") val outputTokens: Long? = null,
    @SerialName("cacheReadTokens") val cacheReadTokens: Long? = null,
    @SerialName("cacheWriteTokens") val cacheWriteTokens: Long? = null,
    @SerialName("reasoningTokens") val reasoningTokens: Long? = null,
    @SerialName("startedAt") val startedAt: String? = null,
    @SerialName("lastUsedAt") val lastUsedAt: String? = null,
    @SerialName("projectId") val projectId: String? = null,
    @SerialName("projectLabel") val projectLabel: String? = null,
    @SerialName("models") val models: Map<String, Long> = emptyMap(),
    @SerialName("providers") val providers: Map<String, Long> = emptyMap()
)

@Serializable
data class LimitsAggregate(
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("refreshMs") val refreshMs: Long? = null,
    @SerialName("providers") val providers: List<ProviderLimit> = emptyList()
)

@Serializable
data class ProviderLimit(
    @SerialName("provider") val provider: String = "",
    @SerialName("accountKey") val accountKey: String? = null,
    @SerialName("accountLabel") val accountLabel: String? = null,
    @SerialName("accountName") val accountName: String? = null,
    @SerialName("accountEmail") val accountEmail: String? = null,
    @SerialName("planLabel") val planLabel: String? = null,
    @SerialName("sourceDeviceId") val sourceDeviceId: String? = null,
    @SerialName("stale") val stale: Boolean = false,
    @SerialName("status") val status: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("windows") val windows: List<LimitWindow> = emptyList(),
    @SerialName("balanceUsd") val balanceUsd: Double? = null,
    @SerialName("balance") val balance: BalanceBlock? = null
)

@Serializable
data class LimitWindow(
    @SerialName("kind") val kind: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("metric") val metric: String? = null,
    @SerialName("currency") val currency: String? = null,
    // The hub emits percentages as 0-100 numbers that may carry float noise
    // (e.g. 0.7999999999999972), so these must be Double, never Int.
    @SerialName("usedPercent") val usedPercent: Double? = null,
    @SerialName("remainingPercent") val remainingPercent: Double? = null,
    @SerialName("used") val used: Double? = null,
    @SerialName("limit") val limit: Double? = null,
    @SerialName("remaining") val remaining: Double? = null,
    @SerialName("resetsAt") val resetsAt: String? = null,
    @SerialName("showMeter") val showMeter: Boolean? = null
)

@Serializable
data class BalanceBlock(
    @SerialName("amount") val amount: Double? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("todaySpend") val todaySpend: Double? = null,
    @SerialName("monthSpend") val monthSpend: Double? = null,
    @SerialName("allTimeSpend") val allTimeSpend: Double? = null
)

@Serializable
data class DeviceRecord(
    @SerialName("deviceId") val deviceId: String = "",
    @SerialName("hostname") val hostname: String? = null,
    @SerialName("platform") val platform: String? = null,
    @SerialName("osName") val osName: String? = null,
    @SerialName("osVersion") val osVersion: String? = null,
    @SerialName("agentVersion") val agentVersion: String? = null,
    @SerialName("agentRuntime") val agentRuntime: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("receivedAt") val receivedAt: String? = null,
    @SerialName("ageMs") val ageMs: Long? = null,
    @SerialName("stale") val stale: Boolean = false,
    @SerialName("trackedClients") val trackedClients: List<String>? = null,
    @SerialName("periods") val periods: Periods? = null,
    @SerialName("limits") val limits: LimitsAggregate? = null
)
