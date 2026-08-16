package com.tokenmonitor.mobile.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.caverock.androidsvg.SVG
import com.tokenmonitor.mobile.util.vendorColor
import java.util.Locale

/**
 * Client (tool) id → SVG asset file name, mirroring the desktop renderer's
 * row-icon CSS: hermes→hermes-agent, grok↔xai, micode/mimo→xiaomi,
 * thirdparty→newapi, zaiteam/zcode→zai. To add a new tool, drop `<client>.svg`
 * (or add one alias line here) into assets/icons/.
 */
private val CLIENT_ICON_FILE: Map<String, String> = mapOf(
    "hermes" to "hermes-agent",
    "grok" to "xai",
    "xai" to "grok",
    "micode" to "xiaomi",
    "mimo" to "xiaomi",
    "thirdparty" to "newapi",
    "zaiteam" to "zai",
    "zcode" to "zai"
)

private const val ICON_ASSET_DIR = "icons"
private const val ICON_RENDER_SIZE = 96f

private val iconCache = mutableMapOf<String, Bitmap?>()

/** Renders an SVG tool icon from assets/icons/<file>.svg, cached per client. */
private fun toolIconBitmap(context: Context, client: String?): Bitmap? {
    val file = CLIENT_ICON_FILE[client] ?: client ?: return null
    return iconCache.getOrPut(file) {
        try {
            val input = context.assets.open("$ICON_ASSET_DIR/$file.svg")
            val svg = SVG.getFromInputStream(input)
            val viewBox = svg.documentViewBox
            val w = (viewBox?.width() ?: 24f).coerceAtLeast(1f)
            val h = (viewBox?.height() ?: 24f).coerceAtLeast(1f)
            val scale = (ICON_RENDER_SIZE / maxOf(w, h)).coerceAtLeast(0.1f)
            val outW = (w * scale).toInt().coerceIn(1, 256)
            val outH = (h * scale).toInt().coerceIn(1, 256)
            svg.setDocumentWidth(outW.toFloat())
            svg.setDocumentHeight(outH.toFloat())
            val bmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
            svg.renderToCanvas(Canvas(bmp))
            bmp
        } catch (_: Exception) {
            null
        }
    }
}

/**
 * A tool's brand icon, loaded straight from assets/icons as SVG and tinted
 * with the vendor color (the desktop's row-icon treatment). Tools without an
 * SVG fall back to a small colored letter tile.
 */
@Composable
fun ToolIcon(
    client: String?,
    modifier: Modifier = Modifier,
    size: Dp = 18.dp
) {
    val context = LocalContext.current
    val bmp = remember(client) { toolIconBitmap(context, client) }
    val color = vendorColor(client)
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            colorFilter = ColorFilter.tint(color, BlendMode.SrcIn),
            modifier = modifier.size(size)
        )
    } else {
        val letter = (client ?: "?").take(1).uppercase(Locale.getDefault())
        Box(
            modifier
                .size(size)
                .clip(RoundedCornerShape(size / 3f))
                .background(color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                letter,
                fontSize = (size.value * 0.5f).sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}
