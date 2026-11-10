package org.shawnz.dailytracker.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class DayPagesTest {
    private val october16 = LocalDate.of(2026, 10, 16)
    private val october18 = LocalDate.of(2026, 10, 18)
    private val pages = DayPages(october16, october18)

    @Test
    fun `there is one page for each date up to and including the last`() {
        assertEquals(3, pages.count)
        assertEquals(october18, pages.dayOf(pages.count - 1))
    }

    @Test
    fun `the page of a date has that date`() {
        val october17 = LocalDate.of(2026, 10, 17)

        assertEquals(october17, pages.dayOf(pages.pageOf(october17)))
    }

    @Test
    fun `a date outside the range is on the nearest page`() {
        assertEquals(0, pages.pageOf(october16.minusDays(5)))
        assertEquals(pages.count - 1, pages.pageOf(october18.plusDays(5)))
    }

    @Test
    fun `the key of a page is the date's day count since the epoch`() {
        assertEquals(october16.toEpochDay(), pages.key(0))
    }
}
