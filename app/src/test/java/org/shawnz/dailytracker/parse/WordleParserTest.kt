package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class WordleParserTest {
    // Three readings across the run: launch day, one reported widely at the time, and one
    // seen in the game five years later.
    @ParameterizedTest(name = "{1} was published on {2}")
    @MethodSource("publishedDays")
    fun `a puzzle number is the day that puzzle was published`(
        text: String,
        header: String,
        day: LocalDate,
    ) {
        assertEquals(day, WordleParser.parse(text)?.day)
    }

    @Test
    fun `parses a solved puzzle and strips the thousands separator`() {
        val result = WordleParser.parse("Wordle 1,234 4/6\n\n$VINYL_IN_FOUR")!!
        assertEquals("1234", result.puzzleNumber)
        assertEquals(4, result.guesses)
        assertFalse(result.hardMode)
        assertTrue(result.success)
    }

    // German, French and Swiss German grouping.
    @ParameterizedTest(name = "reads the number in Wordle {0} 4/6")
    @ValueSource(strings = ["1.234", "1\u202F234", "1’234"])
    fun `reads a number grouped for another locale`(number: String) {
        val result = WordleParser.parse("Wordle $number 4/6\n\n$VINYL_IN_FOUR")!!
        assertEquals("1234", result.puzzleNumber)
        assertEquals(LocalDate.of(2024, 11, 4), result.day)
    }

    @Test
    fun `a failed puzzle has no guess count`() {
        val text =
            """
            Wordle 1,235 X/6

            🟨⬛⬛⬛🟨
            ⬛⬛🟨🟨⬛
            🟨🟨🟩🟩⬛
            ⬛🟨🟩🟨⬛
            ⬛⬛🟨🟨⬛
            🟩⬛🟩🟩⬛
            """.trimIndent()
        val result = WordleParser.parse(text)!!
        // Null for a failed puzzle, so it is left out of any average of guesses.
        assertNull(result.guesses)
        assertFalse(result.success)
        assertEquals(6, result.grid.size)
    }

    @Test
    fun `hard mode is captured alongside the guess count`() {
        val result = WordleParser.parse("Wordle 1,236 3/6*\n\n⬛🟩⬛⬛⬛\n⬛🟩🟩⬛⬛\n🟩🟩🟩🟩🟩")!!
        assertEquals(3, result.guesses)
        assertTrue(result.hardMode)
    }

    @Test
    fun `a failed puzzle in hard mode keeps the hard mode mark`() {
        val text =
            """
            Wordle 1,235 X/6*

            🟨⬛⬛⬛🟨
            ⬛⬛⬛🟨🟨
            🟩⬛⬛🟨🟨
            🟩🟩🟨⬛⬛
            🟩🟩⬛⬛🟨
            🟩🟩⬛🟩⬛
            """.trimIndent()
        val result = WordleParser.parse(text)!!
        assertNull(result.guesses)
        assertTrue(result.hardMode)
    }

    @Test
    fun `collects the emoji grid and ignores surrounding text`() {
        // The last line was added by hand after pasting.
        val text =
            """
            Wordle 1,234 3/6

            ⬛⬛🟨⬛⬛
            🟨🟩🟩⬛⬛
            🟩🟩🟩🟩🟩

            play at nytimes.com
            """.trimIndent()
        val result = WordleParser.parse(text)!!
        assertEquals(3, result.grid.size)
        assertEquals("🟩🟩🟩🟩🟩", result.grid.last())
    }

    @Test
    fun `high contrast tiles are still recognized as grid rows`() {
        val result = WordleParser.parse("Wordle 1,240 2/6\n\n⬛🟦🟧🟧🟧\n🟧🟧🟧🟧🟧")!!
        assertEquals(2, result.grid.size)
    }

    @Test
    fun `an archive puzzle in light mode is read from the line below the date`() {
        val text =
            """
            Archive September 8, 2026
            Wordle 1,907 4/6

            ⬜⬜⬜🟨🟨
            ⬜🟨⬜🟨🟩
            ⬜🟩🟩🟩🟩
            🟩🟩🟩🟩🟩
            """.trimIndent()
        val result = WordleParser.parse(text)!!
        assertEquals(LocalDate.of(2026, 9, 8), result.day)
        assertEquals(4, result.guesses)
        assertEquals("⬜🟨⬜🟨🟩", result.grid[1])
    }

    @Test
    fun `reads the skill and luck scores from text shared by WordleBot`() {
        val text =
            """
            Wordle 1,939 3/6

            ⬛⬛⬛⬛⬛
            ⬛⬛⬛🟨🟨
            🟩🟩🟩🟩🟩

            WordleBot
            Skill 77/99
            Luck 55/99
            """.trimIndent()
        val result = WordleParser.parse(text)!!
        assertEquals(77, result.skill)
        assertEquals(55, result.luck)
        assertEquals(3, result.guesses)
        assertEquals(3, result.grid.size)
        assertEquals(LocalDate.of(2026, 10, 10), result.day)
    }

    @Test
    fun `text shared by the game has no WordleBot scores`() {
        val result = WordleParser.parse("Wordle 1,234 4/6\n\n$VINYL_IN_FOUR")!!
        assertNull(result.skill)
        assertNull(result.luck)
    }

    @Test
    fun `matches only wordle text`() {
        assertTrue(WordleParser.matches("Wordle 1,234 4/6\n\n$VINYL_IN_FOUR"))
        assertFalse(WordleParser.matches("Connections\nPuzzle #412\n🟦🟦🟦🟦\n🟩🟩🟩🟩\n🟨🟨🟨🟨\n🟪🟪🟪🟪"))
        assertFalse(WordleParser.matches("just some text"))
    }

    companion object {
        /** Puzzle 1,234, VINYL, solved on the fourth guess. */
        private const val VINYL_IN_FOUR = "⬛⬛⬛🟨⬛\n⬛🟨⬛⬛⬛\n⬛🟩🟩⬛🟨\n🟩🟩🟩🟩🟩"

        @JvmStatic
        fun publishedDays() =
            listOf(
                arguments(
                    "Wordle 0 2/6\n\n⬛⬛🟨🟨⬛\n🟩🟩🟩🟩🟩",
                    "Wordle 0",
                    LocalDate.of(2021, 6, 19),
                ),
                arguments(
                    "Wordle 500 3/6\n\n⬛⬛⬛🟨🟨\n⬛🟨🟨🟨🟨\n🟩🟩🟩🟩🟩",
                    "Wordle 500",
                    LocalDate.of(2022, 11, 1),
                ),
                arguments(
                    "Wordle 1,907 4/6\n\n⬛⬛⬛🟨🟨\n⬛🟨⬛🟨🟩\n⬛🟩🟩🟩🟩\n🟩🟩🟩🟩🟩",
                    "Wordle 1,907",
                    LocalDate.of(2026, 9, 8),
                ),
            )
    }
}
