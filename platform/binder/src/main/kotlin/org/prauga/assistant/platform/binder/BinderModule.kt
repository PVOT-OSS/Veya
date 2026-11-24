// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.content.Context
import android.content.pm.PackageManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.memory.DonatedContextStore

@Module
@InstallIn(SingletonComponent::class)
object BinderModule {
    @Provides
    @Singleton
    fun integrationGrants(@ApplicationContext context: Context) = IntegrationGrants(context)

    @Provides
    @Singleton
    fun donationGate(
        @ApplicationContext context: Context,
        grants: IntegrationGrants,
        store: DonatedContextStore,
        live: LiveProviders,
    ): DonationGate {
        val packages = context.packageManager
        return DonationGate(
            packagesForUid = packages::getPackagesForUid,
            labelFor = { name -> label(packages, name) },
            access = grants,
            limiter = RateLimiter(),
            sink = StoreDonationSink(store),
            invalidate = { packageName ->
                store.invalidate(packageName)
                live.invalidate(packageName)
            },
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
    }

    @Provides
    @Singleton
    fun liveProviders(
        @ApplicationContext context: Context,
        engine: ContextEngine,
        grants: IntegrationGrants,
    ): LiveProviders =
        LiveProviders(
            directory = PackageManagerDirectory(context.packageManager, context.packageName),
            binder = ContextProviderBinder(context),
            engine = engine,
            grants = grants,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            io = Dispatchers.IO,
        )

    @Provides fun contextWarmUp(live: LiveProviders): ContextWarmUp = live

    @Provides
    @Singleton
    fun packageChanges(@ApplicationContext context: Context) = PackageChanges(context)

    private fun label(packages: PackageManager, packageName: String): String =
        try {
            packages.getApplicationLabel(packages.getApplicationInfo(packageName, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
}
