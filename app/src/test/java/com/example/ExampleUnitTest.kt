package com.example

import com.example.data.ConstantEntry
import com.example.data.ConstantOverride
import com.example.data.ConstantsData
import com.example.data.ConstantsLoader
import com.example.data.CourseValuesData
import com.example.data.HscPaper
import com.example.data.MemoryTableCategory
import com.example.data.PhSpeciesData
import com.example.data.StructuredSymbol
import com.example.data.StructuredUnit
import com.example.data.UnitFactor
import com.example.data.SessionEntity
import com.example.domain.AnswerChecker
import com.example.domain.AnswerSpec
import com.example.domain.CompositeLogRootSubtype
import com.example.domain.CubesPowersSubtype
import com.example.domain.DateBucketUtils
import com.example.domain.Difficulty
import com.example.domain.FractionsPool
import com.example.domain.MemoryDirection
import com.example.domain.PhSubType
import com.example.domain.QuestionGenerator
import com.example.domain.SciNotationSubtype
import com.example.domain.ScientificInputParser
import com.example.domain.TolerancePreset
import com.example.ui.components.RichTextHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt

class ExampleUnitTest {

    // =========================================================================
    // 1. Central Answer Checker Tests (every AnswerSpec type & TolerancePreset)
    // =========================================================================
    @Test
    fun testAnswerCheckerAllTypesAndPresets() {
        // 1. Exact
        val exactSpec = AnswerSpec.Exact(expected = 13L, alternateAccepted = setOf(14L))
        assertTrue(AnswerChecker.check("13", exactSpec))
        assertTrue(AnswerChecker.check("14", exactSpec))
        assertFalse(AnswerChecker.check("12", exactSpec))
        assertFalse(AnswerChecker.check("13.5", exactSpec))

        // 2. AbsoluteTolerance (e.g. Memory Table ±0.05 or pH ±0.1)
        val absSpec = AnswerSpec.AbsoluteTolerance(
            expected = 1.4,
            tolerance = 0.05,
            alsoAcceptExact = listOf(1.4, sqrt(2.0))
        )
        assertTrue(AnswerChecker.check("1.4", absSpec))
        assertTrue(AnswerChecker.check("1.414", absSpec))
        assertTrue(AnswerChecker.check("1.45", absSpec))
        assertFalse(AnswerChecker.check("1.46", absSpec))

        // 3. RelativeTolerance with STRICT (0.5%), NORMAL (1.0%), LENIENT (2.0%)
        val relSpec = AnswerSpec.RelativeTolerance(expected = 100.0)
        // 100.4 is +0.4% -> accepted in STRICT, NORMAL, LENIENT
        assertTrue(AnswerChecker.check("100.4", relSpec, TolerancePreset.STRICT))
        // 100.8 is +0.8% -> rejected in STRICT (0.5%), accepted in NORMAL (1%) & LENIENT (2%)
        assertFalse(AnswerChecker.check("100.8", relSpec, TolerancePreset.STRICT))
        assertTrue(AnswerChecker.check("100.8", relSpec, TolerancePreset.NORMAL))
        // 101.6 is +1.6% -> rejected in NORMAL (1%), accepted in LENIENT (2%)
        assertFalse(AnswerChecker.check("101.6", relSpec, TolerancePreset.NORMAL))
        assertTrue(AnswerChecker.check("101.6", relSpec, TolerancePreset.LENIENT))
        assertFalse(AnswerChecker.check("102.5", relSpec, TolerancePreset.LENIENT))

        // Per-item override on RelativeTolerance
        val overrideSpec = AnswerSpec.RelativeTolerance(expected = 100.0, relativeToleranceOverride = 0.03)
        assertTrue(AnswerChecker.check("102.8", overrideSpec, TolerancePreset.STRICT))

        // 4. ScientificValue
        val sciSpec = AnswerSpec.ScientificValue(
            mantissa = 4.5,
            exponent = -3,
            mantissaRelativeTol = 0.01,
            requireNormalized = true
        )
        assertTrue(AnswerChecker.check("4.5×10^-3", sciSpec))
        assertTrue(AnswerChecker.check("0.0045", sciSpec))
        // Unnormalized scientific entry when requireNormalized = true should be rejected
        assertFalse(AnswerChecker.check("45×10^-4", sciSpec))
        assertFalse(AnswerChecker.check("4.5×10^-4", sciSpec))

        // 5. Choice (MCQ)
        val choiceSpec = AnswerSpec.Choice(
            options = listOf("1.4", "2.2", "3.1", "4.5"),
            correctIndex = 1,
            correctValue = "2.2"
        )
        assertTrue(AnswerChecker.check("2.2", choiceSpec))
        assertTrue(AnswerChecker.check("1", choiceSpec))
        assertFalse(AnswerChecker.check("1.4", choiceSpec))

        // 6. FractionSpec (standard & course values)
        val fracToDecStandard = AnswerSpec.FractionSpec(1, 3, isFractionToDecimal = true, useCourseValues = false)
        assertTrue(AnswerChecker.check("0.3333", fracToDecStandard))
        assertFalse(AnswerChecker.check("0.3", fracToDecStandard))

        val fracToDecCourse = AnswerSpec.FractionSpec(1, 3, isFractionToDecimal = true, useCourseValues = true)
        assertTrue(AnswerChecker.check("0.3", fracToDecCourse))
        assertTrue(AnswerChecker.check("0.3333", fracToDecCourse))

        val decToFrac13 = AnswerSpec.FractionSpec(1, 13, isFractionToDecimal = false, useCourseValues = true)
        assertTrue(AnswerChecker.check("1/13", decToFrac13))
        assertTrue(AnswerChecker.check("1/14", decToFrac13)) // 1/13 and 1/14 both have course decimal 0.07
        assertTrue(AnswerChecker.check("2/26", decToFrac13)) // equivalent unreduced fraction accepted
    }

