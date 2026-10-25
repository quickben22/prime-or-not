package com.primetinder.ui

import com.primetinder.game.GameCard

enum class GamePhase {
    Ready,
    Playing,
    GameOver,
}

enum class GameOverReason {
    WrongGuess,
    TimeUp,
}

data class GameUiState(
    val phase: GamePhase = GamePhase.Playing,
    val card: GameCard? = null,
    val nextCard: GameCard? = null,
    val score: Int = 0,
    val bestScore: Int = 0,
    val remainingMs: Long = 0L,
    val timeLimitMs: Long = 5_000L,
    val gameOverReason: GameOverReason? = null,
    val missedCard: GameCard? = null,
) {
    val timeFraction: Float
        get() = if (timeLimitMs <= 0L) 0f else (remainingMs.toFloat() / timeLimitMs).coerceIn(0f, 1f)
}
