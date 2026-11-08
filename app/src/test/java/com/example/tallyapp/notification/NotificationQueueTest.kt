package com.example.tallyapp.notification

import com.example.tallyapp.ui.notification.NotificationQueue
import com.example.tallyapp.ui.notification.ReceivedNotification
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class NotificationQueueTest {
    @Test
    fun conversionRunsOnConsumerThreadAndCarriesRecordedReceiveTime() = runBlocking {
        data class Pending(val text: String, val receivedAtMillis: Long)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val callerThread = Thread.currentThread()
        val result = CompletableDeferred<Pair<ReceivedNotification, Boolean>>()
        var convertedOffCaller = false
        val queue = NotificationQueue<Pending, ReceivedNotification>(
            scope,
            prepare = {
                convertedOffCaller = Thread.currentThread() !== callerThread
                message(it.text).copy(receivedAtMillis = it.receivedAtMillis)
            },
            onPreparationFailure = { _, error -> result.completeExceptionally(error) },
            save = { result.complete(it to convertedOffCaller) },
            onFailure = { result.completeExceptionally(it) },
        )
        try {
            assertTrue(queue.tryEnqueue(Pending("原始通知", 123L)))
            val (snapshot, offCaller) = withTimeout(5_000) { result.await() }
            assertEquals("原始通知", snapshot.text)
            assertEquals(123L, snapshot.receivedAtMillis)
            assertTrue(offCaller)
        } finally {
            queue.close()
            scope.cancel()
        }
    }

    @Test
    fun malformedInputDoesNotBlockFollowingNotificationOrRetryConversion() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val completed = CompletableDeferred<Unit>()
        val conversions = mutableListOf<String>()
        val rejected = mutableListOf<String>()
        val attempts = mutableListOf<String>()
        val queue = NotificationQueue<String, String>(
            scope, retryDelayMillis = 1,
            prepare = {
                conversions.add(it)
                if (it == "bad") throw IllegalArgumentException("malformed notification")
                it.uppercase()
            },
            onPreparationFailure = { input, _ -> rejected.add(input) },
            save = {
                attempts.add(it)
                if (attempts.size == 1) throw IllegalStateException("temporary database failure")
                completed.complete(Unit)
            },
            onFailure = {},
        )
        try {
            assertTrue(queue.tryEnqueue("bad"))
            assertTrue(queue.tryEnqueue("good"))
            withTimeout(5_000) { completed.await() }
            assertEquals(listOf("bad", "good"), conversions)
            assertEquals(listOf("bad"), rejected)
            assertEquals(listOf("GOOD", "GOOD"), attempts)
        } finally {
            queue.close()
            scope.cancel()
        }
    }

    @Test
    fun burstPreservesOrderAndDoesNotDeduplicateBeforeSaving() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val saved = mutableListOf<String?>()
        val completed = CompletableDeferred<Unit>()
        val queue = NotificationQueue<ReceivedNotification, ReceivedNotification>(scope, prepare = { it }, onPreparationFailure = { _, error -> throw AssertionError(error) }, capacity = 128, save = {
            saved.add(it.text)
            if (saved.size == 100) completed.complete(Unit)
        }, onFailure = { completed.completeExceptionally(it) })
        try {
            repeat(100) { assertTrue(queue.tryEnqueue(message("${it / 2}"))) }
            withTimeout(5_000) { completed.await() }
            assertEquals((0 until 100).map { "${it / 2}" }, saved)
        } finally {
            queue.close()
            scope.cancel()
        }
    }

    @Test
    fun fullQueueReturnsFailureWithoutBlockingOrDroppingOlderMessages() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val completed = CompletableDeferred<Unit>()
        val saved = mutableListOf<String?>()
        val queue = NotificationQueue<ReceivedNotification, ReceivedNotification>(scope, prepare = { it }, onPreparationFailure = { _, error -> throw AssertionError(error) }, capacity = 1, save = {
            entered.complete(Unit)
            release.await()
            saved.add(it.text)
            if (saved.size == 2) completed.complete(Unit)
        }, onFailure = { completed.completeExceptionally(it) })
        try {
            assertTrue(queue.tryEnqueue(message("first")))
            withTimeout(5_000) { entered.await() }
            assertTrue(queue.tryEnqueue(message("second")))
            assertFalse(queue.tryEnqueue(message("overflow")))
            release.complete(Unit)
            withTimeout(5_000) { completed.await() }
            assertEquals(listOf("first", "second"), saved)
        } finally {
            queue.close()
            scope.cancel()
        }
    }

    @Test
    fun failedWriteRetriesSameMessageBeforeProcessingNextOne() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val completed = CompletableDeferred<Unit>()
        val attempts = mutableListOf<String?>()
        val failures = AtomicInteger()
        val queue = NotificationQueue<ReceivedNotification, ReceivedNotification>(scope, prepare = { it }, onPreparationFailure = { _, error -> throw AssertionError(error) }, retryDelayMillis = 1, save = {
            attempts.add(it.text)
            if (attempts.size == 1) throw IllegalStateException("database temporarily unavailable")
            if (it.text == "second") completed.complete(Unit)
        }, onFailure = { failures.incrementAndGet() })
        try {
            assertTrue(queue.tryEnqueue(message("first")))
            assertTrue(queue.tryEnqueue(message("second")))
            withTimeout(5_000) { completed.await() }
            assertEquals(listOf("first", "first", "second"), attempts)
            assertEquals(1, failures.get())
        } finally {
            queue.close()
            scope.cancel()
        }
    }

    @Test
    fun closeCancelsInFlightWriteWithoutRetryAndRejectsNewMessages() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val entered = CompletableDeferred<Unit>()
        val cancelled = CompletableDeferred<Unit>()
        val failures = AtomicInteger()
        val queue = NotificationQueue<ReceivedNotification, ReceivedNotification>(scope, prepare = { it }, onPreparationFailure = { _, error -> throw AssertionError(error) }, save = {
            entered.complete(Unit)
            try { awaitCancellation() } finally { cancelled.complete(Unit) }
        }, onFailure = { failures.incrementAndGet() })
        try {
            assertTrue(queue.tryEnqueue(message("first")))
            withTimeout(5_000) { entered.await() }
            queue.close()
            withTimeout(5_000) { cancelled.await() }
            assertFalse(queue.tryEnqueue(message("after close")))
            assertEquals(0, failures.get())
        } finally {
            queue.close()
            scope.cancel()
        }
    }

    private fun message(text: String) = ReceivedNotification(
        key = "same-key", packageName = "com.tencent.mm", notificationId = 1,
        postedAtMillis = 100, receivedAtMillis = 200, title = "微信", text = text,
        bigText = null, subText = null, textLines = emptyList(), messageTexts = emptyList(),
        isGroupSummary = false,
    )
}
