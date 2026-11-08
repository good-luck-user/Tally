/*
 * Copyright (C) 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.tallyapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.tallyapp.logging.TallyLog

/**
 * Database class with a singleton Instance object.
 */
@Database(
    entities = [Item::class, Category::class, TransactionRecord::class],
    version = 4,
    exportSchema = false,
)
abstract class messageDatabase : RoomDatabase() {

    abstract fun itemDao(): ItemDao

    abstract fun transactionDao(): TransactionDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                TallyLog.i("TallyApp", "数据库迁移开始：3 -> 4")
                // 兼容字段已存在、但数据库仍需执行升级的开发版本，避免重复添加列。
                val columns = db.query("PRAGMA table_info(transactions)").use { cursor ->
                    val nameIndex = cursor.getColumnIndexOrThrow("name")
                    buildSet {
                        while (cursor.moveToNext()) add(cursor.getString(nameIndex).lowercase())
                    }
                }
                if ("note" !in columns) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN note TEXT NOT NULL DEFAULT ''")
                }
                if ("excludedfromstats" !in columns) {
                    db.execSQL("ALTER TABLE transactions ADD COLUMN excludedFromStats INTEGER NOT NULL DEFAULT 0")
                }
                // Room 仍会验证迁移后的完整表结构，不忽略字段类型等不匹配错误。
                TallyLog.i("TallyApp", "迁移 3 -> 4 的 SQL 已执行，等待校验和提交")
            }
        }
        /** TEXT 状态迁移为 INTEGER，保留原始消息、流水和自增 ID 的高水位。 */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val countBefore = db.query("SELECT COUNT(*) FROM items").use { it.moveToFirst(); it.getLong(0) }
                TallyLog.i("TallyApp", "数据库迁移开始：2 -> 3，原始消息=$countBefore 条")
                val oldSequence = db.query("SELECT seq FROM sqlite_sequence WHERE name = 'items'").use {
                    if (it.moveToFirst()) it.getLong(0) else 0L
                }
                db.execSQL("""
                    CREATE TABLE items_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        time INTEGER NOT NULL, app TEXT NOT NULL,
                        message TEXT NOT NULL, status INTEGER NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    INSERT INTO items_new (id, time, app, message, status)
                    SELECT id, time, app, message,
                        CASE CAST(status AS TEXT)
                            WHEN '未处理' THEN 0 WHEN '待处理' THEN 0 WHEN '0' THEN 0
                            WHEN '已处理' THEN 1 WHEN '1' THEN 1
                            WHEN '已忽略' THEN 2 WHEN '2' THEN 2
                            WHEN '处理失败' THEN 3 WHEN '3' THEN 3
                            ELSE 3
                        END
                    FROM items
                """.trimIndent())
                db.execSQL("DROP TABLE items")
                db.execSQL("ALTER TABLE items_new RENAME TO items")
                db.execSQL("UPDATE sqlite_sequence SET seq = MAX(seq, ?) WHERE name = 'items'", arrayOf(oldSequence))
                db.execSQL("CREATE INDEX index_items_status ON items (status)")
                val countAfter = db.query("SELECT COUNT(*) FROM items").use { it.moveToFirst(); it.getLong(0) }
                TallyLog.i("TallyApp", "迁移 2 -> 3 的 SQL 已执行，原始消息=$countAfter 条，等待校验和提交")
            }
        }

        /** 只新增表和索引，保留版本 1 中的原始消息。 */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                TallyLog.i("TallyApp", "数据库迁移开始：1 -> 2")
                db.execSQL("CREATE TABLE IF NOT EXISTS categories (id TEXT NOT NULL, PRIMARY KEY(id))")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transactions (
                        id TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amountMinor INTEGER NOT NULL,
                        paymentChannel TEXT NOT NULL,
                        categoryId TEXT,
                        occurredAt INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(id),
                        FOREIGN KEY(categoryId) REFERENCES categories(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_categoryId ON transactions (categoryId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transactions_occurredAt ON transactions (occurredAt)")
                TallyLog.i("TallyApp", "迁移 1 -> 2 的 SQL 已执行，等待校验和提交")
            }
        }

        @Volatile
        private var Instance: messageDatabase? = null

        fun getDatabase(context: Context): messageDatabase {
            // if the Instance is not null, return it, otherwise create a new database instance.
            return Instance ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, messageDatabase::class.java, "item_database")
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            val items = db.query("SELECT COUNT(*) FROM items").use { it.moveToFirst(); it.getLong(0) }
                            val transactions = db.query("SELECT COUNT(*) FROM transactions").use { it.moveToFirst(); it.getLong(0) }
                            TallyLog.i("TallyApp", "数据库打开成功：版本=${db.version}，items=$items 条，transactions=$transactions 条")
                        }
                    })
                    .build()
                    .also { Instance = it }
            }
        }
    }
}
