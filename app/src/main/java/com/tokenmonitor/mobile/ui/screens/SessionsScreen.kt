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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.data.SessionEntry
import com.tokenmonitor.mobile.data.StatsResponse
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.PeriodSelector
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.theme.TabularFigures
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.util.clientLabel
import com.tokenmonitor.mobile.util.compactTokens
import com.tokenmonitor.mobile.util.formatMoney
import com.tokenmonitor.mobile.util.localTime
import com.tokenmonitor.mobile.util.sessionIdLabel
import com.tokenmonitor.mobile.util.vendorColor
import com.tokenmonitor.mobile.vm.Period
import com.tokenmonitor.mobile.vm.UiState

/** Per-session usage, mirroring the desktop Sessions view. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(state: UiState, onPeriod: (Period) -> Unit, onRefresh: () -> Unit) {
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
    val sessions = period.sessions.values.sortedByDescending { it.totalTokens }
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
                SectionTitle("会话用量", "${sessions.size} 个会话")
            }
            if (sessions.isEmpty()) {
                item { EmptyState("该周期没有会话数据") }
            }
            items(sessions) { s -> SessionRow(s, state.settings.currency, state.settings.currencyRate) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun SessionRow(s: SessionEntry, currency: String, rate: Double?) {
    val clientColor = vendorColor(s.client)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .width(8.dp)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(clientColor)
            )
            Spacer(Modifier.width(6.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    s.projectLabel?.ifBlank { null } ?: clientLabel(s.client),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val sid = sessionIdLabel(s.sessionId)
                if (sid.isNotBlank()) {
                    Text(
                        sid,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                compactTokens(s.totalTokens),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    fontFeatureSettings = TabularFigures
                )
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp)) {
            Text("费用 ${formatMoney(s.costUsd, currency, rate)}", fontSize = 11.sp, color = TextMuted)
            s.messageCount?.let {
                Spacer(Modifier.width(8.dp))
                Text("$it 条消息", fontSize = 11.sp, color = TextMuted)
            }
            s.startedAt?.let {
                Spacer(Modifier.width(8.dp))
                Text("开始 ${localTime(it)}", fontSize = 11.sp, color = TextMuted)
            }
        }
    }
}
