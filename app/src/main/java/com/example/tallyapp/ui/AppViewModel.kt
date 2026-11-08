package com.example.tallyapp.ui

import android.app.Application
import com.example.tallyapp.logging.TallyLog as Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.tallyapp.data.MessageProcessor
import com.example.tallyapp.ui.notification.NotificationAccess
import com.example.tallyapp.ui.notification.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** 管理页面状态和用户操作；只使用 Application，不持有 Activity 或 Composable。 */
class AppViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    init {
        // 启动时继续处理数据库里遗留的未处理消息，不依赖再次收到通知。
        MessageProcessor.start(application)
    }

    private val hasAccess = MutableStateFlow(NotificationAccess.isGranted(application))
    private val selectedTab = savedStateHandle.getStateFlow(SELECTED_TAB, 0)
    private val selectedPage = savedStateHandle.getStateFlow("selected_page", 0)

    val uiState: StateFlow<AppUiState> = combine(
        NotificationRepository.state,
        hasAccess,
        selectedTab,
        selectedPage,
    ) { notifications, granted, tab, page ->
        AppUiState(
            selectedTab = tab,
            selectedPage = page,
            hasNotificationAccess = granted,
            notifications = notifications,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppUiState(
            selectedTab = selectedTab.value,
            selectedPage = selectedPage.value,
            hasNotificationAccess = hasAccess.value,
            notifications = NotificationRepository.state.value,
        ),
    )

    fun selectTab(index: Int) {
        if (index in 0..3) savedStateHandle[SELECTED_TAB] = index
    }

    fun selectPage(index: Int) {
        if (index == 0 || index == 3) savedStateHandle["selected_page"] = index
    }

    /** 从系统设置返回时，重新确认授权，必要时连接通知监听。 */
    fun onForeground() {
        val granted = updateAccess()
        Log.i(
            "TallyApp",
            "页面回到前台：通知使用权=$granted，通知服务已连接=${NotificationRepository.state.value.connected}",
        )
        if (granted) NotificationAccess.reconnectIfGranted(getApplication())
    }

    fun reconnectNotifications() {
        if (updateAccess()) NotificationAccess.reconnectIfGranted(getApplication(), restartBinding = true)
    }

    private fun updateAccess(): Boolean {
        val granted = NotificationAccess.isGranted(getApplication())
        hasAccess.value = granted
        if (!granted) NotificationRepository.permissionRevoked()
        return granted
    }

    private companion object {
        const val SELECTED_TAB = "selected_tab"
    }
}
