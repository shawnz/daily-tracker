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
    private val october16 = LocalDate.of(2026, 10, 16)
    private val october17 = LocalDate.of(2026, 10, 17)

    private fun at(
        time: String,
        zone: ZoneId = newYork,
    ): ZonedDateTime = LocalDateTime.parse(time).atZone(zone)

    private fun saved(
        game: Game,
        puzzleDay: LocalDate,
        time: ZonedDateTime,
    ) = EntryEntity(gameId = game.id, puzzleDay = puzzleDay, completedAt = time.toInstant(), completedZone = time.zone)

    @Test
    fun `a result saved on the device's date is done until midnight`() {
        val entry = saved(queens, october16, at("2026-10-16T22:00"))

        assertEquals(TodayStatus.Done(entry), queens.todayStatus(listOf(entry), at("2026-10-16T23:59:59")))
    }

    @Test
    fun `after midnight, a result from the day before is unreleased until the rollover`() {
        val entry = saved(queens, october16, at("2026-10-16T22:00"))

        assertEquals(
            TodayStatus.Unreleased(Instant.parse("2026-10-17T07:00:00Z")),
            queens.todayStatus(listOf(entry), at("2026-10-17T00:00")),
        )
    }

    // At 01:00 in New York it is still the 16th in Los Angeles, so the puzzle is the 16th's.
    @Test
    fun `a result for the day before that is saved after midnight is done`() {
        val entry = saved(queens, october16, at("2026-10-17T01:00"))

        assertEquals(TodayStatus.Done(entry), queens.todayStatus(listOf(entry), at("2026-10-17T02:00")))
    }

    // The puzzle changes at midnight UTC, which is 20:00 in New York. At 21:00 on the 16th, the
    // puzzle is the 17th's.
    @Test
    fun `a result for the day after that is saved before midnight is unreleased the next day`() {
        val foodGuessr = testGame(id = 4, source = CatalogGame.FOODGUESSR)
        val entry = saved(foodGuessr, october17, at("2026-10-16T21:00"))

        assertEquals(
            TodayStatus.Unreleased(Instant.parse("2026-10-18T00:00:00Z")),
            foodGuessr.todayStatus(listOf(entry), at("2026-10-17T14:00")),
        )
    }

    @Test
    fun `a game with no current entry is unplayed until its rollover`() {
        assertEquals(
            TodayStatus.Unplayed(Instant.parse("2026-10-17T07:00:00Z")),
            queens.todayStatus(emptyList(), at("2026-10-17T00:30")),
        )
    }

    // The rollover time is unknown, so the next puzzle may be out. At 00:30 in New York, it is
    // 23:30 on the 16th in Chicago.
    @Test
    fun `after midnight, a result from the day before is unplayed when the rollover time is unknown`() {
        val raddle = testGame(id = 5, source = CatalogGame.RADDLE)
        val entry = saved(raddle, october17, at("2026-10-16T22:00"))

        assertEquals(TodayStatus.Unplayed(null), raddle.todayStatus(listOf(entry), at("2026-10-17T00:30")))
    }

    // At 06:00 on the 17th in Sydney, it is 14:00 on the 16th in Chicago.
    @Test
    fun `in Sydney, a result from the day before is unplayed when the rollover time is unknown`() {
        val sydney = ZoneId.of("Australia/Sydney")
        val raddle = testGame(id = 5, source = CatalogGame.RADDLE)
        val entry = saved(raddle, october16, at("2026-10-16T20:00", sydney))

        assertEquals(TodayStatus.Unplayed(null), raddle.todayStatus(listOf(entry), at("2026-10-17T06:00", sydney)))
    }

    // The entry was saved at 22:00 on the 16th in Los Angeles, which is 16:00 on the 17th in
    // Sydney. Its date is the 16th, the date in the zone it was saved in.
    @Test
    fun `a result keeps the date of the zone it was saved in`() {
        val sydney = ZoneId.of("Australia/Sydney")
        val entry = saved(queens, october16, at("2026-10-16T22:00", ZoneId.of("America/Los_Angeles")))

        assertEquals(
            TodayStatus.Unreleased(Instant.parse("2026-10-17T07:00:00Z")),
            queens.todayStatus(listOf(entry), at("2026-10-17T17:00", sydney)),
        )
    }

    @Test
    fun `a result with no zone is dated in the device's zone`() {
        val entry = saved(queens, october16, at("2026-10-16T22:00")).copy(completedZone = null)

        assertEquals(TodayStatus.Done(entry), queens.todayStatus(listOf(entry), at("2026-10-16T23:00")))
    }
}
