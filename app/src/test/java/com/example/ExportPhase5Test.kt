package com.example

import com.example.data.QuestionRecordEntity
import com.example.data.SessionEntity
import com.example.domain.GameSettings
import com.example.domain.drills.DrillRegistry
import com.example.export.ExportFileManager
import com.example.export.ExportFormat
import com.example.export.MarkdownReportRenderer
import com.example.export.StatsSnapshotBuilder
import com.example.export.pdf.PdfReportRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.TimeZone

class ExportPhase5UnitTest {

    private fun sampleDataset(): Pair<List<SessionEntity>, List<QuestionRecordEntity>> {
        val sessions = listOf(
            SessionEntity(
                id = "s_norm_1",
                date = "2026-11-08T10:00:00.000Z",
                mode = "square",
                totalQuestions = 4,
                correct = 3,
                avgTimeMs = 2100.0,
                difficulty = "medium",
                duration = 8.4,
                localDate = "2026-11-08",
                sessionType = SessionEntity.SESSION_TYPE_NORMAL
            ),
            SessionEntity(
                id = "s_sr_1",
                date = "2026-11-09T14:30:00.000Z",
                mode = "speed-round",
                totalQuestions = 3,
                correct = 1,
                avgTimeMs = 1800.0,
                difficulty = "medium",
                duration = 60.0,
                localDate = "2026-11-09",
                sessionType = SessionEntity.SESSION_TYPE_SPEED_ROUND
            ),
            SessionEntity(
                id = "s_mr_1",
                date = "2026-11-09T15:00:00.000Z",
                mode = "mistake-review",
                totalQuestions = 2,
                correct = 2,
                avgTimeMs = 1600.0,
                difficulty = "mixed",
                duration = 3.2,
                localDate = "2026-11-09",
                sessionType = SessionEntity.SESSION_TYPE_MISTAKE_REVIEW,
                sourceSessionId = "s_sr_1",
                scopeLabel = "Session Mistakes: Squares & Square Roots, pH & pOH Calculations (2Q)"
            )
        )

        val questions = listOf(
            // Square drill questions
            QuestionRecordEntity(
                id = 1L,
                sessionId = "s_norm_1",
                num1 = 15.0,
                num2 = 2.0,
                operation = "^2",
                correctAnswer = "225",
                userAnswer = "225",
                timeMs = 1500L,
                isCorrect = true,
                mode = "square",
                difficulty = "medium",
                questionText = "15²",
                topicTag = "square",
                drillMode = "square",
                answerFormat = "typed"
            ),
            QuestionRecordEntity(
                id = 2L,
                sessionId = "s_norm_1",
                num1 = 5.0,
                num2 = 2.0,
                operation = "√",
                correctAnswer = "2.24",
                userAnswer = "2.35", // Near-Miss / Rounding Drift
                timeMs = 3100L,
                isCorrect = false,
                mode = "square",
                difficulty = "medium",
                questionText = "√5 = ?",
                topicTag = "sqrt-irrational",
                drillMode = "square",
                answerFormat = "typed"
            ),
            // Speed round questions (MCQ + Typed)
            QuestionRecordEntity(
                id = 3L,
                sessionId = "s_sr_1",
                num1 = 2.0,
                num2 = -4.0,
                operation = "pH",
                correctAnswer = "3.7",
                userAnswer = "-3.7", // Sign Error
                timeMs = 2200L,
                isCorrect = false,
                mode = "speed-round",
                difficulty = "medium",
                questionText = "[H+] = 2×10^-4 M, pH = ?",
                topicTag = "ph-sci-to-ph",
                drillMode = "ph-poh",
                answerFormat = "mcq"
            ),
            QuestionRecordEntity(
                id = 4L,
                sessionId = "s_sr_1",
                num1 = 7.2,
                num2 = -4.0,
                operation = "Sci. Notation",
                correctAnswer = "7.2×10^-4",
                userAnswer = "7.2×10^-3", // Order-of-Magnitude / Exponent Error
                timeMs = 2500L,
                isCorrect = false,
                mode = "speed-round",
                difficulty = "medium",
                questionText = "Normalize 0.00072",
                topicTag = "sci-normalize",
                drillMode = "scientific-notation",
                answerFormat = "typed"
            ),
            QuestionRecordEntity(
                id = 5L,
                sessionId = "s_sr_1",
                num1 = 12.0,
                num2 = 12.0,
                operation = "*",
                correctAnswer = "144",
                userAnswer = null, // Timeout / Unanswered
                timeMs = 5000L,
                isCorrect = false,
                mode = "speed-round",
                difficulty = "medium",
                questionText = "12 × 12",
                topicTag = "multiplication",
                drillMode = "multiplication",
                answerFormat = "typed"
            ),
            // Mistake review resolution
            QuestionRecordEntity(
                id = 6L,
                sessionId = "s_mr_1",
                num1 = 5.0,
                num2 = 2.0,
                operation = "√",
                correctAnswer = "2.24",
                userAnswer = "2.24",
                timeMs = 1600L,
                isCorrect = true,
                mode = "mistake-review",
                difficulty = "mixed",
                questionText = "√5 = ?",
                topicTag = "sqrt-irrational",
                drillMode = "square",
                answerFormat = "typed"
            )
        )
        return sessions to questions
    }

