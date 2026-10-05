package org.shawnz.dailytracker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/**
 * Which days in the grid can be chosen, and which are highlighted as played.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class DayPickerContentTest {
    @get:Rule
    val compose = createComposeRule()

    private val last = LocalDate.of(2026, 9, 20)

    private fun show(
        selected: LocalDate = last,
        playedDays: Set<LocalDate> = emptySet(),
        onPick: (LocalDate) -> Unit = {
        },
    ) = compose.setContent {
        DayPickerContent(
            selected = selected,
            last = last,
            playedDays = playedDays,
            onPick = onPick,
        )
    }

    @Test
    fun `the month of the selected day is drawn`() {
        show()
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithText("20").assertIsDisplayed()
    }

    @Test
    fun `choosing a day reports it`() {
        var picked: LocalDate? = null
        show(onPick = { picked = it })

        compose.onNodeWithText("15").performClick()

        assertEquals(LocalDate.of(2026, 9, 15), picked)
    }

    // Days the game has not published yet are drawn, so the month's grid is complete, but they
    // cannot be chosen.
    @Test
    fun `a day after the last one cannot be chosen`() {
        var picked: LocalDate? = null
        show(onPick = { picked = it })

        compose.onNodeWithText("25").performClick()

        assertNull(picked)
    }

    @Test
    fun `the last day itself can be chosen`() {
        var picked: LocalDate? = null
        show(selected = LocalDate.of(2026, 9, 1), onPick = { picked = it })

        compose.onNodeWithText("20").performClick()

        assertEquals(last, picked)
    }
}
