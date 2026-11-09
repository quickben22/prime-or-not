import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties
import java.util.zip.ZipFile

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Google's sample publisher and banner unit. Fine for debug builds and local
// smoke tests, never for something users can install.
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestBannerUnitId = "ca-app-pub-3940256099942544/9214589741"
val admobTestPublisherId = "3940256099942544"

// Single source of truth for the Play upload. versionCode 3 shipped 2.0.0 and
// 4 shipped 2.0.1; 5 shipped the Phase 1/2 work with a release-only R8 startup
// crash; 6 fixed that crash but was rejected for store metadata. 7 fixes the
// metadata plus the Play edge-to-edge and outdated-SDK recommendations.
// It must increase by at least one for every upload.
val appVersionCode = 7
val appVersionName = "2.1.2"

/**
 * Real ad IDs are machine-specific, so they come from (in order) local.properties,
 * a Gradle property, or an environment variable. None of these are committed.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use { load(it) }
    }
}

fun configuredValue(localKey: String, propertyKey: String): String =
    (localProperties.getProperty(localKey)
        ?: project.findProperty(propertyKey) as String?
        ?: System.getenv(propertyKey))
        ?.trim()
        .orEmpty()

val releaseAdmobAppId = configuredValue("admob.appId", "ADMOB_APP_ID")
val releaseAdmobBannerUnitId = configuredValue("admob.bannerUnitId", "ADMOB_BANNER_UNIT_ID")
val allowTestAdsInRelease =
    (project.findProperty("ALLOW_TEST_ADS") as String?)?.toBoolean() ?: false

/**
 * The Play upload key is machine-specific too. These values are never committed;
 * local.properties wins over a Gradle property over an environment variable.
 */
