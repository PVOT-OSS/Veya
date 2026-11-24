// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: AGPL-3.0-or-later

package org.prauga.assistant.capabilities.commands

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract.CommonDataKinds.Phone

/** One phone number of one contact. */
data class ContactPhone(
    val contactId: Long,
    val name: String,
    val number: String,
    val primary: Boolean,
    val mobile: Boolean,
)

/** Reads the phone's own contacts; needs READ_CONTACTS. */
fun interface ContactDirectory {
    /** Numbers of contacts whose name has a word starting with [word]. */
    fun phones(word: String): List<ContactPhone>
}

class AndroidContacts(private val context: Context) : ContactDirectory {
    override fun phones(word: String): List<ContactPhone> {
        val uri = Uri.withAppendedPath(Phone.CONTENT_FILTER_URI, Uri.encode(word))
        val found = ArrayList<ContactPhone>()
        context.contentResolver.query(uri, PROJECTION, null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                found +=
                    ContactPhone(
                        cursor.getLong(0),
                        cursor.getString(1).orEmpty(),
                        cursor.getString(2).orEmpty(),
                        cursor.getInt(3) != 0,
                        cursor.getInt(4) == Phone.TYPE_MOBILE,
                    )
            }
        }
        return found.filter { it.number.isNotBlank() }
    }

    private companion object {
        val PROJECTION =
            arrayOf(
                Phone.CONTACT_ID,
                Phone.DISPLAY_NAME_PRIMARY,
                Phone.NUMBER,
                Phone.IS_SUPER_PRIMARY,
                Phone.TYPE,
            )
    }
}

/** Finds who the user means by a name, and which of their numbers to use. */
internal object ContactMatch {
    sealed interface Found {
        data class One(val name: String, val number: String, val rest: String) : Found

        data class Several(val names: List<String>) : Found

        data object None : Found
    }

    /**
     * The contact named at the start of [text], and the words after the name. The longest exact
     * name wins; a single contact whose name starts with the first word is taken as that person.
     */
    fun find(text: String, directory: ContactDirectory): Found {
        val words = text.trim().split(SPACES).filter { it.isNotEmpty() }
        if (words.isEmpty()) return Found.None
        val people = directory.phones(words[0]).groupBy { it.contactId }.values
        for (count in words.size downTo 1) {
            val wanted = words.take(count).joinToString(" ")
            val exact = people.filter { it[0].name.equals(wanted, ignoreCase = true) }
            if (exact.size == 1) return one(exact[0], words.drop(count))
            if (exact.size > 1) return Found.Several(exact.map { it[0].name }.distinct())
        }
        val byFirstName =
            people.filter { it[0].name.split(SPACES)[0].equals(words[0], ignoreCase = true) }
        return when (byFirstName.size) {
            0 -> Found.None
            1 -> one(byFirstName[0], words.drop(1))
            else -> Found.Several(byFirstName.map { it[0].name }.distinct().take(MAX_NAMES))
        }
    }

    private fun one(numbers: List<ContactPhone>, rest: List<String>): Found {
        val best =
            numbers.firstOrNull { it.primary } ?: numbers.firstOrNull { it.mobile } ?: numbers[0]
        return Found.One(best.name, best.number, rest.joinToString(" "))
    }

    private val SPACES = Regex("\\s+")
    private const val MAX_NAMES = 3
}
