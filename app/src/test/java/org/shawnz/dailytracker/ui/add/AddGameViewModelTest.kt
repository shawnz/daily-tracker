package org.shawnz.dailytracker.ui.add

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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.testGame

@OptIn(ExperimentalCoroutinesApi::class)
class AddGameViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    // A game already tracked is never offered, so it cannot be added twice.
    @Test
    fun `a tracked game is left out of the catalog list`() =
        runTest(dispatcher) {
            val tracked = testGame(source = CatalogGame.WORDLE)
            val vm = AddGameViewModel(FakeRepository(games = listOf(tracked)))

            val offered = vm.available.first { it != null }!!

            assertFalse(offered.contains(CatalogGame.WORDLE))
            assertTrue(offered.contains(CatalogGame.CONNECTIONS))
        }

    // A search for a hidden game's exact title lists the game, so the game has to be in this list.
    @Test
    fun `hidden games are kept in the catalog list`() =
        runTest(dispatcher) {
            val vm = AddGameViewModel(FakeRepository())

            val offered = vm.available.first { it != null }!!

            assertTrue(offered.contains(CatalogGame.FOUR_BY_SIX))
        }

    @Test
    fun `adding a game reports that the screen can close`() =
        runTest(dispatcher) {
            val vm = AddGameViewModel(FakeRepository())
            assertFalse(vm.added.value)

            vm.add(CatalogGame.WORDLE).join()

            assertTrue(vm.added.value)
        }

    @Test
    fun `restoring puts an archived game back`() =
        runTest(dispatcher) {
            val archived = testGame().copy(archived = true)
            val repo = FakeRepository(games = listOf(archived))
            val vm = AddGameViewModel(repo)

            vm.restore(archived).join()

            assertFalse(
                repo.games.value
                    .single()
                    .archived,
            )
            assertTrue(vm.added.value)
        }

    // The restored game stays listed while the screen closes.
    @Test
    fun `restoring leaves the lists as they were`() =
        runTest(dispatcher) {
            val archived = testGame().copy(archived = true)
            val vm = AddGameViewModel(FakeRepository(games = listOf(archived)))
            backgroundScope.launch { vm.archived.collect {} }
            vm.archived.first { it != null }

            vm.restore(archived).join()
            runCurrent()

            assertTrue(vm.archived.value == listOf(archived))
        }

    // The picked game stays listed, so it can be tapped again.
    @Test
    fun `a second pick is ignored`() =
        runTest(dispatcher) {
            val first = testGame(id = 1).copy(archived = true)
            val second = testGame(id = 2).copy(archived = true)
            val repo = FakeRepository(games = listOf(first, second))
            val vm = AddGameViewModel(repo)

            vm.restore(first).join()
            vm.restore(second).join()

            assertTrue(repo.games.value.map { it.archived } == listOf(false, true))
        }

    @Test
    fun `only archived games are offered to restore`() =
        runTest(dispatcher) {
            val repo =
                FakeRepository(
                    games = listOf(testGame(id = 1), testGame(id = 2).copy(archived = true)),
                )
            val vm = AddGameViewModel(repo)

            val archived = vm.archived.first { it != null }!!

            assertTrue(archived.map { it.id } == listOf(2L))
        }
}
