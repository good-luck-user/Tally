package com.example.tallyapp.ui.notification

import com.example.tallyapp.data.Item
import com.example.tallyapp.data.ItemStatus

/** 按用户定义的消息表保存通知；使用发布时间保证重复读取时标识稳定。 */
internal fun ReceivedNotification.toItem(): Item = Item(
    time = postedAtMillis,
    app = packageName,
    message = buildList {
        title?.let(::add)
        bigText?.let(::add)
        text?.let(::add)
        addAll(textLines)
        addAll(messageTexts)
        subText?.let(::add)
    }.flatMap { it.lines() }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .distinct()
        .joinToString("\n"),
    status = ItemStatus.PENDING,
)
