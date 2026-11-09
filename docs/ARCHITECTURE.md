# Prime or Not Architecture

A native Android game built with Kotlin and Jetpack Compose. The player swipes
number cards in **Classic**: right for prime, left for not prime. Correct guesses
extend the run and add a point; the first wrong guess or timeout ends it. A
five-card, no-pressure **Practice** mode and a **My Records** screen sit
alongside it.

## Module layout

```
com.companyname.primeornot
├── MainActivity.kt        edge-to-edge host, ViewModels + audio + ad consent
├── ads/
│   └── BannerAd.kt        anchored adaptive AdMob banner, Classic game-over only
├── audio/
│   └── SoundPlayer.kt     SoundPool wrapper, loads res/raw samples
├── game/                  pure Kotlin, no Android imports — unit-testable
│   ├── PrimeChecker.kt    sieve ≤ 100k, trial-division fallback, factorization
│   ├── GameCard.kt        value + isPrime
│   ├── Guess.kt           Prime | NotPrime
│   ├── GameRules.kt       tiers, time limits, correctness, max Classic number
│   ├── CardDeck.kt        50/50 deck generation with no repeats
│   └── PracticeDeck.kt    five-card Practice composition + answer mapping
├── data/
│   ├── GameStore.kt       GameStore interface + versioned DataStore impl
│   ├── Models.kt          ClassicRecords, CompletedGame, ActiveClassicSession
│   ├── RecordCodec.kt     pipe-delimited recent-games/session codec
│   └── DailyStreak.kt     local CalendarDay + pure streak arithmetic
└── ui/
    ├── AppRoot.kt         screen state machine, back/abandon dialogs, ads
    ├── AppViewModel.kt    screen, records, active session, onboarding decision
    ├── HomeScreen.kt      menu, resume card, best score and daily streak
    ├── PrimeLogo.kt       shared green 1||0 mark for Home and the Ready card
    ├── MyRecordsScreen.kt four-section Classic records presentation
    ├── HowToPlayScreen.kt onboarding copy, rules and first Practice launch
    ├── PracticeScreen.kt  five-card Practice round and completion screen
    ├── PracticeViewModel.kt isolated Practice state; onboarding flag only
    ├── TeachingTips.kt    reusable divisibility tips + shared tip composable
    ├── GameUiState.kt     phase, cards, score, timer, run stats, derived metrics
    ├── GameEffect.kt      one-shot feedback (swipe, correct, wrong, new best…)
    ├── GameViewModel.kt   Classic state machine, timer, effects, persistence
    ├── GameScreen.kt      glass HUD, glow, card stack, +1, banner, confetti
    ├── SwipeCard.kt       drag physics, deal animation, shake, stamps, a11y
    ├── GameOverPanel.kt   run summary, teaching tip, restart/menu, ad slot
    └── theme/Theme.kt     dark Material 3 scheme, tabular numeric styles
```

The old XML layouts, `GameActivity`, `GameEngine`, `GameRepository`,
`EvaluateGuessUseCase`, and the Livedata/Material-Components dependencies are
gone. The ViewModel replaces the Activity as the owner of the countdown, so
rotation cannot reset a card or a timer.

## Navigation and round lifecycle

The app opens on `Home`. Five destinations are held in `AppUiState.screen`:
`Home`, `Classic`, `Practice`, `HowToPlay` and `Records`. `AppRoot` is a small
explicit state machine rather than Navigation Compose, because the transition
rules (abandon, resume, onboarding) are easier to read in one place.

On first launch, a missing `onboardingComplete` flag opens How to Play and then
the onboarding Practice session. Reaching the end of that five-card session —
answered or skipped — writes the flag. Opening Practice from the menu never
touches it. Existing installations are migrated with `onboardingComplete = true`
as soon as any prior play is detected.

`GamePhase` is `Idle → Ready → Playing → GameOver`. `Idle` means no Classic run
is loaded (the app is on another screen). A fresh Classic run and every restart
from the game-over panel begin in `Ready`: the first card is already drawn but
face down behind a "tap to start" card, and the countdown is loaded but not
running. This avoids burning the clock while the first frame is still being
drawn, and lets the player choose when the round begins.

