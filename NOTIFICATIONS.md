# 微信／支付宝通知：入队、原始消息保存与交易处理

## 使用

1. 运行应用，在“通知消息”页开启系统通知使用权。
2. 等待显示“通知监听已连接”。新的微信／支付宝通知或更新回调仅筛选包名，记录接收时间，把 StatusBarNotification 原始对象引用立即尝试入队。
3. 后台单消费者依次整理文字并写入 Room，初始状态为“未处理”。无需等待固定周期。
4. 点击“查看数据库消息”或顶部“数据库消息”查看已保存内容。该页面通过 ViewModel 订阅 Room Flow，保存后自动刷新，也支持重新加载。

## 数据流程与职责

```text
onNotificationPosted()
    → QueuedNotification(sbn, 接收时间)
    → NotificationQueue.tryEnqueue()：Channel 非阻塞入队
    → Dispatchers.IO 上的单消费者
    → ReceivedNotification.from(sbn, 接收时间)：后台转换快照
    → toItem()：合并标题、正文等文本
    → ItemsRepository → ItemDao → Room items（status = 0）
    → MessageProcessor 订阅未处理消息 → PaymentParser 本地识别
    → 同一数据库事务：去重写 transactions + 更新 items.status
    → ItemDetailsViewModel.uiState → ItemDetailsScreen
```

- 不再使用 NotificationHistory，不轮询 activeNotifications，也不在连接时扫描旧通知。仅处理系统实际交付的新通知／更新回调。
- 入队前不读取 extras、不提取文字、不解析聊天列表。队列持有 StatusBarNotification 引用，按只读方式使用；正文快照在后台创建。接收时间由回调记录，不能用后台开始处理的时间替代。
- 默认队列容量为 256（另有一条可能正在消费）。队列满或已关闭时 tryEnqueue 返回 false，服务记录 TallyApp 错误并在页面显示入队失败；不会阻塞回调或自动覆盖旧消息。
- 后台消费者先转换、整理，再按顺序保存。转换失败时报告错误并跳过该条，继续处理后续消息。数据库保存异常时保留已转换数据，每秒重试，不反复转换，不先消费后续消息；取消异常正常传播，不当作失败重试。
- 完整通知对象可能含图片或布局等较重数据，同样 256 条队列比纯文字快照更占内存。队列仍然有界，未成功入队和无法转换的消息会明确报告，不能保证不丢失。
- 尚未加入 WorkManager。队列是内存缓冲，服务销毁或进程结束时，尚未入库的消息可能丢失。已入库的未处理消息会在应用或通知服务下次启动时继续处理；进程结束期间不保证立即处理。
- 服务断连后已接收的队列继续消费；服务销毁时关闭队列并取消后台任务。重新连接不会创建多个消费者。
- NotificationRepository 仅保存监听和入库状态，使用 StateFlow.update 支持主线程与后台线程更新；不再保存通知列表。
- 原“清空缓存”和“立即读取”操作已移除，数据库记录不会因界面操作或撤销通知权限被清空。

## 数据库

沿用 Item 的 id、time、app、message、status 字段及 item_database 数据库。time 为系统通知发布时间，app 为来源包名，message 为整理后的可见文字。数据库升级到版本 3，将 status 从 TEXT 迁移为 INTEGER，保留旧消息、流水和自增 ID 高水位。

| items.status | 含义 |
| --- | --- |
| 0 | 未处理，后台待识别 |
| 1 | 已处理，交易已保存或已存在 |
| 2 | 已忽略，普通聊天或未完成交易等 |
| 3 | 处理失败，疑似交易但内容不明确或交易标识冲突 |

prepare/toItem 只初始化状态 0；后台处理器负责改变状态。失败原因目前输出到 TallyApp 日志，没有新增失败原因或重试次数字段。解析失败的状态 3 不自动重试；数据库异常会回滚事务并保留状态 0，后台稍后重试。

DAO 在事务内按 time、app、message 去重，重复回调不会重复入库，也不会覆盖已有处理状态。不同正文或时间会保存为不同记录。此规则用于通知去重，不等于支付交易去重。

## 后台识别与 MVVM

- MessageProcessor 属于 Model 层，使用 IO 协程订阅 Room 未处理消息，每批最多 100 条。应用和服务启动调用同一个启动入口，一个进程只启动一个后台消费者。ViewModel 提供页面状态，页面只展示数据。
- PaymentParser 支持明确的支付成功、收款到账、转账成功、退款成功通知，例如 `微信支付\n支付成功12.34元`。金额使用十进制定点转换为分，不使用浮点数。普通聊天忽略；多个候选金额、无效时间等不明确内容标记失败。这是基础模板规则，需要结合实际设备通知样本扩展，不能保证覆盖全部微信／支付宝通知。
- 优先读取通知里的完整交易时间；缺少时暂用通知发布时间。自动生成的 transactions.status 为 PENDING（待确认），与 items.status = 1（原始消息已处理）是不同概念。
- 有渠道交易号时按渠道、交易方向和交易号生成稳定流水 ID；退款使用退款单号。缺少交易号时按原始消息 ID 保证重复处理不重复入账，不按金额或相近时间强行合并不同消息。
- 写入交易与更新原始消息状态放在同一个 Room 事务中，任一步失败都回滚。原始消息保留，方便核对。通知文字只提供识别线索，不能作为支付真实性证明。

## 关键文件

- ui/notification/PaymentNotificationListener.kt：回调、快照、队列及生命周期。
- ui/notification/QueuedNotification.kt：原始通知引用和回调接收时间。
- ui/notification/NotificationQueue.kt：有界 Channel、单消费者、失败重试与取消。
- ui/notification/NotificationItemMapper.kt：后台整理文本并转换为 Item。
- data/ItemDao.kt：事务内去重保存。
- data/MessageProcessor.kt：后台订阅、交易去重和原子提交。
- data/PaymentParser.kt：金额、方向、渠道、时间的本地识别。
- data/messageDatabase.kt：旧数据库迁移。
- ui/notification/NotificationRepository.kt：监听和入库状态。
- ui/item/ItemDetailsViewModel.kt：订阅数据库并提供页面状态。
- ui/item/ItemDetailsScreen.kt：数据库消息展示。

## 验证

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

单元测试覆盖队列、消息转换及支付规则。专用模拟器执行 `:app:connectedDebugAndroidTest`，覆盖 Room 保存、版本 1/2 升级到版本 3、并发重复处理、交易号去重及冲突、事务失败回滚、重新打开数据库后跨批处理未完成消息。

APK：app/build/outputs/apk/debug/app-debug.apk。
