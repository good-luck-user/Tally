# TransactionsScreen 图标使用文档

## 已集成的 Tabler Icons

从 [Tabler Icons](https://tabler.io/icons) 精选并转换为 Android Vector Drawable 格式的图标：

### 1. ic_receipt.xml (收据/明细图标)
- **用途**: 交易明细、收据列表
- **位置**: `app/src/main/res/drawable/ic_receipt.xml`
- **效果**: 未选中时暗色 (#64748B)，选中时亮色 (White)

### 2. ic_edit.xml (编辑/记账图标)
- **用途**: 添加记账、编辑交易
- **位置**: `app/src/main/res/drawable/ic_edit.xml`
- **效果**: 未选中时暗色 (#64748B)，选中时亮色 (White)

### 3. ic_wallet.xml (钱包图标)
- **用途**: 账户管理、钱包功能
- **位置**: `app/src/main/res/drawable/ic_wallet.xml`
- **状态**: 已创建，待使用

### 4. ic_chart_line.xml (图表图标)
- **用途**: 统计分析、数据可视化
- **位置**: `app/src/main/res/drawable/ic_chart_line.xml`
- **状态**: 已创建，待使用

## UI 特性

### Tab 切换效果

- **未选中状态**:
  - 背景色: `#1E2636` (暗灰色)
  - 图标和文字: `#64748B` (暗色)
  - 字重: Normal

- **选中状态**:
  - 背景色: `#38BDF8` (亮青色)
  - 图标和文字: `White`
  - 字重: SemiBold

### 使用示例

```kotlin
TabButton(
    iconRes = R.drawable.ic_receipt,
    label = "明细",
    isSelected = selectedTab == TabItem.DETAILS,
    onClick = { selectedTab = TabItem.DETAILS },
    modifier = Modifier.weight(1f)
)
```

## 可扩展的图标

从 Tabler Icons 还可以添加的实用图标：

- **ic_list** - 列表视图
- **ic_currency_yuan** - 人民币符号
- **ic_calendar** - 日期选择
- **ic_filter** - 筛选功能
- **ic_settings** - 设置
- **ic_download** - 导出数据
- **ic_upload** - 导入数据
- **ic_search** - 搜索
- **ic_category** - 分类管理
- **ic_tag** - 标签

## 添加新图标的步骤

1. 访问 https://tabler.io/icons
2. 找到需要的图标
3. 复制 SVG 代码
4. 转换为 Android Vector Drawable 格式:
   - viewportWidth/Height = "24"
   - strokeWidth = "2"
   - strokeColor = "#000000"
   - fillColor = "#00000000"
   - strokeLineCap = "round"
   - strokeLineJoin = "round"
5. 保存到 `app/src/main/res/drawable/` 目录
6. 在代码中通过 `R.drawable.ic_name` 引用

## 主题颜色

- **主背景**: `#0A0D12`
- **卡片背景**: `#0F131C`
- **按钮未选中**: `#1E2636`
- **强调色**: `#38BDF8` (亮青色)
- **文字暗色**: `#64748B`
- **文字亮色**: `#FFFFFF`
