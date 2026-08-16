package com.tokenmonitor.mobile.util

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Ports of the desktop app's presentation helpers
 * (src/shared/compactTokens.js, src/shared/compactMoney.js,
 * src/electron/renderer/usageCharts.js clientColors / modelVendorFor).
 */

fun compactTokens(n: Long): String {
    val v = n.toDouble()
    return when {
        v >= 1e9 -> String.format(Locale.US, "%.2fB", v / 1e9)
        v >= 1e6 -> String.format(Locale.US, "%.2fM", v / 1e6)
        v >= 1e3 -> String.format(Locale.US, "%.1fK", v / 1e3)
        else -> n.toString()
    }
}

val CURRENCY_CODES = listOf("USD", "CNY", "TWD", "HKD")

fun moneySymbol(currency: String): String = when (currency.uppercase()) {
    "USD" -> "$"
    "CNY" -> "¥"
    "TWD" -> "NT$"
    "HKD" -> "HK$"
    else -> "$"
}

/** Built-in approximate rates used only when the user has not set a custom rate. */
fun defaultRate(currency: String): Double = when (currency.uppercase()) {
    "CNY" -> 7.2
    "TWD" -> 32.5
    "HKD" -> 7.85
    else -> 1.0
}

fun effectiveRate(currency: String, customRate: Double?): Double =
    customRate ?: defaultRate(currency)

fun formatMoneyUsd(usd: Double): String = if (usd >= 10) {
    String.format(Locale.US, "$%.2f", usd)
} else {
    String.format(Locale.US, "$%.4f", usd)
}

fun formatMoney(usd: Double, currency: String, customRate: Double?): String {
    val sym = moneySymbol(currency)
    val value = usd * effectiveRate(currency, customRate)
    return if (value >= 10) {
        String.format(Locale.US, "%s%.2f", sym, value)
    } else {
        String.format(Locale.US, "%s%.4f", sym, value)
    }
}

fun fmtPercent(v: Double): String = String.format(Locale.US, "%.0f%%", v)

/** 0-100 percentage that may carry float noise -> clean display text (0.8 / 99.2 / 42). */
fun pctDisplay(v: Double?): String {
    if (v == null) return "—"
    val rounded = String.format(Locale.US, "%.1f", v).trimEnd('0').trimEnd('.')
    return "$rounded%"
}

/** "3 分钟前" style relative time for hub timestamps. */
fun relativeTime(iso: String?, now: Long = System.currentTimeMillis()): String {
    if (iso.isNullOrBlank()) return "—"
    val t = parseIso(iso) ?: return "—"
    val diff = now - t
    return when {
        diff < 0 -> "刚刚"
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000 -> "${diff / 3_600_000} 小时前"
        else -> "${diff / 86_400_000} 天前"
    }
}

/** Local wall-clock time (HH:mm) of a hub timestamp. */
fun localTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val t = parseIso(iso) ?: return "—"
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(t))
}

/** Local date + time (MM-dd HH:mm) of a hub timestamp. */
fun localDateTime(iso: String?): String {
    if (iso.isNullOrBlank()) return "—"
    val t = parseIso(iso) ?: return "—"
    return SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(t))
}

private fun parseIso(iso: String): Long? = try {
    // Handles "2026-05-18T00:00:00.000Z" and "2026-05-18T00:00:00Z"
    val normalized = if (iso.endsWith("Z")) iso else "$iso Z".trimEnd()
    val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    sdf.timeZone = TimeZone.getTimeZone("UTC")
    val t = sdf.parse(normalized) ?: return null
    t.time
} catch (e: Exception) {
    try {
        val sdf2 = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf2.timeZone = TimeZone.getTimeZone("UTC")
        sdf2.parse(iso)?.time
    } catch (e2: Exception) {
        null
    }
}

// ---------------------------------------------------------------------------
// Vendor identity & colours (mirrors usageCharts.js clientColors + modelVendorFor)
// ---------------------------------------------------------------------------

