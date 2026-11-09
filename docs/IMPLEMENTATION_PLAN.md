# Prime or Not — Implementation Plan

**Audience:** Android coder  
**Date:** 8 November 2026  
**Version:** 1.0  
**Companion product document:** [`PRODUCT_MONETIZATION_PLAN.md`](PRODUCT_MONETIZATION_PLAN.md)

---

## 1. Purpose and scope

Implement the product/monetisation plan for the Kotlin + Jetpack Compose app in this repository.

The work is split into three releases:

| Release | Goal | Scope |
|---|---|---|
| **2.1 Revenue core** | Start earning and measuring | Rewarded revive, capped interstitial, remove-ads IAP, settings, stats persistence, analytics |
| **2.2 Daily habit** | Increase retention and installs | Daily 10, share, onboarding, practice, stats/rank/review, in-app review, store update |
| **2.3 Depth** | Optional experiments | Streak save, notifications, themes, Prime Hunt, friend challenge, remote config, localisation, Play Games |

Do **not** start with 2.2 or 2.3. The rewarded revive and interstitial prove the monetisation model with the least UI work; Daily 10 is the long-term growth engine.

### Pre-flight

- The working tree currently has uncommitted icon/branding changes. Commit or stash them before starting.
- Baseline the current build with `testDebugUnitTest`, `lintDebug`, `assembleDebug` and one smoke test on API 24 and API 35.
- Keep the existing package `com.companyname.primeornot` and the existing Play upload key.
- Do not reset user score data on upgrade.

---

## 2. Current code map

```text
app/src/main/java/com/companyname/primeornot/
├── MainActivity.kt          Activity, UMP consent, MobileAds init, banner slot, sound
├── ads/BannerAd.kt          Adaptive banner on game over
├── audio/SoundPlayer.kt     SoundPool wrapper
├── game/
│   ├── PrimeChecker.kt
│   ├── GameCard.kt
│   ├── Guess.kt
│   ├── GameRules.kt
│   └── CardDeck.kt
├── data/
│   ├── ScoreStore.kt        DataStore: best, today, daily streak
│   └── DailyStreak.kt       Pure streak arithmetic
└── ui/
    ├── GameUiState.kt
    ├── GameEffect.kt
    ├── GameViewModel.kt
    ├── GameScreen.kt
    ├── SwipeCard.kt
    ├── GameOverPanel.kt
    └── theme/Theme.kt
```

Important constraints:

- Game logic under `game/` is pure Kotlin and unit-tested. Keep it that way.
- The timer lives in `GameViewModel`, not the Activity.
- `GameScreen` takes an optional `gameOverAd` slot so previews/tests never touch ads.
- `ScoreStore` is injected into `GameViewModel`; tests use an in-memory fake.
- UMP consent is already implemented in `MainActivity`; keep the same order:
  1. read `canRequestAds()`;
  2. `requestConsentInfoUpdate`;
  3. `loadAndShowConsentFormIfRequired`;
  4. initialise MobileAds only when allowed.
- A release build fails without real AdMob IDs; preserve and extend that guard.

---

## 3. Target architecture

```text
MainActivity
├── GameViewModel (classic run state)
├── AdManager (UMP-aware, preloads banner/interstitial/rewarded)
│   └── AdFrequencyPolicy (pure, testable)
├── BillingManager (remove_ads entitlement)
├── AnalyticsLogger (Firebase or no-op)
└── Compose UI
    ├── GameScreen
    │   ├── GameOverPanel (revive, review, share)
    │   ├── SettingsSheet
    │   └── BannerAd (only when no revive offer)
    ├── DailyViewModel / DailyScreen (2.2)
    ├── PracticeViewModel / PracticeScreen (2.2)
    └── StatsSheet (2.2)
```

### Separation rules

- `GameViewModel` must not know about AdMob, Billing or Firebase.
- `GameViewModel` exposes `revive()`, `declineRevive()`, `restart()` and state flags.
- `MainActivity`/Compose orchestrates ads and billing around those callbacks.
- Pure policies (`AdFrequencyPolicy`, `Rank`, `DailyChallengeFactory`, `ShareText`) get unit tests.
- Persist settings, ad caps and daily results in DataStore; keep score data in its existing store.

---

## 4. Release 2.1 — Revenue core

### 4.1 Build and ad configuration

**Files:** `app/build.gradle.kts`, `gradle/libs.versions.toml`, `AndroidManifest.xml`, `local.properties`, `docs/PLAY_RELEASE.md`

Add:

```kotlin
// Debug sample IDs
val admobTestInterstitialUnitId = "ca-app-pub-3940256099942544/1033173712"
val admobTestRewardedUnitId = "ca-app-pub-3940256099942544/5224354917"
```

Release values from `local.properties`:

```properties
admob.interstitialUnitId=ca-app-pub-XXXXXXXXXXXXXXXX/IIIIIIIIII
admob.rewardedUnitId=ca-app-pub-XXXXXXXXXXXXXXXX/RRRRRRRRRR
```

BuildConfig fields:

- `ADMOB_INTERSTITIAL_UNIT_ID`
- `ADMOB_REWARDED_UNIT_ID`

Extend `validateReleaseAdIds` to fail when any of the three IDs is blank or still contains Google’s sample publisher `3940256099942544`. Update `verifyReleaseBundle` to check the new IDs too.

