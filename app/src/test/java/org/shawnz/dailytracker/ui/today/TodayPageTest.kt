package org.shawnz.dailytracker.ui.today

import org.junit.jupiter.api.Assertions.assertEquals
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
}
