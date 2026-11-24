// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities

import androidx.annotation.StringRes

/** Something to ask Veya that uses this capability, or null when Veya has no command for it yet. */
@get:StringRes
val Capability.example: Int?
    get() =
        when (this) {
            Capability.MAP -> R.string.capability_map_example
            Capability.BROWSE -> R.string.capability_browse_example
            Capability.WEB_SEARCH -> R.string.capability_web_search_example
            Capability.PLAY_MEDIA -> R.string.capability_play_media_example
            Capability.TAKE_PHOTO -> R.string.capability_take_photo_example
            Capability.RECORD_VIDEO -> R.string.capability_record_video_example
            Capability.RECORD_AUDIO -> R.string.capability_record_audio_example
            Capability.SET_ALARM -> R.string.capability_set_alarm_example
            Capability.SET_TIMER -> R.string.capability_set_timer_example
            Capability.SHOW_ALARMS -> R.string.capability_show_alarms_example
            Capability.ADD_EVENT -> R.string.capability_add_event_example
            Capability.DIAL -> R.string.capability_dial_example
            Capability.TEXT -> R.string.capability_text_example
            Capability.APP_SEARCH,
            Capability.VIEW_AUDIO,
            Capability.VIEW_VIDEO,
            Capability.VIEW_IMAGE,
            Capability.VIEW_PDF,
            Capability.VIEW_TEXT_DOCUMENT,
            Capability.VIEW_SPREADSHEET,
            Capability.VIEW_PRESENTATION,
            Capability.ADD_CONTACT,
            Capability.EMAIL,
            Capability.SHARE_TEXT -> null
        }
