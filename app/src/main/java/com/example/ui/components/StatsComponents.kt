package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.StackedLineChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SessionEntity
import com.example.domain.DayBucket
import com.example.domain.RecentPerformanceDay
import com.example.ui.theme.ZenTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun HorizonWaveChart(
    buckets: List<DayBucket>,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    var activeIndex by remember { mutableStateOf<Int?>(null) }

    val totalQ = remember(buckets) { buckets.sumOf { it.questions } }
    val maxQ = remember(buckets) { maxOf(buckets.maxOfOrNull { it.questions } ?: 1, 1) }
    val peakDay = remember(buckets) { buckets.maxByOrNull { it.questions } }
    val avgDaily = remember(buckets) { if (buckets.isNotEmpty()) totalQ / buckets.size else 0 }

    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, cardShape)
            .padding(20.dp)
            .testTag("horizon_wave_chart")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SESSION FLOW",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = colors.textSecondary.copy(alpha = 0.7f)
                )

                Icon(
                    imageVector = Icons.Default.StackedLineChart,
                    contentDescription = null,
                    tint = colors.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Summary stats under title
            if (totalQ > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Peak: ${peakDay?.questions ?: 0}Q",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = colors.textSecondary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Total: ${totalQ}Q",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = colors.textSecondary.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Avg: $avgDaily/day",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (totalQ == 0) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = colors.textSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Complete sessions to see your activity wave",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary.copy(alpha = 0.6f)
                    )
                }
            } else {
                // Interactive Tooltip banner if dot tapped
                val selectedBucket = activeIndex?.let { buckets.getOrNull(it) }
                AnimatedVisibility(visible = selectedBucket != null) {
                    if (selectedBucket != null) {
                        val pillShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clip(pillShape)
                                .background(colors.surface)
                                .border(1.dp, colors.cardBorder, pillShape)
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${selectedBucket.label} (${selectedBucket.date})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textMain
                                    )
                                    Text(
                                        text = "${selectedBucket.sessions} sessions",
                                        fontSize = 10.sp,
                                        color = colors.textSecondary
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${selectedBucket.questions} Qs",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.primary
                                        )
                                        Text(
                                            text = "${selectedBucket.accuracy}% acc",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (selectedBucket.accuracy >= 80) colors.correct else colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Wave Area Chart Canvas
                val primaryColor = colors.primary
                val cardBorderColor = colors.cardBorder

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .pointerInput(buckets) {
                            detectTapGestures { offset ->
                                val w = size.width
                                val stepX = w / (buckets.size - 1).coerceAtLeast(1)
                                val tappedIndex = ((offset.x + stepX / 2f) / stepX).toInt().coerceIn(0, buckets.size - 1)
                                activeIndex = if (activeIndex == tappedIndex) null else tappedIndex
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val padTop = 14.dp.toPx()
                        val padBottom = 22.dp.toPx()
                        val usableHeight = height - padTop - padBottom
                        val n = buckets.size

                        if (n < 2) return@Canvas

                        // Horizontal subtle grid lines
                        val gridFractions = listOf(0.25f, 0.5f, 0.75f)
                        gridFractions.forEach { frac ->
                            val y = padTop + usableHeight * (1f - frac)
                            drawLine(
                                color = cardBorderColor.copy(alpha = 0.5f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1.dp.toPx()
                            )
                        }

                        // Compute points
                        val points = buckets.mapIndexed { idx, bucket ->
                            val x = (idx.toFloat() / (n - 1).toFloat()) * width
                            val y = padTop + usableHeight - (bucket.questions.toFloat() / maxQ.toFloat()) * usableHeight
                            Offset(x, y)
                        }

                        // Build smooth cubic bezier curve
                        val strokePath = Path().apply {
                            moveTo(points[0].x, points[0].y)
                            for (i in 0 until points.size - 1) {
                                val p0 = points[i]
                                val p1 = points[i + 1]
                                val controlX1 = p0.x + (p1.x - p0.x) / 2f
                                val controlY1 = p0.y
                                val controlX2 = p0.x + (p1.x - p0.x) / 2f
                                val controlY2 = p1.y
                                cubicTo(controlX1, controlY1, controlX2, controlY2, p1.x, p1.y)
                            }
                        }

                        // Build filled area
                        val areaPath = Path().apply {
                            addPath(strokePath)
                            lineTo(points.last().x, padTop + usableHeight)
                            lineTo(points.first().x, padTop + usableHeight)
                            close()
                        }

                        // Draw area gradient
                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.28f),
                                    primaryColor.copy(alpha = 0.08f),
                                    primaryColor.copy(alpha = 0.0f)
                                ),
                                startY = padTop,
                                endY = padTop + usableHeight
                            )
                        )

                        // Draw smooth line
                        drawPath(
                            path = strokePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Draw interactive dots
                        points.forEachIndexed { idx, pt ->
                            val isSelected = activeIndex == idx
                            val radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx()

                            drawCircle(
                                color = if (isSelected) primaryColor else colors.card,
                                radius = radius,
                                center = pt
                            )
                            drawCircle(
                                color = primaryColor,
                                radius = radius,
                                center = pt,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                        }
                    }
                }

                // X-Axis Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    buckets.forEachIndexed { idx, bucket ->
                        val isSelected = activeIndex == idx
                        Text(
                            text = bucket.label,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

enum class ChartMetric {
    QUESTIONS,
    SESSIONS
}

@Composable
fun SevenDayQuestionsChart(
    performanceDays: List<RecentPerformanceDay>,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    var metricMode by remember { mutableStateOf(ChartMetric.QUESTIONS) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    val maxVal = remember(performanceDays, metricMode) {
        val maxFromData = if (metricMode == ChartMetric.QUESTIONS) {
            performanceDays.maxOfOrNull { it.count } ?: 1
        } else {
            performanceDays.maxOfOrNull { it.sessionsCount } ?: 1
        }
        maxOf(maxFromData, 1)
    }

    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, cardShape)
            .padding(20.dp)
            .testTag("seven_day_questions_chart")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with Metric Switcher Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LAST 7 DAYS ACTIVITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = colors.textSecondary.copy(alpha = 0.7f)
                )

                // Toggle between Questions and Sessions
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.cardBorder, RoundedCornerShape(10.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (metricMode == ChartMetric.QUESTIONS) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { metricMode = ChartMetric.QUESTIONS }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Questions",
                            fontSize = 10.sp,
                            fontWeight = if (metricMode == ChartMetric.QUESTIONS) FontWeight.Bold else FontWeight.Medium,
                            color = if (metricMode == ChartMetric.QUESTIONS) colors.primary else colors.textSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (metricMode == ChartMetric.SESSIONS) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { metricMode = ChartMetric.SESSIONS }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Sessions",
                            fontSize = 10.sp,
                            fontWeight = if (metricMode == ChartMetric.SESSIONS) FontWeight.Bold else FontWeight.Medium,
                            color = if (metricMode == ChartMetric.SESSIONS) colors.primary else colors.textSecondary
                        )
                    }
                }
            }

            // Interactive Day Details Tooltip Banner if a bar is tapped
            val selectedDay = selectedDayIndex?.let { performanceDays.getOrNull(it) }
            AnimatedVisibility(visible = selectedDay != null) {
                if (selectedDay != null) {
                    val pillShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .clip(pillShape)
                            .background(colors.surface)
                            .border(1.dp, colors.cardBorder, pillShape)
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                val fullDate = try {
                                    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDay.date)
                                    SimpleDateFormat("EEEE, MMM d", Locale.US).format(parsed ?: Date())
                                } catch (e: Exception) {
                                    selectedDay.date
                                }
                                Text(
                                    text = fullDate,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "${selectedDay.sessionsCount} session${if (selectedDay.sessionsCount == 1) "" else "s"}",
                                    fontSize = 10.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "${selectedDay.count} Qs faced",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.primary
                                    )
                                    Text(
                                        text = "${selectedDay.accuracy.toInt()}% accuracy",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedDay.accuracy >= 80) colors.correct else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Bars container with completely fixed height structure that cannot clip or break
            val barMaxHeightDp = 76.dp

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                performanceDays.forEachIndexed { index, day ->
                    val value = if (metricMode == ChartMetric.QUESTIONS) day.count else day.sessionsCount
                    val fraction = if (maxVal > 0 && value > 0) (value.toFloat() / maxVal.toFloat()).coerceIn(0.12f, 1f) else 0f
                    val isSelected = selectedDayIndex == index

                    val weekday = try {
                        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(day.date)
                        SimpleDateFormat("EEE", Locale.US).format(parsed ?: Date())
                    } catch (e: Exception) {
                        day.date.takeLast(2)
                    }

                    val isToday = try {
                        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                        day.date == todayStr
                    } catch (e: Exception) {
                        false
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedDayIndex = if (selectedDayIndex == index) null else index
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Value Number
                        Text(
                            text = if (value > 0) "$value" else "0",
                            fontSize = 10.sp,
                            fontWeight = if (value > 0 || isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontFamily = FontFamily.Monospace,
                            color = if (isSelected) colors.primary else if (value > 0) colors.primary.copy(alpha = 0.9f) else colors.textSecondary.copy(alpha = 0.4f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Fixed Height Bar Track Area
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barMaxHeightDp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Faint Background Track slot
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(barMaxHeightDp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.primary.copy(alpha = if (isSelected) 0.12f else 0.04f))
                            )

                            // Active Fill Bar
                            if (fraction > 0f) {
                                val currentBarHeight = barMaxHeightDp * fraction
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(currentBarHeight.coerceAtLeast(6.dp))
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    colors.primary,
                                                    colors.primary.copy(alpha = if (isSelected) 0.85f else 0.55f)
                                                )
                                            )
                                        )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // Weekday Label
                        Text(
                            text = weekday,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isToday || isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.7f)
                        )

                        // Subtle indicator dot for today
                        if (isToday) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 2.dp)
                                    .size(3.dp)
                                    .clip(CircleShape)
                                    .background(colors.primary)
                            )
                        } else {
                            Spacer(modifier = Modifier.height(5.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityCalendar(
    sessions: List<SessionEntity>,
    goal: Int,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    var monthOffset by remember { mutableIntStateOf(0) }

    // Map sessions by date
    val activityByDate = remember(sessions) {
        sessions.groupBy { it.date.split("T").firstOrNull() ?: "" }
            .mapValues { it.value.size }
    }

    val cal = Calendar.getInstance().apply {
        add(Calendar.MONTH, -monthOffset)
        set(Calendar.DAY_OF_MONTH, 1)
    }
    val year = cal.get(Calendar.YEAR)
    val month = cal.get(Calendar.MONTH)
    val monthTitle = SimpleDateFormat("MMM yyyy", Locale.US).format(cal.time)

    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0 = Sunday
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(20.dp))
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MONTHLY ACTIVITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = colors.textSecondary.copy(alpha = 0.6f)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (monthOffset < 2) monthOffset++ },
                        enabled = monthOffset < 2,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous Month",
                            tint = colors.textSecondary
                        )
                    }

                    IconButton(
                        onClick = { if (monthOffset > 0) monthOffset-- },
                        enabled = monthOffset > 0,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next Month",
                            tint = colors.textSecondary
                        )
                    }
                }
            }

            Text(
                text = monthTitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Day labels S M T W T F S
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(
                        text = day,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }

            // Calendar cells in rows of 7
            val totalCells = firstDayOfWeek + daysInMonth
            val totalRows = (totalCells + 6) / 7

            for (row in 0 until totalRows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOfWeek + 1

                        if (dayNumber in 1..daysInMonth) {
                            val dateStr = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayNumber)
                            val count = activityByDate[dateStr] ?: 0
                            val isToday = dateStr == todayStr

                            val cellBg = when {
                                count >= goal -> colors.correct
                                count > 0 -> colors.primary.copy(alpha = 0.25f)
                                else -> colors.surface
                            }
                            val cellText = when {
                                count >= goal -> Color.Black
                                count > 0 -> colors.textMain
                                else -> colors.textSecondary.copy(alpha = 0.4f)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cellBg)
                                    .then(
                                        if (isToday) Modifier.border(1.dp, colors.primary, RoundedCornerShape(4.dp))
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = if (isToday || count > 0) FontWeight.Bold else FontWeight.Normal,
                                    color = cellText
                                )
                            }
                        } else {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendItem(color = colors.surface, label = "None", textColor = colors.textSecondary)
                LegendItem(color = colors.primary.copy(alpha = 0.25f), label = "Practiced", textColor = colors.textSecondary)
                LegendItem(color = colors.correct, label = "Met goal", textColor = colors.textSecondary)
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, textColor: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Text(text = label, fontSize = 9.sp, color = textColor)
    }
}
