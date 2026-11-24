// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import org.prauga.assistant.capabilities.Capability
import org.prauga.assistant.capabilities.R
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.understanding.Lexicon

class MapCommand(context: Context, router: AppRouter, lexicon: Lexicon) :
    CapabilityCommand(
        context,
        router,
        lexicon,
        ID,
        R.string.command_map,
        question = R.string.command_map_question,
        capability = Capability.MAP,
    ) {
    override val unclear = R.string.command_map_unclear

    override fun request(subject: String?, match: IntentMatch): Plan? {
        val place = subject?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(place)))
        return Request(intent) { app -> context.getString(R.string.command_map_done, place, app) }
    }

    companion object {
        const val ID = "command.map"
    }
}

class WebSearchCommand(context: Context, router: AppRouter, lexicon: Lexicon) :
    CapabilityCommand(
        context,
        router,
        lexicon,
        ID,
        R.string.command_web,
        question = R.string.command_web_question,
        capability = Capability.WEB_SEARCH,
    ) {
    override val unclear = R.string.command_web_unclear

    override fun request(subject: String?, match: IntentMatch): Plan? {
        val query = subject?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val intent = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        return Request(intent) { app -> context.getString(R.string.command_web_done, query, app) }
    }

    companion object {
        const val ID = "command.web"
    }
}

class PlayCommand(context: Context, router: AppRouter, lexicon: Lexicon) :
    CapabilityCommand(
        context,
        router,
        lexicon,
        ID,
        R.string.command_play,
        question = R.string.command_play_question,
        capability = Capability.PLAY_MEDIA,
    ) {
    override val unclear = R.string.command_play_unclear

    override fun request(subject: String?, match: IntentMatch): Plan? {
        val query = subject?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val intent =
            Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH)
                .putExtra(SearchManager.QUERY, query)
                .putExtra(MediaStore.EXTRA_MEDIA_FOCUS, ANY_MEDIA)
        return Request(intent) { app -> context.getString(R.string.command_play_done, query, app) }
    }

    companion object {
        const val ID = "command.play"
        private const val ANY_MEDIA = "vnd.android.cursor.item/*"
    }
}

class LinkCommand(context: Context, router: AppRouter, lexicon: Lexicon) :
    CapabilityCommand(
        context,
        router,
        lexicon,
        ID,
        R.string.command_link,
        question = R.string.command_link_question,
        capability = Capability.BROWSE,
    ) {
    override val unclear = R.string.command_link_unclear

    override fun request(subject: String?, match: IntentMatch): Plan? {
        val address = subject?.trim()?.takeIf { ADDRESS.matches(it) } ?: return null
        val uri = Uri.parse(if (SCHEME.containsMatchIn(address)) address else "https://$address")
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        return Request(intent) { app ->
            context.getString(R.string.command_link_done, uri.host ?: address, app)
        }
    }

    companion object {
        const val ID = "command.link"
        private val ADDRESS =
            Regex(
                "(?:https?://)?(?:[\\p{L}\\p{N}-]+\\.)+\\p{L}{2,}(?::\\d+)?(?:[/?#]\\S*)?",
                RegexOption.IGNORE_CASE,
            )
        private val SCHEME = Regex("^https?://", RegexOption.IGNORE_CASE)
    }
}
