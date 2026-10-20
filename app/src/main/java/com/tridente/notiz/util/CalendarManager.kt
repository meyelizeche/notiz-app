package com.tridente.notiz.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract

object CalendarManager {

    fun addNoteToNativeCalendar(
        context: Context,
        title: String,
        description: String,
        dueDateTimestamp: Long
    ) {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Notiz: $title")
            putExtra(CalendarContract.Events.DESCRIPTION, description)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, dueDateTimestamp)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, dueDateTimestamp + (60 * 60 * 1000)) // 1 hour duration
            putExtra(CalendarContract.Events.ALL_DAY, false)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Launch even if resolveActivity returns null on custom ROMs
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
