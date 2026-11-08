package com.example.tallyapp.ui.my

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.tallyapp.ui.transactions.LedgerBackground
import com.example.tallyapp.ui.transactions.LedgerInk
import com.example.tallyapp.ui.transactions.LedgerMuted
import com.example.tallyapp.ui.transactions.LedgerYellow

/**
 * "我的"页面路由
 */
@Composable
fun MyRoute(
    bottomBar: @Composable () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogs: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val exportLogs = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        uri?.let(viewModel::exportLogs)
    }

    LaunchedEffect(viewModel) {
        viewModel.loadUserData()
    }

    MaterialTheme(colorScheme = lightColorScheme(primary = LedgerInk, onPrimary = Color.White,
        surface = Color.White, onSurface = LedgerInk, background = LedgerBackground,
        onBackground = LedgerInk, secondaryContainer = LedgerYellow)) {
        MyScreen(
            uiState = uiState,
            onRefresh = viewModel::loadUserData,
            onClearError = viewModel::clearError,
            onEdit = viewModel::startEditing,
            onEditValueChange = viewModel::updateEditValue,
            onSaveEdit = viewModel::saveEditing,
            onCancelEdit = viewModel::cancelEditing,
            onShowAbout = viewModel::showAbout,
            onShowMessage = viewModel::showMessage,
            onOpenSettings = onOpenSettings,
            onOpenLogs = onOpenLogs,
            onExportLogs = { exportLogs.launch("tally-logs.txt") },
            bottomBar = bottomBar,
            modifier = modifier,
        )
    }
}

/**
 * "我的"页面 UI
 */
@Composable
fun MyScreen(
    uiState: MyUiState,
    onRefresh: () -> Unit,
    onClearError: () -> Unit,
    onEdit: (MyEditField) -> Unit,
    onEditValueChange: (String) -> Unit,
    onSaveEdit: () -> Unit,
    onCancelEdit: () -> Unit,
    onShowAbout: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLogs: () -> Unit,
    onExportLogs: () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHost = remember { SnackbarHostState() }

    // 显示错误信息
    LaunchedEffect(uiState.error, uiState.message, uiState.editing) {
        (uiState.error ?: uiState.message)?.takeIf { uiState.editing == null }?.let { message ->
            snackbarHost.showSnackbar(message)
            onClearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = LedgerBackground,
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题栏
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "我的",
                            fontSize = 27.sp,
                            lineHeight = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = LedgerInk
                        )
                        Text(
                            "个人中心与设置",
                            fontSize = 12.sp,
                            color = LedgerMuted
                        )
                    }
                    TextButton(onClick = onRefresh, enabled = !uiState.loading) { Text("刷新", color = LedgerInk) }
                }
            }

            if (uiState.loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }

            // 用户信息卡片
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onEdit(MyEditField.USER_NAME) }
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 用户头像
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(LedgerYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.userName.take(1),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = LedgerInk
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        // 用户信息
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = uiState.userName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = LedgerInk
                            )
                            Text(
                                text = "点击编辑个人信息",
                                fontSize = 13.sp,
                                color = LedgerMuted
                            )
                        }

                        // 箭头图标（使用文本代替）
                        Text(
                            text = "›",
                            fontSize = 28.sp,
                            color = LedgerMuted
                        )
                    }
                }
            }

            // 数据统计卡片
            item {
                Surface(
                    color = LedgerYellow,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth().clickable { onEdit(MyEditField.MONTHLY_BUDGET) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "本月预算（元） · 点击设置",
                            fontSize = 13.sp,
                            color = LedgerInk
                        )
                        Text(
                            text = "¥ ${uiState.monthlyBudget}",
                            fontSize = 38.sp,
                            lineHeight = 46.sp,
                            fontWeight = FontWeight.Bold,
                            color = LedgerInk
                        )
                        HorizontalDivider(color = LedgerInk.copy(alpha = 0.12f))

                        Row(Modifier.fillMaxWidth()) {
                            DataItem(
                                label = "总支出",
                                value = uiState.totalExpense,
                                modifier = Modifier.weight(1f)
                            )
                            DataItem(
                                label = "总收入",
                                value = uiState.totalIncome,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text("已记录 ${uiState.transactionCount} 笔账单", fontSize = 12.sp, color = LedgerInk)
                    }
                }
            }

            // 账本信息
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "账本管理",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LedgerInk,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        SettingItem(
                            title = "我的账本",
                            value = "${uiState.accountCount} 个",
                            onClick = { onShowMessage("当前使用默认账本，多账本功能暂未开放") }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = LedgerBackground
                        )

                        SettingItem(
                            title = "已用分类",
                            value = "${uiState.categoryCount} 个",
                            onClick = { onShowMessage("可在明细中打开账单，编辑该笔账单的分类") }
                        )
                    }
                }
            }

            // 设置选项
            item {
                Surface(
                    color = Color.White,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "设置",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LedgerInk,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        SettingItem(
                            title = "通知设置",
                            onClick = onOpenSettings
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = LedgerBackground
                        )

                        SettingItem(
                            title = "日志查看",
                            onClick = onOpenLogs
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = LedgerBackground
                        )

                        SettingItem(
                            title = if (uiState.exporting) "日志导出中…" else "导出日志",
                            onClick = { if (!uiState.exporting) onExportLogs() }
                        )
                        HorizontalDivider(Modifier.padding(vertical = 12.dp), color = LedgerBackground)
                        SettingItem(
                            title = "关于应用",
                            onClick = { onShowAbout(true) }
                        )
                    }
                }
            }

            item {
                Text("数据保存在当前设备，建议定期备份。", color = LedgerMuted, fontSize = 11.sp)
            }
        }
    }
    uiState.editing?.let { field ->
        AlertDialog(
            onDismissRequest = onCancelEdit,
            title = { Text(if (field == MyEditField.USER_NAME) "编辑用户名" else "设置月预算") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.editValue, onValueChange = onEditValueChange,
                        enabled = !uiState.saving, singleLine = true,
                        label = { Text(if (field == MyEditField.USER_NAME) "用户名" else "预算（元）") },
                        keyboardOptions = KeyboardOptions(keyboardType = if (field == MyEditField.USER_NAME) KeyboardType.Text else KeyboardType.Decimal),
                    )
                    uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(onClick = onSaveEdit, enabled = !uiState.saving) { Text(if (uiState.saving) "保存中…" else "保存") } },
            dismissButton = { TextButton(onClick = onCancelEdit, enabled = !uiState.saving) { Text("取消") } },
        )
    }
    if (uiState.showAbout) AlertDialog(
        onDismissRequest = { onShowAbout(false) },
        title = { Text("TallyApp") },
        text = { Text("每一笔，都心中有数。\n\n账单保存在当前设备，支持微信、支付宝通知识别与手动修改。") },
        confirmButton = { TextButton(onClick = { onShowAbout(false) }) { Text("知道了") } },
    )
}

/**
 * 数据项组件
 */
@Composable
private fun DataItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = LedgerInk
        )
        Text(
            text = value,
            fontSize = 23.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.SemiBold,
            color = LedgerInk
        )
    }
}

/**
 * 设置项组件
 */
@Composable
private fun SettingItem(
    title: String,
    value: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            color = LedgerInk
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = LedgerMuted
                )
            }
            Text(
                text = "›",
                fontSize = 24.sp,
                color = LedgerMuted
            )
        }
    }
}
