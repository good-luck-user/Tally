package com.example.tallyapp.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.tallyapp.ui.notification.NotificationSnapshot
import com.example.tallyapp.logging.TallyLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NotificationScreen(
    state: NotificationSnapshot,
    hasAccess: Boolean,
    onOpenSettings: () -> Unit,
    onReconnect: () -> Unit,
    onOpenMessages: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exporting by remember { mutableStateOf(false) }
    val exportLogs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) scope.launch {
            exporting = true
            try {
                TallyLog.export(context.applicationContext, uri)
                Toast.makeText(context, "日志已导出", Toast.LENGTH_SHORT).show()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                TallyLog.e("TallyApp", "导出日志失败", error)
                Toast.makeText(context, "导出失败，请重新选择保存位置", Toast.LENGTH_LONG).show()
            } finally {
                exporting = false
            }
        }
    }
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("微信 / 支付宝通知", style = MaterialTheme.typography.titleLarge)
        Text(when {
            !hasAccess -> "尚未开启通知使用权"
            state.connected -> "通知监听已连接"
            state.connecting -> "正在连接通知监听…"
            else -> "已授权，等待通知监听连接"
        })
        Text("收到新通知后立即加入队列，后台依次保存。已保存的内容可在“数据库消息”查看。")
        Text("只能读取系统实际提供的通知内容；未连接期间已消失的通知无法找回。")
        Button(onClick = onOpenSettings) {
            Text(if (hasAccess) "通知权限设置" else "开启通知使用权")
        }
        OutlinedButton(onClick = onReconnect, enabled = hasAccess && !state.connecting) {
            Text(if (state.connecting) "连接中…" else "重新连接")
        }
        Button(onClick = onOpenMessages) { Text("查看数据库消息") }
        OutlinedButton(onClick = { exportLogs.launch("tally-logs.txt") }, enabled = !exporting) {
            Text(if (exporting) "导出中…" else "导出日志")
        }
        Text("本次进程新增保存 ${state.savedCount} 条，跳过重复 ${state.duplicateCount} 条。")
        Text("最近保存：${state.lastSavedAtMillis?.let {
            SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it))
        } ?: "尚未保存"}")
        listOfNotNull(state.error, state.storageError, state.deliveryError).forEach {
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
