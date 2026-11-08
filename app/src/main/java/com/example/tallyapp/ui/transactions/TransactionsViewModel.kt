package com.example.tallyapp.ui.transactions

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallyapp.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

data class TransactionRow(val record: TransactionRecord, val category: String, val note: String)
data class TransactionDraft(
    val row: TransactionRow,
    val amount: String = money(row.record.amountMinor),
    val category: String = row.record.categoryId.orEmpty(),
    val note: String = row.note,
    val type: TransactionType = row.record.type,
    val channel: PaymentChannel = row.record.paymentChannel,
    val occurredAt: Long = row.record.occurredAt,
    val excluded: Boolean = row.record.excludedFromStats,
)
data class TransactionsUiState(
    val rows: List<TransactionRow> = emptyList(),
    val month: Long = System.currentTimeMillis(),
    val draft: TransactionDraft? = null,
    val loading: Boolean = true,
    val opening: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TransactionsRepository(messageDatabase.getDatabase(application))
    private val mutableState = MutableStateFlow(TransactionsUiState())
    val uiState = mutableState.asStateFlow()
    private var loadJob: Job? = null

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                repository.observeAll().collect { records ->
                    mutableState.update { it.copy(rows = records.map { record -> record.toRow() }, loading = false) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(loading = false, error = "加载失败，请点击重试") } }
        }
    }

    fun open(row: TransactionRow) {
        if (uiState.value.opening || uiState.value.saving) return
        mutableState.update { it.copy(opening = true, error = null) }
        viewModelScope.launch {
            try {
                val record = repository.get(row.record.id) ?: throw IllegalArgumentException("账单已不存在")
                mutableState.update { it.copy(draft = TransactionDraft(record.toRow())) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(error = error.message ?: "无法打开账单") } }
            finally { mutableState.update { it.copy(opening = false) } }
        }
    }
    fun close() {
        if (!uiState.value.saving) mutableState.update { it.copy(draft = null, error = null) }
    }
    fun edit(transform: (TransactionDraft) -> TransactionDraft) {
        if (!uiState.value.saving) mutableState.update { it.copy(draft = it.draft?.let(transform), error = null) }
    }
    fun save() {
        val state = uiState.value
        val draft = state.draft ?: return
        if (state.saving) return
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val input = draft.amount.trim()
                require(Regex("[0-9]+(?:\\.[0-9]{1,2})?").matches(input)) { "请输入正数金额，最多两位小数" }
                val amount = try { BigDecimal(input).movePointRight(2).longValueExact() }
                    catch (error: ArithmeticException) { throw IllegalArgumentException("金额超出支持范围") }
                require(amount > 0) { "金额必须大于零" }
                val original = draft.row.record
                repository.update(original, original.copy(
                    amountMinor = amount, categoryId = draft.category.trim().ifEmpty { null },
                    occurredAt = draft.occurredAt, type = draft.type, paymentChannel = draft.channel,
                    note = draft.note.trim(), excludedFromStats = draft.excluded,
                ))
                mutableState.update { it.copy(draft = null, month = original.createdAt) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(error = error.message ?: "保存失败，请重试") } }
            finally { mutableState.update { it.copy(saving = false) } }
        }
    }
    fun moveMonth(delta: Int) {
        mutableState.update { state ->
            val calendar = Calendar.getInstance().apply { timeInMillis = state.month; add(Calendar.MONTH, delta) }
            state.copy(month = calendar.timeInMillis)
        }
    }
}

private fun TransactionRecord.toRow() = TransactionRow(this, categoryId ?: "未分类", note)
internal fun money(minor: Long): String = BigDecimal.valueOf(minor, 2).toPlainString()
internal fun dateText(time: Long, pattern: String): String = SimpleDateFormat(pattern, Locale.CHINA).format(Date(time))
internal fun TransactionType.label() = when (this) {
    TransactionType.EXPENSE -> "支出"; TransactionType.INCOME -> "收入"
    TransactionType.TRANSFER -> "转账"; TransactionType.REFUND -> "退款"
}
internal fun PaymentChannel.label() = when (this) {
    PaymentChannel.WECHAT -> "微信支付"; PaymentChannel.ALIPAY -> "支付宝"
    PaymentChannel.CASH -> "现金"; PaymentChannel.BANK -> "银行卡"; PaymentChannel.OTHER -> "其他"
}
