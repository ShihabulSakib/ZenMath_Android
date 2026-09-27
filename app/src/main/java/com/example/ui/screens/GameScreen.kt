package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.GameMode
import com.example.domain.MathProblem
import com.example.ui.FeedbackType
import com.example.ui.components.Keypad
import com.example.ui.components.ZenProgressBar
import com.example.ui.theme.ZenTheme

@Composable
fun GameScreen(
    currentQuestion: Int,
    totalQuestions: Int,
    timeRemaining: Int,
    currentQuestionElapsedMs: Long,
    problem: MathProblem?,
    userInput: String,
    feedback: FeedbackType,
    mode: GameMode,
    streak: Int,
    showStreak: Boolean,
    allowNegativeResults: Boolean,
    onKey: (String) -> Unit,
    onQuit: () -> Unit,
    modifier: Modifier = Modifier,
    hapticFeedbackEnabled: Boolean = true
) {
    val colors = ZenTheme.colors

    val elapsedSec = currentQuestionElapsedMs / 1000f
    val stopwatchStr = String.format(java.util.Locale.US, "%.1fs", elapsedSec)
    val isTimeLow = timeRemaining in 1..5

    val negativeEnabled = (mode == GameMode.SUBTRACTION && allowNegativeResults) ||
            mode in listOf(GameMode.CHAIN_CALCULATION, GameMode.APPROXIMATION, GameMode.NUMBER_SERIES)
    val showFraction = mode == GameMode.FRACTION
    val showDecimal = mode in listOf(GameMode.FRACTION, GameMode.DIVISION, GameMode.PERCENTAGE)

    // Flash background on feedback
    val feedbackBgColor by animateColorAsState(
        targetValue = when (feedback) {
            FeedbackType.CORRECT -> colors.correct.copy(alpha = 0.08f)
            FeedbackType.INCORRECT -> colors.incorrect.copy(alpha = 0.08f)
            FeedbackType.TIMEOUT -> colors.timeout.copy(alpha = 0.08f)
            FeedbackType.NONE -> colors.surface
        },
        label = "feedback_bg"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(feedbackBgColor)
            .testTag("game_screen")
    ) {
        // Upper Game Area
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Header: Progress bar + Centered Timer Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Full width progress bar
                ZenProgressBar(
                    value = currentQuestion,
                    max = totalQuestions,
                    modifier = Modifier.fillMaxWidth()
                )

                // Subheader: Q counter (Left), Centered Timer (Center), Stats & Quit (Right)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    // Left: Q counter
                    Row(
                        modifier = Modifier.align(Alignment.CenterStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Q $currentQuestion/$totalQuestions",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textSecondary.copy(alpha = 0.7f),
                            letterSpacing = 1.sp
                        )
                    }

                    // CENTER: The Stopwatch (counts up 0.0s, 0.1s... to per-question target)
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isTimeLow) colors.incorrect.copy(alpha = 0.12f) else colors.card)
                            .border(
                                1.dp,
                                if (isTimeLow) colors.incorrect.copy(alpha = 0.5f) else colors.cardBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Stopwatch",
                                tint = if (isTimeLow) colors.incorrect else colors.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = stopwatchStr,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = if (isTimeLow) colors.incorrect else colors.textMain
                            )
                        }
                    }

                    // Right: Streak and Quit (Stopwatch is centered at top)
                    Row(
                        modifier = Modifier.align(Alignment.CenterEnd),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (showStreak && streak > 1) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocalFireDepartment,
                                    contentDescription = "Streak",
                                    tint = colors.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = streak.toString(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = colors.primary
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onQuit() }
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Quit",
                                tint = colors.textSecondary.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Quit",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Central Question Display
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val questionText = problem?.questionDisplay ?: ""
                    val textSize = when {
                        mode in listOf(GameMode.RATIO, GameMode.CHAIN_CALCULATION, GameMode.NUMBER_SERIES) -> 28.sp
                        questionText.length > 10 -> 34.sp
                        else -> 52.sp
                    }

                    Text(
                        text = questionText,
                        fontSize = textSize,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        color = colors.textMain,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.testTag("question_text")
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Answer / Feedback Display
                    Box(
                        modifier = Modifier
                            .height(64.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        when (feedback) {
                            FeedbackType.NONE -> {
                                if (userInput.isNotEmpty()) {
                                    Text(
                                        text = userInput,
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.primary,
                                        modifier = Modifier.testTag("user_input_display")
                                    )
                                } else {
                                    Text(
                                        text = "_",
                                        fontSize = 38.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.primary.copy(alpha = 0.6f)
                                    )
                                }
                            }
                            FeedbackType.CORRECT -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Correct",
                                        tint = colors.correct,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Text(
                                        text = "Correct!",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.correct
                                    )
                                }
                            }
                            FeedbackType.TIMEOUT -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TimerOff,
                                            contentDescription = "Timeout",
                                            tint = colors.timeout,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = "Time's Up",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.timeout
                                        )
                                    }
                                    Text(
                                        text = "Answer: ${problem?.answer ?: ""}",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            FeedbackType.INCORRECT -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Cancel,
                                            contentDescription = "Incorrect",
                                            tint = colors.incorrect,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Text(
                                            text = userInput,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = colors.incorrect
                                        )
                                    }
                                    Text(
                                        text = "Answer: ${problem?.answer ?: ""}",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Keypad
        Keypad(
            onKey = onKey,
            disabled = feedback != FeedbackType.NONE,
            negativeEnabled = negativeEnabled,
            showFraction = showFraction,
            showDecimal = showDecimal,
            hapticFeedbackEnabled = hapticFeedbackEnabled,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
