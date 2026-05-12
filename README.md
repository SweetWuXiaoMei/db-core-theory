# 数据库内核开发 · 从零到一完整学习体系（Java实现）

## 适合人群

- 零基础小白：无数据库内核、无编译原理、无底层开发经验
- 仅需基础Java语法知识（会写HelloWorld、会用if/for、了解类和对象即可）
- 想深入理解MySQL/PostgreSQL等数据库底层原理的开发者

## 学习路线图

```
第1章  → 核心概念扫盲（什么是数据库内核？）
  ↓
第2章  → 第一个迷你数据库（极简原型，建立感性认识）
  ↓
第3章  → 文件IO基础（数据如何存到磁盘？）
  ↓
第4章  → 数据页管理（数据库的物理存储单元）
  ↓
第5章  → 缓冲区管理（内存缓存，性能核心）
  ↓
第6章  → B+树理论（索引的数学基础）
  ↓
第7章  → B+树Java实现（手写索引结构）
  ↓
第8章  → 词法分析理论（SQL如何被"读懂"？）
  ↓
第9章  → 词法解析器实现（将SQL拆成单词）
  ↓
第10章 → 语法分析器（理解SQL语句的含义）
  ↓
第11章 → 查询执行引擎（真正去取数据）
  ↓
第12章 → 事务管理/ACID（数据一致性的保障）
  ↓
第13章 → 日志系统Redo/Undo（崩溃恢复）
  ↓
第14章 → 并发控制/锁/MVCC（多用户同时访问）
  ↓
第15章 → 完整数据库内核整合项目（最终成品）
```

## 每一章包含什么？

| 内容 | 说明 |
|------|------|
| 详尽理论文档 | 概念拆解、原理详解、流程分析、常见误区 |
| Java实战Demo | 标准Maven项目，完整可运行代码 |
| 中文注释 | 每一行代码都有详细中文解释 |
| 运行指南 | 如何编译、如何运行、预期结果 |

## 技术栈

- **语言**：纯Java（JDK 17+）
- **构建工具**：Maven
- **依赖原则**：最小化依赖，优先使用JDK原生API
- **编码风格**：中文命名，通俗易懂

## 如何使用本项目？

### 1. 按顺序学习

每一章都依赖前一章的知识，请从第1章开始，不要跳章。

### 2. 动手运行Demo

每章demo目录下是一个独立的Maven项目，进入对应目录执行：

```bash
cd chapter-XX-xxxx/demo
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.xxx.Main"
```

### 3. 遇到问题？

- 先看章节文档中的"常见误区"部分
- 仔细阅读代码注释
- 尝试修改代码做实验

## 章节导航

| 章节 | 主题 | 核心收获 |
|------|------|----------|
| [第1章](./chapter-01-basic-concepts/README.md) | 核心概念入门 | 理解数据库内核的组成模块 |
| [第2章](./chapter-02-mini-database/README.md) | 极简数据库原型 | 手写第一个迷你数据库 |
| [第3章](./chapter-03-file-storage/README.md) | 文件存储基础 | 数据如何持久化到磁盘 |
| [第4章](./chapter-04-page-management/README.md) | 数据页管理 | 数据库的最小存储单元 |
| [第5章](./chapter-05-buffer-pool/README.md) | 缓冲区管理 | 内存缓存池的设计与实现 |
| [第6章](./chapter-06-bplus-tree-theory/README.md) | B+树理论 | 索引的底层数据结构 |
| [第7章](./chapter-07-bplus-tree-impl/README.md) | B+树实现 | 手写完整的B+树索引 |
| [第8章](./chapter-08-lexer-theory/README.md) | 词法分析理论 | SQL如何被拆解成Token |
| [第9章](./chapter-09-lexer-impl/README.md) | 词法解析器 | Java实现SQL分词器 |
| [第10章](./chapter-10-parser/README.md) | 语法分析器 | 构建SQL的抽象语法树 |
| [第11章](./chapter-11-executor/README.md) | 查询执行引擎 | 执行计划与实际数据检索 |
| [第12章](./chapter-12-transaction/README.md) | 事务管理 | ACID特性与实现原理 |
| [第13章](./chapter-13-log/README.md) | 日志系统 | Redo/Undo日志与崩溃恢复 |
| [第14章](./chapter-14-concurrency/README.md) | 并发控制 | 锁机制与MVCC多版本控制 |
| [第15章](./chapter-15-complete-database/README.md) | 完整数据库内核 | 整合所有模块的最终成品 |

---

开始你的数据库内核之旅吧！从第1章出发 🚀
