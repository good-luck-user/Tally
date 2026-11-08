package com.example.tallyapp.ui.notification

import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.provider.Settings
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import com.example.tallyapp.logging.TallyLog as Log

/** Notification access is a special user grant, not a runtime permission dialog. */
object NotificationAccess {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingReset: Runnable? = null
    private var pendingTimeout: Runnable? = null
    private var connectionPending = false

    fun isGranted(context: Context): Boolean {
        val component = ComponentName(context, PaymentNotificationListener::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            context.getSystemService(NotificationManager::class.java)
                .isNotificationListenerAccessGranted(component)
        } else {
            Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.split(':')?.any { ComponentName.unflattenFromString(it) == component } == true
        }
    }

    fun openSettings(context: Context) {
        Log.i("TallyApp", "打开系统通知使用权设置")
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /** Manual reset only affects this app's binding; it never grants notification access. */
    fun reconnectIfGranted(context: Context, restartBinding: Boolean = false) {
        val appContext = context.applicationContext
        if (!isGranted(context)) {
            Log.i("TallyApp", "连接跳过：通知使用权未开启，请先到系统设置授权")
            return
        }
        if (NotificationRepository.state.value.connected && !restartBinding) return
        if (connectionPending) {
            Log.i("TallyApp", "连接请求仍在等待系统响应，请勿重复点击")
            return
        }
        connectionPending = true
        NotificationRepository.connecting()
        val component = ComponentName(context, PaymentNotificationListener::class.java)
        try {
            if (restartBinding && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Log.i("TallyApp", "手动重新连接：先解除本应用的旧通知监听绑定，不撤销通知使用权")
                // API 34+ static method works without a connected service instance.
                NotificationListenerService.requestUnbind(component)
                pendingReset = Runnable {
                    pendingReset = null
                    submitRebind(appContext, component)
                }.also { handler.postDelayed(it, 500L) }
            } else {
                submitRebind(appContext, component)
            }
        } catch (exception: RuntimeException) {
            connectionFailed(exception)
        }
    }

    private fun submitRebind(context: Context, component: ComponentName) {
        if (!isGranted(context)) {
            NotificationRepository.permissionRevoked()
            Log.i("TallyApp", "连接取消：通知使用权已关闭")
            return
        }
        try {
            Log.i("TallyApp", "开始连接：请求系统重新绑定通知监听，等待 onListenerConnected")
            NotificationListenerService.requestRebind(component)
            Log.i("TallyApp", "重连请求已提交；这不代表服务已连接，最多等待 15 秒")
            if (!connectionPending) return // Callback may already have completed.
            pendingTimeout = Runnable {
                pendingTimeout = null
                connectionPending = false
                if (!NotificationRepository.state.value.connected) {
                    val message = "系统在 15 秒内未建立连接。请点击重新连接；仍失败时在通知权限设置中关闭后重新开启。"
                    NotificationRepository.connectionFailed(message)
                    Log.i("TallyApp", "连接超时：未收到 onListenerConnected，系统没有完成绑定；可手动重建绑定或重新开启通知使用权")
                }
            }.also { handler.postDelayed(it, 15_000L) }
        } catch (exception: RuntimeException) {
            connectionFailed(exception)
        }
    }

    private fun connectionFailed(exception: RuntimeException) {
        cancelPendingConnection()
        NotificationRepository.connectionFailed("重连请求失败：${exception.javaClass.simpleName}，请查看 TallyApp 日志。")
        Log.i("TallyApp", "提交通知监听重连请求失败", exception)
    }

    internal fun onConnected() {
        cancelPendingConnection()
    }

    internal fun cancelPendingConnection() {
        pendingReset?.let(handler::removeCallbacks)
        pendingTimeout?.let(handler::removeCallbacks)
        pendingReset = null
        pendingTimeout = null
        connectionPending = false
    }
}
