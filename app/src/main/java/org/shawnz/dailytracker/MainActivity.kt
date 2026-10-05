package org.shawnz.dailytracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import org.shawnz.dailytracker.browser.CustomTabLauncher
import org.shawnz.dailytracker.ui.AppNav
import org.shawnz.dailytracker.ui.theme.DailyTrackerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            DailyTrackerTheme {
                AppNav()
            }
        }
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
}
