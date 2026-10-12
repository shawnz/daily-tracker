package org.shawnz.dailytracker

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import org.shawnz.dailytracker.browser.CustomTabLauncher
import org.shawnz.dailytracker.data.SettingsRepository
import org.shawnz.dailytracker.ui.AppNav
import org.shawnz.dailytracker.ui.theme.DailyTrackerTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settings: SettingsRepository

    private val notificationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DailyTrackerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNav()
                }
            }
        }

        // Skipped when the activity is recreated, because the dialog can still be open.
        if (savedInstanceState == null) askForNotificationPermission()
    }

    // Warms up the browser when the app is shown, so opening a game doesn't wait for the browser
    // to start.
    override fun onStart() {
        super.onStart()
        CustomTabLauncher.bind(this)
    }

    override fun onStop() {
        super.onStop()
        CustomTabLauncher.unbind(this)
    }

    /**
     * Reminders are on by default, and a reminder is not posted without the permission.
     *
     * `shouldShowRequestPermissionRationale` is true after the first refusal, so a user who
     * refused is not asked again.
     */
    private fun askForNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (!settings.reminder.value.enabled) return
        val permission = Manifest.permission.POST_NOTIFICATIONS
        if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) return
        if (shouldShowRequestPermissionRationale(permission)) return
        notificationPermissionRequest.launch(permission)
    }
}
