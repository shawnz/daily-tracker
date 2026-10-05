package org.shawnz.dailytracker.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame

/**
 * The example hosts here are the ones RFC 2606 reserves, so none of them is anyone's. The
 * tests that match against the catalog read its real addresses.
 */
class UrlsTest {
    @Test
    fun `scheme does not decide the page`() {
        assertTrue(sameUrl("https://example.com/", "http://example.com/"))
        assertTrue(sameUrl("https://example.com/", "example.com"))
    }

    @Test
    fun `a leading www does not decide the page`() {
        assertTrue(sameUrl("https://www.example.com/", "https://example.com/"))
    }

    @Test
    fun `a trailing slash does not decide the page`() {
        assertTrue(sameUrl("https://example.com", "https://example.com/"))
        assertTrue(sameUrl("https://example.com/", "example.com"))
    }

    @Test
    fun `case does not decide the page`() {
        assertTrue(sameUrl("HTTPS://Example.COM/", "https://example.com"))
    }

    @Test
    fun `a default port does not decide the page`() {
        assertTrue(sameUrl("https://example.com:443/", "https://example.com"))
        assertTrue(sameUrl("http://example.com:80/", "example.com"))
    }

    @Test
    fun `the other scheme's default port decides the page`() {
        assertFalse(sameUrl("https://example.com:80/", "https://example.com"))
        assertFalse(sameUrl("http://example.com:443/", "http://example.com"))
    }

    @Test
    fun `a port that is not the default does decide the page`() {
        assertFalse(sameUrl("https://example.com:8080/", "https://example.com"))
    }

    @Test
    fun `an empty query or fragment does not decide the page`() {
        assertTrue(sameUrl("https://example.com/?", "https://example.com"))
        assertTrue(sameUrl("https://example.com/#", "https://example.com"))
        assertTrue(sameUrl("https://example.com/#how-to-play", "https://example.com"))
    }

    @Test
    fun `all of them together`() {
        assertTrue(
            sameUrl(
                "HTTP://WWW.Example.com:80/games/daily/index.html/#play",
                "https://www.example.com/games/daily/index.html",
            ),
        )
    }

    @Test
    fun `the path decides the page`() {
        assertFalse(sameUrl("https://example.com/games/one", "https://example.com/games/two"))
    }

    @Test
    fun `a different host is a different page`() {
        assertFalse(sameUrl("https://example.com/", "https://example.net/"))
        assertFalse(sameUrl("https://example.com/", "https://example.org/"))
    }

    @Test
    fun `dot segments in a path are resolved`() {
        assertTrue(sameUrl("https://example.com/games/../play", "https://example.com/play"))
        assertTrue(sameUrl("https://example.com/./play", "https://example.com/play"))
    }

    @Test
    fun `text that is not a URL is still compared`() {
        assertTrue(sameUrl("not a url", "NOT A URL"))
        assertFalse(sameUrl("not a url", "some other text"))
    }

    @Test
    fun `a real query still decides the page`() {
        assertFalse(sameUrl("https://example.com/play?id=1", "https://example.com/play?id=2"))
    }

    @Test
    fun `a bare host is a link`() {
        assertTrue(looksLikeUrl("example.com"))
        assertTrue(looksLikeUrl("www.example.com/games/daily/"))
        assertTrue(looksLikeUrl("  example.org  "))
    }

    @Test
    fun `a link with a scheme is a link`() {
        assertTrue(looksLikeUrl("https://www.example.com/games/daily/index.html"))
        assertTrue(looksLikeUrl("http://example.com:8080/play?id=1"))
    }

    @Test
    fun `a word is not a link`() {
        assertFalse(looksLikeUrl("wordgame"))
        assertFalse(looksLikeUrl(""))
        assertFalse(looksLikeUrl("   "))
        assertFalse(looksLikeUrl("some words typed by mistake"))
    }

    @Test
    fun `a space anywhere inside is not a link`() {
        assertFalse(looksLikeUrl("example.com/my page"))
        assertFalse(looksLikeUrl("exam ple.com"))
        assertFalse(looksLikeUrl("https://example.com/a b"))
    }

    @Test
    fun `a host has to have a dot inside it`() {
        assertFalse(looksLikeUrl("localhost"))
        assertFalse(looksLikeUrl(".com"))
        assertFalse(looksLikeUrl("example."))
    }

    @Test
    fun `a star stands for one part of the path`() {
        assertTrue(urlMatches("https://example.com/*", "https://example.com/567"))
        assertTrue(urlMatches("https://example.com/*", "example.com/567"))
        assertTrue(urlMatches("https://example.com/game/*", "https://example.com/game/daily"))
    }

    // So that one game's pattern doesn't match every page below it, including another game's.
    @Test
    fun `a star does not reach past its own part of the path`() {
        assertFalse(urlMatches("https://example.com/*", "https://example.com/archive/567"))
        assertFalse(urlMatches("https://example.com/*", "https://example.com"))
    }

    @Test
    fun `a pattern with no star has to match outright`() {
        assertTrue(urlMatches("https://example.com/game/daily", "example.com/game/daily/"))
        assertFalse(urlMatches("https://example.com", "https://example.com/game/daily"))
    }

    @Test
    fun `an alternate address finds the game it belongs to`() {
        for (game in CatalogGame.entries) {
            for (other in game.otherUrls) {
                assertEquals(UrlMatch.Known(game), matchUrl(other, emptyList()), other)
            }
        }
    }

    // A game's own pages match it, and a sibling game's do not.
    @Test
    fun `a game claims its own pages and not another game's`() {
        val puzzle = matchUrl("https://raddle.quest/567", emptyList())
        assertEquals(UrlMatch.Known(CatalogGame.RADDLE), puzzle)

        val daily = matchUrl("https://www.foodguessr.com/game/daily", emptyList())
        assertEquals(UrlMatch.Known(CatalogGame.FOODGUESSR), daily)

        // The address the game is listed under is matched as well as the page below it.
        val home = matchUrl("foodguessr.com", emptyList())
        assertEquals(UrlMatch.Known(CatalogGame.FOODGUESSR), home)

        // Another daily on the same site is not this game.
        assertNull(matchUrl("https://www.foodguessr.com/game/plate-off/daily", emptyList()))
    }

    // So that a tracked game's alternate address isn't offered as a new game to add.
    @Test
    fun `an alternate address of a tracked game reports it as tracked`() {
        for (game in CatalogGame.entries) {
            for (other in game.otherUrls) {
                val match = matchUrl(other, listOf(testGame(source = game)))
                assertTrue(match is UrlMatch.Tracked, "$other was not reported as tracked")
            }
        }
    }
}