    @Test
    fun `wilson95LowerBoundPct penalizes small samples and increases with n`() {
        assertEquals(0.0, StatsSnapshotBuilder.wilson95LowerBoundPct(0, 0), 1e-6)
        val smallSample = StatsSnapshotBuilder.wilson95LowerBoundPct(2, 2)
        val largeSample = StatsSnapshotBuilder.wilson95LowerBoundPct(50, 50)
        assertTrue("Expected 2/2 ($smallSample) < 50/50 ($largeSample)", smallSample < largeSample)
        assertTrue("Expected 2/2 LB > 30%", smallSample > 30.0)
        assertTrue("Expected 50/50 LB > 90%", largeSample > 90.0)
    }

    @Test
    fun `StatsSnapshotBuilder aggregates overview drills mistake patterns and resolved status`() {
        val (sessions, questions) = sampleDataset()
        val snapshot = StatsSnapshotBuilder.build(
            sessions = sessions,
            questions = questions,
            settings = GameSettings(dailyGoal = 2),
            nowMs = 1794240000000L, // 2026-11-09
            timeZone = TimeZone.getTimeZone("UTC")
        )

        assertEquals(3, snapshot.overview.totalSessions)
        assertEquals(1, snapshot.overview.normalSessions)
        assertEquals(1, snapshot.overview.speedRoundSessions)
        assertEquals(1, snapshot.overview.mistakeReviewSessions)

        // Check mistake pattern classification
        val patternNames = snapshot.mistakePatterns.map { it.patternType }.toSet()
        assertTrue(patternNames.contains("Sign Error"))
        assertTrue(patternNames.contains("Order-of-Magnitude / Exponent Error"))
        assertTrue(patternNames.contains("Timeout / Unanswered"))
        assertTrue(patternNames.contains("Near-Miss / Rounding Drift"))

        // Check resolved vs unresolved in mistake log: √5 was answered right in id=6L so it's resolved
        val sqrt5Entry = snapshot.mistakes.first { it.questionText == "√5 = ?" }
        assertTrue(sqrt5Entry.isResolved)
        val phEntry = snapshot.mistakes.first { it.questionText.contains("[H+]") }
        assertTrue(!phEntry.isResolved)
    }