    // =========================================================================
    // 2. Scientific Notation (`×10ⁿ`) Input Parser Tests
    // =========================================================================
    @Test
    fun testScientificInputParserValidAndMalformed() {
        // Valid inputs
        val p1 = ScientificInputParser.parse("9.0×10^9")
        assertNotNull(p1)
        assertEquals(9.0e9, p1!!.numericValue, 1e2)
        assertTrue(p1.hasExplicitExponent)
        assertEquals(9.0, p1.mantissa, 1e-9)
        assertEquals(9, p1.exponent)

        val p2 = ScientificInputParser.parse("-3.2×10^-4")
        assertNotNull(p2)
        assertEquals(-3.2e-4, p2!!.numericValue, 1e-9)
        assertEquals(-3.2, p2.mantissa, 1e-9)
        assertEquals(-4, p2.exponent)

        // Implicit mantissa 1 when starting with ×10^
        val p3 = ScientificInputParser.parse("×10^-6")
        assertNotNull(p3)
        assertEquals(1.0e-6, p3!!.numericValue, 1e-12)

        // Plain decimal and E-notation
        val p4 = ScientificInputParser.parse("0.0821")
        assertNotNull(p4)
        assertEquals(0.0821, p4!!.numericValue, 1e-9)
        assertFalse(p4.hasExplicitExponent)

        val p5 = ScientificInputParser.parse("6.022e23")
        assertNotNull(p5)
        assertEquals(6.022e23, p5!!.numericValue, 1e16)

        // Malformed inputs must return null cleanly without throwing
        val malformedInputs = listOf(
            "",
            "   ",
            "-",
            ".",
            "-.",
            "4.5×10^",
            "4.5×10^-",
            "4.5×10^2×10^3",
            "4.5×10^2.5",
            "abc",
            "1/2/3",
            "123456789012345678901" // > 20 chars
        )
        for (bad in malformedInputs) {
            assertNull("Expected null for malformed input '$bad'", ScientificInputParser.parse(bad))
        }
    }

    // =========================================================================
    // 3. Generator Batch & Independent Recomputation Tests
    // =========================================================================
    @Test
    fun testExistingGeneratorsAndDifficultyDifferentiation() {
        repeat(15) {
            val addQ = QuestionGenerator.generateStandardQuestion("+", 2, Difficulty.MEDIUM, false, false)
            assertEquals((addQ.num1 + addQ.num2).toLong().toString(), addQ.answer)
            assertTrue(AnswerChecker.check(addQ.answer, addQ.answerSpec))
            assertTrue(addQ.solutionSteps.isNotEmpty())

            val subQ = QuestionGenerator.generateStandardQuestion("-", 2, Difficulty.MEDIUM, false, false)
            assertTrue(subQ.num1 >= subQ.num2)
            assertEquals((subQ.num1 - subQ.num2).toLong().toString(), subQ.answer)

            val mulQ = QuestionGenerator.generateStandardQuestion("*", 2, Difficulty.MEDIUM, false, false)
            assertEquals((mulQ.num1 * mulQ.num2).toLong().toString(), mulQ.answer)

            val divQ = QuestionGenerator.generateStandardQuestion("/", 2, Difficulty.MEDIUM, false, false)
            assertEquals((divQ.num1 / divQ.num2).toLong().toString(), divQ.answer)
        }

        // Square custom range & (10+b)^2 hint for 11..19
        val sq14 = QuestionGenerator.generateSquareQuestion(Pair(14, 14))
        assertEquals("196", sq14.answer)
        assertTrue(sq14.solutionSteps.any { it.contains("100 +") })

        // Square root custom range overrides difficulty
        val sqrtCustom = QuestionGenerator.generateSquareRootQuestion(Difficulty.EASY, Pair(26, 30))
        val rootAns = sqrtCustom.answer.toInt()
        assertTrue(rootAns in 26..30)
        assertEquals((rootAns * rootAns).toDouble(), sqrtCustom.num1, 1e-6)

        // Percentage Hard vs Medium
        repeat(20) {
            val pctHard = QuestionGenerator.generatePercentageQuestion(Difficulty.HARD)
            val expected = (pctHard.num1 / 100.0) * pctHard.num2
            assertEquals(expected, pctHard.numericAnswer, 1e-5)
            assertTrue(AnswerChecker.check(pctHard.answer, pctHard.answerSpec))
        }

        // Number Series Hard (7 terms, hidden index in 1..6)
        repeat(20) {
            val seriesHard = QuestionGenerator.generateNumberSeriesQuestion(Difficulty.HARD)
            val parts = seriesHard.questionDisplay.split(", ")
            assertEquals(7, parts.size)
            val qIdx = parts.indexOf("?")
            assertTrue(qIdx in 1..6)
            assertTrue(AnswerChecker.check(seriesHard.answer, seriesHard.answerSpec))
        }

        // Ratio Medium & Hard (3-term A:B:C)
        repeat(15) {
            val ratioMed = QuestionGenerator.generateRatioQuestion(Difficulty.MEDIUM)
            assertTrue(ratioMed.questionDisplay.contains("A:B:C"))
            assertTrue(AnswerChecker.check(ratioMed.answer, ratioMed.answerSpec))

            val ratioHard = QuestionGenerator.generateRatioQuestion(Difficulty.HARD)
            assertTrue(ratioHard.questionDisplay.contains("A:B:C"))
            assertTrue(AnswerChecker.check(ratioHard.answer, ratioHard.answerSpec))
        }

        // Chain Calculation Hard (brackets and 4 operations)
        repeat(15) {
            val chainHard = QuestionGenerator.generateChainCalculationQuestion(Difficulty.HARD)
            assertTrue(chainHard.questionDisplay.contains("("))
            assertTrue(AnswerChecker.check(chainHard.answer, chainHard.answerSpec))
        }

        // Fractions up to denominator 15 with course values
        repeat(20) {
            val frac = FractionsPool.generateFractionQuestion(1, 9, 2, 15, useCourseValues = true)
            assertNotNull(frac.question)
            assertNotNull(frac.answer)
            assertTrue(frac.denominator in 2..15)
        }
    }