Add Play Billing dependency (latest stable compatible with the project; verify Play policy). Add the billing permission if the library does not merge it:

```xml
<uses-permission android:name="com.android.vending.BILLING" />
```

Bump `versionCode` to **5** and `versionName` to **2.1.0**.

**Acceptance**

- Debug builds use all three sample IDs and work offline.
- Release build fails without real banner/interstitial/rewarded IDs.
- No ad request is made before UMP consent.
- Existing release signing and bundle verification still pass.

---

### 4.2 Data layer

**Files:** `data/ScoreStore.kt`, new `data/PlayerPreferences.kt`, tests

#### Extend `ScoreStats`

```kotlin
data class ScoreStats(
    val best: Int = 0,
    val todayBest: Int = 0,
    val dailyStreak: Int = 0,
    val gamesPlayed: Int = 0,
    val totalCorrect: Int = 0,
    val totalCards: Int = 0,
    val longestStreak: Int = 0,
    val fastestAnswerMs: Long? = null,
    val lastPlayedDay: Long? = null,
)
```

Add methods:

- `recordBestCandidate(score: Int)` — called on every correct answer; updates best/today best so process death cannot lose a record.
- `recordRun(run: RunRecord)` — called once when a run is finalised; increments games played, total correct, total cards, longest streak, fastest answer.
- `registerPlay(todayEpochDay: Long)` — advances the daily streak **only when a run is completed**, not on app open.
- `currentStreakForDisplay(todayEpochDay: Long): Flow<Int>` or derive it in the stats flow: if the last played day is neither today nor yesterday, display 0 until the next play.

`RunRecord` should include:

```kotlin
data class RunRecord(
    val score: Int,
    val cardsSeen: Int,
    val correctCount: Int,
    val bestStreak: Int,
    val fastestAnswerMs: Long?,
    val revivesUsed: Int,
    val reason: String, // wrong_guess | time_up | daily_complete | practice (not recorded)
)
```

#### New `PlayerPreferences`

Use a separate DataStore file, e.g. `prime_or_not_prefs`. Do not create multiple DataStore instances for the same file.

Keys:

- `onboarding_seen` (Boolean)
- `sound_enabled` (Boolean, default true)
- `is_ad_free` (Boolean, cached entitlement)
- `entitlement_last_verified_ms` (Long)
- `last_review_prompt_ms` (Long)
- `ad_total_runs` (Int)
- `ad_runs_since_interstitial` (Int)
- `ad_interstitial_day` (Long)
- `ad_interstitial_count_today` (Int)
- `ad_last_interstitial_ms` (Long)
- `ad_last_rewarded_ms` (Long)
- `daily_result_json` / `daily_in_progress_json` / `daily_history_json` (2.2)
- `notifications_enabled` (2.3)
- `theme_id` (2.3)

Expose immutable flows and suspend update methods. Keep score data in `ScoreStore`.

**Acceptance**

- Upgrade from 2.0.1 preserves best score, today score and streak.
- A new best is persisted even if the app is killed mid-run.
- Opening the app alone does **not** advance the daily streak.
- A completed run advances it exactly once.
- Tests cover same day, yesterday, multi-day gap, first play and display reset.

---

### 4.3 `AdManager` and frequency policy

**New files:** `ads/AdManager.kt`, `ads/AdFrequencyPolicy.kt`

#### `AdFrequencyPolicy` (pure Kotlin)

Inputs:

```kotlin
data class AdFrequencyInput(
    val totalCompletedRuns: Int,
    val runsSinceInterstitial: Int,
    val interstitialsToday: Int,
    val lastInterstitialMs: Long,
    val lastRewardedMs: Long,
    val nowMs: Long,
    val isNewBest: Boolean,
    val reviveOfferShownThisRun: Boolean,
    val isAdFree: Boolean,
)
```

Defaults:

- `MIN_LIFETIME_RUNS = 3` — first three completed runs are always ad-free.
- `RUNS_BETWEEN_INTERSTITIALS = 3`.
- `MIN_INTERVAL_MS = 90_000`.
- `DAILY_CAP = 4`.
- `REWARDED_COOLDOWN_MS = 120_000`.

`shouldShowInterstitial` returns false when:

- `isAdFree` is true;
- lifetime runs < 3;
- runs since last interstitial < 3;
- interstitials today >= 4;
- now - last interstitial < 90 s;
- now - last rewarded < 120 s;
- `isNewBest` is true;
- `reviveOfferShownThisRun` is true.

Add unit tests for every boundary.

#### `AdManager`

Responsibilities:

- Hold `InterstitialAd?` and `RewardedAd?`.
- Preload only after UMP consent and `MobileAds.initialize` (once).
- Preload again after each dismissal and after a failure with backoff.
- Never request or show an ad in Ready, Playing, Practice or Daily.
- Expose:
  - `interstitialReady: Boolean`
  - `rewardedReady: Boolean`
  - `maybeShowInterstitial(activity, onDismiss: () -> Unit)`
  - `showRewarded(activity, onResult: (earned: Boolean) -> Unit)`
- On interstitial:
  - If not due or not loaded: call `onDismiss()` immediately.
  - If shown: record state, then on dismiss preload and call `onDismiss()`.
  - On failed-to-show: call `onDismiss()`, preload.
- On rewarded:
  - If not loaded: `onResult(false)` immediately; UI hides the offer.
  - Track `onUserEarnedReward`; call `onResult(true)` only after dismiss if earned.
  - If dismissed early: `onResult(false)`.
  - Record `lastRewardedMs` when the ad is shown.
