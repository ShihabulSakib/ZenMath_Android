package com.example.data

import kotlin.math.cbrt
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Dedicated data file for memorized "course values" (roots, cube roots, base-10 logs,
 * reciprocals, and rounded fraction decimals).
 *
 * Edit values here without touching generator or answer-checking logic.
 */
data class MemoryTableEntry(
    val id: String,
    val category: MemoryTableCategory,
    val inputNumber: Int,
    val displayPrompt: String,
    val reversePrompt: String,
    val courseValue: Double,
    val courseValueDisplay: String,
    val trueValue: Double,
    val defaultTolerance: Double,
    val alternateReverseInputs: Set<Long> = emptySet()
) {
    val argument: Int get() = inputNumber
    val forwardPrompt: String get() = displayPrompt.removeSuffix(" = ?")
}

enum class MemoryTableCategory(val key: String, val title: String) {
    SQUARE_ROOT("sqrt_table", "Square Roots (√2 – √10)"),
    CUBE_ROOT("cbrt_table", "Cube Roots (∛2 – ∛10)"),
    LOG_10("log_table", "Base-10 Logs (log 1 – log 10)"),
    RECIPROCAL("reciprocal_table", "Reciprocals (1/11 – 1/15)");

    val id: String get() = key
    val displayName: String get() = title
}

object CourseValuesData {

    /**
     * Square roots of 2 to 10: 1.41, 1.73, 2, 2.2, 2.4, 2.6, 2.8, 3, 3.2
     */
    val SQUARE_ROOTS: Map<Int, Double> = linkedMapOf(
        2 to 1.41,
        3 to 1.73,
        4 to 2.0,
        5 to 2.2,
        6 to 2.4,
        7 to 2.6,
        8 to 2.8,
        9 to 3.0,
        10 to 3.2
    )

    val SQUARE_ROOT_DISPLAY: Map<Int, String> = linkedMapOf(
        2 to "1.41",
        3 to "1.73",
        4 to "2",
        5 to "2.2",
        6 to "2.4",
        7 to "2.6",
        8 to "2.8",
        9 to "3",
        10 to "3.2"
    )

    /**
     * Cube roots of 2 to 10: 1.3, 1.4, 1.6, 1.7, 1.8, 1.9, 2, 2.1, 2.2
     */
    val CUBE_ROOTS: Map<Int, Double> = linkedMapOf(
        2 to 1.3,
        3 to 1.4,
        4 to 1.6,
        5 to 1.7,
        6 to 1.8,
        7 to 1.9,
        8 to 2.0,
        9 to 2.1,
        10 to 2.2
    )

    val CUBE_ROOT_DISPLAY: Map<Int, String> = linkedMapOf(
        2 to "1.3",
        3 to "1.4",
        4 to "1.6",
        5 to "1.7",
        6 to "1.8",
        7 to "1.9",
        8 to "2",
        9 to "2.1",
        10 to "2.2"
    )

    /**
     * Logs (base 10) of 1 to 10: 0, 0.3, 0.5, 0.6, 0.7, 0.8, 0.85, 0.9, 0.95, 1
     */
    val LOG_10: Map<Int, Double> = linkedMapOf(
        1 to 0.0,
        2 to 0.3,
        3 to 0.5,
        4 to 0.6,
        5 to 0.7,
        6 to 0.8,
        7 to 0.85,
        8 to 0.9,
        9 to 0.95,
        10 to 1.0
    )

    val LOG_10_DISPLAY: Map<Int, String> = linkedMapOf(
        1 to "0",
        2 to "0.3",
        3 to "0.5",
        4 to "0.6",
        5 to "0.7",
        6 to "0.8",
        7 to "0.85",
        8 to "0.9",
        9 to "0.95",
        10 to "1"
    )

    /**
     * Reciprocals 1/11 to 1/15: 0.0909, 0.08, 0.07, 0.07, 0.06
     */
    val RECIPROCALS_11_TO_15: Map<Int, Double> = linkedMapOf(
        11 to 0.0909,
        12 to 0.08,
        13 to 0.07,
        14 to 0.07,
        15 to 0.06
    )

    val RECIPROCALS_DISPLAY: Map<Int, String> = linkedMapOf(
        11 to "0.0909",
        12 to "0.08",
        13 to "0.07",
        14 to "0.07",
        15 to "0.06"
    )

