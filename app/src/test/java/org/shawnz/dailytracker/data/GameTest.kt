package org.shawnz.dailytracker.data

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.LocalDate

class GameTest {
    private val custom = testGame(id = 1, source = CustomGame("My Game", "https://example.com"))
    private val tomorrow = LocalDate.now().plusDays(1)

    @Test
    fun `a result for the day after is an observed rollover`() {
        val entry = EntryEntity(gameId = custom.id, puzzleDay = tomorrow)

        assertTrue(custom.rolloverObserved(listOf(entry)))
    }

    @Test
    fun `a result for another day or another game is not an observed rollover`() {
        val entries =
            listOf(
                EntryEntity(id = 1, gameId = custom.id, puzzleDay = tomorrow.minusDays(1)),
                EntryEntity(id = 2, gameId = custom.id, puzzleDay = tomorrow.plusDays(1)),
                EntryEntity(id = 3, gameId = custom.id + 1, puzzleDay = tomorrow),
            )

        assertFalse(custom.rolloverObserved(entries))
    }

    // After a move to a time zone further west, a result for tomorrow can be left over. A game
    // that changes at local midnight then has the earlier puzzle again, unplayed.
    @Test
    fun `a rollover is not observed for a game with a known rollover time`() {
        val wordle = testGame(id = 2, source = CatalogGame.WORDLE)
        val entry = EntryEntity(gameId = wordle.id, puzzleDay = tomorrow)

        assertFalse(wordle.rolloverObserved(listOf(entry)))
    }
}
