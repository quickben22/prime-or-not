# Prime or Not Architecture

A single-screen, native Android game built with Kotlin and Jetpack Compose. The
player swipes number cards: right for prime, left for not prime. Correct guesses
extend the run and add a point; the first wrong guess or timeout ends it.

## Module layout

```
com.companyname.primeornot
├── MainActivity.kt        edge-to-edge host, ViewModel + audio + ad consent
├── ads/
│   └── BannerAd.kt        anchored adaptive AdMob banner, retry screen only
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
    ├── GameOverPanel.kt   run summary, restart, optional bottom ad slot
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

- **Direction:** right swipe = prime, left swipe = not prime (matches the familiar
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
  "TAP TO START", direction hints); tapping it calls `begin()`. When UMP says
  privacy options are required, a muted "Privacy options" text button under the
  swipe hint reopens the consent form; it is absent everywhere else.
- The card stack is three deep: `state.card` on top, `state.nextCard` peeking above
  it at 0.96 scale, and a blank surface at 0.92, each nudged up a few dp so the
  deck reads as endless.
- Horizontal drag sets translation and up to 12° rotation. PRIME/NOT PRIME
  stamps fade in with drag progress, on the outer edges of the card.
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
  out, so the final card's explanation stays readable. The panel is laid out in
  a `weight(1f)` area so an optional banner slot can sit below it without ever
  moving or overlapping the summary; its content is `verticalScroll`-able, so a
  short landscape viewport or a large font scale clips nothing.
- Sounds are synthesised WAVs in `res/raw` (`tools/generate_sfx.ps1` regenerates
  them): whoosh on a commit, ding that rises in pitch with the streak, low thud
  on a miss, click in the last two seconds, arpeggio on tier-up/new best/
  milestone. `SoundPlayer` skips playback when media volume is zero or the
  ringer is silent.

## Ads

Monetisation is limited to the retry screen. `Ready` and `Playing` render no ad
composables at all, so a 5–7 s round is never interrupted or shortened.

- `GameScreen(gameOverAd = …)` takes an optional slot and forwards it to
  `GameOverPanel(bottomContent = …)`. Both default to `null`, which is why
  previews and Compose tests never touch the network.
- `BannerAd` requests an anchored adaptive banner sized from its own width
  (≥ 320 dp), reserves that height while the request is in flight, and collapses
  to zero on `onAdFailedToLoad`. The `AdView` is destroyed in `onRelease`.
- The banner sits below the panel, outside its surface, full width. It is
  deliberately **not** under the restart button: an ad adjacent to a control is
  the accidental-click pattern AdMob policy warns about.
- `MainActivity` runs the UMP consent flow in the documented order: read
  `canRequestAds()` (a stored decision counts immediately),
  `requestConsentInfoUpdate`, then `loadAndShowConsentFormIfRequired`.
  `MobileAds.initialize` is called at most once, and only after
  `canRequestAds()` is true, so the SDK stays dormant when the user declines.
  A failed consent update re-reads the last known decision instead of silently
  disabling ads for the session.
- When `privacyOptionsRequirementStatus` is `REQUIRED` (EEA/UK), the ready
  screen shows a small "Privacy options" button that reopens the UMP form via
  `showPrivacyOptionsForm`; consent state — and the banner slot — is
  re-evaluated when the form is dismissed.
- Configuration: `play-services-ads` 25.5.0, `user-messaging-platform` 4.0.0,
  `INTERNET`, and the AdMob App ID via the `admobAppId` manifest placeholder.
  The banner unit ID comes from `BuildConfig.ADMOB_BANNER_UNIT_ID`. IDs are set
  per build type: debug uses Google's samples, release reads
  `admob.appId`/`admob.bannerUnitId` from `local.properties` (or
  `ADMOB_APP_ID`/`ADMOB_BANNER_UNIT_ID` Gradle properties or environment).
- `validateReleaseAdIds` fails a release build when either ID is blank or still
  contains Google's sample publisher `3940256099942544`. There is no silent
  fallback; `-PALLOW_TEST_ADS=true` opts into a test-ID release build for local
  checks only.
- The Play listing URL (`https://quickben22.github.io/`) hosts the privacy
  policy and the generated `app-ads.txt`; `tools/write_app_ads_txt.ps1` derives
  the publisher line from `admob.appId` so the two cannot disagree.

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
  game-over factorization and run summary, NEW BEST/streak/daily chips, the
  retry-only ad slot, the conditional privacy-options button, and
  right/left/short swipe gestures. 10 tests, run via `connectedDebugAndroidTest`
  or `am instrument` against a single device.

## Build

AGP 9.1.1 with built-in Kotlin (no `kotlin-android` plugin), Kotlin 2.4.21,
Gradle 9.4.1, Compose BOM 2026.09.00, compileSdk/targetSdk 37, minSdk 24,
JVM target 17, AdMob 25.5.0 + UMP 4.0.0. Versions live in
`gradle/libs.versions.toml`.

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

Release ad IDs are read from `local.properties` (`admob.appId`,
`admob.bannerUnitId`) or `ADMOB_APP_ID`/`ADMOB_BANNER_UNIT_ID` properties or
environment variables. `assembleRelease` fails via `validateReleaseAdIds` when
they are missing or still test IDs; `-PALLOW_TEST_ADS=true` is the local-only
escape hatch. See [`README.md`](../README.md#admob-setup).

## Release

The Play listing is `Prime or Not` / `com.companyname.primeornot`, version
`2.0.0` (`versionCode 3`). `app/build.gradle.kts` keeps both version values as
constants and reads the 2020 upload key from `local.properties` (never
committed):

- `validateReleaseSigning` fails `bundleRelease` without a usable keystore.
- `validateReleaseAdIds` fails it without real AdMob IDs.
- `verifyReleaseBundle` rebuilds, checks the package/version string in the
  bundle manifest, and rejects a bundle signed by anything other than the Play
  upload certificate.

The launcher icon is an adaptive icon (`mipmap-anydpi-v26`) with a monochrome
layer, plus legacy PNG mipmaps for API 24–25. The public privacy policy,
`app-ads.txt` and store copy live under `docs/`; see
[`PLAY_RELEASE.md`](PLAY_RELEASE.md) for the Signing, AdMob and Play Console
runbook.

## Possible next steps

- A mute toggle persisted alongside the score stats.
- Persist the in-progress run across process death if sessions should survive.
- Per-tier leaderboards or a run history if Room becomes worth the dependency.
- Reset to a dedicated upload key (the current one is shared with a 2019
  project; Play App Signing makes this safe but it requires a Play-side reset).
