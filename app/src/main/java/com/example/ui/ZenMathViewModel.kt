package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.PreferencesManager
import com.example.data.QuestionRecordEntity
import com.example.data.SessionEntity
import com.example.data.ZenMathDatabase
import com.example.data.ZenMathRepository
import com.example.domain.Difficulty
import com.example.domain.FractionsPool
import com.example.domain.GameMode
import com.example.domain.GameSettings
import com.example.domain.MathProblem
import com.example.domain.QuestionGenerator
import com.example.domain.QuestionResult
import com.example.domain.RecentPerformanceDay
import com.example.domain.StatsData
import com.example.notifications.NotificationScheduler
import com.example.ui.components.MainTab
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID
import kotlin.math.abs

enum class ZenScreen {
    MENU,
    SETUP,
    SPECIAL_MENU,
    PLAYING,
    RESULTS,
    REVISION,
    STATS,
    HISTORY,
    SETTINGS
}

enum class FeedbackType {
    NONE,
    CORRECT,
    INCORRECT,
    TIMEOUT
}

class ZenMathViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ZenMathDatabase.getDatabase(application)
    private val preferencesManager = PreferencesManager(application)
    private val repository = ZenMathRepository(db.dao(), preferencesManager)

    init {
        val settings = preferencesManager.loadSettings()
        if (settings.notificationsEnabled) {
            NotificationScheduler.scheduleReminders(application, settings.notificationTimes)
        }
    }

    // Navigation state
    private val _currentScreen = MutableStateFlow(ZenScreen.MENU)
    val currentScreen: StateFlow<ZenScreen> = _currentScreen.asStateFlow()

    private val _selectedTab = MutableStateFlow(MainTab.PRACTICE)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    // Settings
    val settingsFlow: StateFlow<GameSettings> = preferencesManager.settingsFlow
    val dailyProgressFlow: StateFlow<Int> = preferencesManager.dailyProgressFlow
    val bestStreakFlow: StateFlow<Int> = preferencesManager.bestStreakFlow

    // Game Setup Configuration
    private val _selectedMode = MutableStateFlow(GameMode.ADDITION)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _difficulty = MutableStateFlow(Difficulty.MEDIUM)
    val difficulty: StateFlow<Difficulty> = _difficulty.asStateFlow()

    private val _digits = MutableStateFlow(1)
    val digits: StateFlow<Int> = _digits.asStateFlow()

    private val _allowRemainder = MutableStateFlow(false)
    val allowRemainder: StateFlow<Boolean> = _allowRemainder.asStateFlow()

    private val _allowNegativeResults = MutableStateFlow(false)
    val allowNegativeResults: StateFlow<Boolean> = _allowNegativeResults.asStateFlow()

    private val _mixedOps = MutableStateFlow(listOf(true, true, true, true)) // +, -, *, /
    val mixedOps: StateFlow<List<Boolean>> = _mixedOps.asStateFlow()

    private val _tableRange = MutableStateFlow(Pair(1, 10))
    val tableRange: StateFlow<Pair<Int, Int>> = _tableRange.asStateFlow()

    private val _squareRangeType = MutableStateFlow("fixed") // "fixed" or "custom"
    val squareRangeType: StateFlow<String> = _squareRangeType.asStateFlow()

    private val _customSquareRange = MutableStateFlow(Pair(1, 25))
    val customSquareRange: StateFlow<Pair<Int, Int>> = _customSquareRange.asStateFlow()

    private val _sqrtRangeType = MutableStateFlow("fixed")
    val sqrtRangeType: StateFlow<String> = _sqrtRangeType.asStateFlow()

    private val _customSqrtRange = MutableStateFlow(Pair(1, 35))
    val customSqrtRange: StateFlow<Pair<Int, Int>> = _customSqrtRange.asStateFlow()

    private val _fractionDenRange = MutableStateFlow(Pair(2, 10))
    val fractionDenRange: StateFlow<Pair<Int, Int>> = _fractionDenRange.asStateFlow()

    private val _fractionNumRange = MutableStateFlow(Pair(1, 9))
    val fractionNumRange: StateFlow<Pair<Int, Int>> = _fractionNumRange.asStateFlow()

    // Active Gameplay State
    private val _currentQuestionIndex = MutableStateFlow(1)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _currentProblem = MutableStateFlow<MathProblem?>(null)
    val currentProblem: StateFlow<MathProblem?> = _currentProblem.asStateFlow()

    private val _userInput = MutableStateFlow("")
    val userInput: StateFlow<String> = _userInput.asStateFlow()

    private val _timeRemaining = MutableStateFlow(20)
    val timeRemaining: StateFlow<Int> = _timeRemaining.asStateFlow()

    private val _currentQuestionElapsedMs = MutableStateFlow(0L)
    val currentQuestionElapsedMs: StateFlow<Long> = _currentQuestionElapsedMs.asStateFlow()

    private val _feedback = MutableStateFlow(FeedbackType.NONE)
    val feedback: StateFlow<FeedbackType> = _feedback.asStateFlow()

    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _questionResults = MutableStateFlow<List<QuestionResult>>(emptyList())
    val questionResults: StateFlow<List<QuestionResult>> = _questionResults.asStateFlow()

    // Sessions & Stats State
    private val _sessions = MutableStateFlow<List<SessionEntity>>(emptyList())
    val sessions: StateFlow<List<SessionEntity>> = _sessions.asStateFlow()

    private val _statsData = MutableStateFlow(
        StatsData(0, 0, 0, 0.0, 0.0, 0, emptyMap())
    )
    val statsData: StateFlow<StatsData> = _statsData.asStateFlow()

    private val _recentPerformance = MutableStateFlow<List<RecentPerformanceDay>>(emptyList())
    val recentPerformance: StateFlow<List<RecentPerformanceDay>> = _recentPerformance.asStateFlow()

    private val _horizonWaveBuckets = MutableStateFlow<List<com.example.domain.DayBucket>>(emptyList())
    val horizonWaveBuckets: StateFlow<List<com.example.domain.DayBucket>> = _horizonWaveBuckets.asStateFlow()

    private val _historyFilter = MutableStateFlow("all")
    val historyFilter: StateFlow<String> = _historyFilter.asStateFlow()

    // Timers
    private var timerJob: Job? = null
    private var questionStartTimeMs: Long = 0L
    private var isProcessingAnswer: Boolean = false

    init {
        // Collect sessions and update stats
        viewModelScope.launch {
            repository.getAllSessionsFlow().collect { sessionList ->
                _sessions.value = sessionList
                _statsData.value = repository.computeStats(sessionList)
                _recentPerformance.value = repository.getRecentPerformance(sessionList, 7)
                _horizonWaveBuckets.value = repository.getHorizonWaveBuckets(sessionList, 10)
            }
        }
    }

    // Navigation
    fun navigateTo(screen: ZenScreen) {
        stopTimer()
        _currentScreen.value = screen
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
        when (tab) {
            MainTab.PRACTICE -> _currentScreen.value = ZenScreen.MENU
            MainTab.REVISION -> _currentScreen.value = ZenScreen.REVISION
            MainTab.HISTORY -> _currentScreen.value = ZenScreen.HISTORY
            MainTab.STATS -> _currentScreen.value = ZenScreen.STATS
            MainTab.SETTINGS -> _currentScreen.value = ZenScreen.SETTINGS
        }
    }

    fun onSelectMode(mode: GameMode) {
        _selectedMode.value = mode
        if (mode == GameMode.MULTIPLICATION_TABLE || mode == GameMode.FACTOR_FINDING) {
            _currentScreen.value = ZenScreen.SPECIAL_MENU
        } else {
            _currentScreen.value = ZenScreen.SETUP
        }
    }

    fun setDifficulty(d: Difficulty) { _difficulty.value = d }
    fun setDigits(d: Int) { _digits.value = d }
    fun setAllowRemainder(v: Boolean) { _allowRemainder.value = v }
    fun setAllowNegativeResults(v: Boolean) { _allowNegativeResults.value = v }
    fun setMixedOps(ops: List<Boolean>) { _mixedOps.value = ops }
    fun setTableRange(range: Pair<Int, Int>) { _tableRange.value = range }
    fun setSquareRangeType(type: String) { _squareRangeType.value = type }
    fun setCustomSquareRange(range: Pair<Int, Int>) { _customSquareRange.value = range }
    fun setSqrtRangeType(type: String) { _sqrtRangeType.value = type }
    fun setCustomSqrtRange(range: Pair<Int, Int>) { _customSqrtRange.value = range }
    fun setFractionDenRange(range: Pair<Int, Int>) { _fractionDenRange.value = range }
    fun setFractionNumRange(range: Pair<Int, Int>) { _fractionNumRange.value = range }
    fun setHistoryFilter(filter: String) { _historyFilter.value = filter }

    // Gameplay
    fun startGame() {
        _questionResults.value = emptyList()
        _score.value = 0
        _streak.value = 0
        _currentQuestionIndex.value = 1
        _userInput.value = ""
        _feedback.value = FeedbackType.NONE
        isProcessingAnswer = false

        generateNextQuestion(1)
        _currentScreen.value = ZenScreen.PLAYING
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        questionStartTimeMs = System.currentTimeMillis()
        val limit = settingsFlow.value.timeLimit
        _timeRemaining.value = limit
        _currentQuestionElapsedMs.value = 0L

        timerJob = viewModelScope.launch {
            while (true) {
                delay(100L)
                val elapsed = System.currentTimeMillis() - questionStartTimeMs
                _currentQuestionElapsedMs.value = elapsed
                val remaining = (limit - (elapsed / 1000).toInt()).coerceAtLeast(0)
                _timeRemaining.value = remaining

                if (remaining == 0 && _feedback.value == FeedbackType.NONE && !isProcessingAnswer) {
                    handleTimeout()
                    break
                }
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun handleKeyPress(key: String) {
        if (_feedback.value != FeedbackType.NONE || isProcessingAnswer) return

        when (key) {
            "enter" -> submitAnswer()
            "backspace" -> {
                if (_userInput.value.isNotEmpty()) {
                    _userInput.value = _userInput.value.dropLast(1)
                }
            }
            "-" -> {
                val mode = _selectedMode.value
                val allowNeg = _allowNegativeResults.value
                if ((mode == GameMode.SUBTRACTION && allowNeg) ||
                    mode in listOf(GameMode.CHAIN_CALCULATION, GameMode.APPROXIMATION, GameMode.NUMBER_SERIES)
                ) {
                    val current = _userInput.value
                    _userInput.value = if (current.startsWith("-")) current.drop(1) else "-$current"
                }
            }
            "/" -> {
                if (_selectedMode.value == GameMode.FRACTION && !_userInput.value.contains("/") && _userInput.value.isNotEmpty()) {
                    _userInput.value = _userInput.value + "/"
                }
            }
            "." -> {
                if (!_userInput.value.contains(".") && !_userInput.value.contains("/")) {
                    _userInput.value = _userInput.value + "."
                }
            }
            else -> {
                if (key.length == 1 && key[0].isDigit()) {
                    if (_userInput.value.length < 12) {
                        _userInput.value = _userInput.value + key
                    }
                }
            }
        }
    }

    fun submitAnswer() {
        if (isProcessingAnswer || _feedback.value != FeedbackType.NONE || _userInput.value.isEmpty()) return
        isProcessingAnswer = true
        stopTimer()

        val problem = _currentProblem.value ?: return
        val input = _userInput.value.trim()
        val mode = _selectedMode.value

        var isCorrect = false
        if (mode == GameMode.FRACTION) {
            if (!problem.answer.contains("/")) {
                // Expected answer is decimal
                val u = input.toDoubleOrNull()
                val c = problem.answer.toDoubleOrNull()
                isCorrect = u != null && c != null && abs(u - c) < 0.001
            } else {
                // Expected answer is fraction string (e.g. "3/4")
                if (input == problem.answer) {
                    isCorrect = true
                } else if (input.contains("/")) {
                    val partsU = input.split("/")
                    val partsC = problem.answer.split("/")
                    val uNum = partsU.getOrNull(0)?.toDoubleOrNull()
                    val uDen = partsU.getOrNull(1)?.toDoubleOrNull()
                    val cNum = partsC.getOrNull(0)?.toDoubleOrNull()
                    val cDen = partsC.getOrNull(1)?.toDoubleOrNull()
                    if (uNum != null && uDen != null && cNum != null && cDen != null && uDen != 0.0 && cDen != 0.0) {
                        isCorrect = abs((uNum / uDen) - (cNum / cDen)) < 0.0001
                    }
                }
            }
        } else if (mode == GameMode.DIVISION && _allowRemainder.value) {
            val u = input.toDoubleOrNull()
            isCorrect = u != null && abs(u - problem.numericAnswer) < 0.001
        } else {
            val u = input.toLongOrNull()
            val c = problem.numericAnswer.toLong()
            isCorrect = u != null && u == c
        }

        recordResult(isCorrect, input, timedOut = false)
        _feedback.value = if (isCorrect) FeedbackType.CORRECT else FeedbackType.INCORRECT

        if (isCorrect) {
            val newStreak = _streak.value + 1
            _streak.value = newStreak
            preferencesManager.updateBestStreak(newStreak)
        } else {
            _streak.value = 0
        }

        viewModelScope.launch {
            delay(800L)
            advanceToNext()
        }
    }

    private fun handleTimeout() {
        if (isProcessingAnswer) return
        isProcessingAnswer = true
        stopTimer()

        recordResult(isCorrect = false, userAns = null, timedOut = true)
        _feedback.value = FeedbackType.TIMEOUT
        _streak.value = 0

        viewModelScope.launch {
            delay(1200L)
            advanceToNext()
        }
    }

    private fun recordResult(isCorrect: Boolean, userAns: String?, timedOut: Boolean) {
        val problem = _currentProblem.value ?: return
        val elapsedSec = if (timedOut) {
            settingsFlow.value.timeLimit
        } else {
            ((System.currentTimeMillis() - questionStartTimeMs) / 1000).toInt().coerceAtLeast(1)
        }

        val result = QuestionResult(
            num1 = problem.num1,
            num2 = problem.num2,
            operation = problem.operation,
            correctAnswer = problem.answer,
            userAnswer = userAns,
            isCorrect = isCorrect,
            timeTaken = elapsedSec,
            timedOut = timedOut
        )
        _questionResults.value = _questionResults.value + result
        if (isCorrect) {
            _score.value = _score.value + 1
        }
    }

    private fun advanceToNext() {
        val totalQ = settingsFlow.value.totalQuestions
        val currentIndex = _currentQuestionIndex.value

        if (currentIndex >= totalQ) {
            stopTimer()
            completeSession()
        } else {
            _currentQuestionIndex.value = currentIndex + 1
            generateNextQuestion(currentIndex + 1)
            startTimer()
        }
    }

    private fun completeSession() {
        val results = _questionResults.value
        val totalQ = results.size
        val correctCount = results.count { it.isCorrect }
        val avgTimeMs = if (totalQ > 0) results.sumOf { it.timeTaken.toLong() * 1000L } / totalQ.toDouble() else 0.0
        val durationTotal = results.sumOf { it.timeTaken.toDouble() }

        val sessionId = UUID.randomUUID().toString()
        val isoDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())

        val sessionEntity = SessionEntity(
            id = sessionId,
            date = isoDate,
            mode = _selectedMode.value.id,
            totalQuestions = totalQ,
            correct = correctCount,
            avgTimeMs = avgTimeMs,
            difficulty = _difficulty.value.key,
            duration = durationTotal
        )

        val questionEntities = results.map { r ->
            QuestionRecordEntity(
                sessionId = sessionId,
                num1 = r.num1,
                num2 = r.num2,
                operation = r.operation,
                correctAnswer = r.correctAnswer,
                userAnswer = r.userAnswer,
                timeMs = (r.timeTaken * 1000).toLong(),
                isCorrect = r.isCorrect,
                mode = _selectedMode.value.id,
                difficulty = _difficulty.value.key
            )
        }

        viewModelScope.launch {
            repository.insertSession(sessionEntity, questionEntities)
        }

        preferencesManager.incrementDailyProgress()

        // Adaptive Difficulty auto-escalation
        if (settingsFlow.value.adaptiveDifficulty && totalQ > 0) {
            val accuracy = correctCount.toDouble() / totalQ.toDouble()
            if (accuracy >= 0.85) {
                when (_difficulty.value) {
                    Difficulty.EASY -> _difficulty.value = Difficulty.MEDIUM
                    Difficulty.MEDIUM -> _difficulty.value = Difficulty.HARD
                    Difficulty.HARD -> {
                        if (_digits.value < 4) {
                            _digits.value = _digits.value + 1
                        }
                    }
                }
            }
        }

        _currentScreen.value = ZenScreen.RESULTS
    }

    private fun generateNextQuestion(index: Int) {
        val mode = _selectedMode.value
        val diff = _difficulty.value
        val digitsVal = _digits.value

        val problem: MathProblem = when (mode) {
            GameMode.ADDITION -> QuestionGenerator.generateStandardQuestion("+", digitsVal, diff, _allowRemainder.value, _allowNegativeResults.value)
            GameMode.SUBTRACTION -> QuestionGenerator.generateStandardQuestion("-", digitsVal, diff, _allowRemainder.value, _allowNegativeResults.value)
            GameMode.MULTIPLICATION -> QuestionGenerator.generateStandardQuestion("*", digitsVal, diff, _allowRemainder.value, _allowNegativeResults.value)
            GameMode.DIVISION -> QuestionGenerator.generateStandardQuestion("/", digitsVal, diff, _allowRemainder.value, _allowNegativeResults.value)
            GameMode.MIXED -> {
                val ops = listOf("+", "-", "*", "/")
                val enabled = ops.filterIndexed { i, _ -> _mixedOps.value.getOrElse(i) { true } }
                val op = if (enabled.isNotEmpty()) enabled.random() else "+"
                QuestionGenerator.generateStandardQuestion(op, digitsVal, diff, _allowRemainder.value, _allowNegativeResults.value)
            }
            GameMode.MULTIPLICATION_TABLE -> QuestionGenerator.generateTableQuestion(_tableRange.value)
            GameMode.FACTOR_FINDING -> QuestionGenerator.generateFactorFindingQuestion(_tableRange.value)
            GameMode.SQUARE -> {
                val range = if (_squareRangeType.value == "fixed") Pair(1, 25) else _customSquareRange.value
                QuestionGenerator.generateSquareQuestion(range)
            }
            GameMode.SQUARE_ROOT -> {
                val range = if (_sqrtRangeType.value == "custom") _customSqrtRange.value else null
                QuestionGenerator.generateSquareRootQuestion(diff, range)
            }
            GameMode.PERCENTAGE -> QuestionGenerator.generatePercentageQuestion(diff)
            GameMode.APPROXIMATION -> QuestionGenerator.generateApproximationQuestion(diff, _allowNegativeResults.value)
            GameMode.NUMBER_SERIES -> QuestionGenerator.generateNumberSeriesQuestion(diff)
            GameMode.RATIO -> QuestionGenerator.generateRatioQuestion(diff)
            GameMode.CHAIN_CALCULATION -> QuestionGenerator.generateChainCalculationQuestion(diff)
            GameMode.FRACTION -> {
                val gen = FractionsPool.generateFractionQuestion(
                    _fractionNumRange.value.first, _fractionNumRange.value.second,
                    _fractionDenRange.value.first, _fractionDenRange.value.second
                )
                MathProblem(
                    num1 = 0.0,
                    num2 = 0.0,
                    operation = if (gen.isFractionToDecimal) "Fraction to Decimal" else "Decimal to Fraction",
                    questionDisplay = gen.question,
                    answer = gen.answer,
                    numericAnswer = 0.0
                )
            }
        }

        _currentProblem.value = problem
        _userInput.value = ""
        _feedback.value = FeedbackType.NONE
        isProcessingAnswer = false
    }

    fun onQuitGame() {
        stopTimer()
        _currentScreen.value = ZenScreen.MENU
    }

    fun onPlayAgain() {
        startGame()
    }

    fun toggleTheme() {
        preferencesManager.toggleTheme()
    }

    fun updateDailyGoal(goal: Int) {
        preferencesManager.updateDailyGoal(goal)
    }

    fun saveSettings(newSettings: GameSettings) {
        preferencesManager.saveSettings(newSettings)
        if (newSettings.notificationsEnabled) {
            NotificationScheduler.scheduleReminders(
                getApplication(),
                newSettings.notificationTimes
            )
        } else {
            NotificationScheduler.cancelAllReminders(getApplication())
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    suspend fun exportBackup(): String {
        return repository.exportBackupJson()
    }

    suspend fun importBackup(jsonStr: String) {
        repository.importBackupJson(jsonStr)
    }
}