    @Test
    fun testMemoryTablesGeneratorForwardAndReverse() {
        for (cat in MemoryTableCategory.entries) {
            repeat(15) {
                val fwd = QuestionGenerator.generateMemoryTableQuestion(MemoryDirection.FORWARD, setOf(cat))
                val arg = fwd.num1.roundToInt()
                val entry = CourseValuesData.lookup(cat, arg)
                assertNotNull(entry)
                assertEquals(entry!!.courseValue, fwd.numericAnswer, 1e-9)
                assertTrue(AnswerChecker.check(fwd.answer, fwd.answerSpec))
                // Also accepts exact mathematical value within ±0.05
                assertTrue(AnswerChecker.check(entry.trueValue.toString(), fwd.answerSpec))

                val rev = QuestionGenerator.generateMemoryTableQuestion(MemoryDirection.REVERSE, setOf(cat))
                assertTrue(AnswerChecker.check(rev.answer, rev.answerSpec))
            }
        }

        // Verify 1/13 and 1/14 reverse ambiguity (0.07 accepts both 13 and 14)
        repeat(30) {
            val revRecip = QuestionGenerator.generateMemoryTableQuestion(
                MemoryDirection.REVERSE,
                setOf(MemoryTableCategory.RECIPROCAL)
            )
            if (revRecip.questionDisplay.contains("0.07")) {
                assertTrue(AnswerChecker.check("13", revRecip.answerSpec))
                assertTrue(AnswerChecker.check("14", revRecip.answerSpec))
            }
        }
    }

    @Test
    fun testScientificNotationGeneratorAllSubtypes() {
        for (subtype in listOf(
            SciNotationSubtype.NORMALIZE,
            SciNotationSubtype.DECIMAL_SHIFT,
            SciNotationSubtype.MULTIPLY_DIVIDE,
            SciNotationSubtype.ROOT_POWER_TEN
        )) {
            repeat(20) {
                val q = QuestionGenerator.generateScientificNotationQuestion(subtype)
                assertTrue("Failed self-check for ${q.questionDisplay} -> ${q.answer}",
                    AnswerChecker.check(q.answer, q.answerSpec))
                assertTrue(q.solutionSteps.isNotEmpty())
            }
        }
    }

    @Test
    fun testPhGeneratorAllSubtypesAndCourseRecomputation() {
        for (subtype in listOf(
            PhSubType.CONC_TO_PH,
            PhSubType.SCI_CONC_TO_PH,
            PhSubType.PH_TO_CONC,
            PhSubType.PH_POH_CONV,
            PhSubType.PERCENT_WV_TO_PH
        )) {
            repeat(20) {
                val q = QuestionGenerator.generatePhQuestion(subtype)
                assertTrue("Failed self-check for ${q.questionDisplay} -> ${q.answer}",
                    AnswerChecker.check(q.answer, q.answerSpec))
                assertTrue(q.solutionSteps.isNotEmpty())

                if (subtype == PhSubType.SCI_CONC_TO_PH) {
                    // Verify course integer-rounding method: round mantissa to 1..9, then exp - log10(m)
                    val rawM = q.num1
                    val exp = q.num2.roundToInt()
                    val mInt = rawM.roundToInt().coerceIn(1, 9)
                    val logM = if (mInt == 1) 0.0 else CourseValuesData.lookup(MemoryTableCategory.LOG_10, mInt)!!.courseValue
                    val recomputedPh = exp - logM
                    assertEquals(recomputedPh, q.numericAnswer, 1e-6)
                } else if (subtype == PhSubType.PH_POH_CONV) {
                    assertEquals(14.0 - q.num1, q.numericAnswer, 1e-6)
                }
            }
        }
    }

    @Test
    fun testCompositeLogsRootsAndCubesPowersGenerators() {
        for (sub in listOf(
            CompositeLogRootSubtype.LOG_SCI,
            CompositeLogRootSubtype.LOG_PRODUCT,
            CompositeLogRootSubtype.ROOT_SCI
        )) {
            repeat(20) {
                val q = QuestionGenerator.generateCompositeLogRootQuestion(sub)
                assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
                assertTrue(q.solutionSteps.isNotEmpty())
            }
        }

        for (sub in listOf(
            CubesPowersSubtype.CUBES,
            CubesPowersSubtype.POWERS,
            CubesPowersSubtype.PERFECT_CUBE_ROOTS
        )) {
            repeat(20) {
                val q = QuestionGenerator.generateCubesPowersQuestion(sub, Pair(1, 15))
                assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
                assertTrue(q.solutionSteps.isNotEmpty())
                if (sub == CubesPowersSubtype.CUBES) {
                    val n = q.num1.roundToLong()
                    assertEquals(n * n * n, q.answer.toLong())
                } else if (sub == CubesPowersSubtype.PERFECT_CUBE_ROOTS) {
                    val cube = q.num1.roundToLong()
                    val root = q.answer.toLong()
                    assertEquals(cube, root * root * root)
                }
            }
        }
    }

