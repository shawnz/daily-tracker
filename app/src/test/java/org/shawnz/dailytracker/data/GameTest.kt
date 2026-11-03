package org.shawnz.dailytracker.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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

    private val newYork = ZoneId.of("America/New_York")

    // Its puzzle changes at midnight in Los Angeles, which is 03:00 in New York.
    private val queens = testGame(id = 3, source = CatalogGame.QUEENS)

    // Its puzzle changes at midnight UTC, which is 20:00 in New York.
    private val foodGuessr = testGame(id = 4, source = CatalogGame.FOODGUESSR)

    // Its puzzle changes at no fixed hour, and its possible days are the date in Chicago and the
    // day after.
    private val raddle = testGame(id = 5, source = CatalogGame.RADDLE)

    private val october16 = LocalDate.of(2026, 10, 16)
    private val october17 = LocalDate.of(2026, 10, 17)
    private val october18 = LocalDate.of(2026, 10, 18)
    private val october19 = LocalDate.of(2026, 10, 19)

    private fun at(time: String): ZonedDateTime = LocalDateTime.parse(time).atZone(newYork)

    private fun result(
        game: Game,
        puzzleDay: LocalDate,
    ) = EntryEntity(gameId = game.id, puzzleDay = puzzleDay)

    @Test
    fun `a puzzle with a result is done`() {
        val entry = result(queens, october16)

        assertEquals(PuzzleStatus.Done(entry), queens.statusOn(october16, listOf(entry), at("2026-10-17T01:00")))
    }

    // The result was saved on the evening before the puzzle's date, and the puzzle is still done on
    // its date.
    @Test
    fun `a puzzle is done on its date whenever its result was saved`() {
        val entry = result(foodGuessr, october17).copy(completedAt = at("2026-10-16T21:00").toInstant())

        assertEquals(PuzzleStatus.Done(entry), foodGuessr.statusOn(october17, listOf(entry), at("2026-10-17T14:00")))
    }

    // At 01:00 in New York, it is still the 16th in Los Angeles.
    @Test
    fun `the current puzzle is unplayed until the rollover`() {
        assertEquals(
            PuzzleStatus.Unplayed(Instant.parse("2026-10-17T07:00:00Z")),
            queens.statusOn(october16, emptyList(), at("2026-10-17T01:00")),
        )
    }

    // At 21:00 on the 16th in New York, it is already the 17th in UTC.
    @Test
    fun `a puzzle for the day after the device's date can be unplayed`() {
        assertEquals(
            PuzzleStatus.Unplayed(Instant.parse("2026-10-18T00:00:00Z")),
            foodGuessr.statusOn(october17, emptyList(), at("2026-10-16T21:00")),
        )
    }

    @Test
    fun `the next puzzle is unreleased until the rollover`() {
        assertEquals(
            PuzzleStatus.Unreleased(Instant.parse("2026-10-17T07:00:00Z"), currentPuzzleUnplayed = true),
            queens.statusOn(october17, emptyList(), at("2026-10-17T01:00")),
        )
    }

    @Test
    fun `an unreleased puzzle has no earlier unplayed puzzle once the earlier one has a result`() {
        val entries = listOf(result(queens, october16))

        assertEquals(
            PuzzleStatus.Unreleased(Instant.parse("2026-10-17T07:00:00Z"), currentPuzzleUnplayed = false),
            queens.statusOn(october17, entries, at("2026-10-17T01:00")),
        )
    }

    // The 17th's puzzle is released at the rollover at 03:00, not the 18th's.
    @Test
    fun `a puzzle two days ahead is unreleased with no time`() {
        assertEquals(
            PuzzleStatus.Unreleased(null, currentPuzzleUnplayed = true),
            queens.statusOn(october18, emptyList(), at("2026-10-17T01:00")),
        )
    }

    // At 04:00 in New York, it is the 17th in Los Angeles.
    @Test
    fun `a puzzle with no result is missed after the rollover`() {
        assertEquals(
            PuzzleStatus.Missed(currentPuzzleUnplayed = true),
            queens.statusOn(october16, emptyList(), at("2026-10-17T04:00")),
        )
    }

    // At 21:00 on the 17th in New York, it is 20:00 on the 17th in Chicago, so the possible days
    // are the 17th and the 18th.
    @Test
    fun `with an unknown rollover time, every possible day is unplayed with no time`() {
        val now = at("2026-10-17T21:00")

        assertEquals(PuzzleStatus.Unplayed(null), raddle.statusOn(october17, emptyList(), now))
        assertEquals(PuzzleStatus.Unplayed(null), raddle.statusOn(october18, emptyList(), now))
    }

    @Test
    fun `with an unknown rollover time, the day after the last possible day is unreleased`() {
        assertEquals(
            PuzzleStatus.Unreleased(null, currentPuzzleUnplayed = true),
            raddle.statusOn(october19, emptyList(), at("2026-10-17T21:00")),
        )
    }

    // With a result for the 18th, its puzzle is known to be released, so the 17th's puzzle is no
    // longer current.
    @Test
    fun `with an unknown rollover time, a puzzle before a played one is missed`() {
        val entries = listOf(result(raddle, october18))

        assertEquals(
            PuzzleStatus.Missed(currentPuzzleUnplayed = false),
            raddle.statusOn(october17, entries, at("2026-10-17T21:00")),
        )
    }
}
