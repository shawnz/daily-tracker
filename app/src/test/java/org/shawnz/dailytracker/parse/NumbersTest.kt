package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class NumbersTest {
    private val number = Regex("^$GROUPED_NUMBER$")

    private fun read(text: String): Int? = number.find(text)?.value?.let(::groupedNumberValue)

    @Test
    fun `every locale's grouping reads as the same number`() {
        listOf(
            "8791",
            "8,791",
            "8.791",
            "8'791",
            "8’791",
            "8 791",
            "8\u00A0791",
            "8\u202F791",
            "٨٬٧٩١",
        ).forEach { assertEquals(8791, read(it), it) }
    }

    // 8791 as `toLocaleString()` writes it in N'Ko, with the Arabic comma.
    @Test
    fun `the arabic comma groups n'ko digits`() {
        assertEquals(8791, read("߈،߇߉߁"))
    }

    // 8791 as `toLocaleString()` writes it in Chakma.
    @Test
    fun `digits above the basic multilingual plane are read`() {
        assertEquals(8791, read("𑄾,𑄽𑄿𑄷"))
    }

    @Test
    fun `indian grouping reads as one number`() {
        assertEquals(1234567, read("12,34,567"))
    }

    @Test
    fun `a single digit after a separator is not a group`() {
        assertNull(read("8,7"))
    }

    @Test
    fun `a number too large for an int is null`() {
        assertNull(groupedNumberValue("99,999,999,999"))
    }
}
