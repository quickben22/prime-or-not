# Prime Tinder Architecture

A single-screen, native Android game built with Kotlin and Jetpack Compose. The
player swipes number cards: right for prime, left for not prime. Correct guesses
extend the run and add a point; the first wrong guess or timeout ends it.

## Module layout

```
com.primetinder
├── MainActivity.kt        edge-to-edge host, wires ViewModel to GameScreen
├── game/                  pure Kotlin, no Android imports — unit-testable
│   ├── PrimeChecker.kt    sieve ≤ 100k, trial-division fallback, factorization
│   ├── GameCard.kt        value + isPrime
│   ├── Guess.kt           Prime | NotPrime
│   ├── GameRules.kt       tiers, time limits, correctness
│   └── CardDeck.kt        50/50 deck generation with no repeats
├── data/
│   └── ScoreStore.kt      ScoreStore interface + DataStore implementation
└── ui/
    ├── GameUiState.kt     phase, cards, score, best, timer, game-over reason
    ├── GameViewModel.kt   round state machine, coroutine timer, best score
    ├── GameScreen.kt      HUD, timer bar, card stack, hint, previews
    ├── SwipeCard.kt       drag physics, stamps, fling, a11y actions
    ├── GameOverPanel.kt   answer explanation, score, best, restart
    └── theme/Theme.kt     dark Material 3 scheme + typography
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
  Composites are weighted 70% toward odd, non-multiple-of-5 values so the game
  tests primality rather than parity. Values never repeat within a run.
- **Time:** 5 s for two digits, 6 s for three, 7 s for four.
- **Explanation:** the game-over panel shows the missed card's factorization,
  e.g. `91 = 7 × 13`, or `97 is prime` on a timeout.

## Data flow

```
SwipeCard ──onGuess──▶ GameViewModel.onSwipe
                         │
                         ├─ correct: score+1, promote nextCard, new timer
                         ├─ wrong:   GameOver(WrongGuess)
                         └─ timeout: GameOver(TimeUp)
                         │
                    StateFlow<GameUiState>
                         │
                    GameScreen / GameOverPanel
```

`GameUiState` is immutable. The timer is a `viewModelScope` coroutine ticking
every 50 ms against `remainingMs`; it starts only when the phase is `Playing`
and the Activity is foreground, is cancelled and restarted per card, and is
cancelled on game over. `CardDeck` precomputes prime/hard/easy pools per tier
from the sieve on first use and tracks used values.

Best score moves through `ScoreStore` (DataStore Preferences, key
`best_score`) and is collected into state. The ViewModel takes `ScoreStore` as a
constructor dependency, so tests use an in-memory fake and never touch disk.

## UI

- In `Ready` the stack is replaced by a face-down start card (accent `P`,
  "TAP TO START", direction hints); tapping it calls `begin()`.
- The top card is `state.card`; `state.nextCard` peeks behind it at 0.94 scale.
- Horizontal drag sets translation and up to 12° rotation. PRIME/NOT PRIME
  stamps fade in with drag progress, on the side Tinder puts its labels.
- Releasing past 32% of the card width flings the card off-screen, then commits
  the guess; below threshold it springs back. Gestures are ignored when the
  phase is not `Playing`.
- The timer bar animates linearly and turns red under 30%.
- Haptics: tick when crossing the threshold, CONFIRM on a correct answer,
  REJECT on a miss (falls back to a long press below API 30).
- TalkBack gets a card description plus "Guess prime"/"Guess not prime" custom
  actions.
- The game-over panel is a scrim + surface; the restart button is the only way
  out, so the final card's explanation stays readable.

## Testing

- `PrimeCheckerTest` — primes, composites, values below 2, above the sieve
  limit, factorization and its formatting.
- `CardDeckTest` — tier ranges, no repeats, answer/type consistency, ~50/50
  prime ratio across seeded decks.
- `GameRulesTest` — tier thresholds, time limits, correctness mapping.
- `GameViewModelTest` — score/advance, wrong-guess end, timeout end, restart,
  best-score recording, ready gate, and pause/resume (virtual time via
  `StandardTestDispatcher`). 22 tests total across the four unit suites.
- `GameScreenTest` — instrumented Compose tests for the HUD, the ready card,
  the game-over panel with factorization, and right/left/short swipe gestures.
  6 tests total, run on a device or emulator via `connectedDebugAndroidTest`.

## Build

AGP 9.2.1 with built-in Kotlin (no `kotlin-android` plugin), Kotlin 2.4.21,
Gradle 9.4.1, Compose BOM 2026.09.00, compileSdk/targetSdk 37, minSdk 24,
JVM target 17. Versions live in `gradle/libs.versions.toml`. The installed
Android Studio 2025.3.4 supports AGP ≤ 9.2; upgrading Studio allows moving to
AGP 9.4 in one line.

Command-line builds need a JDK 17+; the machine's default `java` is 11, so use
the Studio JBR:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

## Possible next steps

- Sound effects via `SoundPool` once audio assets exist (haptics ship today).
- Adaptive launcher icon (`mipmap-anydpi-v26`) around the existing vector.
- Persist the in-progress run across process death if sessions should survive.
- Leaderboard or run history if Room becomes worth the dependency.
