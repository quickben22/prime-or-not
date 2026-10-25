package com.primetinder.ui

import com.primetinder.data.ScoreStore
import com.primetinder.game.CardDeck
import com.primetinder.game.Guess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.random.Random
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(seed: Int = 1, store: ScoreStore = FakeScoreStore()): GameViewModel =
        GameViewModel(
            scoreStore = store,
            deck = CardDeck(Random(seed)),
            tickMillis = 50L,
        ).also {
            it.onResume()
            it.begin()
        }

    @Test
    fun correctGuessScoresAndAdvances() = runTest(dispatcher) {
        val viewModel = viewModel()
        val first = requireNotNull(viewModel.state.value.card)
        val guess = if (first.isPrime) Guess.Prime else Guess.NotPrime

        viewModel.onSwipe(guess)

        assertEquals(1, viewModel.state.value.score)
        assertEquals(GamePhase.Playing, viewModel.state.value.phase)
        assertNotEquals(first.value, viewModel.state.value.card?.value)
    }

    @Test
    fun wrongGuessEndsRun() = runTest(dispatcher) {
        val viewModel = viewModel()
        val first = requireNotNull(viewModel.state.value.card)
        val wrongGuess = if (first.isPrime) Guess.NotPrime else Guess.Prime

        viewModel.onSwipe(wrongGuess)

        assertEquals(GamePhase.GameOver, viewModel.state.value.phase)
        assertEquals(GameOverReason.WrongGuess, viewModel.state.value.gameOverReason)
        assertEquals(first, viewModel.state.value.missedCard)
    }

    @Test
    fun timeoutEndsRun() = runTest(dispatcher) {
        val viewModel = viewModel()
        val limit = viewModel.state.value.timeLimitMs

        advanceTimeBy(limit + 200)
        runCurrent()

        assertEquals(GamePhase.GameOver, viewModel.state.value.phase)
        assertEquals(GameOverReason.TimeUp, viewModel.state.value.gameOverReason)
        assertEquals(0L, viewModel.state.value.remainingMs)
    }

    @Test
    fun restartClearsTheRound() = runTest(dispatcher) {
        val viewModel = viewModel()
        val first = requireNotNull(viewModel.state.value.card)
        viewModel.onSwipe(if (first.isPrime) Guess.NotPrime else Guess.Prime)

        viewModel.restart()

        assertEquals(GamePhase.Playing, viewModel.state.value.phase)
        assertEquals(0, viewModel.state.value.score)
        assertNotNull(viewModel.state.value.card)
        assertEquals(null, viewModel.state.value.gameOverReason)
    }

    @Test
    fun bestScoreIsRecordedAndExposed() = runTest(dispatcher) {
        val store = FakeScoreStore()
        val viewModel = viewModel(seed = 3, store = store)

        repeat(3) {
            val card = requireNotNull(viewModel.state.value.card)
            viewModel.onSwipe(if (card.isPrime) Guess.Prime else Guess.NotPrime)
        }
        val card = requireNotNull(viewModel.state.value.card)
        viewModel.onSwipe(if (card.isPrime) Guess.NotPrime else Guess.Prime)

        advanceUntilIdle()

        assertEquals(3, store.best.value)
        assertEquals(3, viewModel.state.value.bestScore)
    }

    @Test
    fun roundWaitsForThePlayerToStart() = runTest(dispatcher) {
        val viewModel = GameViewModel(
            scoreStore = FakeScoreStore(),
            deck = CardDeck(Random(9)),
            tickMillis = 50L,
        )

        advanceTimeBy(10_000)
        runCurrent()

        assertEquals(GamePhase.Ready, viewModel.state.value.phase)
        assertEquals(viewModel.state.value.timeLimitMs, viewModel.state.value.remainingMs)

        viewModel.onResume()
        viewModel.begin()

        assertEquals(GamePhase.Playing, viewModel.state.value.phase)

        advanceTimeBy(viewModel.state.value.timeLimitMs + 200)
        runCurrent()

        assertEquals(GamePhase.GameOver, viewModel.state.value.phase)
        assertEquals(GameOverReason.TimeUp, viewModel.state.value.gameOverReason)
    }

    @Test
    fun pausingStopsTheCountdownUntilResumed() = runTest(dispatcher) {
        val viewModel = viewModel()
        val remainingAtPause = viewModel.state.value.remainingMs

        viewModel.onPause()
        advanceTimeBy(remainingAtPause + 500)
        runCurrent()

        assertEquals(GamePhase.Playing, viewModel.state.value.phase)
        assertEquals(remainingAtPause, viewModel.state.value.remainingMs)

        viewModel.onResume()
        advanceTimeBy(remainingAtPause + 500)
        runCurrent()

        assertEquals(GamePhase.GameOver, viewModel.state.value.phase)
        assertEquals(GameOverReason.TimeUp, viewModel.state.value.gameOverReason)
    }

    private class FakeScoreStore(initial: Int = 0) : ScoreStore {
        val best = MutableStateFlow(initial)
        override val bestScore: Flow<Int> = best

        override suspend fun recordScore(score: Int) {
            if (score > best.value) best.value = score
        }
    }
}
