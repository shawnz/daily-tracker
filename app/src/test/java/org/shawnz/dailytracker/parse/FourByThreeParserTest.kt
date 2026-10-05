package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FourByThreeParserTest {
    // Copied from a finished game.
    private val solved =
        """
        September 14, 2026
        156 points • No mistakes
        🌟🟦🟦
        🌟🟨🟨
        🌟🟩🟩
        🌟🟪🟪
        https://4x3.fun
        """.trimIndent()

    @Test
    fun `reads the score, the mistakes and the day`() {
        val result = FourByThreeParser.parse(solved)!!
        assertEquals(156, result.points)
        assertEquals(0, result.mistakes)
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
        assertTrue(result.success)
    }

    @Test
    fun `collects the guesses`() {
        val result = FourByThreeParser.parse(solved)!!
        assertEquals(4, result.grid.size)
        assertEquals("🌟🟪🟪", result.grid.last())
    }

    // Copied from a finished game. "Mistake" is singular for one.
    private val oneMistake =
        """
        September 14, 2026
        126 points • 1 mistake
        🌟🟦🟦
        🌟🟨🟨
        🌟🟩🟪
        🌟🟩🟩
        🌟🟪🟪
        https://4x3.fun
        """.trimIndent()

    @Test
    fun `a single mistake is read from the singular wording`() {
        val result = FourByThreeParser.parse(oneMistake)!!
        assertEquals(126, result.points)
        assertEquals(1, result.mistakes)
        assertTrue(result.success)
    }

    @Test
    fun `a mistake costs a guess, so there is one more row than categories`() {
        assertEquals(5, FourByThreeParser.parse(oneMistake)!!.grid.size)
        assertEquals(4, FourByThreeParser.parse(solved)!!.grid.size)
    }

    // Copied from a lost game. The board ends at the third mistake, so a loss always has three
    // mistakes, and it is the only ending without a score.
    private val lost =
        """
        September 14, 2026
        Out of guesses • 3 mistakes
        🟨🟦🟩
        🟩🟦🟨
        🟪🟪🌟
        🟩🟩🌟
        🟨🌟🟦
        https://4x3.fun
        """.trimIndent()

    @Test
    fun `running out of guesses leaves no score`() {
        val result = FourByThreeParser.parse(lost)!!
        assertNull(result.points)
        assertEquals(3, result.mistakes)
        assertFalse(result.success)
    }

    @Test
    fun `a lost board still keeps every guess`() {
        assertEquals(5, FourByThreeParser.parse(lost)!!.grid.size)
    }

    // Copied from a finished game. The hub word can be named before the first guess: right is
    // worth 20 points with no penalty for mistakes, and wrong fails the board at once.
    private val calledIt =
        """
        September 14, 2026
        161 points • No mistakes • Called It 🎯
        🌟🟦🟦
        🌟🟩🟩
        🌟🟪🟪
        🌟🟨🟨
        https://4x3.fun
        """.trimIndent()

    private val calledWrong =
        """
        September 14, 2026
        0 points • Called the Wrong Hub
        Bold. Wrong, but bold.
        https://4x3.fun
        """.trimIndent()

    @Test
    fun `calling the hub right is marked and still counts the mistakes`() {
        val result = FourByThreeParser.parse(calledIt)!!
        assertTrue(result.calledIt)
        assertEquals(161, result.points)
        assertEquals(0, result.mistakes)
        assertTrue(result.success)
    }

    @Test
    fun `calling the wrong hub scores nothing and does not count as solved`() {
        val result = FourByThreeParser.parse(calledWrong)!!
        assertTrue(result.calledWrongHub)
        assertEquals(0, result.points)
        assertFalse(result.success)
        // Naming the wrong hub ends the board before a guess, so there is nothing to draw.
        assertEquals(emptyList<String>(), result.grid)
    }

    // "Called the Wrong Hub" must not read as the "Called It" that marks a right call.
    @Test
    fun `a wrong call is not a right one`() {
        assertFalse(FourByThreeParser.parse(calledWrong)!!.calledIt)
    }

    // Copied from a finished game. A board solved with the shared word last in every guess
    // scores -100, which is why the sign matters.
    private val ruleBreaker =
        """
        September 14, 2026
        -100 points • RULE BREAKER 💀
        🟦🟦🌟
        🟨🟨🌟
        🟩🟪🌟
        🟪🟪🌟
        🟩🟩🌟
        https://4x3.fun
        """.trimIndent()

    @Test
    fun `a rule breaker scores below zero and still counts as solved`() {
        val result = FourByThreeParser.parse(ruleBreaker)!!
        assertTrue(result.ruleBreaker)
        assertEquals(-100, result.points)
        assertTrue(result.success)
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
    }

    // The hub word went last every guess, which is what the ending is for.
    @Test
    fun `a rule breaker keeps every guess`() {
        val result = FourByThreeParser.parse(ruleBreaker)!!
        assertEquals(5, result.grid.size)
        assertTrue(result.grid.all { it.endsWith("🌟") })
    }

    // The ending replaces the mistake count, so the text doesn't state any mistakes.
    @Test
    fun `a rule breaker reports no mistake count`() {
        assertNull(FourByThreeParser.parse(ruleBreaker)!!.mistakes)
    }

    // A called board can't be lost, so a win can have three mistakes or more.
    private val calledWithMistakes =
        """
        September 14, 2026
        126 points • 3 mistakes • Called It 🎯
        🌟🟦🟦
        🌟🟩🟨
        🌟🟨🟨
        🌟🟩🟪
        🌟🟪🟩
        🌟🟩🟩
        🌟🟪🟪
        https://4x3.fun
        """.trimIndent()

    @Test
    fun `a called board is solved even after three mistakes`() {
        val result = FourByThreeParser.parse(calledWithMistakes)!!
        assertTrue(result.calledIt)
        assertEquals(126, result.points)
        assertEquals(3, result.mistakes)
        assertTrue(result.success)
        assertEquals(7, result.grid.size)
    }

    // An archive board is shared with its own address after the 4x3.fun link.
    private val archive =
        """
        September 14, 2026
        155 points • No mistakes
        🌟🟦🟦
        🌟🟨🟨
        🌟🟩🟩
        🌟🟪🟪
        https://4x3.fun
        https://www.hankgreen.com/fourbythree/#d=2026-09-14
        """.trimIndent()

    @Test
    fun `an archive board is read with its address after the link`() {
        val result = FourByThreeParser.parse(archive)!!
        assertEquals(155, result.points)
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
        assertEquals(4, result.grid.size)
    }

    @Test
    fun `a British English date is read`() {
        val result =
            FourByThreeParser.parse(
                "14 September 2026\n156 points • No mistakes\n" +
                    "🌟🟦🟦\n🌟🟨🟨\n🌟🟩🟩\n🌟🟪🟪\nhttps://4x3.fun",
            )!!
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
    }

    @Test
    fun `a date in another language leaves the day unknown`() {
        val result =
            FourByThreeParser.parse(
                "14 septembre 2026\n156 points • No mistakes\n" +
                    "🌟🟦🟦\n🌟🟨🟨\n🌟🟩🟩\n🌟🟪🟪\nhttps://4x3.fun",
            )!!
        assertNull(result.day)
        assertEquals(156, result.points)
    }

    @Test
    fun `the link is what identifies the text`() {
        assertTrue(FourByThreeParser.matches(solved))
        assertFalse(FourByThreeParser.matches("156 points • No mistakes"))
    }
}
