package org.shawnz.dailytracker.parse

/**
 * A regex fragment for a whole number with the digit grouping of any locale, as formatted by
 * JavaScript's `toLocaleString()`: `1,234`, `1.234`, `1'234`, `1’234`, `1 234` with a plain,
 * no-break or narrow no-break space, `12,34,567`, and `١٬٢٣٤` in Arabic-Indic digits.
 */
internal const val GROUPED_NUMBER = """\p{Nd}+(?:[,.'’٬ \u00A0\u202F]\p{Nd}{2,3})*"""

/** Returns the value of [text], a match of [GROUPED_NUMBER], or null when it is too large for an Int. */
internal fun groupedNumberValue(text: String): Int? =
    text
        .filter { it.isDigit() }
        .map { it.digitToInt() }
        .joinToString("")
        .toIntOrNull()
