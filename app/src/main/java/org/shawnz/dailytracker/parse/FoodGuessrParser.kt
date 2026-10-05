package org.shawnz.dailytracker.parse

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * A FoodGuessr result.
 *
 * A day is scored rather than won or lost, so [success] is always null.
 *
 * @property total The day's total score.
 * @property rounds Each round's score, in the order played.
 * @property perfectRounds The number of rounds marked with 💯.
 */
data class FoodGuessrResult(
    val total: Int?,
    val rounds: List<Int> = emptyList(),
    val perfectRounds: Int = 0,
    override val day: LocalDate? = null,
) : GameResult

/**
 * Reads the FoodGuessr share text, in the Friendly or the Compact format.
 *
 *     I got 8,791 on the FoodGuessr Daily!
 *
 *     🌕🌖🌑🌑🌑 1,718 (Round 1)
 *     🌕🌕🌕🌕🌕 5,000 (Round 2) 💯
 *     🌕🌕🌘🌑🌑 2,073 (Round 3)
 *
 *     Monday, Sep 14, 2026
 *     Play here: https://www.foodguessr.com/
 *
 *     FoodGuessr - Monday, Sep 14, 2026 UTC
 *     🌕🌖🌑🌑🌑 1,718 ⋅ Round 1
 *     🌕🌕🌕🌕🌕 5,000 ⋅ Round 2 💯
 *     🌕🌕🌘🌑🌑 2,073 ⋅ Round 3
 *     Total score: 8,791/15,000
 *     Play here: https://www.foodguessr.com/
 *
 * A run above the day's average adds a number that is not a round score, so a score is read only
 * where its round is named beside it. A Daily Double isn't read, so each day has one result.
 *
 * Scores are grouped for the player's region. The date is in American English and UTC whatever
 * the player's language.
 */
object FoodGuessrParser : ResultParser<FoodGuessrResult> {
    /** Plate-Off, Cuisine Match and Daily Double shares have the name too, but not followed by this. */
    private val NAME = Regex("""FoodGuessr(?: Daily(?! Double)| - )""", RegexOption.IGNORE_CASE)

    private val TOTAL =
        Regex(
            """(?:I got\s+($GROUPED_NUMBER)\s+on the|Total score:\s*($GROUPED_NUMBER)\s*/)""",
            RegexOption.IGNORE_CASE,
        )

    /** A round is its moons, then its score, then which round it was. */
    private val ROUND =
        Regex(
            """($GROUPED_NUMBER)\s*(?:\(\s*Round\s+\d+\s*\)|⋅\s*Round\s+\d+)""",
            RegexOption.IGNORE_CASE,
        )

    private val DATE = Regex("""([A-Z][a-z]{2})\w*\s+(\d{1,2}),\s*(\d{4})""")

    private val DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d yyyy", Locale.ENGLISH)

    override fun matches(text: String): Boolean = NAME.containsMatchIn(text)

    override fun parse(text: String): FoodGuessrResult? {
        if (!NAME.containsMatchIn(text)) return null
        return FoodGuessrResult(
            total =
                TOTAL.find(text)?.let { match ->
                    groupedNumberValue(match.groupValues[1].ifEmpty { match.groupValues[2] })
                },
            rounds =
                ROUND
                    .findAll(text)
                    .mapNotNull { groupedNumberValue(it.groupValues[1]) }
                    .toList(),
            perfectRounds = Regex("💯").findAll(text).count(),
            day = DATE.find(text)?.let { readDay(it.groupValues) },
        )
    }

    private fun readDay(parts: List<String>): LocalDate? =
        try {
            LocalDate.parse("${parts[1]} ${parts[2]} ${parts[3]}", DATE_FORMAT)
        } catch (e: DateTimeParseException) {
            null
        }
}