`MainActivity` forwards `onResume`/`onPause` to the ViewModel. The timer is a
foreground-only job: pausing cancels it with the remaining time intact, resuming
re-schedules it from that value. `restart()` skips the ready gate (the player is
already engaged) and drops straight into `Playing`.

Each run has a unique `sessionId`. The active run is saved after every answer
and on `onPause`, then restored behind the paused `Ready` gate with
`isResumed = true`. Home shows a Resume/Discard card for it. Back from
`Ready`/`Playing` opens an abandon confirmation; a confirmed abandon and a
discarded saved run clear the session without recording a completed game.

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
- **Range:** Classic tops out at `GameRules.MAX_CLASSIC_NUMBER` (9,999), so every
  prime divisor is at most 97. Teaching content is generated from that bound and
  stays correct for the whole game.
- **Practice:** exactly five distinct numbers from 1–100, at least two primes and
  two composites, exactly one trap composite (9, 15, 21, 25, 27, 49 or 91), and
  1 only ever as the fifth card. The onboarding deck always includes 1; menu
  decks include it about half the time. There is no timer, score, streak or ad.

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

`GameStore` owns persistence: one DataStore file (`prime_or_not_scores`) with an
explicit `schema_version`. `ClassicRecords` exposes the best score, longest
correct streak, lifetime totals, current/longest daily streak, the last 20
completed games (newest first) and the onboarding flag. `ActiveClassicSession`
holds the in-progress run. Scalar values are Preferences keys; the recent-games
list and active session are compact pipe-delimited records handled by
`RecordCodec`, whose decoders skip malformed lines instead of crashing.

`migrateIfNeeded()` upgrades version-1 installations: old `streak_days` becomes
`longestDailyStreak`, and it stays the current streak only when the stored
`last_epoch_day` is today or yesterday. `completeClassicGame()` writes the
completed game, updates totals, applies the daily-streak rule and clears the
active session in a single atomic edit. A session id already present in the
records is ignored, so retries and repeated completion callbacks cannot
duplicate a game. A completed game only advances the daily streak when it has
answered at least `STREAK_QUALIFYING_ANSWERS` (5) cards, and only once per local
calendar day.

`DailyStreak` uses a local `CalendarDay` and a time-zone-independent day number,
so "yesterday" remains one day away across daylight-saving changes without
`java.time` (API 26 at minSdk 24). A missed calendar day resets the displayed
current streak while the longest streak is kept permanently. The ViewModel takes
`GameStore` as a constructor dependency, so tests use an in-memory fake and never
touch disk.

## UI

- In `Ready` the stack is replaced by a face-down start card (the green `1||0`
  mark, "TAP TO START", direction hints); tapping it calls `begin()`. A resumed run
  says "TAP TO CONTINUE" and keeps its saved clock paused. A rotating teaching
  tip sits under the swipe hint, and a small "Main menu" action (or system back)
  opens the abandon confirmation.
- Home shows the Classic card with the stored best score and current daily
  streak, a Resume/Discard card when a session exists, and tiles for Practice,
  How to Play and My Records. The privacy-options entry point is also here (and
  on How to Play) when UMP requires it.
- My Records is Classic-only and split into four sections: personal records,
  overall statistics, daily progress and the last 20 completed games. Accuracy
  is shown to one decimal place, or `—` before any number has been answered.
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
  overshoot; it shows the answer, a tip explaining the missed number, best
  streak, accuracy, fastest answer, "N more to beat your best", Try again and
  Main menu. The panel is laid out in a `weight(1f)` area so the optional Classic
  banner can sit below it without ever moving or overlapping the summary; its
  content is `verticalScroll`-able, so a short landscape viewport or a large
  font scale clips nothing.
