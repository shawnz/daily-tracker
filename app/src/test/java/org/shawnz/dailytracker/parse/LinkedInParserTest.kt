package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LinkedInParserTest {
    // Copied from finished games.
    private val queens = "Queens #867 | 1:00 👑\nlnkd.in/queens."
    private val queensSlow = "Queens #873 | 15:05 👑\nlnkd.in/queens."
    private val zip = "Zip #546 | 0:21 🏁\nlnkd.in/zip."
    private val pinpoint = "Pinpoint #873\n🤔 🤔 🤔 🤔 📌 (5/5)\nlnkd.in/pinpoint."

    // Signed-in share text, from the site's own unit tests. There, the 🏅 line stands in for an
    // extra line from the results page.
    private val queensSignedIn = "Queens #2 | 0:50 and flawless\nFirst 👑s: 🟥 🟧 🟨\nlnkd.in/queens."
    private val tangoSignedIn =
        "Tango #2 | 0:50 with no mistakes & no hints\nFirst 3 placements:\n2️⃣🟨\n1️⃣3️⃣\n" +
            "🏅 test message\nlnkd.in/tango."
    private val zipSignedIn = "Zip #2 | 0:50 🏁\nWith 5 backtracks 🛑\n🏅 test message\nlnkd.in/zip."
    private val crossclimbSignedIn =
        "Crossclimb #2 | 0:50 with no mistakes & no hints\n" +
            "Fill order: 1️⃣ 2️⃣ 3️⃣ 4️⃣ 5️⃣ 🔼 🔽 🪜\nlnkd.in/crossclimb."
    private val miniSudokuSignedIn =
        "Mini Sudoku #2 | 0:50 with no mistakes & no hints ✏️\n🏅 test message\n" +
            "The classic game, made mini. Handcrafted by the originators of \"Sudoku.\"\n" +
            "lnkd.in/minisudoku."
    private val wendSignedIn = "Wend #2 | 0:50 🌀\nWith no hints & 1 backtrack\nlnkd.in/wend."
    private val patchesSignedIn =
        "Patches #2 | 0:50 🧶\nWith 2 redraws\n🏅 test message\nlnkd.in/patches."
    private val pinpointSignedIn =
        "Pinpoint #2 | 3 guesses\n1️⃣ | 13% match\n2️⃣ | 79% match\n3️⃣ | 100% match 📌\n" +
            "lnkd.in/pinpoint."
    private val pinpointSignedInFailed =
        "Pinpoint #2\n1️⃣ | 5% match\n2️⃣ | 13% match\n3️⃣ | 35% match\n4️⃣ | 79% match\n" +
            "5️⃣ | 90% match\nlnkd.in/pinpoint."

    @Test
    fun `reads the number and the time`() {
        val result = QueensParser.parse(queens)!!
        assertEquals("867", result.puzzleNumber)
        assertEquals(60, result.seconds)
        assertTrue(result.success)
    }

    // Copied from a signed-in game. The 🏅 line is the gold card that was in focus on the results page.
    @Test
    fun `a signed-in share with a results-page line is read`() {
        val result =
            QueensParser.parse(
                "Queens #878 | 2:41 with no hints\nFirst 👑s: \n🏅 I’m in the Top 75% of all players today!\nlnkd.in/queens.",
            )!!
        assertEquals("878", result.puzzleNumber)
        assertEquals(161, result.seconds)
        assertEquals(LocalDate.of(2026, 9, 25), result.day)
    }

    @Test
    fun `a time under a minute is read`() {
        val result = ZipParser.parse(zip)!!
        assertEquals("546", result.puzzleNumber)
        assertEquals(21, result.seconds)
    }

    @Test
    fun `a time over ten minutes is read`() {
        assertEquals(905, QueensParser.parse(queensSlow)!!.seconds)
    }

    // Past an hour, the time is written H:MM:SS.
    @Test
    fun `a time over an hour is read`() {
        assertEquals(
            3723,
            QueensParser.parse("Queens #873 | 1:02:03 👑\nlnkd.in/queens.")!!.seconds,
        )
    }

    // Each game's text starts with its own name.
    @Test
    fun `each game claims only its own text`() {
        assertTrue(QueensParser.matches(queens))
        assertFalse(ZipParser.matches(queens))
        assertFalse(TangoParser.matches(queens))

        assertTrue(ZipParser.matches(zip))
        assertFalse(QueensParser.matches(zip))
    }

    @Test
    fun `a name of two words is read`() {
        val result =
            MiniSudokuParser.parse(
                "Mini Sudoku #2 | 0:50 ✏️\n" +
                    "The classic game, made mini. Handcrafted by the originators of \"Sudoku.\"\n" +
                    "lnkd.in/minisudoku.",
            )!!
        assertEquals("2", result.puzzleNumber)
        assertEquals(50, result.seconds)
    }

    // Built from a guest play of puzzle #187 in 29 seconds on 2026-09-20.
    @Test
    fun `a guest result is read`() {
        val result = PatchesParser.parse("Patches #187 | 0:29 🧶\nlnkd.in/patches.")!!
        assertEquals(29, result.seconds)
        assertEquals(LocalDate.of(2026, 9, 20), result.day)
    }

    @Test
    fun `signed-in text is read for every timed game`() {
        val parsers =
            listOf(
                QueensParser to queensSignedIn,
                TangoParser to tangoSignedIn,
                ZipParser to zipSignedIn,
                CrossclimbParser to crossclimbSignedIn,
                MiniSudokuParser to miniSudokuSignedIn,
                WendParser to wendSignedIn,
                PatchesParser to patchesSignedIn,
            )
        for ((parser, text) in parsers) {
            val result = parser.parse(text)!!
            assertEquals("2", result.puzzleNumber, text)
            assertEquals(50, result.seconds, text)
        }
    }

    // Constructed. From #1,000 the number is written with the locale's digit grouping.
    @Test
    fun `a grouped number is read`() {
        val result =
            QueensParser.parse(
                "Queens #1,000 | 0:50 and flawless\nFirst 👑s: 🟥 🟧 🟨\nlnkd.in/queens.",
            )!!
        assertEquals("1000", result.puzzleNumber)
        assertEquals(LocalDate.of(2027, 1, 25), result.day)
    }

    // Puzzle #1 of Queens was 2024-05-01, so #867 is 2026-09-14.
    @Test
    fun `the day is worked out from the puzzle number`() {
        assertEquals(LocalDate.of(2026, 9, 14), QueensParser.parse(queens)!!.day)
        assertEquals(LocalDate.of(2026, 9, 20), QueensParser.parse(queensSlow)!!.day)
    }

    // Zip started later than Queens, so the same day has a lower number.
    @Test
    fun `each game counts from its own first day`() {
        assertEquals(LocalDate.of(2026, 9, 14), ZipParser.parse(zip)!!.day)
    }

    @Test
    fun `matches only linkedin text`() {
        assertFalse(QueensParser.matches("Wordle 1,234 4/6"))
        assertFalse(QueensParser.matches("Queens is a good game"))
    }

    // Pinpoint is scored by guesses, with no time at all.
    @Test
    fun `pinpoint reads the guess count`() {
        val result = PinpointParser.parse(pinpoint)!!
        assertEquals("873", result.puzzleNumber)
        assertEquals(5, result.guesses)
        assertEquals(5, result.total)
        assertTrue(result.success!!)
        assertEquals(LocalDate.of(2026, 9, 20), result.day)
    }

    // A failed play has (X/5).
    @Test
    fun `pinpoint reads a failure`() {
        val result =
            PinpointParser.parse(
                "Pinpoint #873\n🤔 🤔 🤔 🤔 🤔 (X/5)\nlnkd.in/pinpoint.",
            )!!
        assertNull(result.guesses)
        assertFalse(result.success!!)
    }

    @Test
    fun `pinpoint reads the signed-in guess count`() {
        val result = PinpointParser.parse(pinpointSignedIn)!!
        assertEquals("2", result.puzzleNumber)
        assertEquals(3, result.guesses)
        assertEquals(5, result.total)
        assertTrue(result.success!!)
    }

    // From the site's own unit tests.
    @Test
    fun `pinpoint reads a signed-in win with no mistakes`() {
        val result =
            PinpointParser.parse(
                "Pinpoint #2 | 1 guess with no mistakes\n1️⃣ | 100% match 📌\nlnkd.in/pinpoint.",
            )!!
        assertEquals(1, result.guesses)
        assertTrue(result.success!!)
    }

    // A signed-in failure doesn't have a pin.
    @Test
    fun `pinpoint reads a signed-in failure`() {
        val result = PinpointParser.parse(pinpointSignedInFailed)!!
        assertNull(result.guesses)
        assertFalse(result.success!!)
    }

    // Constructed. From #1,000 the number is written with the locale's digit grouping.
    @Test
    fun `pinpoint reads a grouped number`() {
        val result =
            PinpointParser.parse(
                "Pinpoint #1,000 | 3 guesses\n1️⃣ | 13% match\n2️⃣ | 79% match\n" +
                    "3️⃣ | 100% match 📌\nlnkd.in/pinpoint.",
            )!!
        assertEquals("1000", result.puzzleNumber)
        assertEquals(LocalDate.of(2027, 1, 25), result.day)
    }

    // The timed reader requires a time, which Pinpoint doesn't have.
    @Test
    fun `the timed reader does not claim pinpoint text`() {
        assertFalse(QueensParser.matches(pinpoint))
        assertFalse(QueensParser.matches(pinpointSignedIn))
        assertTrue(PinpointParser.matches(pinpoint))
    }
}
