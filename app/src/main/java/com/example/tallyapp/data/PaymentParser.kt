package com.example.tallyapp.data

import java.math.BigDecimal
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

sealed interface PaymentParseResult {
    data class Payment(
        val type: TransactionType,
        val amountMinor: Long,
        val channel: PaymentChannel,
        val occurredAt: Long,
        val externalId: String?,
    ) : PaymentParseResult
    data object Ignored : PaymentParseResult
    data class Failed(val reason: String) : PaymentParseResult
}

/** 简单本地规则：只识别明确交易标题和完成语句，不从普通聊天中猜测交易。 */
class PaymentParser(private val timeZone: TimeZone = TimeZone.getDefault()) {
    fun parse(item: Item): PaymentParseResult {
        val channel = when (item.app) {
            "com.tencent.mm" -> PaymentChannel.WECHAT
            "com.eg.android.AlipayGphone" -> PaymentChannel.ALIPAY
            else -> return PaymentParseResult.Ignored
        }
        val text = item.message.trim()
        val title = text.lineSequence().firstOrNull().orEmpty()
        if (!transactionTitle.containsMatchIn(title)) return PaymentParseResult.Ignored
        if (notCompleted.containsMatchIn(text)) return PaymentParseResult.Ignored
        if (Regex("美元|港币|港元|日元|欧元|USD|HKD|EUR", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
            return PaymentParseResult.Failed("暂不支持外币金额")
        }
        val types = completionPatterns.filterValues { it.containsMatchIn(text) }.keys
        if (types.size != 1) return PaymentParseResult.Failed("交易方向不明确或通知包含多笔交易")
        val type = types.single()
        return try {
            val amount = amountInMinor(text, type)
                ?: return PaymentParseResult.Failed("金额缺失、格式不支持或存在多个候选金额")
            if (amount == 0L && text.contains("扣款支付")) return PaymentParseResult.Ignored
            if (amount <= 0) return PaymentParseResult.Failed("金额必须大于零")
            val occurredAt = if (timeLabel.containsMatchIn(text)) {
                val time = transactionTime.find(text)?.groupValues?.get(1)
                    ?: return PaymentParseResult.Failed("交易时间格式不支持")
                parseTime(time) ?: return PaymentParseResult.Failed("交易时间无效")
            } else {
                // 通知没有实际交易时间时暂用发布时间，生成的流水保持 PENDING 待确认。
                item.time
            }
            val idLabels = if (type == TransactionType.REFUND) "退款单号|退款流水号" else "交易单号|交易号|流水号"
            val externalId = Regex("(?:$idLabels)\\s*[:：]\\s*([A-Za-z0-9][A-Za-z0-9_-]{5,127})(?![A-Za-z0-9_-])")
                .find(text)?.groupValues?.get(1)
            PaymentParseResult.Payment(type, amount, channel, occurredAt, externalId)
        } catch (error: ArithmeticException) {
            PaymentParseResult.Failed("金额精度或范围不支持")
        }
    }

    private fun parseTime(text: String): Long? {
        val normalized = text.replace('/', '-').replace('T', ' ')
        val format = SimpleDateFormat(
            if (normalized.count { it == ':' } == 2) "yyyy-M-d H:m:s" else "yyyy-M-d H:m",
            Locale.ROOT,
        )
        format.timeZone = timeZone
        format.isLenient = false
        val position = ParsePosition(0)
        val date = format.parse(normalized, position)
        return date?.time?.takeIf { position.index == normalized.length }
    }

    private fun amountInMinor(text: String, type: TransactionType): Long? {
        val labels = when (type) {
            TransactionType.EXPENSE -> listOf("实付金额", "实付", "支付金额", "付款金额", "支出金额", "消费金额","已扣费")
            TransactionType.INCOME -> listOf("到账金额", "收款金额", "收入金额")
            TransactionType.TRANSFER -> listOf("转账金额", "转出金额")
            TransactionType.REFUND -> listOf("退款金额", "退回金额")
        }
        for (label in labels) {
            if (!text.contains(label)) continue
            val pattern = Regex("${Regex.escape(label)}\\s*[:：]?\\s*(?:人民币\\s*)?[￥¥]?\\s*($number)(?![\\d.,])")
            val amounts = pattern.findAll(text).map { minor(it.groupValues[1]) }.distinct().toList()
            return amounts.singleOrNull()
        }
        // 优先提取支出或扣款语句里的金额，避免红包、积分等推广数字干扰。
        if (type == TransactionType.EXPENSE) expenseSentence.find(text)?.let { return minor(it.groupValues[1]) }
        val amounts = currencyAmount.findAll(text).map {
            minor(it.groupValues[1].ifEmpty { it.groupValues[2] })
        }.distinct().toList()
        return amounts.singleOrNull()
    }

    private fun minor(text: String): Long = BigDecimal(text.replace(",", ""))
        .movePointRight(2).longValueExact()

    private companion object {
        val transactionTitle = Regex("^(微信支付|支付宝|支付助手|收款通知|交易提醒|支付成功|付款成功|收款到账|退款成功|转账成功)")
        val notCompleted = Regex("待支付|等待付款|待付款|支付失败|付款失败|交易失败|退款失败|转账失败|待领取|未领取|验证码|付款请求|支付请求|记账日报")
        val completionPatterns = linkedMapOf(
            TransactionType.EXPENSE to Regex("支付成功|付款成功|成功支付|成功付款|消费成功|已支付|已付款|已扣费|免密/自动扣款支付|自动扣款支付|免密扣款支付|(?<!转账)支出"),
            TransactionType.INCOME to Regex("收款到账|收款成功|已收款|转账到账|收入|微信支付收款\\s*[￥¥]?\\d"),
            TransactionType.TRANSFER to Regex("转账成功|已转账|转账支出|转出成功"),
            TransactionType.REFUND to Regex("退款成功|退款到账|退款已到账|已退款|退款已退回"),
        )
        const val number = "(?:[1-9]\\d{0,2}(?:,\\d{3})+|\\d+)(?:\\.\\d{1,2})?"
        val currencyAmount = Regex("(?:[￥¥]\\s*($number)(?![\\d.,])|(?<![\\d.,+\\-])($number)\\s*元)")
        val expenseSentence = Regex("(?<![\\d.,])($number)\\s*元的(?:支出|免密/自动扣款支付|自动扣款支付|免密扣款支付)")
        val timeLabel = Regex("(?:交易|支付|付款|收款|到账|转账|退款)时间")
        val transactionTime = Regex("(?:交易|支付|付款|收款|到账|转账|退款)时间\\s*[:：]?\\s*(\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}[ T]\\d{1,2}:\\d{1,2}(?::\\d{1,2})?)(?![:\\d])")
    }
}
