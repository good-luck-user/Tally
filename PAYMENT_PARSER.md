# PaymentParser 解析器文档

## 概述
`PaymentParser.kt` 是一个轻量级的支付通知解析器，专门解析支付宝和微信支付的通知消息，提取支付金额。

## 支持的格式

### 1. 支付宝支出通知
- **包名**: `com.eg.android.AlipayGphone`
- **标题**: 交易提醒
- **文本格式**: "你有一笔X.XX元的支出..."
- **示例**: "你有一笔5.00元的支出，领1元生活缴费红包。"
- **提取金额**: 5.00

### 2. 微信扣费通知
- **包名**: `com.tencent.mm`
- **标题**: 微信支付
- **文本格式**: "已扣费¥X.XX"
- **示例**: "已扣费¥234.00"
- **提取金额**: 234.00

### 3. 微信支付通知
- **包名**: `com.tencent.mm`
- **标题**: 微信支付
- **文本格式**: "已支付¥X.XX"
- **示例**: 
  - "已支付¥13.90" → 13.90
  - "已支付¥15.00" → 15.00
  - "已支付¥2.00" → 2.00
  - "已支付¥3.50" → 3.50

## API

### parseAmount(notification: ReceivedNotification): BigDecimal?
解析通知消息，返回支付金额。

**参数**:
- `notification`: 接收到的通知对象

**返回值**:
- 成功解析返回金额（BigDecimal），失败返回 null

**示例**:
```kotlin
val notification = ReceivedNotification(...)
val amount = PaymentParser.parseAmount(notification)
if (amount != null) {
    println("支付金额: ¥$amount")
}
```

## 实现细节

### 正则表达式模式
- **支付宝**: `(\d+(?:\.\d+)?)元的支出`
- **微信扣费**: `已扣费¥(\d+(?:\.\d+)?)`
- **微信已支付**: `已支付¥(\d+(?:\.\d+)?)`

### 设计原则
1. **单一职责**: 仅负责金额提取，不做其他处理
2. **无冗余**: 只包含必要的解析逻辑，没有多余代码
3. **类型安全**: 使用 BigDecimal 确保金额精度
4. **失败友好**: 解析失败返回 null，不抛异常

## 测试

测试文件位于 `app/src/test/java/com/example/mytally/data/PaymentParserTest.kt`，包含以下测试用例：
- 支付宝 5.00 元支出
- 微信扣费 234.00
- 微信已支付 13.90、15.00、2.00、3.50
- 未知包名处理
- 无法解析文本处理

运行测试:
```bash
./gradlew test --tests "com.example.mytally.data.PaymentParserTest"
```

## 验证工具

`PaymentParserDemo.kt` 提供了一个命令行验证工具，可以快速测试所有支持的格式。
