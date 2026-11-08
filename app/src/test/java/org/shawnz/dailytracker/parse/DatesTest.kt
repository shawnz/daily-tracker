package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.MonthDay

class DatesTest {
    private val date = Regex("^$MONTH_AND_DAY$")

    private fun read(text: String): MonthDay? = date.find(text)?.value?.let(::monthAndDayValue)

    @Test
    fun `a month and day is read`() {
        assertEquals(MonthDay.of(9, 14), read("Sep 14"))
    }

    @Test
    fun `a leap day is read`() {
        assertEquals(MonthDay.of(2, 29), read("Feb 29"))
    }

    @Test
    fun `a day the month does not have is null`() {
        assertNull(read("Sep 31"))
    }

    @Test
    fun `every English locale's form reads as the same day`() {
        listOf("Sep 4", "4 Sep", "4 Sept", "04 Sept").forEach {
            assertEquals(MonthDay.of(9, 4), read(it), it)
        }
    }

    @Test
    fun `a month written in four letters is read`() {
        assertEquals(MonthDay.of(6, 4), read("4 June"))
        assertEquals(MonthDay.of(7, 4), read("4 July"))
    }

    @Test
    fun `the forms of other languages are not matched`() {
        listOf("14 sept.", "14. Sept.", "14 sept", "9月14日").forEach {
            assertNull(read(it), it)
        }
    }
}
