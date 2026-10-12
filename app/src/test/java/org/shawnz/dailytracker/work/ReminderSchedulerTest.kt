package org.shawnz.dailytracker.work

import android.app.Application
import androidx.work.Configuration
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.SettingsRepository
import org.shawnz.dailytracker.testdoubles.FakeSettingsRepository
import java.time.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReminderSchedulerTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(app, configuration)
    }

    private fun TestScope.start(settings: SettingsRepository) {
        ReminderScheduler(app, backgroundScope, settings).start()
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
}
