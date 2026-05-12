# 🗄️ 数据库内核开发 · 从零到一完整学习体系

<div class="grid cards" markdown>

- :fontawesome-solid-graduation-cap: **零基础友好**{ .lg }

    ---

    只需基础 Java 语法知识（会写 HelloWorld、会用 if/for、了解类和对象即可），无需数据库内核或编译原理经验。

- :fontawesome-solid-code: **纯 Java 实现**{ .lg }

    ---

    JDK 17+，Maven 构建，最小化依赖，优先使用 JDK 原生 API。中文命名，通俗易懂。

- :fontawesome-solid-book-open: **15 章循序渐进**{ .lg }

    ---

    每章包含详尽理论文档 + Java 实战 Demo + 中文注释 + 运行指南，理论与实践紧密结合。

- :fontawesome-solid-rocket: **最终成品**{ .lg }

    ---

    第 15 章整合所有模块，构建一个完整的、可独立运行的数据库内核。

</div>

## 学习路线图

```mermaid
graph LR
    A[第1章<br/>核心概念] --> B[第2章<br/>迷你数据库]
    B --> C[第3章<br/>文件存储]
    C --> D[第4章<br/>数据页管理]
    D --> E[第5章<br/>缓冲区管理]
    E --> F[第6章<br/>B+树理论]
    F --> G[第7章<br/>B+树实现]
    G --> H[第8章<br/>词法分析]
    H --> I[第9章<br/>词法实现]
    I --> J[第10章<br/>语法分析]
    J --> K[第11章<br/>执行引擎]
    K --> L[第12章<br/>事务管理]
    L --> M[第13章<br/>日志系统]
    M --> N[第14章<br/>并发控制]
    N --> O[第15章<br/>完整数据库]
```

## 每一章包含什么？

| 内容 | 说明 |
|------|------|
| 详尽理论文档 | 概念拆解、原理详解、流程分析、常见误区 |
| Java 实战 Demo | 标准 Maven 项目，完整可运行代码 |
| 中文注释 | 每一行代码都有详细中文解释 |
| 运行指南 | 如何编译、如何运行、预期结果 |

## 技术栈

- **语言**：纯 Java（JDK 17+）
- **构建工具**：Maven
- **依赖原则**：最小化依赖，优先使用 JDK 原生 API
- **编码风格**：中文命名，通俗易懂

## 如何使用本项目？

### 1. 按顺序学习

每一章都依赖前一章的知识，请从第 1 章开始，不要跳章。

### 2. 动手运行 Demo

每章 demo 目录下是一个独立的 Maven 项目，进入对应目录执行：

```bash
cd chapter-XX-xxxx/demo
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.xxx.Main"
```

### 3. 遇到问题？

- 先看章节文档中的"常见误区"部分
- 仔细阅读代码注释
- 尝试修改代码做实验

---

开始你的数据库内核之旅吧！从第 1 章出发 🚀
