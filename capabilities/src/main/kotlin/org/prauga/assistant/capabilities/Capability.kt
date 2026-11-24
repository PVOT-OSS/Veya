// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities

import android.content.Intent
import android.net.Uri
import androidx.annotation.StringRes

enum class Capability(
    private val action: String,
    private val data: String?,
    private val mimeType: String?,
    private val category: String?,
    @StringRes val label: Int,
    @StringRes val verb: Int,
) {
    MAP(
        "android.intent.action.VIEW",
        "geo:0,0?q=place",
        null,
        null,
        R.string.capability_map,
        R.string.capability_map_verb,
    ),
    BROWSE(
        "android.intent.action.VIEW",
        "https://example.org",
        null,
        "android.intent.category.BROWSABLE",
        R.string.capability_browse,
        R.string.capability_browse_verb,
    ),
    WEB_SEARCH(
        "android.intent.action.WEB_SEARCH",
        null,
        null,
        null,
        R.string.capability_web_search,
        R.string.capability_web_search_verb,
    ),
    APP_SEARCH(
        "android.intent.action.SEARCH",
        null,
        null,
        null,
        R.string.capability_app_search,
        R.string.capability_app_search_verb,
    ),
    PLAY_MEDIA(
        "android.media.action.MEDIA_PLAY_FROM_SEARCH",
        null,
        null,
        null,
        R.string.capability_play_media,
        R.string.capability_play_media_verb,
    ),
    VIEW_AUDIO(
        "android.intent.action.VIEW",
        "content://media/audio",
        "audio/*",
        null,
        R.string.capability_view_audio,
        R.string.capability_view_audio_verb,
    ),
    VIEW_VIDEO(
        "android.intent.action.VIEW",
        "content://media/video",
        "video/*",
        null,
        R.string.capability_view_video,
        R.string.capability_view_video_verb,
    ),
    VIEW_IMAGE(
        "android.intent.action.VIEW",
        "content://media/image",
        "image/*",
        null,
        R.string.capability_view_image,
        R.string.capability_view_image_verb,
    ),
    VIEW_PDF(
        "android.intent.action.VIEW",
        "content://files/document",
        "application/pdf",
        null,
        R.string.capability_view_pdf,
        R.string.capability_view_pdf_verb,
    ),
    VIEW_TEXT_DOCUMENT(
        "android.intent.action.VIEW",
        "content://files/document",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        null,
        R.string.capability_view_text_document,
        R.string.capability_view_text_document_verb,
    ),
    VIEW_SPREADSHEET(
        "android.intent.action.VIEW",
        "content://files/document",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        null,
        R.string.capability_view_spreadsheet,
        R.string.capability_view_spreadsheet_verb,
    ),
    VIEW_PRESENTATION(
        "android.intent.action.VIEW",
        "content://files/document",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        null,
        R.string.capability_view_presentation,
        R.string.capability_view_presentation_verb,
    ),
    TAKE_PHOTO(
        "android.media.action.IMAGE_CAPTURE",
        null,
        null,
        null,
        R.string.capability_take_photo,
        R.string.capability_take_photo_verb,
    ),
    RECORD_VIDEO(
        "android.media.action.VIDEO_CAPTURE",
        null,
        null,
        null,
        R.string.capability_record_video,
        R.string.capability_record_video_verb,
    ),
    RECORD_AUDIO(
        "android.provider.MediaStore.RECORD_SOUND",
        null,
        null,
        null,
        R.string.capability_record_audio,
        R.string.capability_record_audio_verb,
    ),
    SET_ALARM(
        "android.intent.action.SET_ALARM",
        null,
        null,
        null,
        R.string.capability_set_alarm,
        R.string.capability_set_alarm_verb,
    ),
    SET_TIMER(
        "android.intent.action.SET_TIMER",
        null,
        null,
        null,
        R.string.capability_set_timer,
        R.string.capability_set_timer_verb,
    ),
    SHOW_ALARMS(
        "android.intent.action.SHOW_ALARMS",
        null,
        null,
        null,
        R.string.capability_show_alarms,
        R.string.capability_show_alarms_verb,
    ),
    ADD_EVENT(
        "android.intent.action.INSERT",
        "content://com.android.calendar/events",
        "vnd.android.cursor.dir/event",
        null,
        R.string.capability_add_event,
        R.string.capability_add_event_verb,
    ),
    ADD_CONTACT(
        "android.intent.action.INSERT",
        "content://com.android.contacts/contacts",
        "vnd.android.cursor.dir/contact",
        null,
        R.string.capability_add_contact,
        R.string.capability_add_contact_verb,
    ),
    DIAL(
        "android.intent.action.DIAL",
        "tel:",
        null,
        null,
        R.string.capability_dial,
        R.string.capability_dial_verb,
    ),
    TEXT(
        "android.intent.action.SENDTO",
        "smsto:",
        null,
        null,
        R.string.capability_text,
        R.string.capability_text_verb,
    ),
    EMAIL(
        "android.intent.action.SENDTO",
        "mailto:",
        null,
        null,
        R.string.capability_email,
        R.string.capability_email_verb,
    ),
    SHARE_TEXT(
        "android.intent.action.SEND",
        null,
        "text/plain",
        null,
        R.string.capability_share_text,
        R.string.capability_share_text_verb,
    );

    fun probe(): Intent {
        val intent = Intent(action)
        when {
            data != null && mimeType != null -> intent.setDataAndType(Uri.parse(data), mimeType)
            data != null -> intent.data = Uri.parse(data)
            mimeType != null -> intent.type = mimeType
        }
        category?.let(intent::addCategory)
        return intent
    }
}
