// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.pvot.android.library)
    `maven-publish`
}

android {
    namespace = "org.prauga.assistant.sdk"
    defaultConfig { minSdk = 23 }
    buildFeatures { aidl = true }
    testOptions { unitTests.isIncludeAndroidResources = true }
    publishing { singleVariant("release") { withSourcesJar() } }
}

dependencies {
    implementation(libs.androidx.annotation)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "org.prauga.assistant"
            artifactId = "assistant-sdk"
            version = "0.2.0"
            afterEvaluate { from(components["release"]) }
        }
    }
}
