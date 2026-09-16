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
    // v0.55 live token rate: cumulative timed counters from tokscale. The
    // renderer derives a rate only from the *delta* between two snapshots;
    // `capabilities.throughput = false` (or absent fields) means "no data".
    @SerialName("capabilities") val capabilities: PeriodCapabilities? = null,
    @SerialName("timedTokens") val timedTokens: Double? = null,
    @SerialName("timedOutputTokens") val timedOutputTokens: Double? = null,
    @SerialName("timedDurationMs") val timedDurationMs: Double? = null,
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

/** v0.55: a period advertises which derived features its counters support. */
@Serializable
data class PeriodCapabilities(
    @SerialName("tokenComponents") val tokenComponents: Boolean? = null,
    @SerialName("throughput") val throughput: Boolean? = null
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
    // v0.57: "background-review" marks a non-interactive Codex review run.
    @SerialName("sessionKind") val sessionKind: String? = null,
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
    // Third-party adapter identity retained across Hub synchronization:
    // "newapi-account", "newapi-token", "sub2api", or "custom". Only present
    // on thirdparty rows (docs/API.md).
    @SerialName("adapterId") val adapterId: String? = null,
    @SerialName("actionRequired") val actionRequired: String? = null,
    @SerialName("sourceDeviceId") val sourceDeviceId: String? = null,
    @SerialName("stale") val stale: Boolean = false,
    @SerialName("status") val status: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
    @SerialName("windows") val windows: List<LimitWindow> = emptyList(),
    @SerialName("balanceUsd") val balanceUsd: Double? = null,
    @SerialName("balance") val balance: BalanceBlock? = null,
    @SerialName("usageSummary") val usageSummary: UsageSummary? = null
)

/** Normalized period-detail block some providers expose (docs/API.md). */
@Serializable
data class UsageSummary(
    @SerialName("period") val period: String? = null,
    @SerialName("requests") val requests: Long? = null,
    @SerialName("inputTokens") val inputTokens: Long? = null,
    @SerialName("outputTokens") val outputTokens: Long? = null,
    @SerialName("cacheReadTokens") val cacheReadTokens: Long? = null,
    @SerialName("cacheCreationTokens") val cacheCreationTokens: Long? = null,
    @SerialName("totalTokens") val totalTokens: Long? = null,
    @SerialName("standardCost") val standardCost: Double? = null,
    @SerialName("actualCost") val actualCost: Double? = null,
    @SerialName("averageDurationMs") val averageDurationMs: Double? = null
)

@Serializable
data class LimitWindow(
    @SerialName("kind") val kind: String? = null,
    @SerialName("source") val source: String? = null,
    @SerialName("metric") val metric: String? = null,
    @SerialName("currency") val currency: String? = null,
    // Backend metered-feature identity (Codex); separately metered buckets are
    // marked additional so compact readers can exclude them (docs/API.md).
    @SerialName("limitId") val limitId: String? = null,
    @SerialName("additional") val additional: Boolean = false,
    // Bounded display-only description (e.g. the Kimi-vs-Code composition of a
    // shared monthly membership meter).
    @SerialName("detail") val detail: String? = null,
    // Canonical display label for the window ("Session", "Weekly", "5-hour",
    // "Token Plan", etc.); takes precedence over the localized kind label.
    @SerialName("label") val label: String? = null,
    // Optional display-only reset description for windows that don't carry a
    // resetsAt timestamp (e.g. Zed's "Unlimited" edit-predictions window).
    @SerialName("resetDescription") val resetDescription: String? = null,
    // v0.56: types the resetsAt boundary — "reset" (the legacy presentation,
    // also used when omitted), "expiry" (quota pool that expires), or "mixed"
    // (simultaneous reset and expiry). Wording only; never scheduling.
    @SerialName("boundaryKind") val boundaryKind: String? = null,
    // The hub emits percentages as 0-100 numbers that may carry float noise
    // (e.g. 0.7999999999999972), so these must be Double, never Int.
    @SerialName("usedPercent") val usedPercent: Double? = null,
    @SerialName("remainingPercent") val remainingPercent: Double? = null,
    @SerialName("used") val used: Double? = null,
    @SerialName("limit") val limit: Double? = null,
    @SerialName("remaining") val remaining: Double? = null,
    @SerialName("resetsAt") val resetsAt: String? = null,
    @SerialName("showMeter") val showMeter: Boolean? = null,
    // MiMo: "expired" means the Token Plan lapsed, so there is no quota window
    // even when a prepaid balance is still available.
    @SerialName("planStatus") val planStatus: String? = null
)

@Serializable
data class BalanceBlock(
    @SerialName("amount") val amount: Double? = null,
    @SerialName("currency") val currency: String? = null,
    @SerialName("todaySpend") val todaySpend: Double? = null,
    @SerialName("monthSpend") val monthSpend: Double? = null,
    @SerialName("allTimeSpend") val allTimeSpend: Double? = null,
    // MiMo Token Plan usage, synthesized into a billing window by the desktop
    // renderer when the provider reports no window of its own.
    @SerialName("planUsed") val planUsed: Double? = null,
    @SerialName("planLimit") val planLimit: Double? = null,
    @SerialName("planPercent") val planPercent: Double? = null,
    @SerialName("planStatus") val planStatus: String? = null,
    @SerialName("giftBalance") val giftBalance: Double? = null,
    @SerialName("cashBalance") val cashBalance: Double? = null,
    // WorkBuddy reports `currency: "CREDITS"`; Claude's prepaid pool carries
    // an expiry on the block and per-grant `tranches` (public stats strip those).
    @SerialName("expiresAt") val expiresAt: String? = null,
    @SerialName("trackingSince") val trackingSince: String? = null,
    @SerialName("monthSinceTracking") val monthSinceTracking: Boolean? = null
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
