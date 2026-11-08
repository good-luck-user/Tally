package com.example.tallyapp.data

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class MessageMigrationTest {
    @Test fun migrateVersionOne() = verifyMigration(1)
    @Test fun migrateVersionTwo() = verifyMigration(2)

    private fun verifyMigration(oldVersion: Int) = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test-${UUID.randomUUID()}"
        val legacyStatuses = listOf("未处理", "已处理", "已忽略", "处理失败", "unknown", "0", "待处理")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            old.execSQL("CREATE TABLE items (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, time INTEGER NOT NULL, app TEXT NOT NULL, message TEXT NOT NULL, status TEXT NOT NULL)")
            legacyStatuses.forEachIndexed { index, status ->
                old.execSQL("INSERT INTO items(id,time,app,message,status) VALUES(?,?,?,?,?)", arrayOf(index + 1, index + 1, "test", "message-$index", status))
            }
            old.execSQL("INSERT INTO items(id,time,app,message,status) VALUES(999,1,'test','deleted','未处理')")
            old.execSQL("DELETE FROM items WHERE id = 999")
            if (oldVersion == 2) {
                old.execSQL("CREATE TABLE categories (id TEXT NOT NULL, PRIMARY KEY(id))")
                old.execSQL("""
                    CREATE TABLE transactions (
                        id TEXT NOT NULL, type TEXT NOT NULL, amountMinor INTEGER NOT NULL,
                        paymentChannel TEXT NOT NULL, categoryId TEXT, occurredAt INTEGER NOT NULL,
                        status TEXT NOT NULL, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id), FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL
                    )
                """.trimIndent())
                old.execSQL("CREATE INDEX index_transactions_categoryId ON transactions(categoryId)")
                old.execSQL("CREATE INDEX index_transactions_occurredAt ON transactions(occurredAt)")
                old.execSQL("INSERT INTO transactions VALUES('existing','EXPENSE',123,'CASH',NULL,1,'CONFIRMED',1,1)")
            }
            old.version = oldVersion
        }
        val upgraded = Room.databaseBuilder(context, messageDatabase::class.java, name)
            .addMigrations(messageDatabase.MIGRATION_1_2, messageDatabase.MIGRATION_2_3, messageDatabase.MIGRATION_3_4)
            .build()
        try {
            // 打开时 Room 会核验升级结果是否与当前实体结构一致。
            val messages = upgraded.itemDao().getAllItems().first().sortedBy { it.id }
            assertEquals(listOf(0, 1, 2, 3, 3, 0, 0), messages.map { it.status })
            assertEquals((0..6).map { "message-$it" }, messages.map { it.message })
            if (oldVersion == 2) {
                assertEquals(123L, upgraded.transactionDao().getTransactionOnce("existing")!!.amountMinor)
            }
            upgraded.itemDao().insert(Item(time = 9999, app = "test", message = "new", status = 0))
            assertTrue(upgraded.itemDao().getAllItems().first().single { it.time == 9999L }.id > 999)
        } finally {
            upgraded.close()
            context.deleteDatabase(name)
        }
    }
}
