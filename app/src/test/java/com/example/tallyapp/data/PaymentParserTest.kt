package com.example.tallyapp.data

import com.example.tallyapp.notification.ReceivedNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class PaymentParserTest {
    @Test
    fun parseAlipay_expense() {
        val notification = createNotification(
            packageName = "com.eg.android.AlipayGphone",
            title = "交易提醒",
            text = "你有一笔5.00元的支出，领1元生活缴费红包。"
        )
        val amount = PaymentParser.parseAmount(notification)
        assertEquals(BigDecimal("5.00"), amount)
    }

    @Test
    fun parseWechat_deduct() {
        val notification = createNotification(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "已扣费¥234.00"
        )
        val amount = PaymentParser.parseAmount(notification)
        assertEquals(BigDecimal("234.00"), amount)
    }

    @Test
    fun parseWechat_paid_15() {
        val notification = createNotification(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "已支付¥15.00"
        )
        val amount = PaymentParser.parseAmount(notification)
        assertEquals(BigDecimal("15.00"), amount)
    }

    @Test
    fun parseWechat_paid_13_90() {
        val notification = createNotification(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "已支付¥13.90"
        )
        val amount = PaymentParser.parseAmount(notification)
        assertEquals(BigDecimal("13.90"), amount)
    }

    @Test
    fun parseWechat_paid_2() {
        val notification = createNotification(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "已支付¥2.00"
        )
        val amount = PaymentParser.parseAmount(notification)
        assertEquals(BigDecimal("2.00"), amount)
    }

    @Test
    fun parseWechat_paid_3_50() {
        val notification = createNotification(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "已支付¥3.50"
        )
        val amount = PaymentParser.parseAmount(notification)
        assertEquals(BigDecimal("3.50"), amount)
    }

    @Test
    fun parseUnknownPackage_returnsNull() {
        val notification = createNotification(
            packageName = "com.unknown.app",
            title = "Payment",
            text = "已支付¥100.00"
        )
        assertNull(PaymentParser.parseAmount(notification))
    }

    @Test
    fun parseNoAmount_returnsNull() {
        val notification = createNotification(
            packageName = "com.tencent.mm",
            title = "微信支付",
            text = "支付失败"
        )
        assertNull(PaymentParser.parseAmount(notification))
    }

    private fun createNotification(
        packageName: String,
        title: String,
        text: String
    ) = ReceivedNotification(
        key = "test_key",
        packageName = packageName,
        notificationId = 1,
        postedAtMillis = System.currentTimeMillis(),
        receivedAtMillis = System.currentTimeMillis(),
        title = title,
        text = text,
        bigText = null,
        subText = null,
        textLines = emptyList(),
        messageTexts = emptyList(),
        isGroupSummary = false
    )
}
