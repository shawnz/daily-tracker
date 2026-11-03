package org.shawnz.dailytracker.parse

import java.time.LocalDate

/**
 * A TimeGuessr daily result.
 *
 * A day is scored rather than won or lost, so [success] is always null.
 *
 * @property total The day's total score, out of 50,000.
 * @property rounds Each round's score, in the order played. Empty for the emoji format, which
 *   has no round scores.
 */
data class TimeGuessrResult(
    override val puzzleNumber: String,
    val total: Int?,
    val rounds: List<Int> = emptyList(),
) : GameResult {
    override val day: LocalDate?
        get() = dayFromNumber(puzzleNumber, FIRST_DAY)
}

/** The day of daily #1. There is one daily a day. */
private val FIRST_DAY: LocalDate = LocalDate.of(2023, 6, 1)

/**
 * Reads the TimeGuessr daily share text, in the detailed or the emoji format.
 *
 *     TimeGuessr #1251 — 38,283/50,000
 *
 *     1️⃣ 🏆2,953 · 📅 10y · 🌍 6404.0mi
 *     2️⃣ 🏆9,944 · 📅 0y · 🌍 2.7mi
 *     3️⃣ 🏆9,904 · 📅 0y · 🌍 6.4mi
 *     4️⃣ 🏆5,781 · 📅 26y · 🌍 6.5mi
 *     5️⃣ 🏆9,701 · 📅 3y · 🌍 442ft
 *
 *     https://timeguessr.com
 *
 *     TimeGuessr #1251 38,283/50,000
 *     🌎⬛⬛⬛ 📅🟩⬛⬛
 *     🌎🟩🟩🟨 📅🟩🟩🟩
 *     🌎🟩🟩🟨 📅🟩🟩🟩
 *     🌎🟩🟩🟨 📅⬛⬛⬛
 *     🌎🟩🟩🟨 📅🟩🟩⬛
 *     https://timeguessr.com
 *
 * Text from single-player and custom games has no number and isn't read, so each day has one
 * result.
 *
 * The total is grouped in English. A round score is grouped for the player's region.
 */
object TimeGuessrParser : ResultParser<TimeGuessrResult> {
    private val HEADER =
        Regex("""TimeGuessr\s*#(\d+)\s*(?:—\s*)?(?:($GROUPED_NUMBER)\s*/\s*50,000)?""", RegexOption.IGNORE_CASE)

    private val ROUND = Regex("""🏆\s*($GROUPED_NUMBER)""")

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): TimeGuessrResult? {
        val header = HEADER.find(text) ?: return null
        return TimeGuessrResult(
            puzzleNumber = header.groupValues[1],
            total = header.groupValues[2].ifEmpty { null }?.let(::groupedNumberValue),
            rounds =
                ROUND
                    .findAll(text)
                    .mapNotNull { groupedNumberValue(it.groupValues[1]) }
                    .toList(),
        )
    }
}
