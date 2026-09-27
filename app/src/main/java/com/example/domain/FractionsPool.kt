package com.example.domain

import java.util.Locale
import kotlin.random.Random

data class IrreducibleFraction(val n: Int, val d: Int)

data class GeneratedFractionQuestion(
    val question: String,
    val answer: String,
    val isFractionToDecimal: Boolean
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
                if (gcd(n, d) === 1) {
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
        maxDenominator: Int
    ): GeneratedFractionQuestion {
        val isFractionToDecimal = Random.nextBoolean()
        val pool = buildIrreduciblePool(minNumerator, maxNumerator, minDenominator, maxDenominator)
        val selected = pool[Random.nextInt(pool.size)]
        val num = selected.n
        val den = selected.d

        val rawVal = num.toDouble() / den.toDouble()
        // Format to 4 decimal places without trailing zeros if possible, matching JS parseFloat((rawVal).toFixed(4)).toString()
        val formattedDecimal = String.format(Locale.US, "%.4f", rawVal).trimEnd('0').trimEnd('.')

        return if (isFractionToDecimal) {
            GeneratedFractionQuestion(
                question = "$num/$den",
                answer = formattedDecimal,
                isFractionToDecimal = true
            )
        } else {
            GeneratedFractionQuestion(
                question = formattedDecimal,
                answer = "$num/$den",
                isFractionToDecimal = false
            )
        }
    }
}
