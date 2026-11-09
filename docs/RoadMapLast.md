# Prime or Not — Developer Implementation Roadmap

**Version:** 1.4  
**Date:** 8 November 2026

**Changes in v1.4 (Phase 1 and Phase 2 only):**

- Rewarded revive is removed from Phases 1 and 2 entirely: no revive UI, state, persistence fields, statistics, records, recent-game flags, tests, or acceptance criteria. Revive remains a Phase 4 feature, unchanged.
- Added manual-abandon versus interruption handling for active Classic games.
- Added an existing-player migration rule for `onboardingComplete`.
- Clarified that completing Practice from the main menu does not set `onboardingComplete`.
- Clarified Practice number range, the "1 only as fifth card" rule, trap-counting, and that traps count toward the composite requirement.
- Added a requirement to confirm `MAX_CLASSIC_NUMBER` before finalizing teaching-tip content.
- Clarified that short completed games appear in recent history and statistics but do not qualify for the streak.
- Clarified the empty-state handling for accuracy.
- Added a local schema/data version for safe future migration.
- Removed revive scenarios from the Phase 1/2 testing table and added Phase 1/2 edge cases.
- Section numbering normalized; the duplicated 4–7 numbering in v1.3 is resolved. Phase 3 onward content is unchanged.

---

## 1. Project objective

Improve the game, encourage players to return, and introduce monetization without making the experience frustrating.

The objective of this release is to improve game stability, player retention, educational value, and the overall gameplay experience while keeping implementation straightforward.

### Scope and principles

For this release:

- Do not introduce a separate analytics SDK or custom analytics system.
- Use AdMob for advertising and advertising performance monitoring.
- Save Classic player statistics, personal records, and game progress locally.
- Focus on gameplay stability, onboarding, Classic statistics, Daily 10 compatibility, persistence, and voluntary advertising.
- Avoid unnecessary features, complicated systems, and major changes to existing gameplay.
- Preserve existing player data and functionality wherever possible.
- Do not introduce forced advertising that interrupts normal gameplay.

### General implementation principle

Before implementing new functionality, review the existing game architecture and reuse existing systems where appropriate.

Do not unnecessarily replace working gameplay logic, redesign unrelated screens, or introduce additional dependencies.

Existing game rules and scoring calculations should remain unchanged unless a specific requirement in this document requires a modification.

---

# 2. Phase 1 — Stability, Saving, Streak Correction and My Records

## Phase 1 scope note

Rewarded revive is out of scope for Phases 1 and 2.

Do not add revive UI, revive state, revive persistence fields, revive statistics, revive records, revive-related recent-game flags, revive-related tests, or revive-related acceptance criteria in Phase 1 or Phase 2. Phase 4 may introduce rewarded revive later.

## A. Game stability and reliability

Review the existing game and fix known crashes, critical bugs, and problems that could cause loss of player progress or statistics.

### Requirements

- Review existing gameplay for crashes and critical bugs.
- Ensure game progress and completed results are saved reliably.
- Ensure saved data remains available after app restarts and application updates.
- Test app interruptions, backgrounding, closing, and reopening.
- Ensure core gameplay and local statistics work without an internet connection.
- Prevent incomplete or interrupted games from being incorrectly recorded as completed.
- Handle missing, outdated, or corrupted saved data safely.
- Avoid resetting existing player records or statistics during updates.

### Active game persistence

An active Classic game should be saved locally so that the player can resume it after an interruption or app restart, where technically supported.

Save relevant game state, including:

- Current score.
- Current game progress.
- Correct-answer streak within the current game.
- Game state and other information necessary to restore gameplay.

Progress should be saved at appropriate gameplay events, particularly after answers and important state changes.

### Manual abandon versus interruption

Manual abandonment and app interruption must be handled differently:

- If the player explicitly chooses to abandon or quit the active Classic game, discard the active session. It must not be recorded as completed.
- If the app is backgrounded, closed, or interrupted, preserve the active session so it can be resumed where supported.
- If an interrupted game cannot be restored safely, discard it without crashing and without recording it as completed.

### Offline functionality

Core gameplay, Practice, My Records, and locally stored statistics must not depend on an internet connection.

An unavailable advertising connection must never prevent normal gameplay.

## B. Classic game completion and qualification rules

The distinction between a completed game, an abandoned game, and a streak-qualifying game must be implemented consistently.

### Completed Classic game

A Classic game is considered completed when it reaches its normal terminal game-over state.