val VENDOR_COLORS: Map<String, Color> = mapOf(
    "claude" to Color(0xFFCC7C5E),
    "codex" to Color(0xFF49A3B0),
    "hermes" to Color(0xFFD4AF37),
    "gemini" to Color(0xFF4285F4),
    "antigravity" to Color(0xFF4285F4),
    "cline" to Color(0xFF64748B),
    "kimi" to Color(0xFF4C9EEB),
    "grok" to Color(0xFFA78BFA),
    "copilot" to Color(0xFFF472B6),
    "deepseek" to Color(0xFF4D6BFE),
    "cursor" to Color(0xFF7DD3FC),
    "opencode" to Color(0xFF34D399),
    "openrouter" to Color(0xFF6566F1),
    "openclaw" to Color(0xFFFF4D4D),
    "xai" to Color(0xFFFBBF24),
    "meta" to Color(0xFF1D65C1),
    "mistral" to Color(0xFFFA520F),
    "qwen" to Color(0xFF615CED),
    "pi" to Color(0xFFFB7185),
    "zed" to Color(0xFF4173E7),
    "kilocode" to Color(0xFFF8F676),
    "commandcode" to Color(0xFF8C4EDD),
    "micode" to Color(0xFFF97316),
    "zcode" to Color(0xFFA3E635),
    "kiro" to Color(0xFF9046FF),
    "codebuddy" to Color(0xFF6C4DFF),
    "workbuddy" to Color(0xFF0DC8A5),
    "proma" to Color(0xFF94A3B8),
    "reasonix" to Color(0xFF4D6BFE),
    "moonshot" to Color(0xFF38BDF8),
    "zai" to Color(0xFFE879F9),
    "zaiteam" to Color(0xFFC084FC),
    "cohere" to Color(0xFF39594D),
    "xiaomi" to Color(0xFFFF6700),
    "minimax" to Color(0xFFF23F5D),
    "doubao" to Color(0xFF1E37FC),
    "hunyuan" to Color(0xFF0053E0),
    "volcengine" to Color(0xFF006EFF),
    "qoder" to Color(0xFF2ADB5C),
    "ollama" to Color(0xFF888888),
    "thirdparty" to Color(0xFFDD2E57),
    "default" to Color(0xFF6AB4F0)
)

fun vendorColor(vendor: String?): Color = VENDOR_COLORS[vendor] ?: VENDOR_COLORS["default"]!!

fun vendorForModel(model: String?): String? {
    val name = (model ?: "").lowercase()
    return when {
        name.isBlank() -> null
        Regex("^(cursor-)?auto$").containsMatchIn(name) -> "cursor"
        Regex("claude|anthropic|sonnet|opus|haiku").containsMatchIn(name) -> "claude"
        Regex("gpt|openai|codex|^o[134](?:-|$)|o[134]-(mini|pro|preview)|chatgpt").containsMatchIn(name) -> "codex"
        Regex("gemini|gemma|google").containsMatchIn(name) -> "gemini"
        Regex("grok|xai").containsMatchIn(name) -> "xai"
        Regex("deepseek").containsMatchIn(name) -> "deepseek"
        Regex("llama|meta").containsMatchIn(name) -> "meta"
        Regex("mistral|mixtral|codestral").containsMatchIn(name) -> "mistral"
        Regex("qwen|qwq|qvq").containsMatchIn(name) -> "qwen"
        Regex("kimi|moonshot").containsMatchIn(name) -> "kimi"
        Regex("chatglm|\\bglm-|\\bzai\\b|z\\.ai|zhipu").containsMatchIn(name) -> "zai"
        Regex("cohere|command-r").containsMatchIn(name) -> "cohere"
        Regex("mimo|xiaomi").containsMatchIn(name) -> "xiaomi"
        Regex("minimax").containsMatchIn(name) -> "minimax"
        Regex("doubao|volc|ark").containsMatchIn(name) -> "doubao"
        Regex("hunyuan|混元").containsMatchIn(name) -> "hunyuan"
        Regex("reasonix|antigravity").containsMatchIn(name) -> "antigravity"
        else -> null
    }
}

