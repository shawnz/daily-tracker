package org.shawnz.dailytracker.share

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import org.shawnz.dailytracker.ui.share.ConfirmShareDialog
import org.shawnz.dailytracker.ui.theme.DailyTrackerTheme

/**
 * Selects the game and/or day for a shared result when the text is ambiguous.
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

        val matched = intent?.getLongExtra(EXTRA_MATCHED_GAME_ID, NO_GAME) ?: NO_GAME

        setContent {
            DailyTrackerTheme {
                ConfirmShareDialog(
                    sharedText = text,
                    matchedGameId = matched.takeIf { it != NO_GAME },
                    onDone = { finish() },
                )
            }
        }
    }

    companion object {
        const val EXTRA_SHARED_TEXT = "shared_text"

        /** The game the receiver matched. Absent when the text didn't match exactly one game. */
        const val EXTRA_MATCHED_GAME_ID = "matched_game_id"

        const val NO_GAME = -1L
    }
}