- Destroy/clear references on Activity destruction; never hold an Activity longer than needed.
- Use `Activity` only at show time; use application context for loading.
- Log analytics events from `MainActivity` or a wrapper, not inside the pure policy.

**Acceptance**

- Debug test ads show correctly on API 24 and API 35.
- No ads before consent.
- Offline play never crashes and never blocks a retry.
- Frequency policy unit tests pass.
- Banner, interstitial and rewarded all reload after dismissal.
- No ad is requested while the app is in the background.

---

### 4.4 Rewarded revive

**Files:** `ui/GameViewModel.kt`, `ui/GameUiState.kt`, `ui/GameOverPanel.kt`, `ui/GameScreen.kt`, `MainActivity.kt`, tests

#### State

Add to `GameUiState`:

```kotlin
val reviveOffered: Boolean = false,
val revivesUsed: Int = 0,
val runFinalized: Boolean = true,
val runAnswers: List<RunAnswer> = emptyList(),
```

`RunAnswer`:

```kotlin
data class RunAnswer(
    val value: Int,
    val guess: Guess,
    val correct: Boolean,
    val timedOut: Boolean,
    val answerMs: Long,
)
```

#### Flow

```text
Wrong guess / timeout
        │
        ├── revive available (classic, revivesUsed == 0)
        │       → phase GameOver, reviveOffered = true, do not finalize
        │       → [Watch ad] → reward → score preserved, streak = 0,
        │                       new card, full timer, revivesUsed = 1
        │       → [Try again] → declineRevive() → finalize run → restart
        │       → [Leave] → pending run is finalised on next launch
        │
        └── no revive available
                → finalize run → game-over panel
```

#### `GameViewModel` changes

- On a correct guess:
  - update score, streak, best streak, cards seen, correct count, fastest;
  - append `RunAnswer`;
  - persist `recordBestCandidate(newScore)` immediately.
- On a wrong guess or timeout:
  - append `RunAnswer`;
  - increment cards seen;
  - set phase `GameOver`, reason and missed card;
  - if `revivesUsed == 0` and classic: `reviveOffered = true`, `runFinalized = false`;
  - else `finalizeRun()`.
- `revive()`:
  - guard `phase == GameOver && reviveOffered && revivesUsed == 0`;
  - `revivesUsed = 1`, `reviveOffered = false`, `runFinalized = false`;
  - preserve score; reset current streak to 0 (best streak remains);
  - clear game-over fields; deal next card; reset timer; phase `Playing`;
  - do not record the run yet.
- `declineRevive()`:
  - `reviveOffered = false`; `finalizeRun()`.
- `finalizeRun()`:
  - idempotent (`runFinalized` guard);
  - `scoreStore.recordRun(...)`, `scoreStore.registerPlay(...)`;
  - set `runFinalized = true`.
- `restart()`:
  - if not finalised, finalise first;
  - prepare new run and begin.
- Persist a minimal `PendingRun` snapshot at first failure (score, cardsSeen, correctCount, bestStreak, runId) so process death does not lose the run. On app start, if a pending run exists, finalise it and clear it. If the player revives, clear the pending snapshot; best score is already persisted incrementally.

**Acceptance**

- Exactly one revive per run.
- Score is preserved; current streak resets; missed card remains in review/accuracy.
- The next card gets a full timer.
- No revive in Daily or Practice.
- Ad failure hides the offer and leaves “Try again” usable.
- Declining or restarting finalises the run exactly once.
- Process death during the revive offer does not lose the run.
- Unit tests cover every branch, including timeout, wrong guess, new best and restart.

#### `GameOverPanel` UI

Add a `ReviveState`:

```kotlin
sealed interface ReviveState {
    data object Hidden : ReviveState
    data object Available : ReviveState
    data object Loading : ReviveState
    data object Failed : ReviveState
}
```

Behavior:

- `Available`:
  - Primary button: **Keep this run** with a small **AD** badge and subtitle “Watch a short ad.”
  - Secondary: **Try again** (outlined/text).
  - Banner hidden while the offer is visible.
  - Text action: **No thanks** or simply “Try again.”
- `Loading`: button disabled, label “Loading ad…”; “Try again” still enabled.
- `Failed`/`Hidden`: normal panel; **Try again** is primary.
- Add **Review run** and **Share** as text actions.
- Keep the panel scrollable; do not let the banner overlap the summary.
- Content descriptions: “Watch a short ad to keep this run”, “End this run and try again.”
- Do not auto-focus the ad button.

---

### 4.5 Capped interstitial

**Files:** `MainActivity.kt`, `GameScreen.kt`, `AdManager.kt`, `PlayerPreferences.kt`

- `MainActivity` passes `onRestart` as:

```kotlin
onRestart = {
    adManager.maybeShowInterstitial(this) { viewModel.restart() }
}
```

- On run finalisation, increment `ad_total_runs` and `ad_runs_since_interstitial`.
- On interstitial shown, reset `ad_runs_since_interstitial`, increment daily count, set `ad_last_interstitial_ms`.
- If the ad is not loaded, restart immediately.
- Do not show an interstitial if `state.reviveOffered` was true for that run. Pass `reviveOfferShownThisRun` (or a flag from the ViewModel) to the policy.
- Do not show an interstitial on the Daily 10 result.
- Do not show an interstitial after a new best.
- Do not show an interstitial when the user is ad-free.

