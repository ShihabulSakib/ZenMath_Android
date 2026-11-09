package com.example.export.pdf

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument

class PdfPageSession(
    private val document: PdfDocument,
    private val reportDateStr: String
) {
    companion object {
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val MARGIN_X = 32f
        const val CONTENT_TOP = 64f
        const val CONTENT_BOTTOM = 804f
        const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN_X
    }

    var pageNumber: Int = 0
        private set

    var currentPage: PdfDocument.Page? = null
        private set

    val canvas: Canvas
        get() = currentPage!!.canvas

    var cursorY: Float = CONTENT_TOP

    fun startNewPage(sectionSubtitle: String) {
        finishCurrentPage()
        pageNumber += 1
        val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
        val page = document.startPage(info)
        currentPage = page
        cursorY = CONTENT_TOP
        drawHeaderAndFooter(page.canvas, sectionSubtitle, pageNumber)
    }

    fun ensureSpace(neededHeight: Float, sectionSubtitleIfNewPage: String) {
        if (currentPage == null || cursorY + neededHeight > CONTENT_BOTTOM) {
            startNewPage(sectionSubtitleIfNewPage)
        }
    }

    fun finishCurrentPage() {
        currentPage?.let {
            document.finishPage(it)
            currentPage = null
        }
    }

    private fun drawHeaderAndFooter(canvas: Canvas, subtitle: String, pageNum: Int) {
        // Top header bar
        val headerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(15, 23, 42)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 48f, headerBg)

        val brandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 13f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        canvas.drawText("ZEN MATH", MARGIN_X, 24f, brandPaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(148, 163, 184)
            textSize = 8.5f
            typeface = Typeface.SANS_SERIF
        }
        canvas.drawText(subtitle, MARGIN_X, 37f, subPaint)

        val rightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(203, 213, 225)
            textSize = 8f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Generated: $reportDateStr", PAGE_WIDTH - MARGIN_X, 29f, rightPaint)

        // Bottom footer
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ChartKit.COLOR_BORDER
            strokeWidth = 0.8f
        }
        canvas.drawLine(MARGIN_X, 814f, PAGE_WIDTH - MARGIN_X, 814f, linePaint)

        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ChartKit.COLOR_TEXT_MUTED
            textSize = 7.5f
            typeface = Typeface.SANS_SERIF
        }
        canvas.drawText("ZenMath Performance & Drill Reference Report", MARGIN_X, 828f, footerPaint)

        val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ChartKit.COLOR_TEXT_MUTED
            textSize = 7.5f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.RIGHT
        }
        canvas.drawText("Page $pageNum", PAGE_WIDTH - MARGIN_X, 828f, pagePaint)
    }
}

object PdfLayout {

    fun makePaint(
        color: Int = ChartKit.COLOR_TEXT_MAIN,
        textSize: Float = 9f,
        bold: Boolean = false,
        mono: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        this.textAlign = align
        this.typeface = when {
            mono && bold -> Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            mono -> Typeface.MONOSPACE
            bold -> Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            else -> Typeface.SANS_SERIF
        }
    }

    fun wrapText(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val words = text.trim().split(Regex("\\s+"))
        val lines = mutableListOf<String>()
        var current = StringBuilder()

        for (word in words) {
            val candidate = if (current.isEmpty()) word else "$current $word"
            if (paint.measureText(candidate) <= maxWidth || current.isEmpty()) {
                current = StringBuilder(candidate)
            } else {
                lines.add(current.toString())
                current = StringBuilder(word)
            }
        }
        if (current.isNotEmpty()) {
            lines.add(current.toString())
        }
        return lines
    }

    fun drawWrappedText(
        canvas: Canvas,
        text: String,
        x: Float,
        startY: Float,
        maxWidth: Float,
        paint: Paint,
        lineHeight: Float
    ): Float {
        val lines = wrapText(text, paint, maxWidth)
        var y = startY
        for (line in lines) {
            canvas.drawText(line, x, y, paint)
            y += lineHeight
        }
        return y
    }

    fun drawKpiCard(
        canvas: Canvas,
        bounds: RectF,
        label: String,
        primaryValue: String,
        secondaryText: String,
        accentColor: Int = ChartKit.COLOR_PRIMARY
    ) {
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ChartKit.COLOR_CARD_BG
            style = Paint.Style.FILL
        }
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ChartKit.COLOR_BORDER
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(bounds, 8f, 8f, bg)
        canvas.drawRoundRect(bounds, 8f, 8f, border)

        val stripe = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(
            RectF(bounds.left, bounds.top, bounds.left + 4f, bounds.bottom),
            4f, 4f, stripe
        )

        val labelPaint = makePaint(color = ChartKit.COLOR_TEXT_MUTED, textSize = 7.5f, bold = true)
        val valPaint = makePaint(color = ChartKit.COLOR_TEXT_MAIN, textSize = 15.5f, bold = true, mono = true)
        val subPaint = makePaint(color = ChartKit.COLOR_TEXT_MUTED, textSize = 7.5f)

        canvas.drawText(label.uppercase(), bounds.left + 12f, bounds.top + 16f, labelPaint)
        canvas.drawText(primaryValue, bounds.left + 12f, bounds.top + 36f, valPaint)
        canvas.drawText(secondaryText, bounds.left + 12f, bounds.top + 50f, subPaint)
    }

    fun drawSectionHeading(
        canvas: Canvas,
        title: String,
        x: Float,
        y: Float
    ): Float {
        val paint = makePaint(color = ChartKit.COLOR_TEXT_MAIN, textSize = 11f, bold = true)
        canvas.drawText(title, x, y, paint)
        val accent = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ChartKit.COLOR_PRIMARY
            strokeWidth = 2f
        }
        canvas.drawLine(x, y + 4f, x + 36f, y + 4f, accent)
        return y + 14f
    }
}
