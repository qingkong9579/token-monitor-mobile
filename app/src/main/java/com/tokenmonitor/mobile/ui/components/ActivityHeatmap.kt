package com.tokenmonitor.mobile.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokenmonitor.mobile.data.HistoryDay
import com.tokenmonitor.mobile.ui.theme.Accent
import com.tokenmonitor.mobile.ui.theme.CardBg
import com.tokenmonitor.mobile.ui.theme.TextMuted
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.ceil

private const val CELL_DP = 10f
private const val GAP_DP = 3f
private const val MONTH_LABEL_HEIGHT = 18f

/** 0..4 activity level for a day, GitHub-style (>=75% → 4 … >0 → 1). */
private fun heatIntensity(value: Long, max: Long): Int {
    if (max <= 0) return 0
    val ratio = value.toDouble() / max
    return when {
        ratio >= 0.75 -> 4
        ratio >= 0.5 -> 3
        ratio >= 0.25 -> 2
        ratio > 0 -> 1
        else -> 0
    }
}

@Composable
private fun heatColor(intensity: Int): Color = when (intensity) {
    0 -> CardBg.copy(alpha = 0.35f)
    1 -> Accent.copy(alpha = 0.28f)
    2 -> Accent.copy(alpha = 0.50f)
    3 -> Accent.copy(alpha = 0.72f)
    else -> Accent
}

private data class HeatCell(val col: Int, val row: Int, val intensity: Int)
private data class HeatGrid(
    val weeks: Int,
    val cells: List<HeatCell>,
    val monthLabels: List<Pair<Int, String>>
)

/**
 * The GitHub-style contribution grid over the history daily rows. It spans the
 * actual data range — the Sunday on or before the earliest day through the
 * latest day — so the leading partial week is padded with empty cells ("补齐")
 * and the newest week sits at the right edge.
 */
private fun buildActivityGrid(daily: List<HistoryDay>): HeatGrid {
    val byDate = daily.associateBy { it.date }
    val maxTokens = daily.maxOfOrNull { it.tokens } ?: 0L
    val minDate = daily.minOfOrNull { it.date }
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return HeatGrid(0, emptyList(), emptyList())
    val endDate = daily.maxOfOrNull { it.date }
        ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return HeatGrid(0, emptyList(), emptyList())
    // Left edge = the Sunday on or before the earliest data day.
    val start = minDate.minusDays(minDate.dayOfWeek.value % 7L)
    val cells = mutableListOf<HeatCell>()
    val monthLabels = mutableListOf<Pair<Int, String>>()
    val monthFmt = DateTimeFormatter.ofPattern("MMM")
    var d = start
    while (!d.isAfter(endDate)) {
        val days = ChronoUnit.DAYS.between(start, d).toInt()
        val col = days / 7
        val row = d.dayOfWeek.value % 7 // SUN=0 … SAT=6
        if (d.dayOfMonth == 1) monthLabels.add(col to d.format(monthFmt))
        val tokens = byDate[d.toString()]?.tokens ?: 0L
        cells.add(HeatCell(col, row, heatIntensity(tokens, maxTokens)))
        d = d.plusDays(1)
    }
    return HeatGrid(weeks = (cells.maxOfOrNull { it.col } ?: 0) + 1, cells = cells, monthLabels = monthLabels)
}

/**
 * Activity heatmap, one rendering for phones and tablets: fixed-size cells in a
 * horizontally scrollable grid, newest week at the right. It opens scrolled to
 * the latest; swiping left reveals older days back to the first record. Month
 * labels use the same size as section subtitles.
 */
@Composable
fun ActivityHeatmap(daily: List<HistoryDay>, modifier: Modifier = Modifier) {
    if (daily.isEmpty()) return
    val grid = remember(daily) { buildActivityGrid(daily) }
    val weeks = grid.weeks
    if (weeks == 0) return
    val cell = CELL_DP.dp
    val gap = GAP_DP.dp
    val pitch = cell + gap
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier) {
        // Pad the left with empty week columns so the grid always fills the
        // available width with the newest week pinned to the right edge.
        val fitColumns = ceil((maxWidth.value + gap.value) / pitch.value).toInt()
        val totalColumns = maxOf(fitColumns, weeks)
        val leading = totalColumns - weeks

        // Open showing the latest week (right edge), after the grid is laid out.
        LaunchedEffect(Unit) {
            withFrameNanos { }
            scrollState.scrollTo(scrollState.maxValue)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
        ) {
            // Grid columns, top-padded to leave room for the month-label row.
            Row(
                Modifier.padding(top = MONTH_LABEL_HEIGHT.dp),
                horizontalArrangement = Arrangement.spacedBy(gap)
            ) {
                repeat(totalColumns) { i ->
                    val dataIndex = i - leading
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        (0..6).forEach { r ->
                            val intensity = if (dataIndex >= 0) {
                                grid.cells.firstOrNull { it.col == dataIndex && it.row == r }?.intensity ?: 0
                            } else 0
                            Box(
                                Modifier
                                    .size(cell)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(heatColor(intensity))
                            )
                        }
                    }
                }
            }
            // Month labels, positioned at their column's x (col × pitch) like
            // token-monitor's SVG — natural width, never clipped or ellipsized.
            grid.monthLabels.forEach { (col, label) ->
                Text(
                    label,
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 1,
                    modifier = Modifier.offset(x = pitch * (col + leading).toFloat())
                )
            }
        }
    }
}

/** Small "less → more" legend for the heatmap. */
@Composable
fun ActivityHeatmapLegend(modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("少", fontSize = 9.sp, color = TextMuted)
        Spacer(Modifier.width(4.dp))
        (0..4).forEach { i ->
            Box(
                Modifier
                    .padding(horizontal = 1.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(heatColor(i))
            )
        }
        Spacer(Modifier.width(4.dp))
        Text("多", fontSize = 9.sp, color = TextMuted)
    }
}

/**
 * A smooth area sparkline of the last 45 daily points, mirroring token-monitor's
 * home trends module. The line is the token count; the fill is a teal wash.
 */
@Composable
fun TrendSparkline(
    daily: List<HistoryDay>,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp
) {
    val points = daily.takeLast(45)
    if (points.isEmpty()) return
    val lineColor = Accent
    val fillBrush = Brush.verticalGradient(
        listOf(Accent.copy(alpha = 0.30f), Accent.copy(alpha = 0.02f))
    )
    Canvas(modifier.fillMaxWidth().height(height)) {
        val maxVal = (points.maxOfOrNull { it.tokens } ?: 0L).coerceAtLeast(1L).toFloat()
        val n = points.size
        val xs = points.indices.map { i -> if (n > 1) size.width * i / (n - 1) else 0f }
        val ys = points.map { size.height * (1f - it.tokens.toFloat() / maxVal) }
        val line = Path()
        if (n >= 3) {
            // Catmull-Rom → cubic bezier, same smoothing as the desktop.
            line.moveTo(xs[0], ys[0])
            for (i in 0 until n - 1) {
                val p0 = Offset(xs[maxOf(0, i - 1)], ys[maxOf(0, i - 1)])
                val p1 = Offset(xs[i], ys[i])
                val p2 = Offset(xs[i + 1], ys[i + 1])
                val p3 = Offset(xs[minOf(n - 1, i + 2)], ys[minOf(n - 1, i + 2)])
                val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
                val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
                line.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
            }
        } else {
            points.forEachIndexed { i, _ -> if (i == 0) line.moveTo(xs[0], ys[0]) else line.lineTo(xs[i], ys[i]) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, fillBrush)
        drawPath(line, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}
