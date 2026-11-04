package com.example.tallyapp.notification

/** Replace the sink in PaymentNotificationListener to connect bookkeeping logic. */
fun interface NotificationSink {
    /** Runs on the main thread. Move database/network/expensive parsing to a worker. */
    fun onNotification(notification: ReceivedNotification)
}
