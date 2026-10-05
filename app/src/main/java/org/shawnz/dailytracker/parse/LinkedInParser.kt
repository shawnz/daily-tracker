package org.shawnz.dailytracker.parse

import java.time.LocalDate

/**
 * A LinkedIn game result.
 *
 * @property seconds The time the game took.
 */
data class LinkedInResult(
    override val puzzleNumber: String?,
    val seconds: Int?,
    override val day: LocalDate? = null,
) : GameResult {
    /** Share text exists only for a finished puzzle. */
    override val success: Boolean get() = true
}

/**
 * Reads the share text of one timed LinkedIn game.
 *
 *     Queens #867 | 1:00 👑
 *     lnkd.in/queens.
 *
 * Signed in, the share text can have a note after the time and more lines before the link:
 *
 *     Queens #2 | 0:50 and flawless
 *     First 👑s: 🟥 🟧 🟨
 *     lnkd.in/queens.
 *
 * Every timed game starts with the name, the number and the time, so they are all parsed by this
 * class. Pinpoint is scored by guesses rather than time and has [PinpointParser].
 *
 * The number is the puzzle's place in the run, counting from [firstDay]. From #1,000 it is written
 * with the locale's digit grouping.
 */
class LinkedInParser(
    game: String,
    private val firstDay: LocalDate,
) : ResultParser<LinkedInResult> {
    // A time over an hour is written H:MM:SS.
    private val header =
        Regex(
            """${Regex.escape(game)}\s*#($GROUPED_NUMBER)\s*\|\s*(?:(\d+):)?(\d+):(\d{2})""",
            RegexOption.IGNORE_CASE,
        )

    override fun matches(text: String): Boolean = header.containsMatchIn(text)

    override fun parse(text: String): LinkedInResult? {
        val found = header.find(text) ?: return null
        val hours = found.groupValues[2].toIntOrNull() ?: 0
        val minutes = found.groupValues[3].toIntOrNull()
        val seconds = found.groupValues[4].toIntOrNull()
        val number = groupedNumberValue(found.groupValues[1])?.toString()
        return LinkedInResult(
            puzzleNumber = number,
            seconds =
                if (minutes == null || seconds == null) {
                    null
                } else {
                    hours * 3600 + minutes * 60 + seconds
                },
            day = dayFromNumber(number, firstDay),
        )
    }
}

// Puzzle #1 of each game, worked back from the puzzle IDs shown for one day.
private val LAUNCH_2024_05_01: LocalDate = LocalDate.of(2024, 5, 1)

// Internal name QUEENSV2.
val QueensParser = LinkedInParser("Queens", LAUNCH_2024_05_01)

// Internal name LOTKA.
val TangoParser = LinkedInParser("Tango", LocalDate.of(2024, 10, 8))

// Internal name TRAIL.
val ZipParser = LinkedInParser("Zip", LocalDate.of(2025, 3, 18))

val CrossclimbParser = LinkedInParser("Crossclimb", LAUNCH_2024_05_01)
val MiniSudokuParser = LinkedInParser("Mini Sudoku", LocalDate.of(2025, 8, 12))
val WendParser = LinkedInParser("Wend", LocalDate.of(2026, 6, 9))
val PatchesParser = LinkedInParser("Patches", LocalDate.of(2026, 3, 18))
