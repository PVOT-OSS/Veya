// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.platform.android.handlers

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import org.prauga.assistant.intent.IntentDescriptor
import org.prauga.assistant.intent.IntentHandler
import org.prauga.assistant.intent.IntentMatch
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PatternMatcher
import org.prauga.assistant.intent.PatternMatcher.Companion.rule
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.R

class TorchHandler(private val context: Context, private val on: Boolean) : IntentHandler {
    private val id = if (on) ID_ON else ID_OFF

    override val descriptor =
        IntentDescriptor(
            id,
            context.getString(if (on) R.string.intent_torch_on else R.string.intent_torch_off),
        )

    private val matcher: PatternMatcher =
        PatternMatcher(
            id,
            listOf(
                rule("(turn|switch) $state (the )?$LIGHT", 0.95f),
                rule("(turn|switch) (the )?$LIGHT $state", 0.95f),
                rule("$LIGHT $state", 0.9f),
                rule(
                    if (on) "(enable|start) (the )?$LIGHT" else "(disable|stop) (the )?$LIGHT",
                    0.9f,
                ),
            ),
        )

    private val state: String
        get() = if (on) "on" else "off"

    override fun match(utterance: Utterance): IntentMatch? = matcher.match(utterance)

    override suspend fun execute(match: IntentMatch): IntentOutcome {
        val cameras = context.getSystemService(CameraManager::class.java)
        return try {
            val cameraId =
                cameras?.cameraIdList?.firstOrNull { id ->
                    cameras
                        .getCameraCharacteristics(id)
                        .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return IntentOutcome.Failed(context.getString(R.string.torch_unavailable))
            cameras.setTorchMode(cameraId, on)
            IntentOutcome.Reply(
                context.getString(if (on) R.string.torch_on else R.string.torch_off)
            )
        } catch (e: CameraAccessException) {
            IntentOutcome.Failed(context.getString(R.string.torch_busy))
        }
    }

    companion object {
        const val ID_ON = "device.torch.on"
        const val ID_OFF = "device.torch.off"
        private const val LIGHT = "(flashlight|flash light|torch)"
    }
}
