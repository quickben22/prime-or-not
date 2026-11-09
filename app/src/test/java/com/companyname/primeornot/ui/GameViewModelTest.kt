package com.companyname.primeornot.ui

import com.companyname.primeornot.data.ActiveClassicSession
import com.companyname.primeornot.data.CalendarDay
import com.companyname.primeornot.data.ClassicRecords
import com.companyname.primeornot.data.CompletedGame
import com.companyname.primeornot.data.DailyStreak
import com.companyname.primeornot.data.GameStore
import com.companyname.primeornot.data.MAX_RECENT_GAMES
import com.companyname.primeornot.game.CardDeck
import com.companyname.primeornot.game.Guess
import java.time.Instant
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val utc: TimeZone = TimeZone.getTimeZone("UTC")
    private val fixedMillis = Instant.parse("2026-11-09T12:00:00Z").toEpochMilli()
    private var nextId = 0

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        seed: Int = 1,
        store: FakeGameStore = FakeGameStore(clock = { fixedMillis }),
        clock: () -> Long = { fixedMillis },
    ): GameViewModel =
        GameViewModel(
            store = store,
            deck = CardDeck(Random(seed)),
            tickMillis = 50L,
            clockMillis = clock,
            timeZone = utc,
            idFactory = { "game-${nextId++}" },
        ).also {
            it.onResume()
            it.startNewGame()
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

    private fun GameViewModel.playQualifyingGame() {
        repeat(5) { answerCorrectly() }
        answerWrongly()
    }

    @Test
    fun startsIdleUntilANewGameIsPrepared() {
        val viewModel = GameViewModel(
            store = FakeGameStore(clock = { fixedMillis }),
            deck = CardDeck(Random(1)),
            clockMillis = { fixedMillis },
            timeZone = utc,
            idFactory = { "idle" },
        )

        assertEquals(GamePhase.Idle, viewModel.state.value.phase)
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
    fun restartClearsTheRoundAndGivesItANewSession() = runTest(dispatcher) {
        val viewModel = viewModel()
        val firstSession = viewModel.state.value.sessionId
        viewModel.answerWrongly()

        viewModel.restart()

        assertEquals(GamePhase.Playing, viewModel.state.value.phase)
        assertEquals(0, viewModel.state.value.score)
        assertNotNull(viewModel.state.value.card)
        assertEquals(null, viewModel.state.value.gameOverReason)
        assertNotEquals(firstSession, viewModel.state.value.sessionId)
    }

    @Test
    fun bestScoreIsRecordedAndExposed() = runTest(dispatcher) {
        val store = FakeGameStore(clock = { fixedMillis })
        val viewModel = viewModel(seed = 3, store = store)

        repeat(3) { viewModel.answerCorrectly() }
        viewModel.answerWrongly()

        advanceUntilIdle()

        assertEquals(1, store.completed.size)
        assertEquals(3, store.recordsFlow.value.bestScore)
        assertEquals(3, viewModel.state.value.bestScore)
    }

    @Test
    fun roundWaitsForThePlayerToStart() = runTest(dispatcher) {
        val viewModel = GameViewModel(
            store = FakeGameStore(clock = { fixedMillis }),
            deck = CardDeck(Random(9)),
            tickMillis = 50L,
            clockMillis = { fixedMillis },
            timeZone = utc,
            idFactory = { "ready" },
        )
        viewModel.startNewGame()

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
    fun recordsFlowFeedsBestAndCurrentStreak() = runTest(dispatcher) {
        val today = CalendarDay(2026, 11, 9)
        val store = FakeGameStore(
            initial = ClassicRecords(
                bestScore = 7,
                currentDailyStreak = 3,
                longestDailyStreak = 4,
                lastQualifyingDay = today,
            ),
            clock = { fixedMillis },
        )

        val viewModel = viewModel(store = store)
        runCurrent()

        assertEquals(7, viewModel.state.value.bestScore)
        assertEquals(3, viewModel.state.value.dailyStreak)
    }

    @Test
    fun abandoningDiscardsTheSessionWithoutRecordingIt() = runTest(dispatcher) {
        val store = FakeGameStore(clock = { fixedMillis })
        val viewModel = viewModel(store = store)
        repeat(2) { viewModel.answerCorrectly() }
        runCurrent()

        viewModel.abandon()
        advanceUntilIdle()

        assertTrue(store.completed.isEmpty())
        assertNull(store.activeFlow.value)
        assertEquals(GamePhase.Idle, viewModel.state.value.phase)
    }

    @Test
    fun abandoningDoesNotKeepAnUnrecordedBestScore() = runTest(dispatcher) {
        val store = FakeGameStore(
            initial = ClassicRecords(bestScore = 2),
            clock = { fixedMillis },
        )
        val viewModel = viewModel(store = store)
        runCurrent()

        repeat(5) { viewModel.answerCorrectly() }
        assertEquals(5, viewModel.state.value.bestScore)

        viewModel.abandon()
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.bestScore)

        viewModel.startNewGame()
        assertEquals(2, viewModel.state.value.bestScore)
        assertEquals(GamePhase.Ready, viewModel.state.value.phase)
    }

    @Test
    fun activeSessionIsPersistedAndCanBeResumed() = runTest(dispatcher) {
        val store = FakeGameStore(clock = { fixedMillis })
        val viewModel = viewModel(store = store)
        repeat(2) { viewModel.answerCorrectly() }
        runCurrent()

        val saved = requireNotNull(store.activeFlow.value)
        assertEquals(2, saved.score)
        assertEquals(viewModel.state.value.sessionId, saved.id)
        assertEquals(viewModel.state.value.card?.value, saved.cardValue)

        val restored = GameViewModel(
            store = store,
            deck = CardDeck(Random(77)),
            tickMillis = 50L,
            clockMillis = { fixedMillis },
            timeZone = utc,
            idFactory = { "unused" },
        )
        restored.onResume()
        restored.resume(saved)

        assertEquals(GamePhase.Ready, restored.state.value.phase)
        assertEquals(2, restored.state.value.score)
        assertTrue(restored.state.value.isResumed)
        assertEquals(saved.cardValue, restored.state.value.card?.value)
        assertEquals(saved.remainingMs, restored.state.value.remainingMs)

        restored.begin()
        assertEquals(GamePhase.Playing, restored.state.value.phase)
    }

    @Test
    fun qualifyingCompletedGameAdvancesTheStreak() = runTest(dispatcher) {
        val store = FakeGameStore(clock = { fixedMillis })
        val viewModel = viewModel(store = store)

        viewModel.playQualifyingGame()
        advanceUntilIdle()

        val records = store.recordsFlow.value
        assertEquals(1, store.completed.size)
        assertEquals(1, records.totalGames)
        assertEquals(5, records.totalCorrect)
        assertEquals(1, records.totalIncorrect)
        assertEquals(6, records.totalAnswered)
        assertEquals(1, records.currentDailyStreak)
        assertEquals(1, records.longestDailyStreak)
        assertEquals(5, records.longestCorrectStreak)
    }

    @Test
    fun shortCompletedGameIsRecordedButDoesNotQualify() = runTest(dispatcher) {
        val store = FakeGameStore(clock = { fixedMillis })
        val viewModel = viewModel(store = store)

        viewModel.answerWrongly()
        advanceUntilIdle()

        val records = store.recordsFlow.value
        assertEquals(1, store.completed.size)
        assertEquals(1, records.totalGames)
        assertEquals(0, records.totalCorrect)
        assertEquals(1, records.totalIncorrect)
        assertEquals(0, records.currentDailyStreak)
        assertEquals(1, records.recentGames.size)
    }

    @Test
    fun streakAdvancesOnceADayAndSurvivesConsecutiveDays() = runTest(dispatcher) {
        var now = fixedMillis
        val store = FakeGameStore(clock = { now })
        val viewModel = viewModel(store = store, clock = { now })

        viewModel.playQualifyingGame()
        advanceUntilIdle()
        assertEquals(1, store.recordsFlow.value.currentDailyStreak)

        viewModel.restart()
        viewModel.playQualifyingGame()
        advanceUntilIdle()
        assertEquals(1, store.recordsFlow.value.currentDailyStreak)

        now += 1 * 86_400_000L
        viewModel.restart()
        viewModel.playQualifyingGame()
        advanceUntilIdle()
        assertEquals(2, store.recordsFlow.value.currentDailyStreak)

        now += 3 * 86_400_000L
        viewModel.restart()
        viewModel.playQualifyingGame()
        advanceUntilIdle()
        assertEquals(1, store.recordsFlow.value.currentDailyStreak)
        assertEquals(2, store.recordsFlow.value.longestDailyStreak)
    }

    @Test
    fun retryRecordsANewGameAndNeverDuplicatesThePreviousOne() = runTest(dispatcher) {
        val store = FakeGameStore(clock = { fixedMillis })
        val viewModel = viewModel(store = store)
        val firstId = viewModel.state.value.sessionId

        viewModel.playQualifyingGame()
        advanceUntilIdle()
        viewModel.restart()
        val secondId = viewModel.state.value.sessionId
        viewModel.playQualifyingGame()
        advanceUntilIdle()

        assertNotEquals(firstId, secondId)
        assertEquals(2, store.completed.size)
        assertEquals(2, store.completed.map { it.id }.toSet().size)
        assertEquals(2, store.recordsFlow.value.totalGames)
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
        val store = FakeGameStore(
            initial = ClassicRecords(bestScore = 2),
            clock = { fixedMillis },
        )
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

    private class FakeGameStore(
        initial: ClassicRecords = ClassicRecords(),
        private val clock: () -> Long = { 0L },
    ) : GameStore {
        val recordsFlow = MutableStateFlow(initial)
        val activeFlow = MutableStateFlow<ActiveClassicSession?>(null)
        val completed = mutableListOf<CompletedGame>()

        override val records: Flow<ClassicRecords> = recordsFlow
        override val activeSession: Flow<ActiveClassicSession?> = activeFlow

        override suspend fun migrateIfNeeded() = Unit

        override suspend fun completeClassicGame(
            session: ActiveClassicSession,
            completedAtMillis: Long,
        ) {
            if (completed.any { it.id == session.id }) return
            val game = CompletedGame(
                id = session.id,
                completedAtMillis = completedAtMillis,
                score = session.score,
                correct = session.correct,
                incorrect = session.incorrect,
                bestStreak = session.bestStreak,
            )
            completed += game
            activeFlow.value = null

            val current = recordsFlow.value
            val today = CalendarDay.today(completedAtMillis, TimeZone.getTimeZone("UTC"))
            val streak = if (game.qualifiesForStreak) {
                DailyStreak.afterQualifyingGame(
                    current.currentDailyStreak,
                    current.longestDailyStreak,
                    current.lastQualifyingDay,
                    today,
                )
            } else {
                DailyStreak.unchanged(
                    current.currentDailyStreak,
                    current.longestDailyStreak,
                )
            }
            recordsFlow.update {
                it.copy(
                    bestScore = maxOf(it.bestScore, game.score),
                    longestCorrectStreak = maxOf(it.longestCorrectStreak, game.bestStreak),
                    totalGames = it.totalGames + 1,
                    totalCorrect = it.totalCorrect + game.correct,
                    totalIncorrect = it.totalIncorrect + game.incorrect,
                    currentDailyStreak = streak.current,
                    longestDailyStreak = streak.longest,
                    lastQualifyingDay = if (game.qualifiesForStreak) {
                        today
                    } else {
                        it.lastQualifyingDay
                    },
                    recentGames = (listOf(game) + it.recentGames).take(MAX_RECENT_GAMES),
                    lastCompletedGameId = session.id,
                )
            }
        }

        override suspend fun saveActiveSession(session: ActiveClassicSession) {
            activeFlow.value = session
        }

        override suspend fun clearActiveSession(sessionId: String?) {
            if (sessionId == null || activeFlow.value?.id == sessionId) {
                activeFlow.value = null
            }
        }

        override suspend fun setOnboardingComplete(complete: Boolean) {
            recordsFlow.update { it.copy(onboardingComplete = complete) }
        }
    }
}
