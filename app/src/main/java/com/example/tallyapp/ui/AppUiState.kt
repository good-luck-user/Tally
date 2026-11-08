package com.example.tallyapp.ui

import com.example.tallyapp.ui.notification.NotificationSnapshot

/** 页面展示所需的全部状态。通知内容只在内存中保存。 */
data class AppUiState(
    val selectedTab: Int = 0,
    val selectedPage: Int = 0,
    val hasNotificationAccess: Boolean = false,
    val notifications: NotificationSnapshot = NotificationSnapshot(),
)
