# Prime or Not — Play release runbook and work log

Status: **2.1.1 hotfix ready to upload.** `versionCode 5` (2.1.0) crashed on
launch in the release build because R8 stripped a reflectively-used Room
constructor (see the 9 November hotfix entry in §10). `app/proguard-rules.pro`
now keeps it, the signed bundle is rebuilt and verified, and the release APK was
cold-started on a device. Upload
`app/build/outputs/bundle/release/app-release.aab` to Internal testing (§8).
After that: smoke test, Closed testing, Production, and the App content forms.

Last updated: 9 November 2026.

This file records everything that was changed to turn the local prototype into a
Play submission, why each change was made, and the exact steps left for a human
in Play Console and AdMob.

---

## 1. App identity

| | |
|---|---|
| Play listing | `PRIME OR NOT` (existing app, do not create a new one) |
| Display name | **Prime or Not** |
| Package / applicationId | `com.companyname.primeornot` — permanent |
| This upload | `versionCode 6`, `versionName 2.1.1` |
| Previous uploads | `versionCode 5` / `2.1.0` (release-only R8 crash), `versionCode 2` / `1.1` (Closed testing – Alpha), `versionCode 1` / `1.0` (Internal testing) |
| minSdk / targetSdk / compileSdk | 24 / 37 / 37 |
| Category | Games › Puzzle |
| Price | Free, contains ads, no in-app purchases |

The package was kept because package names are permanent; it still reads
`com.companyname.…` from the 2020 Android Studio template, and renaming it
would create a second app with no install history. `versionName` jumped from 1.1
to 2.0.0 because this is a full Kotlin/Compose rewrite, not a patch; 2.0.1 was
the 7 November icon change, 2.1.0 is the Phase 1/2 stability, records,
Practice and green-brand release, and 2.1.1 fixes the release-only startup
crash found in 2.1.0.

`versionCode` must keep increasing. It is now a single constant in
`app/build.gradle.kts` (`appVersionCode` / `appVersionName`) so the manifest,
the verification task and this document cannot drift apart.

---

## 2. Release signing (solved — no key reset needed)

The 2020 app was signed with a key that still exists on this machine. It was
found, matched to the certificate exported from Play Console, and copied to a
dedicated location with a new password.

### Verified match

| Item | Value |
|---|---|
| Original key | `D:\Visual studio\BetsKeeper\Frisk-bets-keeper-xamarin-8b72a42ebca0\AndroidTest3\AndroidTest3.Android\quickben.keystore` |
| Type / alias | JKS, alias `quickben`, 2048-bit RSA, SHA256withRSA, valid to 2049 |
| Play upload certificate | `upload_cert.der`, subject `C=HR, S=Zagreb, L=Zagreb`, serial `7ce59e9e` |
| SHA-256 (both) | `2B:41:8C:E6:FE:AE:83:09:8B:71:8A:0F:E2:87:02:86:5A:F5:BC:CB:3E:EA:00:41:FE:DA:95:60:F7:21:20:84` |
| Result | Exact match; no upload-key reset required |

### Where the key lives now

```text
C:\Users\armin\.android-keys\
├── prime-or-not-upload.p12      <- Gradle signs with this (alias quickben)
├── prime-or-not-upload-cert.der <- public certificate from Play Console
├── quickben-original-backup.jks <- untouched 2019 JKS copy
└── README.txt                   <- backup notes + fingerprint
```

The `.p12` is the same key pair re-wrapped in PKCS#12 with a new random
password. The certificate and private key are byte-for-byte the originals, so
Play accepts it as the existing upload key.

The password is **not** in this repo. It is stored in `local.properties`
(gitignored) as `release.storePassword` / `release.keyPassword`; the same file
holds `release.storeFile` and `release.keyAlias`.

**Back this folder up offline, together with the contents of `local.properties`.**
Losing the upload key does not lose the app (Play App Signing holds the app
signing key), but it forces an upload-key reset before the next release.

### Confirmed in Play Console

Checked on 26 October 2026:

1. Play App Signing is **enabled**.
2. The **upload certificate** SHA-256 is exactly
   `2B:41:8C:E6:FE:AE:83:09:8B:71:8A:0F:E2:87:02:86:5A:F5:BC:CB:3E:EA:00:41:FE:DA:95:60:F7:21:20:84`.

