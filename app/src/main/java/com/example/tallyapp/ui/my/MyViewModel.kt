package com.example.tallyapp.ui.my

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tallyapp.data.MyRepository
import com.example.tallyapp.data.TransactionStatus
import com.example.tallyapp.data.TransactionType
import com.example.tallyapp.logging.TallyLog
import com.example.tallyapp.ui.transactions.money
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal

enum class MyEditField { USER_NAME, MONTHLY_BUDGET }

data class MyUiState(
    val userName: String = "记账用户",
    val monthlyBudget: String = "0.00",
    val totalExpense: String = "0.00",
    val totalIncome: String = "0.00",
    val accountCount: Int = 1,
    val categoryCount: Int = 0,
    val transactionCount: Int = 0,
    val loading: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    val editing: MyEditField? = null,
    val editValue: String = "",
    val saving: Boolean = false,
    val exporting: Boolean = false,
    val showAbout: Boolean = false,
)

/** 管理页面状态与操作；存储交给 Repository，不持有 Activity。 */
class MyViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MyRepository(application)
    private val mutableState = MutableStateFlow(MyUiState())
    val uiState = mutableState.asStateFlow()
    private var loadJob: Job? = null

    fun loadUserData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            mutableState.update { it.copy(loading = true, error = null) }
            try {
                val profile = repository.loadProfile()
                mutableState.update { it.copy(userName = profile.userName, monthlyBudget = profile.monthlyBudget) }
                repository.observeTransactions().collect { rows ->
                    val visible = rows.filter { it.status != TransactionStatus.VOID }
                    val counted = visible.filter { !it.excludedFromStats }
                    mutableState.update { it.copy(
                        totalExpense = money(counted.filter { row -> row.type == TransactionType.EXPENSE }.sumOf { row -> row.amountMinor }),
                        totalIncome = money(counted.filter { row -> row.type == TransactionType.INCOME }.sumOf { row -> row.amountMinor }),
                        transactionCount = visible.size,
                        categoryCount = visible.mapNotNull { row -> row.categoryId }.distinct().size,
                        loading = false,
                    ) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                TallyLog.e("TallyApp", "我的页面加载失败", error)
                mutableState.update { it.copy(loading = false, error = "加载失败，请点击刷新") }
            }
        }
    }

    fun startEditing(field: MyEditField) {
        if (uiState.value.saving) return
        mutableState.update { it.copy(editing = field, error = null, editValue = if (field == MyEditField.USER_NAME) it.userName else it.monthlyBudget) }
    }
    fun updateEditValue(value: String) { if (!uiState.value.saving) mutableState.update { it.copy(editValue = value, error = null) } }
    fun cancelEditing() { if (!uiState.value.saving) mutableState.update { it.copy(editing = null, error = null) } }

    fun saveEditing() {
        val state = uiState.value
        val field = state.editing ?: return
        if (state.saving) return
        mutableState.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                val value = state.editValue.trim()
                if (field == MyEditField.USER_NAME) {
                    require(value.isNotEmpty() && value.length <= 20) { "用户名请输入 1 到 20 个字符" }
                    repository.saveUserName(value)
                    mutableState.update { it.copy(userName = value, editing = null) }
                } else {
                    require(Regex("[0-9]+(?:\\.[0-9]{1,2})?").matches(value)) { "预算请输入非负金额，最多两位小数" }
                    val amount = try { BigDecimal(value).movePointRight(2).longValueExact() }
                        catch (error: ArithmeticException) { throw IllegalArgumentException("预算金额过大") }
                    val budget = money(amount)
                    repository.saveMonthlyBudget(budget)
                    mutableState.update { it.copy(monthlyBudget = budget, editing = null) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { mutableState.update { it.copy(error = error.message ?: "保存失败，请重试") } }
            finally { mutableState.update { it.copy(saving = false) } }
        }
    }

    fun exportLogs(target: Uri) {
        if (uiState.value.exporting) return
        mutableState.update { it.copy(exporting = true) }
        viewModelScope.launch {
            try {
                repository.exportLogs(target)
                showMessage("日志已导出")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                TallyLog.e("TallyApp", "我的页面导出日志失败", error)
                mutableState.update { it.copy(error = "日志导出失败，请重试") }
            } finally { mutableState.update { it.copy(exporting = false) } }
        }
    }
    fun showMessage(message: String) { mutableState.update { it.copy(message = message) } }
    fun showAbout(show: Boolean) { mutableState.update { it.copy(showAbout = show) } }
    fun clearError() { mutableState.update { it.copy(error = null, message = null) } }
}
