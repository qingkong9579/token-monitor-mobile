package com.tokenmonitor.mobile.ui.screens

import androidx.compose.foundation.layout.Arrangement
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
import kotlin.math.roundToInt
import com.tokenmonitor.mobile.data.LimitWindow
import com.tokenmonitor.mobile.data.ProviderLimit
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.LimitBar
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.components.ToolIcon
import com.tokenmonitor.mobile.ui.liquid.GlassCard
import com.tokenmonitor.mobile.ui.theme.Error
import com.tokenmonitor.mobile.ui.theme.StaleGrey
import com.tokenmonitor.mobile.ui.theme.Success
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.ui.theme.Warn
import com.tokenmonitor.mobile.util.creditsMeterPercent
import com.tokenmonitor.mobile.util.formatLimitMoney
import com.tokenmonitor.mobile.util.isCreditsWindow
import com.tokenmonitor.mobile.util.isSpendWindow
import com.tokenmonitor.mobile.util.limitFillPercent
import com.tokenmonitor.mobile.util.localDateTime
import com.tokenmonitor.mobile.util.providerLabel
import com.tokenmonitor.mobile.util.relativeTime
import com.tokenmonitor.mobile.util.windowLabel
import com.tokenmonitor.mobile.vm.UiState

/** AI Tool Limits: provider accounts with their quota windows, mirroring the desktop Limits view. */
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
        contentPadding = 14.dp
    ) {
        // Header: tool icon + name (with identity / updated), right-aligned status pill.
        Row(verticalAlignment = Alignment.CenterVertically) {
            ToolIcon(client = p.provider, size = 22.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    providerLabel(p.provider),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val identity = listOfNotNull(
                    p.planLabel,
                    p.accountLabel,
                    p.accountName,
                    p.accountEmail
                ).joinToString(" · ")
                val subLine = buildString {
                    if (identity.isNotBlank()) append(identity)
                    if (!p.updatedAt.isNullOrBlank()) {
                        if (isNotEmpty()) append(" · ")
                        append("更新 ${relativeTime(p.updatedAt)}")
                    } else if (p.stale) {
                        if (isNotEmpty()) append(" · ")
                        append("stale")
                    }
                }
                if (subLine.isNotEmpty()) {
                    Text(
                        subLine,
                        fontSize = 11.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            ProviderStatusPill(p, statusColor)
        }
        if (p.actionRequired == "accountVerification") {
            Text(
                "需要账号验证：请在对应客户端完成验证后刷新",
                fontSize = 11.sp,
                color = Warn,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        val visible = displayWindows(p)
        if (visible.isEmpty()) {
            Text(
                "无额度窗口",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            WindowsGrid(visible, p)
        }
    }
}

/**
 * The windows a provider card renders: Codex's separately-metered buckets
 * (`additional: true`) are collapsed like the upstream compact view, and MiMo
 * gets its Token Plan billing window synthesized from the balance block when
 * the provider reports no window of its own (desktop
 * mimoTokenPlanWindowFromBalance).
 */
private fun displayWindows(p: ProviderLimit): List<LimitWindow> {
    val base = p.windows.filter { !it.additional }
    if (p.provider != "mimo" || base.any { it.kind == "billing" }) return base
    val b = p.balance ?: return base
    val used = b.planUsed
    val limit = b.planLimit
    val pct = b.planPercent
    if (used == null && limit == null && pct == null) return base
    val resolved = pct ?: if (used != null && limit != null && limit > 0.0) (used / limit) * 100.0 else null
    return base + LimitWindow(
        kind = "billing",
        label = "Token Plan",
        used = used,
        limit = limit,
        remaining = if (used != null && limit != null) (limit - used).coerceAtLeast(0.0) else null,
        usedPercent = resolved,
        remainingPercent = resolved?.let { (100.0 - it).coerceIn(0.0, 100.0) },
        showMeter = true,
        planStatus = b.planStatus
    )
}

/**
 * Renders the provider's windows in a 1- or 2-column grid that mirrors the
 * upstream `limit-windows` CSS (grid-template-columns: 1fr 1fr, only-child
 * spans full row). A two-window row gets equal halves; a trailing single
 * window spans the full width.
 */
@Composable
private fun WindowsGrid(windows: List<LimitWindow>, p: ProviderLimit) {
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        val rows = windows.chunked(2)
        for ((i, row) in rows.withIndex()) {
            if (row.size == 2) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WindowCell(row[0], p, Modifier.weight(1f))
                    WindowCell(row[1], p, Modifier.weight(1f))
                }
            } else {
                WindowCell(row[0], p, Modifier.fillMaxWidth())
            }
            if (i != rows.lastIndex) Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun WindowCell(w: LimitWindow, p: ProviderLimit, modifier: Modifier) {
    Column(modifier) {
        WindowRow(w, p)
    }
}

@Composable
private fun WindowRow(w: LimitWindow, p: ProviderLimit) {
    val showMeter = w.showMeter != false
    val isCredits = isCreditsWindow(w)
    val isSpend = isSpendWindow(w)
    val hasPercent = w.remainingPercent != null || w.usedPercent != null
    // Money meters are derived, never carried on the wire
    // (limitBalanceDisplay.creditsMeterPercent): a top-up balance has no fixed
    // quota denominator, so it is visualized against this month's starting funds.
    val meterPercent: Double? = when {
        !showMeter -> null
        isCredits -> creditsMeterPercent(p, w)
        hasPercent -> limitFillPercent(w.remainingPercent, w.usedPercent) * 100.0
        else -> null
    }
    val currency = w.currency ?: p.balance?.currency
    val unlimited = w.detail.equals("unlimited", ignoreCase = true)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            windowLabel(w),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val headline = when {
            w.planStatus == "expired" -> "套餐已过期"
            unlimited -> "无限"
            isCredits -> formatLimitMoney(w.remaining ?: p.balance?.amount, currency)
            isSpend -> if (w.limit != null) {
                "${formatLimitMoney(w.used, currency)} / ${formatLimitMoney(w.limit, currency)}"
            } else {
                "${formatLimitMoney(w.used, currency)} 已用"
            }
            meterPercent != null -> "剩余 ${meterPercent.roundToInt()}%"
            w.remaining != null -> {
                val amt = formatLimitMoney(w.remaining, currency)
                if (showMeter) "$amt 剩余" else amt
            }
            w.limit != null -> "${formatLimitMoney(w.limit, currency)} 上限"
            else -> w.detail ?: "—"
        }
        Text(
            headline,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (meterPercent != null && !unlimited && w.planStatus != "expired") {
                barColor(meterPercent / 100.0)
            } else {
                TextPrimary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
    if (meterPercent != null) {
        LimitBar(meterPercent / 100.0, Modifier.padding(top = 4.dp))
    }
    // Upstream falls back to the display-only resetDescription for windows that
    // carry no resetsAt timestamp (e.g. Zed's "Unlimited" edit predictions).
    val resetText = when {
        !w.resetsAt.isNullOrBlank() -> "重置 ${localDateTime(w.resetsAt)}"
        !w.resetDescription.isNullOrBlank() -> w.resetDescription
        else -> null
    }
    val forecast = exhaustionForecast(w)
    val meta = listOfNotNull(resetText, forecast).joinToString(" · ")
    if (meta.isNotEmpty()) {
        Text(
            meta,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 3.dp)
        )
    }
    val detail = w.detail?.takeIf { it.isNotBlank() && !unlimited && it != w.resetDescription }
    if (detail != null) {
        Text(
            detail,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProviderStatusPill(p: ProviderLimit, fallback: Color) {
    val text = when {
        p.stale -> "stale"
        p.status == "ok" -> "正常"
        p.status == "notConfigured" -> "未配置"
        p.status == "unauthorized" -> "需登录"
        p.status == "rateLimited" || p.status == "sourceRateLimited" -> "受限"
        p.status == "unavailable" -> "不可用"
        p.status == "disabled" -> "已停用"
        p.status.isNullOrBlank() -> "未知"
        p.status == "error" || p.status == "failed" -> "异常"
        else -> p.status
    }
    Text(
        text,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = fallback
    )
}

/**
 * Best-effort "estimated exhaustion" for a quota window, based on linear burn
 * since the window's assumed start (session ≈ 5h, daily ≈ 1d, weekly ≈ 7d,
 * billing ≈ 30d). Returns null when the window has no used percent, no reset
 * time, or the burn rate is not positive. It is a display-layer estimate, not
 * a wire value.
 */
private fun exhaustionForecast(w: LimitWindow): String? {
    val used = w.usedPercent ?: return null
    if (used <= 0.0) return null
    val resetsAt = parseIsoTime(w.resetsAt) ?: return null
    val now = System.currentTimeMillis()
    val windowMs = when (w.kind) {
        "session" -> 5L * 60 * 60 * 1000
        "daily" -> 24L * 60 * 60 * 1000
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
private fun barColor(remainingFraction: Double): Color = when {
    remainingFraction < 0.20 -> Error
    remainingFraction < 0.50 -> Warn
    else -> Success
}