The app-signing certificate fingerprint is different and is expected to be —
Google holds that key. If the upload certificate is ever reset or the key is
recreated, repeat this check before uploading.

### Build guard

- `validateReleaseSigning` runs before `bundleRelease` and fails when any of the
  four values is blank or the keystore file is missing.
- `verifyReleaseBundle` builds the bundle, checks the manifest contains
  `com.companyname.primeornot` and the current `appVersionName` (`2.1.1`), then runs `keytool -printcert
  -jarfile` and fails unless the signer is the fingerprint above.

---

## 3. AdMob (configured)

The release bundle uses the real AdMob app and banner unit:

| | |
|---|---|
| AdMob app | Prime Or Not (Android) — `ca-app-pub-3535032153893847~2009618845` |
| Banner unit | Prime Banner — `ca-app-pub-3535032153893847/2509721149` |
| Publisher | `pub-3535032153893847` |

The app ID is the AdMob app originally created for BetsKeeper, renamed to
Prime Or Not; same publisher, confirmed by the owner on 25 Oct 2026. The banner
unit is new and is the only ad unit the game requests.

`app/build.gradle.kts` reads:

```properties
# local.properties — gitignored, not committed
admob.appId=ca-app-pub-3535032153893847~2009618845
admob.bannerUnitId=ca-app-pub-3535032153893847/2509721149
```

- Debug builds always use Google's sample IDs, so they work offline and can be
  screenshotted without serving real ads.
- Release builds use the configured IDs. `validateReleaseAdIds` fails if either
  is blank or still contains Google's sample publisher `3940256099942544`.
- `verifyReleaseBundle` also checks that the built bundle manifest contains the
  real app ID and not the sample publisher.
- `-PALLOW_TEST_ADS=true` is the only escape hatch and must never be uploaded.

If the AdMob app or unit is ever recreated:

1. **Apps → Add app → Android**, name `Prime or Not`, copy the **App ID**
   (`ca-app-pub-…~…`).
2. **Ad units → Banner →** name `Prime or Not — game over`, copy the **unit ID**
   (`ca-app-pub-…/…`).
3. Put both in `local.properties`. The publisher number (16 digits after
   `ca-app-pub-`) must be identical in both IDs; the helper script checks this.

The banner is only requested on the game-over screen, after UMP consent allows
ad requests. `Ready` and `Playing` render no ad slot at all.

The helper has been run, and `docs/site/app-ads.txt` is committed:

```text
google.com, pub-3535032153893847, DIRECT, f08c47fec0942fa0
```

That file must be served at `https://quickben22.github.io/app-ads.txt`, and the
Play listing website must be `https://quickben22.github.io/`. AdMob crawls it to
authorise sellers.

```powershell
# after any AdMob change
powershell -ExecutionPolicy Bypass -File tools/write_app_ads_txt.ps1
Copy-Item docs\site\app-ads.txt ..\quickben22.github.io\ -Force
```

---

## 4. Public site (`quickben22/quickben22.github.io`)

Source lives in `docs/site/` and is published by GitHub Pages from the repo root.

| File | URL | Purpose |
|---|---|---|
| `index.html` | `https://quickben22.github.io/` | game page, linked from Play as website |
| `privacy.html` | `https://quickben22.github.io/privacy.html` | privacy policy required by Play |
| `app-ads.txt` | `https://quickben22.github.io/app-ads.txt` | AdMob authorised sellers (generated) |
| `icon-512.png` | `https://quickben22.github.io/icon-512.png` | icon on the page |

The policy covers: local-only scores, the AdMob banner, approximate location /
app interactions / diagnostics / device identifiers, EEA-UK consent via UMP and
how to withdraw it, children, retention, deletion and contact.

The site is live. The repo is public, Pages is enabled (**Deploy from a branch →
`main` / `/ (root)`**), the build for `12087ad` completed, and all three URLs
return 200:

```text
https://quickben22.github.io/                 200  (game page)
https://quickben22.github.io/privacy.html     200  (privacy policy)
https://quickben22.github.io/app-ads.txt      200  google.com, pub-3535032153893847, DIRECT, f08c47fec0942fa0
```

To republish after a change, copy the site sources, commit and push:

```powershell
cd "C:\Users\armin\Documents\Prime or not"
Copy-Item docs\site\* ..\quickben22.github.io\ -Force
cd ..\quickben22.github.io
git add -A; git commit -m "Update site"; git push
```

