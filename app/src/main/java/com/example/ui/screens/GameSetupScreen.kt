package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.Difficulty
import com.example.domain.GameMode
import com.example.ui.components.ZenSlider
import com.example.ui.components.ZenToggle
import com.example.ui.theme.ZenTheme

@Composable
fun GameSetupScreen(
    mode: GameMode,
    digits: Int,
    difficulty: Difficulty,
    allowRemainder: Boolean,
    allowNegativeResults: Boolean,
    mixedOps: List<Boolean>,
    squareRangeType: String,
    customSquareRange: Pair<Int, Int>,
    sqrtRangeType: String,
    customSqrtRange: Pair<Int, Int>,
    fractionDenRange: Pair<Int, Int>,
    fractionNumRange: Pair<Int, Int>,
    onDigitsChange: (Int) -> Unit,
    onDifficultyChange: (Difficulty) -> Unit,
    onAllowRemainderChange: (Boolean) -> Unit,
    onAllowNegativeResultsChange: (Boolean) -> Unit,
    onMixedOpsChange: (List<Boolean>) -> Unit,
    onSquareRangeTypeChange: (String) -> Unit,
    onCustomSquareRangeChange: (Pair<Int, Int>) -> Unit,
    onSqrtRangeTypeChange: (String) -> Unit,
    onCustomSqrtRangeChange: (Pair<Int, Int>) -> Unit,
    onFractionDenRangeChange: (Pair<Int, Int>) -> Unit,
    onFractionNumRangeChange: (Pair<Int, Int>) -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .testTag("game_setup_screen")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.textMain
                    )
                }

                Text(
                    text = mode.displayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Scrollable settings
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Mixed Operations Toggles
                if (mode == GameMode.MIXED) {
                    item {
                        SectionHeader(title = "ARITHMETIC OPERATIONS")
                        val opList = listOf(
                            Triple("Addition", Icons.Default.Add, 0),
                            Triple("Subtraction", Icons.Default.Remove, 1),
                            Triple("Multiplication", Icons.Default.Close, 2),
                            Triple("Division", Icons.Default.Percent, 3)
                        )

                        val cardShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, cardShape)
                                .padding(vertical = 4.dp)
                        ) {
                            Column {
                                opList.forEach { (label, icon, idx) ->
                                    val checked = mixedOps.getOrElse(idx) { true }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.primary.copy(alpha = 0.1f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = label,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Text(
                                                text = label,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colors.textMain,
                                                modifier = Modifier.padding(start = 12.dp)
                                            )
                                        }

                                        ZenToggle(
                                            checked = checked,
                                            onCheckedChange = { newVal ->
                                                val updated = mixedOps.toMutableList()
                                                updated[idx] = newVal
                                                if (updated.any { it }) {
                                                    onMixedOpsChange(updated)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Digits selector
                if (mode in listOf(GameMode.ADDITION, GameMode.SUBTRACTION, GameMode.MULTIPLICATION, GameMode.DIVISION, GameMode.MIXED)) {
                    item {
                        SectionHeader(title = "NUMBER OF DIGITS")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(1, 2, 3, 4, 5).forEach { d ->
                                val isSelected = digits == d
                                val shape = RoundedCornerShape(16.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(shape)
                                        .background(if (isSelected) colors.primary else colors.card)
                                        .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, shape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(color = colors.primary.copy(alpha = 0.2f)),
                                            onClick = { onDigitsChange(d) }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = d.toString(),
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Difficulty selector
                item {
                    SectionHeader(title = "DIFFICULTY")
                    val cardShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Difficulty.entries.forEach { diff ->
                                    val isSelected = difficulty == diff
                                    Text(
                                        text = diff.label.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        letterSpacing = 1.sp,
                                        color = if (isSelected) colors.primary else colors.textSecondary.copy(alpha = 0.5f),
                                        modifier = Modifier.clickable { onDifficultyChange(diff) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val diffIndex = Difficulty.entries.indexOf(difficulty).toFloat()
                            ZenSlider(
                                value = diffIndex,
                                onValueChange = { floatVal ->
                                    val idx = floatVal.toInt().coerceIn(0, 2)
                                    onDifficultyChange(Difficulty.entries[idx])
                                },
                                valueRange = 0f..2f,
                                steps = 1
                            )
                        }
                    }
                }

                // Subtraction: Negative Results toggle
                if (mode == GameMode.SUBTRACTION) {
                    item {
                        SectionHeader(title = "SUBTRACTION MODE")
                        ToggleCardRow(
                            title = "Negative Results",
                            subtitle = if (allowNegativeResults) "Negative answers allowed" else "Positive results only",
                            icon = Icons.Default.Remove,
                            checked = allowNegativeResults,
                            onCheckedChange = onAllowNegativeResultsChange
                        )
                    }
                }

                // Division: Allow Remainders toggle
                if (mode == GameMode.DIVISION) {
                    item {
                        SectionHeader(title = "DIVISION MODE")
                        ToggleCardRow(
                            title = "Allow Remainders",
                            subtitle = if (allowRemainder) "Include decimal solutions" else "Exact division only",
                            icon = Icons.Default.Calculate,
                            checked = allowRemainder,
                            onCheckedChange = onAllowRemainderChange
                        )
                    }
                }

                // Fraction: Denominator & Numerator Ranges
                if (mode == GameMode.FRACTION) {
                    item {
                        SectionHeader(title = "MAXIMUM DENOMINATOR")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 4, 6, 8, 10).forEach { den ->
                                val isSelected = fractionDenRange.second == den
                                val shape = RoundedCornerShape(14.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(shape)
                                        .background(if (isSelected) colors.primary else colors.card)
                                        .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, shape)
                                        .clickable { onFractionDenRangeChange(Pair(2, den)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = den.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        SectionHeader(title = "MAXIMUM NUMERATOR")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(2, 4, 6, 8, 10).forEach { num ->
                                val isSelected = fractionNumRange.second == num
                                val shape = RoundedCornerShape(14.dp)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .clip(shape)
                                        .background(if (isSelected) colors.primary else colors.card)
                                        .border(1.dp, if (isSelected) colors.primary else colors.cardBorder, shape)
                                        .clickable { onFractionNumRangeChange(Pair(1, num)) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = num.toString(),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isSelected) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Square Numbers: Range selector
                if (mode == GameMode.SQUARE) {
                    item {
                        SectionHeader(title = "SQUARE RANGE")
                        val cardShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, cardShape)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val fixedActive = squareRangeType == "fixed"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (fixedActive) colors.primary else colors.surface)
                                        .clickable { onSquareRangeTypeChange("fixed") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "1 to 25",
                                        fontWeight = FontWeight.Bold,
                                        color = if (fixedActive) colors.onPrimary else colors.textSecondary
                                    )
                                }

                                val customActive = squareRangeType == "custom"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (customActive) colors.primary else colors.surface)
                                        .clickable { onSquareRangeTypeChange("custom") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Custom",
                                        fontWeight = FontWeight.Bold,
                                        color = if (customActive) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Square Root Range Selector
                if (mode == GameMode.SQUARE_ROOT) {
                    item {
                        SectionHeader(title = "SQUARE ROOT RANGE")
                        val cardShape = RoundedCornerShape(20.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(cardShape)
                                .background(colors.card)
                                .border(1.dp, colors.cardBorder, cardShape)
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val fixedActive = sqrtRangeType == "fixed"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (fixedActive) colors.primary else colors.surface)
                                        .clickable { onSqrtRangeTypeChange("fixed") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "By Difficulty",
                                        fontWeight = FontWeight.Bold,
                                        color = if (fixedActive) colors.onPrimary else colors.textSecondary
                                    )
                                }

                                val customActive = sqrtRangeType == "custom"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (customActive) colors.primary else colors.surface)
                                        .clickable { onSqrtRangeTypeChange("custom") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Custom",
                                        fontWeight = FontWeight.Bold,
                                        color = if (customActive) colors.onPrimary else colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }

            // Fixed CTA Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Button(
                    onClick = onStart,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    ),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("start_practice_btn")
                ) {
                    Text(
                        text = "Start Practice",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val colors = ZenTheme.colors
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = 1.5.sp,
        color = colors.textSecondary.copy(alpha = 0.7f),
        modifier = Modifier.padding(bottom = 10.dp)
    )
}

@Composable
private fun ToggleCardRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, cardShape)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(colors.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textMain
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            }

            ZenToggle(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
