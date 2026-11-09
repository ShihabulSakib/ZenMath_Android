package com.example.domain

import com.example.data.QuestionRecordEntity
import com.example.data.SimpleJsonArray
import com.example.data.SimpleJsonObject
import kotlin.math.abs

/**
 * Pure-Kotlin JSON serializer/deserializer for [MathProblem] and all [AnswerSpec] variants.
 * Enables exact replay of past mistakes across sessions and app restarts.
 */
object ProblemCodec {

    fun encode(problem: MathProblem): String {
        val root = SimpleJsonObject()
        root.put("v", 1)
        root.put("num1", problem.num1)
        root.put("num2", problem.num2)
        root.put("operation", problem.operation)
        root.put("questionDisplay", problem.questionDisplay)
        root.put("answer", problem.answer)
        root.put("numericAnswer", problem.numericAnswer)
        root.put("topicTag", problem.topicTag)
        root.put("unitText", problem.unitText)
        root.put("subjectLabel", problem.subjectLabel)

        val stepsArr = SimpleJsonArray()
        for (step in problem.solutionSteps) {
            stepsArr.put(step)
        }
        root.put("solutionSteps", stepsArr)

        if (problem.mcqOptions != null) {
            val mcqArr = SimpleJsonArray()
            for (opt in problem.mcqOptions) {
                mcqArr.put(opt)
            }
            root.put("mcqOptions", mcqArr)
        }

        val confArr = SimpleJsonArray()
        for (cid in problem.confusableIds) {
            confArr.put(cid)
        }
        root.put("confusableIds", confArr)

        root.put("answerSpec", encodeAnswerSpec(problem.answerSpec))
        return root.toJsonString()
    }

