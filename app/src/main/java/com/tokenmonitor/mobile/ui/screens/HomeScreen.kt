package com.tokenmonitor.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.data.LimitWindow
import com.tokenmonitor.mobile.data.ProviderLimit
import com.tokenmonitor.mobile.data.StatsResponse
import com.tokenmonitor.mobile.ui.components.ActivityHeatmap
import com.tokenmonitor.mobile.ui.components.ActivityHeatmapLegend
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.ErrorCard
import com.tokenmonitor.mobile.ui.components.GlassButton
import com.tokenmonitor.mobile.ui.components.PeriodSelector
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.components.StatCard
import com.tokenmonitor.mobile.ui.components.StatusDot
import com.tokenmonitor.mobile.ui.components.ToolIcon
import com.tokenmonitor.mobile.ui.components.TrendSparkline
import com.tokenmonitor.mobile.ui.liquid.GlassCard
import com.tokenmonitor.mobile.ui.theme.Accent
import com.tokenmonitor.mobile.ui.theme.AccentOn
import com.tokenmonitor.mobile.ui.theme.Success
import com.tokenmonitor.mobile.ui.theme.TabularFigures
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.util.clientLabel
import com.tokenmonitor.mobile.util.compactTokens
import com.tokenmonitor.mobile.util.creditsMeterPercent
import com.tokenmonitor.mobile.util.formatLimitMoney
import com.tokenmonitor.mobile.util.formatMoney
import com.tokenmonitor.mobile.util.isCreditsWindow
import com.tokenmonitor.mobile.util.localDateTime
import com.tokenmonitor.mobile.util.providerLabel
import com.tokenmonitor.mobile.util.windowLabel
import com.tokenmonitor.mobile.vm.Period
import com.tokenmonitor.mobile.vm.UiState
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: UiState,
    onPeriod: (Period) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val stats = state.stats
    val currency = state.settings.currency
    val rate = state.settings.currencyRate
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.error != null && stats == null) {
            ErrorCard(state.error, onRetry = onRefresh)
            if (state.settings.hubUrl.isBlank()) {
                GlassButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    tint = Accent.copy(0.5f)
                ) {
                    Text("去设置填写 Hub URL 和密钥", color = AccentOn, fontWeight = FontWeight.Medium)
                }
            }
        } else if (state.error != null && stats != null) {
            // Graceful degradation: keep showing last-known data with a banner.
            // The raw error is appended so hub/parse failures are diagnosable
            // instead of collapsing into one opaque line.
            ErrorCard(
                "网络异常,显示上次同步数据 · ${clockTime(state.lastUpdated)}\n${state.error}",
                onRetry = onRefresh
            )
        }
        if (stats == null && state.error == null && state.loading) {
            EmptyState("正在连接 hub…")
        }
        if (stats != null) {
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 84.dp + navBottom)
                ) {
                    item {
                        PeriodSelector(state.period, onPeriod) {
                            Text("更新于 ${clockTime(state.lastUpdated)}", fontSize = 11.sp, color = TextMuted)
                        }
                    }
                    item { StatCards(stats, currency, rate) }
                    item { ActivityModule(stats) }
                    item { TrendsModule(stats) }
                    item { LimitsPreview(stats, currency, rate, state.settings.showEmptyLimitProviders) }
                    item { ToolsPreview(stats, state.period, currency, rate) }
                    item { DevicesPreview(stats) }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

private fun clockTime(epochMillis: Long?): String {
    if (epochMillis == null) return "—"
    return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        .format(java.util.Date(epochMillis))
}

@Composable
private fun StatCards(stats: StatsResponse, currency: String, rate: Double?) {
    val p = stats.periods ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(
            "今日",
            compactTokens(p.today?.totalTokens ?: 0),
            formatMoney(p.today?.costUsd ?: 0.0, currency, rate),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            "本月",
            compactTokens(p.month?.totalTokens ?: 0),
            formatMoney(p.month?.costUsd ?: 0.0, currency, rate),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            "累计",
            compactTokens(p.allTime?.totalTokens ?: 0),
            formatMoney(p.allTime?.costUsd ?: 0.0, currency, rate),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun LimitsPreview(
    stats: StatsResponse,
    currency: String,
    rate: Double?,
    showEmptyProviders: Boolean
) {
    // Balance-only providers (MiMo/DeepSeek) render synthesized windows on the
    // desktop home too, so filter on the *display* windows, not raw wire ones.
    val providers = stats.limits?.providers
        ?.filter { showEmptyProviders || compactLimitWindows(it).isNotEmpty() }
        ?: emptyList()
    if (providers.isEmpty()) return
    SectionTitle("额度", "${providers.size} 个 provider")
    // The desktop home module is a TEXT list: up to 3 accounts sorted by
    // remaining, each row = mark + name + per-window label/value lines with a
    // small reset line. No meters — only the Limits view draws bars — and no
    // stale badges (a stale row simply shows its last-known numbers).
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = com.kyant.shapes.RoundedRectangle(18f.dp),
        contentPadding = 12.dp
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            providers
                .sortedBy { p -> tightestRemaining(p) ?: Double.MAX_VALUE }
                .take(3)
                .forEach { p -> HomeLimitAccount(p) }
        }
    }
}

/** One account block of the home limits module (`.home-limit-account`). */
@Composable
private fun HomeLimitAccount(p: ProviderLimit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToolIcon(client = p.provider, size = 16.dp)
            Text(
                providerLabel(p.provider),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                modifier = Modifier.weight(1f, fill = false),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val windows = compactLimitWindows(p)
        if (windows.isEmpty()) {
            Text("无额度窗口", fontSize = 11.sp, color = TextMuted)
        } else {
            windows.forEach { w -> HomeLimitWindowLine(w, p) }
        }
    }
}

/** `.home-limit-window`: the label left, the value right, reset underneath. */
@Composable
private fun HomeLimitWindowLine(w: LimitWindow, p: ProviderLimit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                windowLabel(w),
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(6.dp))
            Text(
                homeLimitValue(w, p),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        val reset = when {
            !w.resetsAt.isNullOrBlank() -> "重置 ${localDateTime(w.resetsAt)}"
            !w.resetDescription.isNullOrBlank() -> w.resetDescription!!
            else -> ""
        }
        if (reset.isNotEmpty()) {
            Text(
                reset,
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 1.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** formatHomeLimitWindowValue: percent by default, money for credit balances. */
private fun homeLimitValue(w: LimitWindow, p: ProviderLimit): String {
    val currency = w.currency ?: p.balance?.currency
    return when {
        w.planStatus == "expired" -> "套餐已过期"
        w.detail.equals("unlimited", ignoreCase = true) -> "无限"
        isCreditsWindow(w) -> formatLimitMoney(w.remaining, currency)
        w.remainingPercent != null -> "剩余 ${w.remainingPercent!!.roundToInt()}%"
        w.usedPercent != null -> "剩余 ${(100.0 - w.usedPercent!!).roundToInt().coerceAtLeast(0)}%"
        w.remaining != null -> "${formatLimitMoney(w.remaining, currency)} 剩余"
        w.limit != null -> "${formatLimitMoney(w.limit, currency)} 上限"
        else -> w.detail ?: "—"
    }
}

/**
 * The tightest metered window of a provider, as (window, remaining percent).
 * Every metered window normalizes to a *remaining* percentage — money meters
 * are derived by the renderer and deliberately never carried on the wire.
 */
private fun meteredWindows(p: ProviderLimit): List<Pair<LimitWindow, Double>> =
    p.windows.filter { it.showMeter != false }.mapNotNull { w ->
        val remaining = when {
            isCreditsWindow(w) -> creditsMeterPercent(p, w)
            w.remainingPercent != null -> w.remainingPercent.coerceIn(0.0, 100.0)
            w.usedPercent != null -> (100.0 - w.usedPercent).coerceIn(0.0, 100.0)
            else -> null
        }
        if (remaining == null) null else w to remaining
    }

private fun tightestRemaining(p: ProviderLimit): Double? =
    meteredWindows(p).minByOrNull { it.second }?.second

@Composable
private fun ToolsPreview(stats: StatsResponse, period: Period, currency: String, rate: Double?) {
    val summary = when (period) {
        Period.TODAY -> stats.periods?.today
        Period.MONTH -> stats.periods?.month
        Period.ALL_TIME -> stats.periods?.allTime
    } ?: return
    val total = summary.totalTokens
    if (total == 0L) return
    val sorted = summary.clients.entries.sortedByDescending { it.value }.take(5)

    SectionTitle("工具排行", "TOP 5")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        sorted.forEach { (id, tokens) ->
            ToolRankRow(id, tokens, total)
        }
    }
}

/**
 * Token-monitor style tool row: logo + name on the left, token count on the
 * right, then a gap before the share percentage.
 */
@Composable
private fun ToolRankRow(id: String, tokens: Long, total: Long) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolIcon(client = id, size = 16.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            clientLabel(id),
            style = MaterialTheme.typography.bodySmall,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            compactTokens(tokens),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFeatureSettings = TabularFigures
            )
        )
        Spacer(Modifier.width(14.dp))
        Text(
            String.format(Locale.US, "%.1f%%", tokens * 100.0 / total),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = TextMuted,
                fontFeatureSettings = TabularFigures
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.width(46.dp)
        )
    }
}