    /**
     * Taught rounded course values for unit fractions 1/d (d = 2..15).
     * Used when "Course values" mode is enabled in Fractions.
     */
    val UNIT_FRACTION_COURSE_DECIMALS: Map<Int, List<Double>> = linkedMapOf(
        2 to listOf(0.5),
        3 to listOf(0.3, 0.33, 0.333, 0.3333),
        4 to listOf(0.25),
        5 to listOf(0.2),
        6 to listOf(0.15, 0.16, 0.17, 0.167, 0.1667),
        7 to listOf(0.14, 0.143, 0.1429),
        8 to listOf(0.125, 0.12, 0.13),
        9 to listOf(0.11, 0.111, 0.1111),
        10 to listOf(0.1),
        11 to listOf(0.0909, 0.09, 0.091),
        12 to listOf(0.08, 0.083, 0.0833),
        13 to listOf(0.07, 0.077, 0.0769),
        14 to listOf(0.07, 0.071, 0.0714),
        15 to listOf(0.06, 0.067, 0.0667)
    )

    /**
     * Returns acceptable course decimal values for an irreducible fraction n/d.
     */
    fun getCourseDecimalsForFraction(n: Int, d: Int): List<Double> {
        val trueVal = n.toDouble() / d.toDouble()
        val unitCandidates = UNIT_FRACTION_COURSE_DECIMALS[d] ?: emptyList()
        val scaledCandidates = unitCandidates.map { u ->
            kotlin.math.round(n * u * 10000.0) / 10000.0
        }
        val explicitOverrides = when (n to d) {
            1 to 3 -> listOf(0.3, 0.33, 0.3333)
            2 to 3 -> listOf(0.6, 0.66, 0.67, 0.6667)
            1 to 6 -> listOf(0.15, 0.16, 0.17, 0.1667)
            5 to 6 -> listOf(0.75, 0.83, 0.85, 0.8333)
            else -> emptyList()
        }
        return (explicitOverrides + scaledCandidates + listOf(trueVal)).distinct()
    }

    val ALL_MEMORY_TABLE_ENTRIES: List<MemoryTableEntry> by lazy {
        val list = mutableListOf<MemoryTableEntry>()

        for ((n, cv) in SQUARE_ROOTS) {
            val disp = SQUARE_ROOT_DISPLAY[n] ?: cv.toString()
            list.add(
                MemoryTableEntry(
                    id = "sqrt_$n",
                    category = MemoryTableCategory.SQUARE_ROOT,
                    inputNumber = n,
                    displayPrompt = "√$n = ?",
                    reversePrompt = "√? ≈ $disp",
                    courseValue = cv,
                    courseValueDisplay = disp,
                    trueValue = sqrt(n.toDouble()),
                    defaultTolerance = 0.05
                )
            )
        }

        for ((n, cv) in CUBE_ROOTS) {
            val disp = CUBE_ROOT_DISPLAY[n] ?: cv.toString()
            list.add(
                MemoryTableEntry(
                    id = "cbrt_$n",
                    category = MemoryTableCategory.CUBE_ROOT,
                    inputNumber = n,
                    displayPrompt = "∛$n = ?",
                    reversePrompt = "∛? ≈ $disp",
                    courseValue = cv,
                    courseValueDisplay = disp,
                    trueValue = cbrt(n.toDouble()),
                    defaultTolerance = 0.05
                )
            )
        }

        for ((n, cv) in LOG_10) {
            val disp = LOG_10_DISPLAY[n] ?: cv.toString()
            list.add(
                MemoryTableEntry(
                    id = "log10_$n",
                    category = MemoryTableCategory.LOG_10,
                    inputNumber = n,
                    displayPrompt = "log₁₀($n) = ?",
                    reversePrompt = "log₁₀(?) ≈ $disp",
                    courseValue = cv,
                    courseValueDisplay = disp,
                    trueValue = log10(n.toDouble()),
                    defaultTolerance = 0.05
                )
            )
        }

        for ((d, cv) in RECIPROCALS_11_TO_15) {
            val disp = RECIPROCALS_DISPLAY[d] ?: cv.toString()
            val alternates = if (d == 13 || d == 14) setOf(13L, 14L) else emptySet()
            list.add(
                MemoryTableEntry(
                    id = "recip_$d",
                    category = MemoryTableCategory.RECIPROCAL,
                    inputNumber = d,
                    displayPrompt = "1/$d = ?",
                    reversePrompt = "1/? ≈ $disp",
                    courseValue = cv,
                    courseValueDisplay = disp,
                    trueValue = 1.0 / d.toDouble(),
                    defaultTolerance = 0.005,
                    alternateReverseInputs = alternates
                )
            )
        }

        list
    }

    val allEntries: List<MemoryTableEntry>
        get() = ALL_MEMORY_TABLE_ENTRIES

    fun byCategory(category: MemoryTableCategory): List<MemoryTableEntry> =
        ALL_MEMORY_TABLE_ENTRIES.filter { it.category == category }

    fun lookup(category: MemoryTableCategory, argument: Int): MemoryTableEntry? =
        ALL_MEMORY_TABLE_ENTRIES.find { it.category == category && it.inputNumber == argument }
}
