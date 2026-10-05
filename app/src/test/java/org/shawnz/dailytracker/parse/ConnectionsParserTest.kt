package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ConnectionsParserTest {
    // Copied from a finished game.
    private val won =
        """
        Connections
        Puzzle #1191
        🟦🟦🟦🟦
        🟩🟩🟩🟩
        🟨🟨🟨🟨
        🟪🟪🟪🟪
        """.trimIndent()

    private val lost =
        """
        Connections
        Puzzle #1191
        🟩🟩🟩🟦
        🟩🟩🟩🟩
        🟨🟪🟨🟨
        🟪🟪🟨🟨
        🟦🟨🟨🟨
        """.trimIndent()

    @Test
    fun `a board with every group solved and no mistake is a win`() {
        val result = ConnectionsParser.parse(won)!!
        assertEquals(4, result.groupsSolved)
        assertEquals(0, result.mistakes)
        assertTrue(result.success)
    }

    // The board ends at the fourth mistake, which is the only way to lose.
    @Test
    fun `four mistakes is a loss however many groups were solved`() {
        val result = ConnectionsParser.parse(lost)!!
        assertEquals(4, result.mistakes)
        assertEquals(1, result.groupsSolved)
        assertFalse(result.success)
    }

    @Test
    fun `reads the puzzle number`() {
        assertEquals("1191", ConnectionsParser.parse(won)!!.puzzleNumber)
    }

    // Puzzle one was published on 2023-06-12.
    @Test
    fun `a puzzle number is the day that puzzle was published`() {
        assertEquals(LocalDate.of(2026, 9, 14), ConnectionsParser.parse(won)!!.day)
        assertEquals(
            LocalDate.of(2023, 6, 12),
            ConnectionsParser.parse("Connections\nPuzzle #1\n🟨🟨🟨🟨\n🟩🟩🟩🟩\n🟦🟦🟦🟦\n🟪🟪🟪🟪")!!.day,
        )
    }

    @Test
    fun `keeps the guesses in the order they were made`() {
        val result = ConnectionsParser.parse(lost)!!
        assertEquals(5, result.grid.size)
        assertEquals("🟩🟩🟩🟦", result.grid.first())
    }

    @Test
    fun `an archive puzzle is read the same way`() {
        val result =
            ConnectionsParser.parse(
                "Archive September 1, 2026\nConnections Puzzle #1178\n" +
                    "🟨🟨🟨🟦\n🟨🟨🟨🟨\n🟩🟩🟩🟩\n🟦🟦🟦🟦\n🟪🟪🟪🟪",
            )!!
        assertEquals("1178", result.puzzleNumber)
        assertEquals(4, result.groupsSolved)
        assertEquals(1, result.mistakes)
        assertTrue(result.success)
    }

    // A bonus puzzle is outside the daily numbering, so its share text doesn't have one.
    @Test
    fun `a bonus puzzle is not read`() {
        val bonus = "Connections 3x3\nBonus September 16, 2026\n\n🟨🟨🟨\n🟦🟦🟦\n🟪🟪🟪"
        assertFalse(ConnectionsParser.matches(bonus))
        assertNull(ConnectionsParser.parse(bonus))
    }

    @Test
    fun `matches only connections text`() {
        assertTrue(ConnectionsParser.matches(won))
        assertFalse(ConnectionsParser.matches("Wordle 1,234 4/6"))
        assertFalse(ConnectionsParser.matches("Connections is a good game"))
    }
}
