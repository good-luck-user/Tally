package com.example.tallyapp.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LogcatNotificationSinkTest {
    @Test
    fun longChineseNotificationAndEmojiSurviveSeparateLogEntryEncoding() {
        // Put a surrogate pair at the old 900-code-unit split boundary.
        val original = "x".repeat(899) + "💰".repeat(1800) + "\n\"微信支付：￥12.34，已收款\""
        val parts = splitForLogcat(original)
        val readFromLogcat = parts.joinToString("") {
            String(it.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
        }

        assertTrue(parts.size > 1)
        assertEquals(original, readFromLogcat)
        assertTrue(parts.all { it.toByteArray(Charsets.UTF_8).size < 4000 })
    }
}
