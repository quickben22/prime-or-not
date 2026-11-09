
# Prime or Not — Product & Monetization Plan

**Audience:** product owner / decision maker

**Date:** 8 November 2026

**Version:** 1.1

**Companion document for the coder:** [`IMPLEMENTATION_PLAN.md`](IMPLEMENTATION_PLAN.md)

---

## 1. Executive summary

The core game is already strong: one rule, a fair 50/50 deck, a visible timer, factorization on failure, good haptics and sound, and a clean one-screen loop. The problem is not the game — it is the **business layer**.

Today the app monetises with a single banner on the game-over screen. There is no rewarded ad, no interstitial, no analytics, and no daily habit loop. A five-second run also produces very few ad impressions, so adding more ad slots to the same screen will not fix revenue. The app needs **more runs per session and more sessions per day**, plus a few well-placed monetisation surfaces.

The app will remain **100% free for everyone** with no in-app billing or purchases.

The old Xamarin app is a feature bank, not a template. Its real value is the **How to Play** screen and the **end-of-run review**. Its second game mode is essentially a duplicate, and its 15-second global timer, free skip and visible factors are mechanics that should not be ported.

**Recommendation:** make the game a daily habit first, then monetise the failure moment.

* **Two ad surfaces:** rewarded revive (score $\ge 15$) and capped interstitials.
* **One retention feature:** Daily 10 challenge with a Wordle-style share.
* **One onboarding feature:** How to play + five-card practice mode.
* **No** billing/in-app purchases, lives/energy system, subscription, app-open ads, loot boxes or pay-to-win.

If the Play production rollout is still at 0%, do not spend the launch on version 2.0.1. Use internal/closed testing for the next two releases and launch with the habit loop and monetisation already in place.

---

## 2. Current state

### What is already good

* The core loop is immediately understandable and fast.
* The deck is fair: 50/50 prime/composite, no repeats, hard composites in higher tiers.
* The game-over panel explains the missed card with factorization.
* The UI is polished and distinctive: dark background, white card, red `1||0` mark, timer bar.
* Haptics, sound, streaks, tiers, new-best badge and run stats are already present.
* The app is offline-friendly (apart from ads) and has no account requirement.
* UMP consent is already wired for AdMob.

### What is missing commercially

* Only one banner on game over; no forced full-screen ad and no rewarded ad.
* The app is completely free, so monetisation relies on well-placed non-intrusive ad surfaces.
* No daily challenge or other reason to return tomorrow.
* No share loop and therefore no free user acquisition.
* No analytics, so ad frequency and retention cannot be tuned with evidence.
* Daily streak currently advances when the app is opened, not when a run is played.
* No stats screen beyond best score / today / streak.
* No onboarding for players who do not know what a prime number is.

---

## 3. Core thesis

> **Make the game a daily habit first, then monetise the failure moment.**

A player who returns daily is worth far more than a player who sees three ads in one session and never comes back. The plan therefore invests in retention and virality, while adding ad surfaces that respect the core loop.

### The two monetisation surfaces

1. **Rewarded revive (Score $\ge 15$)**
* Offered after a wrong answer or timeout in Classic mode **only if the current run score is 15 or higher**.
* “Keep this run” with a small **AD** badge.
* One revive per run; score preserved, current streak reset.
* Highest eCPM and strongest user intent.


2. **Capped interstitial**
* Shown only when the player taps “Try again,” never automatically on the game-over screen.
* Starts after **15 lifetime played games**.
* Minimum **2 minutes** between interstitials.
* Maximum **2 interstitials per day**.
* Skipped after a new best, after a revive offer, or after a recent rewarded ad.
* If not preloaded, the game restarts immediately; the player never waits.



### The one retention feature

**Daily 10** — ten cards, one attempt per local day, eight seconds per card, wrong answers count but do not end the run. The result is a ten-square emoji grid that can be shared. This gives the app a daily ritual, a reason to open it, and a free acquisition loop.

### The one onboarding feature

