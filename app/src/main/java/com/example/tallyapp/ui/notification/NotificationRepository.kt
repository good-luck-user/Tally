package com.example.tallyapp.ui.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class NotificationSnapshot(
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val savedCount: Int = 0,
    val duplicateCount: Int = 0,
    val lastSavedAtMillis: Long? = null,
    val error: String? = null,
    val storageError: String? = null,
    val deliveryError: String? = null,
)

/** 仅提供监听/入库状态；消息内容统一从 Room 查询，不再缓存消息列表。 */
object NotificationRepository {
    private val mutableState = MutableStateFlow(NotificationSnapshot())
    val state = mutableState.asStateFlow()

    internal fun connected() {
        mutableState.update { it.copy(connected = true, connecting = false, error = null) }
    }

    internal fun connecting() {
        mutableState.update { it.copy(connected = false, connecting = true, error = null) }
    }

    internal fun connectionFailed(message: String) {
        mutableState.update { it.copy(connected = false, connecting = false, error = message) }
    }

    internal fun disconnected() {
        mutableState.update { it.copy(connected = false) }
    }

    internal fun stored(inserted: Int) {
        mutableState.update {
            it.copy(
                savedCount = it.savedCount + inserted,
                duplicateCount = it.duplicateCount + (1 - inserted),
                lastSavedAtMillis = System.currentTimeMillis(),
                storageError = null,
            )
        }
    }

    internal fun storageFailed() {
        mutableState.update { it.copy(storageError = "消息保存失败，正在重试。") }
    }

    internal fun deliveryFailed(message: String) {
        // 不因后续写入成功而掩盖曾经发生的入队失败。
        mutableState.update { it.copy(deliveryError = message) }
    }

    fun permissionRevoked() {
        NotificationAccess.cancelPendingConnection()
        mutableState.update { it.copy(connected = false, connecting = false, error = null) }
    }
}
