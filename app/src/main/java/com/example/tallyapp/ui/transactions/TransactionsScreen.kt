package com.example.tallyapp.ui.transactions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.painterResource
import com.example.tallyapp.data.PaymentChannel
import com.example.tallyapp.R
import com.example.tallyapp.data.TransactionType
import com.example.tallyapp.data.TransactionStatus

internal val LedgerBackground = Color(0xFFF8F7F3)
internal val LedgerYellow = Color(0xFFFFE27A)
internal val LedgerInk = Color(0xFF302D27)
internal val LedgerMuted = Color(0xFF817E75)
internal val LedgerGreen = Color(0xFF478168)

@Composable
fun TransactionsRoute(bottomBar: @Composable () -> Unit, modifier: Modifier = Modifier, model: TransactionsViewModel = viewModel()) {
    val state by model.uiState.collectAsState()
    LaunchedEffect(model) { model.load() }
    val draft = state.draft
    MaterialTheme(colorScheme = lightColorScheme(primary = LedgerInk, onPrimary = Color.White,
        surface = Color.White, onSurface = LedgerInk, background = LedgerBackground,
        onBackground = LedgerInk, secondaryContainer = LedgerYellow)) {
        if (draft == null) {
            TransactionsScreen(state, model::open, model::moveMonth, model::load, bottomBar, modifier)
        } else {
            BackHandler { model.close() }
            TransactionDetailScreen(draft, model::edit, model::close, model::save, state.saving, state.error, modifier)
        }
    }
}

@Composable
fun TransactionsScreen(
    state: TransactionsUiState,
    onTransactionClick: (TransactionRow) -> Unit,
    onMonthChange: (Int) -> Unit,
    onReload: () -> Unit,
    bottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val snackbar = remember { SnackbarHostState() }
    val month = dateText(state.month, "yyyy-MM")
    val rows = state.rows.filter { dateText(it.record.createdAt, "yyyy-MM") == month }
        .sortedByDescending { it.record.createdAt }
    // 自动识别的账单初始为待确认，也参与月度汇总；仅排除作废和主动不计入统计的记录。
    val counted = rows.filter { !it.record.excludedFromStats && it.record.status != TransactionStatus.VOID }
    val expense = counted.filter { it.record.type == TransactionType.EXPENSE }.sumOf { it.record.amountMinor }
    val income = counted.filter { it.record.type == TransactionType.INCOME }.sumOf { it.record.amountMinor }
    Scaffold(
        modifier = modifier.fillMaxSize(), containerColor = LedgerBackground,
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = bottomBar,
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).consumeWindowInsets(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // 标题栏
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("收支明细", fontSize = 27.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, color = LedgerInk)
                        Text("每一笔，都心中有数", fontSize = 12.sp, color = LedgerMuted)
                    }
                    Text(
                        "${rows.size} 笔账单", color = LedgerMuted, fontSize = 12.sp,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            // 月份切换：药丸形，左边缘与标题自然对齐
            item {
                MonthSwitcher(month = month, onMonthChange = onMonthChange)
            }
            // 月度汇总卡片
            item {
                Surface(color = LedgerYellow, shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("本月支出（元）", Modifier.weight(1f), color = LedgerInk, fontSize = 13.sp)
                            Text(
                                "默认账本", color = LedgerInk, fontSize = 12.sp,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        Text("¥ ${money(expense)}", fontSize = 38.sp, lineHeight = 46.sp, fontWeight = FontWeight.Bold, color = LedgerInk)
                        HorizontalDivider(color = LedgerInk.copy(alpha = 0.12f))
                        Row(Modifier.fillMaxWidth()) {
                            SummaryAmount("本月收入", money(income), Modifier.weight(1f))
                            SummaryAmount("本月结余", money(income - expense), Modifier.weight(1f))
                        }
                    }
                }
            }
            if (state.loading || state.opening) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error ->
                item {
                    Column {
                        Text(error, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onReload) { Text("重新加载") }
                    }
                }
            }
            if (rows.isEmpty() && !state.loading && state.error == null) {
                item {
                    Text(
                        "这个月还没有账单",
                        Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        color = LedgerMuted,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            rows.groupBy { dateText(it.record.createdAt, "MM月dd日 EEEE") }.forEach { (day, records) ->
                item {
                    Row(Modifier.padding(top = 4.dp)) {
                        Text(day, Modifier.weight(1f), color = LedgerMuted, fontSize = 13.sp)
                        Text("${records.size} 笔", color = LedgerMuted, fontSize = 12.sp)
                    }
                }
                items(records, key = { it.record.id }) { row ->
                    TransactionItem(row, onClick = { onTransactionClick(row) })
                }
            }
            item {
                Text(
                    "按记录时间汇总，转账与退款单独展示",
                    color = LedgerMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun TransactionItem(row: TransactionRow, onClick: () -> Unit) {
    val isIncome = row.record.type == TransactionType.INCOME
    val incoming = isIncome || row.record.type == TransactionType.REFUND
    Surface(
        color = Color.White, shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PaymentChannelIcon(row.record.paymentChannel)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(row.category, fontWeight = FontWeight.SemiBold, color = LedgerInk)
                if (row.note.isNotBlank()) Text(row.note, fontSize = 12.sp, color = LedgerMuted)
                Text(
                    dateText(row.record.createdAt, "yyyy-MM-dd HH:mm"),
                    fontSize = 11.sp, color = LedgerMuted,
                )
            }
            Text(
                (if (incoming) "+" else "−") + money(row.record.amountMinor),
                fontWeight = FontWeight.SemiBold,
                color = if (incoming) LedgerGreen else LedgerInk,
            )
        }
    }
}

@Composable
private fun PaymentChannelIcon(channel: PaymentChannel) {
    val (background, icon) = when (channel) {
        PaymentChannel.WECHAT -> Color(0xFFE8F6ED) to R.drawable.ic_payment_wechat
        PaymentChannel.ALIPAY -> Color(0xFFE9F3FF) to R.drawable.ic_payment_alipay
        else -> Color(0xFFF2F0EB) to R.drawable.ic_receipt
    }
    Box(Modifier.size(44.dp).background(background, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
        Icon(painterResource(icon), contentDescription = channel.label(), tint = when (channel) {
            PaymentChannel.WECHAT -> Color(0xFF07B956)
            PaymentChannel.ALIPAY -> Color(0xFF1677FF)
            else -> LedgerMuted
        }, modifier = Modifier.size(27.dp))
    }
}

@Composable
private fun MonthSwitcher(month: String, onMonthChange: (Int) -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(24.dp)) {
        Row(
            Modifier.height(44.dp).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ChevronButton(forward = false) { onMonthChange(-1) }
            Text(
                text = month,
                modifier = Modifier.widthIn(min = 88.dp),
                textAlign = TextAlign.Center,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = LedgerInk,
            )
            ChevronButton(forward = true) { onMonthChange(1) }
        }
    }
}

@Composable
private fun ChevronButton(forward: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(width = 8.dp, height = 14.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                if (forward) {
                    moveTo(0f, 0f); lineTo(w, h / 2); lineTo(0f, h)
                } else {
                    moveTo(w, 0f); lineTo(0f, h / 2); lineTo(w, h)
                }
            }
            drawPath(
                path, LedgerInk,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}

@Composable
private fun SummaryAmount(label: String, value: String, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = LedgerInk, fontSize = 12.sp)
        Text(value, color = LedgerInk, fontSize = 23.sp, lineHeight = 30.sp, fontWeight = FontWeight.SemiBold)
    }
}
