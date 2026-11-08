package com.example.tallyapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY occurredAt DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionRecord>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    fun getTransaction(id: String): Flow<TransactionRecord?>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionOnce(id: String): TransactionRecord?

    /** 重复流水 ID 直接报错，避免覆盖已存在的交易。 */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionRecord)

    /** 调用时传入 copy(updatedAt = System.currentTimeMillis()) 后的记录。 */
    @Update
    suspend fun update(transaction: TransactionRecord): Int
}
