package com.example.domain

import com.example.data.ConstantEntry
import com.example.data.ConstantsData
import com.example.data.ConstantsLoader
import com.example.data.CourseValuesData
import com.example.data.MemoryTableCategory
import com.example.data.PhSpeciesData
import com.example.data.StructuredSymbol
import com.example.data.StructuredUnit
import com.example.ui.components.RichTextHelper
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sqrt
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
    val structuredUnit: StructuredUnit? = null,
    val constantEntry: ConstantEntry? = null
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
        val n1: Double
        val n2: Double

        when (diff) {
            Difficulty.EASY -> {
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
                n1 = pct.toDouble()
                n2 = baseValue.toDouble()
            }

            Difficulty.MEDIUM -> {
                val isPercentOf = Random.nextBoolean()
                val pct = randomInRange(1, 19) * 5
                val multiplier = 100 / gcd(pct, 100)
                val k = randomInRange(2, maxOf(2, 200 / multiplier))
                val value = k * multiplier
                if (isPercentOf) {
                    answer = (pct.toDouble() * value.toDouble()) / 100.0
                    question = "$pct% of $value"
                    steps = listOf(
                        "Write $pct% as ${pct / gcd(pct, 100)}/${100 / gcd(pct, 100)}",
                        "($pct × $value) ÷ 100 = ${answer.roundToLong()}"
                    )
                    n1 = pct.toDouble()
                    n2 = value.toDouble()
                } else {
                    val result = (value.toDouble() * pct.toDouble()) / 100.0
                    val resultInt = result.roundToInt()
                    answer = pct.toDouble()
                    question = "$resultInt is what % of $value"
                    steps = listOf(
                        "Compute ($resultInt ÷ $value) × 100",
                        "Percentage = ${answer.roundToLong()}%"
                    )
                    n1 = resultInt.toDouble()
                    n2 = value.toDouble()
                }
            }

            Difficulty.HARD -> {
                // Hard: fractional percentages (12.5%, 37.5%, 62.5%, 87.5%) and larger bases
                data class FractionalPct(val label: String, val num: Int, val den: Int, val pctVal: Double)
                val hardFractions = listOf(
                    FractionalPct("12.5%", 1, 8, 12.5),
                    FractionalPct("37.5%", 3, 8, 37.5),
                    FractionalPct("62.5%", 5, 8, 62.5),
                    FractionalPct("87.5%", 7, 8, 87.5)
                )
                if (Random.nextBoolean()) {
                    val chosen = hardFractions[randomInRange(0, hardFractions.size - 1)]
                    val k = randomInRange(12, 65)
                    val baseValue = k * chosen.den
                    val ansLong = k.toLong() * chosen.num.toLong()
                    answer = ansLong.toDouble()
                    question = "${chosen.label} of $baseValue"
                    steps = listOf(
                        "Recognize ${chosen.label} = ${chosen.num}/${chosen.den}",
                        "Divide base by ${chosen.den}: $baseValue ÷ ${chosen.den} = $k",
                        "Multiply by ${chosen.num}: $k × ${chosen.num} = $ansLong"
                    )
                    n1 = chosen.pctVal
                    n2 = baseValue.toDouble()
                } else {
                    val hardPcts = intArrayOf(15, 35, 45, 65, 85, 115, 125, 135, 175)
                    val pct = hardPcts[randomInRange(0, hardPcts.size - 1)]
                    val multiplier = 100 / gcd(pct, 100)
                    val k = randomInRange(12, 45)
                    val baseValue = k * multiplier
                    val ansLong = (pct.toLong() * baseValue.toLong()) / 100L
                    answer = ansLong.toDouble()
                    question = "$pct% of $baseValue"
                    val tenPct = baseValue / 10.0
                    steps = listOf(
                        "Split $pct% into tens and fives on base $baseValue (10% = ${String.format(Locale.US, "%.1f", tenPct)})",
                        "($pct × $baseValue) ÷ 100 = $ansLong"
                    )
                    n1 = pct.toDouble()
                    n2 = baseValue.toDouble()
                }
            }
        }

        val ansLong = answer.roundToLong()
        return MathProblem(
            num1 = n1,
            num2 = n2,
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
            num1 = square.toDouble(),
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

    fun generateNumberSeriesQuestion(diff: Difficulty, forcePattern: String? = null): MathProblem {
        val length = when (diff) {
            Difficulty.EASY -> 5
            Difficulty.MEDIUM -> 6
            Difficulty.HARD -> 7
        }
        val patterns = when (diff) {
            Difficulty.EASY -> arrayOf("arithmetic", "geometric", "square", "fibonacci")
            Difficulty.MEDIUM -> arrayOf("arithmetic", "geometric", "square", "fibonacci")
            Difficulty.HARD -> arrayOf("second_order", "alternating", "fibonacci")
        }
        val pattern = forcePattern ?: patterns[Random.nextInt(patterns.size)]
        val series = mutableListOf<Int>()
        var ruleDesc = ""

        when (pattern) {
            "arithmetic" -> {
                val start = randomInRange(1, 20)
                val diffVal = randomInRange(2, if (diff == Difficulty.EASY) 8 else 15)
                for (i in 0 until length) series.add(start + i * diffVal)
                ruleDesc = "Arithmetic progression with common difference +$diffVal"
            }
            "geometric" -> {
                val start = randomInRange(1, 4)
                val ratio = randomInRange(2, 3)
                var v = start
                for (i in 0 until length) {
                    series.add(v)
                    v *= ratio
                }
                ruleDesc = "Geometric progression multiplying by $ratio each step"
            }
            "square" -> {
                val start = randomInRange(1, 6)
                for (i in 0 until length) series.add((start + i) * (start + i))
                ruleDesc = "Consecutive squares starting from $start^2"
            }
            "fibonacci" -> {
                // Random start pair instead of a fixed array
                val f0 = randomInRange(1, 7)
                val f1 = randomInRange(2, 9)
                series.add(f0)
                series.add(f1)
                for (i in 2 until length) {
                    series.add(series[i - 2] + series[i - 1])
                }
                ruleDesc = "Fibonacci-like rule: each term is the sum of the two previous terms (start $f0, $f1)"
            }
            "second_order" -> {
                val start = randomInRange(2, 12)
                val firstDiff = randomInRange(2, 6)
                val secondDiff = randomInRange(2, 5)
                var curr = start
                var stepDiff = firstDiff
                for (i in 0 until length) {
                    series.add(curr)
                    curr += stepDiff
                    stepDiff += secondDiff
                }
                ruleDesc = "Second-order differences: differences start at +$firstDiff and increase by +$secondDiff each step"
            }
            "alternating" -> {
                val start = randomInRange(10, 25)
                val addStep = randomInRange(5, 12)
                val subStep = randomInRange(2, addStep - 2)
                var curr = start
                for (i in 0 until length) {
                    series.add(curr)
                    curr = if (i % 2 == 0) curr + addStep else curr - subStep
                }
                ruleDesc = "Alternating pattern: +$addStep, then −$subStep"
            }
        }

        if (series.size < length) {
            val start = randomInRange(1, 10)
            val d = randomInRange(2, 5)
            series.clear()
            for (i in 0 until length) series.add(start + i * d)
            ruleDesc = "Arithmetic progression with difference +$d"
        }

        // Vary the hidden position across indices 1..(length - 1)
        val missingIndex = randomInRange(1, length - 1)
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
        return when (diff) {
            Difficulty.EASY -> {
                val isFindRatio = Random.nextBoolean()
                if (isFindRatio) {
                    val a = randomInRange(2, 8)
                    val b = randomInRange(2, 8)
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
                            "Scale factor = B ÷ $b = $bVal ÷ $b = $mult",
                            "A = $a × $mult = $ans"
                        )
                    )
                } else {
                    val ratio = randomInRange(2, 5)
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

            Difficulty.MEDIUM -> {
                // Medium: Three-term ratios A:B:C
                val a = randomInRange(1, 5)
                val b = randomInRange(2, 6)
                val c = randomInRange(2, 7)
                val mult = randomInRange(2, 6)
                if (Random.nextBoolean()) {
                    val bVal = b * mult
                    val cVal = c * mult
                    MathProblem(
                        num1 = 0.0,
                        num2 = 0.0,
                        operation = "Ratio",
                        questionDisplay = "If A:B:C = $a:$b:$c & B = $bVal, C = ?",
                        answer = cVal.toString(),
                        numericAnswer = cVal.toDouble(),
                        topicTag = "ratio",
                        answerSpec = AnswerSpec.exact(cVal.toLong()),
                        solutionSteps = listOf(
                            "Find scale factor from B: $bVal ÷ $b = $mult",
                            "C = $c × $mult = $cVal"
                        )
                    )
                } else {
                    val sumParts = a + b + c
                    val total = sumParts * mult
                    val aVal = a * mult
                    MathProblem(
                        num1 = 0.0,
                        num2 = 0.0,
                        operation = "Ratio",
                        questionDisplay = "If A:B:C = $a:$b:$c & A+B+C = $total, A = ?",
                        answer = aVal.toString(),
                        numericAnswer = aVal.toDouble(),
                        topicTag = "ratio",
                        answerSpec = AnswerSpec.exact(aVal.toLong()),
                        solutionSteps = listOf(
                            "Total ratio parts = $a + $b + $c = $sumParts",
                            "1 part = $total ÷ $sumParts = $mult",
                            "A = $a × $mult = $aVal"
                        )
                    )
                }
            }

            Difficulty.HARD -> {
                // Hard: Larger 3-term ratio shares or chained ratios
                val a = randomInRange(3, 9)
                val b = randomInRange(4, 11)
                val c = randomInRange(5, 13)
                val mult = randomInRange(6, 15)
                val sumParts = a + b + c
                val total = sumParts * mult
                val cVal = c * mult
                MathProblem(
                    num1 = 0.0,
                    num2 = 0.0,
                    operation = "Ratio",
                    questionDisplay = "If A:B:C = $a:$b:$c & A+B+C = $total, C = ?",
                    answer = cVal.toString(),
                    numericAnswer = cVal.toDouble(),
                    topicTag = "ratio",
                    answerSpec = AnswerSpec.exact(cVal.toLong()),
                    solutionSteps = listOf(
                        "Sum of ratio parts = $a + $b + $c = $sumParts",
                        "Value per part = $total ÷ $sumParts = $mult",
                        "C = $c × $mult = $cVal"
                    )
                )
            }
        }
    }

    fun generateChainCalculationQuestion(diff: Difficulty): MathProblem {
        if (diff == Difficulty.HARD) {
            // Hard: 4 operations and brackets
            val a = randomInRange(4, 18)
            val b = randomInRange(3, 14)
            val c = randomInRange(2, 9)
            val d = randomInRange(3, 15)
            val e = randomInRange(2, 9)
            val op1Plus = Random.nextBoolean()
            val op2Plus = Random.nextBoolean()
            val op3Plus = Random.nextBoolean()

            val bracket1 = if (op1Plus) a + b else a - b
            val prod1 = bracket1.toLong() * c.toLong()
            val bracket2 = if (op3Plus) d + e else d - e
            val finalAns = if (op2Plus) prod1 + bracket2.toLong() else prod1 - bracket2.toLong()

            val s1 = if (op1Plus) "+" else "−"
            val s2 = if (op2Plus) "+" else "−"
            val s3 = if (op3Plus) "+" else "−"
            val display = "($a $s1 $b) × $c $s2 ($d $s3 $e)"

            val steps = listOf(
                "Evaluate first bracket: ($a $s1 $b) = $bracket1",
                "Multiply by $c: $bracket1 × $c = $prod1",
                "Evaluate second bracket: ($d $s3 $e) = $bracket2",
                "Combine: $prod1 $s2 $bracket2 = $finalAns"
            )

            return MathProblem(
                num1 = 0.0,
                num2 = 0.0,
                operation = "Chain",
                questionDisplay = display,
                answer = finalAns.toString(),
                numericAnswer = finalAns.toDouble(),
                topicTag = "chain-calculation",
                answerSpec = AnswerSpec.exact(finalAns),
                solutionSteps = steps
            )
        }

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

    // =========================================================================
    // 3.1 Memory Tables Drill (Forward, Reverse, Mixed)
    // =========================================================================
    fun generateMemoryTableQuestion(
        direction: MemoryDirection = MemoryDirection.MIXED,
        categories: Set<MemoryTableCategory> = MemoryTableCategory.entries.toSet()
    ): MathProblem {
        val activeCategories = categories.ifEmpty { MemoryTableCategory.entries.toSet() }
        val pool = CourseValuesData.allEntries.filter { it.category in activeCategories }
            .ifEmpty { CourseValuesData.allEntries }
        val entry = pool.random()

        val isReverse = when (direction) {
            MemoryDirection.FORWARD -> false
            MemoryDirection.REVERSE -> true
            MemoryDirection.MIXED -> Random.nextBoolean()
        }

        if (!isReverse) {
            val valStr = formatCleanDouble(entry.courseValue)
            return MathProblem(
                num1 = entry.argument.toDouble(),
                num2 = 0.0,
                operation = "Memory Tables",
                questionDisplay = "${entry.forwardPrompt} = ?",
                answer = valStr,
                numericAnswer = entry.courseValue,
                topicTag = "memory-${entry.category.id}",
                answerSpec = AnswerSpec.AbsoluteTolerance(
                    expected = entry.courseValue,
                    tolerance = 0.05,
                    alsoAcceptExact = listOf(entry.courseValue, entry.trueValue)
                ),
                solutionSteps = listOf(
                    "Memorized ${entry.category.displayName} table value: ${entry.forwardPrompt} ≈ $valStr",
                    "Exact value: ${String.format(Locale.US, "%.4f", entry.trueValue)} (±0.05 accepted)"
                )
            )
        } else {
            // Find any sibling entries in the same category that share the exact same rounded courseValue
            // (e.g. 1/13 and 1/14 -> 0.07, or ∛8 and ∛9 -> 2.0)
            val matchingArgs = CourseValuesData.byCategory(entry.category)
                .filter { abs(it.courseValue - entry.courseValue) < 1e-9 }
                .map { it.argument.toLong() }
                .toSet()

            val valStr = formatCleanDouble(entry.courseValue)
            val prompt = when (entry.category) {
                MemoryTableCategory.SQUARE_ROOT -> "√? ≈ $valStr"
                MemoryTableCategory.CUBE_ROOT -> "∛? ≈ $valStr"
                MemoryTableCategory.LOG_10 -> "log₁₀(?) ≈ $valStr"
                MemoryTableCategory.RECIPROCAL -> "1/? ≈ $valStr"
            }

            return MathProblem(
                num1 = entry.courseValue,
                num2 = 0.0,
                operation = "Memory Tables",
                questionDisplay = prompt,
                answer = entry.argument.toString(),
                numericAnswer = entry.argument.toDouble(),
                topicTag = "memory-${entry.category.id}-reverse",
                answerSpec = AnswerSpec.Exact(
                    expected = entry.argument.toLong(),
                    alternateAccepted = matchingArgs - entry.argument.toLong()
                ),
                solutionSteps = listOf(
                    "From ${entry.category.displayName} table: ${entry.forwardPrompt} ≈ $valStr",
                    if (matchingArgs.size > 1) {
                        "Accepted arguments for $valStr: ${matchingArgs.sorted().joinToString(" or ")}"
                    } else {
                        "Argument = ${entry.argument}"
                    }
                )
            )
        }
    }

    // =========================================================================
    // 3.2 Scientific Notation Drills
    // =========================================================================
    fun generateScientificNotationQuestion(
        subtype: SciNotationSubtype = SciNotationSubtype.ALL
    ): MathProblem {
        val chosen = if (subtype == SciNotationSubtype.ALL) {
            listOf(
                SciNotationSubtype.NORMALIZE,
                SciNotationSubtype.DECIMAL_SHIFT,
                SciNotationSubtype.MULTIPLY_DIVIDE,
                SciNotationSubtype.ROOT_POWER_TEN
            ).random()
        } else {
            subtype
        }

        return when (chosen) {
            SciNotationSubtype.NORMALIZE -> {
                // Generate mantissa in 1.1..9.5 (with 1 decimal place) and non-zero exponent in -4..5
                val mTenths = randomInRange(11, 95).let { if (it % 10 == 0) it + 2 else it }
                val mantissa = mTenths / 10.0
                val exp = listOf(-4, -3, -2, -1, 2, 3, 4, 5).random()
                val rawVal = mantissa * 10.0.pow(exp.toDouble())
                val rawStr = if (exp > 0) {
                    rawVal.roundToLong().toString()
                } else {
                    String.format(Locale.US, "%.${abs(exp) + 1}f", rawVal).trimEnd('0').trimEnd('.')
                }
                val shiftDir = if (exp > 0) "left by $exp places" else "right by ${abs(exp)} places"
                val ansStr = "${formatCleanDouble(mantissa)}×10^$exp"

                MathProblem(
                    num1 = mantissa,
                    num2 = exp.toDouble(),
                    operation = "Sci Notation",
                    questionDisplay = "Normalize: $rawStr",
                    answer = ansStr,
                    numericAnswer = rawVal,
                    topicTag = "sci-normalize",
                    answerSpec = AnswerSpec.ScientificValue(
                        mantissa = mantissa,
                        exponent = exp,
                        mantissaRelativeTol = 0.01,
                        requireNormalized = true
                    ),
                    solutionSteps = listOf(
                        "Move decimal point $shiftDir so mantissa is in [1, 10): ${formatCleanDouble(mantissa)}",
                        "Result: $ansStr"
                    )
                )
            }

            SciNotationSubtype.DECIMAL_SHIFT -> {
                // Shift decimal point in a * 10^n left or right by k places and give the new exponent
                val mantissa = randomInRange(12, 95) / 10.0
                val exp = randomInRange(-8, 8)
                val shiftLeft = Random.nextBoolean()
                val k = randomInRange(1, 3)
                val unshiftedMantissa = if (shiftLeft) {
                    // Original had larger number, shifting left by k increases exponent by k
                    mantissa * 10.0.pow(k.toDouble())
                } else {
                    // Original had smaller number, shifting right by k decreases exponent by k
                    mantissa / 10.0.pow(k.toDouble())
                }
                val newExp = if (shiftLeft) exp + k else exp - k
                val origStr = formatCleanDouble(unshiftedMantissa)
                val targetMantissaStr = formatCleanDouble(mantissa)
                val dirWord = if (shiftLeft) "left by $k (exponent +$k)" else "right by $k (exponent −$k)"

                MathProblem(
                    num1 = exp.toDouble(),
                    num2 = k.toDouble(),
                    operation = "Sci Notation",
                    questionDisplay = "$origStr×10^$exp = $targetMantissaStr×10^?",
                    answer = newExp.toString(),
                    numericAnswer = newExp.toDouble(),
                    topicTag = "sci-shift",
                    answerSpec = AnswerSpec.Exact(newExp.toLong()),
                    solutionSteps = listOf(
                        "Decimal shifts $dirWord to go from $origStr to $targetMantissaStr",
                        "New exponent = $exp ${if (shiftLeft) "+ $k" else "− $k"} = $newExp"
                    )
                )
            }

            SciNotationSubtype.MULTIPLY_DIVIDE -> {
                val isMultiply = Random.nextBoolean()
                if (isMultiply) {
                    val a = randomInRange(2, 8)
                    val b = randomInRange(2, 8)
                    val e1 = randomInRange(-5, 5)
                    val e2 = randomInRange(-5, 5)
                    val prod = a * b
                    val (normM, extraExp) = if (prod >= 10) {
                        Pair(prod / 10.0, 1)
                    } else {
                        Pair(prod.toDouble(), 0)
                    }
                    val finalExp = e1 + e2 + extraExp
                    val finalVal = normM * 10.0.pow(finalExp.toDouble())
                    val ansStr = "${formatCleanDouble(normM)}×10^$finalExp"

                    MathProblem(
                        num1 = a.toDouble(),
                        num2 = b.toDouble(),
                        operation = "Sci Notation",
                        questionDisplay = "($a×10^$e1) × ($b×10^$e2)",
                        answer = ansStr,
                        numericAnswer = finalVal,
                        topicTag = "sci-mul-div",
                        answerSpec = AnswerSpec.ScientificValue(
                            mantissa = normM,
                            exponent = finalExp,
                            mantissaRelativeTol = 0.015
                        ),
                        solutionSteps = listOf(
                            "Multiply mantissas: $a × $b = $prod",
                            "Add exponents: $e1 + ($e2) = ${e1 + e2}",
                            if (extraExp > 0) "Normalize $prod×10^${e1 + e2} → $ansStr" else "Result: $ansStr"
                        )
                    )
                } else {
                    val b = listOf(2, 4, 5, 8).random()
                    val q = randomInRange(2, 9)
                    val a = b * q
                    // Place a in 1..9.9 by dividing by 10 if needed
                    val aMant = if (a >= 10) a / 10.0 else a.toDouble()
                    val rawQuot = aMant / b.toDouble()
                    val (normM, expAdjust) = if (rawQuot < 1.0) {
                        Pair(rawQuot * 10.0, -1)
                    } else {
                        Pair(rawQuot, 0)
                    }
                    val e1 = randomInRange(-4, 6)
                    val e2 = randomInRange(-4, 5)
                    val finalExp = e1 - e2 + expAdjust
                    val finalVal = normM * 10.0.pow(finalExp.toDouble())
                    val ansStr = "${formatCleanDouble(normM)}×10^$finalExp"

                    MathProblem(
                        num1 = aMant,
                        num2 = b.toDouble(),
                        operation = "Sci Notation",
                        questionDisplay = "(${formatCleanDouble(aMant)}×10^$e1) ÷ ($b×10^$e2)",
                        answer = ansStr,
                        numericAnswer = finalVal,
                        topicTag = "sci-mul-div",
                        answerSpec = AnswerSpec.ScientificValue(
                            mantissa = normM,
                            exponent = finalExp,
                            mantissaRelativeTol = 0.015
                        ),
                        solutionSteps = listOf(
                            "Divide mantissas: ${formatCleanDouble(aMant)} ÷ $b = ${formatCleanDouble(rawQuot)}",
                            "Subtract exponents: $e1 − ($e2) = ${e1 - e2}",
                            if (expAdjust != 0) "Normalize ${formatCleanDouble(rawQuot)}×10^${e1 - e2} → $ansStr" else "Result: $ansStr"
                        )
                    )
                }
            }

            SciNotationSubtype.ROOT_POWER_TEN -> {
                val isEvenExp = Random.nextBoolean()
                if (isEvenExp) {
                    // Even exponent: √(a × 10^(2k)) where a in {1, 2, 3, 4, 5, 6, 7, 8, 9}
                    val a = randomInRange(1, 9)
                    val halfExp = listOf(-5, -4, -3, -2, -1, 1, 2, 3, 4).random()
                    val evenExp = halfExp * 2
                    val rootEntry = CourseValuesData.lookup(MemoryTableCategory.SQUARE_ROOT, a)
                    val rootMant = rootEntry?.courseValue ?: sqrt(a.toDouble())
                    val trueRootMant = sqrt(a.toDouble())
                    val finalVal = rootMant * 10.0.pow(halfExp.toDouble())
                    val trueVal = trueRootMant * 10.0.pow(halfExp.toDouble())
                    val ansStr = "${formatCleanDouble(rootMant)}×10^$halfExp"
                    val prompt = if (a == 1) "√(10^$evenExp)" else "√($a×10^$evenExp)"

                    MathProblem(
                        num1 = a.toDouble(),
                        num2 = evenExp.toDouble(),
                        operation = "Sci Notation",
                        questionDisplay = prompt,
                        answer = ansStr,
                        numericAnswer = finalVal,
                        topicTag = "sci-root-pow10",
                        answerSpec = AnswerSpec.ScientificValue(
                            mantissa = rootMant,
                            exponent = halfExp,
                            mantissaRelativeTol = 0.05,
                            alsoAcceptDoubles = listOf(finalVal, trueVal)
                        ),
                        solutionSteps = listOf(
                            "Halve the even exponent: $evenExp ÷ 2 = $halfExp",
                            "√$a ≈ ${formatCleanDouble(rootMant)}",
                            "Result: $ansStr"
                        )
                    )
                } else {
                    // Odd exponent: rewrite √(a × 10^(2k+1)) = √(10a × 10^(2k))
                    // Pick a in {1, 2, 3, 4, 5, 6, 7, 8, 9} and use √(10a) = √10 × √a or perfect/memorized root
                    val a = listOf(1, 2, 3, 4, 5, 6, 9).random()
                    val halfExp = listOf(-4, -3, -2, -1, 1, 2, 3).random()
                    val evenExp = halfExp * 2
                    val oddExp = evenExp + 1
                    val tenA = 10 * a
                    // Course approximation: √(10 * a) = √10 (3.1) * √a or direct rounded value
                    val root10Course = CourseValuesData.lookup(MemoryTableCategory.SQUARE_ROOT, 10)!!.courseValue // 3.1
                    val rootACourse = if (a == 1) 1.0 else CourseValuesData.lookup(MemoryTableCategory.SQUARE_ROOT, a)!!.courseValue
                    val courseMant = ((root10Course * rootACourse) * 10.0).roundToInt() / 10.0
                    val trueMant = sqrt(tenA.toDouble())
                    val finalVal = courseMant * 10.0.pow(halfExp.toDouble())
                    val trueVal = trueMant * 10.0.pow(halfExp.toDouble())
                    val ansStr = "${formatCleanDouble(courseMant)}×10^$halfExp"

                    MathProblem(
                        num1 = a.toDouble(),
                        num2 = oddExp.toDouble(),
                        operation = "Sci Notation",
                        questionDisplay = "√($a×10^$oddExp)",
                        answer = ansStr,
                        numericAnswer = finalVal,
                        topicTag = "sci-root-pow10",
                        answerSpec = AnswerSpec.ScientificValue(
                            mantissa = courseMant,
                            exponent = halfExp,
                            mantissaRelativeTol = 0.06,
                            alsoAcceptDoubles = listOf(finalVal, trueVal)
                        ),
                        solutionSteps = listOf(
                            "Rewrite odd exponent $oddExp as even $evenExp: √($a×10^$oddExp) = √($tenA×10^$evenExp)",
                            "√$tenA ≈ ${formatCleanDouble(courseMant)} and √(10^$evenExp) = 10^$halfExp",
                            "Result: $ansStr"
                        )
                    )
                }
            }

            SciNotationSubtype.ALL -> generateScientificNotationQuestion(SciNotationSubtype.NORMALIZE)
        }
    }

    // =========================================================================
    // 3.3 pH and pOH Drills
    // =========================================================================
    fun generatePhQuestion(subtype: PhSubType = PhSubType.ALL): MathProblem {
        val chosen = if (subtype == PhSubType.ALL) {
            listOf(
                PhSubType.CONC_TO_PH,
                PhSubType.SCI_CONC_TO_PH,
                PhSubType.PH_TO_CONC,
                PhSubType.PH_POH_CONV,
                PhSubType.PERCENT_WV_TO_PH
            ).random()
        } else {
            subtype
        }

        return when (chosen) {
            PhSubType.CONC_TO_PH -> {
                val species = PhSpeciesData.allSpecies.random()
                val useMilliMolar = Random.nextBoolean()
                // Pick a concentration such that [H+] or [OH-] = m * 10^-exp with m in 1..5
                val m = if (species.valence == 2) {
                    // For diprotic, pick species conc cMant in {1, 2, 3, 4, 5} so 2*cMant in {2, 4, 6, 8, 10}
                    listOf(1, 2, 3, 4, 5).random()
                } else {
                    listOf(1, 2, 3, 4, 5).random()
                }
                val baseExp = if (useMilliMolar) randomInRange(0, 2) else randomInRange(1, 4)
                val molarExp = if (useMilliMolar) baseExp + 3 else baseExp
                // Species concentration = m * 10^-molarExp M
                val ionMantRaw = m * species.valence
                val (ionMant, ionExp) = if (ionMantRaw == 10) {
                    Pair(1, molarExp - 1)
                } else {
                    Pair(ionMantRaw, molarExp)
                }
                val logIonMant = if (ionMant == 1) 0.0 else CourseValuesData.lookup(MemoryTableCategory.LOG_10, ionMant)!!.courseValue
                val pIonCourse = ionExp - logIonMant
                val pIonTrue = ionExp - log10(ionMant.toDouble())
                val phCourse = if (species.isAcid) pIonCourse else 14.0 - pIonCourse
                val phTrue = if (species.isAcid) pIonTrue else 14.0 - pIonTrue

                val concDisplay = if (useMilliMolar) {
                    val mmVal = m * 10.0.pow((-baseExp).toDouble())
                    "${formatCleanDouble(mmVal)} mM"
                } else {
                    val mVal = m * 10.0.pow((-baseExp).toDouble())
                    "${formatCleanDouble(mVal)} M"
                }

                val ionSym = if (species.isAcid) "[H^+]" else "[OH^-]"
                val pIonLabel = if (species.isAcid) "pH" else "pOH"
                val steps = buildList {
                    if (useMilliMolar) {
                        add("Convert mM to M: $concDisplay = $m×10^−$molarExp M")
                    }
                    if (species.valence == 2) {
                        add("${species.formula} is diprotic (×2): $ionSym = 2 × $m×10^−$molarExp = $ionMant×10^−$ionExp M")
                    } else {
                        add("${species.formula} is monoprotic: $ionSym = $ionMant×10^−$ionExp M")
                    }
                    add("$pIonLabel = $ionExp − log₁₀($ionMant) = $ionExp − ${formatCleanDouble(logIonMant)} = ${formatCleanDouble(pIonCourse)}")
                    if (!species.isAcid) {
                        add("pH = 14 − pOH = 14 − ${formatCleanDouble(pIonCourse)} = ${formatCleanDouble(phCourse)}")
                    }
                }

                MathProblem(
                    num1 = phCourse,
                    num2 = 0.0,
                    operation = "pH & pOH",
                    questionDisplay = "pH of $concDisplay ${species.formula}",
                    answer = formatCleanDouble(phCourse),
                    numericAnswer = phCourse,
                    topicTag = "ph-conc-to-ph",
                    answerSpec = AnswerSpec.AbsoluteTolerance(
                        expected = phCourse,
                        tolerance = 0.1,
                        alsoAcceptExact = listOf(phCourse, phTrue)
                    ),
                    solutionSteps = steps
                )
            }

            PhSubType.SCI_CONC_TO_PH -> {
                // Scientific-notation [H+] -> pH by course rounding method:
                // Round mantissa to nearest integer m in 1..9, then pH = exp - log(m)
                val mRounded = randomInRange(2, 9)
                val offsetTenths = listOf(-2, -1, 0, 1, 2).random()
                val rawMantissa = (mRounded * 10 + offsetTenths).coerceIn(11, 94) / 10.0
                val roundedInt = rawMantissa.roundToInt().coerceIn(1, 9)
                val exp = randomInRange(2, 6)
                val logVal = if (roundedInt == 1) 0.0 else CourseValuesData.lookup(MemoryTableCategory.LOG_10, roundedInt)!!.courseValue
                val phCourse = exp - logVal
                val phTrue = exp - log10(rawMantissa)

                MathProblem(
                    num1 = rawMantissa,
                    num2 = exp.toDouble(),
                    operation = "pH & pOH",
                    questionDisplay = "pH when [H^+] = ${formatCleanDouble(rawMantissa)}×10^-$exp M",
                    answer = formatCleanDouble(phCourse),
                    numericAnswer = phCourse,
                    topicTag = "ph-sci-to-ph",
                    answerSpec = AnswerSpec.AbsoluteTolerance(
                        expected = phCourse,
                        tolerance = 0.1,
                        alsoAcceptExact = listOf(phCourse, phTrue)
                    ),
                    solutionSteps = listOf(
                        "Round mantissa ${formatCleanDouble(rawMantissa)} to nearest integer: $roundedInt",
                        "pH = $exp − log₁₀($roundedInt) = $exp − ${formatCleanDouble(logVal)} = ${formatCleanDouble(phCourse)}"
                    )
                )
            }

            PhSubType.PH_TO_CONC -> {
                // Reverse: integer or constructible decimal pH -> [H+]
                // Pick m in {1, 2, 3, 5} (log values: 0, 0.3, 0.5, 0.7) and exp in 2..6
                val m = listOf(1, 2, 3, 5).random()
                val exp = randomInRange(2, 6)
                val logVal = if (m == 1) 0.0 else CourseValuesData.lookup(MemoryTableCategory.LOG_10, m)!!.courseValue
                val ph = exp - logVal
                val hConc = m * 10.0.pow((-exp).toDouble())
                val ansStr = "$m×10^-$exp"

                MathProblem(
                    num1 = ph,
                    num2 = 0.0,
                    operation = "pH & pOH",
                    questionDisplay = "[H^+] (in M) at pH = ${formatCleanDouble(ph)}",
                    answer = ansStr,
                    numericAnswer = hConc,
                    topicTag = "ph-to-conc",
                    unitText = "M",
                    answerSpec = AnswerSpec.ScientificValue(
                        mantissa = m.toDouble(),
                        exponent = -exp,
                        mantissaRelativeTol = 0.05
                    ),
                    solutionSteps = listOf(
                        if (m == 1) {
                            "Integer pH = $exp → [H^+] = 10^−$exp M"
                        } else {
                            "Rewrite pH = ${formatCleanDouble(ph)} = $exp − ${formatCleanDouble(logVal)} = $exp − log₁₀($m)"
                        },
                        "[H^+] = $m×10^−$exp M"
                    )
                )
            }

            PhSubType.PH_POH_CONV -> {
                val givenPh = Random.nextBoolean()
                val valTenths = randomInRange(15, 125)
                val givenVal = valTenths / 10.0
                val targetVal = (140 - valTenths) / 10.0
                val prompt = if (givenPh) {
                    "If pH = ${formatCleanDouble(givenVal)}, pOH = ?"
                } else {
                    "If pOH = ${formatCleanDouble(givenVal)}, pH = ?"
                }

                MathProblem(
                    num1 = givenVal,
                    num2 = 14.0,
                    operation = "pH & pOH",
                    questionDisplay = prompt,
                    answer = formatCleanDouble(targetVal),
                    numericAnswer = targetVal,
                    topicTag = "ph-poh-conv",
                    answerSpec = AnswerSpec.AbsoluteTolerance(
                        expected = targetVal,
                        tolerance = 0.1
                    ),
                    solutionSteps = listOf(
                        "Use pH + pOH = 14 (at 25 °C)",
                        "${if (givenPh) "pOH" else "pH"} = 14 − ${formatCleanDouble(givenVal)} = ${formatCleanDouble(targetVal)}"
                    )
                )
            }

            PhSubType.PERCENT_WV_TO_PH -> {
                // % w/v of a strong base -> molarity via c = (10 * %) / M -> pOH -> pH
                val base = PhSpeciesData.bases.random()
                // Choose target molarity c = m * 10^-exp so % w/v = c * M / 10 is clean
                val exp = listOf(1, 2).random() // 0.1 M or 0.01 M (or 0.2 M)
                val m = if (base.valence == 2) 1 else listOf(1, 2).random()
                val cMolar = m * 10.0.pow((-exp).toDouble())
                val percentWv = (cMolar * base.molarMass) / 10.0
                val ohMant = m * base.valence
                val logOh = if (ohMant == 1) 0.0 else CourseValuesData.lookup(MemoryTableCategory.LOG_10, ohMant)!!.courseValue
                val pohCourse = exp - logOh
                val phCourse = 14.0 - pohCourse
                val phTrue = 14.0 - (exp - log10(ohMant.toDouble()))

                val pctStr = formatCleanDouble(percentWv)
                MathProblem(
                    num1 = percentWv,
                    num2 = base.molarMass,
                    operation = "pH & pOH",
                    questionDisplay = "pH of $pctStr% w/v ${base.formula} (M=${formatCleanDouble(base.molarMass)})",
                    answer = formatCleanDouble(phCourse),
                    numericAnswer = phCourse,
                    topicTag = "ph-percent-wv",
                    answerSpec = AnswerSpec.AbsoluteTolerance(
                        expected = phCourse,
                        tolerance = 0.1,
                        alsoAcceptExact = listOf(phCourse, phTrue)
                    ),
                    solutionSteps = listOf(
                        "Molarity c = (10 × %w/v) ÷ M = (10 × $pctStr) ÷ ${formatCleanDouble(base.molarMass)} = ${formatCleanDouble(cMolar)} M",
                        "[OH^-] = ${if (base.valence == 2) "2 × c = " else ""}$ohMant×10^−$exp M",
                        "pOH = $exp − log₁₀($ohMant) = ${formatCleanDouble(pohCourse)}",
                        "pH = 14 − pOH = 14 − ${formatCleanDouble(pohCourse)} = ${formatCleanDouble(phCourse)}"
                    )
                )
            }

            PhSubType.ALL -> generatePhQuestion(PhSubType.CONC_TO_PH)
        }
    }

    // =========================================================================
    // 3.4 Composite Logs and Roots
    // =========================================================================
    fun generateCompositeLogRootQuestion(
        subtype: CompositeLogRootSubtype = CompositeLogRootSubtype.ALL
    ): MathProblem {
        val chosen = if (subtype == CompositeLogRootSubtype.ALL) {
            listOf(
                CompositeLogRootSubtype.LOG_SCI,
                CompositeLogRootSubtype.LOG_PRODUCT,
                CompositeLogRootSubtype.ROOT_SCI
            ).random()
        } else {
            subtype
        }

        return when (chosen) {
            CompositeLogRootSubtype.LOG_SCI -> {
                // log10(k × 10^n) = log10(k) + n
                val k = randomInRange(2, 9)
                val n = listOf(-6, -5, -4, -3, -2, 2, 3, 4, 5).random()
                val logKCourse = CourseValuesData.lookup(MemoryTableCategory.LOG_10, k)!!.courseValue
                val ansCourse = ((logKCourse + n) * 100.0).roundToInt() / 100.0
                val ansTrue = log10(k.toDouble()) + n

                MathProblem(
                    num1 = k.toDouble(),
                    num2 = n.toDouble(),
                    operation = "Logs & Roots",
                    questionDisplay = "log₁₀($k×10^$n)",
                    answer = formatCleanDouble(ansCourse),
                    numericAnswer = ansCourse,
                    topicTag = "composite-log-sci",
                    answerSpec = AnswerSpec.AbsoluteTolerance(
                        expected = ansCourse,
                        tolerance = 0.06,
                        alsoAcceptExact = listOf(ansCourse, ansTrue)
                    ),
                    solutionSteps = listOf(
                        "log₁₀($k×10^$n) = log₁₀($k) + $n",
                        "From table, log₁₀($k) ≈ ${formatCleanDouble(logKCourse)}",
                        "${formatCleanDouble(logKCourse)} + ($n) = ${formatCleanDouble(ansCourse)}"
                    )
                )
            }

            CompositeLogRootSubtype.LOG_PRODUCT -> {
                // log10(a * b) = log10(a) + log10(b) for composite numbers factorable into 2..9
                val pairs = listOf(
                    Pair(2, 7), // 14
                    Pair(3, 5), // 15
                    Pair(3, 7), // 21
                    Pair(4, 7), // 28
                    Pair(5, 7), // 35
                    Pair(6, 7), // 42
                    Pair(3, 8), // 24
                    Pair(4, 9), // 36
                    Pair(5, 9), // 45
                    Pair(6, 9)  // 54
                )
                val (a, b) = pairs.random()
                val prod = a * b
                val logA = CourseValuesData.lookup(MemoryTableCategory.LOG_10, a)!!.courseValue
                val logB = CourseValuesData.lookup(MemoryTableCategory.LOG_10, b)!!.courseValue
                val ansCourse = ((logA + logB) * 100.0).roundToInt() / 100.0
                val ansTrue = log10(prod.toDouble())

                MathProblem(
                    num1 = a.toDouble(),
                    num2 = b.toDouble(),
                    operation = "Logs & Roots",
                    questionDisplay = "log₁₀($prod)",
                    answer = formatCleanDouble(ansCourse),
                    numericAnswer = ansCourse,
                    topicTag = "composite-log-prod",
                    answerSpec = AnswerSpec.AbsoluteTolerance(
                        expected = ansCourse,
                        tolerance = 0.06,
                        alsoAcceptExact = listOf(ansCourse, ansTrue)
                    ),
                    solutionSteps = listOf(
                        "Factor $prod = $a × $b",
                        "log₁₀($prod) = log₁₀($a) + log₁₀($b) ≈ ${formatCleanDouble(logA)} + ${formatCleanDouble(logB)} = ${formatCleanDouble(ansCourse)}"
                    )
                )
            }

            CompositeLogRootSubtype.ROOT_SCI -> {
                // √(a × 10^n) with even n
                val a = randomInRange(2, 9)
                val halfN = listOf(-4, -3, -2, -1, 1, 2, 3).random()
                val n = halfN * 2
                val rootACourse = CourseValuesData.lookup(MemoryTableCategory.SQUARE_ROOT, a)!!.courseValue
                val rootATrue = sqrt(a.toDouble())
                val ansVal = rootACourse * 10.0.pow(halfN.toDouble())
                val trueVal = rootATrue * 10.0.pow(halfN.toDouble())
                val ansStr = "${formatCleanDouble(rootACourse)}×10^$halfN"

                MathProblem(
                    num1 = a.toDouble(),
                    num2 = n.toDouble(),
                    operation = "Logs & Roots",
                    questionDisplay = "√($a×10^$n)",
                    answer = ansStr,
                    numericAnswer = ansVal,
                    topicTag = "composite-root-sci",
                    answerSpec = AnswerSpec.ScientificValue(
                        mantissa = rootACourse,
                        exponent = halfN,
                        mantissaRelativeTol = 0.05,
                        alsoAcceptDoubles = listOf(ansVal, trueVal)
                    ),
                    solutionSteps = listOf(
                        "√($a×10^$n) = √$a × 10^($n÷2)",
                        "√$a ≈ ${formatCleanDouble(rootACourse)}, exponent = $halfN",
                        "Result: $ansStr"
                    )
                )
            }

            CompositeLogRootSubtype.ALL -> generateCompositeLogRootQuestion(CompositeLogRootSubtype.LOG_SCI)
        }
    }

    // =========================================================================
    // 3.5 Cubes and Powers
    // =========================================================================
    fun generateCubesPowersQuestion(
        subtype: CubesPowersSubtype = CubesPowersSubtype.ALL,
        cubeRange: Pair<Int, Int> = Pair(1, 15)
    ): MathProblem {
        val chosen = if (subtype == CubesPowersSubtype.ALL) {
            listOf(
                CubesPowersSubtype.CUBES,
                CubesPowersSubtype.POWERS,
                CubesPowersSubtype.PERFECT_CUBE_ROOTS
            ).random()
        } else {
            subtype
        }

        val minCube = cubeRange.first.coerceAtLeast(1)
        val maxCube = cubeRange.second.coerceAtLeast(minCube)

        return when (chosen) {
            CubesPowersSubtype.CUBES -> {
                val n = randomInRange(minCube, maxCube)
                val ans = n.toLong() * n.toLong() * n.toLong()
                MathProblem(
                    num1 = n.toDouble(),
                    num2 = 3.0,
                    operation = "Cubes & Powers",
                    questionDisplay = "$n^3",
                    answer = ans.toString(),
                    numericAnswer = ans.toDouble(),
                    topicTag = "cubes",
                    answerSpec = AnswerSpec.exact(ans),
                    solutionSteps = listOf(
                        "$n^2 = ${n * n}",
                        "$n^3 = ${n * n} × $n = $ans"
                    )
                )
            }

            CubesPowersSubtype.POWERS -> {
                // Small powers: 2^1..2^10, 3^1..3^6, 5^1..5^4
                val base = listOf(2, 3, 5).random()
                val exp = when (base) {
                    2 -> randomInRange(1, 10)
                    3 -> randomInRange(1, 6)
                    else -> randomInRange(1, 4)
                }
                val ans = base.toDouble().pow(exp.toDouble()).roundToLong()
                MathProblem(
                    num1 = base.toDouble(),
                    num2 = exp.toDouble(),
                    operation = "Cubes & Powers",
                    questionDisplay = "$base^$exp",
                    answer = ans.toString(),
                    numericAnswer = ans.toDouble(),
                    topicTag = "powers",
                    answerSpec = AnswerSpec.exact(ans),
                    solutionSteps = listOf(
                        "Multiply $base by itself $exp times: $base^$exp = $ans"
                    )
                )
            }

            CubesPowersSubtype.PERFECT_CUBE_ROOTS -> {
                val n = randomInRange(minCube, maxCube)
                val cube = n.toLong() * n.toLong() * n.toLong()
                MathProblem(
                    num1 = cube.toDouble(),
                    num2 = 3.0,
                    operation = "Cubes & Powers",
                    questionDisplay = "∛$cube",
                    answer = n.toString(),
                    numericAnswer = n.toDouble(),
                    topicTag = "cube-roots-exact",
                    answerSpec = AnswerSpec.exact(n.toLong()),
                    solutionSteps = listOf(
                        "Since $n^3 = $cube, ∛$cube = $n"
                    )
                )
            }

            CubesPowersSubtype.ALL -> generateCubesPowersQuestion(CubesPowersSubtype.CUBES, cubeRange)
        }
    }

    // =========================================================================
    // 3.6 Approximation MCQ Helper
    // =========================================================================
    /**
     * Converts any [MathProblem] into a 4-option multiple-choice question (2×2 grid).
     * Options are guaranteed to be spaced at least `3 × tolerance` apart, with realistic distractors
     * (power-of-10 off, wrong sign, neighboring value).
     */
    fun attachMcqOptions(
        problem: MathProblem,
        preset: TolerancePreset = TolerancePreset.NORMAL
    ): MathProblem {
        val correctStr = problem.answer
        val correctVal = problem.numericAnswer
        val minSpacing = AnswerChecker.minimumMcqSpacing(problem.answerSpec, preset)

        val isSciDisplay = correctStr.contains("×10^")
        val candidates = mutableListOf<Pair<Double, String>>()

        if (isSciDisplay && problem.answerSpec is AnswerSpec.ScientificValue) {
            val spec = problem.answerSpec
            val m = spec.mantissa
            val e = spec.exponent
            // Realistic distractors: power-of-10 off, neighboring mantissas, wrong exponent sign
            val rawPairs = listOf(
                Pair(m, e + 1),
                Pair(m, e - 1),
                Pair((m + 1.2).coerceAtMost(9.8), e),
                Pair((m - 1.1).coerceAtLeast(1.1), e),
                Pair(m, if (e != 0) -e else e + 2),
                Pair((m + 2.5).coerceAtMost(9.9), e),
                Pair(m, e + 2)
            )
            for ((dm, de) in rawPairs) {
                val dVal = dm * 10.0.pow(de.toDouble())
                val dStr = "${formatCleanDouble(dm)}×10^$de"
                candidates.add(Pair(dVal, dStr))
            }
        } else {
            val step = maxOf(minSpacing * 1.35, if (abs(correctVal) >= 10) 2.0 else 0.25)
            val rawOffsets = listOf(
                correctVal + step,
                correctVal - step,
                correctVal + 2.1 * step,
                correctVal - 2.1 * step,
                if (abs(correctVal) > 0.1) correctVal * 10.0 else correctVal + 3.2 * step,
                if (abs(correctVal) > minSpacing * 2) -correctVal else correctVal + 4.3 * step,
                correctVal + 3.5 * step
            )
            for (dVal in rawOffsets) {
                val rounded = if (correctStr.contains(".")) {
                    val decimals = correctStr.substringAfter(".").length.coerceIn(1, 4)
                    val factor = 10.0.pow(decimals.toDouble())
                    (dVal * factor).roundToInt() / factor
                } else {
                    dVal.roundToLong().toDouble()
                }
                val dStr = formatCleanDouble(rounded)
                val parsedVal = ScientificInputParser.parseToDouble(dStr) ?: rounded
                candidates.add(Pair(parsedVal, dStr))
            }
        }

        for ( dVal in candidates ) {
            // Note: also ensure correctVal in chosen uses the parsed value of correctStr so options[i] comparison matches
        }
        val parsedCorrectVal = ScientificInputParser.parseToDouble(correctStr) ?: correctVal
        val chosen = mutableListOf(Pair(parsedCorrectVal, correctStr))
        for (cand in candidates) {
            if (chosen.size >= 4) break
            val candParsedVal = ScientificInputParser.parseToDouble(cand.second) ?: cand.first
            if (cand.second != correctStr &&
                chosen.none { it.second == cand.second } &&
                chosen.all { abs(it.first - candParsedVal) >= minSpacing - 1e-9 }
            ) {
                chosen.add(Pair(candParsedVal, cand.second))
            }
        }

        // Fallback if still < 4 options: step outward by 1.5 * minSpacing
        var k = 1
        while (chosen.size < 4) {
            val dVal = parsedCorrectVal + k * maxOf(minSpacing * 1.5, 1.0)
            val dStr = if (isSciDisplay && problem.answerSpec is AnswerSpec.ScientificValue) {
                val e = problem.answerSpec.exponent + k
                val m = problem.answerSpec.mantissa
                "${formatCleanDouble(m)}×10^$e"
            } else {
                formatCleanDouble((dVal * 100.0).roundToInt() / 100.0)
            }
            val actualVal = ScientificInputParser.parseToDouble(dStr) ?: dVal
            if (chosen.none { it.second == dStr } && chosen.all { abs(it.first - actualVal) >= minSpacing - 1e-9 }) {
                chosen.add(Pair(actualVal, dStr))
            }
            k++
        }

        val shuffledOptions = chosen.map { it.second }.shuffled()
        val correctIndex = shuffledOptions.indexOf(correctStr).coerceAtLeast(0)

        return problem.copy(
            mcqOptions = shuffledOptions,
            answerSpec = AnswerSpec.Choice(
                options = shuffledOptions,
                correctIndex = correctIndex,
                correctValue = correctStr
            )
        )
    }

    // =========================================================================
    // 5.4 Constants Drill Generator
    // =========================================================================
    fun generateConstantQuestion(
        papers: Set<com.example.data.HscPaper> = emptySet(),
        chapters: Set<String> = emptySet(),
        reliabilityClasses: Set<com.example.data.ReliabilityClass> = emptySet(),
        includeAlternateUnits: Boolean = false,
        missedCounts: Map<String, Int> = emptyMap()
    ): MathProblem {
        val dataset = ConstantsLoader.getDataset(failLoudly = false)
        val allConstants = dataset.constants
        val filtered = allConstants.filter { c ->
            val matchesPaper = papers.isEmpty() || c.papers.any { it in papers }
            val matchesChapter = chapters.isEmpty() || c.chapters.any { it.key in chapters }
            val matchesRel = reliabilityClasses.isEmpty() || c.reliabilityClass in reliabilityClasses
            matchesPaper && matchesChapter && matchesRel
        }.ifEmpty { allConstants }

        val entry = selectWeightedConstant(filtered, missedCounts)

        // Alternate units logic (Section 4):
        // If includeAlternateUnits toggle is on AND the entry has alternate values,
        // randomly choose between primary and an alternate variant with 30% chance.
        val useAlternate = includeAlternateUnits && entry.allAlternateValues.isNotEmpty() && Random.nextInt(100) < 30
        val alt = if (useAlternate) entry.allAlternateValues.random() else null

        val (targetMantissa, targetExponent, targetUnit, targetVal, targetCourseVal) = if (alt != null) {
            Tuple5(alt.mantissa, alt.exponent, alt.unit, alt.preciseValue, alt.courseValue)
        } else {
            Tuple5(entry.mantissa, entry.exponent, entry.unit, entry.preciseValue, entry.courseValue)
        }

        val ansStr = if (targetExponent == 0) {
            formatCleanDouble(targetMantissa)
        } else {
            "${formatCleanDouble(targetMantissa)}×10^$targetExponent"
        }
        val unitPlain = targetUnit.toPlainString()
        val relTol = entry.toleranceOverride ?: 0.01

        val acceptedValues = mutableListOf(targetVal, targetCourseVal)
        val shortSym = entry.symbol.shortSymbol
        val formattedUnitText = RichTextHelper.formatUnit(targetUnit).text
        val unitDisplaySuffix = if (targetUnit.isDimensionless || formattedUnitText == "no unit") "" else " $formattedUnitText"

        val steps = buildList {
            add("${entry.name} ($shortSym): $ansStr$unitDisplaySuffix")
            add("Precise value: ${formatCleanDouble(targetVal)}$unitDisplaySuffix")
            add("Course value: ${formatCleanDouble(targetCourseVal)}$unitDisplaySuffix")
            if (entry.chapters.isNotEmpty()) {
                val chapList = entry.chapters.joinToString(", ") { "${it.paper.displayName}, ${it.nameEn}" }
                add("Chapters: $chapList")
            }
            entry.notes?.let { add("Note: $it") }
        }

        return MathProblem(
            num1 = targetMantissa,
            num2 = targetExponent.toDouble(),
            operation = "Constants",
            questionDisplay = entry.name,
            answer = ansStr,
            numericAnswer = targetVal,
            topicTag = entry.id,
            answerSpec = AnswerSpec.ScientificValue(
                mantissa = targetMantissa,
                exponent = targetExponent,
                mantissaRelativeTol = relTol,
                requireNormalized = false,
                alsoAcceptDoubles = acceptedValues
            ),
            unitText = if (alt != null) formattedUnitText else null,
            subjectLabel = entry.subjectLabel,
            solutionSteps = steps,
            confusableIds = entry.confusableIds,
            constantSymbol = entry.symbol,
            structuredUnit = targetUnit,
            constantEntry = entry
        )
    }

    private data class Tuple5<A, B, C, D, E>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
        val fifth: E
    )

    private fun selectWeightedConstant(
        entries: List<ConstantEntry>,
        missedCounts: Map<String, Int>
    ): ConstantEntry {
        if (entries.size <= 1) return entries.first()
        val weights = entries.map { e -> 1 + (missedCounts[e.id] ?: 0) * 2 }
        val totalWeight = weights.sum()
        var roll = Random.nextInt(totalWeight)
        for (i in entries.indices) {
            roll -= weights[i]
            if (roll < 0) return entries[i]
        }
        return entries.last()
    }

    // =========================================================================
    // 5.5 Extra Drills (Atomic Masses, Unit Conversions, Math Constants, Ref Values)
    // =========================================================================

    /**
     * Atomic Masses Drill (39 elements):
     * Tests symbol -> school atomic mass or element name -> school atomic mass (tolerance 0.05).
     */
    fun generateAtomicMassQuestion(): MathProblem {
        val dataset = ConstantsLoader.getDataset(failLoudly = false)
        val entry = dataset.atomicMasses.random()
        val askSymbol = Random.nextBoolean()
        val prompt = if (askSymbol) {
            "Atomic mass of ${entry.symbol}"
        } else {
            "Atomic mass of ${entry.element}"
        }
        val mass = entry.relativeAtomicMassSchool
        val ansStr = formatCleanDouble(mass)

        return MathProblem(
            num1 = mass,
            num2 = 0.0,
            operation = "Atomic Masses",
            questionDisplay = prompt,
            answer = ansStr,
            numericAnswer = mass,
            topicTag = "atomic-mass-${entry.symbol.lowercase()}",
            answerSpec = AnswerSpec.AbsoluteTolerance(
                expected = mass,
                tolerance = 0.05,
                exactCourseValues = listOf(mass)
            ),
            unitText = "g/mol",
            subjectLabel = "Chemistry",
            solutionSteps = listOf(
                "${entry.element} (${entry.symbol}): School mass = $ansStr g/mol (u)",
                "Modern CIAAW value: ${entry.modernCiaawValue}",
                entry.notes ?: "Standard HSC Chemistry atomic mass value."
            )
        )
    }

    /**
     * Unit Conversions Drill (13 numeric groups, temperature is revision-only):
     * Forward or reverse conversion with 1% relative tolerance.
     */
    fun generateUnitConversionQuestion(): MathProblem {
        val dataset = ConstantsLoader.getDataset(failLoudly = false)
        val numericGroups = dataset.unitConversions.filter { !it.isFormulaOnly }
        val group = numericGroups.random()
        val numericItems = group.values.filter { it.isNumericFactor }
        val item = numericItems.random()

        val isForward = Random.nextBoolean()
        val fromUnit = if (isForward) item.fromUnit!! else item.toUnit!!
        val toUnit = if (isForward) item.toUnit!! else item.fromUnit!!
        val factor = if (isForward) item.factor!! else (1.0 / item.factor!!)

        val (m, e) = ConstantsLoader.decomposeToMantissaAndExponent(factor)
        val ansStr = if (e == 0) formatCleanDouble(m) else "${formatCleanDouble(m)}×10^$e"
        val fromStr = fromUnit.toPlainString()
        val toStr = toUnit.toPlainString()

        return MathProblem(
            num1 = m,
            num2 = e.toDouble(),
            operation = "Conversions",
            questionDisplay = "1 $fromStr = ? $toStr",
            answer = ansStr,
            numericAnswer = factor,
            topicTag = "conversion-${group.id}",
            answerSpec = AnswerSpec.ScientificValue(
                mantissa = m,
                exponent = e,
                mantissaRelativeTol = 0.01,
                requireNormalized = false,
                alsoAcceptDoubles = listOf(factor)
            ),
            unitText = toStr,
            subjectLabel = group.quantity,
            solutionSteps = listOf(
                "1 $fromStr = $ansStr $toStr",
                "Summary: ${group.conversionSummary}"
            ),
            structuredUnit = toUnit
        )
    }

    /**
     * Mathematical Constants Drill (4 constants: π, e, ln 2, ln 10):
     * Name -> value, accepting any listed approximation including fractions like 22/7.
     */
    fun generateMathConstantQuestion(): MathProblem {
        val dataset = ConstantsLoader.getDataset(failLoudly = false)
        val mc = dataset.mathConstants.random()
        val ansStr = formatCleanDouble(mc.value)
        val shortSym = mc.symbol.shortSymbol

        val acceptedList = mutableListOf(mc.value)
        acceptedList.addAll(mc.acceptedNumericApproximations)

        return MathProblem(
            num1 = mc.value,
            num2 = 0.0,
            operation = "Math Constants",
            questionDisplay = "${mc.name} ($shortSym)",
            answer = ansStr,
            numericAnswer = mc.value,
            topicTag = "math-constant-${mc.id}",
            answerSpec = AnswerSpec.RelativeTolerance(
                expected = mc.value,
                relativeToleranceOverride = 0.01,
                exactCourseValues = acceptedList
            ),
            subjectLabel = "Mathematics",
            solutionSteps = listOf(
                "${mc.name} ($shortSym) = ${formatCleanDouble(mc.value)}",
                "Approximations accepted: ${mc.commonApproximations.joinToString(", ")}",
                "Curriculum uses: ${mc.relevance.joinToString("; ")}"
            ),
            constantSymbol = mc.symbol
        )
    }

    /**
     * Condition-Dependent Reference Values Drill (12 entries):
     * Runs through the same engine as constants, clearly labelled as Reference Values.
     */
    fun generateReferenceValueQuestion(
        missedCounts: Map<String, Int> = emptyMap()
    ): MathProblem {
        val dataset = ConstantsLoader.getDataset(failLoudly = false)
        val refList = dataset.referenceValues
        val entry = selectWeightedConstant(refList, missedCounts)

        val ansStr = if (entry.exponent == 0) {
            formatCleanDouble(entry.mantissa)
        } else {
            "${formatCleanDouble(entry.mantissa)}×10^${entry.exponent}"
        }
        val unitPlain = entry.unit.toPlainString()
        val shortSym = entry.symbol.shortSymbol
        val relTol = entry.toleranceOverride ?: 0.015

        val accepted = listOf(entry.preciseValue, entry.courseValue).distinct()

        return MathProblem(
            num1 = entry.mantissa,
            num2 = entry.exponent.toDouble(),
            operation = "Ref Values",
            questionDisplay = "${entry.name} ($shortSym)",
            answer = ansStr,
            numericAnswer = entry.preciseValue,
            topicTag = "reference-${entry.id}",
            answerSpec = AnswerSpec.ScientificValue(
                mantissa = entry.mantissa,
                exponent = entry.exponent,
                mantissaRelativeTol = relTol,
                requireNormalized = false,
                alsoAcceptDoubles = accepted
            ),
            unitText = unitPlain,
            subjectLabel = entry.subjectLabel,
            solutionSteps = listOf(
                "${entry.name} ($shortSym): Reference value = $ansStr $unitPlain",
                entry.definitionOrMeaning?.let { "Conditions: $it" } ?: "",
                entry.notes?.let { "Notes: $it" } ?: ""
            ).filter { it.isNotBlank() },
            confusableIds = entry.confusableIds,
            constantSymbol = entry.symbol,
            structuredUnit = entry.unit,
            constantEntry = entry
        )
    }

    internal fun formatCleanDouble(v: Double): String {
        val roundedLong = v.roundToLong()
        return if (abs(v - roundedLong.toDouble()) < 1e-9) {
            roundedLong.toString()
        } else {
            String.format(Locale.US, "%.4f", v).trimEnd('0').trimEnd('.')
        }
    }
}
