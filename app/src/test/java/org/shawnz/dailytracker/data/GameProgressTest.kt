package org.shawnz.dailytracker.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.LocalDate

/**
 * These games use `LOCAL_MIDNIGHT`, so the current puzzle day is the device date. The
 * helpers below count back from it.
 */
class GameProgressTest {
    /** An entry for the puzzle day [daysAgo] days before today. */
    private fun entry(daysAgo: Long) =
        EntryEntity(
            gameId = 1,
            puzzleDay = LocalDate.now().minusDays(daysAgo),
        )

    @Test
    fun `streak counts back from today`() {
        val progress = gameProgress(testGame(), listOf(entry(0), entry(1), entry(2)))
        assertEquals(3, progress.streak)
    }

    @Test
    fun `an unplayed today does not end a streak`() {
        // Today is not over, so a streak ending yesterday is still unbroken.
        val progress = gameProgress(testGame(), listOf(entry(1), entry(2), entry(3)))
        assertEquals(3, progress.streak)
    }

    @Test
    fun `a missed day ends a streak`() {
        // Day 2 is missing, so only today and yesterday count.
        val progress = gameProgress(testGame(), listOf(entry(0), entry(1), entry(3), entry(4)))
        assertEquals(2, progress.streak)
    }

    @Test
    fun `two unplayed days end a streak`() {
        val progress = gameProgress(testGame(), listOf(entry(2), entry(3)))
        assertEquals(0, progress.streak)
    }

    @Test
    fun `no entries is no streak and no completion`() {
        val progress = gameProgress(testGame(), emptyList())
        assertEquals(0, progress.streak)
        assertEquals(0, progress.plays)
        assertEquals(0f, progress.completion, 0.001f)
    }

    @Test
    fun `a game with no plays at all contributes nothing`() {
        // Adding a game is no evidence that it was played, so there is no window to measure
        // and nothing to add to the overall figure.
        val progress = gameProgress(testGame(), emptyList())
        assertEquals(0, progress.possibleInWindow)
        assertEquals(0, progress.playedInWindow)
    }

    @Test
    fun `a game played before the window but not inside it still counts`() {
        // Played daily until forty days ago, then stopped. Past plays are evidence the game
        // was being tracked, so the empty window is thirty missed days rather than nothing
        // to say. Taking the game off the Today list is what stops it counting.
        val progress = gameProgress(testGame(), (40L until 60L).map { entry(it) })
        assertEquals(COMPLETION_WINDOW_DAYS, progress.possibleInWindow)
        assertEquals(0, progress.playedInWindow)
        assertEquals(0f, progress.completion, 0.001f)
    }

    @Test
    fun `completion is measured from the first play, not from the day added`() {
        // Added today, then the previous nine days recorded. All ten count, even though nine
        // of them are older than the game itself.
        val progress = gameProgress(testGame(), (0L until 10L).map { entry(it) })
        assertEquals(10, progress.possibleInWindow)
        assertEquals(10, progress.playedInWindow)
        assertEquals(1f, progress.completion, 0.001f)
    }

    @Test
    fun `a game played only recently is not judged on days before it started`() {
        // Added a year ago, first played fifteen days ago, played daily since.
        val progress = gameProgress(testGame(), (0L until 15L).map { entry(it) })
        assertEquals(15, progress.possibleInWindow)
        assertEquals(15, progress.playedInWindow)
        assertEquals(1f, progress.completion, 0.001f)
    }

    @Test
    fun `the window never grows past its limit`() {
        // Played every other day for two months. Only the window is measured, and half the
        // days in it have an entry.
        val everyOtherDay = (0L until 60L step 2).map { entry(it) }
        val progress = gameProgress(testGame(), everyOtherDay)
        assertEquals(COMPLETION_WINDOW_DAYS, progress.possibleInWindow)
        assertEquals(COMPLETION_WINDOW_DAYS / 2, progress.playedInWindow)
        assertEquals(0.5f, progress.completion, 0.001f)
    }

    @Test
    fun `plays counts every entry, including those outside the window`() {
        val old = entry(COMPLETION_WINDOW_DAYS + 10L)
        val progress = gameProgress(testGame(), listOf(entry(0), old))
        assertEquals(2, progress.plays)
        assertEquals(1, progress.playedInWindow)
    }
}
