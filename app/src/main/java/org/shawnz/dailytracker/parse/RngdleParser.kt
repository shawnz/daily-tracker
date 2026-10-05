package org.shawnz.dailytracker.parse

/**
 * An RNGdle result.
 *
 * A roll is neither won nor lost, so [success] is always null.
 *
 * @property roll The number rolled, as printed.
 * @property rarity The game's own word for the roll, from TRASH up to MYTHIC.
 * @property standing Where the roll placed. Printed only for a roll in the top half or the
 *   bottom tenth.
 * @property badges The three badges listed, which are worth the most.
 * @property moreBadges How many badges were earned but not listed.
 * @property points The experience the roll was worth.
 * @property poem The player's poem, if one was written.
 */
data class RngdleResult(
    val roll: String?,
    val rarity: String?,
    val standing: String?,
    val badges: List<String> = emptyList(),
    val moreBadges: Int = 0,
    val points: Int? = null,
    val poem: String? = null,
) : GameResult {
    val badgesEarned: Int get() = badges.size + moreBadges
}

/**
 * Reads the RNGdle share text.
 *
 *     RNGdle 🎲 982702
 *
 *     ⬜ COMMON • Bottom 8%
 *
 *     ⬜ 🎰 Lucky Seven (Divisible)
 *     ⬜ 👻 Ghost
 *     ⬜ 💨 Oxygen (8)
 *     +8 more
 *
 *     2,860 EP
 *     https://rngdle.com
 *
 * A roll between the top half and the bottom tenth doesn't have a standing. A roll can also have
 * a poem, in quotes above the experience.
 *
 * The experience has the digit grouping of the player's region.
 */
object RngdleParser : ResultParser<RngdleResult> {
    private const val TIERS = "🟫⬜🟩🟦🟪🟧🟥"

    private val HEADER = Regex("""RNGdle\s*🎲\s*([^\n]+)""", RegexOption.IGNORE_CASE)

    private val RARITY =
        Regex(
            """^[$TIERS]\s+(TRASH|COMMON|UNCOMMON|RARE|EPIC|ANOMALY|MYTHIC)(?:\s*•\s*(.+))?$""",
        )

    /** A badge is its own rarity, then its own emoji, then what it is called. */
    private val BADGE = Regex("""^[$TIERS]\s+(\S+)\s+(.+)$""")

    private val MORE = Regex("""^\+(\d+)\s+more$""", RegexOption.IGNORE_CASE)
    private val POINTS = Regex("""^($GROUPED_NUMBER) EP$""")
    private val POEM = Regex("""^"(.+)"$""")

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): RngdleResult? {
        val header = HEADER.find(text) ?: return null
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val rarity = lines.firstNotNullOfOrNull { RARITY.find(it) }
        return RngdleResult(
            roll = header.groupValues[1].trim().ifEmpty { null },
            rarity = rarity?.groupValues?.get(1),
            standing =
                rarity
                    ?.groupValues
                    ?.get(2)
                    ?.trim()
                    ?.ifEmpty { null },
            badges =
                lines
                    .filterNot { RARITY.containsMatchIn(it) }
                    .mapNotNull { BADGE.find(it)?.groupValues?.get(2) },
            moreBadges =
                lines.firstNotNullOfOrNull {
                    MORE
                        .find(it)
                        ?.groupValues
                        ?.get(1)
                        ?.toIntOrNull()
                } ?: 0,
            points =
                lines.firstNotNullOfOrNull { POINTS.find(it) }?.let {
                    groupedNumberValue(it.groupValues[1])
                },
            poem = lines.firstNotNullOfOrNull { POEM.find(it)?.groupValues?.get(1) },
        )
    }
}
