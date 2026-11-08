package com.example.tallyapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class PaymentParserTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val parser = PaymentParser(java.util.TimeZone.getTimeZone("Asia/Shanghai"))

    @Test
    fun expenseExtractsExactMinorAmountChannelAndExplicitTime() {
        val result = payment("微信支付\n支付成功\n支付金额：￥1,234.56\n交易单号：WX123456\n交易时间：2026-10-27 08:09:10")
        assertEquals(TransactionType.EXPENSE, result.type)
        assertEquals(123456L, result.amountMinor)
        assertEquals(PaymentChannel.WECHAT, result.channel)
        assertEquals("WX123456", result.externalId)
        assertEquals(LocalDateTime.of(2026, 10, 27, 8, 9, 10).atZone(zone).toInstant().toEpochMilli(), result.occurredAt)
    }

    @Test
    fun recognizesIncomeTransferAndRefundWithPositiveAmounts() {
        val cases = listOf(
            "支付宝\n收款到账 0.01元" to TransactionType.INCOME,
            "支付宝\n转账成功\n转账金额：10元" to TransactionType.TRANSFER,
            "支付宝\n退款成功\n退款金额：￥5.20\n退款单号：RF123456" to TransactionType.REFUND,
        )
        val results = cases.map { (text, type) ->
            payment(text, "com.eg.android.AlipayGphone").also {
                assertEquals(type, it.type)
                assertEquals(PaymentChannel.ALIPAY, it.channel)
                assertEquals(123L, it.occurredAt)
            }
        }
        assertEquals(listOf(1L, 1000L, 520L), results.map { it.amountMinor })
    }

    @Test
    fun ignoresChatUnfinishedPaymentsAndUnsupportedSources() {
        listOf("小明\n支付成功20元了吗？", "微信支付\n支付失败20元", "支付宝\n待支付20元", "微信支付\n验证码123456")
            .forEach { assertEquals(PaymentParseResult.Ignored, parser.parse(item(it))) }
        assertEquals(PaymentParseResult.Ignored, parser.parse(item("微信支付\n支付成功20元", "other.app")))
    }

    @Test
    fun explicitActualAmountWinsOverOrderAmountAndBalance() {
        assertEquals(1200L, payment("微信支付\n支付成功\n订单金额：20元\n实付金额：12元\n余额：200元").amountMinor)
    }

    @Test
    fun rejectsAmbiguousAmountsDirectionsAndInvalidValues() {
        listOf(
            "微信支付\n支付成功20元，余额100元",
            "微信支付\n支付成功20元，退款成功5元",
            "微信支付\n支付成功\n付款金额：1.234元",
            "微信支付\n支付成功￥0",
            "微信支付\n支付成功-20元",
            "微信支付\n支付成功￥9999999999999999999999999999",
            "微信支付\n支付成功20美元",
            "微信支付\n支付成功\n支付金额：不详\n余额200元",
            "微信支付\n支付成功20元\n交易时间：2026-02-30 12:00",
        ).forEach { assertTrue(it, parser.parse(item(it)) is PaymentParseResult.Failed) }
    }

    @Test
    fun parsesRealNotifications() {
        val alipay = payment("交易提醒\n你有一笔5.00元的支出，领1元生活缴费红包。", "com.eg.android.AlipayGphone")
        assertEquals(TransactionType.EXPENSE, alipay.type)
        assertEquals(500L, alipay.amountMinor)
        val wechat = payment("微信支付\n已扣费¥234.00")
        assertEquals(TransactionType.EXPENSE, wechat.type)
        assertEquals(23400L, wechat.amountMinor)
    }

    @Test
    fun refundDoesNotUseOriginalPaymentIdAsRefundIdentity() {
        assertEquals(null, payment("支付宝\n退款成功5元\n交易单号：ORIGINAL123", "com.eg.android.AlipayGphone").externalId)
    }

    private fun payment(text: String, app: String = "com.tencent.mm") =
        parser.parse(item(text, app)) as PaymentParseResult.Payment

    private fun item(text: String, app: String = "com.tencent.mm") =
        Item(id = 1, time = 123, app = app, message = text, status = ItemStatus.PENDING)
}
