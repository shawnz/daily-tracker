package org.shawnz.dailytracker.work

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.common.ApplicationScope
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.SettingsRepository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

private const val WORK_NAME = "daily-reminder"

/** Schedules the reminder from the settings, and schedules it again whenever they change. */
class ReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        @ApplicationScope private val scope: CoroutineScope,
        private val settings: SettingsRepository,
    ) {
        fun start(): Job = scope.launch { settings.reminder.collect { reschedule(it) } }

        private fun reschedule(reminder: ReminderSettings) {
            val manager = WorkManager.getInstance(context)
            if (!reminder.enabled) {
                manager.cancelUniqueWork(WORK_NAME)
                return
            }
            val request =
                PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofDays(1))
                    .setInitialDelay(delayUntilNext(reminder.time))
                    .build()
            manager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request,
            )
        }

        private fun delayUntilNext(time: LocalTime): Duration {
            val zone = ZoneId.systemDefault()
            val now = LocalDateTime.now(zone)
            var next = LocalDateTime.of(LocalDate.now(zone), time)
            if (!next.isAfter(now)) next = next.plusDays(1)
            return Duration.between(now, next)
        }
    }
