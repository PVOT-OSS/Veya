// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.prauga.assistant.capabilities.R
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.understanding.Lexicon

/** Opens the dialer with a number ready; the user presses call. */
class CallCommand(context: Context, router: AppRouter, lexicon: Lexicon) :
    CapabilityCommand(
        context,
        router,
        lexicon,
        ID,
        R.string.command_call,
        question = R.string.command_call_question,
    ) {
    override val unclear = R.string.command_number_unclear

    override fun request(subject: String?, match: IntentMatch): Plan? {
        val number = subject?.let(PhoneNumbers::read) ?: return null
        return Request(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null))) { app ->
            context.getString(R.string.command_call_done, number, app)
        }
    }

    companion object {
        const val ID = "command.call"
    }
}

/** Opens a messaging app with the number and, if given, the message ready; the user sends. */
class TextCommand(context: Context, router: AppRouter, lexicon: Lexicon) :
    CapabilityCommand(context, router, lexicon, ID, R.string.command_text) {
    override val unclear = R.string.command_number_unclear

    override fun request(subject: String?, match: IntentMatch): Plan? {
        val number = match.entities[NUMBER]?.let(PhoneNumbers::read) ?: return null
        val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null))
        subject?.trim()?.takeIf { it.isNotEmpty() }?.let { intent.putExtra(SMS_BODY, it) }
        return Request(intent) { app -> context.getString(R.string.command_text_done, number, app) }
    }

    companion object {
        const val ID = "command.text"
        private const val NUMBER = "number"
        private const val SMS_BODY = "sms_body"
    }
}

internal object PhoneNumbers {
    private val SHAPE = Regex("\\+?[\\d ()-]+")
    private const val MIN_DIGITS = 3

    /** The number with only `+` and digits, or null when [text] isn't one. */
    fun read(text: String): String? {
        val trimmed = text.trim()
        if (!SHAPE.matches(trimmed)) return null
        val digits = trimmed.filter { it.isDigit() || it == '+' }
        return digits.takeIf { it.count(Char::isDigit) >= MIN_DIGITS }
    }
}