GitHub rebuilds Pages automatically; no settings change is needed again. If the
AdMob publisher or app changes, regenerate `app-ads.txt` first:

```powershell
cd "C:\Users\armin\Documents\Prime or not"
powershell -ExecutionPolicy Bypass -File tools\write_app_ads_txt.ps1
```

### Repositories

| Repo | Visibility | Branch | Pushed |
|---|---|---|---|
| `quickben22/prime-or-not` | private | `main` | yes, 25 Oct 2026 |
| `quickben22/quickben22.github.io` | public | `main` | yes, 25 Oct 2026 |

GitHub credentials are stored in Windows Credential Manager for user
`quickben22`, so `git push` no longer prompts. To publish source changes:

```powershell
cd "C:\Users\armin\Documents\Prime or not"
git push
```

---

## 5. What changed in the code

### Rename and branding

- Package and namespace: `com.primetinder` → `com.companyname.primeornot`
  (31 files updated; sources moved to
  `app/src/main/java/com/companyname/primeornot/`, same for `test` and
  `androidTest`).
- Visible name: "Prime Tinder" → "Prime or Not" in `settings.gradle.kts`,
  `strings.xml`, README and architecture notes.
- Theme names: `Theme.PrimeTinder` → `Theme.PrimeOrNot`,
  `PrimeTinderTheme` → `PrimeOrNotTheme`.
- DataStore file: `prime_tinder_scores` → `prime_or_not_scores`. This resets
  local scores once on upgrade — acceptable for a 2020 prototype.
- Root project name: `Prime or Not`.

### Version and Play metadata

- `versionCode 6`, `versionName 2.1.1`.
- Release signing config sourced from `local.properties` / Gradle properties /
  environment variables, never committed.
- `validateReleaseSigning` and `verifyReleaseBundle` tasks (see §2).
- `validateReleaseAdIds` already existed and still blocks sample ad IDs.

### Launcher icon

The mark is the original 2020 Xamarin artwork: black `1||0` glyphs on the app
accent green `#00C2A8`, taken from `quickben22/PrimeOrNot`,
`PrimeOrNot.Android/Resources/mipmap-xxxhdpi/launcher_foreground.png` (432 px).
The ring-and-"P" design was dropped on 7 November 2026; the field was changed
from red `#FF2F17` to green on 9 November 2026 so the launcher, store artwork,
site icon and in-app logo match the app accent.

- Adaptive icon: `mipmap-anydpi-v26/ic_launcher.xml` and
  `ic_launcher_round.xml` use `drawable-xxxhdpi/ic_launcher_foreground.png` (the
  glyph mask scaled to 68 %, so the `1` and `0` stay inside the 72 dp safe zone)
  plus `drawable-xxxhdpi/ic_launcher_monochrome.png` for themed icons.
- Legacy PNG mipmaps for mdpi–xxxhdpi keep the original full-width proportions:
  square for `ic_launcher`, circular for `ic_launcher_round`, so API 24–25
  launchers get the same mark.
- `AndroidManifest.xml` points `android:icon` at `@mipmap/ic_launcher` and
  `android:roundIcon` at `@mipmap/ic_launcher_round`.
- Background colour resource `values/ic_launcher_background.xml` (`#00C2A8`).
- The in-app logo is `ui/PrimeLogo.kt`: the same foreground bitmap over
  `PrimeAccent`, used by Home and the Classic Ready card.

### Store artwork (`docs/store/`)

All artwork is reproducible from HTML/SVG sources in the same folder, rendered
with headless Chrome.

| File | Size | Use |
|---|---|---|
| `icon-512.png` | 512×512 | Play app icon |
| `feature-graphic-1024x500.png` | 1024×500 | Play feature graphic |
| `icon.html`, `icon-source.svg` | — | icon source (self-contained: the 432 px glyph mask is embedded as base64 over the `#00C2A8` field) |
| `feature-graphic.html` | — | feature graphic source |
| `screenshots/` | 1080×1920 | phone screenshots (see below) |

`icon-512.png` is composed at 512 px from the same glyph mask, not upscaled from
a smaller bitmap, and is copied unchanged to `docs/site/icon-512.png`. The
feature graphic uses the same mark; every accent is the app green `#00C2A8`.

### Privacy site and tooling

- `docs/site/index.html`, `privacy.html`, `icon-512.png`,
  `app-ads.txt.example`.
