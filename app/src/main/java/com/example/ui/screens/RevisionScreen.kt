package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConstantEntry
import com.example.data.ConstantsData
import com.example.data.ConstantsLoader
import com.example.data.CourseValuesData
import com.example.data.MemoryTableCategory
import com.example.data.PhSpeciesData
import com.example.ui.components.RichTextHelper
import com.example.ui.theme.ZenTheme
import java.util.Locale

enum class RevisionTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TABLES("Tables", Icons.Default.Grid3x3),
    SQUARES("Powers", Icons.Default.Tag),
    FRACTIONS("Fractions", Icons.Default.Percent),
    MEMORY("Course Values", Icons.Default.Functions),
    PH("pH & Logs", Icons.Default.Science),
    CONSTANTS("Constants", Icons.Default.MenuBook)
}

@Composable
fun RevisionScreen(
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    var activeTab by remember { mutableStateOf(RevisionTab.TABLES) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .testTag("revision_screen")
    ) {
        // Sticky Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Revision",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Scrollable pill selector for all 6 revision tabs
            val tabShape = RoundedCornerShape(14.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(tabShape)
                    .background(colors.card.copy(alpha = 0.6f))
                    .border(1.dp, colors.cardBorder, tabShape)
                    .horizontalScroll(rememberScrollState())
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RevisionTab.entries.forEach { tab ->
                    val isSelected = tab == activeTab
                    val pillShape = RoundedCornerShape(10.dp)

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .clip(pillShape)
                            .background(if (isSelected) colors.primary else Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.primary.copy(alpha = 0.2f)),
                                onClick = { activeTab = tab }
                            )
                            .padding(horizontal = 12.dp)
                            .testTag("revision_tab_${tab.name.lowercase()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = if (isSelected) colors.onPrimary else colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = tab.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) colors.onPrimary else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            when (activeTab) {
                RevisionTab.TABLES -> TablesTab()
                RevisionTab.SQUARES -> SquaresAndCubesTab()
                RevisionTab.FRACTIONS -> FractionsTab()
                RevisionTab.MEMORY -> MemoryTablesTab()
                RevisionTab.PH -> PhRevisionTab()
                RevisionTab.CONSTANTS -> ConstantsRevisionTab()
            }
        }
    }
}

