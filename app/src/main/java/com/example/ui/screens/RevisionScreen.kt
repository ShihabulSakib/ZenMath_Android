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
import com.example.data.AtomicMassEntry
import com.example.data.ChapterRef
import com.example.data.ConstantEntry
import com.example.data.ConstantsData
import com.example.data.ConstantsDataset
import com.example.data.ConstantsLoader
import com.example.data.CourseValuesData
import com.example.data.DerivedFormulaEntry
import com.example.data.HscPaper
import com.example.data.MathConstantEntry
import com.example.data.MemoryTableCategory
import com.example.data.NotFixedTableEntry
import com.example.data.PhSpeciesData
import com.example.data.ReliabilityClass
import com.example.data.UnitConversionEntry
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

private enum class ConstantsSubTab(val label: String) {
    CONSTANTS("Constants (61)"),
    REFERENCE_VALUES("Ref Values (12)"),
    MATH_CONSTANTS("Math (4)"),
    ATOMIC_MASSES("Atomic Masses (39)"),
    UNIT_CONVERSIONS("Conversions (14)"),
    DERIVED_FORMULAS("Formulas (14)"),
    ABOUT_AND_SOURCES("About & Sources")
}

@Composable
private fun ConstantsRevisionTab() {
    val colors = ZenTheme.colors
    val dataset = remember { ConstantsLoader.getDataset(failLoudly = false) }
    val allEntriesAndRefs = remember(dataset) { dataset.constants + dataset.referenceValues }
    var activeSubTab by remember { mutableStateOf(ConstantsSubTab.CONSTANTS) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedPaper by remember { mutableStateOf<HscPaper?>(null) }
    var selectedChapterKey by remember { mutableStateOf<String?>(null) }
    var selectedReliability by remember {
        mutableStateOf(ReliabilityClass.entries.toSet())
    }
    var expandedId by remember { mutableStateOf<String?>(null) }

    val availableChapters = remember(dataset, selectedPaper) {
        if (selectedPaper == null || selectedPaper == HscPaper.GENERAL_PHYSICS) {
            emptyList()
        } else {
            dataset.syllabusChapters.filter { it.paper == selectedPaper }.sortedBy { it.chapterNumber }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("constants_revision_tab")
    ) {
        // Sub-navigation chips for the 7 sections
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ConstantsSubTab.entries.forEach { subTab ->
                val isSelected = activeSubTab == subTab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) colors.primary else colors.card)
                        .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            activeSubTab = subTab
                            expandedId = null
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                        .testTag("constants_subtab_${subTab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subTab.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                    )
                }
            }
        }

        // Search box (shown on searchable tabs)
        if (activeSubTab != ConstantsSubTab.ABOUT_AND_SOURCES) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search name, symbol, chapter (English/বাংলা), or unit...", fontSize = 13.sp) },
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
                    .padding(bottom = 8.dp)
                    .testTag("constants_search_box")
            )
        }

        when (activeSubTab) {
            ConstantsSubTab.CONSTANTS, ConstantsSubTab.REFERENCE_VALUES -> {
                val baseList = if (activeSubTab == ConstantsSubTab.CONSTANTS) {
                    dataset.constants
                } else {
                    dataset.referenceValues
                }

                // Paper filter chips: All, Physics 1st, Physics 2nd, Chemistry 1st, Chemistry 2nd, General physics
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val allSelected = selectedPaper == null
                    FilterChipBox(
                        label = "All Papers",
                        isSelected = allSelected,
                        onClick = {
                            selectedPaper = null
                            selectedChapterKey = null
                        }
                    )
                    HscPaper.entries.forEach { paper ->
                        FilterChipBox(
                            label = paper.displayName,
                            isSelected = selectedPaper == paper,
                            onClick = {
                                selectedPaper = paper
                                selectedChapterKey = null
                            }
                        )
                    }
                }

                // Chapter filter chips (with English and Bangla chapter names) when a specific paper is selected
                if (availableChapters.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChipBox(
                            label = "All Chapters",
                            isSelected = selectedChapterKey == null,
                            onClick = { selectedChapterKey = null }
                        )
                        availableChapters.forEach { ch ->
                            val chLabel = if (ch.nameBn.isNotBlank()) {
                                "Ch ${ch.chapterNumber}: ${ch.nameEn} (${ch.nameBn})"
                            } else {
                                "Ch ${ch.chapterNumber}: ${ch.nameEn}"
                            }
                            FilterChipBox(
                                label = chLabel,
                                isSelected = selectedChapterKey == ch.key,
                                onClick = {
                                    selectedChapterKey = if (selectedChapterKey == ch.key) null else ch.key
                                }
                            )
                        }
                    }
                }

                // Reliability class filters (only on Constants tab since Ref Values are all Reference & Approx)
                if (activeSubTab == ConstantsSubTab.CONSTANTS) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReliabilityClass.entries.forEach { rel ->
                            val isOn = rel in selectedReliability
                            FilterChipBox(
                                label = rel.shortLabel,
                                isSelected = isOn,
                                onClick = {
                                    selectedReliability = if (isOn && selectedReliability.size > 1) {
                                        selectedReliability - rel
                                    } else {
                                        selectedReliability + rel
                                    }
                                }
                            )
                        }
                    }
                }

                val filteredEntries = remember(
                    baseList,
                    selectedPaper,
                    selectedChapterKey,
                    selectedReliability,
                    searchQuery,
                    activeSubTab
                ) {
                    baseList.filter { entry ->
                        val matchesPaper = selectedPaper == null || entry.papers.contains(selectedPaper)
                        val matchesChapter = selectedChapterKey == null ||
                            entry.chapters.any { it.key == selectedChapterKey }
                        val matchesRel = activeSubTab == ConstantsSubTab.REFERENCE_VALUES ||
                            entry.reliabilityClass in selectedReliability
                        val q = searchQuery.trim().lowercase()
                        val matchesSearch = q.isEmpty() ||
                            entry.name.lowercase().contains(q) ||
                            entry.symbol.shortSymbol.lowercase().contains(q) ||
                            entry.symbol.fullSymbol.lowercase().contains(q) ||
                            entry.category.lowercase().contains(q) ||
                            entry.chapters.any {
                                it.nameEn.lowercase().contains(q) ||
                                    it.nameBn.lowercase().contains(q) ||
                                    it.paper.displayName.lowercase().contains(q)
                            }
                        matchesPaper && matchesChapter && matchesRel && matchesSearch
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "${filteredEntries.size} ${if (activeSubTab == ConstantsSubTab.CONSTANTS) "CONSTANTS" else "CONDITION-DEPENDENT REFERENCE VALUES"}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.4.sp,
                            color = colors.textSecondary.copy(alpha = 0.7f)
                        )
                    }

                    items(filteredEntries, key = { it.id }) { entry ->
                        ConstantCard(
                            entry = entry,
                            allEntries = allEntriesAndRefs,
                            isExpanded = expandedId == entry.id,
                            onToggleExpand = {
                                expandedId = if (expandedId == entry.id) null else entry.id
                            }
                        )
                    }

                    item {
                        DatasetAttributionFooter(dataset)
                    }
                    item { Spacer(modifier = Modifier.height(84.dp)) }
                }
            }

            ConstantsSubTab.MATH_CONSTANTS -> {
                MathConstantsSubTab(dataset = dataset, searchQuery = searchQuery)
            }

            ConstantsSubTab.ATOMIC_MASSES -> {
                AtomicMassesSubTab(dataset = dataset, searchQuery = searchQuery)
            }

            ConstantsSubTab.UNIT_CONVERSIONS -> {
                UnitConversionsSubTab(dataset = dataset, searchQuery = searchQuery)
            }

            ConstantsSubTab.DERIVED_FORMULAS -> {
                DerivedFormulasSubTab(dataset = dataset, searchQuery = searchQuery)
            }

            ConstantsSubTab.ABOUT_AND_SOURCES -> {
                AboutAndSourcesSubTab(dataset = dataset)
            }
        }
    }
}

