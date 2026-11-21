package com.example

import com.example.data.CourseValuesData
import com.example.data.MemoryTableCategory
import com.example.domain.AnswerChecker
import com.example.domain.AnswerSpec
import com.example.domain.Difficulty
import com.example.domain.FractionsPool
import com.example.domain.GameMode
import com.example.domain.MathProblem
import com.example.domain.MultiplicationNumberType
import com.example.domain.QuestionGenerator
import com.example.domain.SciNotationSubtype
import com.example.domain.SquaresSubtype
import com.example.domain.TolerancePreset
import com.example.domain.drills.TopicTagResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt
import kotlin.math.sqrt

class DrillsPhase1And2Test {

    @Test
    fun `legacy modes are hidden from home and mapped to canonical active modes`() {
        assertTrue(GameMode.REFERENCE_VALUES.isLegacy)
        assertTrue(GameMode.REFERENCE_VALUES.isHiddenFromHome)
        assertTrue(GameMode.MATH_CONSTANTS.isLegacy)
        assertTrue(GameMode.MATH_CONSTANTS.isHiddenFromHome)
        assertTrue(GameMode.SQUARE_ROOT.isLegacy)
        assertTrue(GameMode.SQUARE_ROOT.isHiddenFromHome)

        assertEquals(GameMode.REFERENCE_VALUES, TopicTagResolver.canonicalActiveMode(GameMode.REFERENCE_VALUES))
        assertEquals(GameMode.CONSTANTS, TopicTagResolver.canonicalActiveMode(GameMode.MATH_CONSTANTS))
        assertEquals(GameMode.SQUARE, TopicTagResolver.canonicalActiveMode(GameMode.SQUARE_ROOT))
    }

    @Test
    fun `Constants generator supports Mathematics category mathOnly and mixed`() {
        // 1. mathOnly = true produces only math-constant-* questions
        val seenMathIds = mutableSetOf<String>()
        repeat(30) {
            val q = QuestionGenerator.generateConstantQuestion(mathOnly = true)
            assertTrue("Expected math-constant-* tag, got ${q.topicTag}", q.topicTag.startsWith("math-constant-"))
            assertEquals("Mathematics", q.subjectLabel)
            assertEquals("Constants", q.operation)
            assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
            seenMathIds.add(q.topicTag)
        }
        assertTrue("Expected multiple math constants from pool of 4, got $seenMathIds", seenMathIds.size >= 2)

        // 2. includeMathConstants = true, mathOnly = false produces both HSC constants and Math constants
        var mathCount = 0
        var hscCount = 0
        repeat(120) {
            val q = QuestionGenerator.generateConstantQuestion(
                includeMathConstants = true,
                mathOnly = false
            )
            if (q.topicTag.startsWith("math-constant-")) {
                mathCount++
            } else {
                hscCount++
            }
            assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
        }
        assertTrue("Expected some math constants in mixed mode, got $mathCount", mathCount > 0)
        assertTrue("Expected some HSC constants in mixed mode, got $hscCount", hscCount > 0)
    }

    @Test
    fun `Squares generator supports SQUARES PERFECT_ROOTS IRRATIONAL_ROOTS and ALL`() {
        assertTrue("GameMode.SQUARE must support MCQ", GameMode.SQUARE.supportsMcq)

        // 1. SQUARES subtype with custom range
        repeat(20) {
            val q = QuestionGenerator.generateSquaresQuestion(SquaresSubtype.SQUARES, Pair(6, 14))
            assertEquals("square", q.topicTag)
            val n = q.num1.roundToInt()
            assertTrue("Expected n in 6..14, got $n", n in 6..14)
            assertEquals((n * n).toString(), q.answer)
            assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
        }

        // 2. PERFECT_ROOTS subtype with custom range
        repeat(20) {
            val q = QuestionGenerator.generateSquaresQuestion(SquaresSubtype.PERFECT_ROOTS, Pair(5, 18))
            assertEquals("square-root", q.topicTag)
            val root = q.answer.toInt()
            assertTrue("Expected root in 5..18, got $root", root in 5..18)
            assertEquals("√${root * root}", q.questionDisplay)
            assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
        }

        // 3. IRRATIONAL_ROOTS subtype (√2 .. √10 with ±0.05 tolerance)
        repeat(30) {
            val q = QuestionGenerator.generateSquaresQuestion(SquaresSubtype.IRRATIONAL_ROOTS)
            assertEquals("sqrt-irrational", q.topicTag)
            val n = q.num1.roundToInt()
            assertTrue("Expected n in 2..10, got $n", n in 2..10)
            val expectedCourse = CourseValuesData.lookup(MemoryTableCategory.SQUARE_ROOT, n)!!.courseValue
            assertEquals(expectedCourse, q.numericAnswer, 1e-6)
            assertTrue("Course value must pass", AnswerChecker.check(q.answer, q.answerSpec))
            assertTrue("Exact sqrt($n) must pass", AnswerChecker.check(sqrt(n.toDouble()).toString(), q.answerSpec))
            assertFalse("Out-of-tolerance value must fail", AnswerChecker.check((expectedCourse + 0.12).toString(), q.answerSpec))
        }

        // 4. ALL subtype produces all three tags
        val seenTags = mutableSetOf<String>()
        repeat(80) {
            val q = QuestionGenerator.generateSquaresQuestion(SquaresSubtype.ALL, Pair(1, 25))
            seenTags.add(q.topicTag)
            val mcq = QuestionGenerator.attachMcqOptions(q)
            assertNotNull(mcq.mcqOptions)
            assertEquals(4, mcq.mcqOptions!!.distinct().size)
            assertTrue(AnswerChecker.check(q.answer, mcq.answerSpec))
        }
        assertEquals(setOf("square", "square-root", "sqrt-irrational"), seenTags)
    }