@Composable
private fun TablesTab() {
    val colors = ZenTheme.colors
    var openTable by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(20) { idx ->
            val n = idx + 1
            val isOpen = openTable == n
            val cardShape = RoundedCornerShape(16.dp)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, cardShape)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openTable = if (isOpen) null else n }
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$n × Table",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(if (isOpen) 180f else 0f)
                        )
                    }

                    AnimatedVisibility(visible = isOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (m in 1..12) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = n.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textMain,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "×",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Text(
                                        text = m.toString(),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textMain,
                                        modifier = Modifier.width(36.dp),
                                        textAlign = TextAlign.End
                                    )
                                    Text(
                                        text = "=",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Text(
                                        text = (n * m).toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun SquaresAndCubesTab() {
    val colors = ZenTheme.colors
    var mode by remember { mutableStateOf("squares") } // "squares", "cubes", "powers"

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Pair("squares", "Squares (1–25)"),
                Pair("cubes", "Cubes (1–15)"),
                Pair("powers", "2ⁿ / 3ⁿ / 5ⁿ")
            ).forEach { (key, label) ->
                val selected = mode == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) colors.primary.copy(alpha = 0.16f) else colors.card)
                        .border(1.dp, if (selected) colors.primary else colors.cardBorder, RoundedCornerShape(10.dp))
                        .clickable { mode = key },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) colors.primary else colors.textSecondary
                    )
                }
            }
        }

        if (mode == "squares") {
            // (10 + b)^2 mental expansion tip card
            val cardShape = RoundedCornerShape(14.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .clip(cardShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, cardShape)
                    .padding(12.dp)
            ) {
                Text(
                    text = RichTextHelper.formatMathText("Tip for 11–19: (10 + b)^2 = 100 + 20b + b^2  (e.g. 14^2 = 100 + 80 + 16 = 196)"),
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(25) { idx ->
                    val n = idx + 1
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, shape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = RichTextHelper.formatMathText("$n^2"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RichTextHelper.MathFontFamily,
                                color = colors.textMain
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "= ${n * n}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.primary
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        } else if (mode == "cubes") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(15) { idx ->
                    val n = idx + 1
                    val cube = n * n * n
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, shape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = RichTextHelper.formatMathText("$n^3"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RichTextHelper.MathFontFamily,
                                color = colors.textMain
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "= $cube",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.primary
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        } else {
            val powerEntries = remember {
                buildList {
                    for (e in 1..10) add(Pair("2^$e", (1 shl e).toString()))
                    for (e in 1..6) {
                        var v = 1
                        repeat(e) { v *= 3 }
                        add(Pair("3^$e", v.toString()))
                    }
                    for (e in 1..4) {
                        var v = 1
                        repeat(e) { v *= 5 }
                        add(Pair("5^$e", v.toString()))
                    }
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(powerEntries.size) { idx ->
                    val (expr, ans) = powerEntries[idx]
                    val shape = RoundedCornerShape(16.dp)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(shape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, shape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = RichTextHelper.formatMathText(expr),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = RichTextHelper.MathFontFamily,
                                color = colors.textMain
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "= $ans",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.primary
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(84.dp)) }
            }
        }
    }
}

private val COMMON_FRACTIONS = listOf(
    Pair("1/2", "0.5"),
    Pair("1/3", "0.3333"),
    Pair("1/4", "0.25"),
    Pair("1/5", "0.2"),
    Pair("1/6", "0.1667"),
    Pair("1/7", "0.1429"),
    Pair("1/8", "0.125"),
    Pair("1/9", "0.1111"),
    Pair("1/10", "0.1"),
    Pair("2/3", "0.6667"),
    Pair("2/5", "0.4"),
    Pair("2/7", "0.2857"),
    Pair("2/9", "0.2222"),
    Pair("3/4", "0.75"),
    Pair("3/5", "0.6"),
    Pair("3/7", "0.4286"),
    Pair("3/8", "0.375"),
    Pair("3/10", "0.3"),
    Pair("4/5", "0.8"),
    Pair("4/7", "0.5714"),
    Pair("4/9", "0.4444"),
    Pair("5/6", "0.8333"),
    Pair("5/7", "0.7143"),
    Pair("5/8", "0.625"),
    Pair("5/9", "0.5556"),
    Pair("6/7", "0.8571"),
    Pair("7/8", "0.875"),
    Pair("7/9", "0.7778"),
    Pair("7/10", "0.7"),
    Pair("8/9", "0.8889"),
    Pair("9/10", "0.9")
)

@Composable
private fun FractionsTab() {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(20.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, cardShape)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = colors.cardBorder,
                                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FRACTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = colors.textSecondary.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "DECIMAL",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = colors.textSecondary.copy(alpha = 0.6f)
                        )
                    }

                    // Fraction items
                    COMMON_FRACTIONS.forEachIndexed { i, (frac, dec) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (i % 2 == 1) colors.surface.copy(alpha = 0.35f) else androidx.compose.ui.graphics.Color.Transparent)
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = frac,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textMain
                            )
                            Text(
                                text = dec,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(84.dp))
        }
    }
}

