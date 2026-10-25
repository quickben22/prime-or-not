package com.companyname.primeornot.ui

/**
 * One-shot feedback. Deliberately kept out of [GameUiState] so a rotation cannot
 * replay a sound or a burst of confetti; the UI and the audio layer each collect
 * this flow for the lifetime of the screen.
 *
 * The ViewModel publishes these on a broadcast `SharedFlow` with no replay, so
 * both listeners see every event and a late subscriber never sees old ones.
 */
sealed interface GameEffect {
    /** A card was flung; played as a whoosh under the rest of the feedback. */
    data object Swipe : GameEffect

    /** A correct guess. [streak] is 1-based and drives the pitch of the ding. */
    data class Correct(val streak: Int) : GameEffect

    /** A wrong guess or a timeout; the card shakes and the player hears a thud. */
    data object Wrong : GameEffect

    /** The run just passed the stored best score. */
    data object NewBest : GameEffect

    /** The score crossed into a harder number range. */
    data class TierUp(val tier: Int) : GameEffect

    /** 10, 25 or 50 correct in one run. */
    data class Milestone(val score: Int) : GameEffect

    /** A whole second elapsed inside the last two seconds of the clock. */
    data object Tick : GameEffect
}
