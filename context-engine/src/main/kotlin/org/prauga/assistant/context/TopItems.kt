// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.context

internal class TopItems(private val capacity: Int) {
    private val scores = IntArray(capacity)
    private val items = arrayOfNulls<ContextItem>(capacity)
    private var size = 0

    fun offer(score: Int, item: ContextItem) {
        if (size == capacity && !ranksBefore(score, item, size - 1)) return
        var position = if (size < capacity) size++ else size - 1
        while (position > 0 && ranksBefore(score, item, position - 1)) {
            scores[position] = scores[position - 1]
            items[position] = items[position - 1]
            position--
        }
        scores[position] = score
        items[position] = item
    }

    inline fun drain(transform: (ContextItem) -> ContextItem): List<ContextItem> {
        val result = ArrayList<ContextItem>(size)
        for (i in 0 until size) result += transform(checkNotNull(items[i]))
        return result
    }

    private fun ranksBefore(score: Int, item: ContextItem, index: Int): Boolean {
        val other = scores[index]
        if (score != other) return score > other
        return item.timestamp > checkNotNull(items[index]).timestamp
    }
}
