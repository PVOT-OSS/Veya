// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.prauga.assistant.capabilities.R
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.understanding.Lexicon

/**
 * "Call Sam" and "text Mom running late": finds the person in the phone's contacts and opens the
 * dialer or a messaging app ready to go. Nothing is placed or sent without the user.
 */
class ContactCommand
private constructor(
    private val context: Context,
    private val router: AppRouter,
    lexicon: Lexicon,
    private val directory: ContactDirectory,
    private val text: Boolean,
) : IntentHandler {
    private val id = if (text) TEXT_ID else CALL_ID

    override val descriptor =
        IntentDescriptor(
            id,
            context.getString(
                if (text) R.string.command_text_contact else R.string.command_call_contact
            ),
            requiredPermissions = setOf(Manifest.permission.READ_CONTACTS),
        )

    private val matcher = PatternMatcher.from(id, lexicon.phrases(id))
    private val bodyLeads = lexicon.optional("text.body.lead")

    override fun match(utterance: Utterance): IntentMatch? {
        val found = matcher.match(utterance) ?: return null
        val subject = found.entities[SUBJECT] ?: return null
        if (subject.first().isDigit() || subject.first() == '+') return null
        val start = utterance.raw.indexOf(subject, ignoreCase = true)
        val original =
            if (start < 0) subject else utterance.raw.substring(start, start + subject.length)
        return found.copy(entities = mapOf(SUBJECT to original))
    }

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val subject = match.entities[SUBJECT].orEmpty()
        val person =
            when (val found = ContactMatch.find(subject, directory)) {
                is ContactMatch.Found.One -> found
                is ContactMatch.Found.Several ->
                    return IntentOutcome.Failed(
                        context.getString(
                            R.string.contact_several,
                            subject,
                            found.names.joinToString(", "),
                        )
                    )
                ContactMatch.Found.None ->
                    return IntentOutcome.Failed(
                        context.getString(R.string.contact_not_found, subject)
                    )
            }
        if (!text && person.rest.isNotEmpty())
            return IntentOutcome.Failed(context.getString(R.string.contact_not_found, subject))
        val intent =
            if (text) {
                Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", person.number, null)).also {
                    body(person.rest)?.let { message -> it.putExtra(SMS_BODY, message) }
                }
            } else {
                Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", person.number, null))
            }
        val apps = router.appsFor(intent)
        return when (val opened = router.open(intent, apps, null)) {
            is AppRouter.Opened.App ->
                IntentOutcome.Reply(
                    context.getString(
                        if (text) R.string.command_text_contact_done
                        else R.string.command_call_contact_done,
                        person.name,
                        person.number,
                        opened.label,
                    )
                )
            AppRouter.Opened.Chooser ->
                IntentOutcome.Reply(context.getString(R.string.command_choose))
            AppRouter.Opened.Nothing ->
                IntentOutcome.Failed(
                    context.getString(R.string.command_no_app, descriptor.displayName)
                )
        }
    }

    /** The message after the name, without a leading "saying" or "that". */
    private fun body(rest: String): String? {
        val words = rest.split(' ').filter { it.isNotEmpty() }
        if (words.isEmpty()) return null
        val lead = bodyLeads.at(words.map { it.lowercase() }, 0)?.consumed ?: 0
        return words.drop(lead).joinToString(" ").trimStart(':', ' ').ifEmpty { null }
    }

    companion object {
        const val CALL_ID = "command.call.contact"
        const val TEXT_ID = "command.text.contact"
        private const val SUBJECT = "subject"
        private const val SMS_BODY = "sms_body"

        fun both(
            context: Context,
            router: AppRouter,
            lexicon: Lexicon,
            directory: ContactDirectory,
        ) =
            listOf(
                ContactCommand(context, router, lexicon, directory, text = false),
                ContactCommand(context, router, lexicon, directory, text = true),
            )
    }
}
