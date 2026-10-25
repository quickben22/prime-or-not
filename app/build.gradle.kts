import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Google's sample publisher and banner unit. Fine for debug builds and local
// smoke tests, never for something users can install.
val admobTestAppId = "ca-app-pub-3940256099942544~3347511713"
val admobTestBannerUnitId = "ca-app-pub-3940256099942544/9214589741"
val admobTestPublisherId = "3940256099942544"

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

fun configuredAdmobId(localKey: String, propertyKey: String): String =
    (localProperties.getProperty(localKey)
        ?: project.findProperty(propertyKey) as String?
        ?: System.getenv(propertyKey))
        ?.trim()
        .orEmpty()

val releaseAdmobAppId = configuredAdmobId("admob.appId", "ADMOB_APP_ID")
val releaseAdmobBannerUnitId = configuredAdmobId("admob.bannerUnitId", "ADMOB_BANNER_UNIT_ID")
val allowTestAdsInRelease =
    (project.findProperty("ALLOW_TEST_ADS") as String?)?.toBoolean() ?: false

android {
    namespace = "com.primetinder"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.primetinder"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

dependencies {
    implementation(platform(libs.compose.bom))
    androidTestImplementation(platform(libs.compose.bom))

    implementation(libs.androidx.activity.compose)
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
