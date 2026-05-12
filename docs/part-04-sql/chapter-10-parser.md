# 第10章：SQL语法分析·生成执行计划

## 章节定位

本章将Token列表进一步解析为**抽象语法树（AST）**，并生成初步的执行计划。

## 核心概念

语法分析把"单词的线性序列"变成"树形结构"。

```
输入: SELECT name FROM student WHERE id = 100

AST:
    SELECT
    ├── 列: [name]
    ├── 表: student
    └── WHERE
         └── id = 100
```

## Java实战Demo

实现一个递归下降解析器，支持SELECT/INSERT/DELETE/CREATE四种语句。

### 运行步骤

```bash
cd chapter-10-parser/demo
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.sql.语法分析演示"
```
