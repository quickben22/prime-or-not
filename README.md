# Prime or Not

A single-screen Android game built with Kotlin and Jetpack Compose. Number cards
are dealt face up; swipe **right for prime**, **left for not prime**. Each hit
extends the run by one point, the first miss or timeout ends it.

- Play app: `Prime or Not`, package `com.companyname.primeornot`, `versionCode 3`,
  `versionName 2.0.0`
- `minSdk 24`, `targetSdk`/`compileSdk 37`, AGP 9.1.1, Compose BOM 2026.09.00
- Game logic is pure Kotlin under `app/src/main/java/com/companyname/primeornot/game`
- Architecture and design notes: [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- Play release runbook and full change log: [`docs/PLAY_RELEASE.md`](docs/PLAY_RELEASE.md)
- Public site (privacy policy, `app-ads.txt`): `https://quickben22.github.io/`

## Build and run

The machine's default `java` may be older than 17, so point Gradle at Android
Studio's JBR:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat assembleDebug
```

Then install and launch:

```powershell
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb install -r -t .\app\build\outputs\apk\debug\app-debug.apk
& $adb shell am start -n com.companyname.primeornot/.MainActivity
```

Or open the project in Android Studio and press **Run**.

## AdMob setup

Monetisation is a single banner on the retry screen. `Ready` and `Playing` never
render an ad, and no ad is requested until UMP consent allows it.

Debug builds use Google's sample App ID and banner unit, so they work out of the
box. **Release builds refuse to build with those IDs.** To build a real release,
add the following to `local.properties` (gitignored):

```properties
admob.appId=ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY
admob.bannerUnitId=ca-app-pub-XXXXXXXXXXXXXXXX/ZZZZZZZZZZ
```

The same values can come from Gradle properties (`-PADMOB_APP_ID=…`,
`-PADMOB_BANNER_UNIT_ID=…`) or environment variables (`ADMOB_APP_ID`,
`ADMOB_BANNER_UNIT_ID`). `local.properties` wins when all are present.

`assembleRelease` runs `validateReleaseAdIds` and fails when an ID is missing or
still contains Google's sample publisher (`3940256099942544`). For a local
R8/signing smoke test before real IDs exist:

```powershell
.\gradlew.bat assembleRelease -PALLOW_TEST_ADS=true
```

That APK contains test ads and must never be distributed.

Consent, the privacy-options form, and the release guard are described in
[`docs/ARCHITECTURE.md#ads`](docs/ARCHITECTURE.md#ads).

## Release signing

The Play listing is signed with the 2020 upload key (SHA-256
`2B:41:8C:E6:FE:AE:83:09:8B:71:8A:0F:E2:87:02:86:5A:F5:BC:CB:3E:EA:00:41:FE:DA:95:60:F7:21:20:84`).
Gradle reads it from `local.properties`, which is gitignored:

```properties
release.storeFile=C:/Users/armin/.android-keys/prime-or-not-upload.p12
release.storePassword=…
release.keyAlias=quickben
release.keyPassword=…
```

`bundleRelease` depends on `validateReleaseSigning`, so it cannot produce an
unsigned bundle. Build and verify:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat bundleRelease
.\gradlew.bat verifyReleaseBundle
```

The keystore itself lives outside the repo in `C:\Users\armin\.android-keys\`.
Back up that folder and the password offline; the same values can also come from
`PLAY_UPLOAD_STORE_FILE`, `PLAY_UPLOAD_STORE_PASSWORD`, `PLAY_UPLOAD_KEY_ALIAS`
and `PLAY_UPLOAD_KEY_PASSWORD` Gradle properties or environment variables. See
[`docs/PLAY_RELEASE.md`](docs/PLAY_RELEASE.md) for the full Play Console runbook.

## Public site

`docs/site/` is published to `quickben22/quickben22.github.io` and serves:

- `https://quickben22.github.io/` — game page
- `https://quickben22.github.io/privacy.html` — privacy policy (used in Play)
- `https://quickben22.github.io/app-ads.txt` — AdMob authorised sellers

Regenerate `app-ads.txt` after the real AdMob app ID is configured:

```powershell
powershell -ExecutionPolicy Bypass -File tools/write_app_ads_txt.ps1
```

## Tests

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

Instrumented tests can be run from Studio, or manually so you can pick a device:

```powershell
$adb="$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
& $adb -s emulator-5556 install -r app\build\outputs\apk\debug\app-debug.apk
& $adb -s emulator-5556 install -r app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
& $adb -s emulator-5556 shell am instrument -w com.companyname.primeornot.test/androidx.test.runner.AndroidJUnitRunner
```

Avoid `connectedDebugAndroidTest` when more than one emulator is running — it
targets every connected device.

## Live Edit

Android Studio's Live Edit requires the target device or emulator to be **API 30
or higher**. It does not work on the API 24 AVD, regardless of the project's
`minSdk`; Studio 2025.3.3 fails that check with a confusing
`IllegalFormatConversionException: d != AndroidApiLevel` instead of the real
message.

Use the `Pixel_5` AVD (API 35) for Live Edit and Compose previews, and keep an
API 24 device only for `minSdk` compatibility checks. If a quick-boot snapshot
restores stale app data, cold-boot the AVD once.
