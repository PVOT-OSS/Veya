// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

plugins {
    alias(libs.plugins.pvot.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

android {
    namespace = "org.prauga.assistant.memory"
    testOptions { unitTests.isIncludeAndroidResources = true }
}

room { schemaDirectory("$projectDir/schemas") }

dependencies {
    api(project(":assistant-core"))
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
