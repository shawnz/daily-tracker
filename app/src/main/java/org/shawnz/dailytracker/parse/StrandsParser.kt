package org.shawnz.dailytracker.parse

import java.time.LocalDate

/** The day of puzzle #1. There is one puzzle a day. */
private val FIRST_DAY: LocalDate = LocalDate.of(2024, 3, 4)

/**
 * The emoji of one puzzle. Each emoji is matched by its first code point, so a variation selector
 * or joiner after it doesn't matter.
 */
private class Marks(
    theme: List<String>,
    spangram: String,
    hint: String = "💡",
) {
    val theme: List<Int> = theme.map { it.codePointAt(0) }
    val spangram: Int = spangram.codePointAt(0)
    val hint: Int = hint.codePointAt(0)
}

private val USUAL = Marks(listOf("🔵"), "🟡")

private val USUAL_POINTS = USUAL.theme + USUAL.spangram + USUAL.hint

/** The puzzles with their own emoji, by puzzle number. */
private val THEMED =
    mapOf(
        "483" to Marks(listOf("🔵", "🟢", "🔴", "🟠", "🟣", "🟡"), "🏳️‍🌈"),
        "607" to Marks(listOf("🦴"), "💀", hint = "🕯️"),
        "652" to Marks(listOf("🟡", "⚪️", "🟢", "🔵"), "📺"),
        "744" to Marks(listOf("🟢"), "☘️"),
        "853" to Marks(listOf("🎆"), "🇺🇸"),
        "899" to Marks(listOf("🧺"), "🌭"),
    )

/**
 * A Strands result.
 *
 * Usually 🔵 is a theme word, 🟡 the spangram and 💡 a hint. A few puzzles have their own emoji.
 * The counts are null for a puzzle with its own emoji that isn't listed in [THEMED], because
 * the meaning of each emoji isn't known.
 *
 * @property clue The theme, in the game's own words.
 * @property emoji The emoji lines as shared, in the order played. Each answer has an emoji, and so
 *   does each hint used. The lines are wrapped at four emoji, so a line break has no meaning.
 */
data class StrandsResult(
    override val puzzleNumber: String?,
    val clue: String?,
    val emoji: List<String> = emptyList(),
) : GameResult {
    private val marks: Marks?
        get() =
            THEMED[puzzleNumber]
                ?: USUAL.takeIf {
                    emoji.all { line -> line.codePoints().allMatch { it in USUAL_POINTS || Character.isWhitespace(it) } }
                }

    val themeWords: Int? get() = marks?.let { marks -> marks.theme.sumOf { countTiles(emoji, it) } }

    val spangramFound: Boolean? get() = marks?.let { countTiles(emoji, it.spangram) > 0 }

    /** A word can take two hints: one to show it, and one to show its letters in order. */
    val hints: Int? get() = marks?.let { countTiles(emoji, it.hint) }

    /** The board is played until it is finished, so there is no loss. */
    override val success: Boolean get() = true

    override val day: LocalDate?
        get() = dayFromNumber(puzzleNumber, FIRST_DAY)
}

/**
 * Reads the Strands share text.
 *
 *     Strands #925
 *     “OH MY GOSH!!”
 *     🔵💡🔵🟡
 *     🔵🔵🔵🔵
 *
 * A puzzle played from the archive has `Archive` and its date above the first line. A bonus
 * puzzle's share text doesn't have a puzzle number, so it isn't read.
 *
 * The emoji lines are found by their position below the clue, because a themed puzzle doesn't
 * use the usual emoji.
 */
object StrandsParser : ResultParser<StrandsResult> {
    private val HEADER = Regex("""Strands\s*#\s*(\d+)""", RegexOption.IGNORE_CASE)
    private val CLUE = Regex("""[“"](.*)[”"]""")

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): StrandsResult? {
        val header = HEADER.find(text) ?: return null
        val lines = text.lines().map { it.trim() }
        val clueLine = lines.indexOfFirst { CLUE.containsMatchIn(it) }
        return StrandsResult(
            puzzleNumber = header.groupValues[1],
            clue = lines.getOrNull(clueLine)?.let { CLUE.find(it)?.groupValues?.get(1) },
            emoji = if (clueLine < 0) emptyList() else lines.drop(clueLine + 1).filter(::isMarks),
        )
    }

    /** Whether [line] is emoji only, without letters or digits. */
    private fun isMarks(line: String): Boolean = line.isNotEmpty() && line.none { it.isLetterOrDigit() }
}
