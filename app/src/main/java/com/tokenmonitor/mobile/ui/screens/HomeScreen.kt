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
import androidx.compose.material3.HorizontalDivider
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
import com.tokenmonitor.mobile.data.ProviderLimit
import com.tokenmonitor.mobile.data.StatsResponse
import com.tokenmonitor.mobile.ui.components.ActivityHeatmap
import com.tokenmonitor.mobile.ui.components.ActivityHeatmapLegend
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.ErrorCard
import com.tokenmonitor.mobile.ui.components.GlassButton
import com.tokenmonitor.mobile.ui.components.LimitBar
import com.tokenmonitor.mobile.ui.components.PeriodSelector
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.components.StatCard
import com.tokenmonitor.mobile.ui.components.StatusDot
import com.tokenmonitor.mobile.ui.components.ToolIcon
import com.tokenmonitor.mobile.ui.components.TrendSparkline
import com.tokenmonitor.mobile.ui.liquid.GlassCard
import com.tokenmonitor.mobile.ui.theme.Accent
import com.tokenmonitor.mobile.ui.theme.AccentOn
import com.tokenmonitor.mobile.ui.theme.Error
import com.tokenmonitor.mobile.ui.theme.Success
import com.tokenmonitor.mobile.ui.theme.TabularFigures
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.ui.theme.Warn
import com.tokenmonitor.mobile.util.clientLabel
import com.tokenmonitor.mobile.util.compactTokens
import com.tokenmonitor.mobile.util.formatMoney
import com.tokenmonitor.mobile.util.pctDisplay
import com.tokenmonitor.mobile.util.providerLabel
import com.tokenmonitor.mobile.vm.Period
import com.tokenmonitor.mobile.vm.UiState
import java.util.Locale

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
            ErrorCard(
                "网络异常,显示上次同步数据 · ${clockTime(state.lastUpdated)}",
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
    val providers = stats.limits?.providers
        ?.filter { showEmptyProviders || it.windows.isNotEmpty() }
        ?: emptyList()
    if (providers.isEmpty()) return
    SectionTitle("额度", "${providers.size} 个 provider")
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        providers
            .sortedByDescending { p -> p.windows.maxOfOrNull { it.usedPercent ?: 0.0 } ?: 0.0 }
            .take(3)
            .forEach { p ->
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = com.kyant.shapes.RoundedRectangle(18f.dp),
                    contentPadding = 10.dp
                ) {
                    LimitProviderRow(p, currency, rate)
                }
            }
    }
}

@Composable
fun LimitProviderRow(p: ProviderLimit, currency: String, rate: Double?) {
    val worst = p.windows.maxByOrNull { it.usedPercent ?: 0.0 }
    val pct = worst?.usedPercent ?: 0.0
    val color = when {
        pct >= 85 -> Error
        pct >= 60 -> Warn
        else -> Success
    }
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                providerLabel(p.provider),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary,
                modifier = Modifier.weight(1f)
            )
            if (p.stale) {
                Text("stale", fontSize = 11.sp, color = TextMuted)
            }
            if (worst != null && pct > 0) {
                Text(
                    pctDisplay(pct),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }
        if (worst != null && worst.usedPercent != null && (worst.metric != "credits")) {
            LimitBar(pct / 100.0, Modifier.padding(top = 3.dp))
        } else if (worst != null && worst.usedPercent == null && worst.metric == "credits") {
            // credits window: headline is money, no meter
            val remaining = worst.remaining
            if (remaining != null) {
                Text(
                    "余额 ${worst.currency?.let { " $it" } ?: ""}${formatCredits(remaining)}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}

private fun formatCredits(v: Double): String =
    if (v >= 10) String.format(java.util.Locale.US, "%.2f", v)
    else String.format(java.util.Locale.US, "%.4f", v)

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
        HorizontalDivider(color = com.tokenmonitor.mobile.ui.theme.DividerColor, modifier = Modifier.padding(vertical = 4.dp))
    }
}




