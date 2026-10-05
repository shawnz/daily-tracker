package org.shawnz.dailytracker.parse

import java.time.LocalDate

/** The day of puzzle #1. There is one puzzle a day. */
private val FIRST_DAY: LocalDate = LocalDate.of(2023, 6, 12)

/** The board ends at the fourth mistake. */
private const val MAX_MISTAKES = 4

/**
 * A Connections result.
 *
 * The text doesn't state a win or loss, so the board is read instead. A row of one colour is a
 * group solved and a row of several is a mistake.
 *
 * @property grid The guesses, one string per row, in the order they were made.
 */
data class ConnectionsResult(
    override val puzzleNumber: String?,
    val grid: List<String> = emptyList(),
) : GameResult {
    val groupsSolved: Int get() = grid.count { isOneColour(it) }

    val mistakes: Int get() = grid.count { !isOneColour(it) }

    override val success: Boolean get() = mistakes < MAX_MISTAKES

    override val day: LocalDate?
        get() = dayFromNumber(puzzleNumber, FIRST_DAY)
}

/**
 * Reads the Connections share text.
 *
 *     Connections
 *     Puzzle #1191
 *     🟦🟦🟦🟦
 *     🟩🟩🟩🟩
 *     🟨🟨🟨🟨
 *     🟪🟪🟪🟪
 *
 * A puzzle played from the archive has `Archive` and its date on the first line, and the name
 * and number on one line below it. A bonus puzzle doesn't have a puzzle number and isn't read.
 */
object ConnectionsParser : ResultParser<ConnectionsResult> {
    private val NAME = Regex("""Connections""", RegexOption.IGNORE_CASE)
    private val NUMBER = Regex("""Puzzle\s*#\s*(\d+)""", RegexOption.IGNORE_CASE)

    /** A board can use more categories than the usual four, so the rarer colours are here. */
    private val TILES = setOf(0x1F7E8, 0x1F7E9, 0x1F7E6, 0x1F7EA, 0x1F7E5, 0x1F7E7)

    override fun matches(text: String): Boolean = NAME.containsMatchIn(text) && NUMBER.containsMatchIn(text)

    override fun parse(text: String): ConnectionsResult? {
        if (!matches(text)) return null
        return ConnectionsResult(
            puzzleNumber = NUMBER.find(text)?.groupValues?.get(1),
            grid =
                text
                    .lineSequence()
                    .map { it.trim() }
                    .filter { isGridRow(it, TILES) }
                    .toList(),
        )
    }
}

/** Whether every tile in the row is the same, which is what a solved group looks like. */
private fun isOneColour(row: String): Boolean {
    val points = row.codePoints().toArray()
    return points.isNotEmpty() && points.all { it == points[0] }
}
