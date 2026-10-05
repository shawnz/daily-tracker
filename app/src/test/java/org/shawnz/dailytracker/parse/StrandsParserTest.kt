package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StrandsParserTest {
    // Copied from a finished game.
    private val played =
        """
        Strands #925
        “OH MY GOSH!!”
        🔵💡🔵🟡
        🔵🔵🔵🔵
        """.trimIndent()

    @Test
    fun `reads the number and the theme`() {
        val result = StrandsParser.parse(played)!!
        assertEquals("925", result.puzzleNumber)
        assertEquals("OH MY GOSH!!", result.clue)
    }

    // 🔵 a theme word, 🟡 the spangram, 💡 a hint.
    @Test
    fun `counts the answers, the spangram and the hints`() {
        val result = StrandsParser.parse(played)!!
        assertEquals(6, result.themeWords)
        assertEquals(true, result.spangramFound)
        assertEquals(1, result.hints)
    }

    // The second hint shows the letters of the same word in order.
    @Test
    fun `two hints in a row are both counted`() {
        val result =
            StrandsParser.parse("Strands #925\n“OH MY GOSH!!”\n🔵🔵💡💡\n🔵🔵🟡🔵\n🔵")!!
        assertEquals(6, result.themeWords)
        assertEquals(2, result.hints)
    }

    // The emoji are wrapped at four, so a line break is not part of the board.
    @Test
    fun `keeps the emoji as printed`() {
        val result = StrandsParser.parse(played)!!
        assertEquals(2, result.emoji.size)
        assertEquals("🔵🔵🔵🔵", result.emoji.last())
    }

    // Puzzle one was published on 2024-03-04.
    @Test
    fun `a puzzle number is the day that puzzle was published`() {
        assertEquals(LocalDate.of(2026, 9, 14), StrandsParser.parse(played)!!.day)
        assertEquals(
            LocalDate.of(2024, 3, 4),
            StrandsParser.parse("Strands #1\n“Mark my words”\n🔵🔵🔵🟡\n🔵🔵🔵")!!.day,
        )
    }

    @Test
    fun `the board is always finished, so there is no loss`() {
        assertTrue(StrandsParser.parse(played)!!.success)
    }

    @Test
    fun `a themed board is counted by its own emoji`() {
        val result =
            StrandsParser.parse("Strands #607\n“Good bones”\n🦴🦴🕯️🦴\n🦴💀🦴🦴\n🦴")!!
        assertEquals(7, result.themeWords)
        assertEquals(true, result.spangramFound)
        assertEquals(1, result.hints)
    }

    // On these boards 🔵 or 🟡 is a theme word, and the spangram has its own emoji.
    @Test
    fun `a themed board can use the usual emoji for theme words`() {
        val rainbow =
            StrandsParser.parse("Strands #483\n“Hue are my sunshine”\n🔵🟢💡🔴\n🟠🏳️‍🌈🟣🟡")!!
        assertEquals(6, rainbow.themeWords)
        assertEquals(true, rainbow.spangramFound)
        assertEquals(1, rainbow.hints)

        val shows =
            StrandsParser.parse("Strands #652\n“Palette episodes”\n🟡🟡⚪️⚪️\n📺🟢🟢🔵\n🔵")!!
        assertEquals(8, shows.themeWords)
        assertEquals(true, shows.spangramFound)
        assertEquals(0, shows.hints)
    }

    @Test
    fun `other themed boards are counted too`() {
        val clover =
            StrandsParser.parse(
                "Strands #744\n“Happy Saint Patrick's Day!”\n🟢🟢🟢☘️\n🟢🟢🟢🟢\n🟢",
            )!!
        assertEquals(8, clover.themeWords)
        assertEquals(true, clover.spangramFound)

        val fireworks =
            StrandsParser.parse("Strands #853\n“Happy 4th of July!”\n🎆💡🎆🎆\n🇺🇸🎆🎆")!!
        assertEquals(5, fireworks.themeWords)
        assertEquals(true, fireworks.spangramFound)
        assertEquals(1, fireworks.hints)

        val hotDogs =
            StrandsParser.parse("Strands #899\n“For better or wurst”\n🧺🧺💡🧺\n🧺🌭🧺🧺")!!
        assertEquals(6, hotDogs.themeWords)
        assertEquals(true, hotDogs.spangramFound)
        assertEquals(1, hotDogs.hints)
    }

    // #607's board under a number that isn't in the table, as a themed puzzle published later would be.
    @Test
    fun `an unlisted themed board has no counts`() {
        val result =
            StrandsParser.parse("Strands #925\n“Good bones”\n🦴🦴🕯️🦴\n🦴💀🦴🦴\n🦴")!!
        assertNull(result.themeWords)
        assertNull(result.spangramFound)
        assertNull(result.hints)
    }

    @Test
    fun `an archive puzzle is read the same way`() {
        val result =
            StrandsParser.parse(
                "Archive September 1, 2026\nStrands #912\n“Called by your calling”\n🔵🔵🟡🔵\n🔵🔵",
            )!!
        assertEquals("912", result.puzzleNumber)
        assertEquals(2, result.emoji.size)
    }

    // Text a user added after the blurb.
    @Test
    fun `text after the emoji is not one of them`() {
        val result =
            StrandsParser.parse(
                "Strands #925\n“OH MY GOSH!!”\n🔵🔵🔵🟡\n🔵🔵🔵\nplay at nytimes.com",
            )!!
        assertEquals(2, result.emoji.size)
    }

    // A bonus puzzle doesn't count toward the stats.
    @Test
    fun `a bonus puzzle is not read`() {
        val bonus = "Bonus Strands\nBonus June 8, 2026\n“Theme”\n\n☺️🤪🫠😉\n🥴😩💙"
        assertFalse(StrandsParser.matches(bonus))
        assertNull(StrandsParser.parse(bonus))
    }

    @Test
    fun `matches only strands text`() {
        assertTrue(StrandsParser.matches(played))
        assertFalse(StrandsParser.matches("Connections\nPuzzle #1191\n🟦🟦🟦🟦\n🟩🟩🟩🟩\n🟨🟨🟨🟨\n🟪🟪🟪🟪"))
    }
}