@Composable
private fun FilterChipBox(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = ZenTheme.colors
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) colors.primary.copy(alpha = 0.18f) else colors.card)
            .border(
                1.dp,
                if (isSelected) colors.primary else colors.cardBorder,
                RoundedCornerShape(9.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) colors.primary else colors.textSecondary
        )
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
    val preciseRich = RichTextHelper.formatScientificNumber(entry.mantissa, entry.exponent)
    val courseRich = RichTextHelper.formatScientificNumber(entry.courseMantissa, entry.courseExponent)
    val firstChapter = entry.chapters.firstOrNull()

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
                    // Short symbol badge for list rows (Section 3)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = RichTextHelper.formatSymbol(entry.symbol, shortForm = true),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = RichTextHelper.MathFontFamily,
                            textAlign = TextAlign.Center,
                            color = colors.primary
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = entry.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textMain
                        )

                        // Primary value + unit
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = preciseRich,
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

                        // Chapter & reliability chips
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (firstChapter != null) {
                                val chapChip = if (firstChapter.nameBn.isNotBlank()) {
                                    "${firstChapter.paper.displayName} • ${firstChapter.nameEn} (${firstChapter.nameBn})"
                                } else {
                                    firstChapter.englishChipLabel
                                }
                                Text(
                                    text = chapChip,
                                    fontSize = 11.sp,
                                    color = colors.textSecondary.copy(alpha = 0.85f)
                                )
                            }
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
                    // Full symbol when different from short symbol (Section 3)
                    if (entry.symbol.fullSymbol != entry.symbol.shortSymbol) {
                        Text(
                            text = androidx.compose.ui.text.buildAnnotatedString {
                                append("Full symbol: ")
                                append(RichTextHelper.formatSymbol(entry.symbol, shortForm = false))
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textMain
                        )
                    }

                    // Course value & raw HSC approximation
                    Text(
                        text = androidx.compose.ui.text.buildAnnotatedString {
                            append("HSC Course value: ")
                            append(courseRich)
                            append(" ")
                            append(RichTextHelper.formatUnit(entry.unit))
                            entry.commonHscApproximationRaw?.let { rawApprox ->
                                append("  (")
                                append(RichTextHelper.formatMathText(rawApprox))
                                append(")")
                            }
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.primary
                    )

                    // Reliability class badge
                    Text(
                        text = "Classification: ${entry.reliabilityClass.displayName} (${entry.constantType})",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )

                    // Definition / meaning
                    entry.definitionOrMeaning?.let { def ->
                        Text(
                            text = RichTextHelper.formatMathText("Meaning: $def"),
                            fontSize = 12.sp,
                            color = colors.textMain
                        )
                    }

                    // Formula uses
                    if (entry.formulaUses.isNotEmpty()) {
                        Text(
                            text = RichTextHelper.formatMathText("Formulas: ${entry.formulaUses.joinToString("  •  ")}"),
                            fontSize = 12.sp,
                            fontFamily = RichTextHelper.MathFontFamily,
                            color = colors.textMain
                        )
                    }

                    // Mapped chapters with English and Bangla names
                    if (entry.chapters.isNotEmpty()) {
                        val chaptersDesc = entry.chapters.joinToString("; ") { it.bilingualLabel }
                        Text(
                            text = "Chapters: $chaptersDesc",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Alternate unit variants
                    if (entry.allAlternateValues.isNotEmpty()) {
                        entry.allAlternateValues.forEach { alt ->
                            Text(
                                text = androidx.compose.ui.text.buildAnnotatedString {
                                    append("Alternate (${alt.system}): ")
                                    append(RichTextHelper.formatScientificNumber(alt.mantissa, alt.exponent))
                                    append(" ")
                                    append(RichTextHelper.formatUnit(alt.unit))
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary
                            )
                        }
                    }

                    // Notes
                    entry.notesList.forEach { note ->
                        Text(
                            text = RichTextHelper.formatMathText("• $note"),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Confusables ("Do not confuse with")
                    if (entry.confusableIds.isNotEmpty()) {
                        val confusables = entry.confusableIds.mapNotNull { cid ->
                            allEntries.find { it.id == cid }
                        }
                        if (confusables.isNotEmpty()) {
                            Text(
                                text = androidx.compose.ui.text.buildAnnotatedString {
                                    append("Do not confuse with: ")
                                    confusables.forEachIndexed { idx, c ->
                                        if (idx > 0) append("; ")
                                        append(c.name)
                                        append(" (")
                                        append(RichTextHelper.formatSymbol(c.symbol, shortForm = true))
                                        append(", ")
                                        append(RichTextHelper.formatUnit(c.unit))
                                        append(")")
                                    }
                                },
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

@Composable
private fun MathConstantsSubTab(
    dataset: ConstantsDataset,
    searchQuery: String
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(18.dp)
    val filtered = remember(dataset.mathConstants, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) dataset.mathConstants
        else dataset.mathConstants.filter {
            it.name.lowercase().contains(q) ||
                it.symbol.shortSymbol.lowercase().contains(q) ||
                it.commonApproximations.any { a -> a.lowercase().contains(q) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filtered, key = { it.id }) { mc ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, cardShape)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.primary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = RichTextHelper.formatSymbol(mc.symbol, shortForm = true),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = RichTextHelper.MathFontFamily,
                                    color = colors.primary
                                )
                            }
                            Column {
                                Text(
                                    text = mc.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Text(
                                    text = "no unit",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Text(
                            text = String.format(Locale.US, "%.6f", mc.value).trimEnd('0').trimEnd('.'),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = colors.primary
                        )
                    }

                    if (mc.commonApproximations.isNotEmpty()) {
                        Text(
                            text = "HSC approximations: ${mc.commonApproximations.joinToString(", ")}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary
                        )
                    }
                    if (mc.relevance.isNotEmpty()) {
                        Text(
                            text = RichTextHelper.formatMathText("Uses: ${mc.relevance.joinToString(" • ")}"),
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    if (mc.chapters.isNotEmpty()) {
                        Text(
                            text = "Chapters: ${mc.chapters.joinToString("; ") { it.bilingualLabel }}",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
        item { DatasetAttributionFooter(dataset) }
        item { Spacer(modifier = Modifier.height(84.dp)) }
    }
}

@Composable
private fun AtomicMassesSubTab(
    dataset: ConstantsDataset,
    searchQuery: String
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(18.dp)
    val filtered = remember(dataset.atomicMasses, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) dataset.atomicMasses
        else dataset.atomicMasses.filter {
            it.element.lowercase().contains(q) ||
                it.symbol.lowercase().contains(q) ||
                it.relativeAtomicMassSchool.toString().contains(q)
        }
    }

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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ELEMENT (SYMBOL)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.textSecondary.copy(alpha = 0.65f)
                        )
                        Text(
                            text = "HSC MASS (g/mol) • MODERN CIAAW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = colors.textSecondary.copy(alpha = 0.65f)
                        )
                    }

                    filtered.forEachIndexed { idx, item ->
                        val schoolStr = RichTextHelper.formatCleanDouble(item.relativeAtomicMassSchool, 3)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (idx % 2 == 1) colors.surface.copy(alpha = 0.35f) else Color.Transparent)
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(colors.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = item.symbol,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            color = colors.primary
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = item.element,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textMain
                                        )
                                        if (!item.notes.isNullOrBlank()) {
                                            Text(
                                                text = item.notes,
                                                fontSize = 11.sp,
                                                color = colors.textSecondary
                                            )
                                        }
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "$schoolStr g·mol⁻¹",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.primary
                                    )
                                    Text(
                                        text = "CIAAW: ${item.modernCiaawValue}",
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { DatasetAttributionFooter(dataset) }
        item { Spacer(modifier = Modifier.height(84.dp)) }
    }
}

@Composable
private fun UnitConversionsSubTab(
    dataset: ConstantsDataset,
    searchQuery: String
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(18.dp)
    val filtered = remember(dataset.unitConversions, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) dataset.unitConversions
        else dataset.unitConversions.filter {
            it.quantity.lowercase().contains(q) ||
                it.conversionSummary.lowercase().contains(q) ||
                it.id.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filtered, key = { it.id }) { conv ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, cardShape)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = conv.quantity.uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            color = colors.primary
                        )
                        if (conv.isFormulaOnly) {
                            Text(
                                text = "FORMULA ONLY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary
                            )
                        }
                    }

                    Text(
                        text = RichTextHelper.formatMathText(conv.conversionSummary),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain
                    )

                    conv.values.forEach { item ->
                        if (item.isNumericFactor && item.fromUnit != null && item.toUnit != null && item.factor != null) {
                            val (m, e) = ConstantsLoader.decomposeToMantissaAndExponent(item.factor)
                            Text(
                                text = androidx.compose.ui.text.buildAnnotatedString {
                                    append("• 1 ")
                                    append(RichTextHelper.formatUnit(item.fromUnit))
                                    append(" = ")
                                    append(RichTextHelper.formatScientificNumber(m, e))
                                    append(" ")
                                    append(RichTextHelper.formatUnit(item.toUnit))
                                },
                                fontSize = 13.sp,
                                fontFamily = RichTextHelper.MathFontFamily,
                                color = colors.textSecondary
                            )
                        } else if (!item.formula.isNullOrBlank()) {
                            Text(
                                text = RichTextHelper.formatMathText("• ${item.formula}"),
                                fontSize = 13.sp,
                                fontFamily = RichTextHelper.MathFontFamily,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
        item { DatasetAttributionFooter(dataset) }
        item { Spacer(modifier = Modifier.height(84.dp)) }
    }
}

@Composable
private fun DerivedFormulasSubTab(
    dataset: ConstantsDataset,
    searchQuery: String
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(18.dp)
    val filtered = remember(dataset.derivedFormulas, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) dataset.derivedFormulas
        else dataset.derivedFormulas.filter {
            it.formula.lowercase().contains(q) ||
                it.description.lowercase().contains(q) ||
                it.chapters.any { ch -> ch.lowercase().contains(q) }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(filtered, key = { it.id }) { df ->
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
                        text = RichTextHelper.formatMathText(df.formula),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = RichTextHelper.MathFontFamily,
                        color = colors.primary
                    )
                    Text(
                        text = RichTextHelper.formatMathText(df.description),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textMain
                    )
                    if (df.chapters.isNotEmpty()) {
                        Text(
                            text = "Chapters: ${df.chapters.joinToString(" • ")}",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
        item { DatasetAttributionFooter(dataset) }
        item { Spacer(modifier = Modifier.height(84.dp)) }
    }
}

@Composable
private fun AboutAndSourcesSubTab(dataset: ConstantsDataset) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(18.dp)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("constants_about_subtab"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Symbol Notes
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, cardShape)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "SYMBOL COLLISIONS & DISAMBIGUATION NOTES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.4.sp,
                        color = colors.primary
                    )
                    dataset.metadata.symbolNotes.forEach { note ->
                        Text(
                            text = RichTextHelper.formatMathText("• $note"),
                            fontSize = 12.sp,
                            color = colors.textMain
                        )
                    }
                }
            }
        }

        // 2. Why certain quantities are NOT fixed constants (8 entries)
        item {
            Text(
                text = "WHY CERTAIN QUANTITIES ARE NOT FIXED CONSTANTS (${dataset.notFixedTables.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.4.sp,
                color = colors.textSecondary.copy(alpha = 0.75f)
            )
        }

        items(dataset.notFixedTables, key = { it.id }) { nf ->
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
                        text = RichTextHelper.formatMathText(nf.items.joinToString(", ")),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = RichTextHelper.formatMathText(nf.whyNotOneFixedValue),
                        fontSize = 12.sp,
                        color = colors.primary
                    )
                    nf.formulaNotes.forEach { fn ->
                        Text(
                            text = RichTextHelper.formatMathText("• $fn"),
                            fontSize = 12.sp,
                            fontFamily = RichTextHelper.MathFontFamily,
                            color = colors.textSecondary
                        )
                    }
                    if (nf.chapters.isNotEmpty()) {
                        Text(
                            text = "Chapters: ${nf.chapters.joinToString(" • ")}",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }

        // 3. Sources & Metadata Footer
        item {
            DatasetAttributionFooter(dataset, expandedDetails = true)
        }
        item { Spacer(modifier = Modifier.height(84.dp)) }
    }
}

@Composable
private fun DatasetAttributionFooter(
    dataset: ConstantsDataset,
    expandedDetails: Boolean = false
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(16.dp)
    val meta = dataset.metadata

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(cardShape)
            .background(colors.card.copy(alpha = 0.7f))
            .border(1.dp, colors.cardBorder, cardShape)
            .padding(14.dp)
            .testTag("constants_attribution_footer")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val genDate = meta.generatedDate.take(10).ifEmpty { "2026-10-26" }
            Text(
                text = "DATASET ATTRIBUTION • v${meta.version} ($genDate)",
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.2.sp,
                color = colors.textSecondary
            )
            val titleText = meta.title.ifEmpty { "HSC Physics & Chemistry Physical Constants" }
            val targetText = meta.target.ifEmpty { "NCTB Bangladesh HSC Physics & Chemistry" }
            Text(
                text = "$titleText — $targetText",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMain
            )
            if (expandedDetails || dataset.sources.isNotEmpty()) {
                val srcSummary = dataset.sources.joinToString(" • ") { "${it.title} (${it.organization})" }
                if (srcSummary.isNotBlank()) {
                    Text(
                        text = "Sources: $srcSummary",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }
            }
            if (expandedDetails && meta.importantLimitations.isNotEmpty()) {
                meta.importantLimitations.forEach { lim ->
                    Text(
                        text = "• $lim",
                        fontSize = 11.sp,
                        color = colors.textSecondary.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
