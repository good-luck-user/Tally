# 数据备份和迁移方案

## 当前情况
- 旧应用包名：`com.example.unscramble`
- 新应用包名：`com.example.tallyapp`
- 问题：包名不同，无法覆盖安装

## 解决方案：数据库导出和导入

### 方案 1：使用 ADB 备份（推荐）

1. **备份旧应用数据**：
```bash
adb backup -f backup.ab com.example.unscramble
```

2. **卸载旧应用**

3. **安装新应用**

4. **恢复数据**：
```bash
adb restore backup.ab
```

### 方案 2：导出数据库文件

1. **找到数据库文件**：
```bash
adb shell run-as com.example.unscramble ls /data/data/com.example.unscramble/databases/
```

2. **导出数据库**：
```bash
adb shell run-as com.example.unscramble cat /data/data/com.example.unscramble/databases/message_database > message_database.db
```

3. **卸载旧应用，安装新应用**

4. **导入数据库**：
```bash
adb push message_database.db /sdcard/
adb shell run-as com.example.tallyapp cp /sdcard/message_database.db /data/data/com.example.tallyapp/databases/
```

### 方案 3：在应用内添加导出/导入功能（最安全）

添加一个导出按钮，将数据导出到外部存储（如 Downloads），然后在新应用中导入。

## 推荐做法

使用**方案 1 的 ADB 备份**，这是最安全的方式。
