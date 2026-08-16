package com.tokenmonitor.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.data.HistoryDay
import com.tokenmonitor.mobile.data.HistorySummary
import com.tokenmonitor.mobile.data.StatsResponse
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.liquid.GlassCard
import com.tokenmonitor.mobile.ui.theme.Accent
import com.tokenmonitor.mobile.ui.theme.TabularFigures
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.util.compactTokens
import com.tokenmonitor.mobile.util.formatMoney
import com.tokenmonitor.mobile.util.historyDateLabel
import com.tokenmonitor.mobile.vm.UiState

/** History trends: a daily token bar chart over historyPreview.daily plus a summary. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendsScreen(state: UiState, onRefresh: () -> Unit) {
    val preview = state.stats?.historyPreview
    val days = preview?.daily ?: emptyList()
    val visible = days.takeLast(30)
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
            item { SectionTitle("趋势", "${days.size} 天记录") }
            if (days.isEmpty()) {
                item { EmptyState("Hub 未提供历史数据\n(桌面端需开启 History)") }
            } else {
                item { TrendSummaryCard(preview?.summary, state.settings.currency, state.settings.currencyRate) }
                item { TrendBarChart(visible) }
                item { SectionTitle("最近记录") }
                items(days.takeLast(14).reversed()) { day ->
                    TrendRow(day, state.settings.currency, state.settings.currencyRate)
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun TrendSummaryCard(summary: HistorySummary?, currency: String, rate: Double?) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = com.kyant.shapes.RoundedRectangle(18f.dp),
        contentPadding = 12.dp
    ) {
        Row {
            SummaryMetric("活跃天数", summary?.activeDays?.toString() ?: "—", Modifier.weight(1f))
            SummaryMetric("最长连续", summary?.longestStreak?.let { "$it 天" } ?: "—", Modifier.weight(1f))
            SummaryMetric("峰值", compactTokens(summary?.peakDayTokens ?: 0), Modifier.weight(1f))
            SummaryMetric("总费用", formatMoney(summary?.totalCost ?: 0.0, currency, rate), Modifier.weight(1f))
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFeatureSettings = TabularFigures
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}

@Composable
private fun TrendBarChart(days: List<HistoryDay>) {
    val max = days.maxOfOrNull { it.tokens } ?: return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(110.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        days.forEach { day ->
            val fraction = (day.tokens.toDouble() / max).coerceIn(0.03, 1.0)
            Box(
                Modifier
                    .weight(1f)
                    .padding(horizontal = 1.dp)
                    .fillMaxHeight(fraction.toFloat())
                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                    .background(Accent.copy(0.85f))
            )
        }
    }
}

@Composable
private fun TrendRow(day: HistoryDay, currency: String, rate: Double?) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            historyDateLabel(day.date),
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.weight(1f)
        )
        Text(
            compactTokens(day.tokens),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFeatureSettings = TabularFigures
            )
        )
        Spacer(Modifier.width(12.dp))
        Text(
            formatMoney(day.cost, currency, rate),
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                color = TextMuted,
                fontFeatureSettings = TabularFigures
            ),
            textAlign = TextAlign.End,
            modifier = Modifier.width(64.dp)
        )
    }
}
