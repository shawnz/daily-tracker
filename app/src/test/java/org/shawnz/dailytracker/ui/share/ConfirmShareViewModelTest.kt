package org.shawnz.dailytracker.ui.share

import androidx.lifecycle.SavedStateHandle
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.CustomGame
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.share.ConfirmShareActivity
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.raddleText
import org.shawnz.dailytracker.testdoubles.testGame
import org.shawnz.dailytracker.testdoubles.wordleText

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmShareViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val wordle = testGame(id = 1)
    private val other = testGame(id = 2, source = CatalogGame.CONNECTIONS)
    private val today = wordle.possiblePuzzleDays(entries = emptyList()).start

    private fun viewModel(
        repo: FakeRepository,
        sharedText: String = wordleText(today),
        matched: CatalogGame? = CatalogGame.WORDLE,
    ) = ConfirmShareViewModel(
        repo = repo,
        savedStateHandle =
            SavedStateHandle(
                buildMap {
                    put(ConfirmShareActivity.EXTRA_SHARED_TEXT, sharedText)
                    if (matched != null) put(ConfirmShareActivity.EXTRA_MATCHED_GAME, matched.name)
                },
            ),
    )

    @Test
    fun `the day comes from the shared text`() =
        runTest(dispatcher) {
            val threeDaysBack = today.minusDays(3)
            val vm =
                viewModel(
                    FakeRepository(games = listOf(wordle)),
                    sharedText = wordleText(threeDaysBack),
                )

            val state = vm.state.first { it.target != null }

            assertEquals(threeDaysBack, state.day)
        }

    // The current puzzle is the likeliest one being shared.
    @Test
    fun `text that names no day falls back to the current puzzle`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle)), sharedText = "nothing here")

            val state = vm.state.first { it.target != null }

            assertEquals(today, state.day)
        }

    @Test
    fun `a day after the current puzzle falls back to the current puzzle`() =
        runTest(dispatcher) {
            val vm =
                viewModel(
                    FakeRepository(games = listOf(wordle)),
                    sharedText = wordleText(today.plusDays(5)),
                )

            val state = vm.state.first { it.target != null }

            assertEquals(today, state.day)
        }

    @Test
    fun `a shared result for the latest possible puzzle day is kept on that day`() =
        runTest(dispatcher) {
            val raddle = testGame(id = 3, source = CatalogGame.RADDLE)
            val last = raddle.possiblePuzzleDays(entries = emptyList()).endInclusive
            val vm =
                viewModel(
                    FakeRepository(games = listOf(raddle)),
                    sharedText = raddleText(last),
                    matched = CatalogGame.RADDLE,
                )

            val state = vm.state.first { it.target != null }

            assertEquals(last, state.day)
        }

    @Test
    fun `a shared result after the latest possible puzzle day is filed on the earliest`() =
        runTest(dispatcher) {
            val raddle = testGame(id = 3, source = CatalogGame.RADDLE)
            val current = raddle.possiblePuzzleDays(entries = emptyList()).start
            val vm =
                viewModel(
                    FakeRepository(games = listOf(raddle)),
                    sharedText = raddleText(raddle.possiblePuzzleDays(entries = emptyList()).endInclusive.plusDays(1)),
                    matched = CatalogGame.RADDLE,
                )

            val state = vm.state.first { it.target != null }

            assertEquals(current, state.day)
        }

    // The day was worked out for the game that was matched, so it isn't kept for another game.
    @Test
    fun `picking a different game works the day out again`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle, other)))
            vm.pickDay(today.minusDays(5))
            assertEquals(today.minusDays(5), vm.state.first { it.day != null }.day)

            vm.pickTarget(ShareTarget.Tracked(other))

            assertEquals(today, vm.state.first { it.target == ShareTarget.Tracked(other) }.day)
        }

    @Test
    fun `a day already recorded is a clash`() =
        runTest(dispatcher) {
            val existing = EntryEntity(id = 1, gameId = wordle.id, puzzleDay = today)
            val vm = viewModel(FakeRepository(games = listOf(wordle), entries = listOf(existing)))

            val state = vm.state.first { it.target != null }

            assertTrue(state.clash)
            assertEquals(setOf(today), state.playedDays)
        }

    @Test
    fun `an untouched day is no clash`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle)))

            val state = vm.state.first { it.target != null }

            assertFalse(state.clash)
        }

    @Test
    fun `the entries shown are those of the picked game`() =
        runTest(dispatcher) {
            val mine = EntryEntity(id = 1, gameId = other.id, puzzleDay = today)
            val theirs = EntryEntity(id = 2, gameId = wordle.id, puzzleDay = today)
            val repo = FakeRepository(games = listOf(wordle, other), entries = listOf(mine, theirs))
            val vm = viewModel(repo)

            vm.pickTarget(ShareTarget.Tracked(other))

            assertEquals(listOf(mine), vm.state.first { it.target == ShareTarget.Tracked(other) }.entries)
        }

    @Test
    fun `saving writes the result and closes the dialog`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(wordle))
            val vm = viewModel(repo, sharedText = wordleText(today))
            assertFalse(vm.saved.value)

            vm.save(ShareTarget.Tracked(wordle), today).join()

            assertTrue(vm.saved.value)
            val saved = repo.entries.value.single()
            assertEquals(today, saved.puzzleDay)
            assertEquals(wordleText(today), saved.rawShareText)
        }

    // The saved entry is on the chosen day, but it is not shown as a clash.
    @Test
    fun `saving leaves the dialog as it was`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle)))
            backgroundScope.launch { vm.state.collect {} }
            vm.state.first { it.target != null }

            vm.save(ShareTarget.Tracked(wordle), today).join()
            runCurrent()

            assertFalse(vm.state.value.clash)
        }

    @Test
    fun `no game is chosen when the share matched none`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle)), matched = null)

            val state = vm.state.first { it.targets != null }

            assertEquals(null, state.target)
            assertEquals(null, state.day)
        }

    @Test
    fun `a matched game that is not tracked is listed and chosen without being added`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(other))
            val vm = viewModel(repo)

            val state = vm.state.first { it.target != null }

            val new = ShareTarget.New(CatalogGame.WORDLE)
            assertEquals(new, state.target)
            assertEquals(listOf(ShareTarget.Tracked(other), new), state.targets)
            assertEquals(listOf(other), repo.games.value)
        }

    @Test
    fun `saving to a matched game that is not tracked adds it`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(other))
            val vm = viewModel(repo)

            vm.save(ShareTarget.New(CatalogGame.WORDLE), today).join()

            val added = repo.games.value.single { it.catalogGame == CatalogGame.WORDLE }
            assertEquals(added.id, repo.entries.value.single().gameId)
        }

    @Test
    fun `a matched game that is archived is listed with its entries and restored on save`() =
        runTest(dispatcher) {
            val archived = wordle.copy(archived = true)
            val entry = EntryEntity(id = 1, gameId = wordle.id, puzzleDay = today)
            val repo = FakeRepository(games = listOf(archived, other), entries = listOf(entry))
            val vm = viewModel(repo)

            val state = vm.state.first { it.target != null && it.entries.isNotEmpty() }
            assertEquals(listOf(ShareTarget.Tracked(other), ShareTarget.Tracked(archived)), state.targets)
            assertTrue(state.clash)
            assertTrue(repo.games.value.single { it.id == wordle.id }.archived)

            vm.save(ShareTarget.Tracked(archived), today).join()

            assertFalse(repo.games.value.single { it.id == wordle.id }.archived)
        }

    private val custom = ShareTarget.New(CustomGame("Alpha", "https://alpha.example"))

    @Test
    fun `a new user-made game is listed and chosen without being added`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(wordle))
            val vm = viewModel(repo, matched = null)

            vm.pickTarget(custom)

            val state = vm.state.first { it.target != null }
            assertEquals(listOf(ShareTarget.Tracked(wordle), custom), state.targets)
            assertEquals(today, state.day)
            assertEquals(listOf(wordle), repo.games.value)
        }

    @Test
    fun `saving to a new user-made game adds it`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(wordle))
            val vm = viewModel(repo, matched = null)

            vm.save(custom, today).join()

            val added = repo.games.value.single { it.source == custom.source }
            assertEquals(added.id, repo.entries.value.single().gameId)
        }

    @Test
    fun `a picked game that is already listed is listed once`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(other)))

            val new = ShareTarget.New(CatalogGame.WORDLE)

            vm.pickTarget(new)

            val state = vm.state.first { it.target != null }
            assertEquals(listOf(ShareTarget.Tracked(other), new), state.targets)
        }
}
