package com.example.tallyapp.ui.notification

import android.content.Intent
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.tallyapp.logging.TallyLog as Log
import com.example.tallyapp.data.AppDataContainer
import com.example.tallyapp.data.Item
import com.example.tallyapp.data.MessageProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

class PaymentNotificationListener : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val itemsRepository by lazy { AppDataContainer(applicationContext).itemsRepository }
    private lateinit var queue: NotificationQueue<QueuedNotification, Item>
    private var connected = false

    override fun onCreate() {
        super.onCreate()
        MessageProcessor.start(applicationContext)
        queue = NotificationQueue(
            scope = serviceScope,
            prepare = { queued ->
                // 提取快照、整理文字都在 IO 消费者中完成，保留真正的接收时间。
                ReceivedNotification.from(queued.sbn, queued.receivedAtMillis).toItem()
            },
            onPreparationFailure = { queued, error ->
                NotificationRepository.deliveryFailed("通知转换失败，本条未保存；后续通知继续处理，请查看 TallyApp 日志。")
                Log.e("TallyApp", "后台通知转换失败：package=${queued.sbn.packageName}", error)
            },
            save = { item ->
                val inserted = itemsRepository.importItems(listOf(item))

                NotificationRepository.stored(inserted)

                Log.i("TallyApp", "队列消息入库成功：新增=$inserted，重复=${1 - inserted}")
            },
            onFailure = { error ->
                NotificationRepository.storageFailed()
                Log.i("TallyApp", "队列消息入库失败，保留当前消息并重试", error)
            },
        )
        Log.i("TallyApp", "通知服务已创建，后台队列消费者已启动")
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.i("TallyApp", "系统正在绑定通知监听服务")
        return super.onBind(intent)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationAccess.onConnected()
        connected = true
        NotificationRepository.connected()
        Log.i("TallyApp", "通知监听连接成功：等待新通知回调，收到后立即入队")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!connected || sbn.packageName !in SOURCE_PACKAGES) return
        // 不解析消息；只记录接收时间并把原始对象引用交给队列。
        val queued = QueuedNotification(sbn, System.currentTimeMillis())
        if (!queue.tryEnqueue(queued)) {
            NotificationRepository.deliveryFailed("消息队列已满或已关闭，本条通知未入队，请查看 TallyApp 日志。")
            Log.e("TallyApp", "通知入队失败：队列已满或已关闭，package=${sbn.packageName}")
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        connected = false
        NotificationRepository.disconnected()
        Log.i("TallyApp", "通知监听断开；已入队消息继续在后台保存")
        if (NotificationAccess.isGranted(this)) NotificationAccess.reconnectIfGranted(this)
        else NotificationRepository.permissionRevoked()
    }

    override fun onDestroy() {
        connected = false
        NotificationRepository.disconnected()
        queue.close()
        serviceScope.cancel()
        Log.i("TallyApp", "通知服务销毁：取消消费者，未入库的内存队列不能跨服务销毁恢复")
        super.onDestroy()
    }

    private companion object {
        val SOURCE_PACKAGES = setOf("com.tencent.mm", "com.eg.android.AlipayGphone")
    }
}
