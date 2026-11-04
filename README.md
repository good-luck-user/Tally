# Mytally

一个基于 Kotlin 和 Jetpack Compose 开发的 Android 记账应用原型，尝试通过读取微信、支付宝的系统通知辅助记账。

## 当前功能

- **基础界面**：提供“明细”和“记账”两个标签页，目前为占位内容。
- **通知监听**：用户授权后，监听微信、支付宝的新通知及更新，并以 JSON 格式输出到 Logcat。
- **金额解析**：独立的解析模块使用正则表达式提取部分支付通知中的金额，支持支付宝“X.XX元的支出”、微信“已扣费¥X.XX”和“已支付¥X.XX”。

目前金额解析尚未接入通知监听流程，交易列表、手动记账、自动入账和数据持久化仍待实现。项目暂未接入数据库或后端服务。

## 技术栈

| 技术 | 用途 |
| --- | --- |
| Kotlin | Android 应用开发语言 |
| Jetpack Compose | 声明式界面和页面状态管理 |
| Material 3 | 界面组件与主题 |
| AndroidX Activity、Core KTX、Lifecycle | Android 基础组件与 Kotlin 扩展 |
| NotificationListenerService | 读取用户授权范围内的系统通知 |
| Regex、BigDecimal | 匹配支付通知文本并表示金额 |
| Gradle Kotlin DSL | 项目构建与依赖管理 |
| JUnit 4、AndroidX Test、Espresso | 单元测试及 Android 测试支持 |

## 运行方式

1. 使用 Android Studio 打开项目，配置 Android SDK 并同步 Gradle。当前项目的 `compileSdk` 和 `targetSdk` 为 37，最低支持 Android 7.0（API 24）。
2. 将 `app` 安装到手机或模拟器上。
3. 首次启动时，在系统设置中为 **Mytally 通知记账调试** 开启通知使用权，然后重新打开应用。
4. 在 Android Studio 的 Logcat 中使用 `tag:MytallyNotification` 筛选，查看后续微信或支付宝通知的输出。

在项目根目录下构建 Debug APK（Windows）：

```powershell
.\gradlew.bat :app:assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`。

## 代码结构

```text
app/src/main/java/com/example/tallyapp/
├── MainActivity.kt       # 应用入口与通知权限检查
├── ui/                  # Compose 页面与主题
├── notification/        # 通知监听、通知数据和日志输出
└── data/                # 支付通知金额解析
```

## 使用说明

通知监听依赖系统授权及微信、支付宝实际发出的通知，无法读取应用内的历史账单。当前日志也可能包含聊天通知内容，分享日志前请删除个人信息；无需监听时可在系统设置中关闭通知使用权。