- A completed game is recorded only once.
- A player manually exiting or abandoning an active game does not count as a completed game.
- Closing the application before reaching the terminal game-over state does not count as a completed game.
- A completed game must not be recorded again when the game-over screen is reopened.
- A completed game must not be counted again when the player retries.

### Streak-qualifying Classic game

Not every completed game needs to qualify for a daily streak.

For the initial implementation, a Classic game qualifies for the daily streak when:

- The game has reached its normal terminal game-over state.
- At least five numbers have been answered.

Answering fewer than five numbers does not qualify for a daily streak.

This threshold is specifically intended to prevent players from advancing their streak by starting a game and immediately abandoning it.

A completed game can contribute to completed-game statistics and recent-game history even when it does not meet the five-answer streak qualification threshold.

Practice and Daily 10 never qualify for the Classic daily streak.

## C. Streak correction

Correct the existing streak implementation so that simply opening the application does not advance a streak.

### Daily streak rules

- Opening the application never advances a streak.
- Starting a Classic game never advances a streak.
- Only a streak-qualifying completed Classic game advances the daily streak.
- Practice and Daily 10 never advance the Classic daily streak.
- A player can increase their daily streak only once per calendar day, regardless of how many qualifying games they complete.
- Completing another qualifying game on the same day must not increase the streak again.
- Completing qualifying games on consecutive calendar days increases the streak by one.
- Missing a complete calendar day breaks the current streak.
- The longest daily streak is permanent and must never be lost when the current streak resets.

### Date handling

Use the device's local calendar date when a qualifying Classic game is completed.

Store the date of the last qualifying game so that the streak can be calculated correctly across app restarts and different days.

The current streak should behave as follows:

- If the player completed a qualifying game today, display the current streak.
- If the last qualifying game was yesterday, keep the current streak active while waiting for today's qualifying game.
- If the player misses an entire calendar day, the current streak resets.
- When the player next completes a qualifying game after missing a day, the current streak starts again at one.

The streak must not advance merely because the date changes or the application is opened.

Local streak tracking does not require a server or a separate analytics system.

## D. My Records and Statistics — Classic only

Add a dedicated **My Records** screen accessible directly from the main menu.

The initial implementation must contain Classic mode statistics and records only.

**Do not add Daily 10 statistics, Daily 10 records, or Daily 10 progress information.**

**Do not add revive-related records, statistics, flags, or empty states in Phase 1.**

### 1. Personal records

Display the following personal records:

- **Best Classic score:** Highest final score achieved in any completed Classic game.
- **Longest correct-answer streak:** Highest number of consecutive correct answers achieved within a single completed Classic game.

Do not display revived or non-revived score records in Phase 1. Rewarded revive is not part of this phase.

### 2. Overall Classic statistics

Display:

- Total Classic games completed.
- Total correct answers.
- Total incorrect answers.
- Overall accuracy percentage.
- Total numbers answered.

### Calculation rules

**Total Classic games completed**

Count each uniquely completed Classic game exactly once.

**Total correct answers**

Sum correct answers from all completed Classic games.

**Total incorrect answers**

Sum incorrect answers from all completed Classic games.

**Total numbers answered**

Calculate as:

`Total correct answers + Total incorrect answers`

Questions that were generated but never answered must not be included.

**Overall accuracy**

Calculate as:

`(Total correct answers / Total numbers answered) × 100`

Display accuracy to one decimal place.

If no numbers have been answered, display accuracy as unavailable using a consistent empty-state value such as `—`. Do not display a misleading `0%` unless the existing UI already uses `0%` consistently for this empty state.

Accuracy must be calculated from aggregate correct and incorrect answers, not by averaging the accuracy percentages of individual games.

### 3. Progress

Display:

- Current daily streak.
- Longest daily streak.
- Recent Classic game results.

### 4. Recent Classic game results

Show the latest 20 completed Classic games, with the newest result displayed first.

Each result should display useful information such as:

- Final score.
- Number of correct answers.
- Number of incorrect answers.
- Accuracy for that game, where applicable.
- Date completed, displayed in local time.

Only completed Classic games should appear in this history.

Abandoned and incomplete games must not appear.

Completed games with fewer than five answered numbers must still appear in recent history and must still count toward completed-game statistics, even though they do not qualify for the daily streak.

Do not display a revived/non-revived indicator in Phase 1.

## E. Statistics, records and data implementation requirements

Create a reliable local data structure for storing Classic statistics, personal records, streak information, and recent game results.

