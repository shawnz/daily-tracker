package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
        assertNull(result.day)
    }

    @Test
    fun `the day is the header's date in the year it was read`() {
        val result = SmushParser.parse(played, today = LocalDate.of(2026, 9, 14))!!
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
    }

    @Test
    fun `an earlier board is filed on its own day`() {
        val result = SmushParser.parse(played, today = LocalDate.of(2026, 10, 12))!!
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
    }

    // The header as `shareText` writes it for a board released on 31 December 2026.
    @Test
    fun `a date later in the year than today is in the year before`() {
        val text = played.replace("Sep 14", "Dec 31")
        val result = SmushParser.parse(text, today = LocalDate.of(2027, 1, 2))!!
        assertEquals(LocalDate.of(2026, 12, 31), result.day)
    }

    // The headers as `shareText` writes them in an Emirati, a British and a South African
    // browser. The last is for the board released on 4 September.
    @Test
    fun `a date in another English locale's form is read`() {
        val today = LocalDate.of(2026, 9, 14)
        for (date in listOf("14 Sep", "14 Sept")) {
            val result = SmushParser.parse(played.replace("Sep 14", date), today)!!
            assertEquals(LocalDate.of(2026, 9, 14), result.day, date)
        }
        val padded = SmushParser.parse(played.replace("Sep 14", "04 Sept"), today)!!
        assertEquals(LocalDate.of(2026, 9, 4), padded.day)
    }

    // The header as `shareText` writes it in an Australian browser for the board released on
    // 4 July.
    @Test
    fun `a month written in four letters is read`() {
        val text = played.replace("Sep 14", "4 July")
        val result = SmushParser.parse(text, today = LocalDate.of(2026, 9, 14))!!
        assertEquals(LocalDate.of(2026, 7, 4), result.day)
    }

    // The headers as `shareText` writes them in a French, a German and a Japanese browser.
    @Test
    fun `a date in another language gives no day`() {
        for (date in listOf("14 sept.", "14. Sept.", "9月14日")) {
            val text = played.replace("Sep 14", date)
            val result = SmushParser.parse(text, today = LocalDate.of(2026, 9, 14))!!
            assertNull(result.day, date)
            assertEquals(298, result.points, date)
        }
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
