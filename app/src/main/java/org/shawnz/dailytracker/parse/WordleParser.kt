package org.shawnz.dailytracker.parse

import java.time.LocalDate

/**
 * A Wordle result.
 *
 * @property guesses Null when the puzzle was not solved.
 * @property grid The rows of coloured squares, in the order played.
 */
data class WordleResult(
    override val puzzleNumber: String?,
    val guesses: Int?,
    val hardMode: Boolean,
    val grid: List<String> = emptyList(),
) : GameResult {
    override val success: Boolean get() = guesses != null

    override val day: LocalDate?
        get() = dayFromNumber(puzzleNumber, LAUNCH_DAY, firstNumber = 0)
}

/** The day of puzzle #0. There is one puzzle a day. */
private val LAUNCH_DAY: LocalDate = LocalDate.of(2021, 6, 19)

/**
 * Reads the Wordle share text. The header has one of these forms:
 *
 *     Wordle 1,234 4/6
 *     Wordle 1,234 4/6*     hard mode
 *     Wordle 1,234 X/6      not solved
 *     Wordle 1,234 X/6*     not solved in hard mode
 *
 * The number is grouped in the player's locale, such as `1.234` in German. A puzzle played
 * from the archive has a line such as `Archive September 8, 2026` above the header. A blank
 * line and then the rows of coloured squares follow the header.
 */
object WordleParser : ResultParser<WordleResult> {
    private val HEADER =
        Regex(
            """Wordle\s+($GROUPED_NUMBER)\s+([1-6X])/6(\*?)""",
            RegexOption.IGNORE_CASE,
        )

    /** Black, white, yellow and green squares, and the orange and blue of high contrast. */
    private val TILES = setOf(0x2B1B, 0x2B1C, 0x1F7E8, 0x1F7E9, 0x1F7E7, 0x1F7E6)

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): WordleResult? {
        val m = HEADER.find(text) ?: return null
        val grade = m.groupValues[2].uppercase()
        return WordleResult(
            puzzleNumber = groupedNumberValue(m.groupValues[1])?.toString(),
            guesses = if (grade == "X") null else grade.toIntOrNull(),
            hardMode = m.groupValues[3] == "*",
            grid =
                text
                    .lineSequence()
                    .map { it.trim() }
                    .filter { isGridRow(it, TILES) }
                    .toList(),
        )
    }
}
