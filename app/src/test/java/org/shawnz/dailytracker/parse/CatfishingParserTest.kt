package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CatfishingParserTest {
    // Copied from a finished day.
    private val played =
        """
        catfishing.net
        #813 - 1/10
        🐟🐟🐟🐟🐟
        🐈🐟🐟🐟🐟
        """.trimIndent()

    @Test
    fun `reads the number and the score`() {
        val result = CatfishingParser.parse(played)!!
        assertEquals("813", result.puzzleNumber)
        assertEquals(1.0, result.score)
        assertEquals(10, result.total)
    }

    @Test
    fun `keeps the board`() {
        val result = CatfishingParser.parse(played)!!
        assertEquals(2, result.grid.size)
        assertEquals("🐈🐟🐟🐟🐟", result.grid.last())
    }

    @Test
    fun `a day is scored rather than won or lost`() {
        assertNull(CatfishingParser.parse(played)!!.success)
    }

    // A close guess is worth half a point.
    @Test
    fun `a close guess scores a half`() {
        val result = CatfishingParser.parse("catfishing.net\n#813 - 4.5/10\n🐈🥚🐟🐈🐈\n🐟🐈🐟🐟🐟")!!
        assertEquals(4.5, result.score)
        assertEquals("🐈🥚🐟🐈🐈", result.grid.first())
    }

    // An article taken out of the day is shown as a black circle and not counted in the total.
    @Test
    fun `an article not shown is left out of the total`() {
        val result = CatfishingParser.parse("catfishing.net\n#148 - 3/9\n🐈🐟🐈⚫🐟\n🐈🐟🐟🐟🐟")!!
        assertEquals(3.0, result.score)
        assertEquals(9, result.total)
        assertEquals("🐈🐟🐈⚫🐟", result.grid.first())
    }

    @Test
    fun `the name is read however the link is written`() {
        val links =
            listOf(
                "catfishing.net",
                "<https://catfishing.net>",
                "https://catfishing.net",
                "catfishing dot net",
            )
        for (name in links) {
            val result = CatfishingParser.parse("$name\n#813 - 1/10\n🐟🐟🐟🐟🐟\n🐈🐟🐟🐟🐟")!!
            assertEquals("813", result.puzzleNumber)
            assertEquals(1.0, result.score)
        }
    }

    // In Markdown form the number has no hash and each line ends in two spaces.
    @Test
    fun `reads the Markdown form`() {
        val result = CatfishingParser.parse("catfishing.net  \n813 - 1/10  \n🐟🐟🐟🐟🐟  \n🐈🐟🐟🐟🐟  \n")!!
        assertEquals("813", result.puzzleNumber)
        assertEquals(1.0, result.score)
        assertEquals(listOf("🐟🐟🐟🐟🐟", "🐈🐟🐟🐟🐟"), result.grid)
    }

    // The board is drawn in a different set each season.
    @Test
    fun `a seasonal board is still a board`() {
        val halloween = CatfishingParser.parse("catfishing.net\n#495 - 1/10\n⚰️⚰️⚰️⚰️⚰️\n👻⚰️⚰️⚰️⚰️")!!
        assertEquals(listOf("⚰️⚰️⚰️⚰️⚰️", "👻⚰️⚰️⚰️⚰️"), halloween.grid)

        val christmas = CatfishingParser.parse("catfishing.net\n#550 - 3.5/10\n🎅🏻🥕❄️❄️🎅🏻\n❄️🎅🏻❄️❄️❄️")!!
        assertEquals(3.5, christmas.score)
        assertEquals(listOf("🎅🏻🥕❄️❄️🎅🏻", "❄️🎅🏻❄️❄️❄️"), christmas.grid)

        // One set uses a keycap digit for a miss, so a digit does not rule a line out.
        val eurovision = CatfishingParser.parse("catfishing.net\n#328 - 1/10\n0️⃣0️⃣0️⃣0️⃣0️⃣\n💃0️⃣0️⃣0️⃣0️⃣")!!
        assertEquals(listOf("0️⃣0️⃣0️⃣0️⃣0️⃣", "💃0️⃣0️⃣0️⃣0️⃣"), eurovision.grid)
    }

    // The articles missed can be listed under the board, in spoiler markers if asked for.
    @Test
    fun `a list of what was missed is not taken for the board`() {
        val result =
            CatfishingParser.parse(
                "catfishing.net\n#813 - 1/10\n🐟🐟🐟🐟🐟\n🐈🐟🐟🐟🐟\n\n" +
                    "Q1 🐟 Anglerfish\nQ2 🐟 Carp\nQ4 🐟 Pike\nSkipped Q3, 5, 7-10",
            )!!
        assertEquals(2, result.grid.size)
    }

    // A score of 8 or more gets a party popper after it.
    @Test
    fun `a bonus emoji after the score is not read as part of it`() {
        val perfect = CatfishingParser.parse("catfishing.net\n#814 - 10/10 🎉\n🐈🐈🐈🐈🐈\n🐈🐈🐈🐈🐈")!!
        assertEquals(10.0, perfect.score)
        assertEquals(10, perfect.total)

        val threshold = CatfishingParser.parse("catfishing.net\n#814 - 8/10 🎉\n🐈🐈🥚🐈🐈\n🐈🐈🥚🐈🐟")!!
        assertEquals(8.0, threshold.score)
    }

    // A day saved without its answers is shared as counts in place of a board.
    @Test
    fun `the score is read from a day without a board`() {
        val result = CatfishingParser.parse("catfishing.net\n#813 - 4.5/10\n🐈4 🥚1 🐟5")!!
        assertEquals(4.5, result.score)
        assertEquals(10, result.total)
    }

    @Test
    fun `matches only catfishing text`() {
        assertTrue(CatfishingParser.matches(played))
        assertFalse(CatfishingParser.matches("Wordle 1,234 4/6"))
        assertFalse(CatfishingParser.matches("catfishing is a good game"))
    }

    @Test
    fun `the shared stats are not a result`() {
        val stats =
            "catfishing.net stats\n📆 42 days played\n🔥 5 day streak\n🔢 6.3 average\n" +
                "🏆 9.5 best\n🐈 58% correct\n🥚 6% close enough"
        assertFalse(CatfishingParser.matches(stats))
    }
}