**Acceptance**

- First three lifetime runs never show an interstitial.
- After that, no more than one per three completed runs.
- 90-second minimum and four-per-day cap enforced across app restarts.
- Skipped after new best, revive offer, recent rewarded ad and remove-ads purchase.
- No wait or blank screen if ad is missing.

---

### 4.6 Remove-ads IAP

**New files:** `billing/BillingManager.kt`, `ui/SettingsSheet.kt`; modify `MainActivity.kt`, `PlayerPreferences.kt`, `app/build.gradle.kts`

#### Product

- ID: `remove_ads`
- Type: non-consumable one-time purchase.
- Recommended price: **$3.99**.
- Removes banner and interstitial.
- Rewarded revive remains optional for everyone.
- Optional later perk: one free revive per day for purchasers. Do not block 2.1 on it.

#### `BillingManager`

- Connect with `BillingClient` using `enablePendingPurchases()`.
- Query `ProductDetails` for `remove_ads`; expose formatted price.
- Query purchases on connection and on app resume.
- Purchase flow:
  - launch billing flow from the Activity;
  - on `PURCHASED`, verify product, acknowledge if needed, set entitlement;
  - cache `is_ad_free` and `entitlement_last_verified_ms`;
  - hide ads immediately.
- Handle:
  - `ITEM_ALREADY_OWNED` → restore;
  - pending purchases → show “Purchase pending” and re-check on resume;
  - user cancel → no error dialog, just return;
  - billing unavailable → use cached entitlement; restore button still works when connected;
  - refund/revocation → query on next launch/resume and turn ads back on.
- Do not consume the purchase.
- Do not store payment data; Google handles it.

#### UI

- Settings row:
  - “Remove ads — $3.99” if not owned;
  - “Ads removed ✓” if owned;
  - “Restore purchases” row.
- Contextual text link after the second interstitial, not on the first screen. Keep it a text link, not a giant button.
- Copy: “Removes banner and full-screen ads. Optional reward ads remain available for a second chance.”
- No countdowns, fake discounts or pressure tactics.

**Acceptance**

- License-tester purchase removes banner and interstitial immediately.
- Restore works after reinstall and on a second device with the same Play account.
- Refund/revocation restores ads.
- Pending/cancel/offline do not crash.
- Product price is read from Play, not hardcoded in UI.
- `isAdFree` survives process death.

---

### 4.7 Settings and sound

**Files:** `ui/SettingsSheet.kt`, `MainActivity.kt`, `audio/SoundPlayer.kt`, `PlayerPreferences.kt`

Settings rows:

- Sound on/off (persisted; apply immediately).
- Privacy options (only when UMP `privacyOptionsRequirementStatus == REQUIRED`).
- Remove ads / Ads removed / Restore purchases.
- How to play.
- Stats (2.2).
- Version name and privacy-policy link.
- Optional daily reminder toggle (2.3).

Placement:

- Accessible from the Ready screen footer: Stats · How to play · Settings.
- Keep the UMP entry point visible; do not hide it behind a long scroll.
- Do not add a bottom navigation bar.

**Acceptance**

- Sound preference persists and is respected by `SoundPlayer`.
- Privacy options reopen the UMP form and re-evaluate consent.
- Settings is accessible before the first run.
- TalkBack labels and 48dp touch targets.

---

### 4.8 Analytics and Crashlytics

**New file:** `analytics/AnalyticsLogger.kt`

Interface:

```kotlin
interface AnalyticsLogger {
    fun log(event: String, params: Map<String, Any> = emptyMap())
    fun setUserProperty(name: String, value: String)
}
```

Implementation:

- Firebase Analytics + Crashlytics if approved.
- A no-op implementation for tests/previews.
- Initialize after UMP consent; do not collect before the approved consent state.
- No PII: no scores tied to identity, no contacts, no location, no ad IDs in custom events.

Event table:

| Event | When | Parameters |
|---|---|---|
| `run_start` | Classic run begins | `mode` |
| `run_end` | Run finalised | `score`, `best`, `best_streak`, `cards_seen`, `accuracy`, `reason`, `duration_ms`, `revives_used` |
| `revive_offer` | Revive panel shown | `score`, `reason` |
| `revive_watch` | Rewarded ad completed | `score` |
| `revive_decline` | Player chooses retry | `score` |
| `interstitial_shown` | Interstitial shown | `runs_since`, `total_runs` |
| `purchase_started` | Billing flow launched | `product` |
| `purchase_success` | Entitlement granted | `product`, `price` |
| `purchase_failed` | Billing error | `code` |
| `purchase_restored` | Restore succeeds | `product` |
| `daily_start` | Daily 10 starts | `day` |
| `daily_complete` | Daily 10 result saved | `day`, `score`, `duration_ms` |
| `daily_share` | Share tapped | `day`, `score` |
| `onboarding_seen` | First-run sheet shown | — |
| `practice_start` | Practice begins | — |
| `practice_complete` | Practice ends | `score` |
| `review_prompt` | In-app review requested | `trigger` |
| `settings_open` | Settings opened | — |

**Acceptance**

- Events visible in Firebase DebugView.
- No event fires before consent.
- Crashlytics captures a test crash in debug only.
- Privacy policy and Data safety updated before release.

---

