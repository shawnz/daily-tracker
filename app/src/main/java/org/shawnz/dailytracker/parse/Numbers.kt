package org.shawnz.dailytracker.parse

/**
 * A regex fragment for a whole number with the digit grouping of any locale, as formatted by
 * JavaScript's `toLocaleString()`: `1,234`, `1.234`, `1'234`, `1’234`, `1 234` with a plain,
 * no-break or narrow no-break space, `12,34,567`, and `١٬٢٣٤` in Arabic-Indic digits. N'Ko digits are
 * grouped with the Arabic comma, `،`.
 */
internal const val GROUPED_NUMBER = """\p{Nd}+(?:[,.'’٬ \u00A0\u202F\u060C]\p{Nd}{2,3})*"""

/**
 * Returns the value of [text], a match of [GROUPED_NUMBER], or null when it is too large for an
 * Int.
 *
 * Reads code points, because Chakma digits are above the Basic Multilingual Plane and do not
 * fit in one Char.
 */
internal fun groupedNumberValue(text: String): Int? =
    text
        .codePoints()
        .filter { Character.isDigit(it) }
        .toArray()
        .joinToString("") { Character.digit(it, 10).toString() }
        .toIntOrNull()
