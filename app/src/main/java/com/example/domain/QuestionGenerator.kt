package com.example.domain

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

data class MathProblem(
    val num1: Double,
    val num2: Double,
    val operation: String,
    val questionDisplay: String,
    val answer: String,
    val numericAnswer: Double
)

object QuestionGenerator {

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

    fun randomInRange(min: Int, max: Int): Int {
        if (min >= max) return min
        return Random.nextInt(min, max + 1)
    }

    private fun generateNumber(
        digits: Int,
        diff: Difficulty,
        operation: String,
        isSecondNum: Boolean
    ): Int {
        var min = 1
        for (i in 0 until digits - 1) min *= 10
        val max = min * 10 - 1
        var num = randomInRange(min, max)

        if (diff == Difficulty.HARD) {
            when (Random.nextInt(4)) {
                0 -> {
                    num = (num / 10) * 10 + 9
                    if (num > max) num = max
                }
                1 -> {
                    num = (num / 10) * 10 + 1
                    if (num < min) num = min
                }
                2 -> {
                    num = if (Random.nextBoolean()) {
                        min + randomInRange(0, 2)
                    } else {
                        max - randomInRange(0, 2)
                    }
                }
                3 -> {}
            }
        } else if (diff == Difficulty.EASY && operation == "*" && isSecondNum) {
            num = randomInRange(2, 9)
        }
        return num
    }

    fun generateStandardQuestion(
        operation: String,
        digits: Int,
        diff: Difficulty,
        allowRemainder: Boolean,
        allowNegativeResults: Boolean
    ): MathProblem {
        var num1 = generateNumber(digits, diff, operation, false)
        var num2 = generateNumber(digits, diff, operation, true)
        val answer: Double
        val answerStr: String

        when (operation) {
            "+" -> {
                answer = (num1 + num2).toDouble()
                answerStr = answer.roundToLong().toString()
            }
            "-" -> {
                if (!allowNegativeResults && num1 < num2) {
                    val temp = num1
                    num1 = num2
                    num2 = temp
                }
                answer = (num1 - num2).toDouble()
                answerStr = answer.roundToLong().toString()
            }
            "*" -> {
                answer = (num1.toLong() * num2.toLong()).toDouble()
                answerStr = answer.roundToLong().toString()
            }
            "/" -> {
                if (!allowRemainder) {
                    val divDigits = maxOf(1, digits / 2)
                    num2 = generateNumber(divDigits, diff, operation, true)
                    if (num2 == 0) num2 = 2
                    val targetMin = 10.0.pow((digits - 1).toDouble()).toInt()
                    val targetMax = 10.0.pow(digits.toDouble()).toInt() - 1
                    val qMin = maxOf(1, ceil(targetMin.toDouble() / num2).toInt())
                    val qMax = maxOf(qMin, floor(targetMax.toDouble() / num2).toInt())
                    val q = randomInRange(qMin, qMax)
                    num1 = num2 * q
                    answer = q.toDouble()
                    answerStr = q.toString()
                } else {
                    if (num2 == 0) num2 = 2
                    val divResult = num1.toDouble() / num2.toDouble()
                    val rounded = String.format(Locale.US, "%.4f", divResult).toDouble()
                    answer = rounded
                    answerStr = String.format(Locale.US, "%.4f", rounded).trimEnd('0').trimEnd('.')
                }
            }
            else -> {
                answer = 0.0
                answerStr = "0"
            }
        }

        val opSymbol = when (operation) {
            "+" -> "+"
            "-" -> "−"
            "*" -> "×"
            "/" -> "÷"
            else -> operation
        }

        return MathProblem(
            num1 = num1.toDouble(),
            num2 = num2.toDouble(),
            operation = opSymbol,
            questionDisplay = "$num1 $opSymbol $num2",
            answer = answerStr,
            numericAnswer = answer
        )
    }

    fun generatePercentageQuestion(diff: Difficulty): MathProblem {
        val question: String
        val answer: Double

        if (diff == Difficulty.EASY) {
            val easyPcts = intArrayOf(10, 25, 50, 75)
            val pct = easyPcts[randomInRange(0, easyPcts.size - 1)]
            val baseValue: Int
            when (pct) {
                75 -> {
                    val m = randomInRange(2, 12)
                    baseValue = m * 4
                    answer = (baseValue * 3.0) / 4.0
                }
                25 -> {
                    val m = randomInRange(2, 10)
                    baseValue = m * 4
                    answer = baseValue / 4.0
                }
                50 -> {
                    val m = randomInRange(2, 10)
                    baseValue = m * 2
                    answer = baseValue / 2.0
                }
                else -> {
                    val m = randomInRange(2, 10)
                    baseValue = m * 10
                    answer = baseValue / 10.0
                }
            }
            question = "$pct% of $baseValue"
        } else {
            val isPercentOf = Random.nextBoolean()
            if (isPercentOf) {
                val pct = randomInRange(1, 20) * 5
                val multiplier = 100 / gcd(pct, 100)
                val k = randomInRange(1, maxOf(1, 100 / multiplier))
                val value = k * multiplier
                answer = (pct.toDouble() * value.toDouble()) / 100.0
                question = "$pct% of $value"
            } else {
                val pct = randomInRange(1, 20) * 5
                val multiplier = 100 / gcd(pct, 100)
                val k = randomInRange(1, maxOf(1, 100 / multiplier))
                val value = k * multiplier
                val result = (value.toDouble() * pct.toDouble()) / 100.0
                val resultInt = result.roundToInt()
                answer = pct.toDouble()
                question = "$resultInt is what % of $value"
            }
        }

        return MathProblem(
            num1 = 0.0,
            num2 = 0.0,
            operation = "%",
            questionDisplay = question,
            answer = answer.roundToLong().toString(),
            numericAnswer = answer
        )
    }

