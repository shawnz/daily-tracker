package org.shawnz.dailytracker.parse

import java.time.LocalDate

/**
 * A MapTap daily result.
 *
 * A day is scored rather than won or lost, so [success] is always null. The puzzles have no
 * number, so [puzzleNumber] is always null.
 *
 * @property total The day's final score, out of 1,000. Rounds 3, 4 and 5 have weights of 2, 3
 *   and 3 in it.
 * @property rounds Each round's score out of 100, in the order played.
 */
data class MapTapResult(
    val total: Int?,
    val rounds: List<Int> = emptyList(),
    override val day: LocalDate? = null,
) : GameResult

/**
 * Reads the MapTap daily share text.
 *
 *     www.maptap.gg November 4
 *     94🏅 99🔥 95🏅 90👑 80🌞
 *     Final score: 893
 *
 * The header starts with `https://maptap.gg` in the Discord format, and with `MapTap` when the
 * link is hidden. With the flags setting, each round is a flag and then the score, as in `🇺🇸83`.
 *
 * Player maps and Best Of maps have a title in place of the date and aren't read, so each day
 * has one result.
 *
 * The date has no year, so the year comes from [monthAndDayValue]. A replay of an earlier day
 * has that day's date. Its month is written as in the page's URL, so the case can differ.
 */
object MapTapParser : ResultParser<MapTapResult> {
    private const val MONTH_NAME =
        "January|February|March|April|May|June|July|August|September|October|November|December"

    private val HEADER =
        Regex(
            """(?:www\.maptap\.gg|https://maptap\.gg|MapTap)[ \t]+($MONTH_NAME)[ \t]+(\d{1,2})\b""",
            RegexOption.IGNORE_CASE,
        )

    private val TOTAL = Regex("""Final score:\s*(\d+)""", RegexOption.IGNORE_CASE)

    private val ROUND = Regex("""\d+""")

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): MapTapResult? = parse(text, LocalDate.now())

    /** Parses [text] as if read on [today]. */
    fun parse(
        text: String,
        today: LocalDate,
    ): MapTapResult? {
        val header = HEADER.find(text) ?: return null
        val total = TOTAL.find(text, header.range.last + 1)
        val roundText = text.substring(header.range.last + 1, total?.range?.first ?: text.length)
        val month = header.groupValues[1].lowercase().replaceFirstChar { it.uppercase() }
        return MapTapResult(
            total = total?.groupValues?.get(1)?.toIntOrNull(),
            rounds = ROUND.findAll(roundText).mapNotNull { it.value.toIntOrNull() }.toList(),
            day = monthAndDayValue("$month ${header.groupValues[2]}", today),
        )
    }
}
