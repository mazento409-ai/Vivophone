package com.example.vivophone.data

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract

data class CallEntry(
    val number: String,
    val name: String?,
    val type: Int,
    val date: Long,
    val duration: Long
)

data class ContactItem(val id: Long, val name: String, val number: String)

object Repos {
    fun callLog(ctx: Context, limit: Int = 200): List<CallEntry> {
        val out = mutableListOf<CallEntry>()
        ctx.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls.NUMBER, CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION
            ),
            null, null, "${CallLog.Calls.DATE} DESC"
        )?.use { c ->
            while (c.moveToNext() && out.size < limit) {
                out += CallEntry(
                    c.getString(0) ?: "", c.getString(1),
                    c.getInt(2), c.getLong(3), c.getLong(4)
                )
            }
        }
        return out
    }

    fun contacts(ctx: Context): List<ContactItem> {
        val out = mutableListOf<ContactItem>()
        val P = ContactsContract.CommonDataKinds.Phone
        ctx.contentResolver.query(
            P.CONTENT_URI,
            arrayOf(P.CONTACT_ID, P.DISPLAY_NAME, P.NUMBER),
            null, null, "${P.DISPLAY_NAME} ASC"
        )?.use { c ->
            while (c.moveToNext()) {
                out += ContactItem(c.getLong(0), c.getString(1) ?: "", c.getString(2) ?: "")
            }
        }
        return out.distinctBy { it.id to it.number }
    }
}
