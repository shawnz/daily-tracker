package org.shawnz.dailytracker.data

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.IOException
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

    /**
     * Runs [block] with a repository on the settings file, then closes the file so that the
     * next call can open it.
     */
    private suspend fun <T> TestScope.withRepository(block: suspend (SettingsRepository) -> T): T {
        val job = Job()
        val store = settingsDataStore(app, CoroutineScope(UnconfinedTestDispatcher(testScheduler) + job))
        try {
            return block(DefaultSettingsRepository(store))
        } finally {
            job.cancelAndJoin()
        }
    }

    private fun oldPreferences() = app.getSharedPreferences("daily-tracker", Context.MODE_PRIVATE)

    /** A settings file on a disk where every read and write fails. */
    private class FailingStore : DataStore<StoredSettings> {
        override val data: Flow<StoredSettings> = flow { throw IOException() }

        override suspend fun updateData(transform: suspend (t: StoredSettings) -> StoredSettings): StoredSettings = throw IOException()
    }

    @Test
    fun `reminders start switched on at eight in the evening`() =
        runTest {
            val settings = withRepository { it.reminder.first() }

            assertTrue(settings.enabled)
            assertEquals(LocalTime.of(20, 0), settings.time)
        }

    @Test
    fun `switching reminders off is reported straight away`() =
        runTest {
            val settings =
                withRepository {
                    it.setEnabled(false)
                    it.reminder.first()
                }

            assertFalse(settings.enabled)
        }

    @Test
    fun `a new time is reported straight away`() =
        runTest {
            val settings =
                withRepository {
                    it.setTime(LocalTime.of(7, 30))
                    it.reminder.first()
                }

            assertEquals(LocalTime.of(7, 30), settings.time)
        }

    @Test
    fun `a change is stored`() =
        runTest {
            withRepository { it.setTime(LocalTime.of(6, 15)) }

            val reopened = withRepository { it.reminder.first() }

            assertEquals(LocalTime.of(6, 15), reopened.time)
        }

    @Test
    fun `changing the time leaves reminders switched on`() =
        runTest {
            val settings =
                withRepository {
                    it.setTime(LocalTime.of(9, 45))
                    it.reminder.first()
                }

            assertTrue(settings.enabled)
        }

    @Test
    fun `settings from shared preferences are kept`() =
        runTest {
            oldPreferences().edit(commit = true) {
                putBoolean("reminders_enabled", false)
                putInt("reminder_hour", 7)
                putInt("reminder_minute", 30)
            }

            val settings = withRepository { it.reminder.first() }

            assertFalse(settings.enabled)
            assertEquals(LocalTime.of(7, 30), settings.time)
        }

    @Test
    fun `shared preferences are emptied once they are read`() =
        runTest {
            oldPreferences().edit(commit = true) { putInt("reminder_hour", 7) }

            withRepository { it.reminder.first() }

            assertTrue(oldPreferences().all.isEmpty())
        }

    @Test
    fun `the settings have their defaults when the file cannot be read`() =
        runTest {
            val settings = DefaultSettingsRepository(FailingStore()).reminder.first()

            assertEquals(ReminderSettings(), settings)
        }

    @Test
    fun `a change is lost when the file cannot be written`() =
        runTest {
            val repository = DefaultSettingsRepository(FailingStore())

            repository.setEnabled(false)

            assertEquals(ReminderSettings(), repository.reminder.first())
        }
}
