// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.app.Application
import android.app.SearchManager
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.assistant.intent.IntentEngine
import org.prauga.assistant.intent.IntentOutcome
import org.prauga.assistant.intent.PlatformGate
import org.prauga.assistant.intent.Resolution
import org.prauga.assistant.intent.Utterance
import org.prauga.assistant.platform.android.DeviceHandlers
import org.prauga.assistant.understanding.Lexicon
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class CapabilityCommandsTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val packages = shadowOf(context.packageManager)
    private val lexicon = Lexicon.english()
    private val now = ZonedDateTime.of(2026, 11, 24, 10, 15, 0, 0, ZoneOffset.UTC)
    private val gate =
        object : PlatformGate {
            override val sdkInt = 35

            override fun isGranted(permission: String) = true
        }
    private val engine =
        IntentEngine(
            DeviceHandlers.create(context, lexicon) +
                CapabilityCommands.create(context, lexicon) { now },
            gate,
            Utterance.Fillers.of(lexicon),
        )

    private fun app(packageName: String, label: String, vararg filters: IntentFilter) {
        val info =
            ApplicationInfo().apply {
                this.packageName = packageName
                nonLocalizedLabel = label
            }
        val activity =
            ActivityInfo().apply {
                this.packageName = packageName
                name = "$packageName.Main"
                applicationInfo = info
                exported = true
            }
        packages.installPackage(
            PackageInfo().apply {
                this.packageName = packageName
                applicationInfo = info
            }
        )
        val component = ComponentName(packageName, activity.name)
        packages.addOrUpdateActivity(activity)
        filters.forEach { packages.addIntentFilterForActivity(component, it) }
    }

    private fun filter(action: String, scheme: String? = null, type: String? = null) =
        IntentFilter(action).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            if (action == Intent.ACTION_VIEW && scheme == "https")
                addCategory(Intent.CATEGORY_BROWSABLE)
            scheme?.let(::addDataScheme)
            type?.let(::addDataType)
        }

    private fun intentOf(text: String): String? =
        (engine.resolve(text) as? Resolution.Matched)?.candidate?.match?.intentId

    private fun run(text: String): IntentOutcome {
        val candidate = (engine.resolve(text) as Resolution.Matched).candidate
        return runBlocking { candidate.handler.execute(candidate.match) }
    }

    private fun started(): Intent = shadowOf(context).nextStartedActivity

    @Test
    fun understandsEachCommand() {
        listOf(
                "set a timer for 10 minutes" to TimerCommand.ID,
                "10 minute timer" to TimerCommand.ID,
                "set an alarm for 7am" to AlarmCommand.ID,
                "wake me up at 6:30" to AlarmCommand.ID,
                "show my alarms" to ShowAlarmsCommand.ID,
                "directions to the eiffel tower" to MapCommand.ID,
                "find coffee near me" to MapCommand.ID,
                "search the web for kotlin flows" to WebSearchCommand.ID,
                "play lo-fi beats" to PlayCommand.ID,
                "open example.org" to LinkCommand.ID,
                "take a photo" to "command.photo",
                "record a video" to "command.video",
                "start recording" to "command.audio",
                "add dinner with sam tomorrow at 8" to EventCommand.ID,
                "call +1 555 123 4567" to CallCommand.ID,
                "text 5551234 running late" to TextCommand.ID,
            )
            .forEach { (text, id) -> assertEquals(text, id, intentOf(text)) }
    }

    @Test
    fun leavesOtherRequestsToTheirHandlers() {
        assertEquals("app.open", intentOf("open camera"))
        assertEquals("app.open", intentOf("start maps"))
        assertEquals("settings.open", intentOf("open wifi settings"))
        assertNull(intentOf("add a contact"))
        assertNull(intentOf("call mom"))
    }

    @Test
    fun startsATimerWithoutShowingTheClock() {
        app("org.lineageos.deskclock", "Clock", filter(AlarmClock.ACTION_SET_TIMER))
        val outcome = run("set a timer for an hour and a half")
        val intent = started()
        assertEquals(AlarmClock.ACTION_SET_TIMER, intent.action)
        assertEquals(5_400, intent.getIntExtra(AlarmClock.EXTRA_LENGTH, 0))
        assertTrue(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, false))
        assertEquals("org.lineageos.deskclock", intent.`package`)
        assertTrue(outcome.toString(), (outcome as IntentOutcome.Reply).text.contains("Clock"))
    }

    @Test
    fun setsAlarmsOnlyWithinADay() {
        app("org.lineageos.deskclock", "Clock", filter(AlarmClock.ACTION_SET_ALARM))
        run("wake me up at 6:30 am")
        val intent = started()
        assertEquals(6, intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(30, intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
        assertTrue(run("set an alarm for next friday at 8am") is IntentOutcome.Failed)
    }

    @Test
    fun aNamedAppGetsTheWorkAndKeepsTheUsersCapitals() {
        app("app.organicmaps", "Organic Maps", filter(Intent.ACTION_VIEW, scheme = "geo"))
        app(
            "com.google.android.apps.maps",
            "Google Maps",
            filter(Intent.ACTION_VIEW, scheme = "geo"),
        )
        val outcome = run("Directions to the Eiffel Tower in Organic Maps")
        val intent = started()
        assertEquals("app.organicmaps", intent.`package`)
        assertEquals("geo:0,0?q=the%20Eiffel%20Tower", intent.dataString)
        assertEquals(
            "Showing the Eiffel Tower in Organic Maps.",
            (outcome as IntentOutcome.Reply).text,
        )
    }

    @Test
    fun wordsThatAreNotAnAppStayInTheSubject() {
        app(
            "com.android.calendar",
            "Samsung Calendar",
            filter(Intent.ACTION_INSERT, type = "vnd.android.cursor.dir/event"),
        )
        run("add Dinner with Sam tomorrow at 8pm")
        val intent = started()
        assertEquals("Dinner with Sam", intent.getStringExtra(CalendarContract.Events.TITLE))
        val begin = ZonedDateTime.of(2026, 11, 25, 20, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(
            begin.toInstant().toEpochMilli(),
            intent.getLongExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, 0),
        )
    }

    @Test
    fun webSearchSaysItGoesOnline() {
        app("org.mozilla.firefox", "Firefox", filter(Intent.ACTION_WEB_SEARCH))
        val outcome = run("search the web for kotlin flows") as IntentOutcome.Reply
        assertEquals("kotlin flows", started().getStringExtra(SearchManager.QUERY))
        assertTrue(outcome.text, outcome.text.contains("online through Firefox"))
    }

    @Test
    fun linksKeepTheirCase() {
        app("org.mozilla.firefox", "Firefox", filter(Intent.ACTION_VIEW, scheme = "https"))
        run("open Example.org/Docs/Intro")
        assertEquals("https://Example.org/Docs/Intro", started().dataString)
    }

    @Test
    fun numbersAreDialledAndTextedButNeverSent() {
        app("com.android.dialer", "Phone", filter(Intent.ACTION_DIAL, scheme = "tel"))
        app("com.android.messaging", "Messaging", filter(Intent.ACTION_SENDTO, scheme = "smsto"))
        run("call +1 (555) 123-4567")
        assertEquals("+15551234567", started().data?.schemeSpecificPart)
        run("text 555 1234 running late")
        val text = started()
        assertEquals(Intent.ACTION_SENDTO, text.action)
        assertEquals("smsto:5551234", text.dataString)
        assertEquals("running late", text.getStringExtra("sms_body"))
    }

    @Test
    fun capturesOpenTheCameraApp() {
        app("org.lineageos.aperture", "Camera", filter(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        val outcome = run("take a photo of the sunset") as IntentOutcome.Reply
        assertEquals(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA, started().action)
        assertEquals("Opening Camera to take a photo.", outcome.text)
    }

    @Test
    fun saysSoWhenNoAppCanDoIt() {
        val outcome = run("record a video")
        assertTrue(outcome is IntentOutcome.Failed)
        assertNull(shadowOf(context).nextStartedActivity)
    }

    @Test
    fun aMissingDurationOrTimeIsAskedFor() {
        assertTrue(engine.resolve("set a timer") is Resolution.MissingParameter)
        assertTrue(engine.resolve("set an alarm") is Resolution.MissingParameter)
    }
}
