package org.shawnz.dailytracker.parse

/**
 * A Catfishing result.
 *
 * A day is scored rather than won or lost, so [success] is always null.
 *
 * @property score A point for each article named and half a point for each close guess.
 * @property total How many articles were shown, which is not always ten.
 * @property grid The board, one string per row.
 */
data class CatfishingResult(
    override val puzzleNumber: String?,
    val score: Double?,
    val total: Int?,
    val grid: List<String> = emptyList(),
) : GameResult

/**
 * Reads the Catfishing share text.
 *
 *     catfishing.net
 *     #813 - 1/10
 *     🐟🐟🐟🐟🐟
 *     🐈🐟🐟🐟🐟
 *
 * The link can be delinked, put in angle brackets or written in full, and the missed articles
 * can be listed below the board. In Markdown form, the number has no `#` and each line ends in
 * two spaces.
 *
 * The board emoji change with the season, so a board row is any line of emoji.
 */
object CatfishingParser : ResultParser<CatfishingResult> {
    private val NAME = Regex("""catfishing""", RegexOption.IGNORE_CASE)

    /** The `#` is absent in Markdown form. A close guess is worth half, so the score can be `7.5`. */
    private val SCORE = Regex("""#?(\d+)\s*[-–—]\s*(\d+(?:\.\d+)?)\s*/\s*(\d+)""")

    override fun matches(text: String): Boolean = NAME.containsMatchIn(text) && SCORE.containsMatchIn(text)

    override fun parse(text: String): CatfishingResult? {
        val header = SCORE.find(text) ?: return null
        if (!NAME.containsMatchIn(text)) return null
        return CatfishingResult(
            puzzleNumber = header.groupValues[1],
            score = header.groupValues[2].toDoubleOrNull(),
            total = header.groupValues[3].toIntOrNull(),
            grid =
                text
                    .lineSequence()
                    .map { it.trim() }
                    .filter { isBoardRow(it) }
                    .toList(),
        )
    }

    /**
     * Letters, spaces, `#` and `/` appear in the link, the score and the missed articles, but not
     * in a board row. Digits are allowed, because one seasonal set uses a keycap digit for a miss.
     */
    private fun isBoardRow(line: String): Boolean =
        line.isNotEmpty() &&
            line.none { it.isLetter() || it.isWhitespace() || it == '#' || it == '/' } &&
            line.any { it.code > 0x7F }
}
