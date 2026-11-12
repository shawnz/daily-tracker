package org.shawnz.dailytracker.share

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.shawnz.dailytracker.ui.share.ConfirmShareDialog
import org.shawnz.dailytracker.ui.theme.DailyTrackerTheme

/**
 * Selects the game and/or day for a shared result when the text is ambiguous, and confirms a
 * save that would replace a different result.
 *
 * Its window is transparent, so the app the text was shared from stays visible and keeps its
 * position. Runs in the share flow's task, so finishing returns to that app.
 */
@AndroidEntryPoint
class ConfirmShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent?.getStringExtra(EXTRA_SHARED_TEXT)
        if (text.isNullOrBlank()) {
            finish()
            return
        }

        val matched = intent?.hasExtra(EXTRA_MATCHED_GAME) == true

        setContent {
            DailyTrackerTheme {
                ConfirmShareDialog(
                    sharedText = text,
                    matched = matched,
                    onDone = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_SHARED_TEXT = "shared_text"

        /**
         * The name of the catalog game whose parser matches the shared text. Absent when the text
         * doesn't match exactly one parser.
         */
        const val EXTRA_MATCHED_GAME = "matched_game"
    }
}
