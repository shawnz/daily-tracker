package org.shawnz.dailytracker.parse

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * A 4×3 result.
 *
 * @property points Null when the board ran out of guesses, where no score is printed. Negative
 *   for a [ruleBreaker], which still solves the board.
 * @property calledIt The hub word was named correctly before the first guess.
 * @property calledWrongHub The hub word was named wrongly before the first guess, which fails the
 *   board.
 * @property ruleBreaker The board was solved with the hub word last in every guess, which scores
 *   -100 points.
 * @property grid The guesses, one string per row.
 */
data class FourByThreeResult(
    val points: Int?,
    val mistakes: Int?,
    val calledIt: Boolean = false,
    val calledWrongHub: Boolean = false,
    val ruleBreaker: Boolean = false,
    override val day: LocalDate? = null,
    val grid: List<String> = emptyList(),
) : GameResult {
    override val success: Boolean get() = points != null && !calledWrongHub
}

/**
 * Reads the 4×3 share text.
 *
 *     September 14, 2026
 *     156 points • No mistakes
 *     🌟🟦🟦
 *     🌟🟨🟨
 *     🌟🟩🟩
 *     🌟🟪🟪
 *     https://4x3.fun
 *
 * The text doesn't name the game, so it is identified by the 4x3.fun link.
 *
 * The date is written in the device's own language. Only English dates are read, and the day
 * is left unknown otherwise.
 */
object FourByThreeParser : ResultParser<FourByThreeResult> {
    private val LINK = Regex("""4x3\.fun""", RegexOption.IGNORE_CASE)

    /** A rule breaker scores below zero, so the sign is part of the number. */
    private val POINTS = Regex("""(-?\d+)\s*points""", RegexOption.IGNORE_CASE)
    private val MISTAKES = Regex("""(\d+)\s*mistakes?""", RegexOption.IGNORE_CASE)
    private val NO_MISTAKES = Regex("""No mistakes""", RegexOption.IGNORE_CASE)
    private val LOST = Regex("""Out of guesses""", RegexOption.IGNORE_CASE)
    private val WRONG_HUB = Regex("""Called the Wrong Hub""", RegexOption.IGNORE_CASE)

    /** The month comes first in US English, and the day comes first in British English. */
    private val DATE =
        Regex(
            """^\s*([A-Z][a-z]+ \d{1,2}, \d{4}|\d{1,2} [A-Z][a-z]+ \d{4})\s*$""",
            RegexOption.MULTILINE,
        )

    private val DATE_FORMATS: List<DateTimeFormatter> =
        listOf("MMMM d, yyyy", "d MMMM yyyy").map {
            DateTimeFormatter.ofPattern(it, Locale.ENGLISH)
        }

    /** The four category colours, the star of the hub word, and the white of a blank. */
    private val TILES = setOf(0x1F7E6, 0x1F7E9, 0x1F7E8, 0x1F7EA, 0x1F31F, 0x2B1C)

    override fun matches(text: String): Boolean = LINK.containsMatchIn(text)

    override fun parse(text: String): FourByThreeResult? {
        if (!LINK.containsMatchIn(text)) return null
        return FourByThreeResult(
            points =
                if (LOST.containsMatchIn(text)) {
                    null
                } else {
                    POINTS
                        .find(text)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
                },
            mistakes =
                when {
                    NO_MISTAKES.containsMatchIn(text) -> {
                        0
                    }

                    else -> {
                        MISTAKES
                            .find(text)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull()
                    }
                },
            calledIt = text.contains("Called It", ignoreCase = true),
            calledWrongHub = WRONG_HUB.containsMatchIn(text),
            ruleBreaker = text.contains("RULE BREAKER", ignoreCase = true),
            day =
                DATE
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    ?.let(::readDay),
            grid =
                text
                    .lineSequence()
                    .map { it.trim() }
                    .filter { isGridRow(it, TILES) }
                    .toList(),
        )
    }

    private fun readDay(written: String): LocalDate? =
        DATE_FORMATS.firstNotNullOfOrNull { format ->
            try {
                LocalDate.parse(written, format)
            } catch (e: DateTimeParseException) {
                null
            }
        }
}