fun modelColor(model: String?): Color = vendorColor(vendorForModel(model))

/** Display labels mirroring the desktop renderer's clientLabels map. */
val CLIENT_LABELS: Map<String, String> = mapOf(
    "claude" to "Claude Code",
    "codex" to "Codex",
    "hermes" to "Hermes Agent",
    "opencode" to "OpenCode",
    "openclaw" to "OpenClaw",
    "cursor" to "Cursor",
    "antigravity" to "Antigravity",
    "cline" to "Cline",
    "kimi" to "Kimi",
    "qwen" to "Qwen CLI",
    "grok" to "Grok Build",
    "copilot" to "GitHub Copilot",
    "pi" to "Pi",
    "zed" to "Zed",
    "kilocode" to "Kilo Code",
    "commandcode" to "Command Code",
    "micode" to "MiMo Code",
    "zcode" to "ZCode",
    "kiro" to "Kiro",
    "codebuddy" to "CodeBuddy",
    "workbuddy" to "WorkBuddy",
    "proma" to "Proma",
    "reasonix" to "Reasonix",
    "deepseek" to "DeepSeek",
    "openrouter" to "OpenRouter",
    "minimax" to "Minimax",
    "volcengine" to "Volcengine",
    "qoder" to "Qoder",
    "ollama" to "Ollama",
    "gemini" to "Gemini",
    "xai" to "xAI",
    "mistral" to "Mistral",
    "moonshot" to "Moonshot",
    "zai" to "Z.ai",
    "zaiteam" to "GLM Team",
    "cohere" to "Cohere",
    "xiaomi" to "Xiaomi",
    "doubao" to "Doubao",
    "hunyuan" to "Hunyuan",
    "thirdparty" to "Third-party APIs",
    "newapi" to "Third-party APIs"
)

fun clientLabel(id: String?): String = CLIENT_LABELS[id] ?: id ?: "Unknown"

val PROVIDER_LABELS: Map<String, String> = mapOf(
    "claude" to "Claude Code",
    "codex" to "Codex",
    "cursor" to "Cursor",
    "antigravity" to "Antigravity",
    "opencode" to "OpenCode",
    "openrouter" to "OpenRouter",
    "deepseek" to "DeepSeek",
    "minimax" to "Minimax",
    "mimo" to "MiMo",
    "grok" to "Grok",
    "copilot" to "GitHub Copilot",
    "kiro" to "Kiro",
    "zai" to "GLM (Z.ai)",
    "zaiteam" to "GLM Team",
    "volcengine" to "Volcengine",
    "qoder" to "Qoder",
    "kimi" to "Kimi",
    "ollama" to "Ollama",
    "thirdparty" to "Third-party APIs"
)

fun providerLabel(id: String?): String = PROVIDER_LABELS[id] ?: id ?: "Unknown"

fun windowKindLabel(kind: String?): String = when (kind) {
    "session" -> "会话额度"
    "weekly" -> "周额度"
    "billing" -> "账单周期"
    "credits" -> "余额"
    else -> kind ?: "额度"
}

/**
 * Short display label for a session id, mirroring the desktop's sessionIdLabel:
 * a "rollout-<timestamp>-<rest>" id reduces to the `<rest>` suffix; a bare
 * timestamp-shaped id (no label after it) renders empty.
 */
fun sessionIdLabel(id: String?): String {
    val raw = id?.trim() ?: return ""
    val rollout = Regex("^rollout-\\d{4}-\\d{2}-\\d{2}T\\d{2}[:-]\\d{2}[:-]\\d{2}-(.+)$").find(raw)
    if (rollout != null) return rollout.groupValues[1]
    if (Regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}[:-]\\d{2}").containsMatchIn(raw)) return ""
    return raw
}

/** "2026-06-14" (local date) -> "06-14" for trends rows. */
fun historyDateLabel(date: String?): String {
    val d = date ?: return "—"
    return if (d.length >= 10) d.substring(5, 10) else d
}