- `tools/write_app_ads_txt.ps1` — derives the publisher ID from
  `admob.appId`, refuses sample IDs, cross-checks the banner unit's publisher,
  and writes `docs/site/app-ads.txt`. Supports `-Check` and `-OutputPath`.
- `.gitignore` now excludes `*.jks`, `*.keystore`, `*.p12`, `*.pfx`,
  `keystore.properties`, `local.properties` and `upload_cert.der`.
- `upload_cert.der` was moved out of the repo to `~\.android-keys\`.

2.1.x adds resumable Classic sessions, Classic-only records and streak rules,
onboarding/How to Play, isolated Practice, teaching tips and the rebranded
green mark; 2.1.1 is the same feature set with the release startup fix. The core
swipe, timer and scoring rules are unchanged; see §10 and
`docs/ARCHITECTURE.md` for the full behaviour.

---

## 6. Build and test

Prerequisite: JDK 17+. The machine default is JDK 11, so always override:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
```

Unit tests, lint and the debug APK:

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

Optional instrumented tests (single device; do not use
`connectedDebugAndroidTest` with several emulators attached):

```powershell
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
& $adb -s emulator-5554 install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb -s emulator-5554 shell am instrument -w com.companyname.primeornot.test/androidx.test.runner.AndroidJUnitRunner
```

Release bundle (needs the configured AdMob IDs and the upload key):

```powershell
.\gradlew.bat verifyReleaseBundle
```

`verifyReleaseBundle` depends on `bundleRelease`, so it builds and checks in one
step. The artifact is `app/build/outputs/bundle/release/app-release.aab`.

Local-only release smoke test with sample ads (never upload this):

```powershell
.\gradlew.bat assembleRelease -PALLOW_TEST_ADS=true
```

### Smoke-test the signed release (required before upload)

Debug builds do not run R8, so they cannot catch release-only failures — an
R8 keep-rule crash shipped in `versionCode 5` exactly because only the debug
build had been launched. Install the signed APK and cold-start it before every
upload:

```powershell
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s <device> install -r app\build\outputs\apk\release\app-release.apk
& $adb -s <device> shell am start -W -n com.companyname.primeornot/.MainActivity
& $adb -s <device> shell pidof com.companyname.primeornot   # must print a pid
```

For the Play-delivered build, repeat the same check after installing from the
internal-testing opt-in link. `verifyReleaseBundle` also fails if R8 removed the
reflectively-used `WorkDatabase_Impl.<init>()` constructor.

### Result of the last run

- `testDebugUnitTest`: **55 tests, 0 failures, 0 skipped** (7 suites).
- `lintDebug`: **0 errors, 9 warnings** — 2 `AndroidGradlePluginVersion`
  (AGP is pinned deliberately, see `docs/ARCHITECTURE.md#build`),
  2 `PluralsCandidate`, and 5 `IconLauncherShape` for the intentionally
  full-bleed legacy square mipmaps.
- `assembleDebug`: debug APK 18,053,786 bytes, installs and runs on API 35.
- `assembleRelease` + install on `emulator-5554` (API 35, Pixel 5): version
  `2.1.1` (code 6) cold-starts, `MainActivity` stays resumed, `pidof` returns a
  process, the crash buffer is empty and `dumpsys activity exit-info` has no
  entry. This is the check that was missing for `versionCode 5`.
- `verifyReleaseBundle` with the real IDs: **passed** — package
  `com.companyname.primeornot`, version `2.1.1` (code 6), signer
  `2B:41:8C:E6:…:20:84`, 7,232 KiB. The bundle manifest contains
  `ca-app-pub-3535032153893847~2009618845`, `classes.dex` contains the banner
  unit `2509721149`, and Google's sample publisher `3940256099942544` appears
  nowhere. SHA-256 of the artifact:
  `7528AB1331B26F81C5ED7FD14CE1D48E985A27B669EEA944EEFB1A51CEDD017F`.
- The new WorkDatabase guard scans for exactly the `public void <init>()`
  removal that the `versionCode 5` `usage.txt` contained.
- `connectedDebugAndroidTest` on `emulator-5554` (API 35, Pixel 5):
  **11/11 `GameScreenTest` cases, OK** on 9 November.

---

## 7. Screenshots

Captured on `emulator-5554` (API 35, Pixel 5 AVD). The emulator's native
1080×2340 is not a valid Play ratio (max 2:1), so the display was temporarily
set to 1080×1920 and restored afterwards with `wm size reset`. Networking is
disabled for the game-over shots so the banner never appears in a store image.

