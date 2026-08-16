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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.data.DeviceRecord
import com.tokenmonitor.mobile.data.StatsResponse
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.components.StatusDot
import com.tokenmonitor.mobile.ui.theme.DividerColor
import com.tokenmonitor.mobile.ui.theme.StaleGrey
import com.tokenmonitor.mobile.ui.theme.Success
import com.tokenmonitor.mobile.ui.theme.TabularFigures
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.util.compactTokens
import com.tokenmonitor.mobile.util.formatMoney
import com.tokenmonitor.mobile.util.relativeTime
import com.tokenmonitor.mobile.vm.UiState

/** Per-device usage, sync status and freshness — stale devices grey out like the desktop. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(state: UiState, onRefresh: () -> Unit) {
    val stats = state.stats
    if (stats == null) {
        EmptyState("暂无数据")
        return
    }
    val devices = stats.devices ?: emptyList()
    val currency = state.settings.currency
    val rate = state.settings.currencyRate
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
            item { SectionTitle("设备", "${devices.size} 台 · stale 置灰") }
            if (devices.isEmpty()) {
                item { EmptyState("Hub 上没有设备记录") }
            }
            items(devices) { d -> DeviceCard(d, currency, rate) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DeviceCard(d: DeviceRecord, currency: String, rate: Double?) {
    val muted = d.stale
    val primaryColor = if (muted) TextMuted else TextPrimary
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(if (d.stale) StaleGrey else Success)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    d.hostname ?: d.deviceId,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = primaryColor
                )
                Text(
                    "${d.deviceId} · ${platformLabel(d.platform)}",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
            Text(
                if (d.stale) "stale" else "live",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (d.stale) TextMuted else Success
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Metric("今日", compactTokens(d.periods?.today?.totalTokens ?: 0), muted, Modifier.weight(1f))
            Metric("本月", compactTokens(d.periods?.month?.totalTokens ?: 0), muted, Modifier.weight(1f))
            Metric("累计", compactTokens(d.periods?.allTime?.totalTokens ?: 0), muted, Modifier.weight(1f))
            Metric(
                "费用",
                formatMoney(d.periods?.allTime?.costUsd ?: 0.0, currency, rate),
                muted,
                Modifier.weight(1f)
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 3.dp)) {
            Text(
                "更新 ${relativeTime(d.receivedAt)}",
                fontSize = 11.sp,
                color = TextMuted
            )
            Spacer(Modifier.width(8.dp))
            Text(
                d.osName?.let { "$it ${d.osVersion ?: ""}".trim() } ?: "",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(1.dp)
                .background(DividerColor)
        )
    }
}

@Composable
private fun Metric(label: String, value: String, muted: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(
            value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (muted) TextMuted else TextPrimary,
                fontFeatureSettings = TabularFigures
            )
        )
        Text(label, fontSize = 11.sp, color = TextMuted)
    }
}

private fun platformLabel(platform: String?): String = when (platform ?: "") {
    "win32-x64", "win32-arm64" -> "Windows"
    "darwin-arm64", "darwin-x64" -> "macOS"
    "linux-x64", "linux-arm64" -> "Linux"
    else -> platform ?: "unknown"
}


