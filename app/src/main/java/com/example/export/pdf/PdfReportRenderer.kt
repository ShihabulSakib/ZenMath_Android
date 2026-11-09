package com.example.export.pdf

import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.domain.drills.DrillCategory
import com.example.domain.drills.DrillRegistry
import com.example.export.StatsSnapshot
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.Locale

object PdfReportRenderer {

    fun renderToBytes(snapshot: StatsSnapshot): ByteArray {
        val baos = ByteArrayOutputStream(65_536)
        renderToStream(snapshot, baos)
        return baos.toByteArray()
    }

    fun renderToStream(snapshot: StatsSnapshot, outputStream: OutputStream): Int {
        val document = PdfDocument()
        try {
            val session = PdfPageSession(document, snapshot.generatedLocalDate)

            // Page 1: Executive Overview, Activity Calendar & 30-Day Trend
            renderExecutivePage(session, snapshot)

            // Page 2: Category Distribution & Per-Drill Accuracy / Latency Analytics
            renderCategoryAndDrillPage(session, snapshot)

            // Page 3: Strengths, Focus Areas, Practice Matrix & Top Mistakes / Recent Sessions
            renderDiagnosticsAndSessionsPage(session, snapshot)

            // Pages 4+: Comprehensive Drill Guide
            renderDrillGuidePages(session, snapshot)

            session.finishCurrentPage()
            document.writeTo(outputStream)
            return session.pageNumber
        } finally {
            document.close()
        }
    }

    private fun renderExecutivePage(session: PdfPageSession, snapshot: StatsSnapshot) {
        session.startNewPage("Executive Performance Summary & Activity Trends")
        val canvas = session.canvas
        val ov = snapshot.overview
        val left = PdfPageSession.MARGIN_X
        val right = PdfPageSession.PAGE_WIDTH - PdfPageSession.MARGIN_X
        val fullW = PdfPageSession.CONTENT_WIDTH

        var y = PdfLayout.drawSectionHeading(canvas, "1. EXECUTIVE PERFORMANCE OVERVIEW", left, session.cursorY + 8f)

        // 3x2 KPI Cards
        val gap = 10f
        val cardW = (fullW - 2 * gap) / 3f
        val cardH = 60f

        PdfLayout.drawKpiCard(
            canvas = canvas,
            bounds = RectF(left, y, left + cardW, y + cardH),
            label = "Total Sessions",
            primaryValue = ov.totalSessions.toString(),
            secondaryText = "Std ${ov.normalSessions} · Spd ${ov.speedRoundSessions} · Rev ${ov.mistakeReviewSessions}",
            accentColor = ChartKit.COLOR_PRIMARY
        )
        PdfLayout.drawKpiCard(
            canvas = canvas,
            bounds = RectF(left + cardW + gap, y, left + 2 * cardW + gap, y + cardH),
            label = "Overall Accuracy",
            primaryValue = String.format(Locale.US, "%.1f%%", ov.overallAccuracyPct),
            secondaryText = "Wilson 95% LB: ${String.format(Locale.US, "%.1f%%", ov.wilson95LowerPct)}",
            accentColor = ChartKit.COLOR_SUCCESS
        )
        PdfLayout.drawKpiCard(
            canvas = canvas,
            bounds = RectF(left + 2 * (cardW + gap), y, right, y + cardH),
            label = "Questions Answered",
            primaryValue = ov.totalQuestions.toString(),
            secondaryText = "${ov.totalCorrect} correct · ${ov.totalQuestions - ov.totalCorrect} missed",
            accentColor = ChartKit.COLOR_PURPLE
        )

        y += cardH + gap

        PdfLayout.drawKpiCard(
            canvas = canvas,
            bounds = RectF(left, y, left + cardW, y + cardH),
            label = "Response Latency",
            primaryValue = String.format(Locale.US, "%.2fs", ov.avgTimeMs / 1000.0),
            secondaryText = "Med ${String.format(Locale.US, "%.1fs", ov.medianTimeMs / 1000.0)} · P90 ${String.format(Locale.US, "%.1fs", ov.p90TimeMs / 1000.0)}",
            accentColor = ChartKit.COLOR_TEAL
        )
        PdfLayout.drawKpiCard(
            canvas = canvas,
            bounds = RectF(left + cardW + gap, y, left + 2 * cardW + gap, y + cardH),
            label = "Practice Streaks",
            primaryValue = "${ov.currentStreakDays}d",
            secondaryText = "Best: ${ov.bestStreakDays}d · Active: ${ov.activeDaysCount}d",
            accentColor = ChartKit.COLOR_WARNING
        )
        val totalMin = (ov.totalPracticeSeconds / 60.0).toInt()
        PdfLayout.drawKpiCard(
            canvas = canvas,
            bounds = RectF(left + 2 * (cardW + gap), y, right, y + cardH),
            label = "Active Practice Time",
            primaryValue = "${totalMin}m",
            secondaryText = "Daily Goal (${snapshot.settings.dailyGoal}/d) Hit: ${String.format(Locale.US, "%.0f%%", ov.dailyGoalHitRatePct)}",
            accentColor = ChartKit.COLOR_SLATE
        )

        y += cardH + 16f

        // 26-Week Calendar Heatmap
        val calBounds = RectF(left, y, right, y + 124f)
        ChartKit.drawCalendarHeatmap(canvas, calBounds, snapshot.dailyActivity, snapshot.settings.dailyGoal)
        y = calBounds.bottom + 14f

        // 30-Day Trend Chart
        val trendBounds = RectF(left, y, right, y + 160f)
        ChartKit.drawThirtyDayTrendChart(canvas, trendBounds, snapshot.dailyActivity)
        y = trendBounds.bottom + 18f

        // Strengths & Weaknesses Executive Callout
        y = PdfLayout.drawSectionHeading(canvas, "2. KEY DIAGNOSTIC HIGHLIGHTS", left, y)
        val colW = (fullW - 12f) / 2f
        val boxH = (PdfPageSession.CONTENT_BOTTOM - y - 4f).coerceAtLeast(110f)

        drawDiagnosticBox(
            canvas = canvas,
            bounds = RectF(left, y, left + colW, y + boxH),
            heading = "TOP STRENGTHS",
            accentColor = ChartKit.COLOR_SUCCESS,
            items = snapshot.strengths.map { "${it.title}: ${it.evidenceText}" }
        )
        drawDiagnosticBox(
            canvas = canvas,
            bounds = RectF(left + colW + 12f, y, right, y + boxH),
            heading = "PRIORITY FOCUS AREAS",
            accentColor = ChartKit.COLOR_DANGER,
            items = snapshot.weaknesses.map { "${it.title}: ${it.evidenceText} ${it.recommendationText}" }
        )
        session.cursorY = y + boxH
    }

