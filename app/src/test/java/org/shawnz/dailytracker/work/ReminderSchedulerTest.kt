package org.shawnz.dailytracker.work

import android.app.Application
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.shawnz.dailytracker.data.ReminderMode
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.SettingsRepository
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.FakeSettingsRepository
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.Duration
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

    private val beforeReset = ReminderSettings(mode = ReminderMode.BEFORE_RESET)

    @Before
    fun setUp() {
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(app, configuration)
    }

    private fun TestScope.start(
        settings: SettingsRepository,
        repository: Repository = FakeRepository(),
    ) {
        ReminderScheduler(app, backgroundScope, settings, repository).start()
    }

    private fun scheduled(): List<WorkInfo> =
        WorkManager
            .getInstance(app)
            .getWorkInfos(WorkQuery.fromStates(WorkInfo.State.ENQUEUED))
            .get()

    @Test
    fun `the reminder is scheduled when reminders are on`() =
        runTest(UnconfinedTestDispatcher()) {
            start(FakeSettingsRepository())

            assertEquals(1, scheduled().size)
        }

    @Test
    fun `nothing is scheduled when reminders are off`() =
        runTest(UnconfinedTestDispatcher()) {
            start(FakeSettingsRepository(ReminderSettings(enabled = false)))

            assertEquals(0, scheduled().size)
        }

    @Test
    fun `switching reminders off cancels the reminder`() =
        runTest(UnconfinedTestDispatcher()) {
            val settings = FakeSettingsRepository()
            start(settings)

            settings.setEnabled(false)

            assertEquals(0, scheduled().size)
        }

    @Test
    fun `switching reminders on schedules the reminder`() =
        runTest(UnconfinedTestDispatcher()) {
            val settings = FakeSettingsRepository(ReminderSettings(enabled = false))
            start(settings)

            settings.setEnabled(true)

            assertEquals(1, scheduled().size)
        }

    // The two times are twelve hours apart, so the delays cannot be equal.
    @Test
    fun `a new time moves the reminder`() =
        runTest(UnconfinedTestDispatcher()) {
            val settings = FakeSettingsRepository(ReminderSettings(time = LocalTime.of(8, 0)))
            start(settings)
            val before = scheduled().single().initialDelayMillis

            settings.setTime(LocalTime.of(20, 0))

            assertNotEquals(before, scheduled().single().initialDelayMillis)
        }

    @Test
    fun `nothing is scheduled before a reset when no game is tracked`() =
        runTest(UnconfinedTestDispatcher()) {
            start(FakeSettingsRepository(beforeReset))

            assertEquals(0, scheduled().size)
        }

    @Test
    fun `a new game schedules the reminder before its reset`() =
        runTest(UnconfinedTestDispatcher()) {
            val repository = FakeRepository()
            start(FakeSettingsRepository(beforeReset), repository)

            repository.games.value = listOf(testGame())

            assertEquals(1, scheduled().size)
        }

    @Test
    fun `a reminder that is already scheduled is kept when the process starts`() =
        runTest(UnconfinedTestDispatcher()) {
            val settings = FakeSettingsRepository()
            start(settings)
            val pending = scheduled().single().id

            start(settings)

            assertEquals(pending, scheduled().single().id)
        }

    @Test
    fun `the periodic reminder of an earlier version is cancelled`() =
        runTest(UnconfinedTestDispatcher()) {
            val periodic = PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofDays(1)).build()
            WorkManager
                .getInstance(app)
                .enqueueUniquePeriodicWork("daily-reminder", ExistingPeriodicWorkPolicy.UPDATE, periodic)

            start(FakeSettingsRepository())

            assertNull(scheduled().single().periodicityInfo)
        }
}
