package com.example.tallyapp.notification

import com.example.tallyapp.ui.notification.ReceivedNotification
import com.example.tallyapp.ui.notification.toItem
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationItemMapperTest {
    private val message = ReceivedNotification(
        key = "key", packageName = "com.tencent.mm", notificationId = 1,
        postedAtMillis = 100, receivedAtMillis = 200,
        title = "微信", text = "消息正文", bigText = "消息正文\n详情",
        subText = null, textLines = listOf("详情"), messageTexts = listOf("消息正文"),
        isGroupSummary = false,
    )

    @Test
    fun mappingPreservesTextWithoutDuplicatingNotificationVariants() {
        val item = message.toItem()
        assertEquals("微信\n消息正文\n详情", item.message)
        assertEquals("com.tencent.mm", item.app)
        assertEquals(100L, item.time)
        assertEquals(com.example.tallyapp.data.ItemStatus.PENDING, item.status)
    }

    @Test
    fun repeatedReadTimeDoesNotChangeStoredIdentity() {
        assertEquals(message.toItem(), message.copy(receivedAtMillis = 999).toItem())
    }
}
