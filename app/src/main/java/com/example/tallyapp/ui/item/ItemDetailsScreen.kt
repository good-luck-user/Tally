package com.example.tallyapp.ui.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tallyapp.data.ItemStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 页面入口连接 ViewModel，下面的 Screen 只展示状态。 */
@Composable
fun ItemDetailsRoute(viewModel: ItemDetailsViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(viewModel) { viewModel.loadMessages() }
    ItemDetailsScreen(state = state, onReload = viewModel::loadMessages)
}

@Composable
fun ItemDetailsScreen(
    state: ItemDetailsUiState,
    onReload: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "database_status") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("数据库消息", style = MaterialTheme.typography.titleLarge)
                Text("已加载 ${state.messages.size} 条消息，按通知时间从新到旧显示。")
                Text("通知通过后台队列保存后会自动显示，已保存的消息在重启后仍然保留。")
                Button(onClick = onReload, enabled = !state.isLoading) {
                    Text(if (state.isLoading) "正在加载…" else "重新加载")
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (!state.isLoading && state.error == null && state.messages.isEmpty()) {
                    Text("暂无已保存的消息。收到新通知并保存成功后，会显示在这里。")
                }
            }
        }
        items(state.messages, key = { it.id }) { message ->
            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    val appName = when (message.app) {
                        "com.tencent.mm" -> "微信"
                        "com.eg.android.AlipayGphone" -> "支付宝"
                        else -> message.app
                    }
                    Text(appName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(Date(message.time)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    SelectionContainer {
                        Text(message.message.ifBlank { "此通知没有可读取的文字内容。" })
                    }
                    val statusText = when (message.status) {
                        ItemStatus.PENDING -> "未处理"
                        ItemStatus.PROCESSED -> "已处理"
                        ItemStatus.IGNORED -> "已忽略"
                        ItemStatus.FAILED -> "处理失败"
                        else -> "未知状态"
                    }
                    Text("状态：$statusText（${message.status}）", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