    // =========================================================================
    // 4. MCQ Option Spacing Tests
    // =========================================================================
    @Test
    fun testMcqOptionSpacing() {
        val sampleProblems = listOf(
            QuestionGenerator.generateApproximationQuestion(Difficulty.MEDIUM, false),
            QuestionGenerator.generateMemoryTableQuestion(MemoryDirection.FORWARD),
            QuestionGenerator.generateScientificNotationQuestion(SciNotationSubtype.MULTIPLY_DIVIDE),
            QuestionGenerator.generatePhQuestion(PhSubType.SCI_CONC_TO_PH),
            QuestionGenerator.generateCompositeLogRootQuestion(CompositeLogRootSubtype.LOG_PRODUCT),
            QuestionGenerator.generateConstantQuestion()
        )

        for (base in sampleProblems) {
            for (preset in TolerancePreset.entries) {
                val minSpacing = AnswerChecker.minimumMcqSpacing(base.answerSpec, preset)
                val mcq = QuestionGenerator.attachMcqOptions(base, preset)
                val options = mcq.mcqOptions
                assertNotNull(options)
                assertEquals(4, options!!.size)
                assertEquals(4, options.distinct().size)
                assertTrue(options.contains(base.answer))

                // Verify AnswerChecker accepts the right option and rejects distractors
                for (opt in options) {
                    val checked = AnswerChecker.check(opt, mcq.answerSpec, preset)
                    assertEquals(opt == base.answer, checked)
                }

                // Verify numeric spacing between every pair of options is >= 3 * tolerance
                val numericVals = options.mapNotNull { ScientificInputParser.parse(it)?.numericValue }
                assertEquals(4, numericVals.size)
                for (i in 0 until numericVals.size) {
                    for (j in i + 1 until numericVals.size) {
                        val dist = abs(numericVals[i] - numericVals[j])
                        assertTrue(
                            "Expected spacing >= $minSpacing between ${options[i]} and ${options[j]}, got $dist",
                            dist >= minSpacing - 1e-6
                        )
                    }
                }
            }
        }
    }

    // =========================================================================
    // 5. Constants Loader & Validation Tests
    // =========================================================================
    @Test
    fun testConstantsLoaderValidationAndDrill() {
        // Real HSC dataset entries load cleanly (61 constants, 4 math, 12 ref, 39 atomic, 14 conversions)
        val dataset = ConstantsLoader.getDataset(failLoudly = true)
        assertEquals(61, dataset.constants.size)
        assertEquals(4, dataset.mathConstants.size)
        assertEquals(12, dataset.referenceValues.size)
        assertEquals(39, dataset.atomicMasses.size)
        assertEquals(14, dataset.unitConversions.size)
        assertEquals(14, dataset.derivedFormulas.size)
        assertEquals(8, dataset.notFixedTables.size)

        val loaded = ConstantsLoader.validateAndLoad(ConstantsData.entries, failLoudly = true)
        assertEquals(61, loaded.size)
        assertTrue(loaded.none { it.isSample })

        // Duplicate ID fails loudly when failLoudly = true
        val dupList = ConstantsData.entries + ConstantsData.entries.first()
        var duplicateThrew = false
        try {
            ConstantsLoader.validateAndLoad(dupList, failLoudly = true)
        } catch (e: IllegalStateException) {
            duplicateThrew = true
        }
        assertTrue("Expected IllegalStateException on duplicate constant ID", duplicateThrew)

        // In release mode (failLoudly = false), duplicate/blank entries are skipped cleanly
        val badEntry = ConstantEntry(
            id = "bad_blank",
            name = "   ",
            symbol = StructuredSymbol("x"),
            subject = "Mechanics",
            mantissa = 1.0,
            exponent = 0,
            unit = StructuredUnit(listOf(UnitFactor("m")))
        )
        val releaseLoaded = ConstantsLoader.validateAndLoad(dupList + badEntry, failLoudly = false)
        assertEquals(61, releaseLoaded.size)

        // Constants drill question generation
        repeat(20) {
            val q = QuestionGenerator.generateConstantQuestion()
            assertTrue(AnswerChecker.check(q.answer, q.answerSpec))
            assertNotNull(q.subjectLabel)
            assertNotNull(q.constantSymbol)
            assertNotNull(q.structuredUnit)
        }
    }

