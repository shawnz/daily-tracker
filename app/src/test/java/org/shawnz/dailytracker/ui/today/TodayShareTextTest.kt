package org.shawnz.dailytracker.ui.today

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.shawnz.dailytracker.data.CustomGame
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame
import org.shawnz.dailytracker.testdoubles.wordleText
import java.time.LocalDate

/**
 * Which games are listed in the text shared from the Today screen, and what is written for each.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class TodayShareTextTest {
    private val day = LocalDate.of(2026, 10, 6)
    private val wordle = testGame(id = 1, source = CatalogGame.WORDLE)
    private val custom = testGame(id = 2, source = CustomGame("My Game", "https://example.com"))

    private fun row(
        game: Game,
        text: String?,
    ) = GameRowState(game, EntryEntity(gameId = game.id, puzzleDay = day, rawShareText = text))

    private fun share(vararg rows: GameRowState) = todayShareText(rows.toList(), day, RuntimeEnvironment.getApplication().resources)

    @Test
    fun `a played game has a line with its title and its result`() {
        assertEquals(
            "Daily Tracker, Oct 6\n\nWordle: 1/6",
            share(row(wordle, wordleText(day))),
        )
    }

    @Test
    fun `games are listed in the order of the rows`() {
        assertEquals(
            "Daily Tracker, Oct 6\n\nMy Game: Done\nWordle: 1/6",
            share(row(custom, null), row(wordle, wordleText(day))),
        )
    }

    @Test
    fun `a game that is not played is left out`() {
        assertEquals(
            "Daily Tracker, Oct 6\n\nWordle: 1/6",
            share(row(wordle, wordleText(day)), GameRowState(custom, entry = null)),
        )
    }

    // A user-made game has no parser, so its text is not summarized.
    @Test
    fun `a game with no parser is listed as done`() {
        assertEquals(
            "Daily Tracker, Oct 6\n\nMy Game: Done",
            share(row(custom, "My Game 3/5")),
        )
    }

    @Test
    fun `a play that is recorded with no text is listed as done`() {
        assertEquals("Daily Tracker, Oct 6\n\nWordle: Done", share(row(wordle, null)))
    }

    @Test
    fun `text that does not parse is listed as done`() {
        assertEquals("Daily Tracker, Oct 6\n\nWordle: Done", share(row(wordle, "not a result")))
    }
}
