package com.example.tallyapp.data

import android.content.Context
import com.example.tallyapp.logging.TallyLog as Log
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

/** Model 层后台处理器。先解析，再在一个事务中去重、写流水和更新原始消息状态。 */
class MessageProcessor(
    private val database: messageDatabase,
    private val parser: PaymentParser = PaymentParser(),
) {
    suspend fun observeAndProcess() {
        database.itemDao().observePendingItems().collect { messages ->
            for (message in messages) process(message)
        }
    }

    suspend fun process(message: Item) {
        if (message.status != ItemStatus.PENDING) return
        val result = parser.parse(message)
        database.withTransaction {
            val items = database.itemDao()
            // 其他处理任务可能已经完成，或者用户已修改消息；不覆盖新状态。
            if (items.getItemOnce(message.id) != message) return@withTransaction
            val status = when (result) {
                PaymentParseResult.Ignored -> ItemStatus.IGNORED
                is PaymentParseResult.Failed -> {
                    Log.i("TallyApp", "消息识别失败：id=${message.id}，原因=${result.reason}")
                    ItemStatus.FAILED
                }
                is PaymentParseResult.Payment -> {
                    // 有渠道交易号时跨通知去重；否则只保证同一原始消息不重复入账。
                    val source = result.externalId?.let { "trade:$it" } ?: "item:${message.id}"
                    val key = "${result.channel}|${result.type}|$source"
                    val id = UUID.nameUUIDFromBytes(key.toByteArray(Charsets.UTF_8)).toString()
                    val transactions = database.transactionDao()
                    val existing = transactions.getTransactionOnce(id)
                    if (existing != null && (existing.amountMinor != result.amountMinor ||
                            existing.type != result.type || existing.paymentChannel != result.channel)) {
                        Log.i("TallyApp", "交易标识冲突：itemId=${message.id}，保留原流水供检查")
                        ItemStatus.FAILED

                    } else {
                        if (existing == null) {
                            transactions.insert(TransactionRecord(
                                id = id,
                                type = result.type,
                                amountMinor = result.amountMinor,
                                paymentChannel = result.channel,
                                occurredAt = result.occurredAt,
                                status = TransactionStatus.PENDING,
                            ))
                        }
                        ItemStatus.PROCESSED
                    }
                }
            }
            check(items.finishProcessing(message.id, status) == 1)
        }
    }

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private var worker: Job? = null

        /** 页面或服务启动都可调用；一个进程只启动一个消费者，不持有 Activity。 */
        @Synchronized
        fun start(context: Context) {
            if (worker?.isActive == true) return
            val processor = MessageProcessor(messageDatabase.getDatabase(context.applicationContext))
            worker = scope.launch {
                while (currentCoroutineContext().isActive) {
                    try {
                        processor.observeAndProcess()
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Exception) {
                        // 数据库异常不会将消息标记成功；下次订阅会再次读到未处理消息。
                        Log.i("TallyApp", "后台交易处理失败，保留未处理消息并重试", error)
                        delay(1_000)
                    }
                }
            }
        }
    }
}