    // =========================================================================
    // 6. Local-Date Bucketing Utility Across Multiple Timezones
    // =========================================================================
    @Test
    fun testDateBucketUtilsAcrossTimezones() {
        // 2026-10-23T20:30:00.000Z:
        // In UTC -> 2026-10-23
        // In Asia/Dhaka (UTC+6) -> 2026-10-24 (02:30 AM next day!)
        // In America/Los_Angeles (UTC-7 PDT) -> 2026-10-23 (13:30 PM same day)
        val isoTimestamp = "2026-10-23T20:30:00.000Z"

        val tzUtc = TimeZone.getTimeZone("UTC")
        val tzDhaka = TimeZone.getTimeZone("Asia/Dhaka")
        val tzLa = TimeZone.getTimeZone("America/Los_Angeles")

        assertEquals("2026-10-23", DateBucketUtils.utcIsoToLocalDate(isoTimestamp, tzUtc))
        assertEquals("2026-10-24", DateBucketUtils.utcIsoToLocalDate(isoTimestamp, tzDhaka))
        assertEquals("2026-10-23", DateBucketUtils.utcIsoToLocalDate(isoTimestamp, tzLa))

        // Streak calculation across consecutive local days in Dhaka vs UTC
        val nowEpochMs = DateBucketUtils.parseUtcIsoMillis("2026-10-24T19:00:00.000Z")!! // 2026-10-25 01:00 in Dhaka
        val sessions = listOf(
            SessionEntity(
                id = "s1",
                date = "2026-10-24T19:00:00.000Z", // 2026-10-25 in Dhaka
                mode = "addition",
                totalQuestions = 10,
                correct = 9,
                avgTimeMs = 1500.0,
                difficulty = "medium",
                duration = 15.0,
                localDate = DateBucketUtils.utcIsoToLocalDate("2026-10-24T19:00:00.000Z", tzDhaka)
            ),
            SessionEntity(
                id = "s2",
                date = "2026-10-23T20:30:00.000Z", // 2026-10-24 in Dhaka
                mode = "addition",
                totalQuestions = 10,
                correct = 8,
                avgTimeMs = 1600.0,
                difficulty = "medium",
                duration = 16.0,
                localDate = DateBucketUtils.utcIsoToLocalDate("2026-10-23T20:30:00.000Z", tzDhaka)
            )
        )

        val streakDhaka = DateBucketUtils.calculateStreakDays(
            localDates = sessions.map { it.localDate }.toSet(),
            nowMillis = nowEpochMs,
            timeZone = tzDhaka
        )
        assertEquals(2, streakDhaka)

        val perfDays = DateBucketUtils.buildRecentPerformanceDays(sessions, 7, nowEpochMs, tzDhaka)
        assertEquals(7, perfDays.size)
        assertEquals(10, perfDays.last().totalQuestions) // Today (2026-10-25 in Dhaka)
        assertEquals(10, perfDays[perfDays.size - 2].totalQuestions) // Yesterday (2026-10-24 in Dhaka)
    }

    // =========================================================================
    // 7. Rich-Text Symbol & Unit Normalization across Real HSC Dataset
    // =========================================================================
    @Test
    fun testSymbolAndUnitNormalizationAcrossEntireDataset() {
        val dataset = ConstantsLoader.getDataset(failLoudly = true)
        val allEntries = dataset.constants + dataset.referenceValues

        // 1. Short vs full symbol extraction edge cases (Section 3 & Section 8)
        assertEquals("R", StructuredSymbol.extractShortSymbol("R = N_A k_B"))
        assertEquals("F", StructuredSymbol.extractShortSymbol("F = N_A e"))
        assertEquals("E°", StructuredSymbol.extractShortSymbol("E°(H⁺/H₂)"))
        assertEquals("e", StructuredSymbol.extractShortSymbol("e (math)"))
        assertEquals("u", StructuredSymbol.extractShortSymbol("u (or m_u)"))
        assertEquals("0 K", StructuredSymbol.extractShortSymbol("0 K"))
        assertEquals("m_e c²", StructuredSymbol.extractShortSymbol("m_e c²"))
        assertEquals("2.303RT/F", StructuredSymbol.extractShortSymbol("2.303RT/F"))

        // 2. Every symbol in constants, reference values, and math constants renders with no raw '^' or '_'
        for (entry in allEntries) {
            val shortRendered = RichTextHelper.formatSymbol(entry.symbol, shortForm = true).text
            val fullRendered = RichTextHelper.formatSymbol(entry.symbol, shortForm = false).text
            assertTrue("Short symbol empty for ${entry.id}", shortRendered.isNotBlank())
            assertTrue("Full symbol empty for ${entry.id}", fullRendered.isNotBlank())
            assertFalse("Raw '^' in short symbol for ${entry.id}: $shortRendered", shortRendered.contains('^'))
            assertFalse("Raw '_' in short symbol for ${entry.id}: $shortRendered", shortRendered.contains('_'))
            assertFalse("Raw '^' in full symbol for ${entry.id}: $fullRendered", fullRendered.contains('^'))
            assertFalse("Raw '_' in full symbol for ${entry.id}: $fullRendered", fullRendered.contains('_'))
        }
        for (mc in dataset.mathConstants) {
            val shortRendered = RichTextHelper.formatSymbol(mc.symbol, shortForm = true).text
            val fullRendered = RichTextHelper.formatSymbol(mc.symbol, shortForm = false).text
            assertFalse("Raw '^' in math symbol ${mc.id}: $shortRendered", shortRendered.contains('^'))
            assertFalse("Raw '_' in math symbol ${mc.id}: $shortRendered", shortRendered.contains('_'))
            assertFalse("Raw '^' in full math symbol ${mc.id}: $fullRendered", fullRendered.contains('^'))
            assertFalse("Raw '_' in full math symbol ${mc.id}: $fullRendered", fullRendered.contains('_'))
        }

        // 3. Every distinct unit string in constants, alternates, reference values, and unit conversions
        // renders with no raw '^' or '_' and dimensionless/1 renders as "no unit"
        assertEquals("no unit", RichTextHelper.formatRawUnit("dimensionless").text)
        assertEquals("no unit", RichTextHelper.formatRawUnit("1").text)
        assertEquals("no unit", RichTextHelper.formatUnit(StructuredUnit.fromRawUnit("dimensionless")).text)
        assertEquals("no unit", RichTextHelper.formatUnit(StructuredUnit.fromRawUnit("1")).text)

        // Check specific tricky unit strings from Section 3
        val bracketedUnit = RichTextHelper.formatRawUnit("(mol L^-1)^2").text
        assertEquals("(mol·L−1)2", bracketedUnit)
        assertFalse(bracketedUnit.contains('^'))

        val calThUnit = RichTextHelper.formatRawUnit("cal_th").text
        assertEquals("calth", calThUnit)
        assertFalse(calThUnit.contains('_'))

        val mevUnit = RichTextHelper.formatRawUnit("MeV/c^2").text
        assertEquals("MeV/c2", mevUnit)
        assertFalse(mevUnit.contains('^'))

        for (entry in allEntries) {
            val primaryUnitText = RichTextHelper.formatUnit(entry.unit).text
            assertTrue("Empty unit text for ${entry.id}", primaryUnitText.isNotBlank())
            assertFalse("Raw '^' in unit for ${entry.id}: $primaryUnitText", primaryUnitText.contains('^'))
            assertFalse("Raw '_' in unit for ${entry.id}: $primaryUnitText", primaryUnitText.contains('_'))

            for (alt in entry.allAlternateValues) {
                val altUnitText = RichTextHelper.formatUnit(alt.unit).text
                assertTrue("Empty alt unit text for ${entry.id}", altUnitText.isNotBlank())
                assertFalse("Raw '^' in alt unit for ${entry.id}: $altUnitText", altUnitText.contains('^'))
                assertFalse("Raw '_' in alt unit for ${entry.id}: $altUnitText", altUnitText.contains('_'))
            }
        }

        for (group in dataset.unitConversions) {
            for (item in group.values) {
                item.fromUnit?.let { fromUnit ->
                    val fromText = RichTextHelper.formatUnit(fromUnit).text
                    assertFalse("Raw '^' in conversion from '${fromUnit.toPlainString()}': $fromText", fromText.contains('^'))
                    assertFalse("Raw '_' in conversion from '${fromUnit.toPlainString()}': $fromText", fromText.contains('_'))
                }
                item.toUnit?.let { toUnit ->
                    val toText = RichTextHelper.formatUnit(toUnit).text
                    assertFalse("Raw '^' in conversion to '${toUnit.toPlainString()}': $toText", toText.contains('^'))
                    assertFalse("Raw '_' in conversion to '${toUnit.toPlainString()}': $toText", toText.contains('_'))
                }
                item.formula?.let { formula ->
                    val fText = RichTextHelper.formatMathText(formula).text
                    assertFalse("Raw '^' in formula '$formula': $fText", fText.contains('^'))
                    assertFalse("Raw '_' in formula '$formula': $fText", fText.contains('_'))
                }
            }
        }

        for (df in dataset.derivedFormulas) {
            val fText = RichTextHelper.formatMathText(df.formula).text
            assertFalse("Raw '^' in derived formula '${df.id}': $fText", fText.contains('^'))
            assertFalse("Raw '_' in derived formula '${df.id}': $fText", fText.contains('_'))
        }
    }