- Sounds are synthesised WAVs in `res/raw` (`tools/generate_sfx.ps1` regenerates
  them): whoosh on a commit, ding that rises in pitch with the streak, low thud
  on a miss, click in the last two seconds, arpeggio on tier-up/new best/
  milestone. `SoundPlayer` skips playback when media volume is zero or the
  ringer is silent.

## Ads

Monetisation is limited to the Classic game-over screen. `Home`, `Ready`,
`Playing`, `Practice`, `How to Play` and `My Records` render no ad composables at
all, so a 5–7 s round is never interrupted or shortened and Practice stays
ad-free.

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
- When `privacyOptionsRequirementStatus` is `REQUIRED` (EEA/UK), Home and How to
  Play show a small "Privacy options" link that reopens the UMP form via
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
  limit, factorization and its formatting, `primesUpTo` and
  `smallestPrimeFactor`.
- `CardDeckTest` — tier ranges, no repeats, answer/type consistency, ~50/50
  prime ratio, the 100+ odd/non-multiple-of-5 rule, and easy composites still
  present in the 2–99 tier.
- `GameRulesTest` — tier thresholds, time limits, correctness mapping.
- `DailyStreakTest` — first qualifying game, same day, yesterday, longer gap,
  displayed expiry, local calendar-day round trips and time-zone handling.
- `RecordCodecTest` — active-session round trip, malformed input, newest-first
  recent games, the 20-game cap, accuracy and the streak threshold.
- `PracticeDeckTest` — 500 generated decks against every composition rule, trap
  validity, answer mapping for 1–100, and the 97 / 9,999 prime-divisor bound.
- `GameViewModelTest` — score/advance, wrong-guess end, timeout end, restart,
  best-score recording, ready gate, pause/resume, records collection, streak
  bookkeeping and daily thresholds, abandon without recording, session
  persistence/resume, duplicate prevention, effect emission, one-shot new best,
  tier promotion, fastest answer (virtual time via `StandardTestDispatcher`).
  55 tests total across the seven unit suites.
- `GameScreenTest` — instrumented Compose tests for the HUD, ready card,
  game-over factorization and run summary, NEW BEST/streak/daily chips, the
  retry-only ad slot, the ready-screen exit action, the game-over teaching
  tip/Main menu action, and right/left/short swipe gestures. 11 tests, run via
  `connectedDebugAndroidTest` or `am instrument` against a single device.

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
`2.1.2` (`versionCode 8`). `app/build.gradle.kts` keeps both version values as
constants and reads the 2020 upload key from `local.properties` (never
committed):

- `validateReleaseSigning` fails `bundleRelease` without a usable keystore.
- `validateReleaseAdIds` fails it without real AdMob IDs.
- `verifyReleaseBundle` rebuilds, checks the package/version string in the
  bundle manifest, and rejects a bundle signed by anything other than the Play
  upload certificate.

`proguard-rules.pro` keeps `<init>()` on every `RoomDatabase` subclass. Room
2.2.5 arrives transitively through `play-services-ads` → `work-runtime` and
instantiates its generated `WorkDatabase_Impl` by reflection; R8 full mode
otherwise removes the constructor, so `InitializationProvider` crashes the
process before `MainActivity` (this shipped in `versionCode 5`). Always launch
the signed release build before uploading — debug builds do not exercise R8.

The launcher icon is an adaptive icon (`mipmap-anydpi-v26`) with a monochrome
layer, plus legacy PNG mipmaps for API 24–25. Its `1||0` glyphs sit on the
`#00C2A8` app accent, matching `ui/PrimeLogo.kt`. The public privacy policy,
`app-ads.txt` and store copy live under `docs/`; see
[`PLAY_RELEASE.md`](PLAY_RELEASE.md) for the Signing, AdMob and Play Console
runbook.

## Possible next steps

- A mute toggle persisted alongside the records.
- Per-tier leaderboards if Room becomes worth the dependency.
- Optional Daily 10 mode; it would need its own records section and must stay
  out of the Classic statistics.
- Reset to a dedicated upload key (the current one is shared with a 2019
  project; Play App Signing makes this safe but it requires a Play-side reset).
