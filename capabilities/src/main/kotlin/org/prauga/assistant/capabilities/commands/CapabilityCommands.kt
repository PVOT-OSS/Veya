// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.content.Context
import java.time.ZonedDateTime
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.understanding.Lexicon

object CapabilityCommands {
    fun create(
        context: Context,
        lexicon: Lexicon,
        now: () -> ZonedDateTime = ZonedDateTime::now,
        contacts: ContactDirectory = AndroidContacts(context),
    ): List<IntentHandler> {
        val router = AppRouter(context)
        return listOf(
            TimerCommand(context, router, lexicon),
            AlarmCommand(context, router, lexicon, now),
            ShowAlarmsCommand(context, router, lexicon),
            MapCommand(context, router, lexicon),
            WebSearchCommand(context, router, lexicon),
            PlayCommand(context, router, lexicon),
            LinkCommand(context, router, lexicon),
            EventCommand(context, router, lexicon, now),
            CallCommand(context, router, lexicon),
            TextCommand(context, router, lexicon),
        ) +
            CaptureCommand.all(context, router, lexicon) +
            ContactCommand.both(context, router, lexicon, contacts)
    }
}