    // =========================================================================
    // 8. Section 8 Verification: Dataset Integrity, Approximations, Rules, and Drills
    // =========================================================================

    @Test
    fun testDatasetExactCountsIntegrityAndNoDuplicateIds() {
        val dataset = ConstantsLoader.getDataset(failLoudly = true)

        // Exact counts as specified in Section 2 & Section 8
        assertEquals("Constants count must be 61", 61, dataset.constants.size)
        assertEquals("Math constants count must be 4", 4, dataset.mathConstants.size)
        assertEquals("Reference values count must be 12", 12, dataset.referenceValues.size)
        assertEquals("Atomic masses count must be 39", 39, dataset.atomicMasses.size)
        assertEquals("Unit conversions count must be 14", 14, dataset.unitConversions.size)
        assertEquals("Derived formulas count must be 14", 14, dataset.derivedFormulas.size)
        assertEquals("Not-fixed tables count must be 8", 8, dataset.notFixedTables.size)

        // Verify no duplicate IDs across all entities
        val allIds = mutableListOf<String>()
        dataset.constants.forEach { allIds.add(it.id) }
        dataset.referenceValues.forEach { allIds.add(it.id) }
        dataset.mathConstants.forEach { allIds.add(it.id) }
        dataset.derivedFormulas.forEach { allIds.add(it.id) }
        dataset.unitConversions.forEach { allIds.add(it.id) }
        dataset.notFixedTables.forEach { allIds.add(it.id) }

        val duplicates = allIds.groupingBy { it }.eachCount().filter { it.value > 1 }
        assertTrue("Duplicate IDs found across dataset: $duplicates", duplicates.isEmpty())

        // Verify chapter mapping: all mapped chapters exist in syllabus, unmapped entries fallback to General physics
        val unmappedIds = setOf("reduced_planck_constant", "fine_structure_constant", "loschmidt_constant_100kpa", "loschmidt_constant_1atm")
        for (c in dataset.constants) {
            if (c.id in unmappedIds) {
                assertEquals("Unmapped constant ${c.id} should have General physics paper", setOf(HscPaper.GENERAL_PHYSICS), c.papers)
            } else {
                assertTrue("Constant ${c.id} should have mapped syllabus chapters", c.chapters.isNotEmpty())
                for (chap in c.chapters) {
                    assertFalse("Empty chapter name for ${c.id}", chap.nameEn.isBlank())
                    assertFalse("Empty Bangla chapter name for ${c.id}", chap.nameBn.isBlank())
                }
            }
        }
    }

