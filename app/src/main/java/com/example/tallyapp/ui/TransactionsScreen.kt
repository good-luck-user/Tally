package com.example.tallyapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tallyapp.R

enum class TabItem {
    DETAILS,
    ADD_RECORD
}

@Composable
fun TransactionsScreen() {
    var selectedTab by remember { mutableStateOf(TabItem.DETAILS) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0D12))
            .padding(16.dp)
    ) {
        // 顶部标题
        Text(
            text = "我的账本",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Tab 切换栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabButton(
                iconRes = R.drawable.ic_receipt,
                label = "明细",
                isSelected = selectedTab == TabItem.DETAILS,
                onClick = { selectedTab = TabItem.DETAILS },
                modifier = Modifier.weight(1f)
            )

            TabButton(
                iconRes = R.drawable.ic_edit,
                label = "记账",
                isSelected = selectedTab == TabItem.ADD_RECORD,
                onClick = { selectedTab = TabItem.ADD_RECORD },
                modifier = Modifier.weight(1f)
            )
        }

        // 内容区域
        when (selectedTab) {
            TabItem.DETAILS -> DetailsContent()
            TabItem.ADD_RECORD -> AddRecordContent()
        }
    }
}

@Composable
fun TabButton(
    iconRes: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E2636)
    val contentColor = if (isSelected) Color.White else Color(0xFF64748B)

    Surface(
        modifier = modifier
            .height(56.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor
            )
        }
    }
}

@Composable
fun DetailsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F131C), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_receipt),
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "交易明细",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // TODO: 添加交易列表
        Text(
            text = "暂无交易记录",
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )
    }
}

@Composable
fun AddRecordContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F131C), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_edit),
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "添加记账",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // TODO: 添加记账表单
        Text(
            text = "记账功能开发中...",
            fontSize = 14.sp,
            color = Color(0xFF64748B)
        )
    }
}
