// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

plugins {
    alias(libs.plugins.pvot.android.library)
    alias(libs.plugins.pvot.hilt)
}

android {
    namespace = "org.prauga.assistant.platform.binder"
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    api(project(":memory-engine"))
    api(project(":context-engine"))
    api(project(":intent-engine"))
    implementation(project(":assistant-sdk"))
    implementation(libs.timber)
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
