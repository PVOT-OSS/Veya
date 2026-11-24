// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

plugins { alias(libs.plugins.pvot.android.library) }

android {
    namespace = "org.prauga.assistant.benchmarks"
    testBuildType = "release"
}

dependencies {
    androidTestImplementation(project(":intent-engine"))
    androidTestImplementation(project(":context-engine"))
    androidTestImplementation(project(":platform:android"))
    androidTestImplementation(project(":capabilities"))
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.core)
}
