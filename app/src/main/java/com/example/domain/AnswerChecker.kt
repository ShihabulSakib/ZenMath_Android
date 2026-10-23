package com.example.domain

import com.example.data.CourseValuesData
import kotlin.math.abs
import kotlin.math.pow

/**
 * Settings preset for tolerance-based answer checking (Section 1.2).
 */
enum class TolerancePreset(
    val key: String,
    val label: String,
    val relativeTolerance: Double
) {
    STRICT("strict", "Strict", 0.005),   // 0.5%
    NORMAL("normal", "Normal", 0.01),    // 1.0% (default)
    LENIENT("lenient", "Lenient", 0.02); // 2.0%

    companion object {
        fun fromKey(key: String): TolerancePreset {
            return entries.find { it.key == key } ?: NORMAL
        }
    }
}

/**
 * Specification of how a question's answer should be checked by [AnswerChecker].
 */
sealed class AnswerSpec {

    /**
     * Exact integer checking (e.g. addition, multiplication, tables, series, reverse table drills).
     */
    data class Exact(
        val expected: Long,
        val alternateAccepted: Set<Long> = emptySet()
    ) : AnswerSpec()

    /**
     * Absolute tolerance checking (e.g. memorized root/cube-root/log tables: 0.05;
     * reciprocal decimals: 0.005; pH answers: 0.1).
     */
    data class AbsoluteTolerance(
        val expected: Double,
        val tolerance: Double,
        val exactCourseValues: List<Double> = emptyList()
    ) : AnswerSpec()

    /**
     * Relative tolerance checking (e.g. approximation, composite values).
     * Uses [relativeToleranceOverride] if non-null, otherwise [TolerancePreset.relativeTolerance].
     */
    data class RelativeTolerance(
        val expected: Double,
        val relativeToleranceOverride: Double? = null,
        val exactCourseValues: List<Double> = emptyList()
    ) : AnswerSpec()

    /**
     * Scientific value checking (mantissa × 10^exponent), compared by relative error.
     * Supports optional [alternateValues] (such as R in a second unit system) and
     * [requireNormalizedMantissa] for normalization drills where 1 <= |mantissa| < 10 is required.
     */
    data class ScientificValue(
        val mantissa: Double,
        val exponent: Int,
        val relativeToleranceOverride: Double? = null,
        val alternateValues: List<Pair<Double, Int>> = emptyList(),
        val requireNormalizedMantissa: Boolean = false
    ) : AnswerSpec() {
        val numericValue: Double
            get() = mantissa * 10.0.pow(exponent.toDouble())
    }

    /**
     * Multiple-choice (MCQ) checking.
     */
    data class Choice(
        val correctOption: String,
        val options: List<String>
    ) : AnswerSpec()

    /**
     * Fraction / Decimal conversion checking (supports both "n/d" and decimal answers,
     * with optional course-value decimal acceptance).
     */
    data class FractionSpec(
        val numerator: Int,
        val denominator: Int,
        val isFractionToDecimal: Boolean,
        val acceptCourseValues: Boolean = false,
        val toleranceOverride: Double? = null
    ) : AnswerSpec()

    companion object {
        const val DEFAULT_TABLE_TOLERANCE = 0.05
        const val DEFAULT_RECIPROCAL_TOLERANCE = 0.005
        const val DEFAULT_PH_TOLERANCE = 0.1

        fun exact(expected: Long, alternates: Set<Long> = emptySet()): Exact =
            Exact(expected, alternates)

        fun memorizedTable(
            courseValue: Double,
            trueValue: Double = courseValue,
            toleranceOverride: Double? = null
        ): AbsoluteTolerance = AbsoluteTolerance(
            expected = courseValue,
            tolerance = toleranceOverride ?: DEFAULT_TABLE_TOLERANCE,
            exactCourseValues = listOf(courseValue, trueValue).distinct()
        )

        fun reciprocalTable(
            courseValue: Double,
            trueValue: Double = courseValue,
            toleranceOverride: Double? = null
        ): AbsoluteTolerance = AbsoluteTolerance(
            expected = courseValue,
            tolerance = toleranceOverride ?: DEFAULT_RECIPROCAL_TOLERANCE,
            exactCourseValues = listOf(courseValue, trueValue).distinct()
        )

        fun phAnswer(
            expected: Double,
            toleranceOverride: Double? = null
        ): AbsoluteTolerance = AbsoluteTolerance(
            expected = expected,
            tolerance = toleranceOverride ?: DEFAULT_PH_TOLERANCE,
            exactCourseValues = listOf(expected)
        )

        fun relative(
            expected: Double,
            relativeToleranceOverride: Double? = null,
            exactCourseValues: List<Double> = emptyList()
        ): RelativeTolerance = RelativeTolerance(
            expected = expected,
            relativeToleranceOverride = relativeToleranceOverride,
            exactCourseValues = exactCourseValues
        )

        fun scientific(
            mantissa: Double,
            exponent: Int,
            relativeToleranceOverride: Double? = null,
            alternateValues: List<Pair<Double, Int>> = emptyList(),
            requireNormalizedMantissa: Boolean = false
        ): ScientificValue = ScientificValue(
            mantissa = mantissa,
            exponent = exponent,
            relativeToleranceOverride = relativeToleranceOverride,
            alternateValues = alternateValues,
            requireNormalizedMantissa = requireNormalizedMantissa
        )
    }
}

