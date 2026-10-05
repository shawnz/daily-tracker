package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

// The lines are joined in code so the trailing spaces in the share text are kept.
class BandleParserTest {
    // Copied from a finished game.
    private val named =
        listOf(
            "Bandle #1489 3/6",
            "🟥🟨🟩⬜⬜⬜",
            "Found: 1/1 (100%)",
            "Current Streak: 1 (max 1)",
            "#Bandle ",
            "https://bandle.app",
        ).joinToString("\n")

    private val withBonus =
        listOf(
            "Bandle #1191 3/6",
            "🟥🟨🟩⬜⬜⬜",
            "Found: 45/50 (90%)",
            "Current Streak: 5 (max 12)",
            "Bonus Rounds: 3/5 🖼️  🌍  🎸  ",
            "#Bandle ",
            "https://bandle.app",
        ).joinToString("\n")

    @Test
    fun `reads the number, the guess it was named on and the guesses available`() {
        val result = BandleParser.parse(named)!!
        assertEquals("1489", result.puzzleNumber)
        assertEquals(3, result.step)
        assertEquals(6, result.instruments)
        assertTrue(result.success)
    }

    // 🟥 wrong, 🟨 the right artist and the wrong song, 🟩 correct, ⬛ skipped, ⬜ never reached.
    @Test
    fun `keeps an emoji for every guess slot`() {
        assertEquals("🟥🟨🟩⬜⬜⬜", BandleParser.parse(named)!!.emoji)
    }

    // A turn taken without a guess is skipped, which is shown differently from a wrong guess.
    private val skipped =
        listOf(
            "Bandle #1489 6/6",
            "⬛⬛⬛⬛⬛🟩",
            "Found: 1/1 (100%)",
            "Current Streak: 1 (max 1)",
            "#Bandle ",
            "https://bandle.app",
        ).joinToString("\n")

    @Test
    fun `a skipped turn is kept apart from a wrong guess`() {
        val result = BandleParser.parse(skipped)!!
        assertEquals("⬛⬛⬛⬛⬛🟩", result.emoji)
        assertEquals(6, result.step)
    }

    // The last guess is still a win. A song never named has x, never the guess count.
    @Test
    fun `naming the song on the last guess is a win`() {
        val result = BandleParser.parse(skipped)!!
        assertEquals(result.instruments, result.step)
        assertTrue(result.success)
    }

    // A song never named has x where the guess would be, and every slot is filled.
    @Test
    fun `a song never named is a loss with no step`() {
        val result =
            BandleParser.parse(
                listOf(
                    "Bandle #1192 x/6",
                    "🟨⬛🟥🟥⬛🟨",
                    "Found: 45/51 (88.2%)",
                    "Current Streak: 0 (max 12)",
                    "#Bandle ",
                    "https://bandle.app",
                ).joinToString("\n"),
            )!!
        assertNull(result.step)
        assertEquals(6, result.instruments)
        assertEquals("🟨⬛🟥🟥⬛🟨", result.emoji)
        assertFalse(result.success)
    }

    // Copied from a finished game from 2024. More hashtags can follow #Bandle, and older share text
    // has a blank line and a trailing slash on the link.
    @Test
    fun `a decimal percentage and more hashtags do not stop it`() {
        val result = BandleParser.parse(OLDER)!!
        assertEquals("733", result.puzzleNumber)
        assertEquals(1, result.step)
        assertEquals("🟩⬜⬜⬜⬜⬜", result.emoji)
    }

    @Test
    fun `counts the bonus rounds`() {
        val result = BandleParser.parse(withBonus)!!
        assertEquals(3, result.bonusWon)
        assertEquals(5, result.bonusAsked)
    }

    // The real game above doesn't have a bonus line, so this is the usual case.
    @Test
    fun `a game with no bonus rounds leaves them unknown`() {
        val result = BandleParser.parse(named)!!
        assertNull(result.bonusWon)
        assertNull(result.bonusAsked)
    }

    // The labels are in the player's language, so the counts are found by shape and the bonus
    // line by its own emoji.
    @Test
    fun `labels in another language do not stop it`() {
        val result =
            BandleParser.parse(
                listOf(
                    "Bandle #1191 3/6",
                    "🟥🟨🟩⬜⬜⬜",
                    "Trouvées: 45/50 (90%)",
                    "Votre flamme: 5 (max 12)",
                    "Manches Bonus: 3/5 🖼️  🌍  🎸  ",
                    "#Bandle ",
                    "https://bandle.app",
                ).joinToString("\n"),
            )!!
        assertEquals(3, result.step)
        assertEquals("🟥🟨🟩⬜⬜⬜", result.emoji)
        assertEquals(3, result.bonusWon)
        assertEquals(5, result.bonusAsked)
    }

    // The line runs to one emoji per guess the song allowed, padded past the winning guess.
    @Test
    fun `there is an emoji for every guess the song allowed`() {
        val result = BandleParser.parse(named)!!
        assertEquals(result.instruments, result.emoji.codePointCount(0, result.emoji.length))
    }

    @Test
    fun `matches only bandle text`() {
        assertTrue(BandleParser.matches(named))
        assertFalse(BandleParser.matches("Wordle 1,234 4/6"))
        assertFalse(BandleParser.matches("#Bandle on its own"))
    }

    // Copied from a finished game from 2025, with a different streak label and a second link.
    @Test
    fun `older labels and links do not stop it`() {
        val result =
            BandleParser.parse(
                "Bandle #959 2/6\n⬛🟩⬜⬜⬜⬜\nFound: 183/226 (81%)\nCurrent Daily Streak: 6 (max 20)\n" +
                    "#Bandle #Heardle \nhttps://bandle.app\nhttps://bandle.app/daily",
            )!!
        assertEquals("959", result.puzzleNumber)
        assertEquals(2, result.step)
        assertEquals("⬛🟩⬜⬜⬜⬜", result.emoji)
    }

    companion object {
        private const val OLDER =
            "Bandle #733 1/6\n🟩⬜⬜⬜⬜⬜\nFound: 7/11 (63.6%)\nCurrent Streak: 1 (max 2)\n" +
                "#Bandle #Heardle #Wordle \n\nhttps://bandle.app/"
    }
}
