# Prime Tinder Architecture

A single-screen, native Android game built with Kotlin and Jetpack Compose. The
player swipes number cards: right for prime, left for not prime. Correct guesses
extend the run and add a point; the first wrong guess or timeout ends it.

## Module layout

```
com.primetinder
├── MainActivity.kt        edge-to-edge host, ViewModel + SoundPlayer wiring
├── audio/
│   └── SoundPlayer.kt     SoundPool wrapper, loads res/raw samples
├── game/                  pure Kotlin, no Android imports — unit-testable
│   ├── PrimeChecker.kt    sieve ≤ 100k, trial-division fallback, factorization
│   ├── GameCard.kt        value + isPrime
│   ├── Guess.kt           Prime | NotPrime
│   ├── GameRules.kt       tiers, time limits, correctness
│   └── CardDeck.kt        50/50 deck generation with no repeats
├── data/
│   ├── ScoreStore.kt      stats (best / today / daily streak) + DataStore impl
│   └── DailyStreak.kt     pure streak arithmetic + epoch-day helper
└── ui/
    ├── GameUiState.kt     phase, cards, score, timer, run stats, derived metrics
    ├── GameEffect.kt      one-shot feedback (swipe, correct, wrong, new best…)
    ├── GameViewModel.kt   round state machine, timer, effects, stats
    ├── GameScreen.kt      glass HUD, glow, card stack, +1, banner, confetti
    ├── SwipeCard.kt       drag physics, deal animation, shake, stamps, a11y
    ├── GameOverPanel.kt   answer + run summary + restart
    └── theme/Theme.kt     dark Material 3 scheme, tabular numeric styles
```

The old XML layouts, `GameActivity`, `GameEngine`, `GameRepository`,
`EvaluateGuessUseCase`, and the Livedata/Material-Components dependencies are
gone. The ViewModel replaces the Activity as the owner of the countdown, so
rotation cannot reset a card or a timer.

## Round lifecycle

`GamePhase` is `Ready → Playing → GameOver`. A fresh launch (and every restart
from the game-over panel) begins in `Ready`: the first card is already drawn but
face down behind a "tap to start" card, and the countdown is loaded but not
running. This avoids burning the clock while the first frame is still being
drawn, and lets the player choose when the round begins.

`MainActivity` forwards `onResume`/`onPause` to the ViewModel. The timer is a
foreground-only job: pausing cancels it with the remaining time intact, resuming
re-schedules it from that value. `restart()` skips the ready gate (the player is
already engaged) and drops straight into `Playing`.

## Game rules

- **Direction:** right swipe = prime, left swipe = not prime (matches Tinder's
  right-swipe-for-yes instinct; the old build had it inverted).
- **Run:** one point per correct guess; the first wrong guess or a timeout ends
  the run and a restart starts from zero.
- **Tiers:** score 0–9 → 2–99, 10–24 → 100–999, 25+ → 1,000–9,999.
- **Deck:** the answer is chosen first (coin flip prime/composite), then a value
  is sampled from that tier's pool. This guarantees a 50/50 deck; natural prime
  density would make "not prime" almost always correct at higher ranges.
  From 100 up every even number and every multiple of 5 is excluded, so the
  three- and four-digit tiers are a real primality test instead of "is it even?".
  In 2–99 composites are weighted 70% toward odd, non-multiple-of-5 values.
  Values never repeat within a run.
- **Time:** 5 s for two digits, 6 s for three, 7 s for four.
- **Explanation:** the game-over panel shows the missed card's factorization,
  e.g. `91 = 7 × 13`, or `97 is prime` on a timeout, plus best streak, accuracy,
  fastest answer and how many correct answers would beat the record.

## Data flow

```
SwipeCard ──onGuess──▶ GameViewModel.onSwipe
                         │
                         ├─ correct: score+1, streak+1, promote nextCard, new timer
                         ├─ wrong:   GameOver(WrongGuess)
                         └─ timeout: GameOver(TimeUp)
                         │
              ┌──────────┴───────────┐
     StateFlow<GameUiState>   SharedFlow<GameEffect>
              │                      │
   GameScreen / GameOverPanel   GameScreen feedback + SoundPlayer
```

`GameUiState` is immutable. The timer is a `viewModelScope` coroutine ticking
every 50 ms against `remainingMs`; it starts only when the phase is `Playing`
and the Activity is foreground, is cancelled and restarted per card, and is
cancelled on game over. `CardDeck` precomputes prime/hard/easy pools per tier
from the sieve on first use and tracks used values.

Feedback that must not survive a rotation — sounds, the `+1`, the shake, the
banner, confetti — is published on `GameEffect` (a `SharedFlow` with no replay)
rather than stored in state. Both the screen and the Activity collect it, which
is why it is a broadcast flow and not a channel: two consumers each see every
event. Events are `Swipe`, `Correct(streak)`, `Wrong`, `NewBest`, `TierUp(tier)`,
`Milestone(score)` and `Tick` (one per second inside the last two seconds).