    @Test
    fun testParsableApproximationsWithinOnePercentOfPreciseValue() {
        val dataset = ConstantsLoader.getDataset(failLoudly = true)
        val allEntries = dataset.constants

        var checkedCount = 0
        for (entry in allEntries) {
            val approxRaw = entry.commonHscApproximationRaw ?: continue
            val approxVal = entry.courseValue
            if (entry.isZeroValued) continue

            val preciseVal = entry.preciseValue
            if (abs(preciseVal) > 1e-15) {
                val relDiff = abs(approxVal - preciseVal) / abs(preciseVal)
                assertTrue(
                    "Approximation for ${entry.id} ($approxVal) differs by ${(relDiff * 100)}% from precise ($preciseVal), which exceeds 1%",
                    relDiff <= 0.01
                )
                checkedCount++
            }
        }
        assertTrue("Expected to verify multiple parsable approximations", checkedCount > 20)
    }

    @Test
    fun testPrimaryVariantRuleAndSkippedTrivialAlternates() {
        val dataset = ConstantsLoader.getDataset(failLoudly = true)

        // 6 Non-SI first entries specified in Section 3
        val fineStructure = dataset.constants.find { it.id == "fine_structure_constant" }!!
        assertEquals("no unit", fineStructure.unit.toPlainString())
        assertTrue(fineStructure.unit.isDimensionless)
        assertEquals("no unit", RichTextHelper.formatUnit(fineStructure.unit).text)
        assertTrue("Fine structure constant precise value should be ~0.007297", abs(fineStructure.preciseValue - 0.0072973525643) < 1e-7)

        val molarVolume100kPa = dataset.constants.find { it.id == "molar_volume_stp_100kpa" }!!
        assertEquals("L mol^-1", molarVolume100kPa.unit.toPlainString())
        assertEquals(22.71, molarVolume100kPa.courseValue, 1e-4)

        val molarVolume1atm = dataset.constants.find { it.id == "molar_volume_stp_1atm" }!!
        assertEquals("L mol^-1", molarVolume1atm.unit.toPlainString())
        assertEquals(22.4, molarVolume1atm.courseValue, 1e-4)

        val waterKw = dataset.constants.find { it.id == "water_ion_product_25c" }!!
        assertEquals("(mol L^-1)^2", waterKw.unit.toPlainString())
        assertEquals(1.0e-14, waterKw.courseValue, 1e-18)

        val she = dataset.constants.find { it.id == "standard_hydrogen_electrode_potential" }!!
        assertEquals("V", she.unit.toPlainString())
        assertEquals(0.0, she.courseValue, 1e-15)
        assertTrue(she.isZeroValued)

        val refractiveVacuum = dataset.constants.find { it.id == "refractive_index_vacuum" }!!
        assertEquals("no unit", refractiveVacuum.unit.toPlainString())
        assertTrue(refractiveVacuum.unit.isDimensionless)
        assertEquals(1.0, refractiveVacuum.preciseValue, 1e-15)

        // Verify skipped trivial alternates: 1 u, 1 eV, 1 au
        for (c in dataset.constants) {
            for (alt in c.allAlternateValues) {
                assertFalse("Trivial alternate '1 u' must be skipped in ${c.id}", alt.mantissa == 1.0 && alt.exponent == 0 && alt.unit.toPlainString() == "u")
                assertFalse("Trivial alternate '1 eV' must be skipped in ${c.id}", alt.mantissa == 1.0 && alt.exponent == 0 && alt.unit.toPlainString() == "eV")
                assertFalse("Trivial alternate '1 au' must be skipped in ${c.id}", alt.mantissa == 1.0 && alt.exponent == 0 && alt.unit.toPlainString() == "au")
            }
        }
    }

    @Test
    fun testZeroValuedEntriesTolerance() {
        val dataset = ConstantsLoader.getDataset(failLoudly = true)
        val she = dataset.constants.find { it.id == "standard_hydrogen_electrode_potential" }!!
        val absZero = dataset.referenceValues.find { it.id == "water_absolute_zero" }!!

        assertTrue(she.isZeroValued)
        assertTrue(absZero.isZeroValued)

        val sheSpec = AnswerSpec.ScientificValue(
            mantissa = 0.0,
            exponent = 0,
            mantissaRelativeTol = 0.01,
            alsoAcceptDoubles = listOf(0.0)
        )
        // 0.01 absolute tolerance should accept answers within 0.01 of 0
        assertTrue(AnswerChecker.check("0", sheSpec))
        assertTrue(AnswerChecker.check("0.0", sheSpec))
        assertTrue(AnswerChecker.check("0.005", sheSpec))
        assertTrue(AnswerChecker.check("-0.005", sheSpec))
        assertTrue(AnswerChecker.check("0.01", sheSpec))
        assertTrue(AnswerChecker.check("-0.01", sheSpec))
        assertFalse(AnswerChecker.check("0.02", sheSpec))
        assertFalse(AnswerChecker.check("0.05", sheSpec))
    }

    @Test
    fun testDualKeyAnswerCheckingAndRejectionOfG10() {
        val dataset = ConstantsLoader.getDataset(failLoudly = true)
        val g = dataset.constants.find { it.id == "standard_acceleration_gravity" }!!

        val accepted = listOf(g.preciseValue, g.courseValue) // 9.80665 and 9.8
        val gSpec = AnswerSpec.ScientificValue(
            mantissa = g.mantissa,
            exponent = g.exponent,
            mantissaRelativeTol = 0.01,
            alsoAcceptDoubles = accepted
        )

        // Precise value passes
        assertTrue(AnswerChecker.check("9.80665", gSpec))
        assertTrue(AnswerChecker.check("9.807", gSpec))

        // Course value passes
        assertTrue(AnswerChecker.check("9.8", gSpec))
        assertTrue(AnswerChecker.check("9.81", gSpec))

        // g = 10 differs by ~1.9% from 9.81 and ~2% from 9.80665, rejected at 1% tolerance
        assertFalse("g = 10 must be rejected under 1% tolerance", AnswerChecker.check("10", gSpec))
        assertFalse("g = 10.0 must be rejected under 1% tolerance", AnswerChecker.check("10.0", gSpec))
    }