**How to play + practice** — a first-run sheet with a simple definition, examples and swipe directions, plus a five-card untimed practice mode. This prevents new players from churning before they understand the game.

---

## 4. Product principles

1. **100% Free Experience.** No paywalls, subscriptions, or in-app purchases.
2. **The classic loop stays pure.** No ads during active play. No interruptions between cards.
3. **One-tap start remains the hero.** Daily 10, practice and stats are secondary choices, not a new home screen.
4. **Every ad is expected or opt-in.** Interstitials happen only at a natural break; rewarded ads are always a choice.
5. **No dark patterns.** No fake urgency, no misleading “continue” buttons, no ads disguised as content.
6. **Respect the player’s time.** A bad run should end in seconds, and a retry should never wait for an ad that is not ready.
7. **Measure before optimising.** Start with conservative ad frequency; increase only when retention data supports it.
8. **The daily loop is sacred.** No ads on the Daily 10 result; that screen exists to create a habit and a share.

---

## 5. Target player journey

### First open

1. Ready screen with the main card.
2. A one-time “How to play” sheet:
* A prime is divisible only by 1 and itself.
* Examples: 2, 3, 5, 7, 11 are prime; 4, 6, 9, 15 are not.
* Swipe right for prime, left for not prime.
* One mistake or timeout ends a Classic run.


3. Buttons: **Practice 5 cards** and **Start**.

### Ready screen

* HUD: score, best, daily streak.
* Main card: **TAP TO START**.
* Secondary choices:
* **Daily 10** with today’s status.
* **Practice**.


* Footer: Stats · How to play · Settings.
* No ads on this screen.

### Classic run

* Swipe right for prime, left for not prime.
* Five to seven seconds per card depending on digits.
* Score, streak chip, best, tier-ups, new-best badge.
* No ads during play.

### Failure

* Game-over panel shows:
* Answer and factorization.
* Score, best, best streak, accuracy, fastest answer.
* “N more to beat your best” when relevant.


* If revive is available (**Score $\ge 15$** and unused):
* Primary: **Keep this run** with a small **AD** badge and “Watch a short ad.”
* Secondary: **Try again**.


* If revive is unavailable (Score < 15 or already used) or declined:
* Primary: **Try again**.
* Text actions: **Review run**, **Share**.



### Revive

* Rewarded ad plays.
* On completion:
* Score preserved.
* Current streak reset to zero.
* Missed card stays in the review and accuracy stats.
* Fresh card and full timer.
* One revive per run only.


* If the ad fails to load:
* Hide the revive offer.
* Never block “Try again.”
* Do not grant a free revive.



### Retry

* If due (after 15 lifetime played games, minimum 2 min interval, max 2 per day), an interstitial may play after tapping “Try again.”
* If no ad is preloaded, skip it immediately.
* Never show an interstitial automatically on the game-over screen.

### Daily 10

* Ten cards, one attempt per local day.
* Eight seconds per card.
* Wrong answer or timeout counts as a miss and advances; the run does not end early.
* Result:
* Ten-square emoji grid.
* Score out of 10.
* Best daily streak.
* Share button.
* Countdown to the next local midnight.
* “Play Classic” button.


* No revive and no ads on the result screen.
* Completing the Daily 10 counts toward the daily streak.

---

## 6. Feature set

### 6.1 Rewarded revive (P0)

**Why:** It converts an emotionally intense failure moment into a voluntary, high-value ad impression while protecting game integrity by requiring players to demonstrate skill first.

**Rules:**

* Classic mode only.
* Requires **current score $\ge 15$**.
* Once per run.
* Offered after a wrong guess or a timeout.
* Score preserved; current streak reset; missed card counted.
* Next card gets a full timer.
* Reward granted only after the ad is completed.
* If the ad fails, hide the offer; “Try again” remains.
* If the player declines, the run is finalised once.

**UX notes:**

* Label the button honestly: “Keep this run” with an **AD** badge and a subtitle such as “Watch a short ad.”
* Do not auto-focus the ad button.
* Hide the banner while the revive offer is visible.
* If score < 15 or user has no ad available, the panel looks normal and hides the revive option.

