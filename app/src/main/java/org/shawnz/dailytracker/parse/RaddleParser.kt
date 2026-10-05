package org.shawnz.dailytracker.parse

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

private const val CLEAN = 0x1F7E2
private const val HINTED = 0x1F4A1
private const val REVEALED = 0x1F441

/**
 * A Raddle result.
 *
 * @property from The word the ladder starts from.
 * @property to The word the ladder runs to.
 * @property percentage The score in the share text. A hundred means the ladder was finished
 *   without help, and only that is a success.
 * @property emoji The ladder, one emoji per rung, with the finishing emoji among them. Each
 *   rung's emoji indicates the help used: 🟢 none, 💡 one reveal, 👁️ more than one.
 */
data class RaddleResult(
    override val puzzleNumber: String?,
    val from: String?,
    val to: String?,
    val percentage: Int?,
    override val day: LocalDate?,
    val emoji: String = "",
) : GameResult {
    val clean: Int get() = countEmoji(CLEAN)
    val hinted: Int get() = countEmoji(HINTED)
    val revealed: Int get() = countEmoji(REVEALED)

    /** How many rungs the ladder had. */
    val rungs: Int get() = clean + hinted + revealed

    override val success: Boolean? get() = percentage?.let { it == 100 }

    private fun countEmoji(point: Int): Int =
        emoji
            .codePoints()
            .filter { it == point }
            .count()
            .toInt()
}

/**
 * Reads the Raddle share text.
 *
 *     COSTA → RICA [💯]
 *     Raddle #567 • Sep 14, 2026
 *     https://raddle.quest/567
 *
 *     🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🙌
 *
 * A ladder short of a hundred has its percentage in place of the 💯.
 *
 * The date is in American English whatever the player's language, so it is read here. The link
 * line is missing when the text is shared through the system sheet, so nothing depends on it.
 */
object RaddleParser : ResultParser<RaddleResult> {
    private val HEADER = Regex("""Raddle\s*#(\d+)""", RegexOption.IGNORE_CASE)
    private val DATE = Regex("""Raddle\s*#\d+\s*•\s*([A-Z][a-z]{2}\s+\d{1,2},\s*\d{4})""")
    private val LADDER =
        Regex(
            """^\s*(\S.*?)\s*→\s*(.+?)\s*\[(?:💯|(\d+)%)\]""",
            RegexOption.MULTILINE,
        )

    /**
     * A run of the rung emoji, and the emoji that marks where the ladder was finished. `\uFE0F` is
     * included because Raddle writes it after the eye.
     */
    private val EMOJI_RUN = Regex("""[🟢💡👁🙌🤝\uFE0F]+""")

    private val DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH)

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): RaddleResult? {
        val header = HEADER.find(text) ?: return null
        val ladder = LADDER.find(text)
        return RaddleResult(
            puzzleNumber = header.groupValues[1],
            from = ladder?.groupValues?.get(1),
            to = ladder?.groupValues?.get(2),
            percentage = ladder?.let { it.groupValues[3].toIntOrNull() ?: 100 },
            day =
                DATE
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    ?.let(::readDay),
            emoji =
                EMOJI_RUN
                    .findAll(text)
                    .map { it.value }
                    .maxByOrNull { it.length }
                    .orEmpty(),
        )
    }

    private fun readDay(written: String): LocalDate? =
        try {
            LocalDate.parse(written.replace(Regex("""\s+"""), " "), DATE_FORMAT)
        } catch (e: DateTimeParseException) {
            null
        }
}
