package org.shawnz.dailytracker.share

import android.content.Intent
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.data.urlMatches
import javax.inject.Inject

/**
 * Saves a shared result, or opens [ConfirmShareActivity] when the text is ambiguous or would
 * replace a different result. Text that is already saved for its game and day is not saved again.
 * Before a result is saved, a game that is archived is restored, and a game that is not tracked
 * is added.
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
            val match = attribute(text)
            val tracked = match?.let { repo.findGame(it) }
            val day = match?.dayIn(text)
            val existing = if (tracked != null && day != null) repo.entryFor(tracked, day) else null

            when {
                match != null && day != null && existing == null -> {
                    repo.record(repo.activate(match), text, day)
                    toast(getString(R.string.share_saved_to, match.title))
                }

                match != null && existing?.hasText(text) == true -> {
                    toast(getString(R.string.share_already_saved_under, match.title))
                }

                // Warns when only the page URL was shared, unless it's a game without a parser, in
                // which case that may be all that's available.
                match == null && isGameUrl(text, CatalogGame.entries.filter { it.parser != null }) -> {
                    toast(getString(R.string.share_link_only))
                    buzz()
                }

                // No task flags, so the dialog opens in this activity's task. Closing the dialog
                // then returns to the page it was shared from.
                else -> {
                    startActivity(
                        Intent(this@ReceiveShareActivity, ConfirmShareActivity::class.java)
                            .putExtra(ConfirmShareActivity.EXTRA_SHARED_TEXT, text)
                            .apply {
                                if (match != null) {
                                    putExtra(ConfirmShareActivity.EXTRA_MATCHED_GAME, match.name)
                                }
                            },
                    )
                }
            }
            finish()
        }
    }

    /**
     * Returns the catalog game whose parser matches [text], or null when [text] doesn't match
     * exactly one parser.
     */
    private fun attribute(text: String): CatalogGame? =
        CatalogGame.entries.singleOrNull { it.parser?.matches(text) == true }

    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    /**
     * Vibrates to draw attention to a warning toast, with the timings of AOSP's
     * `config_notificationFallbackVibePattern`, which is played for a notification without its
     * own vibration when the phone is in vibrate mode. The timings are off and on durations in
     * milliseconds, starting with the delay, and -1 plays them once.
     */
    private fun buzz() {
        getSystemService(Vibrator::class.java)
            ?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 100, 150, 100), -1))
    }
}

/** Whether [text] only contains a link to one of [games]. */
internal fun isGameUrl(
    text: String,
    games: List<CatalogGame>,
): Boolean = games.any { game -> game.urls.any { urlMatches(it, text) } }