    fun decode(jsonStr: String): MathProblem? {
        if (jsonStr.isBlank()) return null
        return try {
            val root = SimpleJsonObject.parse(jsonStr)
            val num1 = root.optDouble("num1", 0.0)
            val num2 = root.optDouble("num2", 0.0)
            val operation = root.optString("operation", "")
            val questionDisplay = root.optString("questionDisplay", "")
            val answer = root.optString("answer", "")
            if (questionDisplay.isBlank() || answer.isBlank()) return null
            val numericAnswer = root.optDouble("numericAnswer", 0.0)
            val topicTag = root.optString("topicTag", operation)
            val unitText = if (root.isNull("unitText")) null else root.optString("unitText").ifBlank { null }
            val subjectLabel = if (root.isNull("subjectLabel")) null else root.optString("subjectLabel").ifBlank { null }

            val stepsArr = root.optJSONArray("solutionSteps")
            val steps = if (stepsArr != null) {
                (0 until stepsArr.length()).map { stepsArr.optString(it) }
            } else {
                emptyList()
            }

            val mcqArr = root.optJSONArray("mcqOptions")
            val mcqOptions = if (mcqArr != null && mcqArr.length() > 0) {
                (0 until mcqArr.length()).map { mcqArr.optString(it) }
            } else {
                null
            }

            val confArr = root.optJSONArray("confusableIds")
            val confusableIds = if (confArr != null) {
                (0 until confArr.length()).map { confArr.optString(it) }
            } else {
                emptyList()
            }

            val specObj = root.optJSONObject("answerSpec")
            val answerSpec = if (specObj != null) {
                decodeAnswerSpec(specObj, answer, numericAnswer)
            } else {
                AnswerSpec.exact(numericAnswer.toLong())
            }

            MathProblem(
                num1 = num1,
                num2 = num2,
                operation = operation,
                questionDisplay = questionDisplay,
                answer = answer,
                numericAnswer = numericAnswer,
                topicTag = topicTag,
                answerSpec = answerSpec,
                unitText = unitText,
                subjectLabel = subjectLabel,
                solutionSteps = steps,
                mcqOptions = mcqOptions,
                confusableIds = confusableIds
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reconstructs a replayable [MathProblem] from a [QuestionRecordEntity].
     * Uses `problemJson` when available (schema v3+), or synthesizes a best-effort problem
     * for pre-v3 historical records. Returns `null` if the record cannot be replayed.
     */
    fun fromRecordOrFallback(record: QuestionRecordEntity): MathProblem? {
        if (record.problemJson.isNotBlank()) {
            decode(record.problemJson)?.let { return it }
        }
        return legacyFallback(record)
    }

    fun legacyFallback(record: QuestionRecordEntity): MathProblem? {
        val ans = record.correctAnswer.trim()
        if (ans.isEmpty()) return null

        val qText = record.questionText.trim().ifBlank {
            if (record.operation in listOf("+", "−", "-", "×", "*", "÷", "/")) {
                "${record.num1.toLong()} ${record.operation} ${record.num2.toLong()}"
            } else {
                record.operation.trim()
            }
        }
        if (qText.isEmpty()) return null

        val tag = record.topicTag.ifBlank { record.mode.ifBlank { "general" } }

        // 1. Fraction "n/d"
        if (ans.contains("/")) {
            val parts = ans.split("/")
            if (parts.size == 2) {
                val n = parts[0].trim().toIntOrNull()
                val d = parts[1].trim().toIntOrNull()
                if (n != null && d != null && d != 0) {
                    val numeric = n.toDouble() / d.toDouble()
                    return MathProblem(
                        num1 = record.num1,
                        num2 = record.num2,
                        operation = record.operation.ifBlank { "Fractions" },
                        questionDisplay = qText,
                        answer = ans,
                        numericAnswer = numeric,
                        topicTag = tag,
                        answerSpec = AnswerSpec.FractionSpec(
                            numerator = n,
                            denominator = d,
                            isFractionToDecimal = false,
                            acceptCourseValues = true
                        ),
                        solutionSteps = listOf("Correct answer: $ans")
                    )
                }
            }
        }

        // 2. Scientific notation "m×10^e"
        if (ans.contains("×10^")) {
            val parsed = ScientificInputParser.parse(ans)
            if (parsed != null) {
                val exp = parsed.exponent ?: 0
                return MathProblem(
                    num1 = record.num1,
                    num2 = record.num2,
                    operation = record.operation.ifBlank { "Sci. Notation" },
                    questionDisplay = qText,
                    answer = ans,
                    numericAnswer = parsed.value,
                    topicTag = tag,
                    answerSpec = AnswerSpec.ScientificValue(
                        mantissa = parsed.mantissa,
                        exponent = exp,
                        relativeToleranceOverride = 0.015,
                        alsoAcceptDoubles = listOf(parsed.value)
                    ),
                    solutionSteps = listOf("Correct answer: $ans")
                )
            }
        }

        // 3. Exact integer
        val asLong = ans.toLongOrNull()
        if (asLong != null && !ans.contains(".")) {
            return MathProblem(
                num1 = record.num1,
                num2 = record.num2,
                operation = record.operation.ifBlank { "Practice" },
                questionDisplay = qText,
                answer = ans,
                numericAnswer = asLong.toDouble(),
                topicTag = tag,
                answerSpec = AnswerSpec.Exact(expected = asLong),
                solutionSteps = listOf("Correct answer: $ans")
            )
        }

        // 4. Plain decimal
        val asDouble = ans.toDoubleOrNull() ?: ScientificInputParser.parseToDouble(ans)
        if (asDouble != null) {
            val tol = maxOf(0.05, abs(asDouble) * 0.015)
            return MathProblem(
                num1 = record.num1,
                num2 = record.num2,
                operation = record.operation.ifBlank { "Practice" },
                questionDisplay = qText,
                answer = ans,
                numericAnswer = asDouble,
                topicTag = tag,
                answerSpec = AnswerSpec.AbsoluteTolerance(
                    expected = asDouble,
                    tolerance = tol,
                    alsoAcceptExact = listOf(asDouble)
                ),
                solutionSteps = listOf("Correct answer: $ans")
            )
        }

        return null
    }

    private fun encodeAnswerSpec(spec: AnswerSpec): SimpleJsonObject {
        val obj = SimpleJsonObject()
        when (spec) {
            is AnswerSpec.Exact -> {
                obj.put("type", "exact")
                obj.put("expected", spec.expected)
                val alts = SimpleJsonArray()
                for (a in spec.alternateAccepted) alts.put(a)
                obj.put("alternates", alts)
            }
            is AnswerSpec.AbsoluteTolerance -> {
                obj.put("type", "abs_tol")
                obj.put("expected", spec.expected)
                obj.put("tolerance", spec.tolerance)
                val exacts = SimpleJsonArray()
                for (e in spec.allAcceptedExact) exacts.put(e)
                obj.put("alsoAcceptExact", exacts)
            }
            is AnswerSpec.RelativeTolerance -> {
                obj.put("type", "rel_tol")
                obj.put("expected", spec.expected)
                obj.put("relTolOverride", spec.relativeToleranceOverride)
                val exacts = SimpleJsonArray()
                for (e in spec.allAcceptedExact) exacts.put(e)
                obj.put("alsoAcceptExact", exacts)
            }
            is AnswerSpec.ScientificValue -> {
                obj.put("type", "sci")
                obj.put("mantissa", spec.mantissa)
                obj.put("exponent", spec.exponent)
                obj.put("relTolOverride", spec.effectiveRelTolOverride)
                obj.put("mustNormalize", spec.mustNormalize)
                val altArr = SimpleJsonArray()
                for ((m, e) in spec.alternateValues) {
                    val pairObj = SimpleJsonObject().put("m", m).put("e", e)
                    altArr.put(pairObj)
                }
                obj.put("alternateValues", altArr)
                val dblArr = SimpleJsonArray()
                for (d in spec.alsoAcceptDoubles) dblArr.put(d)
                obj.put("alsoAcceptDoubles", dblArr)
            }
            is AnswerSpec.Choice -> {
                obj.put("type", "choice")
                obj.put("correctOption", spec.correctOption)
                obj.put("correctIndex", spec.correctIndex)
                val opts = SimpleJsonArray()
                for (o in spec.options) opts.put(o)
                obj.put("options", opts)
            }
            is AnswerSpec.FractionSpec -> {
                obj.put("type", "fraction")
                obj.put("numerator", spec.numerator)
                obj.put("denominator", spec.denominator)
                obj.put("isFractionToDecimal", spec.isFractionToDecimal)
                obj.put("acceptCourseValues", spec.effectiveCourseValues)
                obj.put("toleranceOverride", spec.toleranceOverride)
            }
        }
        return obj
    }

    private fun decodeAnswerSpec(
        obj: SimpleJsonObject,
        fallbackAnswer: String,
        fallbackNumeric: Double
    ): AnswerSpec {
        return when (obj.optString("type")) {
            "exact" -> {
                val expected = obj.optLong("expected", fallbackNumeric.toLong())
                val altsArr = obj.optJSONArray("alternates")
                val alts = if (altsArr != null) {
                    (0 until altsArr.length()).map { altsArr.optLong(it) }.toSet()
                } else {
                    emptySet()
                }
                AnswerSpec.Exact(expected = expected, alternateAccepted = alts)
            }
            "abs_tol" -> {
                val expected = obj.optDouble("expected", fallbackNumeric)
                val tolerance = obj.optDouble("tolerance", 0.05)
                val exactsArr = obj.optJSONArray("alsoAcceptExact")
                val exacts = if (exactsArr != null) {
                    (0 until exactsArr.length()).mapNotNull {
                        val d = exactsArr.optDouble(it)
                        if (d.isNaN()) null else d
                    }
                } else {
                    emptyList()
                }
                AnswerSpec.AbsoluteTolerance(
                    expected = expected,
                    tolerance = tolerance,
                    alsoAcceptExact = exacts
                )
            }
            "rel_tol" -> {
                val expected = obj.optDouble("expected", fallbackNumeric)
                val relTol = if (obj.isNull("relTolOverride")) null else obj.optDouble("relTolOverride").takeIf { !it.isNaN() }
                val exactsArr = obj.optJSONArray("alsoAcceptExact")
                val exacts = if (exactsArr != null) {
                    (0 until exactsArr.length()).mapNotNull {
                        val d = exactsArr.optDouble(it)
                        if (d.isNaN()) null else d
                    }
                } else {
                    emptyList()
                }
                AnswerSpec.RelativeTolerance(
                    expected = expected,
                    relativeToleranceOverride = relTol,
                    alsoAcceptExact = exacts
                )
            }
            "sci" -> {
                val mantissa = obj.optDouble("mantissa", 1.0)
                val exponent = obj.optInt("exponent", 0)
                val relTol = if (obj.isNull("relTolOverride")) null else obj.optDouble("relTolOverride").takeIf { !it.isNaN() }
                val mustNormalize = obj.optBoolean("mustNormalize", false)
                val altArr = obj.optJSONArray("alternateValues")
                val alternates = if (altArr != null) {
                    (0 until altArr.length()).mapNotNull { idx ->
                        val p = altArr.optJSONObject(idx) ?: return@mapNotNull null
                        Pair(p.optDouble("m", 1.0), p.optInt("e", 0))
                    }
                } else {
                    emptyList()
                }
                val dblArr = obj.optJSONArray("alsoAcceptDoubles")
                val doubles = if (dblArr != null) {
                    (0 until dblArr.length()).mapNotNull {
                        val d = dblArr.optDouble(it)
                        if (d.isNaN()) null else d
                    }
                } else {
                    emptyList()
                }
                AnswerSpec.ScientificValue(
                    mantissa = mantissa,
                    exponent = exponent,
                    relativeToleranceOverride = relTol,
                    alternateValues = alternates,
                    alsoAcceptDoubles = doubles,
                    requireNormalizedMantissa = mustNormalize
                )
            }
            "choice" -> {
                val correctOption = obj.optString("correctOption", fallbackAnswer)
                val correctIndex = obj.optInt("correctIndex", 0)
                val optsArr = obj.optJSONArray("options")
                val options = if (optsArr != null) {
                    (0 until optsArr.length()).map { optsArr.optString(it) }
                } else {
                    listOf(correctOption)
                }
                AnswerSpec.Choice(
                    correctOption = correctOption,
                    options = options,
                    correctIndex = correctIndex
                )
            }
            "fraction" -> {
                val num = obj.optInt("numerator", 1)
                val den = obj.optInt("denominator", 2)
                val isF2D = obj.optBoolean("isFractionToDecimal", true)
                val course = obj.optBoolean("acceptCourseValues", false)
                val tol = if (obj.isNull("toleranceOverride")) null else obj.optDouble("toleranceOverride").takeIf { !it.isNaN() }
                AnswerSpec.FractionSpec(
                    numerator = num,
                    denominator = den,
                    isFractionToDecimal = isF2D,
                    acceptCourseValues = course,
                    toleranceOverride = tol
                )
            }
            else -> AnswerSpec.exact(fallbackNumeric.toLong())
        }
    }
}