```powershell
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb shell wm size 1080x1920
& $adb shell svc wifi disable
& $adb shell svc data disable
# status bar: demo mode with clock 09:41 and a full battery
& $adb shell settings put global sysui_demo_allowed 1
& $adb shell am broadcast -a com.android.systemui.demo -e command enter
& $adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941
# ... play to the required state, then:
& $adb shell screencap -p /sdcard/shot.png
& $adb pull /sdcard/shot.png docs\store\screenshots\phone-02-playing.png
& $adb shell rm /sdcard/shot.png
# restore
& $adb shell am broadcast -a com.android.systemui.demo -e command exit
& $adb shell wm size reset
& $adb shell svc wifi enable
& $adb shell svc data enable
```

Files are listed in `docs/store/listing.md` with the order to upload them.

Captured set (all 1080×1920, BEST 2 for continuity):

| File | State |
|---|---|
| `screenshots/phone-01-ready.png` | start card, TAP TO START |
| `screenshots/phone-02-playing.png` | card 81, score 1, 5 s timer |
| `screenshots/phone-03-wrong.png` | wrong guess, `9 = 3²` |
| `screenshots/phone-04-timeout.png` | out of time, `71 is prime`, 100% accuracy |
| `screenshots/phone-05-new-best.png` | NEW BEST, score 2 |

On 9 November 2026 the logo circle in `phone-01-ready.png` was recolored in
place (centre 540,1019; diameter ≈238 px) rather than recaptured, so the rest of
the card and the status bar are unchanged.

---

## 8. Play Console checklist

These pages must be green before a production rollout. Most are one-time.

### Store listing

- [ ] App name **Prime or Not**, short and full description from
      `docs/store/listing.md`
- [ ] App icon 512×512, feature graphic 1024×500, 4–8 phone screenshots
- [ ] Category Games › Puzzle, tags, contact email
- [ ] Privacy policy URL `https://quickben22.github.io/privacy.html`
- [ ] Website `https://quickben22.github.io/`

### App content

- [ ] **Privacy policy** URL saved
- [ ] **Ads:** yes, app contains ads
- [ ] **App access:** all functionality available without restrictions (no login)
- [ ] **Content rating:** complete the IARC questionnaire (game, no violence,
      no language, no controlled substances, no gambling, no UGC, contains ads)
- [ ] **Target audience:** 13+; not designed for children, not in Families
- [ ] **Data safety:** see the table below
- [ ] **News app:** no
- [ ] **COVID-19 apps:** no
- [ ] **Data deletion:** yes, point at the privacy policy (see note)
- [ ] **Financial features:** none
- [ ] **Health:** none
- [ ] **Government app:** no

### Data safety answers

Data is collected by the Google Mobile Ads SDK, not by the developer, but Play
requires the developer to declare it.

| Data type | Collected | Shared | Purposes |
|---|---|---|---|
| Approximate location | yes | yes | Advertising or marketing; Analytics |
| App interactions | yes | yes | Advertising or marketing; Analytics; Fraud prevention, security and compliance |
| Diagnostics | yes | yes | Analytics; Fraud prevention, security and compliance |
| Device or other IDs | yes | yes | Advertising or marketing; Analytics; Fraud prevention, security and compliance |

