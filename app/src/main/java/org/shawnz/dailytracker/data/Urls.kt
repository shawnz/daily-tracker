package org.shawnz.dailytracker.data

import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.net.URI

/**
 * A URL reduced to the parts that identify the page.
 *
 * A URL with no scheme is parsed as though it had one, so that `example.com` gives a host and
 * not a path. Text that [URI] cannot parse is compared as it stands.
 */
fun canonicalUrl(url: String): String {
    val text = url.trim()
    val uri = runCatching { read(text)?.normalize() }.getOrNull()
    val host = uri?.host?.lowercase()?.removePrefix("www.") ?: return text.lowercase()

    val defaultPort =
        when (uri.scheme?.lowercase()) {
            "http" -> 80
            "https" -> 443
            else -> null
        }
    val port = uri.port.takeIf { it != -1 && it != defaultPort }
    val path =
        uri.path
            .orEmpty()
            .lowercase()
            .trimEnd('/')
    val query = uri.query?.lowercase()?.takeIf { it.isNotEmpty() }

    return buildString {
        append(host)
        if (port != null) append(":").append(port)
        append(path)
        if (query != null) append("?").append(query)
    }
}

/** Whether two URLs refer to the same page, once both are reduced by [canonicalUrl]. */
fun sameUrl(
    a: String,
    b: String,
): Boolean = canonicalUrl(a) == canonicalUrl(b)

/**
 * Whether [url] matches [pattern], once both are reduced by [canonicalUrl]. An asterisk in
 * [pattern] matches any characters within one path segment.
 */
fun urlMatches(
    pattern: String,
    url: String,
): Boolean {
    val wanted = canonicalUrl(pattern)
    val found = canonicalUrl(url)
    if (!wanted.contains('*')) return wanted == found
    val parts = wanted.split('*').joinToString("[^/]*") { Regex.escape(it) }
    return Regex(parts).matches(found)
}

/**
 * Whether this could be the address of a page.
 *
 * The scheme is optional, as it is in a browser address bar, so `example.com` passes. A host
 * with no dot, such as `localhost`, does not.
 */
fun looksLikeUrl(url: String): Boolean {
    val host = runCatching { read(url.trim())?.host }.getOrNull() ?: return false
    return host.contains('.') && !host.startsWith('.') && !host.endsWith('.')
}

/** Reads a URL that may be missing its scheme, so that a bare host parses as a host. */
private fun read(text: String): URI? {
    if (text.isEmpty()) return null
    return URI(if (text.contains("://")) text else "https://$text")
}

/** A game the app already has, found from a typed URL. */
sealed interface UrlMatch {
    /** What that game is called, whichever kind it is. */
    val title: String

    /** On the Today list already. */
    data class Tracked(
        val game: Game,
    ) : UrlMatch {
        override val title: String get() = game.title
    }

    /** Tracked before and archived since, so it can be restored with its entries. */
    data class Archived(
        val game: Game,
    ) : UrlMatch {
        override val title: String get() = game.title
    }

    /** In the catalog and not tracked, so it can be added with the catalog's values. */
    data class Known(
        val catalogGame: CatalogGame,
    ) : UrlMatch {
        override val title: String get() = catalogGame.title
    }
}

/**
 * The game that [url] matches, or null when it doesn't match any.
 *
 * A game in [tracked] is returned as [UrlMatch.Tracked] or [UrlMatch.Archived], even when it is
 * also a catalog game.
 */
fun matchUrl(
    url: String,
    tracked: List<Game>,
): UrlMatch? {
    if (url.isBlank()) return null
    tracked.firstOrNull { game -> game.urls.any { urlMatches(it, url) } }?.let {
        return if (it.archived) UrlMatch.Archived(it) else UrlMatch.Tracked(it)
    }
    // Hidden games are matched too.
    return CatalogGame.entries
        .firstOrNull { game -> game.urls.any { urlMatches(it, url) } }
        ?.let { UrlMatch.Known(it) }
}
