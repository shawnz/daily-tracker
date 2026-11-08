package org.shawnz.dailytracker.parse

/**
 * A LinkedIn game result.
 *
 * @property seconds The time the game took.
 */
data class LinkedInResult(
    val number: Int,
    val seconds: Int?,
) : GameResult {
    override val puzzle: PuzzleIdentity get() = PuzzleIdentity.DailyNumber(number)
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
 * From #1,000 the number is written with the locale's digit grouping.
 */
class LinkedInParser(
    game: String,
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
        val number = groupedNumberValue(found.groupValues[1]) ?: return null
        val hours = found.groupValues[2].toIntOrNull() ?: 0
        val minutes = found.groupValues[3].toIntOrNull()
        val seconds = found.groupValues[4].toIntOrNull()
        return LinkedInResult(
            number = number,
            seconds =
                if (minutes == null || seconds == null) {
                    null
                } else {
                    hours * 3600 + minutes * 60 + seconds
                },
        )
    }
}

// Internal name QUEENSV2.
val QueensParser = LinkedInParser("Queens")

// Internal name LOTKA.
val TangoParser = LinkedInParser("Tango")

// Internal name TRAIL.
val ZipParser = LinkedInParser("Zip")

val CrossclimbParser = LinkedInParser("Crossclimb")
val MiniSudokuParser = LinkedInParser("Mini Sudoku")
val WendParser = LinkedInParser("Wend")
val PatchesParser = LinkedInParser("Patches")
