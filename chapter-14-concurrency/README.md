# 第14章：并发控制·锁机制与MVCC入门

## 章节定位

学习数据库如何在多用户并发访问时保证数据正确性。

## 核心概念

### 锁（Lock）
- **共享锁（S锁/读锁）**：允许其他人读，不允许写
- **排他锁（X锁/写锁）**：不允许其他人读和写
- **行锁 vs 表锁**：锁定一行 vs 锁定整张表

### MVCC（多版本并发控制）
- 每个事务看到数据的"快照"版本
- 写操作不阻塞读操作
- 通过版本链（Undo日志）找到合适的版本

## Java实战Demo

演示锁机制和简单的MVCC。

### 运行步骤

```bash
cd chapter-14-concurrency/demo
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.concurrency.并发控制演示"
```