/**
 * Parsed result of a numeric or scientific-notation user input string (Section 1.3).
 */
data class ParsedScientificInput(
    val value: Double,
    val mantissa: Double,
    val exponent: Int?,
    val isScientific: Boolean
)

object ScientificInputParser {

    const val SCI_MARKER = "×10^"
    const val MAX_INPUT_LENGTH = 20

    private val SCI_SEPARATORS = listOf("×10^", "x10^", "X10^", "*10^", "e", "E")

    /**
     * Parses plain decimal/integer or scientific input (e.g. "2.345×10^-1", "-3.5e4", "10^-3")
     * into [ParsedScientificInput]. Returns `null` on empty or malformed input; never throws.
     */
    fun parse(rawInput: String?): ParsedScientificInput? {
        if (rawInput == null) return null
        val cleaned = rawInput
            .trim()
            .replace("−", "-")
            .replace(" ", "")
        if (cleaned.isEmpty()) return null

        // Check for scientific separator
        val matchedSep = SCI_SEPARATORS.firstOrNull { cleaned.contains(it) }
        if (matchedSep != null) {
            val parts = cleaned.split(matchedSep)
            if (parts.size != 2) return null

            val mantissaStr = parts[0]
            val expStr = parts[1]
            if (expStr.isEmpty() || expStr == "-" || expStr == "+") return null

            val mantissa = when (mantissaStr) {
                "", "+" -> 1.0
                "-" -> -1.0
                else -> parsePlainDouble(mantissaStr) ?: return null
            }
            val exponent = expStr.toIntOrNull() ?: return null
            if (exponent !in -999..999) return null

            val computed = mantissa * 10.0.pow(exponent.toDouble())
            if (computed.isNaN() || computed.isInfinite()) return null

            return ParsedScientificInput(
                value = computed,
                mantissa = mantissa,
                exponent = exponent,
                isScientific = true
            )
        }

        val plainVal = parsePlainDouble(cleaned) ?: return null
        return ParsedScientificInput(
            value = plainVal,
            mantissa = plainVal,
            exponent = null,
            isScientific = false
        )
    }

    fun parseToDouble(rawInput: String?): Double? = parse(rawInput)?.value

    private fun parsePlainDouble(str: String): Double? {
        if (str.isEmpty() || str == "-" || str == "+" || str == "." || str == "-." || str == "+.") {
            return null
        }
        // Reject multiple decimal points or trailing/leading letters
        if (str.count { it == '.' } > 1) return null
        if (str.any { !it.isDigit() && it != '.' && it != '-' && it != '+' }) return null
        val v = str.toDoubleOrNull() ?: return null
        if (v.isNaN() || v.isInfinite()) return null
        return v
    }
}

/**
 * Central answer checker supporting all 5 core answer types plus fraction conversions (Section 1.2).
 */
object AnswerChecker {

    private const val EXACT_FLOAT_EPSILON = 1e-9

