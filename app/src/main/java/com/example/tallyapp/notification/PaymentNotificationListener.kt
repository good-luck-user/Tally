package com.example.tallyapp.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/** Android starts/binds this service after the user grants notification access. */
class PaymentNotificationListener : NotificationListenerService() {
    // Integration point for a future bill parser/repository.
    private val sink: NotificationSink = LogcatNotificationSink

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(LogcatNotificationSink.TAG, "Listener connected; waiting for WeChat/Alipay notifications.")
        // Do not replay active notifications; listen to subsequent callbacks only.
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in SOURCE_PACKAGES) return
        try {
            sink.onNotification(ReceivedNotification.from(sbn))
        } catch (exception: RuntimeException) {
            // Malformed extras must not stop later callbacks.
            Log.e(LogcatNotificationSink.TAG, "Unable to read notification from ${sbn.packageName}", exception)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(LogcatNotificationSink.TAG, "Listener disconnected; check notification access/system settings.")
        NotificationAccess.reconnectIfGranted(this)
    }

    companion object {
        private val SOURCE_PACKAGES = setOf(
            "com.tencent.mm", // WeChat
            "com.eg.android.AlipayGphone", // Alipay
        )
    }
}
