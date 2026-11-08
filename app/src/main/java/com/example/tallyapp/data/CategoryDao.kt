package com.example.tallyapp.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CategoryDao {
    /** 使用非空 categoryId 的流水，必须先有对应分类。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: Category)

    /** 删除分类时，关联流水仍保留，categoryId 由外键置空。 */
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: String)
}
