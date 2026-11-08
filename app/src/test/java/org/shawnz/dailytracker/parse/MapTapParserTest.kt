package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.MonthDay

class MapTapParserTest {
    // Copied from finished dailies.
    private val played =
        """
        www.maptap.gg November 4
        94🏅 99🔥 95🏅 90👑 80🌞
        Final score: 893
        """.trimIndent()

    private val other =
        """
        www.maptap.gg November 4
        90👑 68🫢 77🤗 96🔥 86🎓
        Final score: 858
        """.trimIndent()

    @Test
    fun `reads the final score and every round in the order played`() {
        val result = MapTapParser.parse(played)!!
        assertEquals(893, result.total)
        assertEquals(listOf(94, 99, 95, 90, 80), result.rounds)
        assertEquals(listOf(90, 68, 77, 96, 86), MapTapParser.parse(other)!!.rounds)
    }

    @Test
    fun `rounds 3 to 5 have weights of 2, 3 and 3 in the final score`() {
        listOf(played, other).forEach { text ->
            val result = MapTapParser.parse(text)!!
            val weights = listOf(1, 1, 2, 3, 3)
            assertEquals(result.total, result.rounds.zip(weights).sumOf { (score, weight) -> score * weight })
        }
    }

    @Test
    fun `the header has the month and day`() {
        assertEquals(PuzzleIdentity.MonthAndDay(MonthDay.of(11, 4)), MapTapParser.parse(played)!!.puzzle)
    }

    @Test
    fun `a day late in the year before is in that year`() {
        val puzzle = MapTapParser.parse(played)!!.puzzle
        assertEquals(LocalDate.of(2026, 11, 4), puzzle.resolveDay(numbering = null, latest = LocalDate.of(2027, 1, 2)))
    }

    // The Discord format and the format with the link hidden, as built by the game's own code.
    @Test
    fun `reads the header with each kind of link`() {
        listOf("https://maptap.gg", "MapTap").forEach { host ->
            val result = MapTapParser.parse(played.replace("www.maptap.gg", host))!!
            assertEquals(893, result.total, host)
            assertEquals(MonthDay.of(11, 4), result.monthDay, host)
        }
    }

    // Hand-edited into the flags format. The flags are made up.
    @Test
    fun `reads the flags format`() {
        val text = played.replace("94🏅 99🔥 95🏅 90👑 80🌞", "🇫🇷94 🇯🇵99 🇧🇷95 🌐90 🇨🇦80")
        assertEquals(listOf(94, 99, 95, 90, 80), MapTapParser.parse(text)!!.rounds)
    }

    @Test
    fun `the streak line is not a round`() {
        val result = MapTapParser.parse("$played\n🔥 12 day streak")!!
        assertEquals(listOf(94, 99, 95, 90, 80), result.rounds)
        assertEquals(893, result.total)
    }

    // A replay has the month as written in the page's URL, as in ?overrideday=july10.
    @Test
    fun `a replay is filed on its own day`() {
        val result = MapTapParser.parse(played.replace("November 4", "july 10"))!!
        assertEquals(MonthDay.of(7, 10), result.monthDay)
    }

    @Test
    fun `reads text whose line breaks became spaces`() {
        val result = MapTapParser.parse(played.replace("\n", " "))!!
        assertEquals(893, result.total)
        assertEquals(listOf(94, 99, 95, 90, 80), result.rounds)
    }

    // Player maps and Best Of maps, as built by the game's own code, have a title in place of the date.
    @Test
    fun `player maps and best of maps are not read`() {
        listOf(
            "Capitals by sam\n94🏅 99🔥 95🏅 90👑 80🌞\nFinal score: 893\nwww.maptap.gg/m/abc123",
            "Explorers\n94🏅 99🔥 95🏅 90👑 80🌞\nFinal score: 893\nwww.maptap.gg/?bestof=explorers",
        ).forEach { text ->
            assertFalse(MapTapParser.matches(text))
            assertNull(MapTapParser.parse(text))
        }
    }

    @Test
    fun `matches only maptap daily text`() {
        assertTrue(MapTapParser.matches(played))
        assertFalse(MapTapParser.matches("Wordle 1,234 4/6"))
    }
}
