# 第11章：查询执行引擎·数据检索核心流程

## 章节定位

将第10章的AST转化为**实际的数据操作**，连接存储引擎和查询引擎。

## 核心概念

执行引擎拿到执行计划后，调用存储引擎的接口去真正读写数据。

```
AST/执行计划 → 执行引擎 → 调用存储引擎 → 读取数据页 → 返回结果
                        → 调用索引(B+树) → 定位数据
                        → 过滤/排序/聚合
```

## Java实战Demo

实现一个简单的查询执行器，能根据AST执行对应的数据操作。

### 运行步骤

```bash
cd chapter-11-executor/demo
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.executor.执行引擎演示"
```