@Composable
private fun MemoryTablesTab() {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(20.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        MemoryTableCategory.entries.forEach { category ->
            val entries = CourseValuesData.byCategory(category)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = category.displayName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = colors.textSecondary.copy(alpha = 0.7f)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "EXPRESSION",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.textSecondary.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "COURSE VALUE (EXACT)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.textSecondary.copy(alpha = 0.6f)
                                )
                            }

                            entries.forEachIndexed { idx, entry ->
                                val courseStr = String.format(Locale.US, "%.4f", entry.courseValue).trimEnd('0').trimEnd('.')
                                val exactStr = String.format(Locale.US, "%.4f", entry.trueValue)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (idx % 2 == 1) colors.surface.copy(alpha = 0.35f) else Color.Transparent)
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = RichTextHelper.formatMathText(entry.forwardPrompt),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = RichTextHelper.MathFontFamily,
                                        color = colors.textMain
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = courseStr,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.primary
                                        )
                                        Text(
                                            text = "($exactStr)",
                                            fontSize = 12.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.textSecondary.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(84.dp)) }
    }
}

@Composable
private fun PhRevisionTab() {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(18.dp)

    val workedExamples = remember {
        listOf(
            Triple(
                "1. Strong Acid / Base Concentration → pH",
                "Formula: pH = −log₁₀[H^+], pOH = −log₁₀[OH^-], 1 mM = 10^−3 M, diprotic ×2",
                "Ex: 5 mM H₂SO₄ → [H^+] = 2 × 5×10^−3 = 10^−2 M → pH = 2"
            ),
            Triple(
                "2. Scientific [H^+] → pH (Course Rounding Method)",
                "Formula: For [H^+] = m × 10^−n M, round m to integer 1..9, then pH = n − log₁₀(m)",
                "Ex: [H^+] = 3.1×10^−4 M → round m ≈ 3 → pH = 4 − log₁₀(3) = 4 − 0.5 = 3.5"
            ),
            Triple(
                "3. Reverse: pH → [H^+]",
                "Formula: Write pH = n − log₁₀(m) using memorized log table → [H^+] = m × 10^−n M",
                "Ex: pH = 3.7 = 4 − 0.3 = 4 − log₁₀(2) → [H^+] = 2×10^−4 M"
            ),
            Triple(
                "4. pH ↔ pOH Conversion (25 °C)",
                "Formula: pH + pOH = 14",
                "Ex: If pOH = 2.3, then pH = 14 − 2.3 = 11.7"
            ),
            Triple(
                "5. % w/v of Strong Base → pH",
                "Formula: Molarity c = (10 × %w/v) ÷ M → find [OH^-] → pOH → pH = 14 − pOH",
                "Ex: 0.4% w/v NaOH (M=40) → c = (10×0.4)/40 = 0.1 M → pOH = 1 → pH = 13"
            )
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "FORMULAS & WORKED EXAMPLES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = colors.textSecondary.copy(alpha = 0.7f)
            )
        }

        items(workedExamples) { (title, formula, example) ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, cardShape)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = RichTextHelper.formatMathText(title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = RichTextHelper.formatMathText(formula),
                        fontSize = 13.sp,
                        fontFamily = RichTextHelper.MathFontFamily,
                        color = colors.primary
                    )
                    Text(
                        text = RichTextHelper.formatMathText(example),
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "STRONG ACIDS & BASES (SPECIES DATA)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.5.sp,
                color = colors.textSecondary.copy(alpha = 0.7f)
            )
        }

        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, cardShape)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    PhSpeciesData.allSpecies.forEachIndexed { idx, sp ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx % 2 == 1) colors.surface.copy(alpha = 0.35f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = RichTextHelper.formatMathText("${sp.formula} (${sp.name})"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "${if (sp.isAcid) "Strong Acid" else "Strong Base"} • n = ${sp.valence}",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Text(
                                text = "M = ${sp.molarMass} g/mol",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = colors.primary
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(84.dp)) }
    }
}

