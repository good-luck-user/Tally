package com.example.tallyapp.data

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.UUID

/** 核心交易流水。金额始终为正整数分，收入/支出方向通过 type 表达。 */
@Entity(
    tableName = "transactions",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.SET_NULL,
    )],
    indices = [Index(value = ["categoryId"]), Index(value = ["occurredAt"])],
)
@TypeConverters(TransactionConverters::class)
data class TransactionRecord(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val type: TransactionType,
    val amountMinor: Long,
    val paymentChannel: PaymentChannel,
    val categoryId: String? = null,
    /** 实际交易时间，Unix 毫秒。 */
    val occurredAt: Long,
    val status: TransactionStatus = TransactionStatus.PENDING,
    /** 创建时间，Unix 毫秒。 */
    val createdAt: Long = System.currentTimeMillis(),
    /** 修改流水时必须同步更新，数据库不会自动修改此字段。 */
    val updatedAt: Long = createdAt,
    @ColumnInfo(defaultValue = "''") val note: String = "",
    @ColumnInfo(defaultValue = "0") val excludedFromStats: Boolean = false,
) {
    init {
        require(id.isNotBlank()) { "流水 ID 不能为空" }
        require(amountMinor > 0) { "金额必须为正整数分" }
    }
}

enum class TransactionType { EXPENSE, INCOME, TRANSFER, REFUND }

enum class PaymentChannel { WECHAT, ALIPAY, CASH, BANK, OTHER }

enum class TransactionStatus { PENDING, CONFIRMED, VOID }

/** Kotlin 使用枚举限制可选值，Room 中按枚举名称保存为 TEXT。 */
class TransactionConverters {
    @TypeConverter
    fun typeToText(value: TransactionType): String = value.name

    @TypeConverter
    fun textToType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun channelToText(value: PaymentChannel): String = value.name

    @TypeConverter
    fun textToChannel(value: String): PaymentChannel = PaymentChannel.valueOf(value)

    @TypeConverter
    fun statusToText(value: TransactionStatus): String = value.name

    @TypeConverter
    fun textToStatus(value: String): TransactionStatus = TransactionStatus.valueOf(value)
}
