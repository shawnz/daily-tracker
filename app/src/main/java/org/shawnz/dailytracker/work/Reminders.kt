package org.shawnz.dailytracker.work

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.shawnz.dailytracker.MainActivity
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.Repository
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object Reminders {
    const val CHANNEL_ID = "daily-reminders"
    const val NOTIFICATION_ID = 1
    private const val WORK_NAME = "daily-reminder"

    fun reschedule(
        context: Context,
        settings: ReminderSettings,
    ) {
        val manager = WorkManager.getInstance(context)
        if (!settings.enabled) {
            manager.cancelUniqueWork(WORK_NAME)
            return
        }
        val request =
            PeriodicWorkRequestBuilder<ReminderWorker>(Duration.ofDays(1))
                .setInitialDelay(delayUntilNext(settings.hour, settings.minute))
                .build()
        manager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun delayUntilNext(
        hour: Int,
        minute: Int,
    ): Duration {
        val zone = ZoneId.systemDefault()
        val now = LocalDateTime.now(zone)
        var next = LocalDateTime.of(LocalDate.now(zone), LocalTime.of(hour, minute))
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next)
    }
}

/** Posts one notification that lists the games that are still unplayed. */
@HiltWorker
class ReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val repo: Repository,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val context = applicationContext

            val outstanding =
                repo
                    .activeGames()
                    .filter { it.remindersEnabled }
                    .filter { repo.entryFor(it) == null }

            if (outstanding.isEmpty()) return Result.success()

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return Result.success()
            }

            val names = outstanding.joinToString(", ") { it.title }
            val title =
                context.resources.getQuantityString(
                    R.plurals.reminder_title,
                    outstanding.size,
                    outstanding.size,
                )

            // Sends what the launcher sends, which resumes the task. The reminder must not start
            // the task again: that closes a game left open above the app.
            val tapIntent =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java).apply {
                        action = Intent.ACTION_MAIN
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                )

            val notification =
                NotificationCompat
                    .Builder(context, Reminders.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(names)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(names))
                    .setContentIntent(tapIntent)
                    .setAutoCancel(true)
                    .build()

            runCatching {
                NotificationManagerCompat
                    .from(context)
                    .notify(Reminders.NOTIFICATION_ID, notification)
            }
            return Result.success()
        }
    }