### Game identification and duplicate prevention

Each new Classic game must have a unique game/session identifier.

- A new game receives a new identifier.
- Retrying after a completed game starts a new game with a new identifier.
- Each completed game may be submitted to the statistics system only once.
- Reopening the game-over screen must not cause another statistics update.
- Use the game identifier and an explicit completion/submission state to prevent duplicate result submissions.

### Statistics updates

Update statistics when a Classic game reaches its terminal completed state.

Only completed games contribute to:

- Personal records.
- Overall statistics.
- Recent game history.

Abandoned or incomplete games must not affect completed-game totals or personal records.

The longest correct-answer streak is calculated within an individual game. An incorrect answer resets the consecutive correct-answer count.

### Retry handling

Retrying must never duplicate a previously completed result.

A retry creates a new Classic game session and must not cause the previous session's statistics to be submitted again.

### Data persistence and compatibility

- Preserve existing records when the application is updated.
- Handle missing saved data through safe initialization.
- Handle older saved data through appropriate data migration.
- Do not overwrite valid existing records with default values.
- Avoid application crashes when saved data is incomplete or invalid.
- Save completed results reliably so that they remain available after a restart.
- Add a local schema/data version so that future migrations can be handled safely.
- Do not add revive-related fields in Phase 1 or Phase 2.

Use local storage and the existing project architecture wherever possible. Do not introduce unnecessary external services or databases.

## F. Phase 1 acceptance criteria

Phase 1 is considered complete when:

- The application runs without known critical crashes or stability problems.
- Classic game progress is saved and handled safely after interruptions.
- Manual abandonment discards the active session without recording a completed game.
- Core gameplay and local statistics work offline.
- Opening the application does not advance a daily streak.
- Starting and abandoning a Classic game does not advance a daily streak.
- A qualifying completed Classic game advances the daily streak.
- A daily streak can increase only once per calendar day.
- Missing a calendar day resets the current streak without losing the longest streak.
- My Records is accessible directly from the main menu.
- All requested Classic personal records are displayed.
- All requested Classic overall statistics are displayed.
- Accuracy is calculated correctly.
- Recent Classic game results are displayed correctly.
- Completed games with fewer than five answers appear in recent history and count as completed, but do not qualify for the streak.
- Statistics update only once after a completed game.
- Abandoned and incomplete games do not affect completed-game totals or records.
- Retrying a game does not duplicate statistics.
- Records and statistics survive app restarts and application updates.
- Missing or older saved data is handled safely.
- No Daily 10 statistics or records are included.
- No revive UI, revive state, revive statistics, revive records, revive fields, or revive-related tests are introduced in Phase 1.

---

# 3. Phase 2 — Onboarding and Practice Mode

## Phase 2 scope note

Rewarded revive remains out of scope for Phase 2.

Do not add revive UI, revive state, revive persistence fields, revive statistics, revive records, revive-related recent-game flags, revive-related tests, or revive-related acceptance criteria in Phase 2.

## A. First-time experience — How to Play

Create a simple and clear **How to Play** introduction for new players.

The objective is to teach the basic mathematical concepts before the player starts playing.

### Required educational content

Explain:

**Prime numbers**

A prime number is a whole number greater than 1 that has exactly two positive divisors: 1 and itself.

Examples: 2, 3, 5, 7, 11.

**Composite numbers**

A composite number is a whole number greater than 1 that has more than two positive divisors.

Examples: 4, 6, 8, 9, 10.

**Neither prime nor composite**

The number 1 is neither prime nor composite.

Make this distinction especially clear because it is important to understanding the game.

### How to Play explanation

Explain how the player interacts with the game, how to identify the correct answer, and what happens when they make a mistake.

Keep the introduction short, clear, visually understandable, and suitable for players who are unfamiliar with prime numbers.

### First-launch behavior and onboarding completion

- Show the introduction automatically when a new player launches the game for the first time.
- Do not automatically show it every time the player opens the application.
- Store the onboarding completion state locally using an `onboardingComplete` flag.
- Set `onboardingComplete` to `true` when the player completes the full five-card Practice session associated with the first-time onboarding flow.
- Reaching the end of all five cards counts as completing Practice, whether individual cards were answered or skipped.
- If the player exits Practice before reaching the end, do not set `onboardingComplete` to `true`.
- If onboarding has not been completed, offer the first-time experience again the next time the player launches the application.
- Once completed, do not automatically show onboarding again.
- Make How to Play and Practice accessible from the main menu so players can revisit or replay them at any time.
- Replaying Practice from the main menu must not reset or change the existing `onboardingComplete` flag.
- Do not unnecessarily interrupt existing players with a new introduction after an application update.

