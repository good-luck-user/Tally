package com.example.tallyapp.data

import com.example.tallyapp.notification.ReceivedNotification
import java.math.BigDecimal

/**
 * 解析支付宝和微信的支付通知，提取金额。
 * 仅支持当前已知的三种格式：
 * 1. 支付宝：你有一笔X.XX元的支出
 * 2. 微信：已扣费¥X.XX
 * 3. 微信：已支付¥X.XX
 */
object PaymentParser {
    private val ALIPAY_PATTERN = Regex("""(\d+(?:\.\d+)?)元的支出""")
    private val WECHAT_DEDUCT_PATTERN = Regex("""已扣费¥(\d+(?:\.\d+)?)""")
    private val WECHAT_PAID_PATTERN = Regex("""已支付¥(\d+(?:\.\d+)?)""")

    /**
     * 解析通知，返回支付金额（正数表示支出）。
     * @return 支付金额，如果无法解析则返回 null
     */
    fun parseAmount(notification: ReceivedNotification): BigDecimal? {
        val text = notification.text ?: return null

        return when (notification.packageName) {
            "com.eg.android.AlipayGphone" -> parseAlipay(text)
            "com.tencent.mm" -> parseWechat(text)
            else -> null
        }
    }

    private fun parseAlipay(text: String): BigDecimal? {
        val match = ALIPAY_PATTERN.find(text) ?: return null
        return match.groupValues[1].toBigDecimalOrNull()
    }

    private fun parseWechat(text: String): BigDecimal? {
        // 尝试"已扣费"模式
        WECHAT_DEDUCT_PATTERN.find(text)?.let {
            return it.groupValues[1].toBigDecimalOrNull()
        }

        // 尝试"已支付"模式
        WECHAT_PAID_PATTERN.find(text)?.let {
            return it.groupValues[1].toBigDecimalOrNull()
        }

        return null
    }
}
