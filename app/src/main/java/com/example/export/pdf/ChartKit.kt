package com.example.export.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.example.export.CategoryStatRow
import com.example.export.DailyActivityPoint
import com.example.export.DrillStatRow
import com.example.export.PunchCardCell
import java.util.Locale
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object ChartKit {

    // Clean, high-contrast print-friendly palette
    val COLOR_PRIMARY = Color.rgb(37, 99, 235)      // Royal Blue
    val COLOR_SUCCESS = Color.rgb(22, 163, 74)      // Emerald Green
    val COLOR_WARNING = Color.rgb(217, 119, 6)      // Amber
    val COLOR_DANGER = Color.rgb(220, 38, 38)       // Crimson
    val COLOR_PURPLE = Color.rgb(124, 58, 237)      // Violet
    val COLOR_TEAL = Color.rgb(13, 148, 136)        // Teal
    val COLOR_SLATE = Color.rgb(71, 85, 105)        // Slate
    val COLOR_TEXT_MAIN = Color.rgb(15, 23, 42)     // Deep Ink
    val COLOR_TEXT_MUTED = Color.rgb(100, 116, 139) // Muted Gray
    val COLOR_CARD_BG = Color.rgb(248, 250, 252)    // Light Surface
    val COLOR_BORDER = Color.rgb(226, 232, 240)     // Subtle Border

    val CATEGORY_COLORS = intArrayOf(
        COLOR_PRIMARY,
        COLOR_PURPLE,
        COLOR_TEAL,
        COLOR_WARNING,
        COLOR_SUCCESS,
        Color.rgb(225, 29, 72),
        COLOR_SLATE
    )

    private fun paint(
        color: Int = COLOR_TEXT_MAIN,
        style: Paint.Style = Paint.Style.FILL,
        strokeWidth: Float = 1f,
        textSize: Float = 9f,
        bold: Boolean = false,
        mono: Boolean = false
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.style = style
        this.strokeWidth = strokeWidth
        this.textSize = textSize
        this.typeface = when {
            mono && bold -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            mono -> Typeface.MONOSPACE
            bold -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            else -> Typeface.SANS_SERIF
        }
    }

    /**
     * Draws a 26-week (182-day) GitHub-style calendar heatmap inside [bounds].
     */
    fun drawCalendarHeatmap(
        canvas: Canvas,
        bounds: RectF,
        days: List<DailyActivityPoint>,
        dailyGoal: Int
    ) {
        val cardPaint = paint(color = COLOR_CARD_BG)
        val borderPaint = paint(color = COLOR_BORDER, style = Paint.Style.STROKE, strokeWidth = 1f)
        canvas.drawRoundRect(bounds, 8f, 8f, cardPaint)
        canvas.drawRoundRect(bounds, 8f, 8f, borderPaint)

        val titlePaint = paint(color = COLOR_TEXT_MAIN, textSize = 9.5f, bold = true)
        canvas.drawText("26-WEEK PRACTICE ACTIVITY HEATMAP", bounds.left + 12f, bounds.top + 18f, titlePaint)

        val cols = 26
        val rows = 7
        val gridLeft = bounds.left + 32f
        val gridTop = bounds.top + 28f
        val availW = bounds.width() - 44f
        val availH = bounds.height() - 46f
        val cellGap = 2.5f
        val cellSize = min((availW - (cols - 1) * cellGap) / cols, (availH - (rows - 1) * cellGap) / rows)
            .coerceIn(6f, 13f)

        val labelPaint = paint(color = COLOR_TEXT_MUTED, textSize = 7f)
        val dayLabels = arrayOf("D1", "", "D3", "", "D5", "", "D7")
        for (r in 0 until rows) {
            if (dayLabels[r].isNotEmpty()) {
                val y = gridTop + r * (cellSize + cellGap) + cellSize - 1.5f
                canvas.drawText(dayLabels[r], bounds.left + 12f, y, labelPaint)
            }
        }

        val padded = if (days.size >= cols * rows) {
            days.takeLast(cols * rows)
        } else {
            List(cols * rows - days.size) {
                DailyActivityPoint("", 0, 0, 0, 0.0, 0.0, 0.0, false)
            } + days
        }

        val cellPaint = paint()
        val goal = dailyGoal.coerceAtLeast(1)
        for (c in 0 until cols) {
            for (r in 0 until rows) {
                val idx = c * rows + r
                val pt = padded.getOrNull(idx)
                val s = pt?.sessions ?: 0
                cellPaint.color = when {
                    s <= 0 -> Color.rgb(226, 232, 240)
                    s >= goal * 2 -> Color.rgb(21, 128, 61)
                    s >= goal -> Color.rgb(34, 197, 94)
                    else -> Color.rgb(134, 239, 172)
                }
                val x = gridLeft + c * (cellSize + cellGap)
                val y = gridTop + r * (cellSize + cellGap)
                canvas.drawRoundRect(RectF(x, y, x + cellSize, y + cellSize), 2f, 2f, cellPaint)
            }
        }

        // Legend at bottom right
        val legendY = bounds.bottom - 7f
        canvas.drawText("Less", bounds.right - 95f, legendY, labelPaint)
        val legendColors = intArrayOf(
            Color.rgb(226, 232, 240),
            Color.rgb(134, 239, 172),
            Color.rgb(34, 197, 94),
            Color.rgb(21, 128, 61)
        )
        for (i in legendColors.indices) {
            cellPaint.color = legendColors[i]
            val lx = bounds.right - 72f + i * 10f
            canvas.drawRoundRect(RectF(lx, legendY - 6.5f, lx + 7f, legendY + 0.5f), 1.5f, 1.5f, cellPaint)
        }
        canvas.drawText("More", bounds.right - 28f, legendY, labelPaint)
    }

    /**
     * Draws a 30-day combined bar (questions answered) + line (accuracy %) chart.
     */
    fun drawThirtyDayTrendChart(
        canvas: Canvas,
        bounds: RectF,
        days: List<DailyActivityPoint>
    ) {
        val cardPaint = paint(color = COLOR_CARD_BG)
        val borderPaint = paint(color = COLOR_BORDER, style = Paint.Style.STROKE, strokeWidth = 1f)
        canvas.drawRoundRect(bounds, 8f, 8f, cardPaint)
        canvas.drawRoundRect(bounds, 8f, 8f, borderPaint)

        val titlePaint = paint(color = COLOR_TEXT_MAIN, textSize = 9.5f, bold = true)
        canvas.drawText("LAST 30 DAYS — QUESTION VOLUME (BARS) & ACCURACY % (LINE)", bounds.left + 12f, bounds.top + 18f, titlePaint)

        val recent30 = days.takeLast(30)
        val chartLeft = bounds.left + 32f
        val chartRight = bounds.right - 28f
        val chartTop = bounds.top + 30f
        val chartBottom = bounds.bottom - 20f
        val chartW = (chartRight - chartLeft).coerceAtLeast(10f)
        val chartH = (chartBottom - chartTop).coerceAtLeast(10f)

        val gridPaint = paint(color = COLOR_BORDER, style = Paint.Style.STROKE, strokeWidth = 0.7f)
        val axisLabelPaint = paint(color = COLOR_TEXT_MUTED, textSize = 6.8f, mono = true)

        for (pct in listOf(0, 50, 100)) {
            val y = chartBottom - (pct / 100f) * chartH
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
            canvas.drawText("$pct%", bounds.left + 8f, y + 2.5f, axisLabelPaint)
        }

        if (recent30.isEmpty()) return

        val maxQ = recent30.maxOf { it.questions }.coerceAtLeast(10)
        val slotW = chartW / recent30.size.toFloat()
        val barW = (slotW * 0.58f).coerceAtLeast(2f)
        val barPaint = paint(color = Color.argb(90, 37, 99, 235))
        val linePaint = paint(color = COLOR_PRIMARY, style = Paint.Style.STROKE, strokeWidth = 1.8f)
        val dotPaint = paint(color = COLOR_PRIMARY)

        val linePath = Path()
        var pathStarted = false

        recent30.forEachIndexed { i, d ->
            val cx = chartLeft + (i + 0.5f) * slotW
            if (d.questions > 0) {
                val barH = (d.questions.toFloat() / maxQ.toFloat()) * chartH
                val top = chartBottom - barH
                canvas.drawRoundRect(
                    RectF(cx - barW / 2f, top, cx + barW / 2f, chartBottom),
                    2f, 2f, barPaint
                )

                val accY = chartBottom - (d.accuracyPct.toFloat().coerceIn(0f, 100f) / 100f) * chartH
                if (!pathStarted) {
                    linePath.moveTo(cx, accY)
                    pathStarted = true
                } else {
                    linePath.lineTo(cx, accY)
                }
                canvas.drawCircle(cx, accY, 2.2f, dotPaint)
            }
        }
        if (pathStarted) {
            canvas.drawPath(linePath, linePaint)
        }

        // Date labels at start, mid, end
        val firstDate = recent30.first().localDate.takeLast(5)
        val midDate = recent30[recent30.size / 2].localDate.takeLast(5)
        val lastDate = recent30.last().localDate.takeLast(5)
        canvas.drawText(firstDate, chartLeft, bounds.bottom - 6f, axisLabelPaint)
        canvas.drawText(midDate, chartLeft + chartW * 0.46f, bounds.bottom - 6f, axisLabelPaint)
        canvas.drawText(lastDate, chartRight - 22f, bounds.bottom - 6f, axisLabelPaint)
    }

    /**
     * Draws a Category Distribution Donut + Legend table inside [bounds].
     */
    fun drawCategoryDonutCard(
        canvas: Canvas,
        bounds: RectF,
        categories: List<CategoryStatRow>
    ) {
        val cardPaint = paint(color = COLOR_CARD_BG)
        val borderPaint = paint(color = COLOR_BORDER, style = Paint.Style.STROKE, strokeWidth = 1f)
        canvas.drawRoundRect(bounds, 8f, 8f, cardPaint)
        canvas.drawRoundRect(bounds, 8f, 8f, borderPaint)

        val titlePaint = paint(color = COLOR_TEXT_MAIN, textSize = 9.5f, bold = true)
        canvas.drawText("PRACTICE SHARE & ACCURACY BY CATEGORY", bounds.left + 12f, bounds.top + 18f, titlePaint)

        if (categories.isEmpty()) {
            val muted = paint(color = COLOR_TEXT_MUTED, textSize = 8.5f)
            canvas.drawText("No category data recorded yet.", bounds.left + 12f, bounds.top + 42f, muted)
            return
        }

        val cx = bounds.left + 68f
        val cy = bounds.top + 84f
        val radius = 40f
        val oval = RectF(cx - radius, cy - radius, cx + radius, cy + radius)
        val arcPaint = paint(style = Paint.Style.STROKE, strokeWidth = 16f)

        var startAngle = -90f
        val totalAttempts = categories.sumOf { it.attempts }.coerceAtLeast(1)
        categories.forEachIndexed { idx, cat ->
            val sweep = (cat.attempts.toFloat() / totalAttempts.toFloat()) * 360f
            arcPaint.color = CATEGORY_COLORS[idx % CATEGORY_COLORS.size]
            canvas.drawArc(oval, startAngle, max(sweep - 1.5f, 1f), false, arcPaint)
            startAngle += sweep
        }

        val centerLabel = paint(color = COLOR_TEXT_MAIN, textSize = 9f, bold = true, mono = true).apply {
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("$totalAttempts Q", cx, cy + 3f, centerLabel)

        // Legend & metrics on the right side
        val legLeft = bounds.left + 132f
        var rowY = bounds.top + 36f
        val namePaint = paint(color = COLOR_TEXT_MAIN, textSize = 8f, bold = true)
        val statPaint = paint(color = COLOR_TEXT_MUTED, textSize = 7.5f, mono = true)
        val swatchPaint = paint()

        categories.take(7).forEachIndexed { idx, cat ->
            swatchPaint.color = CATEGORY_COLORS[idx % CATEGORY_COLORS.size]
            canvas.drawRoundRect(RectF(legLeft, rowY - 6.5f, legLeft + 7f, rowY + 0.5f), 2f, 2f, swatchPaint)
            val shortName = if (cat.displayName.length > 26) cat.displayName.take(25) + "…" else cat.displayName
            canvas.drawText(shortName, legLeft + 11f, rowY, namePaint)
            val detail = String.format(
                Locale.US,
                "%dQ (%.0f%%) · Acc %.1f%% · LB %.0f%% · %.1fs",
                cat.attempts,
                cat.practiceSharePct,
                cat.accuracyPct,
                cat.wilson95LowerPct,
                cat.avgTimeMs / 1000.0
            )
            canvas.drawText(detail, bounds.right - 210f, rowY, statPaint)
            rowY += 15f
        }
    }

    /**
     * Draws horizontal accuracy bars per drill with Wilson 95% lower-bound markers.
     */
    fun drawDrillAccuracyBars(
        canvas: Canvas,
        bounds: RectF,
        drills: List<DrillStatRow>
    ) {
        val cardPaint = paint(color = COLOR_CARD_BG)
        val borderPaint = paint(color = COLOR_BORDER, style = Paint.Style.STROKE, strokeWidth = 1f)
        canvas.drawRoundRect(bounds, 8f, 8f, cardPaint)
        canvas.drawRoundRect(bounds, 8f, 8f, borderPaint)

        val titlePaint = paint(color = COLOR_TEXT_MAIN, textSize = 9.5f, bold = true)
        canvas.drawText("PER-DRILL ACCURACY (BAR) & WILSON 95% CONFIDENCE LOWER BOUND (TICK)", bounds.left + 12f, bounds.top + 18f, titlePaint)

        if (drills.isEmpty()) {
            val muted = paint(color = COLOR_TEXT_MUTED, textSize = 8.5f)
            canvas.drawText("No drill attempts recorded yet.", bounds.left + 12f, bounds.top + 42f, muted)
            return
        }

        val displayed = drills.take(16)
        val labelX = bounds.left + 12f
        val barLeft = bounds.left + 150f
        val barRight = bounds.right - 125f
        val barMaxW = (barRight - barLeft).coerceAtLeast(40f)
        var y = bounds.top + 35f
        val rowStep = ((bounds.height() - 42f) / displayed.size.coerceAtLeast(1)).coerceIn(14f, 22f)

        val namePaint = paint(color = COLOR_TEXT_MAIN, textSize = 7.8f, bold = true)
        val trackPaint = paint(color = Color.rgb(226, 232, 240))
        val fillPaint = paint()
        val tickPaint = paint(color = COLOR_TEXT_MAIN, style = Paint.Style.STROKE, strokeWidth = 1.6f)
        val metaPaint = paint(color = COLOR_TEXT_MUTED, textSize = 7.3f, mono = true)

        for (d in displayed) {
            val name = if (d.displayName.length > 23) d.displayName.take(22) + "…" else d.displayName
            canvas.drawText(name, labelX, y, namePaint)

            val barTop = y - 6.5f
            val barBottom = y + 1.5f
            canvas.drawRoundRect(RectF(barLeft, barTop, barRight, barBottom), 3f, 3f, trackPaint)

            val accFrac = (d.accuracyPct.toFloat() / 100f).coerceIn(0f, 1f)
            fillPaint.color = when {
                d.accuracyPct >= 85.0 -> COLOR_SUCCESS
                d.accuracyPct >= 65.0 -> COLOR_PRIMARY
                else -> COLOR_DANGER
            }
            canvas.drawRoundRect(
                RectF(barLeft, barTop, barLeft + barMaxW * accFrac, barBottom),
                3f, 3f, fillPaint
            )

            // Wilson 95% LB tick mark
            val lbFrac = (d.wilson95LowerPct.toFloat() / 100f).coerceIn(0f, 1f)
            val tickX = barLeft + barMaxW * lbFrac
            canvas.drawLine(tickX, barTop - 1.5f, tickX, barBottom + 1.5f, tickPaint)

            val meta = String.format(
                Locale.US,
                "%3.0f%% (LB %2.0f%%) · %3dQ · %4.1fs",
                d.accuracyPct,
                d.wilson95LowerPct,
                d.attempts,
                d.avgTimeMs / 1000.0
            )
            canvas.drawText(meta, barRight + 6f, y, metaPaint)
            y += rowStep
        }
    }

    /**
     * Draws a 7-day × 6-block (4h each) punch-card heatmap inside [bounds].
     */
    fun drawPunchCard(
        canvas: Canvas,
        bounds: RectF,
        cells: List<PunchCardCell>
    ) {
        val cardPaint = paint(color = COLOR_CARD_BG)
        val borderPaint = paint(color = COLOR_BORDER, style = Paint.Style.STROKE, strokeWidth = 1f)
        canvas.drawRoundRect(bounds, 8f, 8f, cardPaint)
        canvas.drawRoundRect(bounds, 8f, 8f, borderPaint)

        val titlePaint = paint(color = COLOR_TEXT_MAIN, textSize = 9.5f, bold = true)
        canvas.drawText("TIME-OF-DAY × DAY-OF-WEEK PRACTICE MATRIX", bounds.left + 12f, bounds.top + 18f, titlePaint)

        val dayLabels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val blockLabels = arrayOf("00–04", "04–08", "08–12", "12–16", "16–20", "20–24")

        val gridLeft = bounds.left + 40f
        val gridTop = bounds.top + 34f
        val colW = (bounds.width() - 52f) / 6f
        val rowH = (bounds.height() - 44f) / 7f

        val labelPaint = paint(color = COLOR_TEXT_MUTED, textSize = 7f, mono = true)
        for (b in 0 until 6) {
            canvas.drawText(blockLabels[b], gridLeft + b * colW + 4f, gridTop - 5f, labelPaint)
        }

        // Aggregate into (dow 1..7, block 0..5)
        val agg = mutableMapOf<Pair<Int, Int>, Int>()
        for (c in cells) {
            val block = (c.hourOfDay / 4).coerceIn(0, 5)
            val key = c.dayOfWeek to block
            agg[key] = (agg[key] ?: 0) + c.attempts
        }
        val maxCount = agg.values.maxOrNull()?.coerceAtLeast(1) ?: 1
        val cellPaint = paint()

        for (d in 1..7) {
            val y = gridTop + (d - 1) * rowH
            canvas.drawText(dayLabels[d - 1], bounds.left + 12f, y + rowH * 0.68f, labelPaint)
            for (b in 0 until 6) {
                val count = agg[d to b] ?: 0
                val x = gridLeft + b * colW
                cellPaint.color = if (count == 0) {
                    Color.rgb(226, 232, 240)
                } else {
                    val alpha = (65 + ((count.toFloat() / maxCount.toFloat()) * 190).toInt()).coerceIn(65, 255)
                    Color.argb(alpha, 37, 99, 235)
                }
                canvas.drawRoundRect(
                    RectF(x + 2f, y + 1.5f, x + colW - 2f, y + rowH - 1.5f),
                    3f, 3f, cellPaint
                )
            }
        }
    }
}
