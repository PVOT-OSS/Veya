// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.benchmarks

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.context.ContextEngine
import org.prauga.assistant.context.ContextGrant
import org.prauga.assistant.context.ContextItem
import org.prauga.assistant.context.ContextProvider
import org.prauga.assistant.context.ContextProviderDescriptor
import org.prauga.assistant.context.ContextQuery
import org.prauga.assistant.context.ContextSource
import org.prauga.assistant.context.Sensitivity

@RunWith(AndroidJUnit4::class)
class ContextQueryBenchmark {
    private class InMemoryProvider(id: String, private val items: List<ContextItem>) :
        ContextProvider {
        override val descriptor =
            ContextProviderDescriptor(
                id,
                "com.example.$id",
                id,
                setOf("note"),
                Sensitivity.SENSITIVE,
            )

        override suspend fun query(query: ContextQuery): List<ContextItem> = items
    }

    private fun engine(): ContextEngine {
        val engine = ContextEngine({ ContextGrant(true, Sensitivity.PERSONAL) })
        repeat(PROVIDERS) { p ->
            val id = "app$p"
            engine.register(
                InMemoryProvider(
                    id,
                    List(ITEMS_PER_PROVIDER) { i ->
                        ContextItem(
                            "$id-$i",
                            "note",
                            "Note $i about groceries and trips",
                            "Buy milk, eggs and bread before the weekend trip number $i",
                            ContextSource("com.example.$id", id),
                            timestamp = i.toLong(),
                        )
                    },
                )
            )
        }
        return engine
    }

    @Test
    fun cachedQueryAcrossProviders() {
        val engine = engine()
        val query = ContextQuery(text = "milk trip", limit = 10)
        Latency.measure("context.cached5x200") { runBlocking { engine.query(query) } }
    }

    @Test
    fun uncachedQueryAcrossProviders() {
        val engine = engine()
        val query = ContextQuery(text = "milk trip", limit = 10)
        Latency.measure("context.uncached5x200") {
            repeat(PROVIDERS) { engine.invalidate("app$it") }
            runBlocking { engine.query(query) }
        }
    }

    private companion object {
        const val PROVIDERS = 5
        const val ITEMS_PER_PROVIDER = 200
    }
}
