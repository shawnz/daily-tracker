package org.shawnz.dailytracker.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.Repository
import javax.inject.Inject

/**
 * Saves a shared result, or opens [ConfirmShareActivity] when the text is ambiguous.
 *
 * Its window is transparent, so the app the text was shared from stays visible and keeps its
 * position.
 */
@AndroidEntryPoint
class ReceiveShareActivity : ComponentActivity() {
    @Inject lateinit var repo: Repository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val text = intent?.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (text.isNullOrEmpty()) {
            toast(getString(R.string.share_nothing_to_save))
            finish()
            return
        }

        lifecycleScope.launch {
            val games = repo.activeGames()
            val match = attribute(text, games)
            val day =
                match?.let { game ->
                    game.parser
                        ?.parse(text)
                        ?.day
                        ?.takeIf { !it.isAfter(game.currentPuzzleDay()) }
                }

            when {
                games.isEmpty() -> {
                    toast(getString(R.string.share_no_games))
                }

                match != null && day != null -> {
                    repo.record(match, text, day)
                    toast(getString(R.string.share_saved_to, match.title))
                }

                // No task flags, so the dialog opens in this activity's task. Closing the dialog
                // then returns to the page it was shared from.
                else -> {
                    startActivity(
                        Intent(this@ReceiveShareActivity, ConfirmShareActivity::class.java)
                            .putExtra(ConfirmShareActivity.EXTRA_SHARED_TEXT, text)
                            .apply {
                                if (match != null) {
                                    putExtra(ConfirmShareActivity.EXTRA_MATCHED_GAME_ID, match.id)
                                }
                            },
                    )
                }
            }
            finish()
        }
    }

    /**
     * The game in [games] whose parser matches [text], or null when no game matches or two or
     * more do.
     */
    private fun attribute(
        text: String,
        games: List<Game>,
    ): Game? = games.singleOrNull { it.parser?.matches(text) == true }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
