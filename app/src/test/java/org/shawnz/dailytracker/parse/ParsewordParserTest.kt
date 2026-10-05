package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ParsewordParserTest {
    // Copied from a finished puzzle.
    private val played =
        """
        Parseword #229
        ⏱️ 1m39s
        ⭐️ No Assists
        🥚 Learn Mode
        """.trimIndent()

    // Copied from the same puzzle played again in another browser, with its secret found. A replay
    // in the same browser shares the first result again.
    private val perfect =
        """
        Parseword #229
        ⚡️ 41s
        💎 Perfect
        🎭 Secret Found
        🥚 Learn Mode
        """.trimIndent()

    private val eggHunt =
        """
        Parseword "egg-hunt/jw-egg-hunt-1"
        Egg Hunt
        ⏱️ 3m12s
        🥃 🧙‍♀️
        """.trimIndent()

    // Copied from a finished game from before 2026-05-18.
    private val olderNoHints =
        """
        Parseword #41
        ⚡️ 23s
        ⭐️ No Hints
        🐰 1 Easter Egg Found
        🥚 Learn Mode
        """.trimIndent()

    // Copied from a finished game.
    private val today =
        """
        Parseword #240
        ⚡️ 50s
        💎 Perfect
        🎭 Secret Found
        🥚 Learn Mode
        """.trimIndent()

    // Copied from a finished game.
    private val fast =
        """
        Parseword #68
        ⚡️ 24s
        💎 Perfect
        🥚 Learn Mode
        """.trimIndent()

    @Test
    fun `reads a perfect run with a secret found`() {
        val result = ParsewordParser.parse(perfect)!!
        assertEquals("229", result.puzzleNumber)
        assertEquals(41, result.seconds)
        assertTrue(result.perfect)
        assertEquals(0, result.assists)
        // "Secret" is singular for one, and the emoji are listed instead of a number.
        assertEquals(1, result.secretsFound)
        assertTrue(result.learnMode)
        assertTrue(result.underAMinute)
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
    }

    @Test
    fun `reads a played puzzle`() {
        val result = ParsewordParser.parse(played)!!
        assertEquals("229", result.puzzleNumber)
        assertEquals(99, result.seconds)
        assertEquals(0, result.assists)
        assertFalse(result.perfect)
        assertTrue(result.learnMode)
        // This has a stopwatch and not a bolt, which means a time over a minute.
        assertFalse(result.underAMinute)
    }

    // Puzzle one was published on 2026-01-29.
    @Test
    fun `a puzzle number is the day that puzzle was published`() {
        assertEquals(LocalDate.of(2026, 9, 14), ParsewordParser.parse(played)!!.day)
        assertEquals(
            LocalDate.of(2026, 1, 29),
            ParsewordParser.parse("Parseword #1\n💎 Perfect")!!.day,
        )
    }

    @Test
    fun `a puzzle outside the daily run has its id in quotes and has no day`() {
        val result = ParsewordParser.parse(eggHunt)!!
        assertEquals("egg-hunt/jw-egg-hunt-1", result.puzzleNumber)
        assertNull(result.day)
    }

    @Test
    fun `reads the puzzle number, the time and a perfect run`() {
        val result = ParsewordParser.parse(today)!!
        assertEquals("240", result.puzzleNumber)
        assertEquals(50, result.seconds)
        assertTrue(result.perfect)
        assertEquals(0, result.assists)
    }

    @Test
    fun `a time below a minute has no minute part`() {
        assertEquals(24, ParsewordParser.parse(fast)!!.seconds)
    }

    @Test
    fun `assists are counted`() {
        val result = ParsewordParser.parse("Parseword #214\n⏱️ 2m5s\n✅ 3 Assists")!!
        assertEquals(3, result.assists)
        assertFalse(result.perfect)
        assertEquals(125, result.seconds)
    }

    // Copied from a finished game.
    @Test
    fun `one assist is singular`() {
        val result = ParsewordParser.parse("Parseword #240\n⚡️ 19s\n✅ 1 Assist\n🥚 Learn Mode")!!
        assertEquals(1, result.assists)
        assertFalse(result.perfect)
        assertEquals(19, result.seconds)
    }

    // Copied from a finished game.
    @Test
    fun `a run with no assists counts zero`() {
        val result = ParsewordParser.parse("Parseword #240\n⚡️ 40s\n⭐️ No Assists\n🥚 Learn Mode")!!
        assertEquals(0, result.assists)
        assertFalse(result.perfect)
    }

    // Before 2026-05-18 the share text said "Hints" instead of "Assists". The first text is copied
    // from a finished game.
    @Test
    fun `older text with hints is read as assists`() {
        val result = ParsewordParser.parse("Parseword #68\n⏱️ 2m54s\n✅ 1 Hint\n🏹 Secret Found")!!
        assertEquals(1, result.assists)
        assertEquals(174, result.seconds)
        assertEquals(1, result.secretsFound)
        assertEquals(LocalDate.of(2026, 4, 6), result.day)
        assertEquals(0, ParsewordParser.parse(olderNoHints)!!.assists)
    }

    @Test
    fun `text with no time leaves the seconds unknown`() {
        assertNull(ParsewordParser.parse("Parseword #216\n💎 Perfect")!!.seconds)
    }

    @Test
    fun `an egg hunt counts the eggs found and has no assists`() {
        val result = ParsewordParser.parse(eggHunt)!!
        assertTrue(result.eggHunt)
        assertEquals(192, result.seconds)
        assertEquals(2, result.secretsFound)
        assertNull(result.assists)
    }

    @Test
    fun `secrets are counted by their emoji`() {
        val result =
            ParsewordParser.parse("Parseword #221\n⏱️ 2m40s\n💎 Perfect\n🐣 🐤 Secrets Found")!!
        assertEquals(2, result.secretsFound)
    }

    // A secret with no emoji of its own is shown as a rabbit.
    @Test
    fun `a rabbit secret is counted`() {
        val result =
            ParsewordParser.parse("Parseword #231\n⏱️ 1m12s\n⭐️ No Assists\n🐰 Secret Found")!!
        assertEquals(1, result.secretsFound)
    }

    // A result saved before the egg emoji were kept has a count in their place.
    @Test
    fun `easter eggs in older text are counted`() {
        assertEquals(1, ParsewordParser.parse(olderNoHints)!!.easterEggs)
    }

    @Test
    fun `the two named difficulties are read, and the default is neither`() {
        assertTrue(ParsewordParser.parse(fast)!!.learnMode)
        val challenge =
            ParsewordParser.parse("Parseword #240\n⚡️ 11s\n💎 Perfect\n🎭 Secret Found\n🗿 Challenge Mode")!!
        assertTrue(challenge.challengeMode)
        assertFalse(challenge.learnMode)

        val default = ParsewordParser.parse("Parseword #225\n⏱️ 1m15s\n💎 Perfect")!!
        assertFalse(default.learnMode)
        assertFalse(default.challengeMode)
    }

    // The lightning bolt means a time of a minute or less.
    @Test
    fun `a minute or less counts as under a minute`() {
        assertTrue(ParsewordParser.parse(fast)!!.underAMinute)
        assertTrue(ParsewordParser.parse("Parseword #227\n⚡️ 1m0s\n💎 Perfect")!!.underAMinute)
        assertFalse(
            ParsewordParser.parse("Parseword #228\n⏱️ 1m23s\n⭐️ No Assists")!!.underAMinute,
        )
    }

    @Test
    fun `matches only parseword text`() {
        assertTrue(ParsewordParser.matches(today))
        assertFalse(ParsewordParser.matches("Wordle 1,234 4/6"))
    }
}
