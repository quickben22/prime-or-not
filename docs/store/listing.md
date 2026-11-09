# Play Store listing copy

Everything Play Console asks for on the **Main store listing** page. Copy the
text between the rules; do not add markdown.

| Field | Limit | Value |
|---|---|---|
| App name | 30 | `Prime or Not` |
| Short description | 80 | see below |
| Full description | 4000 | see below |
| Store listing languages | — | English (United States) only |
| App category | — | Games › Puzzle |
| Tags | — | Puzzle, Brain training, Educational |
| Contact email | — | `quickben22@gmail.com` |
| Privacy policy | — | `https://quickben22.github.io/privacy.html` |
| Website | — | `https://quickben22.github.io/` |
| Ads | — | Yes, contains ads |

Before uploading, run the listing check:

```powershell
powershell -ExecutionPolicy Bypass -File tools/check_store_listing.ps1
```

It enforces the 80/4000 character limits and fails if the short and full
descriptions are identical.

## Short description (70 characters)

```text
A fast number game: decide if each card is prime before time runs out.
```

## Full description

```text
Prime or Not is a quick decision game about prime numbers. Each card shows one
whole number, and you have a few seconds to decide whether it is prime. Swipe
right for prime, left for not prime, then take the next card.

Classic mode

• Every correct answer adds a point and gives you a fresh timer. The first wrong
  answer or timeout ends the run.
• Cards start in the two-digit range, move to three digits after ten correct
  answers, and then to four digits after twenty-five.
• The time limit follows the card: five seconds for two digits, six for three,
  seven for four.
• Half the cards are prime and half are composite. From three digits up, easy
  composites such as even numbers and multiples of five are excluded, so
  guessing from the last digit will not work.
• When a run ends, the summary shows the correct answer for the card that beat
  you, including its factorization when it is composite, along with your score,
  accuracy, best streak and fastest answer.

Practice mode

• Five cards from 1 to 100, with no timer, no score and no streak.
• After each answer, the app explains whether the number is prime or composite
  and why.
• If the number 1 appears, the app treats it as a special case: it is neither
  prime nor composite.

How to Play and My Records

• How to Play explains prime numbers, composite numbers and the rule for 1 in
  plain language.
• My Records keeps your best Classic score, longest streak, overall accuracy,
  daily progress and recent completed runs on your device.
• An interrupted Classic run can be resumed or discarded from the main menu.

No account is needed and the game works offline. Scores stay on the device. A
single banner ad appears on the game-over screen only; there are no ads during a
round and no pop-ups between cards.

A run can last seconds, and a good run can last a minute, so Prime or Not fits a
queue, a commute or a short break. It is a simple way to practise mental
arithmetic and chase a personal best.

Swipe carefully.
```

## Store listing languages

The listing is **English (United States) only**. In Play Console, keep the
default language and remove every other language, including English variants.
Do not add machine translations, and never paste the short description into the
full description field.

If a translation is added later, it must have its own short and full text. Play
rejects listings where the translated short and long descriptions are identical.

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
- **Translations:** none — English only
- **Free / Paid:** Free
- **Content rating:** Everyone / PEGI 3 (questionnaire answers: no violence, no
  language, no controlled substances, no gambling, no user-generated content,
  no data sharing beyond ads, no purchases)
- **Target audience:** 13+ (the ad-supported build is not enrolled in Families)
