package com.tokenmonitor.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.data.LimitWindow
import com.tokenmonitor.mobile.data.ProviderLimit
import com.tokenmonitor.mobile.data.StatsResponse
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.LimitBar
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.components.StatusDot
import com.tokenmonitor.mobile.ui.liquid.GlassCard
import com.tokenmonitor.mobile.ui.theme.CardBg
import com.tokenmonitor.mobile.ui.theme.Error
import com.tokenmonitor.mobile.ui.theme.StaleGrey
import com.tokenmonitor.mobile.ui.theme.Success
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.ui.theme.Warn
import com.tokenmonitor.mobile.util.localDateTime
import com.tokenmonitor.mobile.util.pctDisplay
import com.tokenmonitor.mobile.util.providerLabel
import com.tokenmonitor.mobile.util.windowKindLabel
import com.tokenmonitor.mobile.vm.UiState

/** AI Tool Limits: provider accounts with their quota windows, mirroring the Limits view. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LimitsScreen(state: UiState, onRefresh: () -> Unit) {
    val stats = state.stats
    if (stats == null) {
        EmptyState("暂无数据")
        return
    }
    // Providers without any quota window (e.g. notConfigured / unavailable) are
    // hidden unless the user opts in via Settings.
    val providers = stats.limits?.providers
        ?.filter { state.settings.showEmptyLimitProviders || it.windows.isNotEmpty() }
        ?: emptyList()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    PullToRefreshBox(
        isRefreshing = state.refreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 84.dp + navBottom)
        ) {
            item { SectionTitle("AI Tool Limits", "${providers.size} 个 provider") }
            if (providers.isEmpty()) {
                item { EmptyState("Hub 上暂无额度数据\n(桌面端需启用 AI Tool Limits 检测)") }
            }
            items(providers) { p -> ProviderCard(p) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ProviderCard(p: ProviderLimit) {
    val statusColor = when {
        p.stale -> StaleGrey
        p.status == "ok" -> Success
        p.status == "error" || p.status == "failed" -> Error
        else -> Warn
    }
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = com.kyant.shapes.RoundedRectangle(20f.dp),
        contentPadding = 12.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(statusColor)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    providerLabel(p.provider),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                val identity = listOfNotNull(
                    p.planLabel,
                    p.accountLabel,
                    p.accountName,
                    p.accountEmail
                ).joinToString(" · ")
                if (identity.isNotBlank()) {
                    Text(identity, fontSize = 11.sp, color = TextMuted)
                }
            }
            Text(
                when {
                    p.stale -> "stale"
                    p.status == "ok" -> "正常"
                    p.status.isNullOrBlank() -> "未知"
                    else -> p.status
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = statusColor
            )
        }
        if (p.windows.isEmpty()) {
            Text(
                "无额度窗口",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        p.windows.forEach { w -> WindowRow(p, w) }
    }
}

@Composable
private fun WindowRow(p: ProviderLimit, w: LimitWindow) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                windowKindLabel(w.kind),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (w.metric == "credits" || (w.usedPercent == null && w.remaining != null)) {
                // Balance-style quota: headline is money, not a percentage
                Text(
                    "剩余 ${w.currency?.let { "$it " } ?: ""}${formatAmount(w.remaining)}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            } else {
                Text(
                    pctDisplay(w.usedPercent),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = barColor(w.usedPercent ?: 0.0)
                )
            }
        }
        if (w.usedPercent != null) {
            LimitBar(w.usedPercent / 100.0, Modifier.padding(top = 4.dp))
        }
        val resets = w.resetsAt?.let { "重置 ${localDateTime(it)}" }
        val source = w.source?.let { if (it == "web") " · web" else "" } ?: ""
        // Burn-rate forecast: assume linear consumption since the window
        // started; estimated exhaustion time = remaining / rate.
        val forecast = exhaustionForecast(w)
        Text(
            listOfNotNull(resets, forecast).joinToString(" · ") + source,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Best-effort "estimated exhaustion" for a quota window, based on linear burn
 * since the window's assumed start (session ≈ 5h, weekly ≈ 7d, billing ≈ 30d).
 * Returns null when the window has no used percent, no reset time, or the burn
 * rate is not positive. It is a display-layer estimate, not a wire value.
 */
private fun exhaustionForecast(w: LimitWindow): String? {
    val used = w.usedPercent ?: return null
    if (used <= 0.0) return null
    val resetsAt = parseIsoTime(w.resetsAt) ?: return null
    val now = System.currentTimeMillis()
    val windowMs = when (w.kind) {
        "session" -> 5L * 60 * 60 * 1000
        "weekly" -> 7L * 24 * 60 * 60 * 1000
        else -> 30L * 24 * 60 * 60 * 1000
    }
    val windowStart = resetsAt - windowMs
    val elapsed = now - windowStart
    if (elapsed <= 0) return null
    val burnPerMs = used / elapsed
    val remaining = 100.0 - used
    if (burnPerMs <= 0) return null
    val hoursLeft = (remaining / burnPerMs) / (60 * 60 * 1000)
    if (hoursLeft <= 0 || hoursLeft > windowMs / (60 * 60 * 1000)) return null
    return "预计 ${formatHours(hoursLeft)} 后耗尽"
}

private fun formatHours(hours: Double): String = when {
    hours >= 48 -> "${String.format(java.util.Locale.US, "%.0f", hours / 24)} 天"
    hours >= 1 -> "${String.format(java.util.Locale.US, "%.0f", hours)} 小时"
    else -> "${String.format(java.util.Locale.US, "%.0f", hours * 60)} 分钟"
}

private fun parseIsoTime(iso: String?): Long? {
    if (iso.isNullOrBlank()) return null
    return try {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US)
        sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
        sdf.parse(iso)?.time
    } catch (e: Exception) {
        try {
            val sdf2 = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            sdf2.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf2.parse(iso)?.time
        } catch (e2: Exception) {
            null
        }
    }
}

@Composable
private fun barColor(pct: Double): Color = when {
    pct >= 85 -> Error
    pct >= 60 -> Warn
    else -> Success
}

private fun formatAmount(v: Double?): String {
    if (v == null) return "—"
    return if (v >= 10) String.format(java.util.Locale.US, "%.2f", v)
    else String.format(java.util.Locale.US, "%.4f", v)
}



