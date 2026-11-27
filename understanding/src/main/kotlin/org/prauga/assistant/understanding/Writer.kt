// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.understanding

/**
 * An optional on-device language model that can write text. Veya works without one: features that
 * use it (summarizing, composing) fall back to their own rules when [ready] is false or [write]
 * returns null. Nothing here may leave the phone.
 */
interface Writer {
    /** True when a model is loaded and [write] can produce text. */
    val ready: Boolean

    /**
     * Text following [task] (what to do, e.g. "Summarize in two sentences") for [input], in
     * [language] (a BCP 47 tag), at most about [maxWords] words; null when nothing could be
     * written.
     */
    suspend fun write(task: Task, input: String, language: String, maxWords: Int): String?

    enum class Task {
        SUMMARIZE,
        DRAFT_MESSAGE,
        DRAFT_EMAIL,
        REPLY,
    }

    /** The writer of a phone with no model: never ready. */
    object None : Writer {
        override val ready: Boolean = false

        override suspend fun write(task: Task, input: String, language: String, maxWords: Int) =
            null
    }
}