`ScoreStore` owns persistence: a single DataStore holds `best_score`,
`today_score`, `today_epoch_day`, `streak_days` and `last_epoch_day`, exposed as
`stats: Flow<ScoreStats(best, todayBest, dailyStreak)>`. `registerVisit()` runs
once per ViewModel creation and advances or resets the daily streak via the pure
`DailyStreak.next(current, lastPlayedDay, today)`. Day boundaries come from
`epochDayOf(millis)` — integer UTC days from an injectable clock, which avoids
`java.time` (API 26 at minSdk 24) and keeps the streak unit-testable. The
ViewModel takes `ScoreStore` as a constructor dependency, so tests use an
in-memory fake and never touch disk.

## UI

- In `Ready` the stack is replaced by a face-down start card (accent `P`,
  "TAP TO START", direction hints); tapping it calls `begin()`.
- The card stack is three deep: `state.card` on top, `state.nextCard` peeking above
  it at 0.96 scale, and a blank surface at 0.92, each nudged up a few dp so the
  deck reads as endless.
- Horizontal drag sets translation and up to 12° rotation. PRIME/NOT PRIME
  stamps fade in with drag progress, on the side Tinder puts its labels.
- Releasing past 32% of the card width flings the card off-screen, then commits
  the guess; below threshold it springs back. Gestures are ignored when the
  phase is not `Playing`.
- Each new card is dealt: it springs from 0.90 scale and fades in over 110 ms.
  A miss shakes the card three times before the panel arrives.
- HUD is a translucent pill: SCORE, a `3× STREAK` chip from three in a row, a
  NEW BEST badge once the run passes the stored record, BEST, and a `DAY n`
  daily-streak line. Numbers use tabular figures so they never jitter.
- The timer bar animates linearly, turns red under 30%, pulses the seconds text
  on each of the last ticks, and adds a red vignette to the background.
- A slow (20 s) drifting accent glow sits behind the stack.
- Correct answers float a `+1` from the card centre. Tier promotions and
  milestones show a banner; milestones also fire a Canvas confetti burst.
- Haptics: tick when crossing the threshold and on each of the last two seconds,
  CONFIRM on a correct answer, REJECT on a miss (falls back to a long press
  below API 30).
- TalkBack gets a card description plus "Guess prime"/"Guess not prime" custom
  actions.
- The game-over panel is a scrim + surface that springs in with a slight
  overshoot; it shows the answer, score/best, best streak, accuracy, fastest
  answer and "N more to beat your best". The restart button is the only way
  out, so the final card's explanation stays readable.
- Sounds are synthesised WAVs in `res/raw` (`tools/generate_sfx.ps1` regenerates
  them): whoosh on a commit, ding that rises in pitch with the streak, low thud
  on a miss, click in the last two seconds, arpeggio on tier-up/new best/
  milestone. `SoundPlayer` skips playback when media volume is zero or the
  ringer is silent.

## Testing

- `PrimeCheckerTest` — primes, composites, values below 2, above the sieve
  limit, factorization and its formatting.
- `CardDeckTest` — tier ranges, no repeats, answer/type consistency, ~50/50
  prime ratio, the 100+ odd/non-multiple-of-5 rule, and easy composites still
  present in the 2–99 tier.
- `GameRulesTest` — tier thresholds, time limits, correctness mapping.
- `DailyStreakTest` — first visit, same day, yesterday, longer gap, epoch days.
- `GameViewModelTest` — score/advance, wrong-guess end, timeout end, restart,
  best-score recording, ready gate, pause/resume, stats collection, streak
  bookkeeping, effect emission, one-shot new best, tier promotion, fastest
  answer (virtual time via `StandardTestDispatcher`). 35 tests total across the
  five unit suites.
- `GameScreenTest` — instrumented Compose tests for the HUD, ready card,
  game-over factorization and run summary, NEW BEST/streak/daily chips, and
  right/left/short swipe gestures. 8 tests, run via
  `connectedDebugAndroidTest`.

## Build

AGP 9.1.1 with built-in Kotlin (no `kotlin-android` plugin), Kotlin 2.4.21,
Gradle 9.4.1, Compose BOM 2026.09.00, compileSdk/targetSdk 37, minSdk 24,
JVM target 17. Versions live in `gradle/libs.versions.toml`.

The AGP pin is deliberate: Android Studio **2025.3.3 Patch 1** (Panda 3) supports
AGP 7.0–9.1, and AGP 9.1.1 is also Google's minimum for API 37.0 — so
Studio 2025.3.3 Patch 1 + AGP 9.1.1 + compileSdk 37 is a supported combination.
AGP 9.2+ requires Studio 2025.3.4 (Panda 4); upgrading the IDE would be the only
way to raise the plugin. AGP 9.1.1 needs Gradle ≥ 9.3.1, hence 9.4.1.

Command-line builds need a JDK 17+; the machine's default `java` is 11, so use
the Studio JBR:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

## Possible next steps

- Adaptive launcher icon (`mipmap-anydpi-v26`) around the existing vector.
- A mute toggle persisted alongside the score stats.
- Persist the in-progress run across process death if sessions should survive.
- Per-tier leaderboards or a run history if Room becomes worth the dependency.