- Encrypted in transit: **yes**
- Users can request deletion: **yes** → `https://quickben22.github.io/privacy.html`
  (there is no developer server or account; the policy explains device-data
  clearing and Google's ad controls)
- Committed to the Play Families policy: **no**
- Independent security review: **no**

Do not declare precise location, contacts, files, health, messages or financial
information — the app requests none of those permissions.

### Test and release

**Internal testing upload — exact steps**

1. **Test and release → Testing → Internal testing → Create new release.**
2. Keep the existing **Play App Signing** setup; do not create a new key.
3. Under **App bundles → Upload**, select
   `C:\Users\armin\Documents\Prime or not\app\build\outputs\bundle\release\app-release.aab`
   (7,405,676 bytes, SHA-256
   `7528AB1331B26F81C5ED7FD14CE1D48E985A27B669EEA944EEFB1A51CEDD017F`).
   Upload the AAB, never an APK, and never a build made with
   `-PALLOW_TEST_ADS=true`.
4. Wait for “bundle uploaded successfully”; the release name fills in as
   **2.1.1 (6)**. Paste the release notes below.
5. Resolve any **errors** Play reports. Warnings can be reviewed and accepted.
6. **Save → Review release**, confirm version **6 (2.1.1)** and the target
   countries, then **Start rollout to Internal testing → Rollout**.
7. If **Managed publishing** is enabled, go to **Publishing overview** and click
   **Publish changes**. Internal testing normally goes live in minutes.
8. **Testers** tab: put your Google account in the list, copy the opt-in URL,
   open it on the phone, accept and install. Confirm the installed version is
   **2.1.1**.
9. Smoke-test: **cold-start the app and confirm it stays in the foreground**,
   then the start screen, several swipes, one wrong answer, one timeout, retry,
   then force-stop and confirm the best score persisted. Test on API 35 and, if
   available, API 24.

**Promotion**

10. **Closed testing → Alpha → Create new release → Add from library**, select
    `2.1.1 (6)`, and roll out to the existing testers.
11. When production is unlocked: **Production → Create new release**, same
    bundle, staged rollout (e.g. 20% → 100%).
12. If Play shows **Apply for production access** instead, complete that form
    (it appears for apps that have never had a production release). The
    12-tester rule applies only to personal developer accounts created after
    13 November 2023; this account predates it.

After the upload, **App bundle explorer → version 6** shows the app-signing
certificate, which intentionally differs from the upload certificate.

### Release notes (copy/paste)

```text
2.1.1
• Fixes a crash on launch in the release build (2.1.0)
• Resume or abandon an interrupted Classic run
• Streak and records now track Classic only
• Practice mode and teaching tips
• Green 1||0 icon, store art and start card
```

---

## 9. Remaining work

### Resume here (Play Console only)

Fingerprint confirmed, repos pushed, site live. What is left:

1. **Upload `app-release.aab`** — Internal testing → Create new release →
   upload the AAB → release notes `2.1.1` (§8). This replaces the crashing
   `versionCode 5`.
2. **Smoke-test** the installed build: confirm it **stays open after launch**,
   then start, swipes, wrong answer, timeout, retry, score persistence.
3. **Promote** the same bundle to Closed testing → Alpha, then Production.
4. **Complete the store listing and App content forms** (§8).
5. **After the first install:** link the AdMob app to the Play listing and
   confirm the game-over banner fills.

Both repos are pushed and the site is live, so the privacy-policy and website
URLs are safe to paste into Play now.

### After the first Play install

- In AdMob, link the app to the Play listing (App settings → App store) so
  AdMob can verify the store presence.
- With networking on, play one round, fail it, and confirm the game-over banner
  fills. A new ad unit can take up to an hour to start serving.

### Optional follow-ups

- Reset Prime or Not to a dedicated upload key. The current key is shared with
  an old BetsKeeper project; Play App Signing means a reset only changes the
  upload credential, not the installed app.
- The old BetsKeeper folder contains the keystore and a plaintext password in
  `AndroidTest3\aab.txt`. It is not publicly findable, but keep it private and
  consider rotating that key too.
- Add a mute toggle persisted with the score stats.

---

## 10. Work log

### 9 November 2026

**2.1.1 hotfix: release-only startup crash**

- Reported symptom: the Play-installed build "opens and minimizes immediately";
  Android Studio debug builds worked. `dumpsys activity exit-info` on the Honor
  device showed `reason=4 (APP CRASH(EXCEPTION))` on every launch
  (`versionCode 3` and `5`).
- Dropbox stack: `androidx.startup.InitializationProvider` →
  `WorkManagerInitializer` → `WorkManagerImpl` → `WorkDatabase.create()` →
  `Failed to create an instance of androidx.work.impl.WorkDatabase`.
  The message is Room's `InstantiationException` from
  `Class.forName(...).newInstance()`.
- Cause: `play-services-ads:25.5.0` → `play-services-ads-api` →
  `androidx.work:work-runtime:2.7.0` → `androidx.room:room-runtime:2.2.5`.
  Room 2.2.5's consumer rule keeps the DB class name but not its no-arg
  constructor; R8 full mode sees no caller and removes it.
  `dexdump` of the uploaded `versionCode 5` bundle showed
  `androidx.work.impl.WorkDatabase_Impl` with **no direct methods**, and
  `usage.txt` listed `public void <init>()` as removed.
- Fix: `app/proguard-rules.pro` now keeps `<init>()` on every
  `RoomDatabase` subclass.
- Verification: the rebuilt release APK's DEX contains
  `WorkDatabase_Impl.<init>()V`; it was installed and cold-started on
  `emulator-5554` (API 35), stayed resumed, and produced no crash-buffer or
  `exit-info` entry.
- Added a `verifyReleaseBundle` guard that fails if R8 removes
  `WorkDatabase_Impl.<init>()`, and documented a mandatory signed-release
  smoke test in §6.
- Bumped to `versionCode 6` / `versionName 2.1.1`; `verifyReleaseBundle` passed
  (7,405,676 bytes, SHA-256
  `7528AB1331B26F81C5ED7FD14CE1D48E985A27B669EEA944EEFB1A51CEDD017F`).

**Green brand and Phase 1/2 release**

- Added `ui/PrimeLogo.kt`, a shared mark that draws the `1||0` foreground
  bitmap over `PrimeAccent` (`#00C2A8`). Home and the Classic Ready card now use
  it; the red `PrimeMark` constant was removed.
- Changed the adaptive-icon background to `#00C2A8` and recolored the legacy
  square/round mipmaps while keeping their glyphs, masks and transparency.
- Updated `icon-source.svg`/`icon.html`, regenerated `icon-512.png` and copied
  it to `docs/site/`; converted every red accent in the feature graphic to
  green and re-rendered `feature-graphic-1024x500.png`.
- Recolored only the logo circle in `screenshots/phone-01-ready.png` in place
  (centre 540,1019; diameter ≈238 px), preserving the black glyphs and card.
- Phase 1/2: resumable/abandonable Classic runs, Classic-only records and
  streak rules, onboarding/How to Play, isolated Practice mode, teaching tips
  and the four-section My Records screen. Unit tests: **55 passing**.
- Home and the Classic Ready card were checked on
  `emulator-5554` (API 35), and `connectedDebugAndroidTest` passed **11/11**.

### 7 November 2026

**Launcher icon replaced**

- Swapped the ring-and-"P" mark for the original 2020 Xamarin artwork (black
  `1||0` on red `#FF2F17`) at the owner's request.
- Rebuilt the adaptive foreground and monochrome layers from the 432 px source,
  scaled to 68 % so the digits survive circular/rounded masks.
- Regenerated legacy `mipmap-{mdpi…xxxhdpi}` icons (square + round) and
  `docs/store/icon-512.png`, and copied the 512 icon to `docs/site/`.
- Rewrote `docs/store/icon-source.svg` as a self-contained SVG (base64 glyph
  mask) and simplified `icon.html` to render it; both verified in headless
  Chrome.
- Verified on a Pixel 5 emulator (API 35): the circular launcher mask shows the
  whole `1||0` with clear margins.
- Rebuilt `docs/store/feature-graphic-1024x500.png` with the red mark and red
  accents (source: `feature-graphic.html`, rendered with headless Chrome).
- Replaced the teal "P" on the in-app start card with a red mark that draws the
  same adaptive foreground, cropped to the 72 dp circle.
- Bumped to `versionCode 4` / `versionName 2.0.1`; `verifyReleaseBundle` passed
  (6,984 KiB, SHA-256 `6626338C…`).
- Committed and pushed the site icon (`5541aaf`); the live
  `https://quickben22.github.io/icon-512.png` matches the repo file.

### 26 October 2026

- Owner confirmed the Play upload certificate matches
  `2B:41:8C:E6:…:20:84` and Play App Signing is enabled (§2).
- AAB re-checked on disk: unchanged, 7,083,334 bytes, SHA-256
  `95B5FC3C38FDCD8A9CCE95AC7E82F1EE7E5D5C01226437F662D8E8FC0C41968F`.
- Wrote the click-by-click internal-testing upload procedure and the tester
  opt-in steps into §8.
- Paused before the upload. Resume at §9 “Resume here”; nothing else is blocked
  locally.

### 25 October 2026

**Investigation**

- Confirmed `quickben22/PrimeOrNot` on GitHub is the 2020 Xamarin prototype,
  never published; its package matches the existing Play app.
- Read Play Console state: versionCode 2 / 1.1 in Closed testing – Alpha,
  versionCode 1 / 1.0 in Internal testing, production at 0%.
- Matched the Play upload certificate to the 2019 `quickben.keystore` found in
  the old BetsKeeper folder (same serial, same SHA-256), so no key reset.

**Signing**

- Copied the key to `~\.android-keys\prime-or-not-upload.p12` as PKCS#12 with a
  fresh random password; re-verified the certificate fingerprint after
  conversion.
- Wrote `release.*` values to `local.properties`; moved `upload_cert.der` out of
  the repo; added key/cert patterns to `.gitignore`.
- Added `validateReleaseSigning` and `verifyReleaseBundle` to
  `app/build.gradle.kts`.

**Code and branding**

- Renamed package, namespace, directories and visible strings (31 files).
- Bumped to versionCode 3 / 2.0.0 and centralised both values.
- Renamed the DataStore file and theme identifiers.
- Removed the old launcher drawable.

**Artwork**

- Designed the ring-and-P mark, wrote `docs/store/icon-source.svg`,
  `icon.html` and `feature-graphic.html`.
- Rendered `icon-512.png` and `feature-graphic-1024x500.png` with headless
  Chrome; inspected both visually and fixed the P stem overlapping the ring.
- Generated adaptive-icon vectors, monochrome layer and legacy PNG mipmaps.

**Public site**

- Wrote `docs/site/index.html` and `privacy.html` (AdMob/UMP data practices).
- Added `tools/write_app_ads_txt.ps1` and the `.example` template.
- Copied the icon to the site.

**Documentation**

- This file, `docs/store/listing.md`, and updates to `README.md` and
  `docs/ARCHITECTURE.md`.

**AdMob and final bundle**

- Configured the real app ID `ca-app-pub-3535032153893847~2009618845` (the
  AdMob app from the old BetsKeeper project, renamed to Prime Or Not, same
  publisher, confirmed by the owner) and banner unit
  `ca-app-pub-3535032153893847/2509721149` ("Prime Banner").
- Generated `docs/site/app-ads.txt` with
  `google.com, pub-3535032153893847, DIRECT, f08c47fec0942fa0`; committed it in
  the source repo and the site repo.
- `verifyReleaseBundle` with the real IDs passed. The uploadable AAB is at
  `app/build/outputs/bundle/release/app-release.aab` (6,917 KiB, SHA-256
  `95B5FC3C38FDCD8A9CCE95AC7E82F1EE7E5D5C01226437F662D8E8FC0C41968F`).

**GitHub publish**

- GitHub credentials were added to this machine (Windows Credential Manager,
  user `quickben22`), so `git push` works without a prompt.
- Pushed the source repo `quickben22/prime-or-not` (`main`, private) and the
  site repo `quickben22/quickben22.github.io` (`main`, public).
- GitHub Pages was already enabled on the site repo; the build for `12087ad`
  completed in 34 s. Verified `https://quickben22.github.io/`,
  `/privacy.html` and `/app-ads.txt` all return 200, and `app-ads.txt` serves
  `google.com, pub-3535032153893847, DIRECT, f08c47fec0942fa0`.
- Confirmed via the GitHub API that the source repo is private, the site repo
  is public, and both default to `main`.


**Build and verification**

- `testDebugUnitTest`: 35/35 passing; `lintDebug`: 0 errors, 2 warnings (AGP
  pin); `GameScreenTest` instrumented suite 10/10 on API 35; debug APK builds
  and runs.
- `bundleRelease` correctly refused to build without AdMob IDs.
- `verifyReleaseBundle -PALLOW_TEST_ADS=true` signed and verified an AAB with
  the 2020 upload key (package, version and SHA-256 all checked), then the
  test-ad artifact was deleted.
- Captured five 1080×1920 store screenshots and inspected each one.

---

## 11. File map

```text
docs/
├── PLAY_RELEASE.md              this file
├── ARCHITECTURE.md              design and module notes
├── site/                        published to quickben22.github.io
│   ├── index.html
│   ├── privacy.html
│   ├── app-ads.txt              generated from the real AdMob ID
│   ├── app-ads.txt.example
│   └── icon-512.png
└── store/
    ├── listing.md               Play copy and graphics checklist
    ├── icon-512.png
    ├── feature-graphic-1024x500.png
    ├── icon.html / icon-source.svg / feature-graphic.html
    └── screenshots/             phone-01…phone-05, 1080×1920

tools/
├── generate_sfx.ps1
└── write_app_ads_txt.ps1        AdMob ID -> app-ads.txt
```
