package org.shawnz.dailytracker.ui.detail

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.PuzzleStatus
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.testGame
import org.shawnz.dailytracker.testdoubles.wordleText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class GameDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val game = testGame(id = 7)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(
        repo: FakeRepository,
        openingDay: LocalDate = LocalDate.now(),
    ) = GameDetailViewModel(repo = repo, gameId = game.id, openingDay = openingDay)

    @Test
    fun `reads the game it was asked for`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(game, testGame(id = 8))))
            val state = vm.state.first { it.game != null }
            assertEquals(7L, state.game?.id)
        }

    // Archiving and deleting both leave nothing to show, so the screen should close.
    @Test
    fun `archiving closes the page`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            assertFalse(vm.closed.value)

            vm.setArchived(true).join()

            assertTrue(vm.closed.value)
            assertTrue(
                repo.games.value
                    .single()
                    .archived,
            )
        }

    @Test
    fun `deleting closes the page`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)

            vm.delete().join()

            assertTrue(vm.closed.value)
            assertTrue(repo.games.value.isEmpty())
        }

    // The page is blank without its game, so the deleted game stays in the state.
    @Test
    fun `deleting leaves the page as it was`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(game)))
            backgroundScope.launch { vm.state.collect {} }
            vm.state.first { it.game != null }

            vm.delete().join()
            runCurrent()

            assertEquals(game, vm.state.value.game)
        }

    @Test
    fun `recording a result writes it against the selected day`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!

            vm.record(wordleText(day)).join()

            val saved: EntryEntity = repo.entries.value.single()
            assertEquals(day, saved.puzzleDay)
            assertEquals(wordleText(day), saved.rawShareText)
        }

    // The day stays played, because only deleteSelected removes the entry.
    @Test
    fun `recording blank text over a saved result removes the text`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!
            vm.record(wordleText(day)).join()

            vm.record("").join()

            val saved: EntryEntity = repo.entries.value.single()
            assertEquals(day, saved.puzzleDay)
            assertNull(saved.rawShareText)
        }

    @Test
    fun `deleting the result removes the entry for that day`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!
            vm.record(wordleText(day)).join()

            vm.deleteSelected().join()

            assertTrue(repo.entries.value.isEmpty())
        }

    @Test
    fun `a deleted result can be undone`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!
            vm.record(wordleText(day)).join()
            val before = repo.entries.value.single()
            vm.deleteSelected().join()

            val change = vm.undoable.value!!
            assertTrue(change.deleted)
            vm.undo(change).join()

            assertEquals(listOf(before), repo.entries.value)
            assertNull(vm.undoable.value)
        }

    // The replacement is the previous day's result, pasted on the wrong day.
    @Test
    fun `replaced text can be undone`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!
            vm.record(wordleText(day)).join()
            val before = repo.entries.value.single()
            vm.record(wordleText(day.minusDays(1))).join()

            val change = vm.undoable.value!!
            assertFalse(change.deleted)
            vm.undo(change).join()

            assertEquals(listOf(before), repo.entries.value)
        }

    @Test
    fun `removed text can be undone`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!
            vm.record(wordleText(day)).join()

            vm.record("").join()

            assertFalse(vm.undoable.value!!.deleted)
        }

    // Nothing saved is lost, so there is nothing to undo.
    @Test
    fun `no undo is offered for a first result or for the same text again`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!

            vm.record(wordleText(day)).join()
            assertNull(vm.undoable.value)

            vm.record(" ${wordleText(day)}\n").join()
            assertNull(vm.undoable.value)
        }

    @Test
    fun `an earlier change can't be undone once a later one replaces it`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!
            vm.record(wordleText(day)).join()
            vm.record("").join()
            val earlier = vm.undoable.value!!
            vm.deleteSelected().join()
            val later = vm.undoable.value!!

            vm.undo(earlier).join()
            vm.dismissUndo(earlier)

            assertTrue(repo.entries.value.isEmpty())
            assertEquals(later, vm.undoable.value)
        }

    @Test
    fun `dismissing ends the offer to undo`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!
            vm.record(wordleText(day)).join()
            vm.deleteSelected().join()

            vm.dismissUndo(vm.undoable.value!!)

            assertNull(vm.undoable.value)
            assertTrue(repo.entries.value.isEmpty())
        }

    @Test
    fun `the page opens on the day it was opened for`() =
        runTest(dispatcher) {
            val raddle = testGame(id = game.id, source = CatalogGame.RADDLE)
            val last = raddle.possiblePuzzleDays(entries = emptyList()).endInclusive
            val entry = EntryEntity(id = 1, gameId = raddle.id, puzzleDay = last)
            val vm = viewModel(FakeRepository(games = listOf(raddle), entries = listOf(entry)), openingDay = last)

            val state = vm.state.first { it.selectedDay != null }

            assertEquals(last, state.selectedDay)
            assertEquals(entry, state.selected?.entry)
        }

    @Test
    fun `a day after the last possible puzzle day opens as unreleased`() =
        runTest(dispatcher) {
            val later = game.possiblePuzzleDays(entries = emptyList()).endInclusive.plusDays(3)
            val vm = viewModel(FakeRepository(games = listOf(game)), openingDay = later)

            val state = vm.state.first { it.selectedDay != null }

            assertEquals(later, state.selectedDay)
            assertTrue(state.selected?.status is PuzzleStatus.Unreleased)
        }

    @Test
    fun `no result is saved for a puzzle that isn't released yet`() =
        runTest(dispatcher) {
            val later = game.possiblePuzzleDays(entries = emptyList()).endInclusive.plusDays(3)
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo, openingDay = later)
            vm.state.first { it.selectedDay != null }

            vm.record(wordleText(later)).join()

            assertTrue(repo.entries.value.isEmpty())
        }

    @Test
    fun `muting reminders is stored on the game`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)

            vm.setReminders(false).join()

            assertFalse(
                repo.games.value
                    .single()
                    .remindersEnabled,
            )
        }

    private val raddle = testGame(id = 8, source = CatalogGame.RADDLE)
    private val september14 = LocalDate.of(2026, 9, 14)
    private val unplayed = PuzzleStatus.Unplayed(nextRollover = null)

    private fun done(game: Game) = PuzzleStatus.Done(EntryEntity(gameId = game.id, puzzleDay = september14))

    // A game with an unknown rollover time has two possible puzzle days.
    @Test
    fun `play opens the card's puzzle`() {
        val card = DayCard(raddle, september14, unplayed, september14..september14.plusDays(1))

        assertEquals("https://raddle.quest/2026/09/14/", card.playUrl)
    }

    @Test
    fun `play opens the game's page when the game has no page for each puzzle`() {
        val card = DayCard(game, september14, unplayed, september14..september14)

        assertEquals(game.url, card.playUrl)
    }

    @Test
    fun `a done puzzle can be opened and not played`() {
        val card = DayCard(raddle, september14, done(raddle), september14..september14.plusDays(1))

        assertNull(card.playUrl)
        assertEquals("https://raddle.quest/2026/09/14/", card.puzzleUrl)
    }

    @Test
    fun `a done puzzle of a game with no page for each puzzle can't be opened`() {
        val card = DayCard(game, september14, done(game), september14..september14)

        assertNull(card.puzzleUrl)
    }

    @Test
    fun `an unplayed puzzle has no puzzle to open`() {
        val card = DayCard(raddle, september14, unplayed, september14..september14.plusDays(1))

        assertNull(card.puzzleUrl)
    }

    // Its puzzle changes at midnight in New York. At 09:00 on the 17th in Sydney, it is still the
    // 16th in New York, so the puzzle for the 17th isn't out yet.
    @Test
    fun `a day card is computed for the time of the state`() {
        val cluesBySam = testGame(id = 9, source = CatalogGame.CLUES_BY_SAM)
        val now = LocalDateTime.parse("2026-10-17T09:00").atZone(ZoneId.of("Australia/Sydney"))
        val state =
            DetailState(
                game = cluesBySam,
                possiblePuzzleDays = cluesBySam.possiblePuzzleDays(emptyList(), now),
                now = now,
            )

        assertTrue(state.dayCard(LocalDate.of(2026, 10, 17))?.status is PuzzleStatus.Unreleased)
    }
}
