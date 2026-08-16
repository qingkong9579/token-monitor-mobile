/**
 * Design contract — Aurora Glass
 *
 * THESIS: The app's Liquid Glass foundation extends upward: the default
 * background is a living aurora (deep violet → teal) that frosted glass panes
 * float over; each tile's hero number is the brightest point. It refuses the
 * "dark data dashboard = flat near-black + one accent" default.
 *
 * OWN-WORLD: Aurora night ground (violet → indigo → teal gradient); glass
 * tiles tinted a faint cyan; accent = aurora teal; hairline cyan dividers;
 * tabular numerals everywhere. Light mode is a pale lavender → sky aurora with
 * a deep-teal accent for contrast on the bright ground.
 *
 * STORY: The user reads today's token burn and cost at a glance, in tiles that
 * feel like frosted panes over a night sky; live state glows emerald.
 *
 * FIRST VIEWPORT: "Token Monitor" + Live pill over the aurora; period chips;
 * three glass stat tiles (今日/本月/累计) with the hero number in bold tabular
 * teal; 额度/工具/设备 sections; the liquid-glass tab bar.
 *
 * FORM: Aurora Glass, adapted from the Aurora Maximalism anchor, disciplined
 * for Operate — expression never obscures the task.
 */
package com.tokenmonitor.mobile.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Semantic app colors, provided per theme (dark / light) through
 * [LocalAppColors]. Components must read these (or MaterialTheme.colorScheme),
 * never hard-coded hex, so the light theme is a first-class citizen.
 */
@Immutable
data class AppColors(
    val isDark: Boolean,
    val bgDeep: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val cardBg: Color,
    val cardBgRaised: Color,
    val accent: Color,
    val accentOn: Color,
    val success: Color,
    val successBg: Color,
    val warn: Color,
    val warnBg: Color,
    val error: Color,
    val errorBg: Color,
    val stale: Color,
    val staleBg: Color,
    val contentColor: Color,
    val surfaceTint: Color,
    val divider: Color
)

private val DarkAppColors = AppColors(
    isDark = true,
    bgDeep = Color(0xFF171221),
    textPrimary = Color(0xFFEEF3FC),
    textMuted = Color(0xFFA6B0C6),
    cardBg = Color(0xFF332E4C),
    cardBgRaised = Color(0xFF3D3757),
    accent = Color(0xFF7CE0E0),
    accentOn = Color(0xFF0B1B22),
    success = Color(0xFF34D399),
    successBg = Color(0x2634D399),
    warn = Color(0xFFFBBF24),
    warnBg = Color(0x26FBBF24),
    error = Color(0xFFF87171),
    errorBg = Color(0x26F87171),
    stale = Color(0xFF7A8299),
    staleBg = Color(0x267A8299),
    contentColor = Color.White,
    surfaceTint = Color(0xFF7CE0E0).copy(0.07f),
    divider = Color(0x1FFFFFFF)
)

private val LightAppColors = AppColors(
    isDark = false,
    bgDeep = Color(0xFFF3F1FB),
    textPrimary = Color(0xFF1C2434),
    textMuted = Color(0xFF5C6475),
    cardBg = Color(0xFFFFFFFF),
    cardBgRaised = Color(0xFFE7E3F2),
    accent = Color(0xFF0E7C86),
    accentOn = Color.White,
    success = Color(0xFF18794E),
    successBg = Color(0x1A18794E),
    warn = Color(0xFFB45309),
    warnBg = Color(0x1AB45309),
    error = Color(0xFFDC2626),
    errorBg = Color(0x1ADC2626),
    stale = Color(0xFF9CA3AF),
    staleBg = Color(0x1A9CA3AF),
    contentColor = Color.Black,
    surfaceTint = Color.Black.copy(0.06f),
    divider = Color(0x1A000000)
)

val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// Composable getters keep existing call sites (`color = TextMuted`) compiling
// while making every value theme-aware.
val BgDeep: Color @Composable get() = LocalAppColors.current.bgDeep
val TextPrimary: Color @Composable get() = LocalAppColors.current.textPrimary
val TextMuted: Color @Composable get() = LocalAppColors.current.textMuted
val CardBg: Color @Composable get() = LocalAppColors.current.cardBg
val CardBgRaised: Color @Composable get() = LocalAppColors.current.cardBgRaised
val Accent: Color @Composable get() = LocalAppColors.current.accent
val AccentOn: Color @Composable get() = LocalAppColors.current.accentOn
val Success: Color @Composable get() = LocalAppColors.current.success
val SuccessBg: Color @Composable get() = LocalAppColors.current.successBg
val Warn: Color @Composable get() = LocalAppColors.current.warn
val WarnBg: Color @Composable get() = LocalAppColors.current.warnBg
val Error: Color @Composable get() = LocalAppColors.current.error
val ErrorBg: Color @Composable get() = LocalAppColors.current.errorBg
val StaleGrey: Color @Composable get() = LocalAppColors.current.stale
val StaleBg: Color @Composable get() = LocalAppColors.current.staleBg
val ContentColor: Color @Composable get() = LocalAppColors.current.contentColor
val SurfaceTint: Color @Composable get() = LocalAppColors.current.surfaceTint
val DividerColor: Color @Composable get() = LocalAppColors.current.divider

private val DarkColors = darkColorScheme(
    primary = DarkAppColors.accent,
    onPrimary = DarkAppColors.accentOn,
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1B1530),
    tertiary = Color(0xFF7CE0E0),
    background = DarkAppColors.bgDeep,
    onBackground = DarkAppColors.textPrimary,
    surface = Color(0xFF26203A),
    onSurface = DarkAppColors.textPrimary,
    surfaceVariant = DarkAppColors.cardBg,
    onSurfaceVariant = DarkAppColors.textMuted,
    outline = Color(0xFF4E4A66),
    error = DarkAppColors.error,
    onError = Color.White
)

private val LightColors = lightColorScheme(
    primary = LightAppColors.accent,
    onPrimary = Color.White,
    secondary = LightAppColors.success,
    onSecondary = Color.White,
    background = LightAppColors.bgDeep,
    onBackground = LightAppColors.textPrimary,
    surface = Color(0xFFFFFFFF),
    onSurface = LightAppColors.textPrimary,
    surfaceVariant = LightAppColors.cardBg,
    onSurfaceVariant = LightAppColors.textMuted,
    outline = Color(0xFFB9BEC6),
    error = LightAppColors.error,
    onError = Color.White
)

/**
 * Tabular figures ("tnum") for the app's numeric displays. Applied ONLY to
 * pure-Latin/numeric texts (token counts, costs, percents) — never to mixed
 * CJK+digit strings. Applying font features to the whole type ramp caused the
 * fallback-font runs of mixed-script paragraphs to render on misaligned
 * baselines, so the feature is opt-in per numeric Text instead of global.
 */
const val TabularFigures = "tnum"

/**
 * [darkTheme] is the user's explicit choice (Settings). When null the system
 * setting decides; MainActivity passes the stored preference.
 */
@Composable
fun TokenMonitorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val useDark = darkTheme
    val appColors = if (useDark) DarkAppColors else LightAppColors
    androidx.compose.runtime.CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = if (useDark) DarkColors else LightColors,
            content = content
        )
    }
}
