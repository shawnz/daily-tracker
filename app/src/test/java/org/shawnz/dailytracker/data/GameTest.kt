package org.shawnz.dailytracker.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class GameTest {
    private val custom = testGame(id = 1, source = CustomGame("My Game", "https://example.com"))
    private val now = ZonedDateTime.of(2026, 9, 7, 23, 30, 0, 0, ZoneId.of("America/New_York"))
    private val today = LocalDate.of(2026, 9, 7)
    private val tomorrow = LocalDate.of(2026, 9, 8)

    @Test
    fun `a result for the day after leaves only that day possible`() {
        val entry = EntryEntity(gameId = custom.id, puzzleDay = tomorrow)

        assertEquals(tomorrow..tomorrow, custom.possiblePuzzleDays(listOf(entry), now))
    }

    @Test
    fun `a result for another day or another game leaves both days possible`() {
        val entries =
            listOf(
                EntryEntity(id = 1, gameId = custom.id, puzzleDay = today),
                EntryEntity(id = 2, gameId = custom.id, puzzleDay = tomorrow.plusDays(1)),
                EntryEntity(id = 3, gameId = custom.id + 1, puzzleDay = tomorrow),
            )

        assertEquals(today..tomorrow, custom.possiblePuzzleDays(entries, now))
    }

    // After a move to a time zone further west, a result for tomorrow can be left over. A game
    // that changes at local midnight then has the earlier puzzle again, unplayed.
    @Test
    fun `a result for the day after does not change a game with a known rollover time`() {
        val wordle = testGame(id = 2, source = CatalogGame.WORDLE)
        val entry = EntryEntity(gameId = wordle.id, puzzleDay = tomorrow)

        assertEquals(today..today, wordle.possiblePuzzleDays(listOf(entry), now))
    }

    @Test
    fun `the current entry is this game's entry for the first possible day`() {
        val entry = EntryEntity(id = 1, gameId = custom.id, puzzleDay = today)
        val entries =
            listOf(
                entry,
                EntryEntity(id = 2, gameId = custom.id, puzzleDay = today.minusDays(1)),
                EntryEntity(id = 3, gameId = custom.id + 1, puzzleDay = tomorrow),
            )

        assertEquals(entry, custom.currentEntry(entries, now))
    }

    @Test
    fun `a result for the day after is the current entry`() {
        val entries =
            listOf(
                EntryEntity(id = 1, gameId = custom.id, puzzleDay = today),
                EntryEntity(id = 2, gameId = custom.id, puzzleDay = tomorrow),
            )

        assertEquals(entries[1], custom.currentEntry(entries, now))
    }

    @Test
    fun `a game with no entry for the first possible day has no current entry`() {
        val entries =
            listOf(
                EntryEntity(id = 1, gameId = custom.id, puzzleDay = today.minusDays(1)),
                EntryEntity(id = 2, gameId = custom.id + 1, puzzleDay = today),
            )

        assertNull(custom.currentEntry(entries, now))
    }
}