    private fun drawDiagnosticBox(
        canvas: android.graphics.Canvas,
        bounds: RectF,
        heading: String,
        accentColor: Int,
        items: List<String>
    ) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ChartKit.COLOR_CARD_BG }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ChartKit.COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(bounds, 8f, 8f, bg)
        canvas.drawRoundRect(bounds, 8f, 8f, border)

        val headPaint = PdfLayout.makePaint(color = accentColor, textSize = 8.5f, bold = true)
        canvas.drawText(heading, bounds.left + 10f, bounds.top + 16f, headPaint)

        val bodyPaint = PdfLayout.makePaint(color = ChartKit.COLOR_TEXT_MAIN, textSize = 7.4f)
        var cy = bounds.top + 30f
        if (items.isEmpty()) {
            canvas.drawText("Complete additional sessions to populate insights.", bounds.left + 10f, cy, bodyPaint)
            return
        }
        for (item in items.take(4)) {
            if (cy > bounds.bottom - 12f) break
            cy = PdfLayout.drawWrappedText(
                canvas = canvas,
                text = "• $item",
                x = bounds.left + 10f,
                startY = cy,
                maxWidth = bounds.width() - 20f,
                paint = bodyPaint,
                lineHeight = 10f
            ) + 3f
        }
    }

    private fun renderCategoryAndDrillPage(session: PdfPageSession, snapshot: StatsSnapshot) {
        session.startNewPage("Category Distribution & Per-Drill Performance")
        val canvas = session.canvas
        val left = PdfPageSession.MARGIN_X
        val right = PdfPageSession.PAGE_WIDTH - PdfPageSession.MARGIN_X

        var y = PdfLayout.drawSectionHeading(canvas, "3. CATEGORY & DRILL PERFORMANCE BREAKDOWN", left, session.cursorY + 8f)

        val donutBounds = RectF(left, y, right, y + 150f)
        ChartKit.drawCategoryDonutCard(canvas, donutBounds, snapshot.categories)
        y = donutBounds.bottom + 14f

        val drillBarBounds = RectF(left, y, right, y + 340f)
        ChartKit.drawDrillAccuracyBars(canvas, drillBarBounds, snapshot.drills)
        y = drillBarBounds.bottom + 16f

        // Punch card + Mistake pattern summary side by side
        val halfW = (PdfPageSession.CONTENT_WIDTH - 12f) / 2f
        val bottomBoxH = (PdfPageSession.CONTENT_BOTTOM - y - 4f).coerceAtLeast(130f)
        ChartKit.drawPunchCard(
            canvas = canvas,
            bounds = RectF(left, y, left + halfW, y + bottomBoxH),
            cells = snapshot.punchCard
        )

        val patternBounds = RectF(left + halfW + 12f, y, right, y + bottomBoxH)
        val patternLines = if (snapshot.mistakePatterns.isEmpty()) {
            listOf("Zero mistakes recorded so far.")
        } else {
            snapshot.mistakePatterns.map { mp ->
                String.format(
                    Locale.US,
                    "%s: %d (%3.0f%%) — %s",
                    mp.patternType,
                    mp.count,
                    mp.sharePct,
                    mp.description
                )
            }
        }
        drawDiagnosticBox(
            canvas = canvas,
            bounds = patternBounds,
            heading = "MISTAKE PATTERN CLASSIFICATION",
            accentColor = ChartKit.COLOR_PRIMARY,
            items = patternLines
        )
        session.cursorY = y + bottomBoxH
    }

    private fun renderDiagnosticsAndSessionsPage(session: PdfPageSession, snapshot: StatsSnapshot) {
        session.startNewPage("Mistake Log & Recent Session History")
        val canvas = session.canvas
        val left = PdfPageSession.MARGIN_X
        val right = PdfPageSession.PAGE_WIDTH - PdfPageSession.MARGIN_X

        var y = PdfLayout.drawSectionHeading(canvas, "4. TOP UNRESOLVED & FREQUENT MISTAKES", left, session.cursorY + 8f)

        val headerPaint = PdfLayout.makePaint(color = ChartKit.COLOR_TEXT_MUTED, textSize = 7.5f, bold = true)
        val rowPaint = PdfLayout.makePaint(color = ChartKit.COLOR_TEXT_MAIN, textSize = 7.5f)
        val monoPaint = PdfLayout.makePaint(color = ChartKit.COLOR_TEXT_MAIN, textSize = 7.5f, mono = true)

        if (snapshot.mistakes.isEmpty()) {
            canvas.drawText("No mistakes recorded across your sessions.", left, y + 8f, rowPaint)
            y += 24f
        } else {
            canvas.drawText("STATUS", left, y, headerPaint)
            canvas.drawText("DRILL", left + 56f, y, headerPaint)
            canvas.drawText("QUESTION", left + 165f, y, headerPaint)
            canvas.drawText("EXPECTED", left + 335f, y, headerPaint)
            canvas.drawText("YOUR ANS", left + 405f, y, headerPaint)
            canvas.drawText("MISSED", left + 470f, y, headerPaint)
            y += 12f

            for (m in snapshot.mistakes.take(18)) {
                val status = if (m.isResolved) "Resolved" else "Open"
                val drillShort = if (m.drillDisplayName.length > 18) m.drillDisplayName.take(17) + "…" else m.drillDisplayName
                val qShort = if (m.questionText.length > 28) m.questionText.take(27) + "…" else m.questionText
                val cShort = if (m.correctAnswer.length > 11) m.correctAnswer.take(10) + "…" else m.correctAnswer
                val uShort = if (m.latestUserAnswer.length > 11) m.latestUserAnswer.take(10) + "…" else m.latestUserAnswer

                canvas.drawText(status, left, y, rowPaint)
                canvas.drawText(drillShort, left + 56f, y, rowPaint)
                canvas.drawText(qShort, left + 165f, y, monoPaint)
                canvas.drawText(cShort, left + 335f, y, monoPaint)
                canvas.drawText(uShort, left + 405f, y, monoPaint)
                canvas.drawText("${m.errors}/${m.attempts} (${m.errorRatePct.toInt()}%)", left + 470f, y, monoPaint)
                y += 13f
            }
            y += 10f
        }

        y = PdfLayout.drawSectionHeading(canvas, "5. RECENT SESSION LOG (INCLUDING MISTAKE REVIEWS)", left, y + 6f)

        if (snapshot.recentSessions.isEmpty()) {
            canvas.drawText("No sessions completed yet.", left, y + 8f, rowPaint)
        } else {
            canvas.drawText("DATE", left, y, headerPaint)
            canvas.drawText("DRILL / MODE", left + 68f, y, headerPaint)
            canvas.drawText("TYPE / SCOPE", left + 195f, y, headerPaint)
            canvas.drawText("SCORE", left + 375f, y, headerPaint)
            canvas.drawText("ACCURACY", left + 425f, y, headerPaint)
            canvas.drawText("AVG TIME", left + 485f, y, headerPaint)
            y += 12f

            for (s in snapshot.recentSessions.take(20)) {
                if (y > PdfPageSession.CONTENT_BOTTOM - 12f) break
                val drillShort = if (s.modeDisplayName.length > 22) s.modeDisplayName.take(21) + "…" else s.modeDisplayName
                val typeOrScope = s.scopeLabel ?: s.sessionType
                val scopeShort = if (typeOrScope.length > 30) typeOrScope.take(29) + "…" else typeOrScope

                canvas.drawText(s.localDate, left, y, monoPaint)
                canvas.drawText(drillShort, left + 68f, y, rowPaint)
                canvas.drawText(scopeShort, left + 195f, y, rowPaint)
                canvas.drawText("${s.correct}/${s.totalQuestions}", left + 375f, y, monoPaint)
                canvas.drawText(String.format(Locale.US, "%.0f%%", s.accuracyPct), left + 425f, y, monoPaint)
                canvas.drawText(String.format(Locale.US, "%.1fs", s.avgTimeMs / 1000.0), left + 485f, y, monoPaint)
                y += 13f
            }
        }
        session.cursorY = y
    }

    private fun renderDrillGuidePages(session: PdfPageSession, snapshot: StatsSnapshot) {
        session.startNewPage("Complete ZenMath Drill Reference Guide")
        val left = PdfPageSession.MARGIN_X
        val maxW = PdfPageSession.CONTENT_WIDTH

        session.cursorY = PdfLayout.drawSectionHeading(
            session.canvas,
            "6. ZEN MATH DRILL HANDBOOK — MECHANICS, FORMATS & CHECKING RULES",
            left,
            session.cursorY + 8f
        )

        val catPaint = PdfLayout.makePaint(color = ChartKit.COLOR_PRIMARY, textSize = 10f, bold = true)
        val titlePaint = PdfLayout.makePaint(color = ChartKit.COLOR_TEXT_MAIN, textSize = 9f, bold = true)
        val bodyPaint = PdfLayout.makePaint(color = ChartKit.COLOR_TEXT_MAIN, textSize = 7.6f)
        val mutedPaint = PdfLayout.makePaint(color = ChartKit.COLOR_TEXT_MUTED, textSize = 7.4f)
        val monoPaint = PdfLayout.makePaint(color = ChartKit.COLOR_TEXT_MAIN, textSize = 7.4f, mono = true)

        for (cat in DrillCategory.entries) {
            val specs = DrillRegistry.all.filter {
                it.category == cat && (!it.isLegacy || snapshot.drills.any { d -> d.mode == it.mode })
            }
            if (specs.isEmpty()) continue

            session.ensureSpace(46f, "Complete ZenMath Drill Reference Guide")
            session.cursorY += 4f
            session.canvas.drawText(cat.label.uppercase(), left, session.cursorY, catPaint)
            session.cursorY += 11f
            session.cursorY = PdfLayout.drawWrappedText(
                session.canvas, cat.description, left, session.cursorY, maxW, mutedPaint, 10f
            ) + 4f

            for (spec in specs) {
                val detailLines = PdfLayout.wrapText(spec.details, bodyPaint, maxW - 16f)
                val checkLines = PdfLayout.wrapText("Checking: ${spec.answerChecking}", mutedPaint, maxW - 16f)
                val estimatedH = 30f + (detailLines.size + checkLines.size + spec.questionFormats.size) * 10.5f

                session.ensureSpace(estimatedH.coerceAtMost(240f), "Complete ZenMath Drill Reference Guide")

                val cardTop = session.cursorY
                var y = cardTop + 12f
                val mcqTag = if (spec.supportsMcq) " [Typed + 2×2 MCQ]" else " [Typed]"
                session.canvas.drawText("${spec.title} (${spec.mode.id})$mcqTag", left + 8f, y, titlePaint)
                y += 11f

                for (dl in detailLines) {
                    session.canvas.drawText(dl, left + 8f, y, bodyPaint)
                    y += 9.8f
                }

                for (qf in spec.questionFormats) {
                    val exLine = "  • ${qf.subtypeName}: ${qf.promptPattern}  |  e.g. '${qf.examplePrompt}' → '${qf.exampleAnswer}'"
                    session.canvas.drawText(exLine, left + 8f, y, monoPaint)
                    y += 9.8f
                }

                for (cl in checkLines) {
                    session.canvas.drawText(cl, left + 8f, y, mutedPaint)
                    y += 9.5f
                }

                val cardBounds = RectF(left, cardTop, left + maxW, y + 4f)
                val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = ChartKit.COLOR_BORDER
                    style = Paint.Style.STROKE
                    strokeWidth = 0.8f
                }
                session.canvas.drawRoundRect(cardBounds, 6f, 6f, border)
                session.cursorY = cardBounds.bottom + 7f
            }
        }
    }
}
