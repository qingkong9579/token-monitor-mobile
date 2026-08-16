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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.data.PeriodSummary
import com.tokenmonitor.mobile.data.StatsResponse
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.PeriodSelector
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.components.ShareBar
import com.tokenmonitor.mobile.ui.components.ToolIcon
import com.tokenmonitor.mobile.ui.theme.TabularFigures
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.util.clientLabel
import com.tokenmonitor.mobile.util.compactTokens
import com.tokenmonitor.mobile.util.formatMoney
import com.tokenmonitor.mobile.util.vendorColor
import com.tokenmonitor.mobile.util.vendorForModel
import com.tokenmonitor.mobile.vm.Period
import com.tokenmonitor.mobile.vm.UiState

/** Full per-tool breakdown with cache read/write, output and hit rate. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(state: UiState, onPeriod: (Period) -> Unit, onRefresh: () -> Unit) {
    val stats = state.stats
    if (stats == null) {
        EmptyState("暂无数据")
        return
    }
    val summary = when (state.period) {
        Period.TODAY -> stats.periods?.today
        Period.MONTH -> stats.periods?.month
        Period.ALL_TIME -> stats.periods?.allTime
    }
    val period = summary ?: run { EmptyState("暂无数据"); return }
    val total = period.totalTokens
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
                    "工具用量",
                    "${compactTokens(total)} tokens · ${formatMoney(period.costUsd, state.settings.currency, state.settings.currencyRate)}"
                )
            }
            if (period.clients.isEmpty()) {
                item { EmptyState("该周期没有工具用量数据") }
            }
            items(period.clients.entries.sortedByDescending { it.value }) { (id, tokens) ->
                val cacheRead = period.clientCacheReads[id] ?: 0
                val cacheWrite = period.clientCacheWrites[id] ?: 0
                val output = period.clientOutputs[id] ?: 0
                val cost = period.clientCosts[id] ?: 0.0
                ToolDetailCard(
                    client = id,
                    label = clientLabel(id),
                    color = vendorColor(id),
                    tokens = tokens,
                    total = total,
                    cost = cost,
                    cacheRead = cacheRead,
                    cacheWrite = cacheWrite,
                    output = output,
                    currency = state.settings.currency,
                    rate = state.settings.currencyRate
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/** Per-model usage & cost aggregated across tools. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelsScreen(state: UiState, onPeriod: (Period) -> Unit, onRefresh: () -> Unit) {
    val stats = state.stats
    if (stats == null) {
        EmptyState("暂无数据")
        return
    }
    val summary = when (state.period) {
        Period.TODAY -> stats.periods?.today
        Period.MONTH -> stats.periods?.month
        Period.ALL_TIME -> stats.periods?.allTime
    }
    val period = summary ?: run { EmptyState("暂无数据"); return }
    val total = period.totalTokens
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
                    "模型用量",
                    "${compactTokens(total)} tokens · ${formatMoney(period.costUsd, state.settings.currency, state.settings.currencyRate)}"
                )
            }
            if (period.models.isEmpty()) {
                item { EmptyState("该周期没有模型用量数据") }
            }
            items(period.models.entries.sortedByDescending { it.value }) { (model, tokens) ->
                ModelDetailRow(
                    model = model,
                    tokens = tokens,
                    total = total,
                    cost = period.modelCosts[model] ?: 0.0,
                    cacheRead = period.modelCacheReads[model] ?: 0,
                    output = period.modelOutputs[model] ?: 0,
                    unclassified = period.modelUnclassifiedTokens[model] ?: 0,
                    currency = state.settings.currency,
                    rate = state.settings.currencyRate
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ModelDetailRow(
    model: String,
    tokens: Long,
    total: Long,
    cost: Double,
    cacheRead: Long,
    output: Long,
    unclassified: Long,
    currency: String,
    rate: Double?
) {
    // Token-component breakdown, mirroring the desktop's tokenComponentBreakdown:
    // cacheMiss (fresh input) is the classified remainder after cache-hit input
    // and output, with unclassified carved out first.
    val classified = (tokens - unclassified).coerceAtLeast(0)
    val cacheReadClamped = minOf(classified, cacheRead).coerceAtLeast(0)
    val outputClamped = minOf(classified - cacheReadClamped, output).coerceAtLeast(0)
    val cacheMiss = (classified - cacheReadClamped - outputClamped).coerceAtLeast(0)
    val share = if (total > 0) tokens * 100.0 / total else 0.0

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        // Left: model logo + name; right: token count and cost.
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIcon(client = vendorForModel(model), size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                model,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
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
            Spacer(Modifier.width(8.dp))
            Text(
                formatMoney(cost, currency, rate),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    color = TextMuted,
                    fontFeatureSettings = TabularFigures
                )
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            ModelCell("输入(缓存命中)", compactTokens(cacheReadClamped), Modifier.weight(1f))
            ModelCell("占比", String.format(java.util.Locale.US, "%.1f%%", share), Modifier.weight(1f))
            ModelCell("token数", compactTokens(tokens), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            ModelCell("输入(缓存未命中)", compactTokens(cacheMiss), Modifier.weight(1f))
            ModelCell("输出", compactTokens(outputClamped), Modifier.weight(1f))
            ModelCell("未分类", compactTokens(unclassified), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ModelCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 2.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFeatureSettings = TabularFigures
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(label, fontSize = 11.sp, color = TextMuted, maxLines = 1)
    }
}

@Composable
private fun ToolDetailCard(
    client: String?,
    label: String,
    color: Color,
    tokens: Long,
    total: Long,
    cost: Double,
    cacheRead: Long,
    cacheWrite: Long,
    output: Long,
    currency: String,
    rate: Double?
) {
    val attributed = (cacheRead + cacheWrite + output).toDouble()
    val hitRate = if (attributed > 0) cacheRead / attributed else 0.0
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIcon(client = client, size = 18.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                label,
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
        ShareBar(tokens.toDouble() / total, color, Modifier.fillMaxWidth().padding(vertical = 3.dp))
        Row(Modifier.fillMaxWidth()) {
            DetailCell("缓存命中", compactTokens(cacheRead), Modifier.weight(1f))
            DetailCell("缓存写入", compactTokens(cacheWrite), Modifier.weight(1f))
            DetailCell("输出", compactTokens(output), Modifier.weight(1f))
            DetailCell("命中率", String.format(java.util.Locale.US, "%.0f%%", hitRate * 100), Modifier.weight(1f))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(
                "费用 ${formatMoney(cost, currency, rate)}",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "占比 ${String.format(java.util.Locale.US, "%.1f%%", tokens.toDouble() / total * 100)}",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun DetailCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 2.dp)) {
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                fontFeatureSettings = TabularFigures
            )
        )
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}




