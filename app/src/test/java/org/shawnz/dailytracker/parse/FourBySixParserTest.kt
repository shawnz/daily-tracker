package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FourBySixParserTest {
    private val shelves =
        """
        🟥🟥🟥🟥
        🟦🟦🟦🟦
        🟩🟩🟩🟩
        🟧🟧🟧🟧
        🟪🟪🟪🟪
        🟨🟨🟨🟨
        """.trimIndent()

    // Copied from a finished game.
    private val solved = "4×6 · Mon Sep 14\n$shelves\n14/16 moves 💯 · 276 pts\nhankgreen.com/4x6"

    private fun tuesday(stats: String) = "4×6 · Tue Sep 15\n$shelves\n$stats\nhankgreen.com/4x6"

    @Test
    fun `reads the moves against par, the medal and the score`() {
        val result = FourBySixParser.parse(solved)!!
        assertEquals(14, result.moves)
        assertEquals(16, result.par)
        assertEquals("💯", result.medal)
        assertEquals(276, result.points)
    }

    @Test
    fun `a streak is read when the game prints one`() {
        val text = solved.replace("276 pts", "279 pts · 🔥4")
        assertEquals(4, FourBySixParser.parse(text)!!.streakDays)
    }

    @Test
    fun `a run below two days prints no streak`() {
        assertNull(FourBySixParser.parse(solved)!!.streakDays)
    }

    @Test
    fun `collects the shelves`() {
        val result = FourBySixParser.parse(solved)!!
        assertEquals(6, result.grid.size)
        assertEquals("🟨🟨🟨🟨", result.grid.last())
    }

    // Results saved by older versions of the game have no score.
    @Test
    fun `a run with no score leaves it unknown`() {
        val result = FourBySixParser.parse(tuesday("22/17 moves ✅"))!!
        assertNull(result.points)
        assertEquals("✅", result.medal)
    }

    @Test
    fun `the date carries no year, so the day stays unknown`() {
        assertNull(FourBySixParser.parse(solved)!!.day)
    }

    // 🛟 spoiled, 💯 perfect, 🥇 within par, 🥈 within two of par, ✅ otherwise.
    @Test
    fun `the medal says how the board was cleared`() {
        assertTrue(FourBySixParser.parse(solved)!!.perfect)
        assertFalse(FourBySixParser.parse(solved)!!.spoiled)

        val shown = FourBySixParser.parse(tuesday("15/17 moves 🛟 · 1 pts"))!!
        assertTrue(shown.spoiled)
        assertFalse(shown.perfect)
    }

    @Test
    fun `a gold run is read`() {
        val text =
            "4×6 · Sun Sep 13\n$shelves\n21/22 moves 🥇 · 259 pts · 🔥4\nhankgreen.com/4x6"
        val result = FourBySixParser.parse(text)!!
        assertEquals(21, result.moves)
        assertEquals(22, result.par)
        assertEquals("🥇", result.medal)
        assertEquals(259, result.points)
        assertEquals(4, result.streakDays)
        assertFalse(result.perfect)
    }

    @Test
    fun `a silver run is read`() {
        val text = solved.replace("14/16 moves 💯 · 276 pts", "18/16 moves 🥈 · 201 pts")
        val result = FourBySixParser.parse(text)!!
        assertEquals(18, result.moves)
        assertEquals("🥈", result.medal)
        assertEquals(201, result.points)
    }

    // A spoiled board scores only its streak bonus.
    @Test
    fun `a spoiled run keeps its points and streak`() {
        val result = FourBySixParser.parse(tuesday("15/17 moves 🛟 · 3 pts · 🔥3"))!!
        assertTrue(result.spoiled)
        assertEquals(3, result.points)
        assertEquals(3, result.streakDays)
    }

    @Test
    fun `matches only four by six text`() {
        assertTrue(FourBySixParser.matches(solved))
        assertFalse(FourBySixParser.matches("hankgreen.com/smush · Sep 14\n298 pts"))
    }
}
