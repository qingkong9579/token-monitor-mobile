package com.tokenmonitor.mobile.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Aurora ground stops — night (dark theme) and dawn (light theme). The glass
 * panels blur this gradient, so the sky reads through every frosted pane.
 */
val DarkAuroraColors = listOf(Color(0xFF241B3B), Color(0xFF1B2C55), Color(0xFF123A4A))
val LightAuroraColors = listOf(Color(0xFFEDE9FA), Color(0xFFE2EFF7), Color(0xFFD9EFEB))

/**
 * The "living aurora" ground the glass panels float over. A slow vertical drift
 * of the middle stop gives the gradient life; users who turned on the system
 * "Remove animations" accessibility setting (animator duration scale = 0) get a
 * static gradient instead. The drift is read inside
 * [androidx.compose.ui.draw.drawBehind], so it animates the draw pass only —
 * no recomposition on the frames it moves.
 */
@Composable
fun AuroraBackground(isDark: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }
    val colors = remember(isDark) { if (isDark) DarkAuroraColors else LightAuroraColors }

    val drift: State<Float> =
        if (reduceMotion) {
            remember { mutableFloatStateOf(0.45f) }
        } else {
            val infinite = rememberInfiniteTransition(label = "aurora")
            infinite.animateFloat(
                0.30f, 0.62f,
                infiniteRepeatable(tween(50_000, easing = LinearEasing)),
                label = "drift"
            )
        }

    Box(
        modifier.drawBehind {
            drawRect(
                Brush.linearGradient(
                    0f to colors[0],
                    drift.value to colors[1],
                    1f to colors[2],
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
        }
    )
}
