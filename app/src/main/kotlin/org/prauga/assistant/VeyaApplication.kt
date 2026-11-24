// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.prauga.assistant.capabilities.CapabilityRepository
import org.prauga.assistant.intent.LanguageEngines
import org.prauga.assistant.memory.DonatedContextStore
import org.prauga.assistant.platform.binder.LiveProviders
import org.prauga.assistant.platform.binder.PackageChanges
import timber.log.Timber

@HiltAndroidApp
class VeyaApplication : Application() {
    @Inject lateinit var donatedContext: DonatedContextStore

    @Inject @ApplicationScope lateinit var scope: CoroutineScope

    @Inject lateinit var liveProviders: LiveProviders

    @Inject lateinit var packageChanges: PackageChanges

    @Inject lateinit var capabilities: CapabilityRepository

    @Inject lateinit var engines: LanguageEngines

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        scope.launch { donatedContext.start() }
        scope.launch { engines() }
        liveProviders.start()
        packageChanges.watch(liveProviders::refreshDirectory)
        capabilities.refresh()
        packageChanges.watch(capabilities::refresh)
    }
}
