package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class FoodGuessrParserTest {
    // The Friendly format, with scores grouped by [separator].
    private fun friendly(separator: String) =
        """
        I got 8${separator}791 on the FoodGuessr Daily!

        🌕🌖🌑🌑🌑 1${separator}718 (Round 1)
        🌕🌕🌕🌕🌕 5${separator}000 (Round 2) 💯
        🌕🌕🌘🌑🌑 2${separator}073 (Round 3)

        Monday, Sep 14, 2026
        Play here: https://www.foodguessr.com/
        """.trimIndent()

    private val played = friendly(",")

    private val compact =
        """
        FoodGuessr - Monday, Sep 14, 2026 UTC
        🌕🌖🌑🌑🌑 1,718 ⋅ Round 1
        🌕🌕🌕🌕🌕 5,000 ⋅ Round 2 💯
        🌕🌕🌘🌑🌑 2,073 ⋅ Round 3
        Total score: 8,791/15,000
        (+700 above today's average!) 🎉
        Play here: https://www.foodguessr.com/
        """.trimIndent()

    @Test
    fun `reads the total and every round in the order played`() {
        val result = FoodGuessrParser.parse(played)!!
        assertEquals(8791, result.total)
        assertEquals(listOf(1718, 5000, 2073), result.rounds)
    }

    @Test
    fun `the rounds account for the total`() {
        val result = FoodGuessrParser.parse(played)!!
        assertEquals(result.total, result.rounds.sum())
    }

    // A round that reaches five thousand is marked.
    @Test
    fun `counts the rounds marked perfect`() {
        assertEquals(1, FoodGuessrParser.parse(played)!!.perfectRounds)
    }

    @Test
    fun `reads the day`() {
        assertEquals(LocalDate.of(2026, 9, 14), FoodGuessrParser.parse(played)!!.day)
    }

    @Test
    fun `a day is scored rather than won or lost`() {
        assertNull(FoodGuessrParser.parse(played)!!.success)
    }

    @Test
    fun `a score grouped for another region is still read`() {
        for (separator in listOf(".", "'", "\u202F")) {
            val result = FoodGuessrParser.parse(friendly(separator))!!
            assertEquals(8791, result.total, separator)
            assertEquals(listOf(1718, 5000, 2073), result.rounds, separator)
        }
    }

    @Test
    fun `reads the compact format`() {
        val result = FoodGuessrParser.parse(compact)!!
        assertEquals(8791, result.total)
        assertEquals(listOf(1718, 5000, 2073), result.rounds)
        assertEquals(1, result.perfectRounds)
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
    }

    @Test
    fun `the line for a run above the average is not a round score`() {
        val result =
            FoodGuessrParser.parse(
                """
                I got 8,791 on the FoodGuessr Daily!

                That's 700 points above today's average! 🎉

                🌕🌖🌑🌑🌑 1,718 (Round 1)
                🌕🌕🌕🌕🌕 5,000 (Round 2) 💯
                🌕🌕🌘🌑🌑 2,073 (Round 3)

                Monday, Sep 14, 2026
                Play here: https://www.foodguessr.com/
                """.trimIndent(),
            )!!
        assertEquals(8791, result.total)
        assertEquals(listOf(1718, 5000, 2073), result.rounds)
    }

    // Only the Daily is recorded as the day's result.
    @Test
    fun `a daily double share is not read`() {
        val friendlyDouble =
            """
            I got 11,500 on the FoodGuessr Daily Double!

            That's 700 points above today's average! 🎉
            +2,000 from last time! 📈

            🌕🌕🌕🌕🌕 5,000 (Round 1) 💯 — was 4,200
            🌕🌕🌕🌕🌗 4,500 (Round 2) — was 3,900
            🌕🌕🌑🌑🌑 2,000 (Round 3) — was 1,400

            Monday, Sep 14, 2026
            Play here: https://www.foodguessr.com/
            """.trimIndent()
        val compactDouble =
            """
            FoodGuessr Daily Double - Monday, Sep 14, 2026 UTC
            🌕🌕🌕🌕🌘 4,200 ⋅ Round 1 (was 5,000)
            🌕🌕🌕🌕🌑 3,900 ⋅ Round 2 (was 4,500)
            🌕🌗🌑🌑🌑 1,400 ⋅ Round 3 (was 2,000)
            Total score: 9,500/15,000
            -2,000 from last time 📉
            """.trimIndent()
        for (text in listOf(friendlyDouble, compactDouble)) {
            assertFalse(FoodGuessrParser.matches(text))
            assertNull(FoodGuessrParser.parse(text))
        }
    }

    // Hand-edited: the lines joined, as some apps do on paste.
    @Test
    fun `the whole thing on one line reads the same`() {
        val result = FoodGuessrParser.parse(played.replace(Regex("\n+"), " "))!!
        assertEquals(8791, result.total)
        assertEquals(listOf(1718, 5000, 2073), result.rounds)
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
    }

    @Test
    fun `matches only foodguessr daily text`() {
        assertTrue(FoodGuessrParser.matches(played))
        assertTrue(FoodGuessrParser.matches(compact))
        assertFalse(FoodGuessrParser.matches("Wordle 1,234 4/6"))
    }

    @Test
    fun `a plate-off share is not this game`() {
        val plateOff =
            """
            I got 7/10 on today's FoodGuessr Plate-Off!

            ✅✅❌✅✅✅❌✅❌✅

            Monday, Sep 14, 2026
            Play here: https://www.foodguessr.com/game/plate-off/daily
            """.trimIndent()
        assertFalse(FoodGuessrParser.matches(plateOff))
        assertNull(FoodGuessrParser.parse(plateOff))
    }

    @Test
    fun `a cuisine match share is not this game`() {
        val cuisineMatch =
            """
            I got 17,000 on today's FoodGuessr Cuisine Match!

            ✅🟨❌✅✅

            Monday, Sep 14, 2026
            Play here: https://www.foodguessr.com/game/cuisine-match/daily
            """.trimIndent()
        assertFalse(FoodGuessrParser.matches(cuisineMatch))
        assertNull(FoodGuessrParser.parse(cuisineMatch))
    }
}
