package org.shawnz.dailytracker.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.common.ApplicationScope
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.SettingsRepository
import java.time.Duration
import java.time.Instant
import java.time.ZonedDateTime
import javax.inject.Inject

private const val WORK_NAME = "reminder"

// The reminder was periodic work with this name up to version 1.3.
private const val PERIODIC_WORK_NAME = "daily-reminder"

/**
 * Schedules the next reminder as one-time work. The reminder that follows is scheduled from
 * [ReminderWorker], with [scheduleNext].
 */
class ReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @ApplicationScope private val scope: CoroutineScope,
        private val settings: SettingsRepository,
        private val repository: Repository,
    ) {
        /**
         * Schedules the next reminder, and schedules it again whenever the settings or the
         * tracked games change.
         */
        fun start(): Job =
            scope.launch {
                val manager = WorkManager.getInstance(context)
                manager.cancelUniqueWork(PERIODIC_WORK_NAME)
                combine(settings.reminder, repository.observeActiveGames(), ::Pair)
                    .distinctUntilChanged()
                    .collectIndexed { index, (reminder, games) ->
                        val next = nextReminder(reminder, games, ZonedDateTime.now())
                        if (next == null) {
                            manager.cancelUniqueWork(WORK_NAME)
                        } else {
                            // The first values are read when the process starts, which can be
                            // to run a reminder that is due. REPLACE would cancel that reminder.
                            val policy = if (index == 0) ExistingWorkPolicy.KEEP else ExistingWorkPolicy.REPLACE
                            enqueue(next, policy)
                        }
                    }
            }

        /**
         * Schedules the reminder that follows one posted now for [games]. Call it from the
         * running [ReminderWorker].
         */
        fun scheduleNext(
            reminder: ReminderSettings,
            games: List<Game>,
        ) {
            val after = includedUntil(reminder, ZonedDateTime.now())
            val next = nextReminder(reminder, games, after) ?: return
            // REPLACE would cancel the worker that is running.
            enqueue(next, ExistingWorkPolicy.APPEND_OR_REPLACE)
        }

        private fun enqueue(
            at: Instant,
            policy: ExistingWorkPolicy,
        ) {
            val request =
                OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(Duration.between(Instant.now(), at))
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
        }
    }
