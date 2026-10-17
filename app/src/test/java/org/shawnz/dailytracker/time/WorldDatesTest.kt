package org.shawnz.dailytracker.time

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class WorldDatesTest {
    // At 11:00 UTC, it is 23:00 the day before at UTC-12.
    @Test
    fun `earliest date anywhere is the date at UTC-12`() {
        assertEquals(LocalDate.of(2026, 9, 7), earliestDateAnywhere(Instant.parse("2026-09-08T11:00:00Z")))
    }
}
