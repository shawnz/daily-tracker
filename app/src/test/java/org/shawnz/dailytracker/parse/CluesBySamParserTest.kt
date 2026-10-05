package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class CluesBySamParserTest {
    // The two forms of share text.
    private val sentence =
        """
        I solved the daily #CluesBySam, Sep 25th 2026 (Hard), in 03:08
        🟩🟩🟩🟩
        🟩🟩🟩🟩
        🟩🟩🟩🟩
        🟩🟩🟩🟩
        🟩🟩🟩🟩
        https://cluesbysam.com
        """.trimIndent()

    private val heading =
        """
        #CluesBySam - Sep 25th 2026 (Hard)
        03:08
        🟩🟩🟩🟩
        🟩🟩🟩🟩
        🟩🟩🟩🟩
        🟩🟩🟩🟩
        🟩🟩🟩🟩
        """.trimIndent()

    private val board = List(5) { "🟩🟩🟩🟩" }.joinToString("\n")

    @Test
    fun `both forms give the same day, difficulty and time`() {
        for (text in listOf(sentence, heading)) {
            val result = CluesBySamParser.parse(text)!!
            assertEquals(LocalDate.of(2026, 9, 25), result.day)
            assertEquals("Hard", result.difficulty)
            assertEquals(188, result.seconds)
        }
    }

    @Test
    fun `both forms give the same board`() {
        for (text in listOf(sentence, heading)) {
            assertEquals(5, CluesBySamParser.parse(text)!!.grid.size)
        }
    }

    // A wrong answer is not accepted, so the wording of the two forms doesn't indicate how the
    // board went.
    @Test
    fun `both forms count as solved`() {
        assertTrue(CluesBySamParser.parse(sentence)!!.success)
        assertTrue(CluesBySamParser.parse(heading)!!.success)
    }

    // Wrong guesses and hints are logged in the tiles, so a board is not all green.
    @Test
    fun `a board with wrong guesses and hints keeps every tile`() {
        val result =
            CluesBySamParser.parse(
                """
                I solved the daily #CluesBySam, Sep 25th 2026 (Hard), in 01:14
                🟨🟩🟩🟩
                🟩🟩🟩🟩
                🟠🟨🟩🟩
                🟩🟨🟩🟩
                🟩🟩🟩🟩
                https://cluesbysam.com
                """.trimIndent(),
            )!!
        assertEquals(5, result.grid.size)
        assertEquals("🟠🟨🟩🟩", result.grid[2])
        assertEquals(74, result.seconds)
        assertTrue(result.success)
    }

    // 🟩 correct, 🟨 at least one mistake, 🟡 a hint, 🟠 a double hint.
    @Test
    fun `counts each way a person was identified`() {
        val result =
            CluesBySamParser.parse(
                """
                I solved the daily #CluesBySam, Sep 25th 2026 (Hard), in 01:14
                🟨🟩🟩🟩
                🟩🟩🟩🟩
                🟠🟨🟩🟡
                🟩🟨🟩🟩
                🟩🟩🟩🟩
                https://cluesbysam.com
                """.trimIndent(),
            )!!
        assertEquals(15, result.correct)
        assertEquals(3, result.mistakes)
        assertEquals(1, result.hints)
        assertEquals(1, result.doubleHints)
    }

    @Test
    fun `a clean board is every person correct`() {
        val result = CluesBySamParser.parse(sentence)!!
        assertEquals(20, result.correct)
        assertEquals(0, result.mistakes)
        assertEquals(0, result.hints)
        assertEquals(0, result.doubleHints)
    }

    @Test
    fun `a time written in words leaves the seconds unknown`() {
        val texts =
            listOf(
                "I solved the daily #CluesBySam, Sep 25th 2026 (Hard), in less than 7 minutes\n" +
                    "$board\nhttps://cluesbysam.com",
                "#CluesBySam - Sep 25th 2026 (Hard)\nLess than 7 minutes\n$board",
            )
        for (text in texts) {
            val result = CluesBySamParser.parse(text)!!
            assertNull(result.seconds)
            assertEquals(LocalDate.of(2026, 9, 25), result.day)
        }
    }

    @Test
    fun `a share without a time leaves the seconds unknown`() {
        val texts =
            listOf(
                "I solved the daily #CluesBySam, Sep 25th 2026 (Hard)!\n$board\nhttps://cluesbysam.com",
                "#CluesBySam - Sep 25th 2026 (Hard)\n\n$board",
            )
        for (text in texts) {
            val result = CluesBySamParser.parse(text)!!
            assertNull(result.seconds)
            assertEquals("Hard", result.difficulty)
            assertEquals(5, result.grid.size)
        }
    }

    @Test
    fun `a share without a difficulty leaves it unknown`() {
        val result =
            CluesBySamParser.parse(
                "I solved the daily #CluesBySam, Sep 25th 2026, in 03:08\n$board\nhttps://cluesbysam.com",
            )!!
        assertNull(result.difficulty)
        assertEquals(188, result.seconds)
        assertEquals(LocalDate.of(2026, 9, 25), result.day)
    }

    @Test
    fun `an ordinal other than th is read`() {
        val result = CluesBySamParser.parse("#CluesBySam - Sep 1st 2026 (Hard)\n01:02\n$board")!!
        assertEquals(LocalDate.of(2026, 9, 1), result.day)
        assertEquals(62, result.seconds)
    }

    // The percentile line is only in the text copied by double-clicking Copy Text.
    @Test
    fun `reads the percentile when the game ranks the time`() {
        val result =
            CluesBySamParser.parse(
                "I solved the daily #CluesBySam, Sep 25th 2026 (Hard), in 01:14\n" +
                    "Percentile: top 5%\n$board\nhttps://cluesbysam.com",
            )!!
        assertEquals("top 5%", result.percentile)
        assertEquals(5, result.grid.size)
    }

    // Double-clicking Copy Text leaves the percentile line blank for a time outside the ranked
    // half.
    @Test
    fun `a blank percentile line has no percentile`() {
        val result =
            CluesBySamParser.parse(
                "I solved the daily #CluesBySam, Sep 25th 2026 (Hard), in 03:08\n" +
                    "\n$board\nhttps://cluesbysam.com",
            )!!
        assertNull(result.percentile)
        assertEquals(188, result.seconds)
        assertEquals(5, result.grid.size)
    }

    @Test
    fun `a share without a double click has no percentile`() {
        assertNull(CluesBySamParser.parse(sentence)!!.percentile)
    }

    // Double-clicking Copy Text gives the exact time, even above six minutes.
    @Test
    fun `reads an exact time of over a hundred minutes`() {
        val result =
            CluesBySamParser.parse(
                "I solved the daily #CluesBySam, Sep 25th 2026 (Hard), in 123:45\n" +
                    "\n$board\nhttps://cluesbysam.com",
            )!!
        assertEquals(7425, result.seconds)
    }

    @Test
    fun `matches only clues by sam text`() {
        assertTrue(CluesBySamParser.matches(sentence))
        assertTrue(CluesBySamParser.matches(heading))
        assertFalse(CluesBySamParser.matches("Wordle 1,234 4/6"))
    }
}