### Existing-player migration rule

Existing players with previously saved game data should be treated as existing players rather than automatically being forced through onboarding.

During migration, if saved data indicates prior play — for example, at least one completed Classic game, existing statistics, existing records, or an older data version that clearly represents prior use — set `onboardingComplete` to `true`.

If no prior play is detected, initialize `onboardingComplete` to `false`.

### Onboarding flow rule

The first-time onboarding flow consists of How to Play followed by the five-card Practice session.

Completing a Practice session launched from the main menu does not set `onboardingComplete`. Only completing the Practice session that is part of the first-time onboarding flow sets `onboardingComplete` to `true`.

## B. Practice mode

Implement a short, untimed Practice mode to help players understand prime and composite numbers before playing Classic.

### Practice session requirements

- Exactly five cards per session.
- No timer.
- No score.
- No streak impact.
- No impact on Classic statistics or personal records.
- Immediate feedback after each answer.
- A short mathematical explanation when an answer is incorrect.
- Option to skip the current card.
- Option to exit Practice at any time.
- Option to restart Practice after completing the session.
- Practice can be replayed at any time from the main menu.
- Practice must never show advertisements.
- Practice must not introduce or reference rewarded revive.

### Practice deck composition rules

Each Practice session must contain exactly five distinct numbers.

The deck must satisfy all of the following rules:

- Include at least two prime numbers.
- Include at least two composite numbers.
- Include at least one trap number selected from the following list: **9, 15, 21, 25, 27, 49, 91**.
- The fifth and final card may be the number 1, demonstrating that it is neither prime nor composite.
- The number 1, if included, may appear only as the fifth card.
- Do not repeat the same number within a single Practice session.
- Use the same mathematical classification logic as Classic so that a number cannot be classified differently in Practice and Classic.

All numbers in the trap list are composite. They are intentionally included because beginners may mistake them for prime numbers.

Examples of why these numbers are useful:

- 9 = 3 × 3
- 15 = 3 × 5
- 21 = 3 × 7
- 25 = 5 × 5
- 27 = 3 × 9
- 49 = 7 × 7
- 91 = 7 × 13

The inclusion of these examples reinforces that a number can have factors greater than 11 and still be composite.

If 1 is included as the fifth card, the other four cards must still contain at least two primes and at least two composites. One of those two composites must be a trap number.

If 1 is not included, the five cards must still contain at least two primes and at least two composites, and at least one of those composites must be a trap number.

The deck-generation logic must always satisfy these composition requirements. Random selection must not produce invalid decks or duplicate numbers.

Use the existing game's supported number range for classification. Unless the existing game already defines a smaller teaching pool, limit Practice numbers to **1–100** so the educational content remains appropriate for beginners.

The same number may appear again in a later Practice session. The distinct-number requirement applies within each individual session.

### Practice card behavior

Each card presents a number and asks the player to identify whether it is:

- Prime.
- Composite.
- Neither.

After the player answers:

- Immediately indicate whether the answer is correct or incorrect.
- Give simple positive feedback for a correct answer.
- Explain the correct classification when the answer is incorrect.
- Allow the player to continue to the next card.

Skipping a card moves to the next card without recording an answer or affecting any statistics.

### Practice isolation

Practice must be completely separate from Classic gameplay statistics and streak calculations.

- Practice answers must not affect Classic correct-answer totals.
- Practice answers must not affect Classic incorrect-answer totals.
- Practice must not update personal records.
- Practice must not advance daily streaks.
- Practice must never show advertisements.
- Practice must never reference or grant rewarded revive.

Use the same mathematical classification logic as the actual game so that a number cannot be classified differently in Practice and Classic.

## C. Teaching tips

Create a reusable teaching-tip component that can be displayed during gameplay and on game-over screens.

The purpose is to improve the player's understanding of prime and composite numbers through short, useful mathematical explanations.

### Content requirements

Include mathematically correct explanations of useful divisibility rules, including rules for relevant prime divisors greater than 11.

Examples of divisibility concepts include:

- Divisibility by 2.
- Divisibility by 3.
- Divisibility by 5.
- Divisibility by 7.
- Divisibility by 11.
- Divisibility by 13 and other relevant prime divisors when required by the game's number range.

