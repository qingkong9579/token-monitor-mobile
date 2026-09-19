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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.tokenmonitor.mobile.data.BalanceBlock
import com.tokenmonitor.mobile.data.LimitWindow
import com.tokenmonitor.mobile.data.ProviderLimit
import com.tokenmonitor.mobile.ui.components.EmptyState
import com.tokenmonitor.mobile.ui.components.LimitBar
import com.tokenmonitor.mobile.ui.components.SectionTitle
import com.tokenmonitor.mobile.ui.components.ToolIcon
import com.tokenmonitor.mobile.ui.liquid.GlassCard
import com.tokenmonitor.mobile.ui.theme.DividerColor
import com.tokenmonitor.mobile.ui.theme.StaleGrey
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.ui.theme.Warn
import com.tokenmonitor.mobile.util.creditsMeterPercent
import com.tokenmonitor.mobile.util.compactTokens
import com.tokenmonitor.mobile.util.formatLimitBoundary
import com.tokenmonitor.mobile.util.formatLimitMoney
import com.tokenmonitor.mobile.util.isCreditsWindow
import com.tokenmonitor.mobile.util.isSpendWindow
import com.tokenmonitor.mobile.util.limitFillPercent
import com.tokenmonitor.mobile.util.localDateTime
import com.tokenmonitor.mobile.util.providerLabel
import com.tokenmonitor.mobile.util.relativeTime
import com.tokenmonitor.mobile.util.vendorColor
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
    // Providers without any rendered content (e.g. notConfigured / unavailable)
    // are hidden unless the user opts in via Settings. The check uses the
    // *display* cells, not the raw wire windows: balance-only providers like
    // MiMo/DeepSeek render synthesized windows on the desktop too.
    val providers = stats.limits?.providers
        ?.filter {
            state.settings.showEmptyLimitProviders ||
                providerWindowCells(it).isNotEmpty() ||
                it.status != "ok"
        }
        ?: emptyList()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // The desktop keeps single-account rows clean (no email/name clutter) and
    // only labels identities where several accounts share one provider, so the
    // rows can be told apart.
    val accountsPerProvider = providers.groupingBy { it.provider }.eachCount()

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
            items(providers) { p -> ProviderCard(p, (accountsPerProvider[p.provider] ?: 1) > 1) }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ProviderCard(p: ProviderLimit, showIdentity: Boolean) {
    // Upstream paints the row with the provider's brand colour (clientColors),
    // shared by the mark, the meter track/fill and the window hierarchy.
    val brand = limitBrandColor(p)
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 5.dp),
        shape = com.kyant.shapes.RoundedRectangle(20f.dp),
        contentPadding = 14.dp
    ) {
        ProviderHead(p, showIdentity)
        if (p.actionRequired == "accountVerification") {
            Text(
                "需要账号验证：请在对应客户端完成验证后刷新",
                fontSize = 11.sp,
                color = Warn,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        val cells = providerWindowCells(p)
        if (cells.isEmpty()) {
            Text(
                "无额度窗口",
                fontSize = 11.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 6.dp)
            )
        } else {
            WindowsGrid(cells, p, brand)
        }
    }
}

/**
 * The provider header, mirroring the desktop `.limit-head`:
 *
 * ```
 * [mark] Provider name                    plan / status (right column)
 *        identity? · updated 2m ago
 * ```
 *
 * `.limit-title` stacks the name row and the meta line (2px apart); the plan
 * label is its own right-aligned column (`.limit-plan`, capped so it never
 * squeezes the name). Upstream surfaces a non-ok status *as* the plan text, so
 * no separate badge is drawn — a healthy row stays quiet.
 */
