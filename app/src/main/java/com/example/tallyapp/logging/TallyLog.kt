package com.example.tallyapp.logging

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** 与 Log.i/e 用法一致：同时输出 Logcat 和本地文件，单线程按顺序落盘。 */
object TallyLog {
    private lateinit var directory: File
    private val writer = ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS,
        ArrayBlockingQueue<Runnable>(1024), { task -> Thread(task, "TallyLog").apply { isDaemon = true } })

    fun init(context: Context) {
        directory = File(context.applicationContext.filesDir, "logs")
        i("TallyApp", "本地日志已启用")
    }

    fun i(tag: String, message: String, error: Throwable? = null) = write(false, tag, message, error)
    fun e(tag: String, message: String, error: Throwable? = null) = write(true, tag, message, error)

    private fun write(isError: Boolean, tag: String, message: String, error: Throwable?) {
        if (isError) Log.e(tag, message, error) else Log.i(tag, message, error)
        if (!::directory.isInitialized) {
            Log.e("TallyApp", "本地日志尚未初始化，本条仅输出到 Logcat")
            return
        }
        val time = System.currentTimeMillis()
        try {
            writer.execute {
                try {
                    check(directory.isDirectory || directory.mkdirs()) { "无法创建日志目录" }
                    val day = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(time))
                    val file = File(directory, "tally-$day.log")
                    // 单日最多保留两个约 1 MB 的文件；保留最近 7 天。
                    if (file.length() >= 1_048_576) {
                        val previous = File(directory, "tally-$day.previous.log")
                        check(!previous.exists() || previous.delete()) { "无法清理旧日志" }
                        check(file.renameTo(previous)) { "无法轮转日志" }
                    }
                    directory.listFiles()?.filter { it.name.startsWith("tally-") && it.name.endsWith(".log") &&
                        it.lastModified() < time - TimeUnit.DAYS.toMillis(7) }?.forEach { it.delete() }
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.ROOT).format(Date(time))
                    file.appendText("$timestamp ${if (isError) "ERROR" else "INFO"} $tag: $message\n" +
                        (error?.let { Log.getStackTraceString(it) + "\n" } ?: ""), Charsets.UTF_8)
                } catch (failure: Exception) {
                    Log.e("TallyApp", "本地日志写入失败", failure)
                }
            }
        } catch (failure: RejectedExecutionException) {
            Log.e("TallyApp", "日志队列已满，本条未写入文件", failure)
        }
    }

    /** 与写日志共用一个队列，先完成已排队写入，再导出快照。 */
    suspend fun export(context: Context, target: Uri) = withContext(Dispatchers.IO) {
        writer.submit {
            val output = requireNotNull(context.contentResolver.openOutputStream(target)) { "无法打开导出文件" }
            output.bufferedWriter(Charsets.UTF_8).use { destination ->
                directory.listFiles()?.filter { it.name.endsWith(".log") }?.sortedBy { it.lastModified() }
                    ?.forEach { file ->
                        destination.appendLine("=== ${file.name} ===")
                        file.reader(Charsets.UTF_8).use { it.copyTo(destination) }
                    }
            }
        }.get()
    }
}
