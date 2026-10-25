package com.primetinder.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.primetinder.data.DataStoreScoreStore
import com.primetinder.data.ScoreStore
import com.primetinder.data.epochDayOf
import com.primetinder.game.CardDeck
import com.primetinder.game.GameRules
import com.primetinder.game.Guess
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the whole round, including the timer, so rotation and process-level
 * recomposition never restart a card or reset the countdown.
 */
class GameViewModel(
    private val scoreStore: ScoreStore,
    private val deck: CardDeck = CardDeck(),
    private val tickMillis: Long = 50L,
    private val clockMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<GameEffect>(extraBufferCapacity = 8)
    val effects: SharedFlow<GameEffect> = _effects.asSharedFlow()

    private var timerJob: Job? = null
    private var isForeground = false

    /** Displayed second last ticked, so the countdown cannot spam audio. */
    private var lastTickSecond = -1

    /** Best score when the current run started; "new best" is measured against it. */
    private var bestAtRunStart = 0

    init {
        prepareRun()
        viewModelScope.launch { scoreStore.registerVisit(epochDayOf(clockMillis())) }
        viewModelScope.launch {
            scoreStore.stats.collect { stats ->
                _state.update { current ->
                    current.copy(
                        bestScore = maxOf(stats.best, current.bestScore),
                        todayBest = maxOf(stats.todayBest, current.todayBest),
                        dailyStreak = maxOf(stats.dailyStreak, current.dailyStreak),
                    )
                }
                // Freeze the baseline once the player has scored this run; before
                // that, a late-arriving record is still the score to beat.
                if (_state.value.score == 0) {
                    bestAtRunStart = maxOf(bestAtRunStart, stats.best)
                }
            }
        }
    }

    /** Leaves the ready gate and starts the countdown for the first card. */
    fun begin() {
        if (_state.value.phase != GamePhase.Ready) return
        bestAtRunStart = maxOf(bestAtRunStart, _state.value.bestScore)
        _state.update { it.copy(phase = GamePhase.Playing) }
        startTimer()
    }

    fun onSwipe(guess: Guess) {
        val snapshot = _state.value
        if (snapshot.phase != GamePhase.Playing) return
        val card = snapshot.card ?: return
        val answerMs = (snapshot.timeLimitMs - snapshot.remainingMs).coerceAtLeast(0L)
        _effects.tryEmit(GameEffect.Swipe)

        if (!GameRules.isCorrect(card, guess)) {
            _state.update { it.copy(streak = 0, cardsSeen = it.cardsSeen + 1) }
            endRun(GameOverReason.WrongGuess)
            return
        }

        val newScore = snapshot.score + 1
        val newStreak = snapshot.streak + 1
        val oldTier = GameRules.tierForScore(snapshot.score)
        val newTier = GameRules.tierForScore(newScore)
        val tierUp = newTier > oldTier
        val newBest = bestAtRunStart > 0 && newScore > bestAtRunStart

        val nextCard = if (tierUp) {
            deck.nextCard(newScore)
        } else {
            snapshot.nextCard ?: deck.nextCard(newScore)
        }
        val upcoming = deck.nextCard(newScore)

        _state.update {
            it.copy(
                score = newScore,
                card = nextCard,
                nextCard = upcoming,
                gameOverReason = null,
                missedCard = null,
                streak = newStreak,
                bestStreak = maxOf(it.bestStreak, newStreak),
                cardsSeen = it.cardsSeen + 1,
                correctCount = it.correctCount + 1,
                fastestAnswerMs = minOf(it.fastestAnswerMs ?: Long.MAX_VALUE, answerMs)
                    .takeIf { value -> value != Long.MAX_VALUE },
                isNewBest = it.isNewBest || newBest,
                bestScore = maxOf(it.bestScore, newScore),
                todayBest = maxOf(it.todayBest, newScore),
            )
        }

        _effects.tryEmit(GameEffect.Correct(newStreak))
        if (newBest && !snapshot.isNewBest) _effects.tryEmit(GameEffect.NewBest)
        if (tierUp) _effects.tryEmit(GameEffect.TierUp(newTier))
        if (newScore in MILESTONES) _effects.tryEmit(GameEffect.Milestone(newScore))

        startTimer(newLimit = GameRules.timeLimitMsFor(nextCard.value))
    }

    /** Called from the Activity when the screen becomes interactive. */
    fun onResume() {
        isForeground = true
        if (_state.value.phase == GamePhase.Playing) {
            startTimer()
        }
    }

    /** Called when the app leaves the foreground; the countdown should not run in the background. */
    fun onPause() {
        isForeground = false
        timerJob?.cancel()
        timerJob = null
    }

    fun restart() {
        prepareRun()
        begin()
    }

    private fun prepareRun() {
        timerJob?.cancel()
        timerJob = null
        deck.reset()
        bestAtRunStart = _state.value.bestScore

        val first = deck.nextCard(0)
        val upcoming = deck.nextCard(0)
        val limit = GameRules.timeLimitMsFor(first.value)

        _state.update {
            it.copy(
                phase = GamePhase.Ready,
                card = first,
                nextCard = upcoming,
                score = 0,
                remainingMs = limit,
                timeLimitMs = limit,
                gameOverReason = null,
                missedCard = null,
                streak = 0,
                bestStreak = 0,
                cardsSeen = 0,
                correctCount = 0,
                fastestAnswerMs = null,
                isNewBest = false,
            )
        }
    }

    private fun startTimer(newLimit: Long? = null) {
        timerJob?.cancel()
        timerJob = null
        lastTickSecond = -1

        if (newLimit != null) {
            _state.update { it.copy(remainingMs = newLimit, timeLimitMs = newLimit) }
        }
        if (!isForeground) return

        timerJob = viewModelScope.launch {
            while (true) {
                val remaining = _state.value.remainingMs
                if (remaining <= 0L) break
                delay(tickMillis)
                val next = (_state.value.remainingMs - tickMillis).coerceAtLeast(0L)
                _state.update { current ->
                    if (current.phase == GamePhase.Playing) current.copy(remainingMs = next) else current
                }
                maybeTick(next)
            }
            if (_state.value.phase == GamePhase.Playing && _state.value.remainingMs <= 0L) {
                endRun(GameOverReason.TimeUp)
            }
        }
    }

    /** Emits one tick per displayed second inside the final [TICK_WINDOW_MS]. */
    private fun maybeTick(remainingMs: Long) {
        if (remainingMs > TICK_WINDOW_MS) return
        val second = ((remainingMs + 999L) / 1000L).toInt()
        if (second != lastTickSecond) {
            lastTickSecond = second
            _effects.tryEmit(GameEffect.Tick)
        }
    }

    private fun endRun(reason: GameOverReason) {
        val snapshot = _state.value
        if (snapshot.phase == GamePhase.GameOver) return

        timerJob?.cancel()
        timerJob = null

        _state.update {
            it.copy(
                phase = GamePhase.GameOver,
                remainingMs = if (reason == GameOverReason.TimeUp) 0L else it.remainingMs,
                gameOverReason = reason,
                missedCard = it.card,
                bestScore = maxOf(it.bestScore, it.score),
                todayBest = maxOf(it.todayBest, it.score),
            )
        }

        _effects.tryEmit(GameEffect.Wrong)
        viewModelScope.launch { scoreStore.recordScore(snapshot.score) }
    }

    companion object {
        private val MILESTONES = setOf(10, 25, 50)
        private const val TICK_WINDOW_MS = 2_000L

        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GameViewModel(scoreStore = DataStoreScoreStore(context.applicationContext))
            }
        }
    }
}
