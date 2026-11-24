// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

plugins { alias(libs.plugins.pvot.android.library) }

android { namespace = "org.prauga.assistant.inference.litert" }

dependencies {
    api(project(":inference:api"))
    implementation(libs.litertlm.android)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
