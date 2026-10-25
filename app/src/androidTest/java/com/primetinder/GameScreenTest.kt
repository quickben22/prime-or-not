package com.primetinder

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.primetinder.game.GameCard
import com.primetinder.game.Guess
import com.primetinder.ui.GameOverReason
import com.primetinder.ui.GamePhase
import com.primetinder.ui.GameScreen
import com.primetinder.ui.GameUiState
import com.primetinder.ui.theme.PrimeTinderTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsCardScoreAndBest() {
        compose.setContent {
            PrimeTinderTheme {
                GameScreen(
                    state = GameUiState(
                        card = GameCard(97),
                        nextCard = GameCard(91),
                        score = 4,
                        bestScore = 9,
                        remainingMs = 3_000,
                        timeLimitMs = 5_000,
                    ),
                    onSwipe = {},
                    onRestart = {},
                )
            }
        }

        compose.onNodeWithTag("scoreValue").assertTextEquals("4")
        compose.onNodeWithTag("bestValue").assertTextEquals("9")
        compose.onNodeWithTag("card").assertIsDisplayed()
        compose.onNodeWithTag("gameOverPanel").assertDoesNotExist()
    }

    @Test
    fun swipingRightGuessesPrime() {
        var guess: Guess? = null
        compose.setContent {
            PrimeTinderTheme {
                GameScreen(
                    state = GameUiState(
                        card = GameCard(97),
                        nextCard = GameCard(91),
                        remainingMs = 5_000,
                        timeLimitMs = 5_000,
                    ),
                    onSwipe = { guess = it },
                    onRestart = {},
                )
            }
        }

        compose.onNodeWithTag("card").performTouchInput { swipeRight() }
        compose.runOnIdle { assertEquals(Guess.Prime, guess) }
    }

    @Test
    fun swipingLeftGuessesNotPrime() {
        var guess: Guess? = null
        compose.setContent {
            PrimeTinderTheme {
                GameScreen(
                    state = GameUiState(
                        card = GameCard(97),
                        nextCard = GameCard(91),
                        remainingMs = 5_000,
                        timeLimitMs = 5_000,
                    ),
                    onSwipe = { guess = it },
                    onRestart = {},
                )
            }
        }

        compose.onNodeWithTag("card").performTouchInput { swipeLeft() }
        compose.runOnIdle { assertEquals(Guess.NotPrime, guess) }
    }

    @Test
    fun shortSwipeDoesNotCommit() {
        var guess: Guess? = null
        compose.setContent {
            PrimeTinderTheme {
                GameScreen(
                    state = GameUiState(
                        card = GameCard(97),
                        nextCard = GameCard(91),
                        remainingMs = 5_000,
                        timeLimitMs = 5_000,
                    ),
                    onSwipe = { guess = it },
                    onRestart = {},
                )
            }
        }

        compose.onNodeWithTag("card").performTouchInput {
            swipeRight(startX = centerX, endX = centerX + 30f)
        }
        compose.runOnIdle { assertNull(guess) }
    }

    @Test
    fun readyOverlayStartsTheRound() {
        var started = false
        compose.setContent {
            PrimeTinderTheme {
                GameScreen(
                    state = GameUiState(
                        phase = GamePhase.Ready,
                        card = GameCard(97),
                        nextCard = GameCard(91),
                        score = 0,
                        bestScore = 3,
                        remainingMs = 5_000,
                        timeLimitMs = 5_000,
                    ),
                    onSwipe = {},
                    onRestart = {},
                    onStart = { started = true },
                )
            }
        }

        compose.onNodeWithText("TAP TO START").assertIsDisplayed()
        compose.onNodeWithText("TAP TO START").performClick()
        compose.runOnIdle { assertTrue(started) }
    }

    @Test
    fun showsFactorizationOnGameOver() {
        compose.setContent {
            PrimeTinderTheme {
                GameScreen(
                    state = GameUiState(
                        phase = GamePhase.GameOver,
                        card = GameCard(91),
                        nextCard = GameCard(97),
                        score = 12,
                        bestScore = 21,
                        remainingMs = 2_000,
                        timeLimitMs = 5_000,
                        gameOverReason = GameOverReason.WrongGuess,
                        missedCard = GameCard(91),
                    ),
                    onSwipe = {},
                    onRestart = {},
                )
            }
        }

        compose.onNodeWithTag("gameOverPanel").assertIsDisplayed()
        compose.onNodeWithText("91 = 7 × 13").assertIsDisplayed()
        compose.onNodeWithText("Try again").assertIsDisplayed()
    }

    @Test
    fun showsNewBestStreakAndDailyChips() {
        compose.setContent {
            PrimeTinderTheme {
                GameScreen(
                    state = GameUiState(
                        card = GameCard(97),
                        nextCard = GameCard(91),
                        score = 8,
                        bestScore = 7,
                        remainingMs = 4_000,
                        timeLimitMs = 6_000,
                        streak = 4,
                        isNewBest = true,
                        dailyStreak = 3,
                    ),
                    onSwipe = {},
                    onRestart = {},
                )
            }
        }

        compose.onNodeWithTag("newBestBadge").assertTextEquals("NEW BEST")
        compose.onNodeWithTag("streakChip").assertIsDisplayed()
        compose.onNodeWithTag("dailyStreak").assertTextEquals("DAY 3")
    }

    @Test
    fun gameOverSummaryShowsRunStats() {
        compose.setContent {
            PrimeTinderTheme {
                GameScreen(
                    state = GameUiState(
                        phase = GamePhase.GameOver,
                        card = GameCard(91),
                        nextCard = GameCard(97),
                        score = 12,
                        bestScore = 21,
                        remainingMs = 2_000,
                        timeLimitMs = 5_000,
                        gameOverReason = GameOverReason.WrongGuess,
                        missedCard = GameCard(91),
                        bestStreak = 5,
                        cardsSeen = 13,
                        correctCount = 12,
                        fastestAnswerMs = 1_240,
                    ),
                    onSwipe = {},
                    onRestart = {},
                )
            }
        }

        compose.onNodeWithTag("summaryScore").assertTextEquals("12")
        compose.onNodeWithTag("summaryBest").assertTextEquals("21")
        compose.onNodeWithTag("summaryStreak").assertTextEquals("5")
        compose.onNodeWithTag("summaryAccuracy").assertTextEquals("92%")
        compose.onNodeWithTag("summaryFastest").assertTextEquals("1.2s")
        compose.onNodeWithText("10 more to beat your best").assertIsDisplayed()
    }

    @Test
    fun retryBannerIsOnlyShownOnTheGameOverScreen() {
        var state by mutableStateOf(
            GameUiState(
                card = GameCard(97),
                nextCard = GameCard(91),
                remainingMs = 4_000,
                timeLimitMs = 5_000,
            ),
        )

        compose.setContent {
            PrimeTinderTheme {
                GameScreen(
                    state = state,
                    onSwipe = {},
                    onRestart = {},
                    gameOverAd = {
                        Text(text = "AD", modifier = Modifier.testTag("retryAd"))
                    },
                )
            }
        }

        compose.onNodeWithTag("retryAd").assertDoesNotExist()

        compose.runOnIdle {
            state = state.copy(
                phase = GamePhase.GameOver,
                gameOverReason = GameOverReason.TimeUp,
                missedCard = GameCard(97),
            )
        }

        compose.onNodeWithText("Try again").assertIsDisplayed()
        compose.onNodeWithTag("retryAd").assertIsDisplayed()
    }
}
