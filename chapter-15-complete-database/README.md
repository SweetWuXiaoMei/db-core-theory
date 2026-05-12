# 第15章：完整数据库内核整合项目

## 章节定位

整合前14章所有知识，构建一个**完整的、可独立运行的数据库内核**。

## 项目特性

| 特性 | 实现 |
|------|------|
| 持久化存储 | 基于文件的页式存储 |
| B+树索引 | 加速主键查询 |
| SQL解析 | 支持 CREATE/INSERT/SELECT/DELETE |
| 事务 | BEGIN/COMMIT/ROLLBACK |
| 缓冲区 | LRU淘汰策略 |
| 日志 | Redo/Undo（WAL机制） |

## 项目结构

```
chapter-15-complete-database/ourdb/
├── pom.xml
└── src/main/java/com/ourdatabase/
    ├── OurDB.java              (主入口，REPL交互)
    ├── 存储引擎接口.java
    ├── 存储引擎.java            (文件持久化+页管理)
    ├── 缓冲池.java              (LRU缓存)
    ├── BPlus树索引.java         (完整B+树)
    ├── 词法分析器.java          (SQL→Token)
    ├── 语法分析器.java          (Token→AST)
    ├── 执行引擎.java            (AST→实际数据操作)
    ├── 事务管理器.java
    └── 日志管理器.java
```

## 运行步骤

```bash
cd chapter-15-complete-database/ourdb
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.OurDB"
```

## 交互演示

```
OurDB> CREATE TABLE student id name age
OurDB> INSERT INTO student VALUES (1, 张三, 20)
OurDB> INSERT INTO student VALUES (2, 李四, 22)
OurDB> SELECT * FROM student
OurDB> SELECT name FROM student WHERE id = 1
OurDB> BEGIN
OurDB> INSERT INTO student VALUES (3, 王五, 19)
OurDB> ROLLBACK
OurDB> SELECT * FROM student
```

## 整合总结

经过15章的学习，你从零开始，完整实现了：
1. 数据页管理（磁盘存储）
2. 缓冲区（内存优化）
3. B+树索引（快速查找）
4. SQL解析引擎（词法+语法）
5. 查询执行引擎
6. 事务管理（ACID）
7. 日志系统（崩溃恢复）

这就是一个数据库内核的完整骨架！