Do not assume that checking divisibility only by numbers up to 11 is sufficient for all numbers used in the game.

### Mathematical correctness

For determining whether a number greater than 1 is prime, it is sufficient to test divisibility by prime numbers up to and including the square root of that number.

Therefore:

- Confirm the maximum number that the game can generate from the existing implementation.
- Record that maximum as `MAX_CLASSIC_NUMBER` in the implementation notes or configuration.
- Ensure the divisibility rules and teaching content cover the relevant prime divisors up to the square root of `MAX_CLASSIC_NUMBER`.
- Do not incorrectly classify a composite number as prime because its smaller divisors were not checked.
- Verify all mathematical explanations and examples before including them in the game.

Do not finalize teaching-tip content until `MAX_CLASSIC_NUMBER` has been confirmed from the existing game code.

### Component requirements

The teaching-tip component should:

- Be reusable.
- Support use during gameplay.
- Support use on game-over screens.
- Present short, easy-to-understand explanations.
- Be visually consistent with the rest of the game.
- Avoid interrupting or blocking normal gameplay.
- Not reference or promote rewarded revive in Phase 2.

Teaching tips should explain why a divisibility rule works or how it helps identify a composite number, rather than simply presenting a rule without context.

## D. My Records — Presentation improvements

Improve the visual presentation and organization of the Classic-only My Records screen implemented in Phase 1.

This phase concerns presentation and usability, not a redesign of the underlying statistics system.

### Presentation requirements

Organize the screen into clearly separated sections:

**1. Personal Records**

Display the player's best Classic score and longest correct-answer streak.

Make personal achievements easy to identify.

Do not display revive-related records in Phase 2.

**2. Overall Statistics**

Display completed games, correct answers, incorrect answers, total numbers answered, and accuracy percentage.

Make the accuracy percentage and total gameplay statistics easy to understand.

**3. Daily Progress**

Display the current daily streak and longest daily streak.

Make the current progress visually clear without adding unnecessary complexity.

**4. Recent Games**

Present recent Classic game results in a clear and readable format.

Do not display revive indicators in Phase 2.

### Design principles

- Use clear visual separation between sections.
- Make important records easy to identify.
- Present numbers and percentages in a readable format.
- Avoid overcrowding the screen.
- Use consistent typography, spacing, and visual elements.
- Reuse the existing game design language.
- Ensure the screen is suitable for the supported device sizes.

**Do not add Daily 10 statistics, records, history, or progress information.**

**Do not add revive-related statistics, records, history, flags, or presentation elements.**

## E. Phase 2 acceptance criteria

Phase 2 is considered complete when:

- New players see How to Play on their first launch.
- Existing players are not unnecessarily forced through onboarding after updating.
- Existing players with prior saved data are migrated to `onboardingComplete = true`.
- The onboarding completion state is saved locally using `onboardingComplete`.
- Completing all five Practice cards in the first-time onboarding flow sets `onboardingComplete` to `true`.
- Exiting Practice before reaching the end does not mark onboarding as complete.
- Replaying Practice from the main menu does not change the onboarding completion state.
- How to Play and Practice can be reopened from the main menu.
- The educational explanations of prime, composite, and neither are mathematically correct.
- Practice contains exactly five distinct cards.
- Each Practice deck includes at least two prime numbers and at least two composite numbers.
- The fifth card may be 1, and 1 may appear only as the fifth card.
- Each Practice deck includes at least one trap number from the specified list.
- Deck generation never produces duplicate numbers within a session or violates the composition rules.
- Practice has no timer, score, or streak impact.
- Practice provides immediate feedback.
- Incorrect answers include a useful explanation.
- Players can skip cards and exit Practice.
- Practice can be replayed.
- Practice does not change Classic statistics, records, or streaks.
- Practice never displays advertisements.
- Practice never references or grants rewarded revive.
- The teaching-tip component can be reused during gameplay and on game-over screens.
- Teaching tips are mathematically correct and appropriate for the game's number range.
- Divisibility rules beyond 11 are included where relevant.
- `MAX_CLASSIC_NUMBER` has been confirmed and teaching content covers primes up to its square root.
- My Records has a clear and readable presentation.
- Personal records, overall statistics, daily progress, and recent results are clearly separated.
- My Records remains Classic-only.
- No revive UI, revive state, revive statistics, revive records, revive fields, or revive-related tests are introduced in Phase 2.

---

# 4. Advertising and monetization boundaries

