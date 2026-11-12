package org.shawnz.dailytracker.share

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.raddleText
import java.time.LocalDate

class IsGameUrlTest {
    private val games = listOf(CatalogGame.RADDLE)

    // The url field Raddle passes to navigator.share, which Firefox shares alone (bug 2077771).
    @Test
    fun `the page URL of a puzzle is a link to its game`() {
        assertTrue(isGameUrl("https://raddle.quest/567", games))
    }

    @Test
    fun `share text with a result is not a link`() {
        assertFalse(isGameUrl(raddleText(LocalDate.of(2026, 9, 14)), games))
    }

    @Test
    fun `a link to a game that is not in the list is not a link to one of them`() {
        assertFalse(isGameUrl("https://www.foodguessr.com/game/daily", games))
    }
}
