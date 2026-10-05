package org.shawnz.dailytracker.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame

class SearchTest {
    private val catalog = CatalogGame.entries

    @Test
    fun `a blank query lists every game that is not hidden`() {
        assertEquals(catalog.filterNot { it.hidden }, searchCatalog(catalog, ""))
        assertEquals(catalog.filterNot { it.hidden }, searchCatalog(catalog, "  "))
    }

    @Test
    fun `a query matches part of a title in any case`() {
        val found = searchCatalog(catalog, "WORD")

        assertTrue(CatalogGame.WORDLE in found)
        assertTrue(CatalogGame.PARSEWORD in found)
        assertFalse(CatalogGame.CONNECTIONS in found)
    }

    @Test
    fun `spaces and punctuation are not compared`() {
        assertEquals(listOf(CatalogGame.CLUES_BY_SAM), searchCatalog(catalog, "cluesby"))
        assertEquals(listOf(CatalogGame.CLUES_BY_SAM), searchCatalog(catalog, "clues-by sam"))
    }

    @Test
    fun `accents are not compared`() {
        assertEquals("cafe", searchFold("Café"))
    }

    @Test
    fun `a query matches a publisher`() {
        assertEquals(
            catalog.filter { it.publisher == "LinkedIn" },
            searchCatalog(catalog, "linkedin"),
        )
    }

    @Test
    fun `a typed x matches the multiplication sign in a title`() {
        assertEquals(listOf(CatalogGame.FOUR_BY_THREE), searchCatalog(catalog, "4x3"))
    }

    @Test
    fun `a hidden game is listed for its exact title`() {
        assertEquals(listOf(CatalogGame.FOUR_BY_SIX), searchCatalog(catalog, "4x6"))
        assertEquals(listOf(CatalogGame.FOUR_BY_SIX), searchCatalog(catalog, "4×6"))
        assertEquals(listOf(CatalogGame.FOUR_BY_SIX), searchCatalog(catalog, " 4 X 6 "))
    }

    @Test
    fun `a hidden game is not listed for part of its title`() {
        assertEquals(listOf(CatalogGame.FOUR_BY_THREE), searchCatalog(catalog, "4x"))
    }

    @Test
    fun `a hidden game is not listed for its publisher`() {
        assertEquals(listOf(CatalogGame.FOUR_BY_THREE), searchCatalog(catalog, "hank green"))
    }

    @Test
    fun `tracked games are matched by title or publisher`() {
        val wordle = testGame(id = 1, source = CatalogGame.WORDLE)
        val own = testGame(id = 2, source = CustomGame("Crosswörd Club", "https://example.com/"))
        val games = listOf(wordle, own)

        assertEquals(games, searchGames(games, ""))
        assertEquals(listOf(own), searchGames(games, "crossword"))
        assertEquals(listOf(wordle), searchGames(games, "new york"))
    }
}