val releaseStoreFilePath = configuredValue("release.storeFile", "PLAY_UPLOAD_STORE_FILE")
val releaseStorePassword = configuredValue("release.storePassword", "PLAY_UPLOAD_STORE_PASSWORD")
val releaseKeyAlias = configuredValue("release.keyAlias", "PLAY_UPLOAD_KEY_ALIAS")
val releaseKeyPassword = configuredValue("release.keyPassword", "PLAY_UPLOAD_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it.isNotBlank() }

android {
    namespace = "com.companyname.primeornot"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.companyname.primeornot"
        minSdk = 24
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Debug builds always use Google's sample IDs.
            manifestPlaceholders["admobAppId"] = admobTestAppId
            buildConfigField(
                "String",
                "ADMOB_BANNER_UNIT_ID",
                "\"$admobTestBannerUnitId\"",
            )
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            // validateReleaseAdIds runs before this variant is built and fails
            // unless both IDs are configured (or ALLOW_TEST_ADS=true is set).
            manifestPlaceholders["admobAppId"] =
                if (allowTestAdsInRelease) admobTestAppId else releaseAdmobAppId
            buildConfigField(
                "String",
                "ADMOB_BANNER_UNIT_ID",
                "\"${if (allowTestAdsInRelease) admobTestBannerUnitId else releaseAdmobBannerUnitId}\"",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

/**
 * A release APK with Google's sample IDs is unusable (and Play rejects it), so
 * make it impossible to produce one by accident. Debug builds are untouched.
 */
val validateReleaseAdIds = tasks.register("validateReleaseAdIds") {
    group = "verification"
    description = "Fails when a release build would ship Google's sample AdMob IDs."

    inputs.property("allowTestAds", allowTestAdsInRelease)
    inputs.property("admobAppId", releaseAdmobAppId)
    inputs.property("admobBannerUnitId", releaseAdmobBannerUnitId)

    doLast {
        if (allowTestAdsInRelease) {
            logger.warn(
                "ALLOW_TEST_ADS=true: the release build uses Google's sample AdMob IDs. " +
                    "Use it for local checks only, never for distribution.",
            )
            return@doLast
        }

        val problems = buildList {
            if (releaseAdmobAppId.isBlank()) {
                add("admob.appId is not set (local.properties / -PADMOB_APP_ID / ADMOB_APP_ID)")
            } else if (releaseAdmobAppId.contains(admobTestPublisherId)) {
                add("admob.appId still points at Google's sample publisher ($releaseAdmobAppId)")
            }
            if (releaseAdmobBannerUnitId.isBlank()) {
                add(
                    "admob.bannerUnitId is not set " +
                        "(local.properties / -PADMOB_BANNER_UNIT_ID / ADMOB_BANNER_UNIT_ID)",
                )
            } else if (releaseAdmobBannerUnitId.contains(admobTestPublisherId)) {
                add(
                    "admob.bannerUnitId still points at Google's sample publisher " +
                        "($releaseAdmobBannerUnitId)",
                )
            }
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Release AdMob IDs are not configured:\n" +
                    problems.joinToString("\n") { "  - $it" } +
                    "\nSet real IDs or pass -PALLOW_TEST_ADS=true for a local-only build.",
            )
        }
    }
}

tasks.matching {
    it.name == "preReleaseBuild" || it.name == "assembleRelease" || it.name == "bundleRelease"
}.configureEach {
    dependsOn(validateReleaseAdIds)
}

/**
 * Play only accepts a signed upload, and this app must keep the 2020 upload key
 * so the existing listing can be updated. assembleRelease stays usable unsigned
 * for local smoke tests; bundleRelease must never be.
 */
val validateReleaseSigning = tasks.register("validateReleaseSigning") {
    group = "verification"
    description = "Fails when bundleRelease has no usable Play upload signing config."

    inputs.property("storeFile", releaseStoreFilePath)
    inputs.property("keyAlias", releaseKeyAlias)

    doLast {
        val problems = buildList {
            if (releaseStoreFilePath.isBlank()) {
                add(
                    "release.storeFile is not set " +
                        "(local.properties / -PPLAY_UPLOAD_STORE_FILE / PLAY_UPLOAD_STORE_FILE)",
                )
            } else if (!file(releaseStoreFilePath).isFile) {
                add("release.storeFile does not exist: $releaseStoreFilePath")
            }
            if (releaseStorePassword.isBlank()) add("release.storePassword is not set")
            if (releaseKeyAlias.isBlank()) add("release.keyAlias is not set")
            if (releaseKeyPassword.isBlank()) add("release.keyPassword is not set")
        }

        if (problems.isNotEmpty()) {
            throw GradleException(
                "Release signing is not configured:\n" +
                    problems.joinToString("\n") { "  - $it" } +
                    "\nSee docs/PLAY_RELEASE.md for the upload-key setup.",
            )
        }
    }
}

tasks.matching { it.name == "bundleRelease" }.configureEach {
    dependsOn(validateReleaseSigning)
}

/**
 * Fails the release unless the Play upload key signed the bundle. This catches
 * the worst possible mistake: uploading version 3 signed by the debug key or by
 * a freshly generated key, which Play rejects and which cannot be undone after
 * the first upload.
 */
val verifyReleaseBundle = tasks.register("verifyReleaseBundle") {
    group = "verification"
    description = "Checks the release bundle's package and Play upload signer."

    dependsOn("bundleRelease")

    val bundle = layout.buildDirectory.file("outputs/bundle/release/app-release.aab")
    val expectedSha256 =
        "2B:41:8C:E6:FE:AE:83:09:8B:71:8A:0F:E2:87:02:86:5A:F5:BC:CB:3E:EA:00:41:FE:DA:95:60:F7:21:20:84"
    val expectedAdmobAppId = if (allowTestAdsInRelease) admobTestAppId else releaseAdmobAppId

    doLast {
        val aab = bundle.get().asFile
        if (!aab.isFile) throw GradleException("Missing release bundle: $aab")

        ZipFile(aab).use { zip ->
            val entry = zip.getEntry("base/manifest/AndroidManifest.xml")
                ?: throw GradleException("Bundle has no base manifest: $aab")
            val manifest = zip.getInputStream(entry).readBytes().toString(Charsets.ISO_8859_1)
            listOf("com.companyname.primeornot", appVersionName, expectedAdmobAppId).forEach { expected ->
                if (!manifest.contains(expected)) {
                    throw GradleException("Bundle manifest does not contain '$expected'.")
                }
            }
            if (!allowTestAdsInRelease && manifest.contains(admobTestPublisherId)) {
                throw GradleException("Bundle manifest still contains Google's sample AdMob publisher.")
            }
        }

        val keytool = File(System.getProperty("java.home"), "bin/keytool")
            .let { if (it.exists()) it else File(it.path + ".exe") }
        val process = ProcessBuilder(
            keytool.absolutePath,
            "-printcert",
            "-jarfile",
            aab.absolutePath,
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        if (process.waitFor() != 0) {
            throw GradleException("Bundle is not signed:\n$output")
        }
        if (!output.contains(expectedSha256)) {
            throw GradleException(
                "Bundle is signed by the wrong key.\nExpected: $expectedSha256\nkeytool:\n$output",
            )
        }

        // Regression guard: R8 strips Room's reflectively-used no-arg constructor
        // unless proguard-rules.pro keeps it, which crashes WorkManager at startup.
        val usage = layout.buildDirectory.file("outputs/mapping/release/usage.txt").get().asFile
        if (usage.isFile) {
            var inWorkDatabase = false
            var removedCtor = false
            usage.forEachLine { line ->
                if (line.isNotBlank() && !line.startsWith("    ")) {
                    inWorkDatabase = line.trim() == "androidx.work.impl.WorkDatabase_Impl:"
                } else if (inWorkDatabase && line.trim() == "public void <init>()") {
                    removedCtor = true
                }
            }
            if (removedCtor) {
                throw GradleException(
                    "R8 removed WorkDatabase_Impl.<init>(); WorkManager will crash at " +
                        "startup. Check the Room keep rule in app/proguard-rules.pro.",
                )
            }
        } else {
            logger.warn("verifyReleaseBundle: no $usage; skipped the WorkDatabase guard.")
        }

        logger.lifecycle("verifyReleaseBundle: ${aab.name} OK")
        logger.lifecycle("  package    com.companyname.primeornot")
        logger.lifecycle("  version    $appVersionName (code $appVersionCode)")
        logger.lifecycle("  signer     $expectedSha256")
        logger.lifecycle("  size       ${aab.length() / 1024} KiB")
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    // play-services-basement 18.9.0 still requests fragment 1.1.0 transitively.
    // Keep the current AndroidX release so Play does not flag an outdated SDK.
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)

    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.compose.ui.test.junit4)
}
