package com.example.tallyapp.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MessageProcessingTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val name = "processing-test-${UUID.randomUUID()}"
    private lateinit var db: messageDatabase

    @Before fun open() {
        db = Room.databaseBuilder(context, messageDatabase::class.java, name).build()
    }

    @After fun close() {
        db.close()
        context.deleteDatabase(name)
    }

    @Test fun observerProcessesPaymentsIgnoresChatsAndRetainsFailures() = runBlocking {
        val processor = MessageProcessor(db)
        val consumer = launch { processor.observeAndProcess() }
        try {
            db.itemDao().importItems(listOf(
                item("微信支付\n支付成功12.34元", 1),
                item("微信支付\n收款到账2元", 2),
                item("微信支付\n转账成功3元", 3),
                item("微信支付\n退款成功4元", 4),
                item("小明\n晚上见", 5),
                item("微信支付\n支付成功，金额未知", 6),
            ))
            val messages = withTimeout(15_000) {
                db.itemDao().getAllItems().first { rows -> rows.size == 6 && rows.none { it.status == ItemStatus.PENDING } }
            }
            assertEquals(listOf(1, 1, 1, 1, 2, 3), messages.sortedBy { it.time }.map { it.status })
            val transactions = db.transactionDao().getAllTransactions().first()
            assertEquals(4, transactions.size)
            assertEquals(setOf(TransactionType.EXPENSE, TransactionType.INCOME, TransactionType.TRANSFER, TransactionType.REFUND), transactions.map { it.type }.toSet())
            assertEquals(1234L, transactions.single { it.type == TransactionType.EXPENSE }.amountMinor)
            assertTrue(transactions.all { it.paymentChannel == PaymentChannel.WECHAT && it.status == TransactionStatus.PENDING })
        } finally {
            consumer.cancelAndJoin()
        }
    }

    @Test fun repeatedAndConcurrentProcessingCreatesOnlyOneTransaction() = runBlocking {
        val message = save(item("微信支付\n支付成功12元"))
        val a = async { MessageProcessor(db).process(message) }
        val b = async { MessageProcessor(db).process(message) }
        a.await()
        b.await()
        MessageProcessor(db).process(message)
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
        assertEquals(ItemStatus.PROCESSED, db.itemDao().getItemOnce(message.id)!!.status)
    }

    @Test fun providerIdDeduplicatesDifferentNotificationsButAmountConflictFails() = runBlocking {
        val processor = MessageProcessor(db)
        val first = save(item("微信支付\n支付成功12元\n交易单号：WX123456", 1))
        val duplicate = save(item("微信支付\n付款成功12元\n交易单号：WX123456", 2))
        val conflict = save(item("微信支付\n支付成功20元\n交易单号：WX123456", 3))
        processor.process(first)
        processor.process(duplicate)
        processor.process(conflict)
        val transactions = db.transactionDao().getAllTransactions().first()
        assertEquals(1, transactions.size)
        assertEquals(1200L, transactions.single().amountMinor)
        assertEquals(ItemStatus.PROCESSED, db.itemDao().getItemOnce(duplicate.id)!!.status)
        assertEquals(ItemStatus.FAILED, db.itemDao().getItemOnce(conflict.id)!!.status)
    }

    @Test fun equalAmountsWithoutProviderIdAreNotMergedAcrossMessages() = runBlocking {
        val a = save(item("微信支付\n支付成功12元", 1))
        val b = save(item("微信支付\n支付成功12元", 2))
        val processor = MessageProcessor(db)
        processor.process(a)
        processor.process(b)
        assertEquals(2, db.transactionDao().getAllTransactions().first().size)
    }

    @Test fun statusWriteFailureRollsBackInsertedTransactionAndCanRetry() = runBlocking {
        val message = save(item("微信支付\n支付成功12元"))
        db.openHelper.writableDatabase.execSQL("""
            CREATE TRIGGER fail_status BEFORE UPDATE OF status ON items
            WHEN NEW.status = 1 BEGIN SELECT RAISE(ABORT, 'test failure'); END
        """.trimIndent())
        try {
            MessageProcessor(db).process(message)
            fail("Expected transaction to roll back")
        } catch (expected: android.database.sqlite.SQLiteException) {
            assertEquals(ItemStatus.PENDING, db.itemDao().getItemOnce(message.id)!!.status)
            assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        }
        db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_status")
        MessageProcessor(db).process(message)
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)
        assertEquals(ItemStatus.PROCESSED, db.itemDao().getItemOnce(message.id)!!.status)
    }

    @Test fun reopenedDatabaseProcessesPendingMessagesAcrossMultipleBatches() = runBlocking {
        db.itemDao().importItems((1L..105L).map { item("微信支付\n支付成功1元", it) })
        db.close()
        open()
        val consumer = launch { MessageProcessor(db).observeAndProcess() }
        try {
            withTimeout(30_000) {
                db.itemDao().getAllItems().first { rows -> rows.size == 105 && rows.all { it.status == ItemStatus.PROCESSED } }
            }
            assertEquals(105, db.transactionDao().getAllTransactions().first().size)
        } finally {
            consumer.cancelAndJoin()
        }
    }

    private suspend fun save(item: Item): Item {
        db.itemDao().insert(item)
        return db.itemDao().getAllItems().first().first { it.time == item.time }
    }

    private fun item(text: String, time: Long = 1) =
        Item(time = time, app = "com.tencent.mm", message = text, status = ItemStatus.PENDING)
}
