package com.example.tallyapp.ui.notification

import android.service.notification.StatusBarNotification

/** 回调只保存原始对象引用和接收时间；入队后只读，不修改通知内容。 */
internal data class QueuedNotification(
    val sbn: StatusBarNotification,
    val receivedAtMillis: Long,
)