### 6.2 Capped interstitial (P0)

**Why:** It adds revenue per session without putting ads inside active gameplay.

**Default policy:**

* First 15 lifetime played games: no interstitials.
* Minimum **2 minutes** between interstitials.
* Maximum **2 per day**.
* Skip if:
* The run set a new best.
* A revive offer was shown for that run.
* A rewarded ad was watched in the last two minutes.


* Show only after the player taps “Try again.”
* If the ad is not preloaded, restart immediately.

### 6.3 Daily 10 (P0/P1)

**Why:** Creates a daily ritual, a shareable artifact, and a reason to return.

**Design:**

* Ten cards, one attempt per local day.
* Five primes and five composites, mixed across tiers, no repeats.
* Eight seconds per card.
* Wrong answer or timeout counts as a miss and advances.
* Result: emoji grid, score, share, countdown, “Play Classic.”
* Daily completion advances the daily streak.
* No ads on the Daily result.

### 6.4 Onboarding + practice (P0/P1)

**Design:**

* First-run bottom sheet with core rules and examples.
* Practice mode: five cards, untimed, instant feedback.

### 6.5 Stats, rank, review and share (P1)

* Stats sheet: best, games played, total correct, accuracy, longest streak, daily streak, fastest answer, rank.
* Ranks: Rookie (0–9), Factor Hunter (10–24), Prime Time (25–49), Prime Master (50–99), Prime Legend (100+).
* Review run: sheet listing cards and factorizations.
* In-app review prompt after positive moments.

---

## 7. What to take from the old app

| Old feature | Verdict | Reason |
| --- | --- | --- |
| “How to Play” screen | **Port as first-run sheet + practice** | Protects D1 retention for new players |
| Results review with factors | **Port as “Review run”** | Educational, increases session time |
| “Only Primes” mode | **Do not port as-is** | Duplicate mechanic |
| 15-second global timer | **Do not port** | Per-card timer is superior |
| Free up-swipe skip | **Do not port** | Undermines challenge |
| Factors shown on the card face | **Do not port** | Leaks the answer |
| “High Scores” menu | **Implement properly as Stats/Rank** | Better visual feedback |

---

## 8. Revenue model

### Formula

```text
Revenue = installs × D1 × D7 × sessions/day × runs/session
          × (ads/run × eCPM)

```

### Surface map

| Surface | Placement | Requirement / Cap |
| --- | --- | --- |
| Banner | Game-over only | Baseline revenue; hide during revive offer |
| Rewarded revive | Game-over after a loss | **Score $\ge 15$**, max 1 per run |
| Interstitial | On “Try again” tap | **After 15 played games**, **min 2 min gap**, **max 2 per day** |
| Daily 10 | Result share | No ads on result screen; drives organic acquisition |

---

## 9. Roadmap

### Release 2.1 — Revenue core (P0, roughly 2 weeks)

1. Ad infrastructure: interstitial and rewarded test IDs, preloading, consent, lifecycle.
2. Rewarded revive (score $\ge 15$ rule) with state-machine handling.
3. Capped interstitial on “Try again” (after 15 games, 2 min gap, 2/day cap).
4. Settings sheet: sound, privacy options, how to play, stats, version.
5. Stats persistence and daily-streak fix.
6. Analytics and Crashlytics.

### Release 2.2 — Daily habit and virality (P0/P1, roughly 2 weeks)

1. Daily 10 challenge & share loop.
2. Onboarding + practice.
3. Stats, rank, run review.
4. In-app review prompt.

---

## 10. Guardrails and policy

* **100% Free App.** No billing SDK or in-app purchases required.
* **No ads during active play.**
* **No app-open ads.**
* **No ads in Daily 10, practice or onboarding.**
* **Never stack ads.**
* **Rewarded ads are opt-in and gated by score $\ge 15$.**
* **Strict interstitial caps:** Start after 15 games, 2 minutes between ads, max 2/day.