### 4.9 Release 2.1 QA

Run:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Manual matrix:

- Debug build with test ads: banner, interstitial, rewarded.
- EEA consent reset: no ads before consent; privacy options in Settings; consent withdrawal works.
- Revive: wrong answer, timeout, ad complete, ad dismissed early, ad failed, decline, restart, process death.
- Interstitial: first three runs ad-free; fourth/fifth/sixth behavior; daily cap; new-best skip.
- Billing: purchase, pending, cancel, restore, refund, offline.
- Rotation during run, revive offer and ad.
- API 24 and API 35; small screen; large font scale; TalkBack.
- Release build: `bundleRelease` and `verifyReleaseBundle`.
- No test ad IDs or debug signing in the release bundle.

---

## 5. Release 2.2 — Daily habit and virality

### 5.1 Local day helper

**New file:** `data/LocalDay.kt`

- Use the device’s local calendar day for Daily 10, not UTC.
- Recommended: `java.time.LocalDate` + core library desugaring (project minSdk 24). Alternatively use `Calendar` with `TimeZone.getDefault()`.
- Keep the existing UTC `epochDayOf` for score data, or migrate consistently if safe.
- Test:
  - local midnight rollover;
  - timezone changes;
  - DST transitions;
  - same instant in two timezones produces different daily days.

### 5.2 Daily challenge engine

**New file:** `game/DailyChallenge.kt`; small changes to `CardDeck.kt`

Data:

```kotlin
data class DailyChallenge(
    val epochDay: Long,
    val dayNumber: Long,       // for share text
    val cards: List<GameCard>,
)

data class DailyAnswer(
    val value: Int,
    val guess: Guess?,
    val correct: Boolean,
    val timedOut: Boolean,
    val answerMs: Long,
)

data class DailyResult(
    val epochDay: Long,
    val score: Int,
    val answers: List<DailyAnswer>,
    val completedAtMs: Long,
)
```

Deck requirements:

- 10 cards.
- Exactly 5 primes and 5 composites.
- Tier mix: 3 × two-digit, 4 × three-digit, 3 × four-digit (or another documented fixed mix).
- No duplicates.
- Deterministic for the same local day on every device.
- Use `kotlin.random.Random(seed)` with a stable seed derived from `epochDay` (for example `epochDay * 1_000_003 + 17`).
- Do not depend on `Random.Default`, time of day, locale or device.

Implementation options:

- Add `CardDeck.nextCardForTier(tier: Int)` and shuffle a fixed answer pattern, or
- Create a dedicated `DailyChallengeFactory` that samples from the same sieve-backed pools.

Tests:

- Same seed → same ten cards.
- No duplicates.
- 5/5 prime/composite.
- Tier mix correct.
- 1,000 consecutive days do not throw or exhaust pools.
- Different days produce different decks (statistically).

### 5.3 Daily state and screen

**New files:** `ui/DailyViewModel.kt`, `ui/DailyScreen.kt` or `ui/DailyPanel.kt`

State:

```kotlin
enum class DailyPhase { Ready, Playing, Result }

data class DailyUiState(
    val phase: DailyPhase,
    val dayNumber: Long,
    val cards: List<GameCard>,
    val index: Int = 0,
    val correctCount: Int = 0,
    val answers: List<DailyAnswer> = emptyList(),
    val remainingMs: Long = DAILY_TIME_LIMIT_MS,
    val result: DailyResult? = null,
    val alreadyPlayed: Boolean = false,
)
```

Rules:

- 10 cards, **8 seconds per card** (constant).
- Wrong answer or timeout counts as a miss and advances; the run does not end early.
- Feedback before advancing:
  - correct → green stamp/check for ~700 ms;
  - wrong/timeout → red stamp, correct answer/factorization for ~900 ms.
- Timer pauses during feedback.
- One attempt per local day.
- If a result exists, open the Result screen instead of replaying.
- If an in-progress attempt exists, resume at the saved index and remaining time.
- Persist after each answer and at result.
- Keep the last 30 days of results for a possible calendar/history view.
- No revive, no ads, no interstitial.
- Daily completion calls `registerPlay` for the daily streak.
- Daily result updates `gamesPlayed`, `totalCorrect`, `totalCards` if the product wants daily included in overall stats. Document the choice. Recommended: include it.

Result screen:

- Emoji grid: 🟩 for correct, 🟥 for wrong/timeout.
- `8/10`.
- Daily streak.
- **Share result** button.
- Countdown to next local midnight.
- **Play Classic** button.
- No banner/ad.

Entry points:

- Ready screen **Daily 10** chip/button:
  - not played → “Daily 10 · Play”;
  - in progress → “Daily 10 · Resume”;
  - completed → “Daily 10 · 8/10” → opens result.
- Optional notification deep link in 2.3.

### 5.4 Share

**New files:** `share/ShareText.kt`, `share/ShareLauncher.kt`

Daily share:

```text
Prime or Not Daily #42
🟩🟩🟥🟩🟩🟩🟩🟩🟥🟩
8/10 · streak 5
https://play.google.com/store/apps/details?id=com.companyname.primeornot
```

Classic share:

```text
I scored 23 in Prime or Not — can you beat it?
https://play.google.com/store/apps/details?id=com.companyname.primeornot
```

Rules:

