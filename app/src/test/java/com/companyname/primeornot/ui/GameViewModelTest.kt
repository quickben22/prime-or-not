package com.companyname.primeornot.ui

import com.companyname.primeornot.data.ScoreStats
import com.companyname.primeornot.data.ScoreStore
import com.companyname.primeornot.game.CardDeck
import com.companyname.primeornot.game.Guess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
import org.junit.Assert.assertTrue
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

    private fun GameViewModel.answerCorrectly() {
        val card = requireNotNull(state.value.card)
        onSwipe(if (card.isPrime) Guess.Prime else Guess.NotPrime)
    }

    private fun GameViewModel.answerWrongly() {
        val card = requireNotNull(state.value.card)
        onSwipe(if (card.isPrime) Guess.NotPrime else Guess.Prime)
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

        viewModel.answerWrongly()

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
        viewModel.answerWrongly()

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

        repeat(3) { viewModel.answerCorrectly() }
        viewModel.answerWrongly()

        advanceUntilIdle()

        assertEquals(3, store.best)
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

    @Test
    fun statsFlowFeedsBestTodayAndDailyStreak() = runTest(dispatcher) {
        val store = FakeScoreStore(initialBest = 7, initialTodayBest = 4, initialStreak = 3)

        val viewModel = viewModel(store = store)
        runCurrent()

        assertEquals(7, viewModel.state.value.bestScore)
        assertEquals(4, viewModel.state.value.todayBest)
        assertEquals(3, viewModel.state.value.dailyStreak)
    }

    @Test
    fun streakTracksBestAndResetsOnMiss() = runTest(dispatcher) {
        val viewModel = viewModel()

        repeat(3) { viewModel.answerCorrectly() }

        assertEquals(3, viewModel.state.value.streak)
        assertEquals(3, viewModel.state.value.bestStreak)
        assertEquals(3, viewModel.state.value.correctCount)
        assertEquals(3, viewModel.state.value.cardsSeen)
        assertEquals(100, viewModel.state.value.accuracyPercent)

        viewModel.answerWrongly()

        assertEquals(0, viewModel.state.value.streak)
        assertEquals(3, viewModel.state.value.bestStreak)
        assertEquals(4, viewModel.state.value.cardsSeen)
        assertEquals(75, viewModel.state.value.accuracyPercent)
        assertEquals(GamePhase.GameOver, viewModel.state.value.phase)
    }

    @Test
    fun correctAnswersEmitCorrectAndSwipeEffects() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = mutableListOf<GameEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects += it }
        }

        repeat(2) { viewModel.answerCorrectly() }

        assertEquals(2, effects.count { it is GameEffect.Swipe })
        val correct = effects.filterIsInstance<GameEffect.Correct>()
        assertEquals(listOf(1, 2), correct.map { it.streak })
    }

    @Test
    fun newBestIsAnnouncedExactlyOnce() = runTest(dispatcher) {
        val store = FakeScoreStore(initialBest = 2)
        val viewModel = viewModel(store = store)
        runCurrent()

        val effects = mutableListOf<GameEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects += it }
        }

        repeat(4) { viewModel.answerCorrectly() }

        assertEquals(1, effects.count { it is GameEffect.NewBest })
        assertTrue(viewModel.state.value.isNewBest)
    }

    @Test
    fun tenthCorrectGuessPromotesTheTierOnce() = runTest(dispatcher) {
        val viewModel = viewModel()
        val effects = mutableListOf<GameEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.effects.collect { effects += it }
        }

        repeat(10) { viewModel.answerCorrectly() }

        assertEquals(1, effects.count { it is GameEffect.TierUp })
        assertEquals(1, effects.filterIsInstance<GameEffect.TierUp>().single().tier)
        assertEquals(1, viewModel.state.value.tier)
        assertTrue(requireNotNull(viewModel.state.value.card).value in 100..999)
        assertTrue(effects.contains(GameEffect.Milestone(10)))
    }

    @Test
    fun fastestAnswerIsTheQuickestCorrectGuess() = runTest(dispatcher) {
        val viewModel = viewModel()

        advanceTimeBy(600)
        runCurrent()
        viewModel.answerCorrectly()

        val fastest = requireNotNull(viewModel.state.value.fastestAnswerMs)
        assertTrue("fastest was $fastest", fastest in 500..650)

        advanceTimeBy(1_500)
        runCurrent()
        viewModel.answerCorrectly()

        assertEquals(fastest, requireNotNull(viewModel.state.value.fastestAnswerMs))
    }

    private class FakeScoreStore(
        initialBest: Int = 0,
        initialTodayBest: Int = 0,
        initialStreak: Int = 0,
    ) : ScoreStore {
        val statsFlow = MutableStateFlow(
            ScoreStats(
                best = initialBest,
                todayBest = initialTodayBest,
                dailyStreak = initialStreak,
            ),
        )

        override val stats: Flow<ScoreStats> = statsFlow

        val best: Int get() = statsFlow.value.best

        override suspend fun recordScore(score: Int) {
            val current = statsFlow.value
            statsFlow.value = current.copy(
                best = maxOf(current.best, score),
                todayBest = maxOf(current.todayBest, score),
            )
        }

        override suspend fun registerVisit(todayEpochDay: Long) = Unit
    }
}
