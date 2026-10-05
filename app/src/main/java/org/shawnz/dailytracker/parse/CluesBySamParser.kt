package org.shawnz.dailytracker.parse

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

// The board has one of these tiles for each person.
private const val CORRECT = 0x1F7E9
private const val MISTAKE = 0x1F7E8
private const val HINT = 0x1F7E1
private const val DOUBLE_HINT = 0x1F7E0

/**
 * A Clues by Sam result.
 *
 * @property seconds The time taken. Null above six minutes, where the share text has a phrase
 *   such as `less than 7 minutes` in place of the time.
 * @property difficulty The game's own word for the board, such as Hard.
 * @property percentile How the time ranked, in the game's own words, such as `top 5%`. Null when
 *   the text doesn't include it.
 * @property grid The board, one string per row.
 */
data class CluesBySamResult(
    val seconds: Int?,
    val difficulty: String?,
    override val day: LocalDate?,
    val percentile: String? = null,
    val grid: List<String> = emptyList(),
) : GameResult {
    /** A wrong answer is not accepted, so a board that was shared was solved. */
    override val success: Boolean get() = true

    /** People identified with no mistake and no hint. */
    val correct: Int get() = count(CORRECT)

    /** People identified after at least one mistake and without a hint. */
    val mistakes: Int get() = count(MISTAKE)

    /** People identified with a hint. */
    val hints: Int get() = count(HINT)

    /** People identified with a double hint. */
    val doubleHints: Int get() = count(DOUBLE_HINT)

    private fun count(tile: Int): Int = countTiles(grid, tile)
}

/**
 * Reads the Clues by Sam share text, which comes in two forms.
 *
 *     I solved the daily #CluesBySam, Sep 25th 2026 (Hard), in 03:08
 *     🟩🟩🟩🟩
 *     🟩🟩🟩🟩
 *     🟩🟩🟩🟩
 *     🟩🟩🟩🟩
 *     🟩🟩🟩🟩
 *     https://cluesbysam.com
 *
 *     #CluesBySam - Sep 25th 2026 (Hard)
 *     03:08
 *     🟩🟩🟩🟩
 *     🟩🟩🟩🟩
 *     🟩🟩🟩🟩
 *     🟩🟩🟩🟩
 *     🟩🟩🟩🟩
 *
 * Both have the same date, difficulty and time, so both are read the same way.
 */
object CluesBySamParser : ResultParser<CluesBySamResult> {
    private val HEADER = Regex("""#CluesBySam|cluesbysam\.com""", RegexOption.IGNORE_CASE)
    private val DATE = Regex("""([A-Z][a-z]{2})\s+(\d{1,2})(?:st|nd|rd|th)\s+(\d{4})""")
    private val TIME = Regex("""\b(\d+):(\d{2})\b""")
    private val DIFFICULTY = Regex("""\(([^)]{1,20})\)""")
    private val PERCENTILE = Regex("""Percentile:\s*([^\n]+?)\s*$""", RegexOption.MULTILINE)

    private val DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH)

    private val TILES = setOf(CORRECT, MISTAKE, HINT, DOUBLE_HINT)

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): CluesBySamResult? {
        if (!HEADER.containsMatchIn(text)) return null
        val time = TIME.find(text)
        return CluesBySamResult(
            seconds =
                time?.let {
                    val minutes = it.groupValues[1].toIntOrNull() ?: return@let null
                    val seconds = it.groupValues[2].toIntOrNull() ?: return@let null
                    minutes * 60 + seconds
                },
            difficulty =
                DIFFICULTY
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    ?.trim(),
            day = DATE.find(text)?.let { readDay(it.groupValues) },
            percentile =
                PERCENTILE
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    ?.trim(),
            grid =
                text
                    .lineSequence()
                    .map { it.trim() }
                    .filter { isGridRow(it, TILES) }
                    .toList(),
        )
    }

    private fun readDay(parts: List<String>): LocalDate? =
        try {
            LocalDate.parse("${parts[1]} ${parts[2]} ${parts[3]}", DATE_FORMAT)
        } catch (e: DateTimeParseException) {
            null
        }
}