    @Test
    fun `Speed Round MCQ produces 4 distinct valid options across every topic mode including fractions`() {
        val sampleGenerators: List<Pair<GameMode, () -> MathProblem>> = listOf(
            GameMode.ADDITION to { QuestionGenerator.generateStandardQuestion("+", 2, Difficulty.MEDIUM, false, false) },
            GameMode.SUBTRACTION to { QuestionGenerator.generateStandardQuestion("-", 2, Difficulty.MEDIUM, false, true) },
            GameMode.MULTIPLICATION to { QuestionGenerator.generateStandardQuestion("*", 2, Difficulty.MEDIUM, false, false) },
            GameMode.DIVISION to { QuestionGenerator.generateStandardQuestion("/", 2, Difficulty.MEDIUM, true, false) },
            GameMode.SQUARE to { QuestionGenerator.generateSquaresQuestion(SquaresSubtype.ALL, Pair(1, 25)) },
            GameMode.CUBES_AND_POWERS to { QuestionGenerator.generateCubesPowersQuestion() },
            GameMode.FRACTION to {
                val gen = FractionsPool.generateFractionQuestion(1, 9, 2, 12, useCourseValues = true)
                MathProblem(
                    num1 = gen.numerator.toDouble(),
                    num2 = gen.denominator.toDouble(),
                    operation = if (gen.isFractionToDecimal) "Fraction to Decimal" else "Decimal to Fraction",
                    questionDisplay = gen.question,
                    answer = gen.answer,
                    numericAnswer = gen.numerator.toDouble() / gen.denominator.toDouble(),
                    topicTag = "fraction",
                    answerSpec = AnswerSpec.FractionSpec(
                        numerator = gen.numerator,
                        denominator = gen.denominator,
                        isFractionToDecimal = gen.isFractionToDecimal,
                        useCourseValues = true
                    ),
                    solutionSteps = gen.solutionSteps
                )
            },
            GameMode.PERCENTAGE to { QuestionGenerator.generatePercentageQuestion(Difficulty.MEDIUM) },
            GameMode.APPROXIMATION to { QuestionGenerator.generateApproximationQuestion(Difficulty.MEDIUM, false) },
            GameMode.MEMORY_TABLES to { QuestionGenerator.generateMemoryTableQuestion() },
            GameMode.SCIENTIFIC_NOTATION to { QuestionGenerator.generateScientificNotationQuestion() },
            GameMode.PH_POH to { QuestionGenerator.generatePhQuestion() },
            GameMode.COMPOSITE_LOGS_ROOTS to { QuestionGenerator.generateCompositeLogRootQuestion() },
            GameMode.CONSTANTS to { QuestionGenerator.generateConstantQuestion(includeMathConstants = true) }
        )

        for ((mode, generator) in sampleGenerators) {
            repeat(15) {
                val base = generator()
                val mcq = QuestionGenerator.attachMcqOptions(base, TolerancePreset.NORMAL)
                val opts = mcq.mcqOptions
                assertNotNull("Expected mcqOptions for mode ${mode.id}", opts)
                assertEquals("Expected 4 options for mode ${mode.id}", 4, opts!!.size)
                assertEquals("Expected 4 distinct options for mode ${mode.id}: $opts", 4, opts.distinct().size)
                assertTrue("Options $opts must contain answer ${base.answer}", opts.contains(base.answer))

                for (opt in opts) {
                    val isAccepted = AnswerChecker.check(opt, mcq.answerSpec, TolerancePreset.NORMAL)
                    assertEquals(
                        "Option '$opt' in mode ${mode.id} (correct='${base.answer}') check mismatch",
                        opt == base.answer,
                        isAccepted
                    )
                }
            }
        }
    }

