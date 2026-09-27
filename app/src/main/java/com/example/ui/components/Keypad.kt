package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZenTheme

@Composable
fun Keypad(
    onKey: (String) -> Unit,
    modifier: Modifier = Modifier,
    disabled: Boolean = false,
    negativeEnabled: Boolean = true,
    showFraction: Boolean = true,
    showDecimal: Boolean = true,
    hapticFeedbackEnabled: Boolean = true
) {
    val colors = ZenTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.keypadBg)
            .testTag("keypad_container")
    ) {
        val totalWidth = maxWidth
        val horizontalPadding = 12.dp
        val gap = 8.dp
        // 4 keys across: divide available width evenly so each key is ideally square
        val keySize = ((totalWidth - horizontalPadding * 2 - gap * 3) / 4).coerceAtLeast(48.dp)
        val enterHeight = keySize * 2 + gap

        Column(modifier = Modifier.fillMaxWidth()) {
            // Divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.keypadBorder)
            )

            // 4-row keypad grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                // Row 1: 1, 2, 3, backspace (all square)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    KeyButton(text = "1", onClick = { onKey("1") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                    KeyButton(text = "2", onClick = { onKey("2") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                    KeyButton(text = "3", onClick = { onKey("3") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                    KeyButton(
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                tint = colors.keypadText.copy(alpha = 0.85f),
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        onClick = { onKey("backspace") },
                        size = keySize,
                        disabled = disabled,
                        hapticEnabled = hapticFeedbackEnabled
                    )
                }

                // Row 2: 4, 5, 6, - (all square)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    KeyButton(text = "4", onClick = { onKey("4") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                    KeyButton(text = "5", onClick = { onKey("5") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                    KeyButton(text = "6", onClick = { onKey("6") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                    KeyButton(
                        text = "−",
                        onClick = { onKey("-") },
                        size = keySize,
                        disabled = disabled || !negativeEnabled,
                        enabledAlpha = if (negativeEnabled) 1f else 0.25f,
                        hapticEnabled = hapticFeedbackEnabled
                    )
                }

                // Rows 3 & 4: Left 3 cols (7, 8, 9 / ., 0, /) + Right col Enter button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(gap)
                ) {
                    // Left 3 columns
                    Column(
                        verticalArrangement = Arrangement.spacedBy(gap)
                    ) {
                        // Row 3: 7, 8, 9 (all square)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            KeyButton(text = "7", onClick = { onKey("7") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                            KeyButton(text = "8", onClick = { onKey("8") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                            KeyButton(text = "9", onClick = { onKey("9") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                        }

                        // Row 4: . or blank, 0, / or ÷ (all square)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(gap)
                        ) {
                            if (showDecimal) {
                                KeyButton(text = ".", onClick = { onKey(".") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                            } else {
                                Spacer(modifier = Modifier.size(keySize))
                            }

                            KeyButton(text = "0", onClick = { onKey("0") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)

                            if (showFraction) {
                                KeyButton(text = "/", onClick = { onKey("/") }, size = keySize, disabled = disabled, hapticEnabled = hapticFeedbackEnabled)
                            } else {
                                KeyButton(
                                    text = "÷",
                                    onClick = {},
                                    size = keySize,
                                    disabled = true,
                                    enabledAlpha = 0.25f,
                                    hapticEnabled = hapticFeedbackEnabled
                                )
                            }
                        }
                    }

                    // Right column: Enter button (spans rows 3 and 4)
                    KeyButton(
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Submit Answer",
                                tint = colors.onPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        },
                        onClick = { onKey("enter") },
                        width = keySize,
                        height = enterHeight,
                        disabled = disabled,
                        isPrimary = true,
                        hapticEnabled = hapticFeedbackEnabled
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    width: Dp? = null,
    height: Dp? = null,
    text: String? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    disabled: Boolean = false,
    isPrimary: Boolean = false,
    enabledAlpha: Float = 1f,
    hapticEnabled: Boolean = true
) {
    val colors = ZenTheme.colors
    val view = LocalView.current
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }
    val shape = RoundedCornerShape(14.dp)

    // Background color: clean keypad background or primary for enter
    val bgColor = if (isPrimary) colors.primary else colors.keypadBtn
    val contentColor = if (isPrimary) colors.onPrimary else colors.keypadText

    // Sharp, thin black (theme invertable) border
    val borderColor = if (isPrimary) {
        colors.primary
    } else {
        if (colors.isDark) Color(0xFF52525B) else Color(0xFF000000)
    }

    val actualWidth = width ?: size
    val actualHeight = height ?: size

    Box(
        modifier = modifier
            .width(actualWidth)
            .height(actualHeight)
            .clip(shape)
            .background(bgColor.copy(alpha = if (disabled) 0.4f else enabledAlpha))
            .border(1.dp, borderColor, shape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = contentColor.copy(alpha = 0.2f)),
                enabled = !disabled && enabledAlpha > 0.3f,
                onClick = {
                    if (hapticEnabled) {
                        try {
                            view.isHapticFeedbackEnabled = true
                            view.performHapticFeedback(
                                HapticFeedbackConstants.KEYBOARD_TAP,
                                HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
                            )
                        } catch (_: Exception) {}

                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator?.vibrate(20L)
                            }
                        } catch (_: Exception) {}
                    }
                    onClick()
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            icon()
        } else if (text != null) {
            Text(
                text = text,
                color = contentColor.copy(alpha = if (disabled) 0.4f else enabledAlpha),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
