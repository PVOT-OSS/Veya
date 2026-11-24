// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import androidx.annotation.StringRes
import org.prauga.assistant.capabilities.R
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.understanding.Lexicon

/** Opens an app ready to take a photo, record a video or record sound; it saves as usual. */
class CaptureCommand
private constructor(context: Context, router: AppRouter, lexicon: Lexicon, private val kind: Kind) :
    CapabilityCommand(context, router, lexicon, kind.id, kind.label) {
    enum class Kind(
        val id: String,
        val action: String,
        @StringRes val label: Int,
        @StringRes val done: Int,
    ) {
        PHOTO(
            "command.photo",
            MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA,
            R.string.command_photo,
            R.string.command_photo_done,
        ),
        VIDEO(
            "command.video",
            MediaStore.INTENT_ACTION_VIDEO_CAMERA,
            R.string.command_video,
            R.string.command_video_done,
        ),
        AUDIO(
            "command.audio",
            MediaStore.Audio.Media.RECORD_SOUND_ACTION,
            R.string.command_audio,
            R.string.command_audio_done,
        ),
    }

    override val unclear = kind.label

    /** Words after the command ("… of the sunset") don't change what opens. */
    override fun request(subject: String?, match: IntentMatch): Plan =
        Request(Intent(kind.action)) { app -> context.getString(kind.done, app) }

    companion object {
        fun all(context: Context, router: AppRouter, lexicon: Lexicon) =
            Kind.entries.map { CaptureCommand(context, router, lexicon, it) }
    }
}
