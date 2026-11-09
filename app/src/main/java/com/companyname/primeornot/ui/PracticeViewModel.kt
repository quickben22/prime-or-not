package com.companyname.primeornot.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.companyname.primeornot.data.GameStore
import com.companyname.primeornot.game.GameCard
import com.companyname.primeornot.game.PracticeAnswer
import com.companyname.primeornot.game.PracticeDeck
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class PracticeUiState(
    val cards: List<GameCard> = emptyList(),
    val index: Int = 0,
    val answered: PracticeAnswer? = null,
    val isCorrect: Boolean? = null,
    val skipped: Boolean = false,
    val finished: Boolean = false,
    val correctCount: Int = 0,
) {
    val currentCard: GameCard? get() = cards.getOrNull(index)
    val cardNumber: Int get() = index + 1
    val isLastCard: Boolean get() = index >= PracticeDeck.SIZE - 1
    val answeredCount: Int get() = correctCount + (if (answered != null && isCorrect == false) 1 else 0)
    val progress: Float
        get() = if (cards.isEmpty()) 0f else (index + if (answered != null || skipped) 1 else 0) / cards.size.toFloat()
}

/**
 * Practice is deliberately isolated: it only ever talks to
 * [GameStore.setOnboardingComplete]. It cannot touch Classic statistics, the
 * daily streak, records, or advertising.
 */
class PracticeViewModel(
    private val store: GameStore,
    private val onboarding: Boolean,
    private val random: Random = Random.Default,
) : ViewModel() {

    private val _state = MutableStateFlow(newSession())
    val state: StateFlow<PracticeUiState> = _state.asStateFlow()

    private var onboardingRecorded = false

    val isOnboarding: Boolean get() = onboarding

    fun answer(answer: PracticeAnswer) {
        val snapshot = _state.value
        if (snapshot.finished || snapshot.answered != null) return
        val card = snapshot.currentCard ?: return
        val correctAnswer = PracticeDeck.correctAnswerFor(card)
        val correct = answer == correctAnswer

        _state.update {
            it.copy(
                answered = answer,
                isCorrect = correct,
                skipped = false,
                correctCount = it.correctCount + if (correct) 1 else 0,
            )
        }
        if (snapshot.isLastCard) recordOnboardingComplete()
    }

    /** Skips the current card without recording any answer. */
    fun skip() {
        val snapshot = _state.value
        if (snapshot.finished || snapshot.answered != null) return

        if (snapshot.isLastCard) {
            recordOnboardingComplete()
            _state.update { it.copy(skipped = true, finished = true) }
        } else {
            _state.update {
                it.copy(index = it.index + 1, answered = null, isCorrect = null, skipped = false)
            }
        }
    }

    /** Moves past the answered card. */
    fun next() {
        val snapshot = _state.value
        if (snapshot.finished || snapshot.answered == null) return

        if (snapshot.isLastCard) {
            _state.update { it.copy(finished = true) }
        } else {
            _state.update {
                it.copy(index = it.index + 1, answered = null, isCorrect = null, skipped = false)
            }
        }
    }

    fun restart() {
        _state.value = newSession()
    }

    private fun newSession(): PracticeUiState =
        PracticeUiState(
            cards = PracticeDeck.generate(
                random = random,
                includeOne = onboarding || random.nextBoolean(),
            ),
        )

    /**
     * Reaching the end of the onboarding session — answered or skipped — sets
     * the flag. Replaying Practice from the menu never does.
     */
    private fun recordOnboardingComplete() {
        if (!onboarding || onboardingRecorded) return
        onboardingRecorded = true
        viewModelScope.launch { store.setOnboardingComplete(true) }
    }

    companion object {
        fun factory(store: GameStore, onboarding: Boolean): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    PracticeViewModel(store = store, onboarding = onboarding)
                }
            }
    }
}
