package org.shawnz.dailytracker.parse

/**
 * A Kinda Hard Golf result.
 *
 * A round is played to the end, so [success] is always null.
 *
 * @property strokes The total printed for the round.
 * @property holes The stroke count from each checkpoint and the tee, in the order listed. A
 *   skipped checkpoint is 0.
 * @property mapName The name of a custom map, printed in quotes. Null for a daily.
 */
data class KindaHardGolfResult(
    override val puzzleNumber: String?,
    val strokes: Int?,
    val holes: List<Int> = emptyList(),
    val infuriating: Boolean = false,
    val mapName: String? = null,
) : GameResult

/**
 * Reads the Kinda Hard Golf share text.
 *
 *     kindahard.golf #530
 *
 *     📝 46
 *
 *     6.⛳ 3
 *     5.⛳ 5
 *     4.⛳ 3
 *     3.⛳ 17
 *     2.⛳ 10
 *     1.⛳ 4
 *     0.🏌️ 4
 *
 *     https://kindahard.golf
 *
 * A custom map has no number, and its name, if any, is quoted on the next line. The first line
 * of an infuriating round is wrapped in fire emoji. A skipped checkpoint has a dash in place of
 * its count.
 */
object KindaHardGolfParser : ResultParser<KindaHardGolfResult> {
    private val HEADER = Regex("""kindahard\.golf(?:\s*#(\d+))?""", RegexOption.IGNORE_CASE)
    private val MAP_NAME = Regex("""^\s*"(.+)"\s*$""", RegexOption.MULTILINE)
    private val STROKES = Regex("""📝\s*(\d+)""")
    private val HOLE = Regex("""^\s*\d+\.\s*(?:⛳|🏌)\uFE0F?\s*(\d+|-)\s*$""")

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): KindaHardGolfResult? {
        val header = HEADER.find(text) ?: return null
        return KindaHardGolfResult(
            puzzleNumber = header.groupValues[1].ifEmpty { null },
            strokes =
                STROKES
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull(),
            holes =
                text
                    .lineSequence()
                    .mapNotNull { HOLE.find(it)?.groupValues?.get(1) }
                    .map { it.toIntOrNull() ?: 0 }
                    .toList(),
            infuriating = text.contains("🔥"),
            mapName = MAP_NAME.find(text)?.groupValues?.get(1),
        )
    }
}
