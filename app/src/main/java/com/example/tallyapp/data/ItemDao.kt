package com.example.tallyapp.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * Database access object to access the Inventory database
 */
@Dao
interface ItemDao {

    @Query("SELECT * from items ORDER BY time DESC, id DESC")
    fun getAllItems(): Flow<List<Item>>

    @Query("SELECT * from items WHERE id = :id")
    fun getItem(id: Int): Flow<Item>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun getItemOnce(id: Int): Item?

    /** 分批读取，Room 有新消息或状态变化时自动再次通知后台处理器。 */
    @Query("SELECT * FROM items WHERE status = 0 ORDER BY id ASC LIMIT 100")
    fun observePendingItems(): Flow<List<Item>>

    @Query("UPDATE items SET status = :status WHERE id = :id AND status = 0")
    suspend fun finishProcessing(id: Int, status: Int): Int

    // Specify the conflict strategy as IGNORE, when the user tries to add an
    // existing Item into the database Room ignores the conflict.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Item)

    @Update
    suspend fun update(item: Item)

    @Delete
    suspend fun delete(item: Item)

    @Query("SELECT EXISTS(SELECT 1 FROM items WHERE time = :time AND app = :app AND message = :message)")
    suspend fun containsMessage(time: Long, app: String, message: String): Boolean

    /** 在同一事务中检查和写入；重复投递不会新增记录或覆盖处理状态。 */
    @Transaction
    suspend fun importItems(items: List<Item>): Int {
        var inserted = 0
        for (item in items) {
            if (!containsMessage(item.time, item.app, item.message)) {
                insert(item)
                inserted++
            }
        }
        return inserted
    }
}
