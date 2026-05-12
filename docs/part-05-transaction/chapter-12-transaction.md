# 第12章：事务管理基础·ACID原理

## 章节定位

学习事务的核心概念ACID和Java实现。

## 核心概念

- **原子性**：要么全做，要么全不做（通过Undo日志实现）
- **一致性**：事务前后数据满足约束（业务层保证）
- **隔离性**：事务之间互不干扰（锁/MVCC实现）
- **持久性**：提交后数据不丢失（Redo日志实现）

## Java实战Demo

模拟转账事务：A扣100元、B加100元，中间发生故障时回滚。

### 运行步骤

```bash
cd chapter-12-transaction/demo
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.transaction.事务管理演示"
```
