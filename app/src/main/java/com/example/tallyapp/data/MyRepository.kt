package com.example.tallyapp.data

import android.content.Context
import android.net.Uri
import com.example.tallyapp.logging.TallyLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class MyProfile(val userName: String, val monthlyBudget: String)

/** 我的页面数据源：个人设置存本机，账单统计沿用 Room 流水。 */
class MyRepository(context: Context) {
    private val application = context.applicationContext
    private val preferences = application.getSharedPreferences("my_profile", Context.MODE_PRIVATE)
    private val transactions = TransactionsRepository(messageDatabase.getDatabase(application))

    fun observeTransactions() = transactions.observeAll()

    suspend fun loadProfile() = withContext(Dispatchers.IO) {
        MyProfile(preferences.getString("user_name", "记账用户") ?: "记账用户",
            preferences.getString("monthly_budget", "0.00") ?: "0.00")
    }

    suspend fun saveUserName(name: String) = save("user_name", name)
    suspend fun saveMonthlyBudget(budget: String) = save("monthly_budget", budget)

    private suspend fun save(key: String, value: String) = withContext(Dispatchers.IO) {
        check(preferences.edit().putString(key, value).commit()) { "设置保存失败，请重试" }
    }

    suspend fun exportLogs(target: Uri) { TallyLog.export(application, target) }
}
