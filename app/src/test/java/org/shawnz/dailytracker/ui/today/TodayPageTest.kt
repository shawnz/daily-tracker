package org.shawnz.dailytracker.ui.today

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Which dates the Today screen can show, at a fixed time. */
class TodayPageTest {
    // Its puzzle changes at midnight in New York.
    private val cluesBySam = testGame(id = 1, source = CatalogGame.CLUES_BY_SAM)

    // Its puzzle changes at midnight UTC.
    private val foodGuessr = testGame(id = 2, source = CatalogGame.FOODGUESSR)

    private val october16 = LocalDate.of(2026, 10, 16)
    private val october17 = LocalDate.of(2026, 10, 17)
    private val october18 = LocalDate.of(2026, 10, 18)

    private fun inputs(
        game: Game,
        chosen: LocalDate? = null,
    ) = Inputs(games = listOf(game), recent = emptyList(), earliestPlayed = null, chosen = chosen)

    // At 09:00 on the 17th in Sydney, it is still the 16th in New York.
    @Test
    fun `the earliest date includes a possible puzzle day before the device's date`() {
        val now = LocalDateTime.parse("2026-10-17T09:00").atZone(ZoneId.of("Australia/Sydney"))

        val page = inputs(cluesBySam).page(now)

        assertEquals(october16, page.earliest)
        assertEquals(october17, page.latest)
    }

    // At 21:00 on the 17th in New York, it is already the 18th in UTC.
    @Test
    fun `the latest date includes a possible puzzle day after the device's date`() {
        val now = LocalDateTime.parse("2026-10-17T21:00").atZone(ZoneId.of("America/New_York"))

        val page = inputs(foodGuessr).page(now)

        assertEquals(october17, page.earliest)
        assertEquals(october18, page.latest)
    }

    @Test
    fun `a chosen date after the latest date shows the latest date`() {
        val now = LocalDateTime.parse("2026-10-17T12:00").atZone(ZoneId.of("America/New_York"))

        val page = inputs(cluesBySam, chosen = october18).page(now)

        assertEquals(october17, page.day)
    }

    private val raddle = testGame(id = 3, source = CatalogGame.RADDLE)
    private val catfishing = testGame(id = 4, source = CatalogGame.CATFISHING)
    private val noonInChicago = LocalDateTime.parse("2026-10-17T12:00").atZone(ZoneId.of("America/Chicago"))

    /** The rows on the page for [day], with no results. */
    private fun rows(
        day: LocalDate,
        vararg games: Game,
    ) = Inputs(games = games.toList(), recent = emptyList(), earliestPlayed = day, chosen = day)
        .page(noonInChicago)
        .state(emptyList())
        .rows

    // With an unknown rollover time, the 18th's puzzle may not be out yet.
    @Test
    fun `a tap on an unplayed puzzle opens its own page`() {
        assertEquals("https://raddle.quest/2026/10/18/", rows(october18, raddle).single().siteUrl)
    }

    @Test
    fun `a tap on an unplayed puzzle opens the game's page when its day is the only possible puzzle day`() {
        assertEquals(catfishing.url, rows(october17, catfishing).single().siteUrl)
    }

    @Test
    fun `a tap on an unplayed puzzle with no page of its own opens the game's page`() {
        assertEquals(cluesBySam.url, rows(october17, cluesBySam).single().siteUrl)
    }

    @Test
    fun `a tap on a missed puzzle opens its own page`() {
        assertEquals("https://catfishing.net/game/845", rows(october16, catfishing).single().siteUrl)
    }

    @Test
    fun `a tap on a missed puzzle with no page of its own opens only the detail page`() {
        assertNull(rows(october16, cluesBySam).single().siteUrl)
    }

    // The game with an unknown rollover time has the 18th as a possible puzzle day, so the page for
    // the 18th can be shown.
    @Test
    fun `a tap on an unreleased puzzle opens only the detail page`() {
        assertNull(rows(october18, raddle, catfishing).single { it.game == catfishing }.siteUrl)
    }
}