- Use `Intent.ACTION_SEND` with `text/plain` and `Intent.createChooser`.
- No auto-share.
- No reward for sharing.
- No ads or personal data in the shared text.
- Keep the Play URL in one constant so the store link cannot drift.
- Localize the surrounding text later; emoji grid is universal.

### 5.5 Onboarding and practice

**New files:** `ui/OnboardingSheet.kt`, `ui/PracticeViewModel.kt`, `ui/PracticeScreen.kt`

Onboarding:

- Show on first launch only, after the ready state is loaded.
- Three short points:
  1. A prime is divisible only by 1 and itself.
  2. Examples: 2, 3, 5, 7, 11 are prime; 4, 6, 9, 15 are not.
  3. Swipe right for prime, left for not prime. One mistake or timeout ends a Classic run.
- Buttons: **Practice 5 cards** and **Start**.
- Persist `onboarding_seen` immediately when dismissed.
- Accessible later from **How to play**.

Practice:

- Five cards, no timer, no score, no best, no streak, no ads.
- On guess:
  - correct → green feedback and factorization;
  - wrong → red feedback and factorization;
  - advance after ~1.2 s.
- End: “Practice complete. Ready for the real thing?” with **Play Classic** and **Back**.
- Use a seeded or random deck; no persistence needed beyond the session.
- Do not call `ScoreStore` or `registerPlay`.

### 5.6 Stats, rank, review

**New files:** `ui/StatsSheet.kt`, `ui/RunReviewSheet.kt`, `game/Rank.kt`

Stats:

- Use `ModalBottomSheet`.
- Show:
  - best score;
  - games played;
  - total correct / total cards;
  - accuracy;
  - longest streak;
  - daily streak;
  - fastest answer;
  - rank.
- Share stats as text.

Rank:

```kotlin
enum class Rank(val minBest: Int) {
    Rookie(0),
    FactorHunter(10),
    PrimeTime(25),
    PrimeMaster(50),
    PrimeLegend(100),
}
```

- Derive from best score.
- Persist nothing extra; best is the source of truth.
- Add strings for new ranks.

Run review:

- `GameUiState.runAnswers` already populated in 2.1.
- Show a sheet with each card:
  - value;
  - your answer (Prime / Not prime);
  - correct answer;
  - factorization (`91 = 7 × 13`) or “prime”.
- Limit to the last 50 cards to keep the sheet light.
- Use the existing `PrimeChecker.factorizationText`.
- Do not nest a scrollable list inside the scrollable game-over panel; use a separate sheet.

### 5.7 In-app review

**New file:** `ui/ReviewPromptManager.kt`

- Use Play In-App Review API.
- Trigger after:
  - a new best, or
  - a Daily 10 score of 7+.
- Never after a loss with a low score.
- Cap to once every ~120 days using `last_review_prompt_ms`.
- If the API is unavailable or fails, do nothing.
- The prompt must not block gameplay; request it from the game-over/result screen after a short delay.
- Log `review_prompt`.

### 5.8 Store and listing

Update:

- `docs/store/listing.md`
- `docs/store/screenshots/`
- `docs/site/privacy.html`
- Play Data safety and content rating answers
- `docs/PLAY_RELEASE.md` release notes

Copy must be honest:

> No ads during a round. Optional rewarded ads give you a second chance. A capped full-screen ad can appear between runs. Remove ads with a one-time purchase.

New screenshots:

1. Gameplay: big number, timer, swipe hint.
2. Daily 10 result with emoji grid and share.
3. Game over with factorization and revive offer (no ad visible).
4. New best / rank.
5. Practice / How to play.

Do not include an ad in any screenshot.

### 5.9 Release 2.2 QA

- Daily deck determinism across two devices/timezones.
- Local midnight rollover and DST.
- Resume after process death mid-daily.
- Share text and Play link.
- Onboarding first-run only; practice does not affect stats/streak.
- Stats values match runs.
- In-app review prompt only after positive events.
- No ads on Daily result, practice or onboarding.
- Migration from 2.1 data.

Bump `versionCode` to **6**, `versionName` to **2.2.0**.

---

## 6. Release 2.3 — Optional depth

Implement only after 2.1/2.2 metrics are understood.

### 6.1 Streak save

- If the user missed exactly one day and has a streak of 3+, offer one rewarded ad to restore it.
- Once per streak; never on the first day; no fake urgency.
- Update `registerPlay` to detect the gap and expose a `streakSaveAvailable` state.
- Add analytics `streak_save_offer`, `streak_save_watch`, `streak_save_decline`.

### 6.2 Daily reminder notification

- Add WorkManager; `POST_NOTIFICATIONS` on API 33+.
- Opt-in after a completed run with a clear explanation.
- Schedule one notification per day at ~19:00 local if no play today.
- Deep-link to Daily 10.
- Cancel/reschedule on play; one per day maximum.
- Settings toggle.
- Do not notify users who have not opted in.

### 6.3 Themes / supporter pack

- `ThemeStore` + `PrimeTheme` data class.
- Themes change background/card/accent colors only; no gameplay effect.
- Default theme is the current dark teal/red brand.
- Unlock via rank or a small IAP pack.
- Product ID `theme_pack` (non-consumable).
- Persist selected theme.

### 6.4 Prime Hunt mode

- 60-second global timer.
- Swipe right prime, left composite.
- Correct: +1 point and +0.5 s.
- Wrong: -3 s.
- Combo multiplier every 5 correct.
- Separate high score; do not mix with Classic best.
- New `HuntViewModel`/state; do not overload `GameViewModel`.
- Entry point only after Daily 10 proves retention.

