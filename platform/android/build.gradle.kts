// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

plugins { alias(libs.plugins.pvot.android.library) }

android {
    namespace = "org.prauga.assistant.platform.android"
    testOptions { unitTests.isIncludeAndroidResources = true }
}

dependencies {
    api(project(":assistant-core"))
    implementation(libs.androidx.core.ktx)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
