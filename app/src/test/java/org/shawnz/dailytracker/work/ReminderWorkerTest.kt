package org.shawnz.dailytracker.work

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkQuery
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.FakeSettingsRepository
import org.shawnz.dailytracker.testdoubles.testGame

/**
 * Which games are still unplayed today, and whether a notification is posted at all.
 */
@RunWith(RobolectricTestRunner::class)
class ReminderWorkerTest {
    private val app: Application get() = RuntimeEnvironment.getApplication()
    private val game = testGame(id = 1)

    private fun allowNotifications() {
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun postedNotifications(): Int {
        val manager = app.getSystemService(NotificationManager::class.java)
        return shadowOf(manager).size()
    }

    @Before
    fun setUp() {
        val configuration = Configuration.Builder().setExecutor(SynchronousExecutor()).build()
        WorkManagerTestInitHelper.initializeTestWorkManager(app, configuration)
    }

    private fun scheduled(): List<WorkInfo> =
        WorkManager
            .getInstance(app)
            .getWorkInfos(WorkQuery.fromStates(WorkInfo.State.ENQUEUED))
            .get()

    private fun run(repo: Repository): ListenableWorker.Result {
        val settings = FakeSettingsRepository()
        val scheduler = ReminderScheduler(app, CoroutineScope(Job()), settings, repo)
        val worker =
            TestListenableWorkerBuilder<ReminderWorker>(app)
                .setWorkerFactory(
                    object : WorkerFactory() {
                        override fun createWorker(
                            appContext: Context,
                            workerClassName: String,
                            workerParameters: WorkerParameters,
                        ) = ReminderWorker(appContext, workerParameters, repo, settings, scheduler)
                    },
                ).build()
        return runBlocking { worker.doWork() }
    }

    private fun playedToday() =
        EntryEntity(
            id = 1,
            gameId = game.id,
            puzzleDay = game.currentPuzzleDay(),
        )

    @Test
    fun `a game not played today is worth a reminder`() {
        allowNotifications()

        val result = run(FakeRepository(games = listOf(game)))

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(1, postedNotifications())
    }

    @Test
    fun `the next reminder is scheduled after a reminder`() {
        allowNotifications()

        run(FakeRepository(games = listOf(game)))

        assertEquals(1, scheduled().size)
    }

    @Test
    fun `nothing is said when every game has been played`() {
        allowNotifications()

        val repo = FakeRepository(games = listOf(game), entries = listOf(playedToday()))
        val result = run(repo)

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(0, postedNotifications())
    }

    @Test
    fun `nothing is said when no games are tracked`() {
        allowNotifications()

        val result = run(FakeRepository())

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(0, postedNotifications())
    }

    // Muting a game takes it out of the reminder without archiving it.
    @Test
    fun `a muted game is not counted`() {
        allowNotifications()
        val muted = game.copy(remindersEnabled = false)

        val result = run(FakeRepository(games = listOf(muted)))

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(0, postedNotifications())
    }

    // Reminders can be on while the system permission is refused, and the work still succeeds.
    @Test
    fun `nothing is posted without permission`() {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)

        val result = run(FakeRepository(games = listOf(game)))

        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(0, postedNotifications())
    }
}
