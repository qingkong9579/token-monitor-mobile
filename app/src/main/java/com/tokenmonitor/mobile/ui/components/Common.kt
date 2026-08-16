package com.tokenmonitor.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.ui.theme.Accent
import com.tokenmonitor.mobile.ui.theme.AccentOn
import com.tokenmonitor.mobile.ui.theme.CardBg
import com.tokenmonitor.mobile.ui.theme.CardBgRaised
import com.tokenmonitor.mobile.ui.theme.Error
import com.tokenmonitor.mobile.ui.theme.ErrorBg
import com.tokenmonitor.mobile.ui.theme.StaleBg
import com.tokenmonitor.mobile.ui.theme.StaleGrey
import com.tokenmonitor.mobile.ui.theme.Success
import com.tokenmonitor.mobile.ui.theme.SuccessBg
import com.tokenmonitor.mobile.ui.theme.TabularFigures
import com.tokenmonitor.mobile.ui.theme.TextMuted
import com.tokenmonitor.mobile.ui.theme.TextPrimary
import com.tokenmonitor.mobile.ui.theme.Warn
import com.tokenmonitor.mobile.ui.theme.WarnBg
import com.tokenmonitor.mobile.ui.liquid.GlassCard
import com.tokenmonitor.mobile.util.fmtPercent
import com.tokenmonitor.mobile.vm.Period

@Composable
fun SectionTitle(title: String, subtitle: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Aurora streak: the short teal tick that anchors every section header.
        Box(
            Modifier
                .width(3.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Accent)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.alignByBaseline()
        )
        if (subtitle != null) {
            Text(
                "  $subtitle",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                modifier = Modifier.alignByBaseline()
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    sub: String,
    accent: Color = Accent,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, shape = com.kyant.shapes.RoundedRectangle(18f.dp), contentPadding = 12.dp) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = TextMuted)
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = accent,
                fontFeatureSettings = TabularFigures
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(sub, style = MaterialTheme.typography.labelSmall, color = TextMuted)
    }
}

