package com.example.tallyapp.data

import androidx.room.withTransaction

/** 编辑草稿提交的唯一入口，查询、校验和更新在同一个事务中完成。 */
class TransactionsRepository(private val database: messageDatabase) {
    fun observeAll() = database.transactionDao().getAllTransactions()
    suspend fun get(id: String) = database.transactionDao().getTransactionOnce(id)

    suspend fun update(original: TransactionRecord, edited: TransactionRecord) {
        database.withTransaction {
            val current = get(original.id) ?: throw IllegalArgumentException("账单已不存在，请返回明细")
            require(current == original) { "账单已发生变化，请返回后重新打开" }
            require(edited.id == original.id) { "账单 ID 不一致" }
            require(edited.amountMinor > 0) { "金额必须大于零" }
            require(edited.occurredAt > 0) { "请选择有效日期" }
            edited.categoryId?.let { database.categoryDao().insert(Category(it)) }
            check(database.transactionDao().update(edited.copy(
                createdAt = current.createdAt,
                updatedAt = System.currentTimeMillis(),
                status = current.status,
            )) == 1) { "保存失败，账单已不存在" }
        }
    }
}