### 6.5 Friend challenge

- Encode a seed in a share link; the friend plays the same 10 cards.
- Compare scores in the message.
- Needs deep links and a web fallback; do not start until Daily 10 sharing works.

### 6.6 Remote config and A/B tests

- Firebase Remote Config flags:
  - interstitial runs between (3 vs 4);
  - interstitial daily cap;
  - revive copy key;
  - feature flags for themes/Prime Hunt.
- Do not A/B test the remove-ads price through Remote Config; use Play Console price experiments.
- Do not build this before there is enough DAU to read a result.

### 6.7 Localisation

- Extract all hardcoded user-facing strings:
  - `GameOverPanel.formatFastest` (“ms”, “s”);
  - factorization formatting;
  - share text;
  - ad/revive labels.
- Add translations for the top install geos.
- Test RTL only if adding Arabic/Hebrew; the swipe direction should remain physical.
- Localize the store listing too.

### 6.8 Play Games and web Daily

- Play Games leaderboards/achievements only if competitive users ask.
- A web Daily 10 on the existing site is a later acquisition experiment; it needs the same deterministic deck and a “Play on Android” CTA.

---

## 7. DataStore keys

### `prime_or_not_scores` (existing, extend)

- `best_score`
- `today_score`
- `today_epoch_day`
- `streak_days`
- `last_epoch_day`
- `games_played`
- `total_correct`
- `total_cards`
- `longest_streak`
- `fastest_answer_ms`

### `prime_or_not_prefs` (new)

- `onboarding_seen`
- `sound_enabled`
- `is_ad_free`
- `entitlement_last_verified_ms`
- `last_review_prompt_ms`
- `ad_total_runs`
- `ad_runs_since_interstitial`
- `ad_interstitial_day`
- `ad_interstitial_count_today`
- `ad_last_interstitial_ms`
- `ad_last_rewarded_ms`
- `daily_result_json`
- `daily_in_progress_json`
- `daily_history_json`
- `notifications_enabled` (2.3)
- `theme_id` (2.3)

Rules:

- Use one DataStore instance per file.
- New keys default safely; no migration that clears existing scores.
- `daily_history_json` keeps at most 30 days.
- Do not store PII.

---

## 8. Testing plan

### Unit tests

- `AdFrequencyPolicyTest`
  - lifetime grace;
  - every-3-runs;
  - 90 s interval;
  - daily cap;
  - new-best skip;
  - revive-offer skip;
  - rewarded cooldown;
  - ad-free skip.
- `GameViewModelReviveTest`
  - revive offered once;
  - score preserved;
  - streak reset;
  - next card/timer;
  - decline finalises once;
  - restart finalises once;
  - timeout path;
  - no revive in non-classic modes;
  - new best persistence.
- `ScoreStoreTest`
  - streak on play, not open;
  - same day / yesterday / gap;
  - display reset;
  - new stats fields;
  - migration.
- `DailyChallengeTest`
  - determinism;
  - no repeats;
  - 5/5 answers;
  - tier mix;
  - 1,000-day robustness.
- `DailyViewModelTest`
  - wrong/timeout advance;
  - result persistence;
  - one attempt;
  - resume;
  - local day boundary.
- `LocalDayTest`
  - timezone and DST.
- `RankTest`
- `ShareTextTest`
- `BillingEntitlementTest` with a fake billing client.
- `ReviewPromptTest` cap logic.

### Instrumented Compose tests

- Ready screen shows Daily 10 / Practice / Stats / Settings.
- Onboarding first-run only; skippable.
- Game-over panel:
  - revive hidden when unavailable;
  - revive visible when available;
  - loading/failed states;
  - banner hidden during revive;
  - review/share actions.
- Settings toggles sound and opens privacy options.
- Daily result emoji grid and share button.
- Practice feedback and no score.
- No ad composable in Ready/Playing/Practice/Daily.
- TalkBack labels.

### Manual QA

- Debug test ads on API 24 and API 35.
- EEA consent: reset consent, verify no ads before consent, privacy options, withdrawal.
- Rewarded ad: complete, close early, fail to load, offline.
- Interstitial: caps, new-best skip, revive-skip, daily skip.
- Billing: purchase, pending, cancel, restore, refund, reinstall, offline.
- Rotation during run, revive offer and ad.
- Process death during run, revive offer, daily.
- Small screen, large font, TalkBack.
- Release bundle and Play pre-launch report.

---

## 9. Release and QA checklist

