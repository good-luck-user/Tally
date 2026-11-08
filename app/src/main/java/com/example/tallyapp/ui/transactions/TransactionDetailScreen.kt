package com.example.tallyapp.ui.transactions

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tallyapp.data.*
import java.util.Calendar

/** 编辑更新草稿；完成后由 ViewModel 校验并通过 Repository 提交。 */
@Composable
fun TransactionDetailScreen(draft: TransactionDraft, onEdit: ((TransactionDraft) -> TransactionDraft) -> Unit, onBack: () -> Unit, onSave: () -> Unit, saving: Boolean, error: String?, modifier: Modifier = Modifier) {
    var notice by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    Scaffold(modifier.fillMaxSize(), containerColor = LedgerBackground, bottomBar = {
        Button(onClick = onSave, enabled = !saving, modifier = Modifier.navigationBarsPadding().padding(20.dp).fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = LedgerYellow, contentColor = LedgerInk)) { Text(if (saving) "保存中…" else "完成") }
    }) { padding ->
        Column(Modifier.padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack, enabled = !saving) { Text("‹ 返回", color = LedgerInk) }
                Text("账单详情", fontSize = 24.sp, color = LedgerInk)
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            DetailCard {
                Text("账单金额（元）", color = LedgerMuted)
                OutlinedTextField(draft.amount, { value -> onEdit { it.copy(amount = value) } }, prefix = { Text("¥") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TransactionType.entries.forEach { type -> FilterChip(draft.type == type, { onEdit { it.copy(type = type) } }, label = { Text(type.label(), fontSize = 12.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LedgerYellow)) }
                }
            }
            DetailCard {
                OutlinedTextField(draft.category, { value -> onEdit { it.copy(category = value) } }, label = { Text("分类") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("交易日期", Modifier.weight(1f))
                    TextButton(onClick = {
                        val calendar = Calendar.getInstance().apply { timeInMillis = draft.occurredAt }
                        DatePickerDialog(context, { _, year, month, day -> calendar.set(year, month, day); onEdit { it.copy(occurredAt = calendar.timeInMillis) } }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                    }) { Text(dateText(draft.occurredAt, "yyyy-MM-dd") + " ›", color = LedgerMuted) }
                }
                var expanded by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("支付渠道", Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { expanded = true }) { Text(draft.channel.label() + " ›", color = LedgerMuted) }
                        DropdownMenu(expanded, { expanded = false }) {
                            PaymentChannel.entries.forEach { channel -> DropdownMenuItem(text = { Text(channel.label()) }, onClick = { onEdit { it.copy(channel = channel) }; expanded = false }) }
                        }
                    }
                }
                Row { Text("账本", Modifier.weight(1f)); Text("默认账本", color = LedgerMuted) }
            }
            DetailCard {
                OutlinedTextField(draft.note, { value -> onEdit { it.copy(note = value) } }, label = { Text("备注") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("不计入统计", Modifier.weight(1f))
                    Switch(draft.excluded, { value -> onEdit { it.copy(excluded = value) } }, colors = SwitchDefaults.colors(checkedTrackColor = LedgerYellow, checkedThumbColor = LedgerInk))
                }
                Text("记录日期  " + dateText(draft.row.record.createdAt, "yyyy-MM-dd HH:mm"), color = LedgerMuted, fontSize = 12.sp)
            }
            OutlinedButton(onClick = { notice = "删除功能尚未接入，当前记录不会被删除。" }, modifier = Modifier.fillMaxWidth()) { Text("删除账单", color = LedgerMuted) }
            Text("点击完成保存修改；返回会放弃未保存的草稿。", color = LedgerMuted, fontSize = 11.sp)
        }
    }
    notice?.let { message -> AlertDialog(onDismissRequest = { notice = null }, title = { Text("提示") }, text = { Text(message) }, confirmButton = { TextButton(onClick = { notice = null }) { Text("知道了") } }) }
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp), content = content)
    }
}
