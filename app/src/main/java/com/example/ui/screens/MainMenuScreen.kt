package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MultipleStop
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.GameMode
import com.example.domain.GameSettings
import com.example.ui.components.ZenProgressBar
import com.example.ui.theme.ZenTheme

sealed class SpecializedIcon {
    data class Vector(val imageVector: ImageVector) : SpecializedIcon()
    data class Resource(val resId: Int) : SpecializedIcon()
    data class Glyph(val text: String) : SpecializedIcon()
}

@Composable
fun MainMenuScreen(
    onSelectMode: (GameMode) -> Unit,
    dailyProgress: Int,
    settings: GameSettings,
    onToggleTheme: () -> Unit,
    onUpdateDailyGoal: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .testTag("main_menu_screen")
    ) {
        // Sticky Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ZenMath",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textMain,
                letterSpacing = (-0.5).sp
            )

            IconButton(
                onClick = onToggleTheme,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, CircleShape)
                    .testTag("theme_toggle_btn")
            ) {
                Icon(
                    imageVector = if (colors.isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Mode Select Section
            item {
                Text(
                    text = "SELECT MODE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )

                // 2x2 Core Operation Grid
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OperationCard(
                            mode = GameMode.ADDITION,
                            symbol = "+",
                            label = "Addition",
                            onClick = { onSelectMode(GameMode.ADDITION) },
                            modifier = Modifier.weight(1f)
                        )
                        OperationCard(
                            mode = GameMode.SUBTRACTION,
                            symbol = "−",
                            label = "Subtraction",
                            onClick = { onSelectMode(GameMode.SUBTRACTION) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OperationCard(
                            mode = GameMode.MULTIPLICATION,
                            symbol = "×",
                            label = "Multiplication",
                            onClick = { onSelectMode(GameMode.MULTIPLICATION) },
                            modifier = Modifier.weight(1f)
                        )
                        OperationCard(
                            mode = GameMode.DIVISION,
                            symbol = "÷",
                            label = "Division",
                            onClick = { onSelectMode(GameMode.DIVISION) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Mixed Operations Card
            item {
                val cardShape = RoundedCornerShape(20.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, cardShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = ripple(color = colors.primary.copy(alpha = 0.15f)),
                            onClick = { onSelectMode(GameMode.MIXED) }
                        )
                        .padding(20.dp)
                        .testTag("mode_mixed")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Mixed Operations",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain
                            )
                            Text(
                                text = "Randomized challenge",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(colors.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Mixed",
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // Specialized Practice Section with authentic vector icons & glyphs
            item {
                Text(
                    text = "SPECIALIZED PRACTICE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val specializedModes = listOf(
                    Triple(GameMode.MULTIPLICATION_TABLE, SpecializedIcon.Resource(R.drawable.ic_tables), "Tables"),
                    Triple(GameMode.FACTOR_FINDING, SpecializedIcon.Resource(R.drawable.ic_factors), "Factors"),
                    Triple(GameMode.SQUARE, SpecializedIcon.Glyph("x²"), "Squares"),
                    Triple(GameMode.FRACTION, SpecializedIcon.Glyph("½"), "Fractions"),
                    Triple(GameMode.PERCENTAGE, SpecializedIcon.Glyph("%"), "Percent"),
                    Triple(GameMode.SQUARE_ROOT, SpecializedIcon.Glyph("√"), "Roots"),
                    Triple(GameMode.APPROXIMATION, SpecializedIcon.Glyph("≈"), "Estimation"),
                    Triple(GameMode.NUMBER_SERIES, SpecializedIcon.Vector(Icons.Default.LinearScale), "Series"),
                    Triple(GameMode.RATIO, SpecializedIcon.Glyph("::"), "Ratio"),
                    Triple(GameMode.CHAIN_CALCULATION, SpecializedIcon.Vector(Icons.Default.MultipleStop), "Chain")
                )

                // Grid of 3 columns
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val chunked = specializedModes.chunked(3)
                    chunked.forEach { rowModes ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowModes.forEach { (mode, icon, label) ->
                                SpecializedCard(
                                    mode = mode,
                                    icon = icon,
                                    label = label,
                                    onClick = { onSelectMode(mode) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Fill remaining slots in last row if not multiple of 3
                            val emptySlots = 3 - rowModes.size
                            for (i in 0 until emptySlots) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            // Daily Goal Card — Exact replica of Github repo layout
            item {
                val cardShape = RoundedCornerShape(24.dp)
                val canDecrease = dailyProgress >= settings.dailyGoal
                var textValue by remember(settings.dailyGoal) { mutableStateOf(if (settings.dailyGoal > 0) settings.dailyGoal.toString() else "") }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(colors.card)
                        .border(1.dp, colors.cardBorder, cardShape)
                        .padding(22.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Goal",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(top = 3.dp)
                                ) {
                                    Text(
                                        text = "$dailyProgress / ${settings.dailyGoal} sessions today",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textSecondary
                                    )
                                    if (dailyProgress >= settings.dailyGoal && settings.dailyGoal > 0) {
                                        Text(
                                            text = " ✓",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }

                            // Right side: Lock (if !canDecrease) + Target Input Box + "TARGET" / "COMPLETE GOAL TO CHANGE" label
                            Column(horizontalAlignment = Alignment.End) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (!canDecrease) {
                                        Icon(
                                            imageVector = Icons.Default.Lock,
                                            contentDescription = "Complete today's goal to lower it",
                                            tint = Color(0xFFF59E0B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    BasicTextField(
                                        value = textValue,
                                        onValueChange = { newVal ->
                                            if (newVal.isEmpty()) {
                                                textValue = ""
                                                onUpdateDailyGoal(0)
                                            } else {
                                                val clean = newVal.filter { it.isDigit() }.take(3)
                                                textValue = clean
                                                val intVal = clean.toIntOrNull()
                                                if (intVal != null && intVal in 0..100) {
                                                    onUpdateDailyGoal(intVal)
                                                }
                                            }
                                        },
                                        enabled = canDecrease,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = if (canDecrease) colors.primary else colors.primary.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        ),
                                        modifier = Modifier
                                            .width(64.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.primary.copy(alpha = 0.06f))
                                            .border(
                                                1.dp,
                                                colors.primary.copy(alpha = if (canDecrease) 0.3f else 0.15f),
                                                RoundedCornerShape(12.dp)
                                            )
                                            .padding(vertical = 8.dp)
                                            .testTag("daily_goal_input")
                                    )
                                }
                                Text(
                                    text = if (canDecrease) "TARGET" else "COMPLETE GOAL TO CHANGE",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp,
                                    color = colors.textSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        ZenProgressBar(value = dailyProgress, max = settings.dailyGoal)
                    }
                }
            }

            // Bottom space for floating navbar
            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }
    }
}

@Composable
private fun OperationCard(
    mode: GameMode,
    symbol: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cardShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.primary.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .testTag("mode_${mode.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = symbol,
                fontSize = 44.sp,
                fontWeight = FontWeight.Light,
                fontFamily = FontFamily.Monospace,
                color = colors.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun SpecializedCard(
    mode: GameMode,
    icon: SpecializedIcon,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(cardShape)
            .background(colors.card)
            .border(1.dp, colors.cardBorder, cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.primary.copy(alpha = 0.2f)),
                onClick = onClick
            )
            .padding(8.dp)
            .testTag("mode_${mode.id}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (icon) {
                is SpecializedIcon.Vector -> {
                    Icon(
                        imageVector = icon.imageVector,
                        contentDescription = label,
                        tint = colors.primary.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                is SpecializedIcon.Resource -> {
                    Icon(
                        painter = painterResource(id = icon.resId),
                        contentDescription = label,
                        tint = colors.primary.copy(alpha = 0.85f),
                        modifier = Modifier.size(24.dp)
                    )
                }
                is SpecializedIcon.Glyph -> {
                    Text(
                        text = icon.text,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = label.uppercase(),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = colors.textSecondary.copy(alpha = 0.8f)
            )
        }
    }
}
