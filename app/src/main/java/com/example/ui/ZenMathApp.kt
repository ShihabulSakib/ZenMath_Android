package com.example.ui

/**
 * ZenMath Application Main Entry
 *
 * Inspired by Shihabul Sakib's open-source ZenMath:
 * Repository: https://github.com/ShihabulSakib/zenmath
 * Original Creator: Shihabul Sakib
 */

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.MainTab
import com.example.ui.components.ZenNavbar
import com.example.ui.screens.GameScreen
import com.example.ui.screens.GameSetupScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.MainMenuScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.RevisionScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SpecialMenuScreen
import com.example.ui.screens.StatsScreen
import com.example.ui.theme.ZenMathTheme
import com.example.ui.theme.ZenTheme

@Composable
fun ZenMathApp(
    viewModel: ZenMathViewModel = viewModel()
) {
    val settings by viewModel.settingsFlow.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val dailyProgress by viewModel.dailyProgressFlow.collectAsState()

    // Game Setup state
    val selectedMode by viewModel.selectedMode.collectAsState()
    val difficulty by viewModel.difficulty.collectAsState()
    val digits by viewModel.digits.collectAsState()
    val allowRemainder by viewModel.allowRemainder.collectAsState()
    val allowNegativeResults by viewModel.allowNegativeResults.collectAsState()
    val mixedOps by viewModel.mixedOps.collectAsState()
    val squareRangeType by viewModel.squareRangeType.collectAsState()
    val customSquareRange by viewModel.customSquareRange.collectAsState()
    val sqrtRangeType by viewModel.sqrtRangeType.collectAsState()
    val customSqrtRange by viewModel.customSqrtRange.collectAsState()
    val fractionDenRange by viewModel.fractionDenRange.collectAsState()
    val fractionNumRange by viewModel.fractionNumRange.collectAsState()

    // Active Game state
    val currentQuestionIndex by viewModel.currentQuestionIndex.collectAsState()
    val currentProblem by viewModel.currentProblem.collectAsState()
    val userInput by viewModel.userInput.collectAsState()
    val timeRemaining by viewModel.timeRemaining.collectAsState()
    val currentQuestionElapsedMs by viewModel.currentQuestionElapsedMs.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val score by viewModel.score.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val questionResults by viewModel.questionResults.collectAsState()

    // Stats & History
    val sessions by viewModel.sessions.collectAsState()
    val statsData by viewModel.statsData.collectAsState()
    val recentPerformance by viewModel.recentPerformance.collectAsState()
    val horizonWaveBuckets by viewModel.horizonWaveBuckets.collectAsState()
    val historyFilter by viewModel.historyFilter.collectAsState()

    // Handle Android system back button
    BackHandler(enabled = currentScreen != ZenScreen.MENU) {
        when (currentScreen) {
            ZenScreen.PLAYING -> viewModel.onQuitGame()
            ZenScreen.RESULTS -> viewModel.navigateTo(ZenScreen.MENU)
            ZenScreen.SETUP, ZenScreen.SPECIAL_MENU -> viewModel.navigateTo(ZenScreen.MENU)
            ZenScreen.REVISION, ZenScreen.STATS, ZenScreen.HISTORY, ZenScreen.SETTINGS -> {
                viewModel.selectTab(MainTab.PRACTICE)
            }
            ZenScreen.MENU -> { /* let system handle exit */ }
        }
    }

    ZenMathTheme(darkTheme = settings.isDarkTheme) {
        val colors = ZenTheme.colors

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.surface)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
        ) {
            // Screen switcher
            when (currentScreen) {
                ZenScreen.MENU -> {
                    MainMenuScreen(
                        onSelectMode = { mode -> viewModel.onSelectMode(mode) },
                        dailyProgress = dailyProgress,
                        settings = settings,
                        onToggleTheme = { viewModel.toggleTheme() },
                        onUpdateDailyGoal = { goal -> viewModel.updateDailyGoal(goal) }
                    )
                }

                ZenScreen.SETUP -> {
                    GameSetupScreen(
                        mode = selectedMode,
                        digits = digits,
                        difficulty = difficulty,
                        allowRemainder = allowRemainder,
                        allowNegativeResults = allowNegativeResults,
                        mixedOps = mixedOps,
                        squareRangeType = squareRangeType,
                        customSquareRange = customSquareRange,
                        sqrtRangeType = sqrtRangeType,
                        customSqrtRange = customSqrtRange,
                        fractionDenRange = fractionDenRange,
                        fractionNumRange = fractionNumRange,
                        onDigitsChange = { viewModel.setDigits(it) },
                        onDifficultyChange = { viewModel.setDifficulty(it) },
                        onAllowRemainderChange = { viewModel.setAllowRemainder(it) },
                        onAllowNegativeResultsChange = { viewModel.setAllowNegativeResults(it) },
                        onMixedOpsChange = { viewModel.setMixedOps(it) },
                        onSquareRangeTypeChange = { viewModel.setSquareRangeType(it) },
                        onCustomSquareRangeChange = { viewModel.setCustomSquareRange(it) },
                        onSqrtRangeTypeChange = { viewModel.setSqrtRangeType(it) },
                        onCustomSqrtRangeChange = { viewModel.setCustomSqrtRange(it) },
                        onFractionDenRangeChange = { viewModel.setFractionDenRange(it) },
                        onFractionNumRangeChange = { viewModel.setFractionNumRange(it) },
                        onStart = { viewModel.startGame() },
                        onBack = { viewModel.navigateTo(ZenScreen.MENU) }
                    )
                }

                ZenScreen.SPECIAL_MENU -> {
                    SpecialMenuScreen(
                        mode = selectedMode,
                        onSelectRange = { range ->
                            viewModel.setTableRange(range)
                            viewModel.startGame()
                        },
                        onBack = { viewModel.navigateTo(ZenScreen.MENU) }
                    )
                }

                ZenScreen.PLAYING -> {
                    GameScreen(
                        currentQuestion = currentQuestionIndex,
                        totalQuestions = settings.totalQuestions,
                        timeRemaining = timeRemaining,
                        currentQuestionElapsedMs = currentQuestionElapsedMs,
                        problem = currentProblem,
                        userInput = userInput,
                        feedback = feedback,
                        mode = selectedMode,
                        streak = streak,
                        showStreak = settings.showStreak,
                        allowNegativeResults = allowNegativeResults,
                        onKey = { key -> viewModel.handleKeyPress(key) },
                        onQuit = { viewModel.onQuitGame() },
                        hapticFeedbackEnabled = settings.hapticFeedback
                    )
                }

                ZenScreen.RESULTS -> {
                    ResultsScreen(
                        score = score,
                        totalQuestions = settings.totalQuestions,
                        results = questionResults,
                        onPlayAgain = { viewModel.onPlayAgain() },
                        onMenu = { viewModel.navigateTo(ZenScreen.MENU) }
                    )
                }

                ZenScreen.REVISION -> {
                    RevisionScreen()
                }

                ZenScreen.STATS -> {
                    StatsScreen(
                        statsData = statsData,
                        sessions = sessions,
                        recentPerformance = recentPerformance,
                        horizonWaveBuckets = horizonWaveBuckets,
                        dailyGoal = settings.dailyGoal,
                        onClearAllData = { viewModel.clearAllData() },
                        onExportBackup = { viewModel.exportBackup() },
                        onImportBackup = { json -> viewModel.importBackup(json) }
                    )
                }

                ZenScreen.HISTORY -> {
                    HistoryScreen(
                        sessions = sessions,
                        selectedFilter = historyFilter,
                        onFilterChange = { viewModel.setHistoryFilter(it) }
                    )
                }

                ZenScreen.SETTINGS -> {
                    SettingsScreen(
                        settings = settings,
                        onSaveSettings = { viewModel.saveSettings(it) }
                    )
                }
            }

            // Floating Navigation Bar (shown on tabbed screens)
            val showNavbar = currentScreen in listOf(
                ZenScreen.MENU,
                ZenScreen.REVISION,
                ZenScreen.HISTORY,
                ZenScreen.STATS,
                ZenScreen.SETTINGS
            )

            if (showNavbar) {
                ZenNavbar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab -> viewModel.selectTab(tab) },
                    dailyProgress = dailyProgress,
                    dailyGoal = settings.dailyGoal,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