    @Test
    fun `MarkdownReportRenderer produces self-contained AI coaching report with all sections and drills`() {
        val (sessions, questions) = sampleDataset()
        val snapshot = StatsSnapshotBuilder.build(
            sessions = sessions,
            questions = questions,
            settings = GameSettings(dailyGoal = 2),
            nowMs = 1794240000000L,
            timeZone = TimeZone.getTimeZone("UTC")
        )
        val md = MarkdownReportRenderer.render(snapshot)

        assertTrue(md.startsWith("---\n"))
        assertTrue(md.contains("report_type: \"zen_math_ai_coaching_report\""))
        assertTrue(md.contains("## 1. Instructions for AI Agents & LLMs"))
        assertTrue(md.contains("## 2. App Overview, Glossary & Scoring Mechanics"))
        assertTrue(md.contains("## 3. Complete ZenMath Drill Handbook"))
        assertTrue(md.contains("## 4. Executive Performance Summary"))
        assertTrue(md.contains("## 5. Performance Breakdown by Category, Drill & Sub-Topic"))
        assertTrue(md.contains("## 6. Temporal Trends & Practice Habits"))
        assertTrue(md.contains("## 7. Diagnostic Findings: Strengths, Weaknesses & Error Patterns"))
        assertTrue(md.contains("## 8. Item-Level Mistake & Weak-Spot Log"))
        assertTrue(md.contains("## 9. Recent Session History"))

        // Every active drill in DrillRegistry must be documented in Section 3
        for (spec in DrillRegistry.activeDrills) {
            assertTrue("Expected Markdown handbook to include ${spec.title}", md.contains(spec.title))
        }

        // Mistake review session scope label should appear in Section 9
        assertTrue(md.contains("Session Mistakes: Squares & Square Roots, pH & pOH Calculations (2Q)"))
    }
}

@RunWith(SafeRobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.LEGACY)
class ExportPhase5RobolectricTest {

    @Test
    fun `PdfReportRenderer renders valid multi-page A4 PDF document`() {
        val sessions = listOf(
            SessionEntity(
                id = "s1",
                date = "2026-11-09T10:00:00.000Z",
                mode = "constants",
                totalQuestions = 10,
                correct = 8,
                avgTimeMs = 2400.0,
                difficulty = "medium",
                duration = 24.0,
                localDate = "2026-11-09",
                sessionType = SessionEntity.SESSION_TYPE_NORMAL
            )
        )
        val questions = listOf(
            QuestionRecordEntity(
                id = 1L,
                sessionId = "s1",
                num1 = 3.14159,
                num2 = 0.0,
                operation = "Constants",
                correctAnswer = "3.1416",
                userAnswer = "3.1416",
                timeMs = 1800L,
                isCorrect = true,
                mode = "constants",
                difficulty = "medium",
                questionText = "Pi (π)",
                topicTag = "math-constant-pi",
                drillMode = "constants"
            ),
            QuestionRecordEntity(
                id = 2L,
                sessionId = "s1",
                num1 = 6.63,
                num2 = -34.0,
                operation = "Constants",
                correctAnswer = "6.63×10^-34",
                userAnswer = "6.63×10^-31",
                timeMs = 3200L,
                isCorrect = false,
                mode = "constants",
                difficulty = "medium",
                questionText = "Planck constant (h)",
                topicTag = "constant-planck_h",
                drillMode = "constants"
            )
        )

        val snapshot = StatsSnapshotBuilder.build(
            sessions = sessions,
            questions = questions,
            settings = GameSettings(dailyGoal = 3),
            nowMs = 1794240000000L,
            timeZone = TimeZone.getTimeZone("UTC")
        )

        val baos = ByteArrayOutputStream()
        val pageCount = PdfReportRenderer.renderToStream(snapshot, baos)
        val pdfBytes = baos.toByteArray()

        assertTrue("Expected at least 4 pages in PDF report, got $pageCount", pageCount >= 4)
        assertTrue("Expected non-empty PDF byte array (> 1000 bytes), got ${pdfBytes.size}", pdfBytes.size > 1000)
        val header = String(pdfBytes, 0, 5, Charsets.US_ASCII)
        assertEquals("%PDF-", header)

        val mdBytes = ExportFileManager.renderBytes(snapshot, ExportFormat.MARKDOWN)
        assertTrue(mdBytes.size > 2000)
    }
}
