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
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import org.shawnz.dailytracker.MainActivity
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.SettingsRepository
import org.shawnz.dailytracker.time.earliestDateAnywhere
import java.time.ZonedDateTime

const val REMINDER_CHANNEL_ID = "daily-reminders"
private const val NOTIFICATION_ID = 1

/**
 * Posts one notification that lists the games that are still unplayed, then schedules the
 * next reminder.
 */
@HiltWorker
class ReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val repo: Repository,
        private val settings: SettingsRepository,
        private val scheduler: ReminderScheduler,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            val reminder = settings.reminder.first()
            val games = repo.activeGames()

            val recent = repo.observeRecentEntries(earliestDateAnywhere()).first()
            val now = ZonedDateTime.now()
            val outstanding =
                gamesToRemind(reminder, games, now)
                    .filter { game ->
                        val day = game.possiblePuzzleDays(recent, now).start
                        recent.none { it.gameId == game.id && it.puzzleDay == day }
                    }
            if (outstanding.isNotEmpty()) post(outstanding)

            scheduler.scheduleNext(reminder, games)
            return Result.success()
        }

        private fun post(outstanding: List<Game>) {
            val context = applicationContext
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return
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
                    .Builder(context, REMINDER_CHANNEL_ID)
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
                    .notify(NOTIFICATION_ID, notification)
            }
        }
    }
