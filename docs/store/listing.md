# Play Store listing copy

Everything Play Console asks for on the **Main store listing** page. Copy the
text between the rules; do not add markdown.

| Field | Limit | Value |
|---|---|---|
| App name | 30 | `Prime or Not` |
| Short description | 80 | see below |
| Full description | 4000 | see below |
| App category | — | Games › Puzzle |
| Tags | — | Puzzle, Brain training, Educational |
| Contact email | — | `quickben22@gmail.com` |
| Privacy policy | — | `https://quickben22.github.io/privacy.html` |
| Website | — | `https://quickben22.github.io/` |
| Ads | — | Yes, contains ads |

## Short description (78 characters)

```text
Swipe right for prime, left for not prime. Beat the clock and build a streak.
```

## Full description

```text
A number appears. You have five seconds to decide.

Swipe right for prime. Swipe left for not prime.

That is the whole game — and it gets hard much faster than you expect. Every
correct answer adds a point and resets the clock. The first wrong swipe or the
first timeout ends the run.

WHY IT WORKS

• One rule, no tutorial. Start playing in two seconds.
• Two-digit cards for the first ten points, then three digits, then four.
• The clock tightens as you climb: 5 seconds, then 6, then 7.
• Composites are built to look prime — odd numbers, no obvious factors — so
  guessing "it's even" will not carry you.
• The run summary shows the factorization of the card that beat you, plus your
  best streak, accuracy and fastest answer.
• A daily streak and a saved best score give you a reason to come back.

NO CLUTTER

No account, no sign-in, no tutorial, no energy timers, no pop-ups between cards.
One banner ad appears on the game-over screen only, never during a round.

BUILT FOR QUICK SESSIONS

A run lasts seconds, a good run lasts a minute. Perfect for a queue, a commute,
or the gap between two things.

Whether you are refreshing mental arithmetic or chasing a personal best, Prime
or Not turns a five-second decision into a habit.

Swipe carefully.
```

## Graphics checklist

Upload from `docs/store/`:

| Asset | File | Spec |
|---|---|---|
| App icon | `icon-512.png` | 512 × 512 PNG, 32-bit, no transparency |
| Feature graphic | `feature-graphic-1024x500.png` | 1024 × 500 PNG/JPEG, no transparency |
| Phone screenshots | `screenshots/phone-*.png` | 1080 × 1920, 4–8 images |

Screenshot slots for the upload (in this order):

1. `phone-01-ready.png` — start screen, face-down card, BEST 2
2. `phone-02-playing.png` — 81 on the card, score 1, full timer
3. `phone-03-wrong.png` — wrong guess, `9 = 3²`, run summary
4. `phone-04-timeout.png` — out of time, `71 is prime`, 100% accuracy
5. `phone-05-new-best.png` — NEW BEST badge with score 2

Captured on the Pixel 5 AVD, 1080 × 1920, status bar in demo mode. Networking
was off, so no test ad appears in any shot. Do not upload a screenshot that
contains an ad.

## Category and tags

- **App category:** Games › Puzzle (alternate: Games › Educational)
- **Tags:** Puzzle, Brain training, Educational
- **Contains ads:** Yes
- **In-app purchases:** No

## Store settings to confirm

- **App name:** Prime or Not (matches `@string/app_name`)
- **Package:** `com.companyname.primeornot` (cannot change)
- **Default language:** English (United States)
- **Free / Paid:** Free
- **Content rating:** Everyone / PEGI 3 (questionnaire answers: no violence, no
  language, no controlled substances, no gambling, no user-generated content,
  no data sharing beyond ads, no purchases)
- **Target audience:** 13+ (the ad-supported build is not enrolled in Families)
