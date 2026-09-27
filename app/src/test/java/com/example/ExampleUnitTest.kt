package com.example

import com.example.domain.Difficulty
import com.example.domain.FractionsPool
import com.example.domain.QuestionGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testStandardQuestions() {
        val addQ = QuestionGenerator.generateStandardQuestion("+", 2, Difficulty.MEDIUM, false, false)
        assertTrue(addQ.questionDisplay.contains("+"))
        assertEquals((addQ.num1 + addQ.num2).toLong().toString(), addQ.answer)

        val subQ = QuestionGenerator.generateStandardQuestion("-", 2, Difficulty.MEDIUM, false, false)
        assertTrue(subQ.num1 >= subQ.num2)
        assertEquals((subQ.num1 - subQ.num2).toLong().toString(), subQ.answer)

        val mulQ = QuestionGenerator.generateStandardQuestion("*", 2, Difficulty.MEDIUM, false, false)
        assertTrue(mulQ.questionDisplay.contains("×"))
        assertEquals((mulQ.num1 * mulQ.num2).toLong().toString(), mulQ.answer)

        val divQ = QuestionGenerator.generateStandardQuestion("/", 2, Difficulty.MEDIUM, false, false)
        assertTrue(divQ.num1 % divQ.num2 == 0.0)
        assertEquals((divQ.num1 / divQ.num2).toLong().toString(), divQ.answer)
    }

    @Test
    fun testSquareAndSquareRoot() {
        val sqQ = QuestionGenerator.generateSquareQuestion(Pair(1, 25))
        assertTrue(sqQ.questionDisplay.endsWith("²"))
        val n = sqQ.num1.toInt()
        assertEquals((n * n).toString(), sqQ.answer)

        val sqrtQ = QuestionGenerator.generateSquareRootQuestion(Difficulty.EASY)
        assertTrue(sqrtQ.questionDisplay.startsWith("√"))
        val root = sqrtQ.answer.toInt()
        assertTrue(root in 1..10)
    }

    @Test
    fun testPercentageQuestion() {
        val pctQ = QuestionGenerator.generatePercentageQuestion(Difficulty.EASY)
        assertNotNull(pctQ.answer)
        assertTrue(pctQ.questionDisplay.contains("%"))
    }

    @Test
    fun testApproximationQuestion() {
        val approxQ = QuestionGenerator.generateApproximationQuestion(Difficulty.MEDIUM, false)
        assertTrue(approxQ.questionDisplay.contains("≈"))
        assertNotNull(approxQ.answer)
    }

    @Test
    fun testNumberSeriesQuestion() {
        val seriesQ = QuestionGenerator.generateNumberSeriesQuestion(Difficulty.MEDIUM)
        assertTrue(seriesQ.questionDisplay.contains("?"))
        assertNotNull(seriesQ.answer)
    }

    @Test
    fun testRatioQuestion() {
        val ratioQ = QuestionGenerator.generateRatioQuestion(Difficulty.MEDIUM)
        assertTrue(ratioQ.questionDisplay.contains("ratio") || ratioQ.questionDisplay.contains("A:B"))
        assertNotNull(ratioQ.answer)
    }

    @Test
    fun testChainCalculationQuestion() {
        val chainQ = QuestionGenerator.generateChainCalculationQuestion(Difficulty.EASY)
        assertNotNull(chainQ.answer)
    }

    @Test
    fun testFractionQuestion() {
        val fracQ = FractionsPool.generateFractionQuestion(1, 9, 2, 10)
        assertNotNull(fracQ.question)
        assertNotNull(fracQ.answer)
    }
}
