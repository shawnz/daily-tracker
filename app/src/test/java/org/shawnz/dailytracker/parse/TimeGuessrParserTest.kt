package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class TimeGuessrParserTest {
    // Copied from a finished daily.
    private val detailed =
        """
        TimeGuessr #1251 — 38,283/50,000

        1️⃣ 🏆2,953 · 📅 10y · 🌍 6404.0mi
        2️⃣ 🏆9,944 · 📅 0y · 🌍 2.7mi
        3️⃣ 🏆9,904 · 📅 0y · 🌍 6.4mi
        4️⃣ 🏆5,781 · 📅 26y · 🌍 6.5mi
        5️⃣ 🏆9,701 · 📅 3y · 🌍 442ft

        https://timeguessr.com
        """.trimIndent()

    // The same daily in the emoji format, as built from those rounds by the game's own code.
    private val emoji =
        """
        TimeGuessr #1251 38,283/50,000
        🌎⬛⬛⬛ 📅🟩⬛⬛
        🌎🟩🟩🟨 📅🟩🟩🟩
        🌎🟩🟩🟨 📅🟩🟩🟩
        🌎🟩🟩🟨 📅⬛⬛⬛
        🌎🟩🟩🟨 📅🟩🟩⬛
        https://timeguessr.com
        """.trimIndent()

    @Test
    fun `reads the number and the total`() {
        val result = TimeGuessrParser.parse(detailed)!!
        assertEquals("1251", result.puzzleNumber)
        assertEquals(38283, result.total)
    }

    @Test
    fun `reads every round score in the order played`() {
        assertEquals(listOf(2953, 9944, 9904, 5781, 9701), TimeGuessrParser.parse(detailed)!!.rounds)
    }

    @Test
    fun `the total is the score of every round`() {
        val result = TimeGuessrParser.parse(detailed)!!
        assertEquals(result.total, result.rounds.sum())
    }

    // Daily 1251 is dated 2 November 2026 in the game's archive.
    @Test
    fun `a daily number is the day that daily was published`() {
        assertEquals(LocalDate.of(2026, 11, 2), TimeGuessrParser.parse(detailed)!!.day)
    }

    @Test
    fun `reads the emoji format, which has no round scores`() {
        val result = TimeGuessrParser.parse(emoji)!!
        assertEquals("1251", result.puzzleNumber)
        assertEquals(38283, result.total)
        assertEquals(emptyList<Int>(), result.rounds)
    }

    // A round score is grouped for the player's region. The separators are edited in.
    @Test
    fun `reads round scores grouped for other regions`() {
        listOf(".", "\u202F", "\u00A0", "'").forEach { separator ->
            val result =
                TimeGuessrParser.parse(
                    detailed.replace(Regex("""(🏆\d),""")) { it.groupValues[1] + separator },
                )!!
            assertEquals(listOf(2953, 9944, 9904, 5781, 9701), result.rounds, "separator $separator")
            assertEquals(38283, result.total)
        }
    }

    @Test
    fun `reads text whose line breaks became spaces`() {
        val result = TimeGuessrParser.parse(detailed.replace(Regex("\n+"), " "))!!
        assertEquals(38283, result.total)
        assertEquals(5, result.rounds.size)
    }

    @Test
    fun `a daily has no win or loss`() {
        assertNull(TimeGuessrParser.parse(detailed)!!.success)
    }

    // Text from single-player and custom games, as built by the game's own code, has no number.
    @Test
    fun `single-player and custom games are not read`() {
        listOf(
            detailed.replace("TimeGuessr #1251 —", "TimeGuessr —"),
            emoji.replace("TimeGuessr #1251", "TimeGuessr"),
        ).forEach { text ->
            assertFalse(TimeGuessrParser.matches(text))
            assertNull(TimeGuessrParser.parse(text))
        }
    }

    @Test
    fun `matches only timeguessr daily text`() {
        assertTrue(TimeGuessrParser.matches(detailed))
        assertTrue(TimeGuessrParser.matches(emoji))
        assertFalse(TimeGuessrParser.matches("Wordle 1,234 4/6"))
    }
}