### Build

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
.\gradlew.bat verifyReleaseBundle
```

### Before upload

- [ ] `versionCode` incremented; `versionName` correct.
- [ ] Real AdMob app ID + banner + interstitial + rewarded IDs in `local.properties`.
- [ ] `validateReleaseAdIds` passes.
- [ ] `verifyReleaseBundle` passes with the Play upload key.
- [ ] No test ad IDs, debug signing or `ALLOW_TEST_ADS` build in the artifact.
- [ ] `remove_ads` product active in Play Console; license testers configured.
- [ ] UMP consent form and privacy options verified.
- [ ] Privacy policy and Data safety updated.
- [ ] Store listing declares ads and in-app purchases.
- [ ] Screenshots contain no ads.
- [ ] Internal testing smoke test.
- [ ] Closed testing with 20–50 users.
- [ ] Staged production rollout 20% → 100%.
- [ ] Monitor crashes, ANR, ratings, ad revenue and retention.

### Release notes templates

2.1:

```text
• Second chance: watch a short ad to keep your run
• Remove ads with a one-time purchase
• Sound settings and privacy options
• Performance and stability fixes
```

2.2:

```text
• New Daily 10 challenge — a fresh set every day
• Share your daily result
• Practice mode and How to play
• Stats, ranks and run review
• Faster onboarding for new players
```

---

## 10. File-by-file change map

### New files

```text
app/src/main/java/com/companyname/primeornot/
├── ads/AdManager.kt
├── ads/AdFrequencyPolicy.kt
├── analytics/AnalyticsLogger.kt
├── billing/BillingManager.kt
├── data/PlayerPreferences.kt
├── data/LocalDay.kt                    (2.2)
├── game/DailyChallenge.kt              (2.2)
├── game/Rank.kt                        (2.2)
├── share/ShareText.kt                  (2.2)
├── share/ShareLauncher.kt              (2.2)
├── ui/DailyViewModel.kt                (2.2)
├── ui/DailyScreen.kt                   (2.2)
├── ui/OnboardingSheet.kt               (2.2)
├── ui/PracticeViewModel.kt             (2.2)
├── ui/PracticeScreen.kt                (2.2)
├── ui/StatsSheet.kt                    (2.2)
├── ui/RunReviewSheet.kt                (2.2)
├── ui/ReviewPromptManager.kt           (2.2)
└── ui/SettingsSheet.kt                 (2.1)
```

### Modified files

```text
app/build.gradle.kts
gradle/libs.versions.toml
app/src/main/AndroidManifest.xml
app/src/main/java/com/companyname/primeornot/MainActivity.kt
app/src/main/java/com/companyname/primeornot/data/ScoreStore.kt
app/src/main/java/com/companyname/primeornot/data/DailyStreak.kt
app/src/main/java/com/companyname/primeornot/ui/GameUiState.kt
app/src/main/java/com/companyname/primeornot/ui/GameViewModel.kt
app/src/main/java/com/companyname/primeornot/ui/GameScreen.kt
app/src/main/java/com/companyname/primeornot/ui/GameOverPanel.kt
app/src/main/java/com/companyname/primeornot/audio/SoundPlayer.kt
app/src/main/res/values/strings.xml
docs/ARCHITECTURE.md
docs/PLAY_RELEASE.md
docs/store/listing.md
docs/site/privacy.html
```

### Dependencies

- Play Billing: latest stable `com.android.billingclient:billing-ktx` (verify Play policy).
- Firebase Analytics + Crashlytics + `google-services` plugin, if approved.
- WorkManager: 2.3 notifications.
- `kotlinx.serialization-json` or `org.json`: daily JSON persistence (coder’s choice; keep it simple and testable).
- Core library desugaring if using `java.time` for local days.
- No new UI libraries; Material 3 is already present.

---

## 11. Open questions / owner dependencies

Resolve before or during the relevant release:

1. Is Firebase Analytics approved? If not, keep the `AnalyticsLogger` interface and ship a no-op, but retention tuning will be blind.
2. Is the remove-ads price $3.99? Test $2.99 vs $3.99 after enough traffic.
3. Is the launch with 2.1 or 2.2? Recommendation: if production is still 0%, launch with 2.2.
4. Should remove-ads purchasers get one free revive per day? Recommendation: consider only if reviews show rewarded ads still feel intrusive after purchase.
5. Is the target audience 13+ only? If the app is marketed to children, ad personalisation and Families policy change the design.
6. Is the Daily 10 local-day or UTC? Recommendation: local day, with a clear countdown.
7. Is 8 seconds per Daily card the right pressure? Test 8 vs 10 later; do not block 2.2.
8. Is a web Daily 10 or friend challenge worth the maintenance? Treat as later bets.

---

## 12. Do not do

- Do not show ads during Ready, Playing, Practice or Daily.
- Do not show app-open ads.
- Do not auto-show an interstitial on the game-over screen.
- Do not stack an interstitial on top of a revive offer or a rewarded ad.
- Do not grant a revive when the ad was not completed.
- Do not hide “Try again” behind an ad.
- Do not add a free skip to Classic.
- Do not reveal factors before the guess.
- Do not add a subscription, energy system, loot boxes or pay-to-win.
- Do not add a second game mode before Daily 10 and onboarding are live.
- Do not add mediation or multiple ad networks before there is enough traffic.
- Do not reset user scores on upgrade.
- Do not ship Google sample ad IDs or a debug-signed release.
- Do not put PII into analytics.
- Do not make the remove-ads purchase a dark pattern.
- Do not use fake urgency or misleading “continue” buttons.

---

## 13. Suggested first sprint

1. Commit the existing icon/branding changes.
2. Extend `ScoreStore` + add `PlayerPreferences`.
3. Add `AdFrequencyPolicy` with unit tests.
4. Add `AdManager` with debug test IDs and consent handling.
5. Implement the revive state machine and the game-over panel states.
6. Add the interstitial on retry with conservative caps.
7. Add Play Billing + `remove_ads` + Settings.
8. Add analytics events and Crashlytics.
9. Run the 2.1 QA matrix and internal testing.

The critical path is **revive → interstitial → billing**. Daily 10 can start in parallel once the ad/billing interfaces are stable.
