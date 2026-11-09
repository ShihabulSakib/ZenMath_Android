package com.example

import com.example.data.QuestionRecordEntity
import com.example.domain.AnswerChecker
import com.example.domain.AnswerSpec
import com.example.domain.GameMode
import com.example.domain.MathProblem
import com.example.domain.MistakeSelector
import com.example.domain.ProblemCodec
import com.example.domain.QuestionResult
import com.example.domain.drills.DrillCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MistakeReviewPhase3And4Test {

    @Test
    fun `fromSessionResults groups missed questions by category and canonical drill mode`() {
        val sqProb = MathProblem(
            num1 = 5.0,
            num2 = 2.0,
            operation = "√",
            questionDisplay = "√5",
            answer = "2.24",
            numericAnswer = 2.236,
            topicTag = "sqrt-irrational",
            answerSpec = AnswerSpec.AbsoluteTolerance(expected = 2.24, tolerance = 0.05),
            solutionSteps = listOf("√5 ≈ 2.24")
        )
        val phProb = MathProblem(
            num1 = 2.0,
            num2 = -4.0,
            operation = "pH",
            questionDisplay = "[H+] = 2 × 10^-4 M, pH = ?",
            answer = "3.7",
            numericAnswer = 3.7,
            topicTag = "ph-sci-to-ph",
            answerSpec = AnswerSpec.AbsoluteTolerance(expected = 3.7, tolerance = 0.05),
            solutionSteps = listOf("pH = 4 - log10(2) = 3.7")
        )

        val results = listOf(
            QuestionResult(
                num1 = 5.0,
                num2 = 2.0,
                operation = "√",
                correctAnswer = "2.24",
                userAnswer = "2.5",
                isCorrect = false,
                timeTaken = 4,
                timedOut = false,
                timeTakenMs = 4000L,
                questionText = "√5",
                topicTag = "sqrt-irrational",
                drillModeId = "square",
                problemJson = ProblemCodec.encode(sqProb)
            ),
            QuestionResult(
                num1 = 2.0,
                num2 = -4.0,
                operation = "pH",
                correctAnswer = "3.7",
                userAnswer = "4.3",
                isCorrect = false,
                timeTaken = 6,
                timedOut = false,
                timeTakenMs = 6000L,
                questionText = "[H+] = 2 × 10^-4 M, pH = ?",
                topicTag = "ph-sci-to-ph",
                drillModeId = "ph-poh",
                problemJson = ProblemCodec.encode(phProb)
            ),
            QuestionResult(
                num1 = 12.0,
                num2 = 12.0,
                operation = "+",
                correctAnswer = "24",
                userAnswer = "24",
                isCorrect = true,
                timeTaken = 2,
                timedOut = false,
                timeTakenMs = 2000L,
                questionText = "12 + 12",
                topicTag = "addition",
                drillModeId = "addition"
            )
        )

        val groups = MistakeSelector.fromSessionResults(results, GameMode.SPEED_ROUND)
        assertEquals(2, groups.size)

        val numbersCat = groups.first { it.category == DrillCategory.NUMBER_FACTS }
        assertEquals(1, numbersCat.drillGroups.size)
        val sqGroup = numbersCat.drillGroups.first()
        assertEquals(GameMode.SQUARE, sqGroup.drillMode)
        assertEquals(1, sqGroup.items.size)
        val sqItem = sqGroup.items.first()
        assertEquals("√5", sqItem.questionText)
        assertEquals("2.5", sqItem.latestWrongUserAnswer)
        assertEquals("2.24", sqItem.correctAnswer)
        assertTrue(AnswerChecker.check("2.236", sqItem.problem.answerSpec))

        val scienceCat = groups.first { it.category == DrillCategory.SCIENCE_MATH }
        assertEquals(GameMode.PH_POH, scienceCat.drillGroups.first().drillMode)
    }

    @Test
    fun `fromAllRecords filters resolved mistakes by default and includes them when requested`() {
        val records = listOf(
            // Q1: missed twice, still unresolved
            QuestionRecordEntity(
                id = 1L,
                sessionId = "s1",
                num1 = 7.0,
                num2 = 8.0,
                operation = "*",
                correctAnswer = "56",
                userAnswer = "54",
                timeMs = 2000L,
                isCorrect = false,
                mode = "multiplication",
                difficulty = "medium",
                questionText = "7 × 8",
                topicTag = "multiplication",
                drillMode = "multiplication"
            ),
            QuestionRecordEntity(
                id = 2L,
                sessionId = "s2",
                num1 = 7.0,
                num2 = 8.0,
                operation = "*",
                correctAnswer = "56",
                userAnswer = "58",
                timeMs = 1900L,
                isCorrect = false,
                mode = "multiplication",
                difficulty = "medium",
                questionText = "7 × 8",
                topicTag = "multiplication",
                drillMode = "multiplication"
            ),
            // Q2: missed once in legacy square-root, then answered right without hint -> resolved
            QuestionRecordEntity(
                id = 3L,
                sessionId = "s1",
                num1 = 144.0,
                num2 = 12.0,
                operation = "√",
                correctAnswer = "12",
                userAnswer = "14",
                timeMs = 2500L,
                isCorrect = false,
                mode = "square-root",
                difficulty = "medium",
                questionText = "√144",
                topicTag = "square-root",
                drillMode = "square-root"
            ),
            QuestionRecordEntity(
                id = 4L,
                sessionId = "s2",
                num1 = 144.0,
                num2 = 12.0,
                operation = "√",
                correctAnswer = "12",
                userAnswer = "12",
                timeMs = 1500L,
                isCorrect = true,
                mode = "square",
                difficulty = "medium",
                questionText = "√144",
                topicTag = "square-root",
                hintUsed = false,
                drillMode = "square"
            )
        )

        val unresolvedGroups = MistakeSelector.fromAllRecords(records, includeResolved = false)
        val unresolvedKeys = MistakeSelector.allKeys(unresolvedGroups)
        assertEquals(1, unresolvedKeys.size)
        val multItem = unresolvedGroups.first().drillGroups.first().items.first()
        assertEquals("7 × 8", multItem.questionText)
        assertEquals(2, multItem.errorCount)
        assertFalse(multItem.isResolved)

        val allGroups = MistakeSelector.fromAllRecords(records, includeResolved = true)
        val allKeys = MistakeSelector.allKeys(allGroups)
        assertEquals(2, allKeys.size)
        val resolvedItem = allGroups.first { it.category == DrillCategory.NUMBER_FACTS }
            .drillGroups.first { it.drillMode == GameMode.SQUARE }
            .items.first()
        assertEquals("√144", resolvedItem.questionText)
        assertTrue(resolvedItem.isResolved)
    }

    @Test
    fun `buildReplayQueueFromKeys deduplicates ranks by errorCount and caps at 30`() {
        val records = (1..40).map { i ->
            QuestionRecordEntity(
                id = i.toLong(),
                sessionId = "s1",
                num1 = i.toDouble(),
                num2 = 2.0,
                operation = "+",
                correctAnswer = "${i + 2}",
                userAnswer = "0",
                timeMs = 1000L,
                isCorrect = false,
                mode = "addition",
                difficulty = "easy",
                questionText = "$i + 2",
                topicTag = "addition",
                drillMode = "addition"
            )
        } + listOf(
            // Extra miss on "5 + 2" so it ranks #1
            QuestionRecordEntity(
                id = 100L,
                sessionId = "s2",
                num1 = 5.0,
                num2 = 2.0,
                operation = "+",
                correctAnswer = "7",
                userAnswer = "8",
                timeMs = 1000L,
                isCorrect = false,
                mode = "addition",
                difficulty = "easy",
                questionText = "5 + 2",
                topicTag = "addition",
                drillMode = "addition"
            )
        )

        val groups = MistakeSelector.fromAllRecords(records, includeResolved = false)
        val allKeys = MistakeSelector.allKeys(groups)
        assertEquals(40, allKeys.size)

        val queue = MistakeSelector.buildReplayQueueFromKeys(groups, allKeys, cap = 30)
        assertEquals(30, queue.size)
        assertEquals("5 + 2", queue.first().questionText)
        assertEquals(2, queue.first().errorCount)

        val label = MistakeSelector.formatScopeLabel(groups, allKeys, queue.size, isSessionScope = false)
        assertEquals("Weak Spots: Addition (30Q)", label)
    }
}
