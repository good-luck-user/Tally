package com.example.tallyapp.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MessageStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "message-storage-test-${UUID.randomUUID()}"
    private lateinit var database: messageDatabase

    @Before
    fun open() {
        database = Room.databaseBuilder(context, messageDatabase::class.java, databaseName).build()
    }

    @After
    fun close() {
        database.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun repeatedImportsPreserveProcessedStatus() = runBlocking {
        val repository = OfflineItemsRepository(database.itemDao())
        val message = Item(time = 123, app = "com.tencent.mm", message = "测试消息", status = ItemStatus.PENDING)
        assertEquals(1, repository.importItems(listOf(message, message)))
        val saved = repository.getAllItemsStream().first().single()
        repository.updateItem(saved.copy(status = ItemStatus.PROCESSED))
        assertEquals(0, repository.importItems(listOf(message)))
        assertEquals(listOf(saved.copy(status = ItemStatus.PROCESSED)), repository.getAllItemsStream().first())
    }

    @Test
    fun messagesAndDeduplicationSurviveDatabaseReopen() = runBlocking {
        val message = Item(time = 123, app = "com.tencent.mm", message = "持久化测试", status = ItemStatus.PENDING)
        database.itemDao().importItems(listOf(message))
        val saved = database.itemDao().getAllItems().first()
        database.close()
        open()
        assertEquals(saved, database.itemDao().getAllItems().first())
        assertEquals(0, database.itemDao().importItems(listOf(message)))
    }

    @Test
    fun changedContentIsSavedAndFlowEmitsNewestFirst() = runBlocking {
        val repository = OfflineItemsRepository(database.itemDao())
        val old = Item(time = 100, app = "com.tencent.mm", message = "旧消息", status = ItemStatus.PENDING)
        repository.importItems(listOf(old))
        val observation = async {
            withTimeout(10_000) { repository.getAllItemsStream().first { it.size == 2 } }
        }
        val newer = old.copy(time = 200, message = "新消息")
        assertEquals(1, repository.importItems(listOf(old, newer)))
        assertEquals(listOf("新消息", "旧消息"), observation.await().map { it.message })
    }

    @Test
    fun concurrentImportsDoNotDuplicateMessages() = runBlocking {
        val message = Item(time = 123, app = "com.tencent.mm", message = "并发测试", status = ItemStatus.PENDING)
        val first = async { database.itemDao().importItems(listOf(message)) }
        val second = async { database.itemDao().importItems(listOf(message)) }
        assertEquals(1, first.await() + second.await())
        assertEquals(1, database.itemDao().getAllItems().first().size)
    }
}
