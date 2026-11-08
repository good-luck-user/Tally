package com.example.tallyapp.ui

import androidx.activity.compose.BackHandler
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tallyapp.ui.item.ItemDetailsRoute
import com.example.tallyapp.ui.my.MyRoute
import com.example.tallyapp.ui.transactions.TransactionsRoute
import com.example.tallyapp.ui.transactions.TransactionsViewModel

@Composable
fun AppScreen(state: AppUiState, onSelectTab: (Int) -> Unit, onSelectPage: (Int) -> Unit, onOpenSettings: () -> Unit, onReconnect: () -> Unit, modifier: Modifier = Modifier) {
    var tools by rememberSaveable { mutableStateOf(false) }
    val transactionsModel: TransactionsViewModel = viewModel()
    val context = LocalContext.current
    BackHandler(enabled = tools || state.selectedPage == 3) {
        if (tools) tools = false else onSelectPage(0)
    }
    val bottomBar: @Composable () -> Unit = {
        AppBottomBar(state.selectedPage) { index ->
            when (index) {
                0 -> {
                    if (state.selectedPage == 0) transactionsModel.load()
                    onSelectPage(0)
                }
                1 -> Toast.makeText(context, "图表页面暂未实现", Toast.LENGTH_SHORT).show()
                2 -> Toast.makeText(context, "记账页面暂未实现", Toast.LENGTH_SHORT).show()
                3 -> onSelectPage(3)
            }
        }
    }
    // 外层铺满窗口，明细和详情页由各自的 Scaffold 给内容留出系统栏安全距离。
    Column(modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            if (!tools) {
                if (state.selectedPage == 3) MyRoute(
                    bottomBar = bottomBar,
                    onOpenSettings = onOpenSettings,
                    onOpenLogs = {
                        onSelectTab(0)
                        tools = true
                    },
                )
                else TransactionsRoute(bottomBar = bottomBar, model = transactionsModel)
            }
            // 日志查看复用原通知工具页面，仅在此分支为内容保留安全距离。
            else Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    TextButton(onClick = { tools = false }) { Text("‹ 返回我的") }
                    Text("日志查看", style = MaterialTheme.typography.titleMedium)
                }
                TabRow(selectedTabIndex = if (state.selectedTab == 2) 1 else 0) {
                    Tab(state.selectedTab != 2, { onSelectTab(0) }, text = { Text("通知消息") })
                    Tab(state.selectedTab == 2, { onSelectTab(2) }, text = { Text("数据库消息") })
                }
                if (state.selectedTab == 2) ItemDetailsRoute()
                else NotificationScreen(state.notifications, state.hasNotificationAccess, onOpenSettings, onReconnect, { onSelectTab(2) })
            }
        }
    }
}
