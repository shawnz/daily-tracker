package org.shawnz.dailytracker.data.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate

class CatalogGameTest {
    @Test
    fun `a puzzle's address can have its date`() {
        val day = LocalDate.of(2026, 9, 14)

        assertEquals("https://raddle.quest/2026/09/14/", CatalogGame.RADDLE.dayUrl(day))
        assertEquals("https://www.hankgreen.com/fourbythree/#d=2026-09-14", CatalogGame.FOUR_BY_THREE.dayUrl(day))
    }

    // Day 866 is listed on 6 November 2026 in the Catfishing archive.
    @Test
    fun `a puzzle's address can have its number`() {
        assertEquals("https://catfishing.net/game/866", CatalogGame.CATFISHING.dayUrl(LocalDate.of(2026, 11, 6)))
    }

    @Test
    fun `a game with no page for each puzzle has no puzzle address`() {
        assertNull(CatalogGame.WORDLE.dayUrl(LocalDate.of(2026, 9, 14)))
    }

    @Test
    fun `a day before the first puzzle has no puzzle address`() {
        assertNull(CatalogGame.CATFISHING.dayUrl(LocalDate.of(2024, 6, 23)))
    }

    @ParameterizedTest
    @EnumSource(CatalogGame::class)
    fun `a puzzle address has every value it uses`(game: CatalogGame) {
        val url = game.dayUrl(LocalDate.of(2026, 9, 14)) ?: return

        assertFalse(url.contains("null"), url)
    }

    @ParameterizedTest(name = "{0} #{1} was published on {2}")
    @MethodSource("publishedDays")
    fun `a puzzle number is the day that puzzle was published`(
        game: CatalogGame,
        number: Int,
        day: LocalDate,
    ) {
        val numbering = game.numbering!!

        assertEquals(day, numbering.dayOf(number))
        assertEquals(number, numbering.numberOn(day))
    }

    companion object {
        /** Puzzle numbers and their days, from the games themselves and from the parser tests' samples. */
        @JvmStatic
        fun publishedDays() =
            listOf(
                // Launch day, one reported widely at the time, and one seen in the game.
                arguments(CatalogGame.WORDLE, 0, LocalDate.of(2021, 6, 19)),
                arguments(CatalogGame.WORDLE, 500, LocalDate.of(2022, 11, 1)),
                arguments(CatalogGame.WORDLE, 1907, LocalDate.of(2026, 9, 8)),
                arguments(CatalogGame.CONNECTIONS, 1, LocalDate.of(2023, 6, 12)),
                arguments(CatalogGame.CONNECTIONS, 1191, LocalDate.of(2026, 9, 14)),
                arguments(CatalogGame.STRANDS, 1, LocalDate.of(2024, 3, 4)),
                arguments(CatalogGame.STRANDS, 925, LocalDate.of(2026, 9, 14)),
                arguments(CatalogGame.KINDA_HARD_GOLF, 0, LocalDate.of(2025, 4, 2)),
                // Map 552 was the daily map on 6 October 2026.
                arguments(CatalogGame.KINDA_HARD_GOLF, 552, LocalDate.of(2026, 10, 6)),
                // The game's own count of days for 6 November 2026 was 114.
                arguments(CatalogGame.KRILLION, 1, LocalDate.of(2026, 7, 16)),
                arguments(CatalogGame.KRILLION, 114, LocalDate.of(2026, 11, 6)),
                arguments(CatalogGame.PARSEWORD, 1, LocalDate.of(2026, 1, 29)),
                arguments(CatalogGame.PARSEWORD, 229, LocalDate.of(2026, 9, 14)),
                // Daily 1251 has the date 2 November 2026 in the game's archive.
                arguments(CatalogGame.TIMEGUESSR, 1251, LocalDate.of(2026, 11, 2)),
                // The share text states both: "Raddle #567 • Sep 14, 2026".
                arguments(CatalogGame.RADDLE, 567, LocalDate.of(2026, 9, 14)),
                // The Catfishing page for day 100 shows "Day 100 · Oct 1, 2024", and day 866 is
                // listed on 6 November 2026 in the archive.
                arguments(CatalogGame.CATFISHING, 100, LocalDate.of(2024, 10, 1)),
                arguments(CatalogGame.CATFISHING, 866, LocalDate.of(2026, 11, 6)),
                arguments(CatalogGame.QUEENS, 867, LocalDate.of(2026, 9, 14)),
                arguments(CatalogGame.PINPOINT, 873, LocalDate.of(2026, 9, 20)),
                arguments(CatalogGame.ZIP, 546, LocalDate.of(2026, 9, 14)),
                // Built from a guest play of puzzle #187 on 2026-09-20.
                arguments(CatalogGame.PATCHES, 187, LocalDate.of(2026, 9, 20)),
            )
    }
}
