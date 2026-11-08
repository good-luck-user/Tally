package com.example.tallyapp.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 分类主键表，为流水的 categoryId 提供外键目标；名称等分类属性可后续补充。 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey
    val id: String,
)
