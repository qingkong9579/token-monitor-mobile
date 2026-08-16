package com.tokenmonitor.mobile.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.data.ProjectEntry
import com.tokenmonitor.mobile.data.StatsResponse
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.PeriodSelector
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.components.ShareBar
import com.tokenmonitor.mobile.ui.theme.TabularFigures
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.util.clientLabel
import com.tokenmonitor.mobile.util.compactTokens
import com.tokenmonitor.mobile.util.formatMoney
import com.tokenmonitor.mobile.util.vendorColor
import com.tokenmonitor.mobile.vm.Period
import com.tokenmonitor.mobile.vm.UiState
import java.util.Locale

/** Per-workspace-project usage rollup, mirroring the desktop Projects view. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(state: UiState, onPeriod: (Period) -> Unit, onRefresh: () -> Unit) {
    val stats = state.stats
    if (stats == null) {
        EmptyState("暂无数据")
        return
    }
    val period = when (state.period) {
        Period.TODAY -> stats.periods?.today
        Period.MONTH -> stats.periods?.month
        Period.ALL_TIME -> stats.periods?.allTime
    } ?: run { EmptyState("暂无数据"); return }
    val projects = period.projects.values
        .filter { it.tokens > 0 }
        .sortedByDescending { it.tokens }
    val total = projects.sumOf { it.tokens }
    val totalCost = projects.sumOf { it.costUsd }
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
            item {
                PeriodSelector(state.period, onPeriod)
                SectionTitle(
                    "项目用量",
                    "${compactTokens(total)} tokens · ${formatMoney(totalCost, state.settings.currency, state.settings.currencyRate)}"
                )
            }
            if (projects.isEmpty()) {
                item { EmptyState("该周期没有项目用量数据\n(桌面端需启用项目追踪)") }
            }
            items(projects) { entry -> ProjectRow(entry, total, state.settings.currency, state.settings.currencyRate) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ProjectRow(entry: ProjectEntry, total: Long, currency: String, rate: Double?) {
    val tokens = entry.tokens
    val topClients = entry.clients.entries.sortedByDescending { it.value }.take(3)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                entry.label ?: "项目",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                compactTokens(tokens),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFeatureSettings = TabularFigures
                )
            )
        }
        ShareBar(
            tokens.toDouble() / total.coerceAtLeast(1),
            vendorColor(entry.clients.keys.firstOrNull()),
            Modifier.fillMaxWidth().padding(vertical = 3.dp)
        )
        Row(Modifier.fillMaxWidth()) {
            Text(
                "费用 ${formatMoney(entry.costUsd, currency, rate)}",
                fontSize = 11.sp,
                color = TextMuted
            )
            topClients.forEach { (client, value) ->
                Spacer(Modifier.width(8.dp))
                Text(
                    "${clientLabel(client)} ${String.format(Locale.US, "%.0f%%", value * 100.0 / tokens)}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }
    }
}