@Composable
private fun ConstantsRevisionTab() {
    val colors = ZenTheme.colors
    val allEntries = remember { ConstantsLoader.validateAndLoad(ConstantsData.entries, failLoudly = false) }
    val subjects = remember(allEntries) { listOf("All") + ConstantsLoader.subjects(allEntries) }
    var selectedSubject by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val filteredEntries = remember(allEntries, selectedSubject, searchQuery) {
        allEntries.filter { entry ->
            val matchesSubject = selectedSubject == "All" || entry.subject.displayName == selectedSubject || entry.subject.id == selectedSubject
            val q = searchQuery.trim().lowercase()
            val matchesSearch = q.isEmpty() ||
                entry.name.lowercase().contains(q) ||
                entry.symbol.toPlainString().lowercase().contains(q) ||
                entry.subject.displayName.lowercase().contains(q)
            matchesSubject && matchesSearch
        }
    }

    val grouped = remember(filteredEntries) { filteredEntries.groupBy { it.subject.displayName } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("constants_revision_tab")
    ) {
        // Search box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search constant name, symbol, or subject...", fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = colors.textSecondary
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.cardBorder,
                focusedTextColor = colors.textMain,
                unfocusedTextColor = colors.textMain
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .testTag("constants_search_box")
        )

        // Subject filter chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            subjects.forEach { subj ->
                val isSelected = selectedSubject == subj
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primary else colors.card)
                        .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedSubject = subj }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subj,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            grouped.forEach { (subjectName, entriesInSubject) ->
                item {
                    Text(
                        text = subjectName.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.5.sp,
                        color = colors.textSecondary.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(entriesInSubject, key = { it.id }) { entry ->
                    ConstantCard(
                        entry = entry,
                        allEntries = allEntries,
                        isExpanded = expandedId == entry.id,
                        onToggleExpand = {
                            expandedId = if (expandedId == entry.id) null else entry.id
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }
    }
}

@Composable
private fun ConstantCard(
    entry: ConstantEntry,
    allEntries: List<ConstantEntry>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(18.dp)
    val valStr = if (entry.exponent == 0) {
        String.format(Locale.US, "%.4f", entry.mantissa).trimEnd('0').trimEnd('.')
    } else {
        "${String.format(Locale.US, "%.4f", entry.mantissa).trimEnd('0').trimEnd('.')}×10^${entry.exponent}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, cardShape)
            .clickable { onToggleExpand() }
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Large symbol badge
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = RichTextHelper.formatSymbol(entry.symbol),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RichTextHelper.MathFontFamily,
                            color = colors.primary
                        )
                    }

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = entry.name,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            if (entry.isSample) {
                                Text(
                                    text = "SAMPLE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Text(
                                text = RichTextHelper.formatMathText(valStr),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = RichTextHelper.MathFontFamily,
                                color = colors.primary
                            )
                            Text(
                                text = RichTextHelper.formatUnit(entry.unit),
                                fontSize = 13.sp,
                                fontFamily = RichTextHelper.MathFontFamily,
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(20.dp)
                        .rotate(if (isExpanded) 180f else 0f)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entry.notes?.let { note ->
                        Text(
                            text = note,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    if (entry.alternateValues.isNotEmpty()) {
                        entry.alternateValues.forEach { alt ->
                            val altStr = if (alt.exponent == 0) {
                                String.format(Locale.US, "%.4f", alt.mantissa).trimEnd('0').trimEnd('.')
                            } else {
                                "${String.format(Locale.US, "%.4f", alt.mantissa).trimEnd('0').trimEnd('.')}×10^${alt.exponent}"
                            }
                            Text(
                                text = RichTextHelper.formatMathText("Alternate (${alt.label ?: "alt"}): $altStr ${alt.unit.toPlainString()}"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary
                            )
                        }
                    }

                    if (entry.confusableWith.isNotEmpty()) {
                        val confusables = entry.confusableWith.mapNotNull { cid ->
                            allEntries.find { it.id == cid }
                        }
                        if (confusables.isNotEmpty()) {
                            val desc = confusables.joinToString("; ") { c ->
                                "${c.name} (${c.symbol.toPlainString()}, ${c.subject.displayName})"
                            }
                            Text(
                                text = "Do not confuse with: $desc",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
            }
        }
    }
}
