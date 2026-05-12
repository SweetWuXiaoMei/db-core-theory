# 第13章：日志系统·Redo/Undo日志实现

## 章节定位

学习WAL（Write-Ahead Logging）机制，实现Redo日志（恢复已提交数据）和Undo日志（回滚未提交数据）。

## 核心概念

- **Redo日志**：记录"做了什么"，崩溃后用Redo重放来恢复已提交但未落盘的数据
- **Undo日志**：记录"修改前的旧值"，事务回滚时用Undo恢复旧数据
- **WAL原则**：先写日志，再写数据（日志必须先于数据落盘）

## Java实战Demo

实现Redo/Undo日志，演示崩溃恢复流程。

### 运行步骤

```bash
cd chapter-13-log/demo
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.log.日志系统演示"
```
