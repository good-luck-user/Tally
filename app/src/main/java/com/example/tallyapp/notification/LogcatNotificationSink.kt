package com.example.tallyapp.notification

import android.util.Log

/** Local debug output only. No persistence, server, or transaction inference. */
object LogcatNotificationSink : NotificationSink {
    const val TAG = "MytallyNotification"

    override fun onNotification(notification: ReceivedNotification) {
        // Keep entries below Logcat's byte limit even with Chinese text.
        val parts = splitForLogcat(notification.toJson())
        parts.forEachIndexed { index, part ->
            Log.i(TAG, "notification[${index + 1}/${parts.size}]=$part")
        }
    }
}

/** Keep UTF-16 surrogate pairs together when each chunk is encoded by Logcat. */
internal fun splitForLogcat(message: String): List<String> = buildList {
    var start = 0
    while (start < message.length) {
        var end = minOf(start + 900, message.length)
        if (end < message.length && message[end - 1].isHighSurrogate() && message[end].isLowSurrogate()) {
            end--
        }
        add(message.substring(start, end))
        start = end
    }
}
