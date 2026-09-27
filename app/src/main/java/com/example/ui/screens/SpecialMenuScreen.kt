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
import com.example.domain.GameMode
import com.example.ui.theme.ZenTheme

@Composable
fun SpecialMenuScreen(
    mode: GameMode,
    onSelectRange: (Pair<Int, Int>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ZenTheme.colors
    var isCustomMode by remember { mutableStateOf(false) }
    var customStart by remember { mutableStateOf("1") }
    var customEnd by remember { mutableStateOf("12") }

    val isTables = mode == GameMode.MULTIPLICATION_TABLE
    val title = if (isTables) "Multiplication Tables" else "Factor Finding"

    val standardRanges = listOf(
        Triple(Pair(1, 10), "1 to 10", "Foundation Tables"),
        Triple(Pair(11, 20), "11 to 20", "Extended Tables"),
        Triple(Pair(1, 20), "1 to 20", "Complete Standard Set")
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .testTag("special_menu_screen")
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.textMain
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "SELECT RANGE",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMain,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = title.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    color = colors.textSecondary.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                )
            }

            if (!isCustomMode) {
                // Preset ranges
                items(standardRanges.size) { i ->
                    val (range, label, sub) = standardRanges[i]
                    val cardShape = RoundedCornerShape(18.dp)

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.primary.copy(alpha = 0.2f)),
                                onClick = { onSelectRange(range) }
                            )
                            .padding(20.dp)
                    ) {
                        Column {
                            Text(
                                text = label,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textMain,
                                letterSpacing = (-1).sp
                            )
                            Text(
                                text = sub.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                // Custom Range Button
                item {
                    val cardShape = RoundedCornerShape(18.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = ripple(color = colors.primary.copy(alpha = 0.2f)),
                                onClick = { isCustomMode = true }
                            )
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Custom",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textMain,
                                    letterSpacing = (-1).sp
                                )
                                Text(
                                    text = "RANGE OF TABLES",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Custom Range",
                                tint = colors.primary
                            )
                        }
                    }
                }
            } else {
                // Custom Range Form
                item {
                    val cardShape = RoundedCornerShape(24.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(cardShape)
                            .background(colors.card)
                            .border(1.dp, colors.cardBorder, cardShape)
                            .padding(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Practice tables from",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textSecondary
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = customStart,
                                    onValueChange = { if (it.length <= 3) customStart = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textMain
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.cardBorder,
                                        focusedContainerColor = colors.surface,
                                        unfocusedContainerColor = colors.surface
                                    )
                                )

                                Text(
                                    text = "—",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textSecondary,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                OutlinedTextField(
                                    value = customEnd,
                                    onValueChange = { if (it.length <= 3) customEnd = it },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textMain
                                    ),
                                    singleLine = true,
                                    modifier = Modifier.width(80.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colors.primary,
                                        unfocusedBorderColor = colors.cardBorder,
                                        focusedContainerColor = colors.surface,
                                        unfocusedContainerColor = colors.surface
                                    )
                                )
                            }

                            val s = customStart.toIntOrNull() ?: 1
                            val e = customEnd.toIntOrNull() ?: 12
                            val minVal = minOf(s, e)
                            val maxVal = maxOf(s, e)

                            Text(
                                text = "Tables $minVal to $maxVal (× 1-12)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textSecondary,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val s = customStart.toIntOrNull() ?: 1
                            val e = customEnd.toIntOrNull() ?: 12
                            onSelectRange(Pair(minOf(s, e), maxOf(s, e)))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Text(
                            text = "Start Practice",
                            fontSize = 16.sp,
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
}