    fun check(
        userInput: String?,
        spec: AnswerSpec,
        preset: TolerancePreset = TolerancePreset.NORMAL
    ): Boolean {
        if (userInput.isNullOrBlank()) return false
        val trimmed = userInput.trim()

        return when (spec) {
            is AnswerSpec.Choice -> {
                if (trimmed.equals(spec.correctOption.trim(), ignoreCase = true)) {
                    true
                } else {
                    // If both are numeric/scientific, also compare parsed values within 1e-6
                    val uVal = ScientificInputParser.parseToDouble(trimmed)
                    val cVal = ScientificInputParser.parseToDouble(spec.correctOption)
                    uVal != null && cVal != null && abs(uVal - cVal) <= maxOf(EXACT_FLOAT_EPSILON, abs(cVal) * 1e-6)
                }
            }

            is AnswerSpec.Exact -> {
                val parsed = ScientificInputParser.parse(trimmed) ?: return false
                // Must represent an integer value
                val rounded = kotlin.math.round(parsed.value)
                if (abs(parsed.value - rounded) > EXACT_FLOAT_EPSILON) return false
                val longVal = rounded.toLong()
                longVal == spec.expected || longVal in spec.alternateAccepted
            }

            is AnswerSpec.AbsoluteTolerance -> {
                val userVal = ScientificInputParser.parseToDouble(trimmed) ?: return false
                // 1. Check exact match with any stored course value
                if (spec.exactCourseValues.any { abs(userVal - it) <= EXACT_FLOAT_EPSILON }) {
                    return true
                }
                // 2. Check within absolute tolerance of expected or any stored course value
                val tol = spec.tolerance + EXACT_FLOAT_EPSILON
                if (abs(userVal - spec.expected) <= tol) return true
                spec.exactCourseValues.any { abs(userVal - it) <= tol }
            }

            is AnswerSpec.RelativeTolerance -> {
                val userVal = ScientificInputParser.parseToDouble(trimmed) ?: return false
                if (spec.exactCourseValues.any { abs(userVal - it) <= EXACT_FLOAT_EPSILON }) {
                    return true
                }
                val relTol = spec.relativeToleranceOverride ?: preset.relativeTolerance
                isWithinRelativeTolerance(userVal, spec.expected, relTol) ||
                    spec.exactCourseValues.any { isWithinRelativeTolerance(userVal, it, relTol) }
            }

            is AnswerSpec.ScientificValue -> {
                val parsed = ScientificInputParser.parse(trimmed) ?: return false
                if (spec.requireNormalizedMantissa) {
                    // For normalization drills, require scientific entry with 1 <= |mantissa| < 10 and matching exponent
                    if (!parsed.isScientific || parsed.exponent == null) return false
                    val absM = abs(parsed.mantissa)
                    if (absM < 1.0 - EXACT_FLOAT_EPSILON || absM >= 10.0 - EXACT_FLOAT_EPSILON) return false
                    if (parsed.exponent != spec.exponent) return false
                }
                val relTol = spec.relativeToleranceOverride ?: preset.relativeTolerance
                if (isWithinRelativeTolerance(parsed.value, spec.numericValue, relTol)) {
                    return true
                }
                spec.alternateValues.any { (altM, altE) ->
                    val altVal = altM * 10.0.pow(altE.toDouble())
                    isWithinRelativeTolerance(parsed.value, altVal, relTol)
                }
            }

            is AnswerSpec.FractionSpec -> {
                val expectedVal = spec.numerator.toDouble() / spec.denominator.toDouble()
                if (spec.isFractionToDecimal) {
                    val userVal = ScientificInputParser.parseToDouble(trimmed) ?: return false
                    val baseTol = spec.toleranceOverride ?: if (spec.acceptCourseValues) {
                        if (spec.denominator in 11..15) AnswerSpec.DEFAULT_RECIPROCAL_TOLERANCE else AnswerSpec.DEFAULT_TABLE_TOLERANCE
                    } else {
                        0.001
                    }
                    if (abs(userVal - expectedVal) <= baseTol + EXACT_FLOAT_EPSILON) return true
                    if (spec.acceptCourseValues) {
                        val courseDecimals = CourseValuesData.getCourseDecimalsForFraction(spec.numerator, spec.denominator)
                        return courseDecimals.any { cv ->
                            abs(userVal - cv) <= baseTol + EXACT_FLOAT_EPSILON
                        }
                    }
                    false
                } else {
                    // Expected answer is a fraction string "n/d"
                    val normalized = trimmed.replace(" ", "")
                    if (normalized == "${spec.numerator}/${spec.denominator}") return true
                    if (normalized.contains("/")) {
                        val parts = normalized.split("/")
                        if (parts.size == 2) {
                            val uNum = parts[0].toDoubleOrNull()
                            val uDen = parts[1].toDoubleOrNull()
                            if (uNum != null && uDen != null && uDen != 0.0) {
                                return abs((uNum / uDen) - expectedVal) < 0.0001
                            }
                        }
                    }
                    false
                }
            }
        }
    }

    fun isWithinRelativeTolerance(userVal: Double, expectedVal: Double, relTol: Double): Boolean {
        if (abs(expectedVal) <= EXACT_FLOAT_EPSILON) {
            return abs(userVal) <= maxOf(EXACT_FLOAT_EPSILON, relTol)
        }
        val relError = abs(userVal - expectedVal) / abs(expectedVal)
        return relError <= relTol + EXACT_FLOAT_EPSILON
    }
}