    fun generateSquareRootQuestion(diff: Difficulty, customRange: Pair<Int, Int>? = null): MathProblem {
        val (minRoot, maxRoot) = customRange ?: when (diff) {
            Difficulty.EASY -> Pair(1, 10)
            Difficulty.MEDIUM -> Pair(11, 25)
            Difficulty.HARD -> Pair(26, 35)
        }
        val root = randomInRange(minRoot, maxRoot)
        val square = root * root
        return MathProblem(
            num1 = 0.0,
            num2 = 0.0,
            operation = "√",
            questionDisplay = "√$square",
            answer = root.toString(),
            numericAnswer = root.toDouble()
        )
    }

    fun generateSquareQuestion(customRange: Pair<Int, Int>? = null): MathProblem {
        val (min, max) = customRange ?: Pair(1, 25)
        val n = randomInRange(min, max)
        val square = n * n
        return MathProblem(
            num1 = n.toDouble(),
            num2 = n.toDouble(),
            operation = "²",
            questionDisplay = "$n²",
            answer = square.toString(),
            numericAnswer = square.toDouble()
        )
    }

    fun generateTableQuestion(range: Pair<Int, Int>): MathProblem {
        val n1 = randomInRange(range.first, range.second)
        val n2 = randomInRange(1, 12)
        val product = n1 * n2
        return MathProblem(
            num1 = n1.toDouble(),
            num2 = n2.toDouble(),
            operation = "×",
            questionDisplay = "$n1 × $n2",
            answer = product.toString(),
            numericAnswer = product.toDouble()
        )
    }

    fun generateFactorFindingQuestion(range: Pair<Int, Int>): MathProblem {
        val n1 = randomInRange(range.first, range.second)
        val n2 = randomInRange(1, 12)
        val product = n1 * n2
        return MathProblem(
            num1 = product.toDouble(),
            num2 = n1.toDouble(),
            operation = "×",
            questionDisplay = "$product = $n1 × __",
            answer = n2.toString(),
            numericAnswer = n2.toDouble()
        )
    }

    fun generateApproximationQuestion(diff: Difficulty, allowNegativeResults: Boolean): MathProblem {
        val ops = arrayOf("+", "-", "*")
        val op = ops[Random.nextInt(if (diff == Difficulty.EASY) 2 else 3)]
        val digitsCount = when (diff) {
            Difficulty.EASY -> 2
            Difficulty.MEDIUM -> 3
            Difficulty.HARD -> 4
        }
        val min = 10.0.pow((digitsCount - 1).toDouble()).toInt()
        val max = 10.0.pow(digitsCount.toDouble()).toInt() - 1

        var num1 = randomInRange(min, max)
        var num2 = randomInRange(min, max)
        val rounding = when (diff) {
            Difficulty.EASY -> 10
            Difficulty.MEDIUM -> 100
            Difficulty.HARD -> 1000
        }

        val approxNum1 = ((num1 + rounding / 2) / rounding) * rounding
        val approxNum2 = ((num2 + rounding / 2) / rounding) * rounding
        val n1 = if (approxNum1 == 0) min else approxNum1
        val n2 = if (approxNum2 == 0) min else approxNum2

        val approxAnswer: Long = when (op) {
            "+" -> (n1 + n2).toLong()
            "-" -> {
                if (!allowNegativeResults && num1 < num2) {
                    val t = num1
                    num1 = num2
                    num2 = t
                    val a1 = ((num1 + rounding / 2) / rounding) * rounding
                    val a2 = ((num2 + rounding / 2) / rounding) * rounding
                    (a1 - a2).toLong()
                } else {
                    (n1 - n2).toLong()
                }
            }
            else -> n1.toLong() * n2.toLong()
        }

        val opSym = if (op == "+") "+" else if (op == "-") "−" else "×"
        return MathProblem(
            num1 = 0.0,
            num2 = 0.0,
            operation = "≈",
            questionDisplay = "$num1 $opSym $num2 ≈ ?",
            answer = approxAnswer.toString(),
            numericAnswer = approxAnswer.toDouble()
        )
    }

