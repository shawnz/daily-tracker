package org.shawnz.dailytracker.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

class ConvertersTest {
    private val converters = Converters()

    @Test
    fun `a date is stored as days since 1970`() {
        val date = LocalDate.of(2026, 9, 25)
        assertEquals(date.toEpochDay(), converters.fromLocalDate(date))
        assertEquals(date, converters.toLocalDate(converters.fromLocalDate(date)))
    }

    @Test
    fun `an instant keeps its nanoseconds`() {
        val instant = Instant.parse("2026-09-25T14:03:07.123456789Z")
        assertEquals(instant, converters.toInstant(converters.fromInstant(instant)))
    }

    @Test
    fun `an instant before 1970 keeps its nanoseconds`() {
        val instant = Instant.parse("1969-12-31T23:59:59.000000001Z")
        assertEquals(-999_999_999L, converters.fromInstant(instant))
        assertEquals(instant, converters.toInstant(converters.fromInstant(instant)))
    }
}