/** GitHub-style activity heatmap over the history daily rows. */
@Composable
private fun ActivityModule(stats: StatsResponse?) {
    val daily = stats?.historyPreview?.daily ?: emptyList()
    SectionTitle("活动", if (daily.isNotEmpty()) "近一年" else null)
    if (daily.isEmpty()) {
        Text(
            "Hub 未提供历史数据\n(桌面端需开启 History)",
            fontSize = 11.sp,
            color = TextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )
        return
    }
    ActivityHeatmap(daily, Modifier.padding(horizontal = 16.dp))
    ActivityHeatmapLegend(Modifier.padding(start = 16.dp, top = 2.dp))
}

/** Token trend sparkline over the last 45 history days. */
@Composable
private fun TrendsModule(stats: StatsResponse?) {
    val daily = stats?.historyPreview?.daily ?: emptyList()
    if (daily.isEmpty()) return
    val peak = daily.maxOfOrNull { it.tokens } ?: 0L
    SectionTitle("趋势", "峰值 ${compactTokens(peak)} · 近 45 天")
    TrendSparkline(daily, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
}

@Composable
private fun DevicesPreview(stats: StatsResponse) {
    val devices = stats.devices ?: emptyList()
    if (devices.isEmpty()) return
    SectionTitle("设备", "${devices.size} 台")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        devices.forEach { d ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(if (d.stale) com.tokenmonitor.mobile.ui.theme.StaleGrey else Success)
                Spacer(Modifier.width(8.dp))
                Text(
                    d.hostname ?: d.deviceId,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = if (d.stale) TextMuted else TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "今日 ${compactTokens(d.periods?.today?.totalTokens ?: 0)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
            }
        }
    }
}




