package com.primetinder.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.primetinder.data.DataStoreScoreStore
import com.primetinder.data.ScoreStore
import com.primetinder.game.CardDeck
import com.primetinder.game.GameRules
import com.primetinder.game.Guess
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var isForeground = false

    init {
        prepareRun()
        viewModelScope.launch {
            scoreStore.bestScore.collect { best ->
                _state.update { it.copy(bestScore = maxOf(best, it.bestScore)) }
            }
        }
    }

    /** Leaves the ready gate and starts the countdown for the first card. */
    fun begin() {
        if (_state.value.phase != GamePhase.Ready) return
        _state.update { it.copy(phase = GamePhase.Playing) }
        startTimer()
    }

    fun onSwipe(guess: Guess) {
        val snapshot = _state.value
        if (snapshot.phase != GamePhase.Playing) return
        val card = snapshot.card ?: return

        if (!GameRules.isCorrect(card, guess)) {
            endRun(GameOverReason.WrongGuess)
            return
        }

        val newScore = snapshot.score + 1
        val promoted = snapshot.nextCard?.takeIf {
            GameRules.tierForScore(newScore) == GameRules.tierForScore(snapshot.score)
        }
        val nextCard = promoted ?: deck.nextCard(newScore)
        val upcoming = deck.nextCard(newScore)

        _state.update {
            it.copy(
                score = newScore,
                card = nextCard,
                nextCard = upcoming,
                gameOverReason = null,
                missedCard = null,
            )
        }
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
            )
        }
    }

    private fun startTimer(newLimit: Long? = null) {
        timerJob?.cancel()
        timerJob = null

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
            }
            if (_state.value.phase == GamePhase.Playing && _state.value.remainingMs <= 0L) {
                endRun(GameOverReason.TimeUp)
            }
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
            )
        }

        viewModelScope.launch { scoreStore.recordScore(snapshot.score) }
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GameViewModel(scoreStore = DataStoreScoreStore(context.applicationContext))
            }
        }
    }
}
