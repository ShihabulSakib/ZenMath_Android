package com.example.domain

import com.example.data.CourseValuesData
import java.util.Locale
import kotlin.random.Random

data class IrreducibleFraction(val n: Int, val d: Int)

data class GeneratedFractionQuestion(
    val question: String,
    val answer: String,
    val isFractionToDecimal: Boolean,
    val numerator: Int = 1,
    val denominator: Int = 2,
    val solutionSteps: List<String> = emptyList()
)

object FractionsPool {
    private val poolCache = mutableMapOf<String, List<IrreducibleFraction>>()

    private fun gcd(a: Int, b: Int): Int {
        var x = kotlin.math.abs(a)
        var y = kotlin.math.abs(b)
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return x
    }

    private fun buildIrreduciblePool(
        minNumerator: Int,
        maxNumerator: Int,
        minDenominator: Int,
        maxDenominator: Int
    ): List<IrreducibleFraction> {
        val key = "$minNumerator,$maxNumerator,$minDenominator,$maxDenominator"
        poolCache[key]?.let { return it }

        val pool = mutableListOf<IrreducibleFraction>()
        for (d in minDenominator..maxDenominator) {
            val maxN = minOf(maxNumerator, d - 1)
            for (n in minNumerator..maxN) {
                if (gcd(n, d) == 1) {
                    pool.add(IrreducibleFraction(n, d))
                }
            }
        }
        if (pool.isEmpty()) {
            pool.add(IrreducibleFraction(1, 2))
        }
        poolCache[key] = pool
        return pool
    }

    fun generateFractionQuestion(
        minNumerator: Int,
        maxNumerator: Int,
        minDenominator: Int,
        maxDenominator: Int,
        useCourseValues: Boolean = false
    ): GeneratedFractionQuestion {
        val isFractionToDecimal = Random.nextBoolean()
        val pool = buildIrreduciblePool(minNumerator, maxNumerator, minDenominator, maxDenominator)
        val selected = pool[Random.nextInt(pool.size)]
        val num = selected.n
        val den = selected.d

        val rawVal = num.toDouble() / den.toDouble()
        val courseDecimals = CourseValuesData.getCourseDecimalsForFraction(num, den)
        val primaryCourseDec = courseDecimals.firstOrNull() ?: rawVal

        val formattedDecimal = if (useCourseValues) {
            String.format(Locale.US, "%.4f", primaryCourseDec).trimEnd('0').trimEnd('.')
        } else {
            String.format(Locale.US, "%.4f", rawVal).trimEnd('0').trimEnd('.')
        }

        val unitDecimal = String.format(Locale.US, "%.4f", 1.0 / den.toDouble()).trimEnd('0').trimEnd('.')
        val steps = if (isFractionToDecimal) {
            if (num == 1) {
                listOf(
                    "Recall unit fraction 1/$den ≈ $formattedDecimal"
                )
            } else {
                listOf(
                    "Start from unit fraction: 1/$den ≈ $unitDecimal",
                    "Multiply by $num: $num × (1/$den) ≈ $formattedDecimal"
                )
            }
        } else {
            listOf(
                "Identify decimal $formattedDecimal as a multiple of 1/$den ≈ $unitDecimal",
                "Simplify fraction to irreducible form: $num/$den"
            )
        }

        return if (isFractionToDecimal) {
            GeneratedFractionQuestion(
                question = "$num/$den",
                answer = formattedDecimal,
                isFractionToDecimal = true,
                numerator = num,
                denominator = den,
                solutionSteps = steps
            )
        } else {
            GeneratedFractionQuestion(
                question = formattedDecimal,
                answer = "$num/$den",
                isFractionToDecimal = false,
                numerator = num,
                denominator = den,
                solutionSteps = steps
            )
        }
    }
}
