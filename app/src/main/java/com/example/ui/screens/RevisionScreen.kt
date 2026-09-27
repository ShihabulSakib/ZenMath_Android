package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grid3x3
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZenTheme

enum class RevisionTab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    TABLES("Tables", Icons.Default.Grid3x3),
    SQUARES("Squares", Icons.Default.Tag),
    FRACTIONS("Fractions", Icons.Default.Percent)
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

            // 3-tab pill selector
            val tabShape = RoundedCornerShape(14.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(tabShape)
                    .background(colors.card.copy(alpha = 0.6f))
                    .border(1.dp, colors.cardBorder, tabShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                RevisionTab.entries.forEach { tab ->
                    val isSelected = tab == activeTab
                    val pillShape = RoundedCornerShape(10.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(pillShape)
                            .background(if (isSelected) colors.primary else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.primary.copy(alpha = 0.2f)),
                                onClick = { activeTab = tab }
                            ),
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
                RevisionTab.SQUARES -> SquaresTab()
                RevisionTab.FRACTIONS -> FractionsTab()
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
private fun SquaresTab() {
    val colors = ZenTheme.colors

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(25) { idx ->
            val n = idx + 1
            val cardShape = RoundedCornerShape(16.dp)

            Box(
                modifier = Modifier
                    .aspectRatio(1f)
                    .clip(cardShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, cardShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$n²",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
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

        item {
            Spacer(modifier = Modifier.height(84.dp))
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
