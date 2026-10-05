package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmushParserTest {
    // Copied from a finished game.
    private val played =
        """
        hankgreen.com/smush · Sep 14
        298 pts · ★★ pangram first
        🟩🟨🥞
        🥞⭐🥞
        🥞🥞🟨
        """.trimIndent()

    private val cleanPlate =
        """
        hankgreen.com/smush · Sep 14
        1240 pts · ★★ pangram first · clean plate · ICE COLD 🧊 · 3-day streak
        🥞🥞🥞
        🥞⭐🥞
        🥞🥞🥞
        """.trimIndent()

    @Test
    fun `reads the score`() {
        assertEquals(298, SmushParser.parse(played)!!.points)
    }

    @Test
    fun `reads the badges and leaves the score out of them`() {
        assertEquals(listOf("★★ pangram first"), SmushParser.parse(played)!!.badges)
    }

    @Test
    fun `collects the grid`() {
        val result = SmushParser.parse(played)!!
        assertEquals(3, result.grid.size)
        assertEquals("🥞⭐🥞", result.grid[1])
    }

    @Test
    fun `several badges are read in the order they are printed`() {
        val result =
            SmushParser.parse(
                """
                hankgreen.com/smush · Sep 14
                124 pts · ★ pangram · no hints · 3-day streak
                🟩🟨🥞
                🥞⭐🥞
                🥞🥞🟨
                """.trimIndent(),
            )!!
        assertEquals(listOf("★ pangram", "no hints", "3-day streak"), result.badges)
    }

    @Test
    fun `a score with no badges gives an empty list`() {
        val result =
            SmushParser.parse(
                """
                hankgreen.com/smush · Sep 14
                120 pts
                🟩🟩🟩
                🟩⭐🟩
                🟩🟩🟩
                """.trimIndent(),
            )!!
        assertEquals(emptyList<String>(), result.badges)
        assertEquals(120, result.points)
    }

    @Test
    fun `there is no win or loss, and no puzzle number`() {
        val result = SmushParser.parse(played)!!
        assertNull(result.success)
        assertNull(result.puzzleNumber)
    }

    @Test
    fun `names each badge in the share text`() {
        val result = SmushParser.parse(cleanPlate)!!
        assertTrue(result.pangram)
        assertTrue(result.pangramFirst)
        assertTrue(result.cleanPlate)
        assertTrue(result.iceCold)
        assertEquals(3, result.streakDays)
    }

    @Test
    fun `perfect stands for pangram first, clean plate and no hints`() {
        val result =
            SmushParser.parse(
                """
                hankgreen.com/smush · Sep 14
                612 pts · PERFECT 💯
                🥞🥞🥞
                🥞⭐🥞
                🥞🥞🥞
                """.trimIndent(),
            )!!
        assertEquals(listOf("PERFECT 💯"), result.badges)
        assertTrue(result.perfect)
        assertTrue(result.pangram)
        assertTrue(result.pangramFirst)
        assertTrue(result.cleanPlate)
        assertTrue(result.noHints)
        assertNull(result.streakDays)
    }

    @Test
    fun `perfect can come with ICE COLD and a streak`() {
        val result =
            SmushParser.parse(
                """
                hankgreen.com/smush · Sep 14
                3120 pts · PERFECT 💯 · ICE COLD 🧊 · 5-day streak
                🥞🥞🥞
                🥞⭐🥞
                🥞🥞🥞
                """.trimIndent(),
            )!!
        assertEquals(3120, result.points)
        assertTrue(result.perfect)
        assertTrue(result.iceCold)
        assertEquals(5, result.streakDays)
    }

    @Test
    fun `a clean plate and pangram first are still not perfect`() {
        val result = SmushParser.parse(cleanPlate)!!
        assertFalse(result.noHints)
        assertFalse(result.perfect)
    }

    @Test
    fun `found the pangram, but not first`() {
        val result =
            SmushParser.parse(
                """
                hankgreen.com/smush · Sep 14
                84 pts · ★ pangram
                🟩🟨🥞
                🥞⭐🥞
                🥞🥞🟨
                """.trimIndent(),
            )!!
        assertTrue(result.pangram)
        assertFalse(result.pangramFirst)
    }

    @Test
    fun `one tile short of perfect is read with the badges around it`() {
        val result =
            SmushParser.parse(
                """
                hankgreen.com/smush · Sep 14
                214 pts · ★★ pangram first · frik! 😤 · no hints
                🥞🥞🥞
                🥞⭐🟨
                🥞🥞🥞
                """.trimIndent(),
            )!!
        assertEquals(listOf("★★ pangram first", "frik! 😤", "no hints"), result.badges)
        assertTrue(result.pangramFirst)
        assertTrue(result.noHints)
        assertFalse(result.cleanPlate)
        assertFalse(result.perfect)
    }

    @Test
    fun `a streak can be the only badge`() {
        val result =
            SmushParser.parse(
                """
                hankgreen.com/smush · Sep 14
                57 pts · 2-day streak
                🟩🟨🟩
                🟨⭐🟩
                🟩🥞🟩
                """.trimIndent(),
            )!!
        assertEquals(57, result.points)
        assertEquals(listOf("2-day streak"), result.badges)
        assertFalse(result.pangram)
        assertEquals(2, result.streakDays)
    }

    @Test
    fun `a board without a release date has no date in the header`() {
        val text =
            """
            hankgreen.com/smush
            95 pts · ★ pangram
            🟩🥞🟨
            🥞⭐🥞
            🟨🥞🥞
            """.trimIndent()
        assertTrue(SmushParser.matches(text))
        val result = SmushParser.parse(text)!!
        assertEquals(95, result.points)
        assertEquals(listOf("★ pangram"), result.badges)
        assertEquals(3, result.grid.size)
    }

    @Test
    fun `matches only smush text`() {
        assertTrue(SmushParser.matches(played))
        assertFalse(
            SmushParser.matches(
                """
                4×6 · Mon Sep 14
                🟥🟥🟥🟥
                🟦🟦🟦🟦
                🟩🟩🟩🟩
                🟧🟧🟧🟧
                🟪🟪🟪🟪
                🟨🟨🟨🟨
                14/16 moves 💯 · 276 pts
                hankgreen.com/4x6
                """.trimIndent(),
            ),
        )
    }
}
