// Top-level build file. AGP 9 has built-in Kotlin support, so the
// `org.jetbrains.kotlin.android` plugin must not be applied. The Kotlin Gradle
// plugin is only placed on the classpath to upgrade AGP's bundled Kotlin.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