Advertising should support monetization without frustrating players or interfering with normal gameplay.

### General requirements

- Use AdMob for advertising functionality and advertising performance monitoring.
- Do not introduce a separate analytics SDK or custom analytics system.
- Keep advertisements voluntary.
- Do not introduce forced interstitial advertisements or other unexpected advertising interruptions in Phases 1 and 2.
- Do not display advertisements in How to Play or Practice.
- Do not allow an unavailable advertisement to block gameplay or cause loss of progress.

### Rewarded revive

Rewarded revive is not implemented in Phases 1 and 2. The requirements below apply only from Phase 4 onward.

Where the existing rewarded revive functionality is used:

- The player must voluntarily choose to watch the advertisement.
- The game must only apply the revive after the reward has been successfully granted.
- Ad loading failures, unavailable ads, and cancelled attempts must be handled safely.
- The game must preserve the appropriate game state if an advertisement fails.
- A successful revive continues the existing game session.
- A revive must not create duplicate statistics or game results.

Do not introduce additional advertising placements in these phases unless explicitly required.

---

# 5. Testing requirements for Phases 1 and 2

Test the actual implementation using normal gameplay, application restarts, interruptions, and relevant edge cases.

The following scenarios must be verified before considering Phases 1 and 2 complete.

| Scenario | Expected behavior |
|---|---|
| Open the application without playing | No streak increase |
| Start a Classic game and immediately abandon it | No completed game or streak increase |
| Manually abandon an active Classic game partway through | Active session discarded; no completed game recorded; no streak increase |
| Background or close the app during an active Classic game | Active session preserved and resumable where supported |
| Complete a Classic game with fewer than five answered numbers | Game counted as completed and shown in recent history; no streak increase |
| Complete a qualifying Classic game | Statistics updated once; streak qualifies |
| Complete multiple qualifying games on the same day | Streak increases only once |
| Complete qualifying games on consecutive days | Streak increases correctly |
| Miss an entire calendar day | Current streak resets; longest streak remains |
| Retry a completed game | New game session; no duplicate previous result |
| Reopen a completed game's game-over screen | No duplicate statistics |
| Restart the app after completing a game | Statistics remain correct and are not duplicated |
| Play Practice | No Classic statistics or streak changes; no advertisements |
| Generate a Practice deck | Exactly five distinct numbers; at least two primes; at least two composites; at least one trap number |
| Generate multiple Practice decks | Every deck contains at least two primes, at least two composites, and at least one trap number |
| Include 1 in a Practice deck | 1 appears only as the fifth card; the other four cards satisfy the required composition |
| Skip a Practice card | Next card displayed; no statistics affected |
| Exit Practice before completing all cards from the onboarding flow | `onboardingComplete` remains false |
| Complete all five Practice cards in the onboarding flow | `onboardingComplete` is set to `true` |
| Replay Practice from the main menu | Practice works without resetting `onboardingComplete` |
| Update the app as an existing player with saved data | `onboardingComplete` is set to `true`; onboarding is not forced |
| Start as a new player with no saved data | `onboardingComplete` is `false`; onboarding is offered |
| Exit and restart Practice | No impact on Classic data |
| Play Daily 10 | No Classic statistics or streak changes |
| Play without internet | Core gameplay and locally saved statistics remain functional |
| Update the application | Existing records and statistics remain intact |
| Start with missing or older saved data | Safe initialization or migration without crashing |
| Open My Records without internet | Locally saved data displayed correctly |
| Use Phase 1 and Phase 2 flows | No revive UI, revive state, revive statistics, revive records, or revive-related prompts appear |

---

# 6. Out of scope for Phases 1 and 2

To avoid unnecessary complexity and scope expansion, do not implement the following as part of these phases:

- Daily 10 statistics, records, or historical progress.
- A separate analytics SDK or custom analytics platform.
- Online leaderboards.
- Social features or player accounts.
- Cloud synchronization or cross-device statistics synchronization, unless already supported by the existing architecture.
- Major changes to existing Classic or Daily 10 gameplay mechanics.
- Unrequested redesigns of unrelated screens.
- Forced advertising placements.
- Rewarded revive in any form, including revive UI, revive state, revive persistence fields, revive statistics, revive records, and revive-related recent-game flags.
- Additional monetization systems beyond the existing or explicitly approved voluntary rewarded-ad functionality.

---

# 7. Final implementation priorities

The implementation should follow this order to reduce the risk of introducing bugs or having to redo work.

