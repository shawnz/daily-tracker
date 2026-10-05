package org.shawnz.dailytracker.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class SettingsRepositoryTest {
    private fun repository() =
        DefaultSettingsRepository(
            prefs = Prefs(RuntimeEnvironment.getApplication()),
            context = RuntimeEnvironment.getApplication(),
        )

    @Test
    fun `reminders start switched on at eight in the evening`() {
        val settings = repository().reminder.value

        assertTrue(settings.enabled)
        assertEquals(20, settings.hour)
        assertEquals(0, settings.minute)
    }

    @Test
    fun `switching reminders off is reported straight away`() {
        val repo = repository()

        repo.setEnabled(false)

        assertFalse(repo.reminder.value.enabled)
    }

    @Test
    fun `a new time is reported straight away`() {
        val repo = repository()

        repo.setTime(7, 30)

        assertEquals(7, repo.reminder.value.hour)
        assertEquals(30, repo.reminder.value.minute)
    }

    // The screen and the reminder scheduler read the same settings, so a change has to
    // survive past the object that made it.
    @Test
    fun `a change is stored rather than only remembered`() {
        repository().setTime(6, 15)

        val reopened = repository().reminder.value

        assertEquals(6, reopened.hour)
        assertEquals(15, reopened.minute)
    }

    @Test
    fun `changing the time leaves reminders switched on`() {
        val repo = repository()

        repo.setTime(9, 45)

        assertTrue(repo.reminder.value.enabled)
    }
}
