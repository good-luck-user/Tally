package com.example.tallyapp.notification

import android.app.Activity
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.core.app.NotificationManagerCompat

/** Notification access is a special user grant, not a runtime permission dialog. */
object NotificationAccess {
    fun isGranted(context: Context): Boolean =
        context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Safe while disconnected; never attempts to bypass a revoked user grant. */
    fun reconnectIfGranted(context: Context) {
        if (isGranted(context)) {
            NotificationListenerService.requestRebind(
                ComponentName(context, PaymentNotificationListener::class.java)
            )
        }
    }
}
