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
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.testGame
import org.shawnz.dailytracker.testdoubles.wordleText

@OptIn(ExperimentalCoroutinesApi::class)
class GameDetailViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val game = testGame(id = 7)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(repo: FakeRepository) = GameDetailViewModel(repo = repo, gameId = game.id)

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

    // The day stays played, because only Clear removes the entry.
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
    fun `clearing removes the entry for that day`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(game))
            val vm = viewModel(repo)
            val day = vm.state.first { it.selectedDay != null }.selectedDay!!
            vm.record(wordleText(day)).join()

            vm.clearSelected().join()

            assertTrue(repo.entries.value.isEmpty())
        }

    @Test
    fun `the stepper stops at the current puzzle day`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(game)))
            backgroundScope.launch { vm.state.collect {} }
            val day = vm.state.first { it.selectedDay != null }.selectedDay

            vm.stepDay(1)
            runCurrent()

            assertEquals(day, vm.state.value.selectedDay)
        }

    @Test
    fun `with an unknown rollover time, the stepper stops at the latest possible puzzle day`() =
        runTest(dispatcher) {
            val raddle = testGame(id = game.id, source = CatalogGame.RADDLE)
            val vm = viewModel(FakeRepository(games = listOf(raddle)))
            backgroundScope.launch { vm.state.collect {} }
            vm.state.first { it.selectedDay != null }

            repeat(3) {
                vm.stepDay(1)
                runCurrent()
            }

            assertEquals(raddle.possiblePuzzleDays(rolloverObserved = false).endInclusive, vm.state.value.selectedDay)
        }

    @Test
    fun `a game with an unknown rollover time and a result for its latest possible day opens on that day`() =
        runTest(dispatcher) {
            val raddle = testGame(id = game.id, source = CatalogGame.RADDLE)
            val last = raddle.possiblePuzzleDays(rolloverObserved = false).endInclusive
            val entry = EntryEntity(id = 1, gameId = raddle.id, puzzleDay = last)
            val vm = viewModel(FakeRepository(games = listOf(raddle), entries = listOf(entry)))

            val state = vm.state.first { it.selectedDay != null }

            assertEquals(last, state.selectedDay)
            assertEquals(entry, state.selectedEntry)
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
}