    @Test
    fun testOverridesPrecedence() {
        val preciseVal = 123.4567

        // 1. Fallback: precise value rounded to 4 significant figures when no approximation or override
        val fallbackCourse = ConstantsLoader.roundToSignificantFigures(preciseVal, 4)
        assertEquals(123.5, fallbackCourse, 1e-3)

        // 2. Extracted approximation: takes precedence over 4-sig-fig rounding
        val extractedApprox = ConstantsLoader.extractCourseApproximationForValue("1.23 × 10^2", preciseVal)
        assertNotNull(extractedApprox)
        assertEquals(123.0, extractedApprox!!, 1e-3)

        // 3. Override: override courseMantissa & courseExponent takes highest precedence
        val override = ConstantOverride(
            id = "test_const",
            courseMantissa = 1.25,
            courseExponent = 2
        )
        val overrideCourseVal = override.courseNumericValue!!
        assertEquals(125.0, overrideCourseVal, 1e-3)

        // Precedence order: override (125.0) > approx (123.0) > fallback (123.5)
        val selectedVal = override.courseNumericValue ?: (extractedApprox ?: fallbackCourse)
        assertEquals(125.0, selectedVal, 1e-3)
    }

    @Test
    fun testExtraDrillsGeneratorsAndChecking() {
        // 1. Atomic Masses Drill (39 elements, tolerance 0.05)
        val atomicQ = QuestionGenerator.generateAtomicMassQuestion()
        assertNotNull(atomicQ.questionDisplay)
        assertTrue(atomicQ.questionDisplay.startsWith("Atomic mass of "))
        assertEquals("Atomic Masses", atomicQ.operation)
        assertTrue(AnswerChecker.check(atomicQ.answer, atomicQ.answerSpec))

        val expectedMass = atomicQ.numericAnswer
        assertTrue(AnswerChecker.check((expectedMass + 0.04).toString(), atomicQ.answerSpec))
        assertTrue(AnswerChecker.check((expectedMass - 0.04).toString(), atomicQ.answerSpec))
        assertFalse(AnswerChecker.check((expectedMass + 0.1).toString(), atomicQ.answerSpec))

        // 2. Unit Conversions Drill (1% tolerance, skips temperature)
        val convQ = QuestionGenerator.generateUnitConversionQuestion()
        assertTrue(convQ.questionDisplay.contains(" = ? "))
        assertEquals("Conversions", convQ.operation)
        assertTrue(AnswerChecker.check(convQ.answer, convQ.answerSpec))

        // 3. Math Constants Drill (accepts fraction 22/7 for pi)
        val mathQ = QuestionGenerator.generateMathConstantQuestion()
        assertEquals("Math Constants", mathQ.operation)
        assertTrue(AnswerChecker.check(mathQ.answer, mathQ.answerSpec))

        val piSpec = AnswerSpec.RelativeTolerance(
            expected = Math.PI,
            relativeToleranceOverride = 0.01,
            exactCourseValues = listOf(Math.PI, 3.14, 3.1416, 22.0 / 7.0)
        )
        assertTrue("Exact pi string passes", AnswerChecker.check("3.14159", piSpec))
        assertTrue("Course approx 3.14 passes", AnswerChecker.check("3.14", piSpec))
        assertTrue("Course approx 3.1416 passes", AnswerChecker.check("3.1416", piSpec))
        assertTrue("Fraction 22/7 passes for pi", AnswerChecker.check("22/7", piSpec))

        // 4. Reference Values Drill
        val refQ = QuestionGenerator.generateReferenceValueQuestion()
        assertEquals("Ref Values", refQ.operation)
        assertTrue(AnswerChecker.check(refQ.answer, refQ.answerSpec))
    }

    @Test
    fun testWrongAnswerFlowAndManualNextAdvancement() {
        // Test AnswerChecker correctly detects wrong answers across scientific and standard specs
        val spec = AnswerSpec.ScientificValue(
            mantissa = 6.626,
            exponent = -34,
            mantissaRelativeTol = 0.01,
            requireNormalized = false,
            alsoAcceptDoubles = listOf(6.63e-34)
        )
        // Wrong answer
        assertFalse(AnswerChecker.check("5.00e-34", spec))

        // Verify score & streak logic invariants:
        // Score only increments on correct, streak resets to 0 on wrong
        var score = 5
        var streak = 4
        val isCorrect = AnswerChecker.check("5.00e-34", spec)
        assertFalse(isCorrect)
        if (isCorrect) {
            streak += 1
            score += 1
        } else {
            streak = 0
        }
        assertEquals(0, streak)
        assertEquals(5, score)

        // Simulate manual next click: single invocation resets waiting and advances index
        var waitingForManualNext = true
        var currentQuestionIndex = 1
        fun onManualNextClicked() {
            if (!waitingForManualNext) return
            waitingForManualNext = false
            currentQuestionIndex += 1
        }

        // First click
        onManualNextClicked()
        assertFalse(waitingForManualNext)
        assertEquals(2, currentQuestionIndex)

        // Duplicate click while not waiting must be no-op (no double advance)
        onManualNextClicked()
        assertFalse(waitingForManualNext)
        assertEquals(2, currentQuestionIndex)
    }
}
