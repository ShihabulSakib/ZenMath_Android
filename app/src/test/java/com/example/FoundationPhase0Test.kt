package com.example

import com.example.data.QuestionRecordEntity
import com.example.domain.AnswerChecker
import com.example.domain.AnswerSpec
import com.example.domain.GameMode
import com.example.domain.MathProblem
import com.example.domain.ProblemCodec
import com.example.domain.TolerancePreset
import com.example.domain.drills.DrillRegistry
import com.example.domain.drills.TopicTagResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FoundationPhase0Test {

    @Test
    fun `DrillRegistry contains complete spec for every GameMode`() {
        val allModes = GameMode.entries
        assertEquals(allModes.size, DrillRegistry.all.size)

        for (mode in allModes) {
            val spec = DrillRegistry.of(mode)
            assertEquals(mode, spec.mode)
            assertTrue("Title blank for $mode", spec.title.isNotBlank())
            assertTrue("Summary blank for $mode", spec.summary.isNotBlank())
            assertTrue("Details blank for $mode", spec.details.isNotBlank())
            assertTrue("AnswerChecking blank for $mode", spec.answerChecking.isNotBlank())
            assertTrue("TimingAndScoring blank for $mode", spec.timingAndScoring.isNotBlank())
            assertTrue("QuestionFormats empty for $mode", spec.questionFormats.isNotEmpty())
            assertEquals(mode.isLegacy, spec.isLegacy)
        }
    }

    @Test
    fun `TopicTagResolver resolves compound modes and tags accurately`() {
        assertEquals(
            GameMode.PH_POH,
            TopicTagResolver.resolveDrillMode(topicTag = "ph-sci-to-ph", modeId = "speed-round")
        )
        assertEquals(
            GameMode.SCIENTIFIC_NOTATION,
            TopicTagResolver.resolveDrillMode(topicTag = "sci-mul-div", modeId = "weak-spots")
        )
        assertEquals(
            GameMode.MEMORY_TABLES,
            TopicTagResolver.resolveDrillMode(topicTag = "memory-log_table-reverse", modeId = "speed-round")
        )
        assertEquals(
            GameMode.SQUARE,
            TopicTagResolver.resolveDrillMode(topicTag = "sqrt-irrational", modeId = "speed-round")
        )
        assertEquals(
            GameMode.CONSTANTS,
            TopicTagResolver.resolveDrillMode(topicTag = "math-constant-pi", modeId = "speed-round")
        )
        assertEquals(
            GameMode.ATOMIC_MASSES,
            TopicTagResolver.resolveDrillMode(topicTag = "atomic-mass-cl", modeId = "mistake-review", drillModeId = "atomic-masses")
        )

        assertEquals(GameMode.SQUARE, TopicTagResolver.canonicalActiveMode(GameMode.SQUARE_ROOT))
        assertEquals(GameMode.CONSTANTS, TopicTagResolver.canonicalActiveMode(GameMode.MATH_CONSTANTS))
        assertTrue(TopicTagResolver.friendlyTopicLabel("ph-sci-to-ph").contains("pH"))
        assertTrue(TopicTagResolver.friendlyTopicLabel("sqrt-irrational").contains("√2–√10"))
    }

    @Test
    fun `ProblemCodec round-trips all 6 AnswerSpec variants with identical AnswerChecker behavior`() {
        val problems = listOf(
            MathProblem(
                num1 = 19.0,
                num2 = 19.0,
                operation = "²",
                questionDisplay = "19²",
                answer = "361",
                numericAnswer = 361.0,
                topicTag = "square",
                answerSpec = AnswerSpec.Exact(expected = 361L, alternateAccepted = setOf(361L)),
                solutionSteps = listOf("19 × 19 = 361")
            ),
            MathProblem(
                num1 = 7.0,
                num2 = 0.0,
                operation = "√",
                questionDisplay = "√7 = ?",
                answer = "2.65",
                numericAnswer = 2.65,
                topicTag = "sqrt-irrational",
                answerSpec = AnswerSpec.AbsoluteTolerance(
                    expected = 2.65,
                    tolerance = 0.05,
                    alsoAcceptExact = listOf(2.65, 2.64575)
                ),
                solutionSteps = listOf("√7 ≈ 2.65")
            ),
            MathProblem(
                num1 = 3.14159,
                num2 = 0.0,
                operation = "Constants",
                questionDisplay = "Pi (π)",
                answer = "3.1416",
                numericAnswer = 3.14159265,
                topicTag = "math-constant-pi",
                answerSpec = AnswerSpec.RelativeTolerance(
                    expected = 3.14159265,
                    relativeToleranceOverride = 0.01,
                    exactCourseValues = listOf(3.14, 3.1416)
                ),
                subjectLabel = "Mathematics"
            ),
            MathProblem(
                num1 = 6.63,
                num2 = -34.0,
                operation = "Constants",
                questionDisplay = "Planck constant (h)",
                answer = "6.63×10^-34",
                numericAnswer = 6.626e-34,
                topicTag = "planck_constant",
                unitText = "J·s",
                answerSpec = AnswerSpec.ScientificValue(
                    mantissa = 6.63,
                    exponent = -34,
                    relativeToleranceOverride = 0.01,
                    alternateValues = listOf(Pair(4.136, -15)),
                    alsoAcceptDoubles = listOf(6.626e-34),
                    requireNormalizedMantissa = true
                ),
                confusableIds = listOf("boltzmann_constant")
            ),
            MathProblem(
                num1 = 12.0,
                num2 = 3.0,
                operation = "Cubes & Powers",
                questionDisplay = "12^3",
                answer = "1728",
                numericAnswer = 1728.0,
                topicTag = "cubes",
                mcqOptions = listOf("1331", "1728", "2197", "1440"),
                answerSpec = AnswerSpec.Choice(
                    options = listOf("1331", "1728", "2197", "1440"),
                    correctIndex = 1,
                    correctValue = "1728"
                )
            ),
            MathProblem(
                num1 = 3.0,
                num2 = 8.0,
                operation = "Decimal to Fraction",
                questionDisplay = "0.375",
                answer = "3/8",
                numericAnswer = 0.375,
                topicTag = "fraction",
                answerSpec = AnswerSpec.FractionSpec(
                    numerator = 3,
                    denominator = 8,
                    isFractionToDecimal = false,
                    acceptCourseValues = true
                )
            )
        )

        val testInputs = listOf("361", "360", "2.65", "2.646", "3.0", "3.14", "6.63×10^-34", "4.136×10^-15", "1728", "1331", "3/8", "6/16", "1/2")

        for (p in problems) {
            val json = ProblemCodec.encode(p)
            val decoded = ProblemCodec.decode(json)
            assertNotNull("Failed to decode $json", decoded)
            decoded!!
            assertEquals(p.questionDisplay, decoded.questionDisplay)
            assertEquals(p.answer, decoded.answer)
            assertEquals(p.topicTag, decoded.topicTag)
            assertEquals(p.unitText, decoded.unitText)
            assertEquals(p.mcqOptions, decoded.mcqOptions)

            for (input in testInputs) {
                val expectedCheck = AnswerChecker.check(input, p.answerSpec, TolerancePreset.NORMAL)
                val decodedCheck = AnswerChecker.check(input, decoded.answerSpec, TolerancePreset.NORMAL)
                assertEquals("Check mismatch on '${p.questionDisplay}' with input '$input'", expectedCheck, decodedCheck)
            }
        }
    }

    @Test
    fun `ProblemCodec legacyFallback reconstructs integer, decimal, fraction, and scientific records`() {
        val intRec = QuestionRecordEntity(
            sessionId = "s1", num1 = 14.0, num2 = 14.0, operation = "²",
            correctAnswer = "196", userAnswer = "169", timeMs = 2000L, isCorrect = false,
            mode = "square", difficulty = "medium", questionText = "14²", topicTag = "square"
        )
        val intProb = ProblemCodec.fromRecordOrFallback(intRec)
        assertNotNull(intProb)
        assertTrue(AnswerChecker.check("196", intProb!!.answerSpec))
        assertFalse(AnswerChecker.check("169", intProb.answerSpec))

        val fracRec = intRec.copy(correctAnswer = "5/8", questionText = "0.625", topicTag = "fraction")
        val fracProb = ProblemCodec.fromRecordOrFallback(fracRec)
        assertNotNull(fracProb)
        assertTrue(AnswerChecker.check("5/8", fracProb!!.answerSpec))

        val sciRec = intRec.copy(correctAnswer = "6.02×10^23", questionText = "Avogadro constant (N_A)", topicTag = "avogadro_constant")
        val sciProb = ProblemCodec.fromRecordOrFallback(sciRec)
        assertNotNull(sciProb)
        assertTrue(AnswerChecker.check("6.02×10^23", sciProb!!.answerSpec))

        val emptyRec = intRec.copy(correctAnswer = "", questionText = "")
        assertNull(ProblemCodec.fromRecordOrFallback(emptyRec))
    }
}