    @Test
    fun `Decimal multiplication supports INTEGERS_ONLY DECIMALS_ONLY and MIXED across all 3 categories`() {
        // 1. INTEGERS_ONLY always produces topicTag = "multiplication" with Exact integer answers
        repeat(25) {
            val q = QuestionGenerator.generateStandardQuestion(
                operation = "*",
                digits = 2,
                diff = Difficulty.MEDIUM,
                allowRemainder = false,
                allowNegativeResults = false,
                multiplicationNumberType = MultiplicationNumberType.INTEGERS_ONLY
            )
            assertEquals("multiplication", q.topicTag)
            assertTrue(q.answerSpec is AnswerSpec.Exact)
            assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
        }

        // 2. DECIMALS_ONLY always produces topicTag = "multiplication-decimal" across all 3 categories
        for (cat in listOf(0, 1, 2)) {
            for (diff in Difficulty.entries) {
                repeat(15) {
                    val q = QuestionGenerator.generateDecimalMultiplicationQuestion(
                        digits = 2,
                        diff = diff,
                        categoryOverride = cat
                    )
                    assertEquals("multiplication-decimal", q.topicTag)
                    assertEquals(GameMode.MULTIPLICATION, TopicTagResolver.inferModeFromTag(q.topicTag))
                    assertTrue("Expected question display to contain '.', got ${q.questionDisplay}", q.questionDisplay.contains("."))
                    assertTrue("Correct answer '${q.answer}' must pass", AnswerChecker.check(q.answer, q.answerSpec))
                    assertFalse("Wrong answer must fail", AnswerChecker.check((q.numericAnswer + 1.0).toString(), q.answerSpec))
                }
            }
        }

        // 3. MIXED produces both "multiplication" and "multiplication-decimal"
        val seenTags = mutableSetOf<String>()
        repeat(50) {
            val q = QuestionGenerator.generateStandardQuestion(
                operation = "*",
                digits = 2,
                diff = Difficulty.MEDIUM,
                allowRemainder = false,
                allowNegativeResults = false,
                multiplicationNumberType = MultiplicationNumberType.MIXED
            )
            seenTags.add(q.topicTag)
            assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
        }
        assertEquals(setOf("multiplication", "multiplication-decimal"), seenTags)
    }

    @Test
    fun `Scientific Notation supports all 9 subtypes including pure 10^x laws add_sub reciprocals multi-term and decimal shift`() {
        val concreteSubtypes = SciNotationSubtype.entries.filter { it != SciNotationSubtype.ALL }
        assertEquals(9, concreteSubtypes.size)

        for (sub in concreteSubtypes) {
            repeat(20) {
                val q = QuestionGenerator.generateScientificNotationQuestion(sub)
                assertEquals(
                    "Expected topicTag '${q.topicTag}' to resolve to SCIENTIFIC_NOTATION",
                    GameMode.SCIENTIFIC_NOTATION,
                    TopicTagResolver.inferModeFromTag(q.topicTag)
                )
                assertTrue("Correct answer '${q.answer}' must pass for $sub (${q.questionDisplay})", AnswerChecker.check(q.answer, q.answerSpec))
                assertTrue("Solution steps must be non-empty for $sub", q.solutionSteps.isNotEmpty())

                // Verify MCQ generation works cleanly for every subtype
                val mcq = QuestionGenerator.attachMcqOptions(q, TolerancePreset.NORMAL)
                assertNotNull(mcq.mcqOptions)
                assertEquals(4, mcq.mcqOptions!!.distinct().size)
                assertTrue(AnswerChecker.check(q.answer, mcq.answerSpec))
            }
        }

        // ALL subtype samples across the full pool
        val seenTags = mutableSetOf<String>()
        repeat(200) {
            val q = QuestionGenerator.generateScientificNotationQuestion(SciNotationSubtype.ALL)
            seenTags.add(q.topicTag)
            assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
        }
        assertTrue("Expected at least 6 distinct sci-* tags from ALL, got $seenTags", seenTags.size >= 6)
    }
}
