package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HuedParserTest {
    // Copied from a share.
    private val shared =
        """
        Hued #179: 100% Avis
        🟢👑🌈
        Hue Master!
        https://playhued.com
        """.trimIndent()

    // Copied with the Copy button, which leaves the link out.
    private val copied =
        """
        Hued #179: 61% Avis
        🟡🔴⚫${"\uFE0F"}
        Bright Effort
        """.trimIndent()

    @Test
    fun `reads the number, the accuracy and the color`() {
        val result = HuedParser.parse(shared)!!
        assertEquals("179", result.puzzleNumber)
        assertEquals(100, result.accuracy)
        assertEquals("Avis", result.colorName)
    }

    @Test
    fun `the accuracy is the best guess, not the last`() {
        assertEquals(61, HuedParser.parse(copied)!!.accuracy)
    }

    @Test
    fun `counts the guesses`() {
        assertEquals(3, HuedParser.parse(shared)!!.guesses)
        assertEquals(3, HuedParser.parse(copied)!!.guesses)
    }

    // As built by the game's own code for a color matched on the first guess.
    @Test
    fun `an unused guess is not counted`() {
        val text = shared.replace("🟢👑🌈", "🌈⚪⚪")
        assertEquals(1, HuedParser.parse(text)!!.guesses)
    }

    // As built by the game's own code for a hint shown after the first guess.
    @Test
    fun `reads whether the hint was used`() {
        assertFalse(HuedParser.parse(shared)!!.hintUsed)
        val result = HuedParser.parse(shared.replace("🟢👑🌈", "🟢📸👑🌈"))!!
        assertTrue(result.hintUsed)
        assertEquals(3, result.guesses)
    }

    // Puzzle 193 is named Gingerbread Cookie in the game's puzzle list.
    @Test
    fun `reads a color name of more than one word`() {
        val text = shared.replace("#179: 100% Avis", "#193: 100% Gingerbread Cookie")
        assertEquals("Gingerbread Cookie", HuedParser.parse(text)!!.colorName)
    }

    // Puzzle 179 is dated 7 December 2025 in the game's puzzle list. It was served again on
    // 4 November 2026.
    @Test
    fun `a puzzle number has no day`() {
        assertNull(HuedParser.parse(shared)!!.day)
    }

    @Test
    fun `a day has no win or loss`() {
        assertNull(HuedParser.parse(shared)!!.success)
    }

    @Test
    fun `matches only hued text`() {
        assertTrue(HuedParser.matches(shared))
        assertTrue(HuedParser.matches(copied))
        assertFalse(HuedParser.matches("Wordle 1,234 4/6"))
    }
}