@Composable
private fun ProviderHead(p: ProviderLimit, showIdentity: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ToolIcon(client = p.provider, size = 16.dp)
                Text(
                    providerLabel(p.provider),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    // `.limit-row.stale .limit-name` dims the whole row.
                    color = if (p.stale) TextMuted else TextPrimary,
                    modifier = Modifier.weight(1f, fill = false),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            val meta = providerMetaLine(p, showIdentity)
            if (meta.isNotEmpty()) {
                Text(
                    meta,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        val plan = providerPlanText(p)
        if (plan != null) {
            Text(
                plan,
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.widthIn(max = 104.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Account identity + freshness, mirroring `.limit-meta` ("Updated 2m ago").
 *
 * Upstream omits the meta line entirely for a non-ok (and non-stale) row —
 * the status already occupies the plan column, so it is not repeated here. */
private fun providerMetaLine(p: ProviderLimit, showIdentity: Boolean): String {
    val status = p.status
    if (!p.stale && status != null && status != "ok") return ""
    return buildString {
        if (showIdentity) {
            val identity = listOfNotNull(
                p.accountName?.takeIf { it.isNotBlank() },
                p.accountEmail?.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (identity.isNotBlank()) append(identity)
        }
        if (!p.updatedAt.isNullOrBlank()) {
            if (isNotEmpty()) append(" · ")
            append("更新 ${relativeTime(p.updatedAt)}")
        }
        if (p.stale) {
            if (isNotEmpty()) append(" · ")
            append("stale")
        }
    }
}

/**
 * The right-hand plan column, ported from limitProviderPlan: a non-ok status
 * takes its place, the plan label wins otherwise, and Zed's API-provided
 * "Zed Pro" loses the redundant prefix.
 */
private fun providerPlanText(p: ProviderLimit): String? {
    val status = p.status
    val statusActive = !status.isNullOrBlank() && status != "ok" && !p.stale
    if (statusActive) return statusText(p)
    val label = p.planLabel?.trim()?.takeIf { it.isNotEmpty() }
        ?: p.accountLabel?.trim()?.takeIf { it.isNotEmpty() }
    if (label != null) return planDisplayLabel(label, p.provider)
    return if (!status.isNullOrBlank() && status != "ok") statusText(p) else null
}

/** limitProviderDisplayLabel: capitalize unless it looks like an email. */
private fun planDisplayLabel(label: String, provider: String): String {
    var value = label
    if (provider == "zed") {
        value = value.replace(Regex("^Zed\\s+", RegexOption.IGNORE_CASE), "").ifBlank { label }
    }
    if (value.contains("@")) return value
    return value.replaceFirstChar { c ->
        if (c.isLowerCase()) c.titlecase() else c.toString()
    }
}

/** The Chinese status labels the row shows when a provider is not healthy. */
private fun statusText(p: ProviderLimit): String = when (p.status) {
    "notConfigured" -> "未配置"
    "unauthorized" -> "需登录"
    "rateLimited", "sourceRateLimited" -> "受限"
    "unavailable" -> "不可用"
    "disabled" -> "已停用"
    "error", "failed" -> "异常"
    null, "ok" -> if (p.stale) "stale" else "ok"
    else -> p.status ?: "未知"
}

/**
 * Row colour, ported from renderLimits: MiMo trades under Xiaomi's brand, and
 * a third-party row takes its adapter's colour (newapi/sub2api/custom).
 */
@Composable
private fun limitBrandColor(p: ProviderLimit): Color = when (p.provider) {
    "mimo" -> vendorColor("xiaomi")
    // The factory limits row trades under the droid client colour, like the
    // desktop's limitProviderRowColor.
    "factory" -> vendorColor("droid")
    "thirdparty" -> when (p.adapterId?.lowercase()) {
        "newapi-account", "newapi-token" -> Color(0xFFC738FB)
        "sub2api" -> vendorColor("sub2api")
        "custom" -> Color(0xFF8A96A8)
        else -> vendorColor("thirdparty")
    }
    else -> vendorColor(p.provider)
}

// ---------------------------------------------------------------------------
// Window layout. Upstream (renderProviderWindows) picks, orders, groups and
// spans each provider's windows explicitly — a 1fr/1fr grid where most windows
// span both columns (`limit-window-wide`) and the rest pair up — with a
// per-window opacity `tone` ranking primary windows over secondary ones.
// ---------------------------------------------------------------------------

/**
 * One rendered cell of the windows grid: a quota window, a meter-less note
 * row (balance/spend), or an antigravity model group.
 */
private data class WindowCell(
    val window: LimitWindow? = null,
    /** Hard label override (antigravity "5-hour", "MCP", "Balance", codex additional). */
    val label: String? = null,
    /** Branch-default label used only when the window carries no canonical one. */
    val fallbackLabel: String? = null,
    val tone: Float = 1f,
    /** Span the full row instead of pairing up in the 2-column grid. */
    val wide: Boolean = false,
    /** Fixed headline (money balance, credits value) replacing the percent. */
    val valueOverride: String? = null,
    /** Right-hand detail under the bar ("12/800", "Gift ¥x · Cash ¥y"). */
    val detail: String? = null,
    /** `limit-window-no-reset`: the reset line is hidden entirely. */
    val noReset: Boolean = false,
    /** `limit-window-note`: label + value line only, no meter. */
    val note: Boolean = false,
    /** Antigravity grouped quotas: a titled block holding its own grid. */
    val groupTitle: String? = null,
    val groupCells: List<WindowCell> = emptyList()
)

private fun firstKind(p: ProviderLimit, kind: String): LimitWindow? =
    p.windows.firstOrNull { it.kind == kind && !it.additional }

private fun kindsOf(p: ProviderLimit, kind: String): List<LimitWindow> =
    p.windows.filter { it.kind == kind && !it.additional }

/**
 * Compact window list for text rows (Home preview), ported from
 * limitProviderCompactWindows: Codex drops the separately-metered additional
 * buckets; Antigravity keeps only the tightest window per model group (max 2
 * groups, tightest first); MiMo synthesizes its Token Plan from the balance.
 */
internal fun compactLimitWindows(p: ProviderLimit): List<LimitWindow> {
    val base = p.windows.filter { !it.additional }
    return when (p.provider) {
        "codex" -> base
        "antigravity" -> {
            data class Entry(val group: String, val window: LimitWindow, val index: Int)

            val entries = base.mapIndexed { index, w ->
                val suffix = if (w.kind == "session") Regex("\\s+5-hour$", RegexOption.IGNORE_CASE)
                else Regex("\\s+weekly$", RegexOption.IGNORE_CASE)
                val label = w.label?.trim().orEmpty()
                val group = suffix.find(label)?.let { label.replace(suffix, "").trim() }.orEmpty()
                Entry(group, w, index)
            }
            if (entries.isEmpty() || entries.any { it.group.isEmpty() }) return base
            val remaining = { w: LimitWindow ->
                w.remainingPercent?.takeIf { !it.isNaN() }
                    ?: w.usedPercent?.takeIf { !it.isNaN() }?.let { 100.0 - it }
                    ?: Double.POSITIVE_INFINITY
            }
            entries.groupBy { it.group }.entries
                .map { (group, groupEntries) ->
                    val tightest = groupEntries.minWith(
                        compareBy({ remaining(it.window) }, { it.index })
                    )
                    groupEntries.indexOf(tightest) to (remaining(tightest.window) to tightest)
                }
                .sortedBy { (_, pair) -> pair.first }
                .take(2)
                .sortedBy { (_, pair) -> pair.second.index }
                .map { (_, pair) -> pair.second.window }
        }
        "mimo" -> {
            val plan = firstKind(p, "billing") ?: mimoTokenPlanWindow(p.balance)
            if (plan != null && base.none { it.kind == "billing" }) base + plan else base
        }
        // zai mirrors the desktop limit views' selection semantics: ONE rolling
        // session and ONE weekly (a GLM Coding row can carry several same-kind
        // windows once ZCode logins merge into it — duplicates on the home
        // card otherwise), every daily lane, the Start plan buckets, and one
        // MCP + one cash window.
        "zai", "zaiteam" -> {
            val billing = base.filter { it.kind == "billing" }
            buildList {
                firstKind(p, "session")?.let { add(it) }
                addAll(base.filter { it.kind == "daily" })
                firstKind(p, "weekly")?.let { add(it) }
                addAll(billing.filter { !it.limitId.isNullOrBlank() && it.metric.isNullOrBlank() })
                billing.filter { it.metric.isNullOrBlank() && it.limitId.isNullOrBlank() }
                    .take(1)
                    .let { addAll(it) }
                base.filter { it.metric == "credits" }.take(1).let { addAll(it) }
            }
        }
        else -> base
    }
}

private fun moneyOf(value: Double?, currency: String?): String =
    formatLimitMoney(value, currency)

/**
 * Per-provider window selection, ported from renderProviderWindows. The shape
 * decides the layout, not the configured variant — a synced row from another
 * device has no access to local settings.
 */
private fun providerWindowCells(p: ProviderLimit): List<WindowCell> {
    val cells = when (p.provider) {
        "codex" -> codexCells(p)
        "cursor" -> p.windows.map { WindowCell(it, tone = 0.68f, wide = true) }
        "antigravity" -> antigravityCells(p)
        "opencode" -> opencodeCells(p)
        "openrouter" -> openrouterCells(p)
        "thirdparty" -> thirdPartyCells(p)
        "deepseek" -> deepseekCells(p)
        "mimo" -> mimoCells(p)
        "grok", "copilot" -> kindsOf(p, "billing").map {
            WindowCell(it, fallbackLabel = "Monthly", tone = 0.68f, wide = true)
        }
        "zed" -> zedCells(p)
        "zai", "zaiteam" -> zaiCells(p)
        // v0.57: Factory Droid renders its rolling Standard 5-hour + Weekly
        // quota pair like the desktop default branch; Core/legacy billing
        // windows stay in the desktop account details only.
        "factory" -> sessionWeeklyCells(p)
        "volcengine" -> volcengineCells(p)
        "kiro" -> kiroCells(p)
        "qoder" -> kindsOf(p, "billing").firstOrNull()?.let {
            listOf(WindowCell(it, fallbackLabel = "Credits", tone = 0.68f, wide = true, detail = countDetail(it)))
        } ?: emptyList()
        "workbuddy", "trae" -> workbuddyCells(p)
        "commandcode" -> commandcodeCells(p)
        "kimi" -> kimiCells(p)
        "alibaba" -> alibabaCells(p)
        "ollama" -> sessionWeeklyCells(p)
        "claude" -> claudeCells(p)
        else -> sessionWeeklyCells(p, includeOthers = true)
    }
    // `.limit-windows > .limit-window:only-child` spans the full row.
    if (cells.size == 1 && !cells[0].wide && cells[0].groupCells.isEmpty()) {
        return listOf(cells[0].copy(wide = true))
    }
    // An odd non-wide tail (e.g. OpenCode session next to a wide Monthly, or
    // Claude's session beside a model-scoped weekly cap) would leave a
    // half-empty grid row — the desktop widens such odd tails per provider
    // (volcengine: "nodes.length % 2 === 1"); apply it globally so no row
    // renders half-empty.
    val lastNonWide = cells.indexOfLast { !it.wide && it.groupCells.isEmpty() }
    val nonWideCount = cells.count { !it.wide && it.groupCells.isEmpty() }
    if (nonWideCount % 2 == 1 && lastNonWide >= 0) {
        return cells.subList(0, lastNonWide) + listOf(cells[lastNonWide].copy(wide = true)) +
            cells.subList(lastNonWide + 1, cells.size)
    }
    return cells
}

/** Codex: canonical session/weekly/monthly plus the separately-metered buckets. */
private fun codexCells(p: ProviderLimit): List<WindowCell> {
    val session = firstKind(p, "session")
    val weekly = firstKind(p, "weekly")
    val monthly = firstKind(p, "billing")
    val additional = p.windows.filter { it.additional }
    val cells = mutableListOf<WindowCell>()
    if (session != null) {
        cells.add(WindowCell(session, fallbackLabel = "Session", tone = 0.95f,
            wide = weekly == null && monthly == null))
    }
    if (weekly != null) {
        cells.add(WindowCell(weekly, fallbackLabel = "Weekly", tone = 0.68f,
            wide = session == null && monthly == null))
    }
    if (monthly != null) {
        cells.add(WindowCell(monthly, fallbackLabel = "Monthly", tone = 0.68f, wide = true))
    }
    // Shown by default, like the desktop (showCodexAdditionalLimits defaults on).
    for (w in additional) {
        cells.add(WindowCell(w, tone = 0.78f, wide = true, label = codexAdditionalLabel(w, additional)))
    }
    return cells
}

/** codexAdditionalWindowLabel: "gpt-reserve" → "Luna Reserve", disambiguated by period. */
private fun codexAdditionalLabel(w: LimitWindow, siblings: List<LimitWindow>): String {
    val name = w.label?.trim().orEmpty()
    val display = if (name.equals("gpt-reserve", ignoreCase = true)) "Luna Reserve" else name
    val period = when (w.kind) {
        "daily" -> "Daily"
        "weekly" -> "Weekly"
        "billing" -> "Monthly"
        "session" -> "Session"
        else -> ""
    }
    if (display.isEmpty()) return period.ifEmpty { "Additional limit" }
    val sameName = siblings.count { it.label?.trim()?.lowercase() == name.lowercase() }
    return if (sameName > 1 && period.isNotEmpty()) "$display · $period" else display
}

/**
 * Session/weekly pair shared by ollama and the default branch: each window is
 * wide only when it has no partner, so the grid never hides a half cell.
 */
private fun sessionWeeklyCells(p: ProviderLimit, includeOthers: Boolean = false): List<WindowCell> {
    val session = firstKind(p, "session")
    val weekly = firstKind(p, "weekly")
    val cells = mutableListOf<WindowCell>()
    if (session != null) {
        cells.add(WindowCell(session, fallbackLabel = "Session", tone = 0.95f, wide = weekly == null))
    }
    if (weekly != null) {
        cells.add(WindowCell(weekly, fallbackLabel = "Weekly", tone = 0.68f, wide = session == null))
    }
    if (includeOthers) {
        for (w in p.windows.filter { !it.additional && it.kind != "session" && it.kind != "weekly" }) {
            cells.add(WindowCell(w, fallbackLabel = "Quota", tone = 0.68f))
        }
    }
    return cells
}

/**
 * Antigravity: windows labelled "<model> 5-hour" / "<model> weekly" group by
 * model; anything else falls back to a flat layout (legacy GetUserStatus pools
 * keep their flat rows instead of guessing a hierarchy).
 */
private fun antigravityCells(p: ProviderLimit): List<WindowCell> {
    data class Entry(val groupLabel: String, val windowLabel: String, val window: LimitWindow)

    val entries = p.windows
        .filter { it.kind == "session" || it.kind == "weekly" }
        .map { w ->
            val suffix = if (w.kind == "session") Regex("\\s+5-hour$", RegexOption.IGNORE_CASE)
            else Regex("\\s+weekly$", RegexOption.IGNORE_CASE)
            val label = w.label?.trim().orEmpty()
            val match = suffix.find(label)
            if (match == null) null
            else Entry(
                label.replace(suffix, "").trim(),
                if (w.kind == "session") "5-hour" else "Weekly",
                w
            )
        }
    if (entries.isNotEmpty() && entries.none { it == null }) {
        return entries.filterNotNull()
            .groupBy { it.groupLabel }
            .map { (title, group) ->
                WindowCell(
                    groupTitle = title,
                    groupCells = group.map { e ->
                        WindowCell(e.window, label = e.windowLabel, tone = if (e.window.kind == "session") 0.95f else 0.78f)
                    }
                )
            }
    }
    val weeklies = kindsOf(p, "weekly")
    if (weeklies.isNotEmpty()) {
        return weeklies.map { WindowCell(it, fallbackLabel = "Weekly", tone = 0.78f, wide = true) }
    }
    return kindsOf(p, "session").map { WindowCell(it, fallbackLabel = "5-hour", tone = 0.95f, wide = true) }
}

/** OpenCode: Go's three rolling windows plus the Zen prepaid balance note. */
private fun opencodeCells(p: ProviderLimit): List<WindowCell> {
    val cells = mutableListOf<WindowCell>()
    firstKind(p, "session")?.let { cells.add(WindowCell(it, fallbackLabel = "Session", tone = 0.95f)) }
    firstKind(p, "weekly")?.let { cells.add(WindowCell(it, fallbackLabel = "Weekly", tone = 0.68f)) }
    // Monthly spans the full row (like Balance) so it never leaves a half-empty cell.
    firstKind(p, "billing")?.let {
        cells.add(WindowCell(it, fallbackLabel = "Monthly", tone = 0.5f, wide = true))
    }
    p.balanceUsd?.let {
        cells.add(
            WindowCell(note = true, label = "Balance", tone = 0.68f, wide = true,
                valueOverride = moneyOf(it, "USD"), noReset = true)
        )
    }
    return cells
}

/** OpenRouter: prepaid balance first, then every quota window full-width. */
private fun openrouterCells(p: ProviderLimit): List<WindowCell> {
    val balance = p.balance
    val currency = balance?.currency ?: "USD"
    val amount = balance?.amount
    val credits = p.windows.firstOrNull { it.metric == "credits" }
        ?: p.windows.firstOrNull { it.metric.isNullOrBlank() && it.label == "Credits" }
    val cells = mutableListOf<WindowCell>()
    if (amount != null) {
        val balanceWindow = credits
            ?: if (amount == 0.0) {
                LimitWindow(usedPercent = 100.0, remainingPercent = 0.0, showMeter = true)
            } else {
                LimitWindow(showMeter = false)
            }
        cells.add(
            WindowCell(balanceWindow.copy(label = "Balance"), tone = 0.95f, wide = true,
                valueOverride = moneyOf(amount, currency), noReset = true)
        )
    }
    for (w in p.windows.filter { it !== credits }) {
        val hasMeter = w.showMeter != false
        val absolute = if (hasMeter && w.remaining != null && w.limit != null) {
            "${moneyOf(w.remaining, "USD")} 剩余 · 共 ${moneyOf(w.limit, "USD")}"
        } else {
            null
        }
        cells.add(
            WindowCell(w, fallbackLabel = "Usage", tone = if (hasMeter) 0.85f else 0.6f, wide = true,
                valueOverride = if (hasMeter) null else (w.detail ?: "—"), detail = absolute)
        )
    }
    spendNote(balance)?.let { cells.add(it) }
    return cells
}

/** Third-party adapters: balance (or quota) note plus a month/all-time spend row. */
private fun thirdPartyCells(p: ProviderLimit): List<WindowCell> {
    val balance = p.balance
    val currency = balance?.currency ?: "USD"
    val amount = balance?.amount
    val quota = p.windows.firstOrNull { it.metric == "credits" }
    val cells = mutableListOf<WindowCell>()
    if (amount != null) {
        // Balance presets without a fixed quota denominator get the same
        // display-layer meter DeepSeek uses: balance / (balance + month spend).
        val meter = creditsMeterPercent(p, quota)
        val base = (quota ?: LimitWindow(showMeter = false)).copy(label = quota?.label ?: "Balance")
        val window = if (meter != null) base.copy(remainingPercent = meter, showMeter = true) else base
        cells.add(
            WindowCell(window, tone = 0.95f, wide = true,
                valueOverride = moneyOf(amount, currency), noReset = true)
        )
    } else if (quota?.showMeter == false && !quota.detail.isNullOrBlank()) {
        val value = if (quota.detail.equals("unlimited", ignoreCase = true)) "无限" else quota.detail!!
        cells.add(
            WindowCell(quota.copy(label = quota.label ?: "Balance"), tone = 0.95f, wide = true,
                valueOverride = value, noReset = true)
        )
    }
    thirdPartySpendNote(p)?.let { cells.add(it) }
    return cells
}

/** DeepSeek: no quota denominator — visualize the balance against this month's spend. */
private fun deepseekCells(p: ProviderLimit): List<WindowCell> {
    val balance = p.balance ?: return emptyList()
    val cells = mutableListOf(
        WindowCell(
            LimitWindow(remainingPercent = creditsMeterPercent(p, null), showMeter = true),
            tone = 0.95f, wide = true, label = "Balance",
            valueOverride = moneyOf(balance.amount, balance.currency), noReset = true
        )
    )
    spendNote(balance)?.let { cells.add(it) }
    return cells
}

/** MiMo: the Token Plan window (synthesized from the balance when absent) + balances. */
private fun mimoCells(p: ProviderLimit): List<WindowCell> {
    val balance = p.balance
    val cells = mutableListOf<WindowCell>()
    val tokenPlan = firstKind(p, "billing") ?: mimoTokenPlanWindow(balance)
    if (tokenPlan != null) {
        cells.add(WindowCell(tokenPlan, fallbackLabel = "Token Plan", tone = 0.68f, wide = true))
    } else if (balance?.planStatus == "expired") {
        cells.add(WindowCell(note = true, label = "Token Plan", tone = 0.68f, wide = true,
            valueOverride = "套餐已过期", noReset = true))
    }
    val amount = balance?.amount
    val gift = balance?.giftBalance
    val cash = balance?.cashBalance
    if (amount != null || gift != null || cash != null) {
        val currency = balance?.currency
        val detail = listOfNotNull(
            gift?.let { "赠送 ${moneyOf(it, currency)}" },
            cash?.let { "现金 ${moneyOf(it, currency)}" }
        ).joinToString(" · ").ifEmpty { null }
        cells.add(WindowCell(note = true, label = "Balance", tone = 0.68f, wide = true,
            valueOverride = moneyOf(amount, currency) ?: "—", detail = detail))
    }
    return cells
}

/** mimoTokenPlanWindowFromBalance: synthesize the billing window from the plan block. */
private fun mimoTokenPlanWindow(balance: BalanceBlock?): LimitWindow? {
    if (balance == null || balance.planStatus == "expired") return null
    val used = balance.planUsed
    val limit = balance.planLimit
    val percent = balance.planPercent
    if (used == null && limit == null && percent == null) return null
    val resolved = percent?.coerceIn(0.0, 100.0)
        ?: if (used != null && limit != null && limit > 0.0) ((used / limit) * 100.0).coerceIn(0.0, 100.0) else null
    return LimitWindow(
        kind = "billing",
        label = "Token Plan",
        used = used,
        limit = limit,
        remaining = if (used != null && limit != null) (limit - used).coerceAtLeast(0.0) else null,
        usedPercent = resolved,
        remainingPercent = resolved?.let { (100.0 - it).coerceIn(0.0, 100.0) },
        showMeter = true
    )
}

/** Zed: Token Spend / Edit Predictions share the Command Code shape. */
private fun zedCells(p: ProviderLimit): List<WindowCell> =
    kindsOf(p, "billing").map { w ->
        val unlimited = w.limitId == "zed.edit-predictions" && w.detail.equals("unlimited", ignoreCase = true)
        WindowCell(w, fallbackLabel = "Token Spend", tone = 0.95f, wide = true,
            detail = zedBillingDetail(w), noReset = unlimited)
    }

/** Money under the bar for Token Spend, a raw count for metered Edit Predictions. */
private fun zedBillingDetail(w: LimitWindow): String? {
    val used = w.used ?: return null
    val limit = w.limit ?: return null
    if (limit <= 0.0) return null
    if (w.limitId == "zed.edit-predictions") return countDetail(w)
    val currency = w.currency ?: "USD"
    return "${moneyOf((limit - used).coerceAtLeast(0.0), currency)} / ${moneyOf(limit, currency)}"
}

/** zai/GLM Team: the quota pair, ZCode Start/Weekend plan buckets, MCP and cash. */
private fun zaiCells(p: ProviderLimit): List<WindowCell> {
    // Billing-kind windows are one of three things: the subscription MCP
    // monthly bucket (no metric, no limitId), ZCode Start/Weekend plan buckets
    // (limitId set, per-model labels), or the cash balance (metric 'credits').
    val session = firstKind(p, "session")
    val weekly = firstKind(p, "weekly")
    val dailyWindows = kindsOf(p, "daily")
    val billingWindows = kindsOf(p, "billing")
    val planBuckets = billingWindows.filter { !it.limitId.isNullOrBlank() && it.metric.isNullOrBlank() }
    val monthlyWindows = billingWindows.filter { it.metric.isNullOrBlank() && it.limitId.isNullOrBlank() }
    val balanceWindow = p.windows.firstOrNull { it.metric == "credits" }

    val zcodeDetail: (LimitWindow) -> String? = { w ->
        w.detail?.takeIf { it.isNotBlank() } ?: zcodeTokensDetail(w)
    }
    val cells = buildList {
        session?.let { add(WindowCell(it, fallbackLabel = "5-hour", tone = 0.95f)) }
        for ((index, daily) in dailyWindows.withIndex()) {
            add(
                WindowCell(
                    daily,
                    fallbackLabel = if (dailyWindows.size > 1) "Daily ${index + 1}" else "Daily",
                    tone = 0.78f,
                    detail = zcodeDetail(daily)
                )
            )
        }
        weekly?.let { add(WindowCell(it, fallbackLabel = "Weekly", tone = 0.68f)) }
        for (plan in planBuckets) {
            add(
                WindowCell(
                    plan,
                    fallbackLabel = "Start Plan",
                    tone = 0.68f,
                    detail = zcodeDetail(plan)
                )
            )
        }
    }
    // An odd quota pair tail spans the row; monthly buckets stay full width
    // regardless, and the cash pool sits at the bottom as the last resort.
    val padded = if (cells.size % 2 == 1) cells.dropLast(1) + listOf(cells.last().copy(wide = true)) else cells
    return padded +
        monthlyWindows.map {
            WindowCell(it, fallbackLabel = "MCP", tone = 0.68f, wide = true, detail = it.detail)
        } +
        listOfNotNull(
            balanceWindow?.let {
                WindowCell(
                    LimitWindow(
                        remainingPercent = creditsMeterPercent(p, it),
                        showMeter = true
                    ),
                    label = "Balance",
                    tone = 0.95f,
                    wide = true,
                    valueOverride = moneyOf(it.remaining, it.currency),
                    noReset = true
                )
            },
            spendNote(p.balance)
        )
}

/** formatZcodeTokensDetail: "1.2M / 5M" remaining token count for ZCode pools. */
private fun zcodeTokensDetail(w: LimitWindow): String? {
    val remaining = w.remaining ?: return null
    val limit = w.limit ?: return null
    if (limit <= 0.0) return null
    return "${compactTokens(remaining.toLong())} / ${compactTokens(limit.toLong())}"
}

/** Volcengine: four windows; an odd tail spans the row instead of half-filling it. */
private fun volcengineCells(p: ProviderLimit): List<WindowCell> {
    val session = firstKind(p, "session")
    val daily = firstKind(p, "daily")
    val weekly = firstKind(p, "weekly")
    val monthly = firstKind(p, "billing")
    val cells = buildList {
        session?.let { add(WindowCell(it, fallbackLabel = "5-hour", tone = 0.95f)) }
        daily?.let { add(WindowCell(it, fallbackLabel = "Daily", tone = 0.78f)) }
        weekly?.let { add(WindowCell(it, fallbackLabel = "Weekly", tone = 0.68f)) }
        monthly?.let { add(WindowCell(it, fallbackLabel = "Monthly", tone = 0.68f)) }
    }
    return if (cells.size % 2 == 1) {
        cells.dropLast(1) + listOf(cells.last().copy(wide = true))
    } else {
        cells
    }
}

/** Kiro: monthly credit pools + the meter-less overage line. */
private fun kiroCells(p: ProviderLimit): List<WindowCell> =
    kindsOf(p, "billing").map { w ->
        if (w.showMeter == false) {
            WindowCell(w, fallbackLabel = "Overage", tone = 0.6f, wide = true,
                valueOverride = kiroOverageValue(w), noReset = true)
        } else {
            WindowCell(w, fallbackLabel = "Credits", tone = 0.68f, wide = true, detail = countDetail(w))
        }
    }

/** "12.5 credits · $3.20" — credits used, then estimated cost; either may be absent. */
private fun kiroOverageValue(w: LimitWindow): String {
    val parts = mutableListOf<String>()
    w.used?.let { parts.add("${trimCount(it)} credits") }
    w.remaining?.let { parts.add(moneyOf(it, w.currency ?: "USD")) }
    return parts.joinToString(" · ").ifEmpty { "—" }
}

/** Absolute count for credit windows: "rem/total" (quota mode). */
private fun countDetail(w: LimitWindow): String? {
    val used = w.used ?: return null
    val limit = w.limit ?: return null
    if (limit <= 0.0) return null
    return "${trimCount((limit - used).coerceAtLeast(0.0))}/${trimCount(limit)}"
}

private fun trimCount(v: Double): String =
    java.math.BigDecimal(v.coerceAtLeast(0.0)).setScale(2, java.math.RoundingMode.HALF_UP)
        .stripTrailingZeros().toPlainString()

/** WorkBuddy/Trae: prepaid credits + spend note. */
private fun workbuddyCells(p: ProviderLimit): List<WindowCell> {
    val credits = firstKind(p, "billing") ?: return emptyList()
    val balance = p.balance
    val value = credits.remaining ?: balance?.amount ?: return emptyList()
    val cells = mutableListOf(
        WindowCell(
            credits,
            fallbackLabel = "Credits",
            tone = 0.95f, wide = true,
            valueOverride = moneyOf(value, credits.currency ?: balance?.currency),
            noReset = credits.resetsAt.isNullOrBlank() && credits.resetDescription.isNullOrBlank()
        )
    )
    spendNote(balance)?.let { cells.add(it) }
    return cells
}

/** Command Code: percent rate windows + money grants spanning the row. */
private fun commandcodeCells(p: ProviderLimit): List<WindowCell> {
    val fiveHour = firstKind(p, "session")
    val weekly = firstKind(p, "weekly")
    val cells = mutableListOf<WindowCell>()
    if (fiveHour != null) {
        cells.add(WindowCell(fiveHour, fallbackLabel = "5-hour", tone = 0.95f, wide = weekly == null))
    }
    if (weekly != null) {
        cells.add(WindowCell(weekly, fallbackLabel = "Weekly", tone = 0.68f, wide = fiveHour == null))
    }
    for (credits in kindsOf(p, "billing")) {
        val detail = credits.remaining?.let { rem ->
            credits.limit?.takeIf { it > 0.0 }?.let { lim ->
                "${moneyOf(rem, credits.currency)} / ${moneyOf(lim, credits.currency)}"
            }
        }
        cells.add(WindowCell(credits, fallbackLabel = "Monthly", tone = 0.5f, wide = true, detail = detail,
            noReset = credits.showMeter == false))
    }
    return cells
}

/** Kimi: rate windows + the shared monthly membership meter. */
private fun kimiCells(p: ProviderLimit): List<WindowCell> {
    val fiveHour = firstKind(p, "session")
    val weekly = firstKind(p, "weekly")
    val monthly = firstKind(p, "billing")
    val cells = mutableListOf<WindowCell>()
    if (fiveHour != null) {
        cells.add(WindowCell(fiveHour, fallbackLabel = "5-hour", tone = 0.95f, wide = weekly == null))
    }
    if (weekly != null) {
        cells.add(WindowCell(weekly, fallbackLabel = "Weekly", tone = 0.68f, wide = fiveHour == null))
    }
    if (monthly != null) {
        cells.add(WindowCell(monthly, fallbackLabel = "Monthly", tone = 0.5f, wide = true, detail = monthly.detail))
    }
    return cells
}

/** Alibaba Cloud: Team = one credit pool, Personal = rolling 5-hour + weekly. */
private fun alibabaCells(p: ProviderLimit): List<WindowCell> {
    val billing = firstKind(p, "billing")
    val session = firstKind(p, "session")
    val weekly = firstKind(p, "weekly")
    val cells = mutableListOf<WindowCell>()
    if (billing != null) cells.add(WindowCell(billing, fallbackLabel = "Monthly", tone = 0.68f, wide = true))
    if (session != null) {
        cells.add(WindowCell(session, fallbackLabel = "5-hour", tone = 0.95f, wide = weekly == null))
    }
    if (weekly != null) cells.add(WindowCell(weekly, fallbackLabel = "Weekly", tone = 0.68f))
    return cells
}

/** Claude: session + weeklies (a model-scoped cap has no partner), credits + balance. */
private fun claudeCells(p: ProviderLimit): List<WindowCell> {
    val cells = mutableListOf<WindowCell>()
    firstKind(p, "session")?.let { cells.add(WindowCell(it, fallbackLabel = "Session", tone = 0.95f)) }
    for (weekly in kindsOf(p, "weekly")) {
        cells.add(WindowCell(weekly, fallbackLabel = "Weekly", tone = 0.68f, wide = !weekly.label.isNullOrBlank()))
    }
    // Usage credits: "$2.35 / $20.00" (or "$2.35 spent") headline, meter-less reset.
    p.windows.firstOrNull { it.metric == "spend" }?.let {
        cells.add(WindowCell(it, fallbackLabel = "Usage credits", tone = 0.5f, wide = true, noReset = true))
    }
    // Prepaid credits: deliberately meter-less, the headline is the grant sum.
    p.balance?.amount?.let {
        cells.add(WindowCell(note = true, label = "Balance", tone = 0.95f, wide = true,
            valueOverride = moneyOf(it, p.balance?.currency), noReset = true))
    }
    return cells
}

/** The meter-less spend row: "今日 $x · 本月 $y" from the balance block. */
private fun spendNote(balance: BalanceBlock?): WindowCell? {
    if (balance == null) return null
    val currency = balance.currency
    val preferred = listOfNotNull(
        balance.todaySpend?.let { "今日 ${moneyOf(it, currency)}" },
        balance.monthSpend?.let { "本月 ${moneyOf(it, currency)}" }
    )
    val summary = preferred.ifEmpty {
        listOfNotNull(balance.allTimeSpend?.let { "累计 ${moneyOf(it, currency)}" })
    }
    if (summary.isEmpty()) return null
    return WindowCell(note = true, label = "Spend", valueOverride = summary.joinToString(" · "), noReset = true)
}

/** Third-party spend row: month/all-time figures (the desktop tooltip lives here). */
private fun thirdPartySpendNote(p: ProviderLimit): WindowCell? {
    val balance = p.balance ?: return null
    val currency = balance.currency
    val summary = listOfNotNull(
        balance.monthSpend?.let { "本月 ${moneyOf(it, currency)}" },
        balance.allTimeSpend?.let { "累计 ${moneyOf(it, currency)}" }
    ).joinToString(" · ")
    if (summary.isEmpty()) return null
    return WindowCell(note = true, label = "Spend", valueOverride = summary, noReset = true)
}

// ---------------------------------------------------------------------------
// Rendering
// ---------------------------------------------------------------------------

/**
 * The windows grid, mirroring `limit-windows` (1fr 1fr, gap 10px): non-wide
 * cells pair up in row-major order, wide cells (and antigravity groups) start
 * a fresh full-width row, and a lone trailing window leaves its right half
 * empty — exactly the CSS grid's auto-placement — with the only-child rule
 * already applied in [providerWindowCells].
 */
@Composable
private fun WindowsGrid(cells: List<WindowCell>, p: ProviderLimit, brand: Color) {
    Column(
        Modifier.fillMaxWidth().padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Each row is either a full-width cell or one/two half-width cells.
        val rows = mutableListOf<Pair<WindowCell?, List<WindowCell>>>()
        var halves = mutableListOf<WindowCell>()
        for (cell in cells) {
            val blocks = cell.groupCells.isNotEmpty() || cell.wide
            if (blocks) {
                if (halves.isNotEmpty()) {
                    rows.add(null to halves)
                    halves = mutableListOf()
                }
                rows.add(cell to emptyList())
            } else {
                halves.add(cell)
                if (halves.size == 2) {
                    rows.add(null to halves)
                    halves = mutableListOf()
                }
            }
        }
        if (halves.isNotEmpty()) rows.add(null to halves)

        for ((full, pair) in rows) {
            when {
                full != null && full.groupCells.isNotEmpty() -> AntigravityGroup(full, p, brand)
                full != null -> WindowCellView(full, p, brand, Modifier.fillMaxWidth())
                pair.size == 2 -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WindowCellView(pair[0], p, brand, Modifier.weight(1f))
                    WindowCellView(pair[1], p, brand, Modifier.weight(1f))
                }
                else -> Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WindowCellView(pair[0], p, brand, Modifier.weight(1f))
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** `.limit-window-group`: the model title above its own pair of windows. */
@Composable
private fun AntigravityGroup(cell: WindowCell, p: ProviderLimit, brand: Color) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            cell.groupTitle.orEmpty(),
            fontSize = 10.sp,
            color = TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // `> .limit-window:only-child` spans the full row inside a group too.
            val single = cell.groupCells.size == 1
            for (pair in cell.groupCells.chunked(2)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WindowCellView(pair[0], p, brand, if (single) Modifier.fillMaxWidth() else Modifier.weight(1f))
                    if (pair.size == 2) WindowCellView(pair[1], p, brand, Modifier.weight(1f))
                    else if (!single) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun WindowCellView(c: WindowCell, p: ProviderLimit, brand: Color, modifier: Modifier) {
    val w = c.window
    Column(modifier) {
        // `.limit-window-text`: the label is muted, the value carries the primary
        // text colour. Upstream never tints the limits-panel value by quota
        // health — only the Home card does that.
        val cellLabel = c.label
            ?: w?.label?.takeIf { it.isNotBlank() }
            ?: c.fallbackLabel
            ?: windowLabel(w)
        if (w == null) {
            // `.limit-spend` note row: the label keeps its intrinsic width and
            // the summary ellipsizes, so "Spend"/"Balance" never truncates.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cellLabel,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    c.valueOverride ?: "—",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    cellLabel,
                    fontSize = 11.sp,
                    color = TextMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    headline(c, w, p),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        val meterPercent = if (w == null || w.showMeter == false) null else meterPercent(w, p)
        if (meterPercent != null) {
            LimitBar(
                percent = meterPercent / 100.0,
                color = brand,
                modifier = Modifier.padding(top = 5.dp),
                // Upstream's meter `tone` separates a provider's primary window
                // from its secondary ones (session full strength, weekly softer).
                tone = c.tone
            )
        }

        val detail = c.detail ?: autoDetail(w)
        if (c.noReset || w == null) {
            // Meter-less notes with an attached figure (MiMo Gift/Cash); note
            // rows themselves carry no reset line at all, like the desktop.
            if (detail != null) {
                Text(
                    detail,
                    fontSize = 10.sp,
                    color = TextPrimary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            // `.limit-reset` carries a *minimum* height (upstream: min-height,
            // not a fixed height) so the two columns of a row stay aligned even
            // when empty, while taller type (large font scale) still fits — a
            // fixed height clipped the line to its top half; the split variant
            // puts the absolute detail on the right.
            val resetText = resetLine(w, p)
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .heightIn(min = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    resetText,
                    fontSize = 10.sp,
                    color = TextMuted,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (detail != null) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        detail,
                        fontSize = 10.sp,
                        color = TextPrimary.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/** Remaining percent (0-100) feeding the meter, or null when there is none. */
private fun meterPercent(w: LimitWindow, p: ProviderLimit): Double? {
    // Money meters are derived, never carried on the wire
    // (limitBalanceDisplay.creditsMeterPercent): a top-up balance has no fixed
    // quota denominator, so it is visualized against this month's starting funds.
    if (isCreditsWindow(w)) return creditsMeterPercent(p, w)
    if (w.remainingPercent != null || w.usedPercent != null) {
        return limitFillPercent(w.remainingPercent, w.usedPercent) * 100.0
    }
    return null
}

/** The headline value on the right of the label line. */
private fun headline(c: WindowCell, w: LimitWindow?, p: ProviderLimit): String {
    if (w == null) return c.valueOverride ?: "—"
    if (c.valueOverride != null) return c.valueOverride
    val currency = w.currency ?: p.balance?.currency
    val unlimited = w.detail.equals("unlimited", ignoreCase = true)
    return when {
        w.planStatus == "expired" -> "套餐已过期"
        unlimited -> "无限"
        isCreditsWindow(w) -> moneyOf(w.remaining ?: p.balance?.amount, currency)
        isSpendWindow(w) -> if (w.limit != null) {
            "${moneyOf(w.used, currency)} / ${moneyOf(w.limit, currency)}"
        } else {
            "${moneyOf(w.used, currency)} 已用"
        }
        meterPercent(w, p) != null -> "剩余 ${meterPercent(w, p)!!.roundToInt()}%"
        w.remaining != null -> {
            val amt = moneyOf(w.remaining, currency)
            if (w.showMeter != false) "$amt 剩余" else amt
        }
        w.limit != null -> "${moneyOf(w.limit, currency)} 上限"
        else -> w.detail ?: "—"
    }
}

/** "重置 2天 3小时后" / "到期 5小时20分后", falling back to the reset description. */
private fun resetLine(w: LimitWindow?, p: ProviderLimit): String {
    if (w == null) return ""
    val base = when {
        !w.resetsAt.isNullOrBlank() -> formatLimitBoundary(w)
        !w.resetDescription.isNullOrBlank() -> w.resetDescription!!
        else -> return ""
    }
    if (base.isEmpty()) return ""
    // Best-effort burn-down estimate, Codex only — upstream keeps its forecast
    // scoped to Codex's reset-credits module too.
    if (p.provider != "codex") return base
    val forecast = exhaustionForecast(w) ?: return base
    return "$base · $forecast"
}

/** The absolute figure under the bar when the provider branch passed none. */
private fun autoDetail(w: LimitWindow?): String? {
    if (w == null) return null
    val unlimited = w.detail.equals("unlimited", ignoreCase = true)
    return w.detail?.takeIf { it.isNotBlank() && !unlimited }
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
