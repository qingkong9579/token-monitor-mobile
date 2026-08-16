/*
 * Glass card built on the Backdrop Liquid Glass effect library
 * (https://github.com/Kyant0/AndroidLiquidGlass), Apache-2.0.
 */
package com.tokenmonitor.mobile.ui.liquid

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangle
import com.tokenmonitor.mobile.ui.theme.BgDeep
import com.tokenmonitor.mobile.ui.theme.SurfaceTint

/**
 * Optional app-wide backdrop for glass surfaces — when the app renders a
 * wallpaper / custom background image, the host provides a layer backdrop of
 * that image so glass cards blur the real background (Control Center look).
 * Null (default) falls back to a flat canvas backdrop.
 */
val LocalGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * A liquid-glass panel: vibrancy + blur + lens over a backdrop,
 * with an edge highlight and drop shadow — the Control Center tile look.
 * Colors come from the active theme; on devices without RenderEffect support
 * (Android < 12) the library skips the effects and the tint still renders.
 *
 * [tint] overrides the theme surface tint for the whole panel — used for
 * "selected" glass chips (a teal frosted capsule). Null keeps the theme tint.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedRectangle(24f.dp),
    contentPadding: Dp = 12.dp,
    tint: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundColor = BgDeep
    val surfaceTint = SurfaceTint
    val effectiveTint = tint ?: surfaceTint

    val provided = LocalGlassBackdrop.current
    val backdrop = provided
        ?: rememberCanvasBackdrop { drawRect(backgroundColor) }

    Column(
        modifier
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(18f.dp.toPx())
                    lens(36f.dp.toPx(), 36f.dp.toPx())
                },
                highlight = { Highlight.Default.copy(alpha = 0.25f) },
                shadow = { Shadow(radius = 18f.dp, alpha = 0.35f) },
                onDrawSurface = {}
            )
            // The surface tint is applied as a clipped background so it follows
            // the rounded shape — an unclipped draw inside the glass layer paints
            // a square that shows past the corners.
            .clip(shape)
            .background(effectiveTint)
            .padding(contentPadding),
        content = content
    )
}