    fun generateNumberSeriesQuestion(diff: Difficulty): MathProblem {
        val length = if (diff == Difficulty.EASY) 5 else 6
        val patterns = arrayOf("arithmetic", "geometric", "square", "fibonacci")
        val pattern = patterns[Random.nextInt(patterns.size)]
        var series = mutableListOf<Int>()

        when (pattern) {
            "arithmetic" -> {
                val start = randomInRange(1, 20)
                val diffVal = randomInRange(2, 10)
                for (i in 0 until length) series.add(start + i * diffVal)
            }
            "geometric" -> {
                val start = randomInRange(1, 5)
                val ratio = randomInRange(2, 3)
                var v = start
                for (i in 0 until length) {
                    if (v > 1000) {
                        series = mutableListOf(1, 2, 4, 8, 16, 32).subList(0, length)
                        break
                    }
                    series.add(v)
                    v *= ratio
                }
            }
            "square" -> {
                val start = randomInRange(1, 5)
                for (i in 0 until length) series.add((start + i) * (start + i))
            }
            "fibonacci" -> {
                val fib = intArrayOf(1, 1, 2, 3, 5, 8, 13, 21)
                for (i in 0 until length) series.add(fib[i])
            }
        }

        if (series.size < length) {
            val start = randomInRange(1, 10)
            val d = randomInRange(2, 5)
            series.clear()
            for (i in 0 until length) series.add(start + i * d)
        }

        val missingIndex = length / 2
        val answer = series[missingIndex]
        val displayList = series.mapIndexed { idx, v -> if (idx == missingIndex) "?" else v.toString() }

        return MathProblem(
            num1 = 0.0,
            num2 = 0.0,
            operation = "Series",
            questionDisplay = displayList.joinToString(", "),
            answer = answer.toString(),
            numericAnswer = answer.toDouble()
        )
    }

    fun generateRatioQuestion(diff: Difficulty): MathProblem {
        val isFindRatio = Random.nextBoolean()
        return if (isFindRatio) {
            val a = randomInRange(2, if (diff == Difficulty.HARD) 20 else 10)
            val b = randomInRange(2, if (diff == Difficulty.HARD) 20 else 10)
            val mult = randomInRange(2, 5)
            val bVal = b * mult
            val ans = a * mult
            MathProblem(
                num1 = 0.0,
                num2 = 0.0,
                operation = "Ratio",
                questionDisplay = "If A:B = $a:$b & B = $bVal, A = ?",
                answer = ans.toString(),
                numericAnswer = ans.toDouble()
            )
        } else {
            val ratio = randomInRange(2, if (diff == Difficulty.HARD) 10 else 5)
            val a = randomInRange(2, 10) * ratio
            val ans = a / ratio
            MathProblem(
                num1 = 0.0,
                num2 = 0.0,
                operation = "Ratio",
                questionDisplay = "In ratio $ratio:1, if 1st = $a, 2nd = ?",
                answer = ans.toString(),
                numericAnswer = ans.toDouble()
            )
        }
    }

    fun generateChainCalculationQuestion(diff: Difficulty): MathProblem {
        val operations = arrayOf("+", "-", "*")
        val numOps = if (diff == Difficulty.EASY) 2 else 3
        val numbers = mutableListOf(randomInRange(2, if (diff == Difficulty.EASY) 9 else 20))
        val ops = mutableListOf<String>()

        for (i in 0 until numOps) {
            numbers.add(randomInRange(2, if (diff == Difficulty.EASY) 9 else 15))
            ops.add(operations[randomInRange(0, operations.size - 1)])
        }

        val displayBuilder = StringBuilder("${numbers[0]}")
        for (i in 0 until numOps) {
            val sym = if (ops[i] == "+") "+" else if (ops[i] == "-") "−" else "×"
            displayBuilder.append(" $sym ${numbers[i + 1]}")
        }

        // Evaluate using BODMAS: * first, then + and -
        val tokens = mutableListOf<Any>()
        tokens.add(numbers[0].toLong())
        for (i in 0 until numOps) {
            tokens.add(ops[i])
            tokens.add(numbers[i + 1].toLong())
        }

        var j = 1
        while (j < tokens.size) {
            if (tokens[j] == "*") {
                val left = tokens[j - 1] as Long
                val right = tokens[j + 1] as Long
                val prod = left * right
                tokens[j - 1] = prod
                tokens.removeAt(j + 1)
                tokens.removeAt(j)
            } else {
                j += 2
            }
        }

        var k = 1
        while (k < tokens.size) {
            val opToken = tokens[k] as String
            val left = tokens[k - 1] as Long
            val right = tokens[k + 1] as Long
            val result = if (opToken == "+") left + right else left - right
            tokens[k - 1] = result
            tokens.removeAt(k + 1)
            tokens.removeAt(k)
        }

        val finalAns = tokens[0] as Long
        return MathProblem(
            num1 = 0.0,
            num2 = 0.0,
            operation = "Chain",
            questionDisplay = displayBuilder.toString(),
            answer = finalAns.toString(),
            numericAnswer = finalAns.toDouble()
        )
    }
}
