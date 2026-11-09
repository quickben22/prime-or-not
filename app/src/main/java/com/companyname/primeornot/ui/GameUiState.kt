package com.companyname.primeornot.ui

import com.companyname.primeornot.game.GameCard
import com.companyname.primeornot.game.GameRules

enum class GamePhase {
    /** No run is loaded; the app is on another screen. */
    Idle,
    Ready,
    Playing,
    GameOver,
}

enum class GameOverReason {
    WrongGuess,
    TimeUp,
}

data class GameUiState(
    val phase: GamePhase = GamePhase.Idle,
    val sessionId: String = "",
    val card: GameCard? = null,
    val nextCard: GameCard? = null,
    val score: Int = 0,
    val bestScore: Int = 0,
    val remainingMs: Long = 0L,
    val timeLimitMs: Long = 5_000L,
    val gameOverReason: GameOverReason? = null,
    val missedCard: GameCard? = null,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val cardsSeen: Int = 0,
    val correctCount: Int = 0,
    val fastestAnswerMs: Long? = null,
    val isNewBest: Boolean = false,
    val dailyStreak: Int = 0,
    /** True when a saved run was restored and is waiting on the ready gate. */
    val isResumed: Boolean = false,
) {
    val timeFraction: Float
        get() = if (timeLimitMs <= 0L) 0f else (remainingMs.toFloat() / timeLimitMs).coerceIn(0f, 1f)

    val tier: Int
        get() = GameRules.tierForScore(score)

    /** Correct guesses as a percentage of cards answered in this run. */
    val accuracyPercent: Int
        get() = if (cardsSeen <= 0) 0 else correctCount * 100 / cardsSeen

    /** Correct guesses still needed to pass [bestScore]; null once beaten. */
    val cardsToBeatBest: Int?
        get() = when {
            isNewBest -> null
            bestScore <= 0 -> null
            score >= bestScore -> null
            else -> bestScore - score + 1
        }
}
