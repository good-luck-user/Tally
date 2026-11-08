package com.example.tallyapp.ui.notification

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 单消费者顺序写入。有界队列不会阻塞回调，也不覆盖尚未保存的旧消息。 */
internal class NotificationQueue<T, R>(
    scope: CoroutineScope,
    capacity: Int = 256,
    private val retryDelayMillis: Long = 1_000,
    private val prepare: (T) -> R,
    private val onPreparationFailure: (T, Exception) -> Unit,
    private val save: suspend (R) -> Unit,
    private val onFailure: (Exception) -> Unit,
) {
    private val channel = Channel<T>(capacity)
    private val consumer = scope.launch {
        for (message in channel) {
            // 转换只做一次。格式错误单独报告并跳过，不占住队列无限重试。
            val prepared = try {
                prepare(message)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onPreparationFailure(message, error)
                continue
            }
            while (true) {
                try {
                    save(prepared)
                    break
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    onFailure(error)
                    delay(retryDelayMillis)
                }
            }
        }
    }

    /** false 必须由调用方报告；不会静默丢弃旧消息来腾出空间。 */
    fun tryEnqueue(message: T): Boolean = channel.trySend(message).isSuccess

    fun close() {
        consumer.cancel()
        channel.cancel()
    }
}
