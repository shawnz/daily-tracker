package org.shawnz.dailytracker.data

import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.text.Normalizer

private val NOT_LETTER_OR_DIGIT = Regex("[^\\p{L}\\p{N}]")

/**
 * The letters and digits of [text] in the form compared by a search: lower case and without
 * accents.
 *
 * `×` becomes the letter `x`, since `×` can't be typed on most keyboards and Unicode
 * normalization leaves it unchanged.
 */
internal fun searchFold(text: String): String =
    Normalizer
        .normalize(text.replace('×', 'x'), Normalizer.Form.NFKD)
        .replace(NOT_LETTER_OR_DIGIT, "")
        .lowercase()

/**
 * The games in [games] to list for [query]: the ones whose title or publisher contains [query],
 * or all of them when [query] is blank.
 *
 * A hidden game is listed only when [query] matches its title exactly.
 */
fun searchCatalog(
    games: List<CatalogGame>,
    query: String,
): List<CatalogGame> {
    val key = searchFold(query)
    return games.filter { if (it.hidden) searchFold(it.title) == key else it.matches(key) }
}

/**
 * The games in [games] whose title or publisher contains [query], or all of them when [query] is
 * blank.
 */
fun searchGames(
    games: List<Game>,
    query: String,
): List<Game> {
    val key = searchFold(query)
    return games.filter { it.source.matches(key) }
}

private fun GameSource.matches(key: String): Boolean =
    key in searchFold(title) ||
        (this as? CatalogGame)?.publisher?.let { key in searchFold(it) } == true
