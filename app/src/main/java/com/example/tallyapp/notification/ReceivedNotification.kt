package com.example.tallyapp.notification

import android.app.Notification
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import org.json.JSONObject

/** Raw notification data, deliberately not treated as a confirmed bill. */
data class ReceivedNotification(
    val key: String,
    val packageName: String,
    val notificationId: Int,
    val postedAtMillis: Long,
    val receivedAtMillis: Long,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val textLines: List<String>,
    val messageTexts: List<String>,
    val isGroupSummary: Boolean,
) {
    fun toJson(): String = JSONObject().apply {
        put("event", "posted_or_updated")
        put("key", key)
        put("packageName", packageName)
        put("notificationId", notificationId)
        put("postedAtMillis", postedAtMillis)
        put("receivedAtMillis", receivedAtMillis)
        put("title", title ?: JSONObject.NULL)
        put("text", text ?: JSONObject.NULL)
        put("bigText", bigText ?: JSONObject.NULL)
        put("subText", subText ?: JSONObject.NULL)
        put("textLines", JSONArray(textLines))
        put("messageTexts", JSONArray(messageTexts))
        put("isGroupSummary", isGroupSummary)
    }.toString()

    companion object {
        fun from(sbn: StatusBarNotification): ReceivedNotification {
            val notification = sbn.notification
            val extras = notification.extras
            val messagingStyle = NotificationCompat.MessagingStyle
                .extractMessagingStyleFromNotification(notification)
            return ReceivedNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                notificationId = sbn.id,
                postedAtMillis = sbn.postTime,
                receivedAtMillis = System.currentTimeMillis(),
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
                subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                    ?.map { it.toString() }.orEmpty(),
                messageTexts = messagingStyle?.messages
                    ?.mapNotNull { it.text?.toString() }.orEmpty(),
                isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            )
        }
    }
}
