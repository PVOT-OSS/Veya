// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.binder

import android.os.RemoteException
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.prauga.assistant.context.ContextItem
import org.prauga.assistant.context.ContextProvider
import org.prauga.assistant.context.ContextProviderDescriptor
import org.prauga.assistant.context.ContextProviderException
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.context.ContextSource
import org.prauga.assistant.sdk.protocol.ContextQueryParcel
import org.prauga.assistant.sdk.protocol.IContextProvider

internal class RemoteContextProvider(
    override val descriptor: ContextProviderDescriptor,
    private val remote: IContextProvider,
    private val io: CoroutineDispatcher,
    private val clock: () -> Long,
) : ContextProvider {
    private val source = ContextSource(descriptor.packageName, descriptor.label)
    val lastUsed = AtomicLong(clock())

    override suspend fun query(query: ContextQuery): List<ContextItem> {
        lastUsed.set(clock())
        val request =
            ContextQueryParcel(
                query.text,
                query.types.toTypedArray(),
                query.since ?: ContextQueryParcel.NO_SINCE,
                query.limit,
            )
        val parcels =
            withContext(io) {
                try {
                    remote.query(request)
                } catch (e: RemoteException) {
                    throw ContextProviderException(e.message, e)
                }
            }
        val items = ArrayList<ContextItem>(parcels?.size ?: 0)
        parcels?.forEach { parcel -> parcel?.let { items += it.toContextItem(source) } }
        return items
    }

    companion object {
        fun idFor(packageName: String) = "live:$packageName"
    }
}
