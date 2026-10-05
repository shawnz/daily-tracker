package org.shawnz.dailytracker

import android.annotation.SuppressLint
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.shawnz.dailytracker.data.SettingsRepository
import org.shawnz.dailytracker.work.Reminders
import javax.inject.Inject

// AndroidManifest.xml removes WorkManagerInitializer, but the check reports it anyway:
// https://issuetracker.google.com/issues/195025254
@SuppressLint("RemoveWorkManagerInitializer")
@HiltAndroidApp
class DailyTrackerApp :
    Application(),
    Configuration.Provider {
    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var settings: SettingsRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Reminders.reschedule(this, settings.reminder.value)
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                Reminders.CHANNEL_ID,
                getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = getString(R.string.reminder_channel_description) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