@Composable
fun AttrRow(
    label: String,
    value: String,
    valueColor: Color = TextPrimary,
    barFraction: Double? = null,
    barColor: Color = Accent,
    weight: Float = 1f
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(weight)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (barFraction != null) {
            ShareBar(barFraction, barColor, Modifier.width(64.dp).padding(horizontal = 6.dp))
        }
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

@Composable
fun ShareBar(fraction: Double, color: Color, modifier: Modifier = Modifier) {
    val clamped = fraction.coerceIn(0.0, 1.0)
    Box(
        modifier = modifier
            .height(6.dp)
            .clip(CircleShape)
            .background(CardBgRaised)
    ) {
        Box(
            Modifier
                .fillMaxWidth(clamped.toFloat().coerceAtLeast(0.02f))
                .height(6.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun LimitBar(percent: Double, modifier: Modifier = Modifier) {
    val clamped = percent.coerceIn(0.0, 1.0)
    val color = when {
        percent >= 0.85 -> Error
        percent >= 0.6 -> Warn
        else -> Success
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(CircleShape)
            .background(CardBgRaised)
    ) {
        Box(
            Modifier
                .fillMaxWidth(clamped.toFloat().coerceAtLeast(0.02f))
                .height(8.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun StatusDot(
    color: Color,
    size: Int = 8,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
    )
}

/** Live / stale / offline pill, mirroring the desktop status pill. */
@Composable
fun StatusPill(state: PillState, label: String) {
    val (color, bg) = when (state) {
        PillState.LIVE -> Success to SuccessBg
        PillState.WARN -> Warn to WarnBg
        PillState.OFFLINE -> StaleGrey to StaleBg
        PillState.ERROR -> Error to ErrorBg
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatusDot(color)
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

enum class PillState { LIVE, WARN, OFFLINE, ERROR }

@Composable
fun EmptyState(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun ErrorCard(text: String, onRetry: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ErrorBg)
            .then(
                if (onRetry != null) {
                    Modifier.clickable(onClick = onRetry)
                } else Modifier
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text,
                style = MaterialTheme.typography.bodySmall,
                color = Error,
                modifier = Modifier.weight(1f)
            )
            if (onRetry != null) {
                Text(
                    "重试",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = Error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

/** Frosted-glass period chip (today / month / all-time), shared by Home and Tools/Models. */
@Composable
fun PeriodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Selected chip reads as a teal frosted capsule; unselected keeps the
    // neutral glass tint. Text uses the same ink-on-accent pairing as before.
    val tint = if (selected) Accent.copy(0.5f) else null
    val fg = if (selected) AccentOn else TextMuted
    GlassCard(
        modifier = modifier.clickable(onClick = onClick),
        shape = com.kyant.shapes.Capsule(),
        contentPadding = 0.dp,
        tint = tint
    ) {
        Box(
            Modifier
                .height(40.dp)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = fg
            )
        }
    }
}

/**
 * The period selector row (今天 / 本月 / 累计) with one spacing rhythm across
 * every page. [trailing] lets a screen append a right-aligned element (Home
 * shows the last-update time); Tools / Models pass no trailing content.
 */
@Composable
fun PeriodSelector(
    selected: Period,
    onPeriod: (Period) -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Period.entries.forEach { p ->
            PeriodChip(
                label = p.label,
                selected = p == selected,
                onClick = { onPeriod(p) }
            )
        }
        if (trailing != null) {
            Spacer(Modifier.weight(1f))
            trailing()
        }
    }
}

/**
 * Frosted-glass capsule button in the app's glass language (period chips,
 * glass cards). Primary actions pass a teal [tint] and ink text; secondary
 * actions keep the neutral glass. When [enabled] is false the button dims and
 * the click is ignored.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    content: @Composable RowScope.() -> Unit
) {
    GlassCard(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.5f)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        shape = com.kyant.shapes.Capsule(),
        contentPadding = 0.dp,
        tint = tint
    ) {
        Row(
            Modifier
                .height(44.dp)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            content()
        }
    }
}

/**
 * A frosted-glass selection chip, matching the period chips and glass buttons
 * so every interactive control on a page uses the same material and press
 * effect. Selected chips read as teal frosted tiles.
 */
@Composable
fun GlassChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) Accent.copy(0.5f) else null
    val fg = if (selected) AccentOn else TextMuted
    GlassCard(
        modifier = modifier
            .height(32.dp)
            .clickable(onClick = onClick),
        shape = com.kyant.shapes.RoundedRectangle(10f.dp),
        contentPadding = 0.dp,
        tint = tint
    ) {
        // Wrap content width (not fillMaxSize) so several chips fit a row.
        Box(
            Modifier
                .height(32.dp)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = fg
            )
        }
    }
}

/**
 * Rounded-rectangle glass tab for the bottom navigation — the desktop's
 * view-switcher buttons carried into the app's liquid-glass language. The
 * selected tab reads as a teal frosted tile, like the period chips.
 */
@Composable
fun NavTabButton(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tint = if (selected) Accent.copy(0.5f) else null
    val fg = if (selected) AccentOn else TextMuted
    GlassCard(
        modifier = modifier
            .height(50.dp)
            .clickable(onClick = onClick),
        shape = com.kyant.shapes.RoundedRectangle(12f.dp),
        contentPadding = 0.dp,
        tint = tint
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
                Text(label, fontSize = 8.sp, color = fg, maxLines = 1)
            }
        }
    }
}

/** Circular liquid-glass settings button for the bottom-right corner. */
@Composable
fun SettingsCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier
            .size(50.dp)
            .clickable(onClick = onClick),
        shape = com.kyant.shapes.RoundedRectangle(25f.dp),
        contentPadding = 0.dp,
        tint = Accent.copy(0.35f)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Settings, contentDescription = "设置", tint = AccentOn)
        }
    }
}