### Phase 1

1. Review existing architecture, gameplay logic, and saved data.
2. Fix critical stability and persistence issues.
3. Correct Classic game completion and abandonment handling.
4. Implement reliable game identification and duplicate-result prevention.
5. Correct daily streak qualification and date handling.
6. Implement Classic statistics and personal records.
7. Implement the My Records screen.
8. Verify persistence, retries, offline behavior, and edge cases.

### Phase 2

1. Implement first-launch onboarding and How to Play.
2. Implement the five-card Practice mode and enforce its deck composition rules.
3. Implement and persist the `onboardingComplete` flag, including existing-player migration.
4. Verify that Practice is isolated from Classic statistics and advertising.
5. Implement the reusable teaching-tip component.
6. Confirm `MAX_CLASSIC_NUMBER` and verify mathematical correctness across the game's supported number range.
7. Improve My Records presentation and readability.
8. Test the complete first-time player experience and returning-player experience.

### Final principle

The priority is to deliver a stable, reliable, and enjoyable game experience.

Correct game behavior, accurate statistics, reliable local saving, and mathematically correct educational content are more important than adding extra features.

Keep the implementation as simple as possible while satisfying all requirements and acceptance criteria in this document.

Do not add Daily 10 statistics in this phase.

---

# 8. Phase 3 — Daily 10

Implement Daily 10 as a separate daily challenge.

### A. Gameplay rules

- 10 cards per challenge.
- 5 prime and 5 composite numbers.
- Mix of two-, three-, and four-digit numbers.
- No repeated numbers within a daily challenge.
- 8 seconds per card.
- Wrong answers and timeouts advance to the next card.
- No revive option.
- No ads during the challenge.

### B. Daily challenge consistency

The same daily challenge must be presented consistently to all players.

Requirements:

- Generate the daily deck deterministically.
- Use UTC as the daily reset reference.
- Ensure the same deck is generated across devices, app versions, and time zones.
- Version the deck-generation logic.
- Include fixed test cases so the developer can verify expected daily decks.
- Define a migration strategy if deck-generation logic changes in a future update.

### C. Persistence

- Save the player's current challenge progress.
- Allow the player to leave and resume without losing progress.
- Prevent restarting the same daily challenge to obtain a different deck.

### D. Results screen

Display:

- Final score.
- Correct answers.
- Incorrect answers.
- Completion time, if applicable.
- Personal best.
- Share result option.

### E. Daily 10 statistics — add in this phase

Extend My Records to include a separate Daily 10 section.

Include:

- Daily 10 games completed.
- Daily 10 personal best.
- Daily 10 completion history.
- Daily 10 performance over time.
- Daily 10 streak information, if applicable.

Keep Daily 10 records separate from Classic records. Do not combine the two modes into one overall score or accuracy figure.

### F. Additional features

- Add a Daily 10 ready screen.
- Add a run review showing the numbers answered and the correct answers.
- Show a relevant teaching tip after a game-over event.

---

# 9. Phase 4 — Voluntary monetization

### A. Rewarded revive

Implement a rewarded ad that allows a player to continue a Classic run after failing.

Rules:

- Available only in Classic mode.
- Maximum one revive per run.
- Completely optional.
- The player must choose to watch the ad.
- Do not grant the revive unless the rewarded-ad completion callback confirms completion.
- Handle ad loading failures and unavailable ads gracefully.
- Do not interrupt active gameplay with an ad.

### B. Remove Ads purchase

Implement a one-time, non-consumable in-app purchase.

Proposed starting price: **$3.99**, subject to final pricing decision.

Requirements:

- Purchase removes interstitial ads.
- Rewarded revive remains available.
- Support purchase restoration.
- Persist purchase status correctly.
- Handle cancelled and failed purchases.
- Verify the billing result before granting the purchase.

---

# 10. Phase 5 — Interstitial ads (conditional)

Do not prioritise interstitial ads until the core experience is stable.

If introduced, use conservative limits:

- First five lifetime completed Classic runs are ad-free.
- Maximum one interstitial per four eligible completed runs.
- Minimum 120 seconds between interstitials.
- Maximum two interstitials per UTC day.
- Show only after the player voluntarily taps Try Again.

Never show an interstitial:

- During gameplay.
- During Daily 10.
- Immediately after a personal best.
- Instead of or immediately after a rewarded revive offer.
- After a successful rewarded revive.
- After a Remove Ads purchase.
- When the ad is unavailable.

