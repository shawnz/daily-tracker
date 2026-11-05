package org.shawnz.dailytracker.parse

/**
 * A Hued result.
 *
 * A day is scored rather than won or lost, so [success] is always null. A puzzle can be served
 * again on a later day, so [day] is always null.
 *
 * @property accuracy The best guess's match to the color, as a percentage.
 * @property colorName The name of the day's color.
 * @property guesses How many guesses were made, out of three.
 * @property hintUsed Whether the hint photo was shown.
 */
data class HuedResult(
    override val puzzleNumber: String,
    val accuracy: Int?,
    val colorName: String?,
    val guesses: Int?,
    val hintUsed: Boolean,
) : GameResult

/**
 * Reads the Hued share text.
 *
 *     Hued #179: 100% Avis
 *     🟢👑🌈
 *     Hue Master!
 *     https://playhued.com
 *
 * The second line has a rating of each guess in the order made, a white circle for each unused
 * guess, and a camera before the guess made after the hint. The Copy button leaves the link
 * out.
 */
object HuedParser : ResultParser<HuedResult> {
    private val HEADER = Regex("""Hued\s*#(\d+):\s*(\d+)%[ \t]*([^\n]*)""", RegexOption.IGNORE_CASE)

    private const val UNUSED = 0x26AA
    private const val CAMERA = 0x1F4F8

    /** Rainbow, crown, green, blue, yellow, red and black, from 100% down. */
    private val RATINGS = setOf(0x1F308, 0x1F451, 0x1F7E2, 0x1F535, 0x1F7E1, 0x1F534, 0x26AB)

    private val TILES = RATINGS + UNUSED + CAMERA + 0xFE0F

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): HuedResult? {
        val header = HEADER.find(text) ?: return null
        val row =
            text
                .substring(header.range.last + 1)
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { isGridRow(it, TILES) }
        return HuedResult(
            puzzleNumber = header.groupValues[1],
            accuracy = header.groupValues[2].toIntOrNull(),
            colorName = header.groupValues[3].trim().ifEmpty { null },
            guesses =
                row
                    ?.codePoints()
                    ?.filter { it in RATINGS }
                    ?.count()
                    ?.toInt(),
            hintUsed = row?.let { CAMERA in it.codePoints().toArray() } ?: false,
        )
    }
}
