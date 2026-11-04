package com.example.tallyapp.data

import com.example.tallyapp.notification.ReceivedNotification
import java.math.BigDecimal

/**
 * 验证 PaymentParser 对你提供的三种数据格式的解析能力
 */
fun main() {
    println("=== PaymentParser 验证 ===\n")

    // 测试数据
    val testCases = listOf(
        Triple(
            "支付宝 - 5.00元支出",
            "com.eg.android.AlipayGphone",
            "你有一笔5.00元的支出，领1元生活缴费红包。"
        ),
        Triple(
            "微信 - 扣费234.00",
            "com.tencent.mm",
            "已扣费¥234.00"
        ),
        Triple(
            "微信 - 已支付13.90",
            "com.tencent.mm",
            "已支付¥13.90"
        ),
        Triple(
            "微信 - 已支付15.00",
            "com.tencent.mm",
            "已支付¥15.00"
        ),
        Triple(
            "微信 - 已支付2.00",
            "com.tencent.mm",
            "已支付¥2.00"
        ),
        Triple(
            "微信 - 已支付3.50",
            "com.tencent.mm",
            "已支付¥3.50"
        )
    )

    var passed = 0
    var failed = 0

    testCases.forEach { (name, packageName, text) ->
        val notification = ReceivedNotification(
            key = "test_key",
            packageName = packageName,
            notificationId = 1,
            postedAtMillis = System.currentTimeMillis(),
            receivedAtMillis = System.currentTimeMillis(),
            title = if (packageName.contains("Alipay")) "交易提醒" else "微信支付",
            text = text,
            bigText = null,
            subText = null,
            textLines = emptyList(),
            messageTexts = emptyList(),
            isGroupSummary = false
        )

        val amount = PaymentParser.parseAmount(notification)
        if (amount != null) {
            println("✓ $name")
            println("  文本: $text")
            println("  金额: ¥$amount")
            println()
            passed++
        } else {
            println("✗ $name")
            println("  文本: $text")
            println("  错误: 解析失败")
            println()
            failed++
        }
    }

    println("===================")
    println("通过: $passed/${testCases.size}")
    println("失败: $failed/${testCases.size}")
}