---

# 11. AdMob monitoring

Use the AdMob dashboard as the initial source of advertising performance information.

Monitor:

- Ad impressions.
- Estimated earnings.
- eCPM.
- Match rate and fill rate.
- Rewarded-ad performance.
- Interstitial performance, once introduced.

No separate analytics SDK, custom event tracking, retention dashboard, or analytics implementation is required for this release.

AdMob measures advertising performance, not complete player retention or gameplay behaviour.

---

# 12. Features to postpone

Do not implement these in the current release:

- Leaderboards.
- Social features.
- Friend challenges.
- Cosmetics and custom themes.
- Prime Hunt.
- Subscriptions.
- Lives or energy systems.
- Pay-to-win mechanics.
- Complex A/B testing.
- Advanced analytics.
- Additional localization beyond what is already supported.

---

# 13. Technical requirements

- Reliable local persistence.
- Correct handling of app restarts and interruptions.
- Offline functionality for core gameplay.
- Mathematically correct prime/composite classification.
- Reliable Classic statistics and personal-record storage.
- Safe data migration between app versions, using an explicit local schema version.
- Deterministic Daily 10 generation.
- Correct handling of UTC daily resets.
- Proper AdMob rewarded-ad callbacks (Phase 4).
- Proper Google Play Billing integration and purchase restoration (Phase 4).
- Test ad unit IDs during development.
- Graceful handling of unavailable ads and network failures.

---

# 14. Release sequence

| Release | Scope |
|---|---|
| 2.1a | Stability, local saving, streak correction, Classic-only My Records |
| 2.1b | How to Play, practice mode, improved Classic statistics presentation |
| 2.2 | Daily 10, persistence, results, Daily 10 statistics, sharing and run review |
| 2.3 | Rewarded revive and Remove Ads purchase |
| 2.4 (optional) | Conservative interstitial ads |

---

# 15. Testing checklist

Phase 1 and Phase 2 items:

- [ ] Streaks update only after qualifying Classic activity.
- [ ] Streaks survive app restarts.
- [ ] Manual abandonment does not record a completed game.
- [ ] Interrupted games are resumable where supported.
- [ ] My Records is accessible from the main menu.
- [ ] Classic personal records save and update correctly.
- [ ] Classic statistics calculate correctly.
- [ ] Accuracy percentage is calculated correctly.
- [ ] Incomplete games do not inflate completed-game totals.
- [ ] Short completed games appear in recent history but do not qualify for the streak.
- [ ] Retrying a game does not double-count results.
- [ ] Records survive app updates.
- [ ] Existing players are migrated to `onboardingComplete = true`.
- [ ] No Daily 10 statistics are included before Phase 3.
- [ ] Practice mode does not affect scores or streaks.
- [ ] Practice never shows advertisements.
- [ ] Prime/composite classification is correct.
- [ ] No revive UI, state, statistics, or records exist before Phase 4.

Later-phase items:

- [ ] Daily 10 contains exactly 10 cards, with 5 prime and 5 composite. (Phase 3)
- [ ] Daily 10 is consistent across time zones and devices. (Phase 3)
- [ ] Daily 10 progress survives app restarts. (Phase 3)
- [ ] Daily 10 cannot be rerolled by restarting the app. (Phase 3)
- [ ] Daily 10 results update the correct statistics. (Phase 3)
- [ ] Rewarded revive works only after confirmed ad completion. (Phase 4)
- [ ] A player cannot revive more than once per run. (Phase 4)
- [ ] Remove Ads purchase works and can be restored. (Phase 4)
- [ ] Rewarded revive remains available to Remove Ads purchasers. (Phase 4)
- [ ] Interstitial frequency limits are respected, if implemented. (Phase 5)
- [ ] No ads appear during Daily 10 or active gameplay. (All phases)

---

# 16. Developer handover and reporting

For each release, the developer should provide:

1. A list of implemented features.
2. A list of fixed bugs.
3. A list of unfinished items.
4. Confirmation that the testing checklist has been completed.
5. Screenshots or a short recording of new screens and flows.
6. Any technical decisions that require approval.

Do not add extra features without approval if they increase scope or delay the planned release.

---

## Final priority

Build a polished Classic experience first. Give players a reason to improve through personal records and visible progress. Introduce Daily 10 and its own statistics later, as a separate feature. Keep monetization voluntary and unobtrusive, and use AdMob to understand initial advertising performance without implementing a separate analytics system.