package com.example.tallyapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tallyapp.R
import com.example.tallyapp.ui.transactions.*

/** 页面共用底部栏，只展示选中状态和转发点击。 */
@Composable
fun AppBottomBar(selectedPage: Int, onSelectPage: (Int) -> Unit) {
    val items = listOf(
        "明细" to R.drawable.ic_receipt,
        "图表" to R.drawable.ic_chart_bar,
        "记账" to R.drawable.ic_plus,
        "我的" to R.drawable.ic_person,
    )
    Surface(color = Color.White) {
        Row(
            Modifier.fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .heightIn(min = 58.dp).selectableGroup(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { index, (title, icon) ->
                val selected = index == selectedPage
                Column(
                    Modifier.weight(1f).selectable(selected, role = Role.Tab, onClick = { onSelectPage(index) })
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(
                        Modifier.size(48.dp, 28.dp)
                            .background(if (selected) LedgerYellow else Color.Transparent, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(icon), null, Modifier.size(21.dp), tint = if (selected) LedgerInk else LedgerMuted)
                    }
                    Text(title, color = if (selected) LedgerInk else LedgerMuted, fontSize = 11.sp,
                        lineHeight = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }
    }
}
