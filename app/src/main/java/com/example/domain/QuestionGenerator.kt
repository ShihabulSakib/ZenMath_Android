package com.example.domain

import com.example.data.StructuredSymbol
import com.example.data.StructuredUnit
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Extended problem model (Section 1.1).
 * Preserves all existing fields (`num1`, `num2`, `operation`, `questionDisplay`, `answer`, `numericAnswer`)
 * while adding `topicTag`, `answerSpec`, optional `unitText`, optional `subjectLabel`, and `solutionSteps`.
 */
data class MathProblem(
    val num1: Double,
    val num2: Double,
    val operation: String,
    val questionDisplay: String,
    val answer: String,
    val numericAnswer: Double,
    val topicTag: String = operation,
    val answerSpec: AnswerSpec = AnswerSpec.exact(numericAnswer.roundToLong()),
    val unitText: String? = null,
    val subjectLabel: String? = null,
    val solutionSteps: List<String> = emptyList(),
    val mcqOptions: List<String>? = null,
    val confusableIds: List<String> = emptyList(),
    val constantSymbol: StructuredSymbol? = null,
    val structuredUnit: StructuredUnit? = null
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
        val topicTag: String
        val spec: AnswerSpec
        val steps: List<String>

        when (operation) {
            "+" -> {
                val sum = num1.toLong() + num2.toLong()
                answer = sum.toDouble()
                answerStr = sum.toString()
                topicTag = "addition"
                spec = AnswerSpec.exact(sum)
                steps = listOf(
                    "Split by place value: $num1 + $num2",
                    "Sum = $sum"
                )
            }
            "-" -> {
                if (!allowNegativeResults && num1 < num2) {
                    val temp = num1
                    num1 = num2
                    num2 = temp
                }
                val diffVal = num1.toLong() - num2.toLong()
                answer = diffVal.toDouble()
                answerStr = diffVal.toString()
                topicTag = "subtraction"
                spec = AnswerSpec.exact(diffVal)
                steps = listOf(
                    "Compute difference: $num1 − $num2",
                    "Result = $diffVal"
                )
            }
            "*" -> {
                val prod = num1.toLong() * num2.toLong()
                answer = prod.toDouble()
                answerStr = prod.toString()
                topicTag = "multiplication"
                spec = AnswerSpec.exact(prod)
                val tens = (num2 / 10) * 10
                val ones = num2 % 10
                steps = if (tens > 0 && ones > 0) {
                    listOf(
                        "Decompose $num2 = $tens + $ones",
                        "$num1 × $tens = ${num1.toLong() * tens}, $num1 × $ones = ${num1.toLong() * ones}",
                        "Add products: ${num1.toLong() * tens} + ${num1.toLong() * ones} = $prod"
                    )
                } else {
                    listOf("Multiply $num1 × $num2 = $prod")
                }
            }
            "/" -> {
                topicTag = "division"
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
                    spec = AnswerSpec.exact(q.toLong())
                    steps = listOf(
                        "Find quotient: $num1 ÷ $num2",
                        "Since $num2 × $q = $num1, answer = $q"
                    )
                } else {
                    if (num2 == 0) num2 = 2
                    val divResult = num1.toDouble() / num2.toDouble()
                    val rounded = String.format(Locale.US, "%.4f", divResult).toDouble()
                    answer = rounded
                    answerStr = String.format(Locale.US, "%.4f", rounded).trimEnd('0').trimEnd('.')
                    spec = AnswerSpec.AbsoluteTolerance(expected = rounded, tolerance = 0.01, exactCourseValues = listOf(rounded, divResult))
                    steps = listOf(
                        "Integer part: ${num1 / num2} with remainder ${num1 % num2}",
                        "Decimal remainder: ${num1 % num2} / $num2 ≈ ${String.format(Locale.US, "%.4f", (num1 % num2).toDouble() / num2).trimEnd('0').trimEnd('.')}",
                        "Result ≈ $answerStr"
                    )
                }
            }
            else -> {
                answer = 0.0
                answerStr = "0"
                topicTag = "addition"
                spec = AnswerSpec.exact(0L)
                steps = listOf("Result = 0")
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
            numericAnswer = answer,
            topicTag = topicTag,
            answerSpec = spec,
            solutionSteps = steps
        )
    }

    fun generatePercentageQuestion(diff: Difficulty): MathProblem {
        val question: String
        val answer: Double
        val steps: List<String>

        if (diff == Difficulty.EASY) {
            val easyPcts = intArrayOf(10, 25, 50, 75)
            val pct = easyPcts[randomInRange(0, easyPcts.size - 1)]
            val baseValue: Int
            when (pct) {
                75 -> {
                    val m = randomInRange(2, 12)
                    baseValue = m * 4
                    answer = (baseValue * 3.0) / 4.0
                    steps = listOf("75% = 3/4", "$baseValue ÷ 4 = $m, then $m × 3 = ${answer.roundToLong()}")
                }
                25 -> {
                    val m = randomInRange(2, 10)
                    baseValue = m * 4
                    answer = baseValue / 4.0
                    steps = listOf("25% = 1/4", "$baseValue ÷ 4 = ${answer.roundToLong()}")
                }
                50 -> {
                    val m = randomInRange(2, 10)
                    baseValue = m * 2
                    answer = baseValue / 2.0
                    steps = listOf("50% = 1/2", "$baseValue ÷ 2 = ${answer.roundToLong()}")
                }
                else -> {
                    val m = randomInRange(2, 10)
                    baseValue = m * 10
                    answer = baseValue / 10.0
                    steps = listOf("10% = 1/10", "$baseValue ÷ 10 = ${answer.roundToLong()}")
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
                steps = listOf(
                    "Write $pct% as $pct / 100",
                    "($pct × $value) / 100 = ${answer.roundToLong()}"
                )
            } else {
                val pct = randomInRange(1, 20) * 5
                val multiplier = 100 / gcd(pct, 100)
                val k = randomInRange(1, maxOf(1, 100 / multiplier))
                val value = k * multiplier
                val result = (value.toDouble() * pct.toDouble()) / 100.0
                val resultInt = result.roundToInt()
                answer = pct.toDouble()
                question = "$resultInt is what % of $value"
                steps = listOf(
                    "Compute ($resultInt / $value) × 100",
                    "Percentage = ${answer.roundToLong()}%"
                )
            }
        }

        val ansLong = answer.roundToLong()
        return MathProblem(
            num1 = 0.0,
            num2 = 0.0,
            operation = "%",
            questionDisplay = question,
            answer = ansLong.toString(),
            numericAnswer = answer,
            topicTag = "percentage",
            answerSpec = AnswerSpec.exact(ansLong),
            solutionSteps = steps
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
            numericAnswer = root.toDouble(),
            topicTag = "square-root",
            answerSpec = AnswerSpec.exact(root.toLong()),
            solutionSteps = listOf(
                "Since $root × $root = $square",
                "√$square = $root"
            )
        )
    }

    fun generateSquareQuestion(customRange: Pair<Int, Int>? = null): MathProblem {
        val (min, max) = customRange ?: Pair(1, 25)
        val n = randomInRange(min, max)
        val square = n * n
        val steps = if (n in 11..25) {
            val tens = (n / 10) * 10
            val b = n % 10
            listOf(
                "Use ($tens + $b)^2 = $tens^2 + 2·$tens·$b + $b^2",
                "Tens part: ${tens * tens}, cross term: ${2 * tens * b}, last-digit square: ${b * b}",
                "${tens * tens} + ${2 * tens * b} + ${b * b} = $square"
            )
        } else {
            listOf("$n × $n = $square")
        }
        return MathProblem(
            num1 = n.toDouble(),
            num2 = n.toDouble(),
            operation = "²",
            questionDisplay = "$n²",
            answer = square.toString(),
            numericAnswer = square.toDouble(),
            topicTag = "square",
            answerSpec = AnswerSpec.exact(square.toLong()),
            solutionSteps = steps
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
            numericAnswer = product.toDouble(),
            topicTag = "multiplication-table",
            answerSpec = AnswerSpec.exact(product.toLong()),
            solutionSteps = listOf("$n1 × $n2 = $product")
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
            numericAnswer = n2.toDouble(),
            topicTag = "factor-finding",
            answerSpec = AnswerSpec.exact(n2.toLong()),
            solutionSteps = listOf("Divide $product by $n1: $product ÷ $n1 = $n2")
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

        if (op == "-" && !allowNegativeResults && num1 < num2) {
            val t = num1
            num1 = num2
            num2 = t
        }

        val approxNum1 = ((num1 + rounding / 2) / rounding) * rounding
        val approxNum2 = ((num2 + rounding / 2) / rounding) * rounding
        val n1 = if (approxNum1 == 0) min else approxNum1
        val n2 = if (approxNum2 == 0) min else approxNum2

        val approxAnswer: Long = when (op) {
            "+" -> (n1 + n2).toLong()
            "-" -> (n1 - n2).toLong()
            else -> n1.toLong() * n2.toLong()
        }

        val trueAnswer: Double = when (op) {
            "+" -> (num1 + num2).toDouble()
            "-" -> (num1 - num2).toDouble()
            else -> num1.toDouble() * num2.toDouble()
        }

        val opSym = if (op == "+") "+" else if (op == "-") "−" else "×"
        return MathProblem(
            num1 = num1.toDouble(),
            num2 = num2.toDouble(),
            operation = "≈",
            questionDisplay = "$num1 $opSym $num2 ≈ ?",
            answer = approxAnswer.toString(),
            numericAnswer = approxAnswer.toDouble(),
            topicTag = "approximation",
            answerSpec = AnswerSpec.relative(
                expected = approxAnswer.toDouble(),
                exactCourseValues = listOf(approxAnswer.toDouble(), trueAnswer)
            ),
            solutionSteps = listOf(
                "Round to nearest $rounding: $num1 ≈ $n1, $num2 ≈ $n2",
                "Compute: $n1 $opSym $n2 = $approxAnswer"
            )
        )
    }

    fun generateNumberSeriesQuestion(diff: Difficulty): MathProblem {
        val length = if (diff == Difficulty.EASY) 5 else 6
        val patterns = arrayOf("arithmetic", "geometric", "square", "fibonacci")
        val pattern = patterns[Random.nextInt(patterns.size)]
        var series = mutableListOf<Int>()
        var ruleDesc = ""

        when (pattern) {
            "arithmetic" -> {
                val start = randomInRange(1, 20)
                val diffVal = randomInRange(2, 10)
                for (i in 0 until length) series.add(start + i * diffVal)
                ruleDesc = "Arithmetic progression with common difference +$diffVal"
            }
            "geometric" -> {
                val start = randomInRange(1, 5)
                val ratio = randomInRange(2, 3)
                var v = start
                for (i in 0 until length) {
                    if (v > 1000) {
                        series = mutableListOf(1, 2, 4, 8, 16, 32).subList(0, length)
                        ruleDesc = "Geometric progression multiplying by 2 each step"
                        break
                    }
                    series.add(v)
                    v *= ratio
                }
                if (ruleDesc.isEmpty()) ruleDesc = "Geometric progression multiplying by $ratio each step"
            }
            "square" -> {
                val start = randomInRange(1, 5)
                for (i in 0 until length) series.add((start + i) * (start + i))
                ruleDesc = "Consecutive squares starting from $start^2"
            }
            "fibonacci" -> {
                val fib = intArrayOf(1, 1, 2, 3, 5, 8, 13, 21)
                for (i in 0 until length) series.add(fib[i])
                ruleDesc = "Each term is the sum of the two preceding terms"
            }
        }

        if (series.size < length) {
            val start = randomInRange(1, 10)
            val d = randomInRange(2, 5)
            series.clear()
            for (i in 0 until length) series.add(start + i * d)
            ruleDesc = "Arithmetic progression with difference +$d"
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
            numericAnswer = answer.toDouble(),
            topicTag = "number-series",
            answerSpec = AnswerSpec.exact(answer.toLong()),
            solutionSteps = listOf(
                "Pattern: $ruleDesc",
                "Missing term at position ${missingIndex + 1} is $answer"
            )
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
                numericAnswer = ans.toDouble(),
                topicTag = "ratio",
                answerSpec = AnswerSpec.exact(ans.toLong()),
                solutionSteps = listOf(
                    "Scale factor = B / $b = $bVal / $b = $mult",
                    "A = $a × $mult = $ans"
                )
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
                numericAnswer = ans.toDouble(),
                topicTag = "ratio",
                answerSpec = AnswerSpec.exact(ans.toLong()),
                solutionSteps = listOf(
                    "Divide 1st term by $ratio: $a ÷ $ratio = $ans"
                )
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

        val steps = mutableListOf<String>()
        var j = 1
        while (j < tokens.size) {
            if (tokens[j] == "*") {
                val left = tokens[j - 1] as Long
                val right = tokens[j + 1] as Long
                val prod = left * right
                steps.add("Multiply first: $left × $right = $prod")
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
            val sym = if (opToken == "+") "+" else "−"
            steps.add("Then $left $sym $right = $result")
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
            numericAnswer = finalAns.toDouble(),
            topicTag = "chain-calculation",
            answerSpec = AnswerSpec.exact(finalAns),
            solutionSteps = steps
        )
    }
}
