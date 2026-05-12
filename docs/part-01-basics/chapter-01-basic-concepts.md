# 第1章：数据库内核核心概念入门

## 章节定位

### 本章学什么？

本章是**零基础扫盲章节**。你将学习：

1. **数据库和数据库内核的区别** —— 很多人搞混这两个概念
2. **数据库内核的五大核心模块** —— 存储引擎、查询引擎、事务管理器、日志系统、并发控制
3. **一条SQL语句在数据库内部经历了什么** —— 从输入到返回结果的完整旅程
4. **为什么学习数据库内核** —— 对你职业发展的实际价值

### 能解决什么问题？

学完本章后，你会拥有数据库内核的**全局地图**。后续每一章深入学习某个模块时，你都能知道"这个模块在地图上的哪里"，不会迷失方向。

### 和后续章节的关联

```
本章(全景地图) → 第2章(迷你原型，跑通全流程)
              → 第3-5章(存储模块深入)
              → 第6-7章(索引模块深入)
              → 第8-11章(查询模块深入)
              → 第12-14章(事务/日志/并发深入)
              → 第15章(全部整合)
```

---

## 详尽理论

### 一、概念解释

#### 1.1 什么是数据库？（Database）

**数据库**就是一个**有组织的数据仓库**。

想象你有一个Excel表格，里面存了100条学生信息（学号、姓名、成绩）。这个Excel文件就是一个最简单的"数据库"——它存储了数据。

但Excel有个问题：当你和同事同时编辑同一个文件时，数据会乱掉。而且当数据量达到100万条时，Excel打开都费劲。

**专业数据库**（如MySQL、PostgreSQL）解决了这些问题：
- 支持多人同时读写
- 数据量可以非常大（TB级别甚至更多）
- 提供专门的查询语言（SQL）来操作数据
- 保证数据不会丢失（即使断电）

> **关键理解**：数据库 = 数据的仓库。你存入数据，查询数据，删除数据。就这么简单。

#### 1.2 什么是数据库内核？（Database Kernel / Database Engine）

**数据库内核**是数据库软件的**核心引擎**。

打个比方：
- **数据库**（如MySQL）= 一整辆车
- **数据库内核**（如InnoDB）= 车的发动机

你开车时，踩油门、打方向盘，这些操作最终都由发动机来执行。同样，你写一条SQL语句 `SELECT * FROM student WHERE age > 18`，这条语句最终由数据库内核去执行——它要找到数据存在磁盘的哪个位置、读出来、过滤、返回给你。

> **关键理解**：数据库内核是"看不见的手"。你平时用数据库时感觉不到它的存在，但没有它，数据库就是个空壳。

#### 1.3 数据库内核的五大核心模块

把数据库内核想象成一个工厂，这个工厂有五个核心车间：

```
                           ┌──────────────────┐
    SQL语句 ──────────────►│   查询引擎        │  "理解SQL并制定执行计划"
                           │   (Query Engine)  │
                           └────────┬─────────┘
                                    │ 执行计划
                                    ▼
┌──────────────┐    ┌──────────────────────────┐    ┌──────────────┐
│  事务管理器   │◄──►│       存储引擎            │◄──►│   日志系统    │
│(Transaction) │    │   (Storage Engine)       │    │   (Logging)  │
│              │    │                          │    │              │
│ "保证一组操   │    │ "管理数据在磁盘上的       │    │ "记录所有操   │
│  作要么全成   │    │  存储和内存中的缓存"      │    │  作，崩溃恢   │
│  功要么全失   │    │                          │    │  复用"        │
│  败"         │    │                          │    │              │
└──────────────┘    └──────────────────────────┘    └──────────────┘
                                    │
                            ┌───────┴───────┐
                            │   并发控制     │
                            │ (Concurrency) │
                            │               │
                            │ "多用户同时操  │
                            │  作时保证数据  │
                            │  不错乱"       │
                            └───────────────┘
```

##### 模块1：存储引擎（Storage Engine）—— "数据存在哪里、怎么存、怎么取"

**做什么的？**
- 把数据写到磁盘文件里（持久化）
- 从磁盘文件里把数据读出来
- 管理内存缓存（Buffer Pool），让常用数据留在内存里，访问更快

**生活类比**：图书馆的书架系统
- 新书到馆 → 存储引擎决定放在哪个书架的哪个位置（写数据）
- 读者要借某本书 → 存储引擎快速找到它（读数据）
- 热门书放在前台展示架 → 类比内存缓存（Buffer Pool）

**为什么重要？**
- 磁盘IO（读写硬盘）是数据库最慢的操作
- 存储引擎的设计直接决定了数据库的性能上限

##### 模块2：查询引擎（Query Engine）—— "理解SQL语句，制定执行计划"

**做什么的？**
- 把你写的SQL字符串拆成有意义的单词（词法分析）
- 理解这些单词组成的句子是什么意思（语法分析）
- 制定最优的"执行计划"（先做什么、后做什么）
- 真正去执行这个计划，拿到结果

**生活类比**：餐厅的点餐流程
- 你说"来一份宫保鸡丁，不要花生" → 这是"SQL语句"
- 服务员理解你的需求 → 词法分析 + 语法分析
- 服务员决定"先通知厨房，再通知配菜" → 执行计划
- 厨房做菜 → 执行引擎

**查询引擎的三个子模块：**

| 子模块 | 做什么 | 输入 | 输出 |
|--------|--------|------|------|
| 词法分析器（Lexer） | 把SQL字符串拆成Token（单词） | `"SELECT * FROM student"` | `[SELECT, *, FROM, student]` |
| 语法分析器（Parser） | 理解Token之间的关系，构建语法树 | Token列表 | 抽象语法树（AST） |
| 执行器（Executor） | 按照语法树真正去操作数据 | 语法树 | 查询结果 |

##### 模块3：事务管理器（Transaction Manager）—— "一组操作，要么全成功，要么全失败"

**做什么的？**
- 管理事务的开始、提交、回滚
- 保证ACID特性（原子性、一致性、隔离性、持久性）

**生活类比**：银行转账
- 张三给李四转100元
- 步骤1：张三账户 -100
- 步骤2：李四账户 +100
- 这必须是**一个事务**：要么两步都成功，要么都失败
- 如果步骤1成功但步骤2失败（银行断电了），张三的钱就凭空消失了！这是绝对不允许的

**ACID四个字母的含义：**

| 特性 | 含义 | 生活类比 |
|------|------|----------|
| 原子性（Atomicity） | 事务是不可分割的最小单位 | 转账：扣钱和加钱必须绑定 |
| 一致性（Consistency） | 事务前后数据都满足业务规则 | 转账前后，总金额不变 |
| 隔离性（Isolation） | 多个事务同时执行互不干扰 | 你转账时，别人查余额不受影响 |
| 持久性（Durability） | 事务提交后，数据永久保存 | 转账成功后，银行断电数据也不丢 |

##### 模块4：日志系统（Logging System）—— "记录所有操作，用于崩溃恢复"

**做什么的？**
- 记录数据库的每一次修改操作（写日志）
- 如果数据库崩溃了，根据日志恢复数据（恢复）

**两种核心日志：**

| 日志类型 | 记录什么 | 用途 |
|----------|----------|------|
| Redo日志（重做日志） | "做了什么修改" | 崩溃后重放操作，恢复已提交的数据 |
| Undo日志（回滚日志） | "修改前的旧值" | 事务回滚时恢复旧值 |

**生活类比**：建筑工地的施工日志
- 每天都在日志上记录"今天砌了哪堵墙"（Redo日志）
- 如果地震了，根据日志重建（崩溃恢复）
- 如果发现砌错了，看日志知道原来的样子（Undo日志）

##### 模块5：并发控制（Concurrency Control）—— "多个人同时操作，数据不能乱"

**做什么的？**
- 当多个用户同时读写数据时，保证数据正确性
- 核心手段：锁（Lock）和多版本并发控制（MVCC）

**生活类比**：公共厕所的坑位
- 一个人进去，锁门（加锁）→ 用完出来，开门（释放锁）
- 后面的人排队等待 → 这就是并发控制

**两种核心机制：**

| 机制 | 原理 | 优点 | 缺点 |
|------|------|------|------|
| 锁（Lock） | 操作数据前先"上锁"，别人不能动 | 实现简单 | 并发性能差（大家排队等） |
| MVCC | 每个事务看到数据的"快照版本" | 读写不冲突，并发性能好 | 实现复杂，需要更多存储空间 |

---

### 二、原理详解：一条SQL语句的完整旅程

这是本章最重要的部分。我们追踪一条最简单的SQL语句，看看它在数据库内核中经历了哪些步骤。

**SQL语句**：
```sql
SELECT name FROM student WHERE id = 100;
```

**旅程开始：**

```
步骤1: 用户输入SQL
  │  "SELECT name FROM student WHERE id = 100"
  │
  ▼
步骤2: 词法分析（第8-9章）
  │  把字符串拆成单词（Token）
  │  SELECT → 关键字
  │  name → 标识符（列名）
  │  FROM → 关键字
  │  student → 标识符（表名）
  │  WHERE → 关键字
  │  id → 标识符
  │  = → 运算符
  │  100 → 数字
  │
  ▼
步骤3: 语法分析（第10章）
  │  理解Token之间的关系，构建语法树
  │
  │       SELECT
  │         │
  │       name
  │         │
  │       FROM
  │         │
  │      student
  │         │
  │       WHERE
  │         │
  │     id = 100
  │
  │  语法树的结构：
  │  - 这是一个SELECT查询
  │  - 查询的列是 name
  │  - 查询的表是 student
  │  - 过滤条件是 id = 100
  │
  ▼
步骤4: 生成执行计划（第10章）
  │  优化器决定"怎么做最高效"
  │
  │  方案A: 全表扫描（一行一行检查id是不是100）
  │  方案B: 用id上的索引直接定位（如果id有索引）（第6-7章）
  │
  │  优化器选择方案B（因为更高效）
  │
  │  执行计划：
  │  ┌─────────────────────────────┐
  │  │ 1. 在 id 索引上查找 key=100 │
  │  │ 2. 通过索引找到数据位置    │
  │  │ 3. 读取该行的 name 字段     │
  │  │ 4. 返回结果                 │
  │  └─────────────────────────────┘
  │
  ▼
步骤5: 执行器开始执行（第11章）
  │  执行器调用存储引擎的接口
  │  说："请给我 student 表中 id=100 那一行的 name 值"
  │
  ▼
步骤6: 存储引擎处理（第3-5章）
  │  6.1 先检查内存缓存（Buffer Pool）（第5章）
  │      如果数据已经在内存中 → 直接返回（很快！）
  │      如果不在内存中 → 继续下一步
  │
  │  6.2 从磁盘读取数据页（第3-4章）
  │      存储引擎知道数据在文件的哪个位置
  │      把包含 id=100 的数据页读到内存
  │
  │  6.3 如果是通过索引查找（第6-7章）
  │      先查B+树索引，找到 id=100 的叶子节点
  │      叶子节点里存储了数据在磁盘上的精确位置
  │      然后精确读取那个位置
  │
  ▼
步骤7: 并发控制检查（第14章）
  │  检查这行数据有没有被其他事务锁住
  │  如果没锁 → 直接读取
  │  如果被锁 → 根据隔离级别决定等待还是读旧版本（MVCC）
  │
  ▼
步骤8: 返回结果
  │  把读取到的 name 值返回给用户
  │
  └── 用户看到结果：比如 "张三"
```

**关键理解**：整个过程对你（用户）来说只是一瞬间的事，但对数据库内核来说，它经历了8个步骤、涉及5大模块的协作。

---

### 三、核心流程：数据库内核的启动和运行流程

#### 3.1 数据库启动流程

```
1. 读取配置文件
   └─ 数据文件放在哪里？内存给多大？等等

2. 初始化存储引擎
   └─ 打开数据文件，检查文件完整性

3. 初始化缓冲区（Buffer Pool）
   └─ 在内存中分配一大块空间，用于缓存数据

4. 执行崩溃恢复（如果有必要）
   └─ 检查日志，看看上次关闭是否正常
   └─ 如果不正常，用Redo/Undo日志恢复数据

5. 初始化锁管理器
   └─ 准备处理并发请求

6. 开始监听客户端连接
   └─ 等待用户发送SQL语句
```

#### 3.2 处理一条SQL的简化流程

```
接收SQL → 词法分析 → 语法分析 → 生成执行计划
    → 执行器调用存储引擎 → 存储引擎读/写数据
    → 记录日志 → 返回结果
```

---

### 四、常见误区

#### 误区1：数据库 = MySQL软件

**错误理解**：以为MySQL这个软件就是数据库的全部。

**正确理解**：MySQL是一个"数据库管理系统（DBMS）"，它包含：
- 数据库内核（InnoDB/MyISAM）
- 网络层（处理客户端连接）
- 管理工具（备份、用户管理）
- SQL接口（接收和解析SQL）

我们学的是**内核部分**，不是整个MySQL。

#### 误区2：数据库内核一定要用C/C++写

**错误理解**：以为学数据库内核必须先精通C语言。

**正确理解**：
- 工业级数据库（MySQL、PostgreSQL）确实用C/C++写的（因为要极致的性能）
- 但我们学习原理时，Java完全够用！
- Java写数据库内核的教学价值极高：代码清晰、不关心指针、内存管理自动化
- 理解了原理后，以后想深入C/C++版本也容易得多

#### 误区3：数据库就是把数据存到文件里

**错误理解**：数据库 = 把数据写入文件 + 从文件读出来

**正确理解**：文件存储只是数据库内核的一小部分（第3章）。完整的内核还包括：
- 高效的数据组织方式（B+树索引）
- 并发访问控制（锁/MVCC）
- 崩溃恢复（日志系统）
- 事务管理（ACID）
- SQL解析和执行

如果只是"文件的读写"，那和记事本没有区别。

#### 误区4：学习数据库内核需要先精通算法和数据结构

**错误理解**：必须先把《算法导论》学完才能入门。

**正确理解**：
- 你只需要了解基础的数组、链表、树的概念
- B+树、哈希索引这些会在对应章节从零讲起
- 我们边学边补，不需要前置学习完整的数据结构课程

#### 误区5：一个SQL查询的执行过程是线性的

**错误理解**：SQL → 解析 → 执行 → 返回，一条直线走到底。

**正确理解**：
- 执行计划有多个可能的方案（全表扫描 vs 索引查找）
- 优化器会选择代价最小的方案
- 执行过程中可能触发缓冲区淘汰、日志写入等额外操作
- 并发环境下还有锁等待、死锁检测等

---

## Java实战Demo

### Demo目标

本章的Demo不是实现数据库，而是用Java代码**模拟数据库内核各模块的协作流程**。通过运行代码，你能直观感受到一条SQL从输入到返回结果的完整过程。

### 项目结构

```
chapter-01-basic-concepts/demo/
├── pom.xml
└── src/main/java/com/ourdatabase/core/
    ├── Main.java                    (主程序，演示完整流程)
    ├── 词法分析器.java               (模拟SQL词法分析)
    ├── 语法分析器.java               (模拟SQL语法分析)
    ├── 存储引擎.java                 (模拟数据存取)
    ├── 事务管理器.java               (模拟事务控制)
    └── 缓冲区.java                   (模拟内存缓存)
```

### 代码详解

#### pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.ourdatabase</groupId>
    <artifactId>db-core-chapter01</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>第1章：数据库内核核心概念入门</name>
    <description>演示数据库内核五大模块的协作流程</description>

    <properties>
        <maven.compiler.source>17</maven.compiler.source>
        <maven.compiler.target>17</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
</project>
```

#### Main.java

```java
package com.ourdatabase.core;

/**
 * 数据库内核主程序 —— 演示一条SQL语句的完整处理流程
 *
 * 学习要点：
 * 1. 数据库内核的五大模块如何协作
 * 2. 一条SQL从输入到返回结果经历了哪些步骤
 * 3. 每个模块在整个流程中扮演什么角色
 */
public class Main {

    public static void main(String[] args) {
        // 打印欢迎信息
        打印分隔线();
        System.out.println("🎯 欢迎来到数据库内核模拟程序！");
        System.out.println("🎯 本程序将演示一条SQL语句在数据库内核中的完整旅程");
        打印分隔线();

        // ========== 第一步：用户输入SQL ==========
        System.out.println("\n【第1步】用户输入SQL语句：");
        String 用户输入的SQL = "SELECT name, age FROM student WHERE id = 100";
        System.out.println("  → " + 用户输入的SQL);

        // ========== 第二步：词法分析 ==========
        System.out.println("\n【第2步】词法分析器开始工作（将SQL字符串拆成单词）：");
        词法分析器 词法器 = new 词法分析器();
        String[] 单词列表 = 词法器.分析(用户输入的SQL);
        System.out.println("  → 词法分析完成，共识别出 " + 单词列表.length + " 个单词");

        // ========== 第三步：语法分析 ==========
        System.out.println("\n【第3步】语法分析器开始工作（理解SQL的含义）：");
        语法分析器 语法器 = new 语法分析器();
        语法分析结果 分析结果 = 语法器.分析(单词列表);
        System.out.println("  → 操作类型：" + 分析结果.获取操作类型());
        System.out.println("  → 目标表名：" + 分析结果.获取表名());
        System.out.println("  → 查询列：" + String.join(", ", 分析结果.获取列名()));
        System.out.println("  → 过滤条件：" + 分析结果.获取条件());

        // ========== 第四步：事务管理器检查 ==========
        System.out.println("\n【第4步】事务管理器开始工作（确保操作安全）：");
        事务管理器 事务器 = new 事务管理器();
        事务器.开始事务();
        System.out.println("  → 事务已开启，事务ID：" + 事务器.获取当前事务ID());

        // ========== 第五步：缓冲区/存储引擎读取数据 ==========
        System.out.println("\n【第5步】存储引擎开始工作（从磁盘或缓存读取数据）：");
        缓冲区 缓存 = new 缓冲区();
        存储引擎 存储 = new 存储引擎(缓存);

        // 先往数据库里放一些模拟数据（第3章才会学习真正的磁盘存储）
        System.out.println("  → 模拟数据准备中...");
        存储.插入数据("student", new String[]{"id", "name", "age"},
                new String[]{"100", "张三", "20"});
        存储.插入数据("student", new String[]{"id", "name", "age"},
                new String[]{"101", "李四", "22"});
        存储.插入数据("student", new String[]{"id", "name", "age"},
                new String[]{"102", "王五", "19"});

        // 模拟查询
        String 查询结果 = 存储.查询数据(分析结果);
        System.out.println("  → 查询结果：" + 查询结果);

        // ========== 第六步：事务提交 ==========
        System.out.println("\n【第6步】事务提交（确认操作完成）：");
        事务器.提交事务();
        System.out.println("  → 事务已提交，数据已持久化");

        // ========== 总结 ==========
        打印分隔线();
        System.out.println("\n📋 流程总结：");
        System.out.println("  用户输入SQL → 词法分析(拆单词) → 语法分析(理解含义)");
        System.out.println("  → 事务开始 → 存储引擎查询 → 返回结果 → 事务提交");
        System.out.println("\n  整个过程涉及了数据库内核的三大核心模块：");
        System.out.println("  ① 查询引擎（词法分析 + 语法分析）");
        System.out.println("  ② 事务管理器（事务控制）");
        System.out.println("  ③ 存储引擎（缓冲区 + 数据存取）");
        打印分隔线();
    }

    /** 打印一条装饰性的分隔线 */
    private static void 打印分隔线() {
        System.out.println("═".repeat(60));
    }
}
```

#### 词法分析器.java

```java
package com.ourdatabase.core;

/**
 * 词法分析器 —— 负责把SQL字符串拆分成一个一个的"单词"（Token）
 *
 * 这是查询引擎的第一个子模块。
 *
 * 工作原理（简化版）：
 * 1. 用空格把SQL字符串切开
 * 2. 去掉空字符串
 * 3. 返回单词数组
 */
public class 词法分析器 {

    /**
     * 分析SQL字符串，拆成单词列表
     *
     * @param sql 用户输入的SQL语句
     * @return 拆分后的单词数组
     */
    public String[] 分析(String sql) {
        // 第1步：用空格拆分SQL字符串
        // 例如："SELECT name FROM student" → ["SELECT", "name", "FROM", "student"]
        String[] 原始单词 = sql.split(" ");

        // 第2步：过滤掉空字符串（多个连续空格会产生空字符串）
        // 先数一数有多少个非空单词
        int 有效单词数 = 0;
        for (String 单词 : 原始单词) {
            if (单词.length() > 0) {
                有效单词数++;
            }
        }

        // 第3步：把有效单词放入新数组
        String[] 结果 = new String[有效单词数];
        int 索引 = 0;
        for (String 单词 : 原始单词) {
            if (单词.length() > 0) {
                结果[索引] = 单词;
                索引++;
            }
        }

        // 第4步：打印每个单词（帮助你理解词法分析的结果）
        for (int i = 0; i < 结果.length; i++) {
            String 类型说明 = 识别单词类型(结果[i]);
            System.out.println("    Token[" + i + "]: \"" + 结果[i] + "\" → " + 类型说明);
        }

        return 结果;
    }

    /**
     * 识别一个单词的类型
     *
     * 在真实的数据库词法分析器中，这里会用有限状态机(FSM)来精确识别。
     * 第8-9章会详细学习这部分。
     *
     * @param 单词 要识别的单词
     * @return 该单词的类型说明
     */
    private String 识别单词类型(String 单词) {
        // 判断是否为SQL关键字
        String 大写单词 = 单词.toUpperCase();
        switch (大写单词) {
            case "SELECT":
                return "关键字(SELECT)";
            case "FROM":
                return "关键字(FROM)";
            case "WHERE":
                return "关键字(WHERE)";
            case "INSERT":
                return "关键字(INSERT)";
            case "INTO":
                return "关键字(INTO)";
            case "VALUES":
                return "关键字(VALUES)";
            case "DELETE":
                return "关键字(DELETE)";
            case "UPDATE":
                return "关键字(UPDATE)";
            case "SET":
                return "关键字(SET)";
            case "CREATE":
                return "关键字(CREATE)";
            case "TABLE":
                return "关键字(TABLE)";
        }

        // 判断是否为运算符
        if (单词.equals("=") || 单词.equals(">") || 单词.equals("<")
                || 单词.equals(">=") || 单词.equals("<=") || 单词.equals("!=")) {
            return "运算符";
        }

        // 判断是否为逗号
        if (单词.equals(",")) {
            return "分隔符(逗号)";
        }

        // 判断是否为星号（表示查询所有列）
        if (单词.equals("*")) {
            return "通配符(所有列)";
        }

        // 尝试解析为数字
        try {
            Integer.parseInt(单词);
            return "数字(字面量)";
        } catch (NumberFormatException e) {
            // 不是数字，继续判断
        }

        // 尝试解析为带引号的字符串
        if (单词.startsWith("'") && 单词.endsWith("'")) {
            return "字符串(字面量)";
        }

        // 剩下的默认当作标识符（表名、列名等）
        return "标识符(表名/列名)";
    }
}
```

#### 语法分析器.java

```java
package com.ourdatabase.core;

/**
 * 语法分析器 —— 理解SQL语句的含义，生成结构化的分析结果
 *
 * 工作原理（简化版）：
 * 1. 检查SQL语句的语法是否正确（比如SELECT后面必须是列名）
 * 2. 提取关键信息：操作类型、表名、列名、条件
 * 3. 返回结构化的分析结果
 */
public class 语法分析器 {

    /**
     * 分析单词列表，提取SQL语句的结构化信息
     *
     * @param 单词列表 词法分析器输出的单词数组
     * @return 结构化的分析结果
     */
    public 语法分析结果 分析(String[] 单词列表) {
        // 创建一个分析结果对象，用来存放提取的信息
        语法分析结果 结果 = new 语法分析结果();

        // 获取第一个单词，判断操作类型（SELECT / INSERT / DELETE等）
        String 操作类型 = 单词列表[0].toUpperCase();

        switch (操作类型) {
            case "SELECT":
                解析SELECT语句(单词列表, 结果);
                break;
            case "INSERT":
                解析INSERT语句(单词列表, 结果);
                break;
            case "DELETE":
                解析DELETE语句(单词列表, 结果);
                break;
            default:
                throw new IllegalArgumentException("不支持的SQL操作类型：" + 操作类型);
        }

        return 结果;
    }

    /**
     * 解析 SELECT 语句
     *
     * 标准格式：SELECT 列名1, 列名2, ... FROM 表名 WHERE 条件
     * 简化格式：SELECT 列名1, 列名2, ... FROM 表名
     *
     * 例如：SELECT name, age FROM student WHERE id = 100
     */
    private void 解析SELECT语句(String[] 单词列表, 语法分析结果 结果) {
        结果.设置操作类型("SELECT");

        // 找到 FROM 关键字的位置
        // FROM 前面是列名，FROM 后面是表名
        int from位置 = -1;
        for (int i = 0; i < 单词列表.length; i++) {
            if (单词列表[i].equalsIgnoreCase("FROM")) {
                from位置 = i;
                break;
            }
        }

        if (from位置 == -1) {
            throw new IllegalArgumentException("SELECT语句缺少FROM关键字！");
        }

        // 提取列名（SELECT 和 FROM 之间的部分）
        // SELECT name, age FROM ... → 列名是 ["name", "age"]
        for (int i = 1; i < from位置; i++) {
            String 列名 = 单词列表[i];
            // 去掉逗号（列名可能是 "name," 这样带有逗号的）
            if (列名.endsWith(",")) {
                列名 = 列名.substring(0, 列名.length() - 1);
            }
            // "*" 表示查询所有列
            if (!列名.equals(",")) {
                结果.添加列名(列名);
            }
        }

        // 提取表名（FROM 后面、WHERE 前面的部分）
        String 表名 = 单词列表[from位置 + 1];
        结果.设置表名(表名);

        // 检查是否有 WHERE 子句
        int where位置 = -1;
        for (int i = 0; i < 单词列表.length; i++) {
            if (单词列表[i].equalsIgnoreCase("WHERE")) {
                where位置 = i;
                break;
            }
        }

        // 如果有 WHERE 子句，提取条件
        if (where位置 != -1) {
            StringBuilder 条件 = new StringBuilder();
            for (int i = where位置 + 1; i < 单词列表.length; i++) {
                条件.append(单词列表[i]);
                if (i < 单词列表.length - 1) {
                    条件.append(" ");
                }
            }
            结果.设置条件(条件.toString());
        }
    }

    /**
     * 解析 INSERT 语句
     *
     * 标准格式：INSERT INTO 表名 (列名1, 列名2) VALUES (值1, 值2)
     */
    private void 解析INSERT语句(String[] 单词列表, 语法分析结果 结果) {
        结果.设置操作类型("INSERT");

        // INSERT INTO student (id, name) VALUES (100, '张三')
        // [0]      [1]  [2]       [3] [4]   [5]  [6]     [7]
        结果.设置表名(单词列表[2]);

        // 提取列名和值（简化处理，第10章会详细实现）
        System.out.println("    (INSERT语句的详细解析将在第10章实现)");
    }

    /**
     * 解析 DELETE 语句
     *
     * 标准格式：DELETE FROM 表名 WHERE 条件
     */
    private void 解析DELETE语句(String[] 单词列表, 语法分析结果 结果) {
        结果.设置操作类型("DELETE");
        // DELETE FROM student WHERE id = 100
        结果.设置表名(单词列表[2]);

        // 提取WHERE条件
        if (单词列表.length > 3 && 单词列表[3].equalsIgnoreCase("WHERE")) {
            StringBuilder 条件 = new StringBuilder();
            for (int i = 4; i < 单词列表.length; i++) {
                条件.append(单词列表[i]);
                if (i < 单词列表.length - 1) {
                    条件.append(" ");
                }
            }
            结果.设置条件(条件.toString());
        }
    }
}
```

#### 语法分析结果.java

```java
package com.ourdatabase.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 语法分析结果 —— 存储语法分析器提取的结构化信息
 *
 * 这是一个"数据对象"，用来在模块之间传递信息。
 * 在真实的数据库中，这对应"抽象语法树（AST）"——第10章会详细学习。
 */
public class 语法分析结果 {

    private String 操作类型;       // SELECT / INSERT / DELETE / UPDATE
    private String 表名;           // 要操作的表名
    private List<String> 列名列表; // 要查询的列（SELECT语句）
    private String 条件;           // WHERE条件

    /** 构造函数，初始化列名列表 */
    public 语法分析结果() {
        this.列名列表 = new ArrayList<>();
    }

    // ========== Getter 和 Setter 方法 ==========

    public String 获取操作类型() {
        return 操作类型;
    }

    public void 设置操作类型(String 操作类型) {
        this.操作类型 = 操作类型;
    }

    public String 获取表名() {
        return 表名;
    }

    public void 设置表名(String 表名) {
        this.表名 = 表名;
    }

    public List<String> 获取列名() {
        return 列名列表;
    }

    public void 添加列名(String 列名) {
        this.列名列表.add(列名);
    }

    public String 获取条件() {
        return 条件;
    }

    public void 设置条件(String 条件) {
        this.条件 = 条件;
    }
}
```

#### 缓冲区.java

```java
package com.ourdatabase.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓冲区（Buffer） —— 模拟数据库的内存缓存
 *
 * 数据库中最慢的操作是读写磁盘（硬盘）。
 * 为了加速，数据库会把常用的数据放在内存里，这就是"缓冲区"。
 *
 * 生活类比：
 * - 你的办公桌（内存/缓冲区）：放着常用的文件，伸手就能拿到
 * - 旁边的文件柜（磁盘）：存着所有文件，但需要站起来走过去才能拿到
 * - 缓冲区命中：你要的文件刚好在桌上 → 很快
 * - 缓冲区未命中：你要的文件在柜子里 → 需要走过去拿 → 慢
 *
 * 第5章会详细学习缓冲区的设计原理。
 */
public class 缓冲区 {

    // 用HashMap模拟内存缓存
    // 键 = 表名，值 = 表中的数据行
    // 真实数据库中会使用更复杂的数据结构
    private Map<String, Map<String, String[]>> 缓存 = new HashMap<>();

    /**
     * 从缓冲区读取数据
     *
     * @param 表名 要查询的表名
     * @param 列名 主键列名
     * @param 主键值 主键的值
     * @return 如果命中缓存则返回数据，否则返回null
     */
    public String[] 读取(String 表名, String 列名, String 主键值) {
        Map<String, String[]> 表数据 = 缓存.get(表名);
        if (表数据 != null) {
            String[] 行数据 = 表数据.get(主键值);
            if (行数据 != null) {
                System.out.println("    ✓ 缓冲区命中！直接从内存读取，速度快！");
                return 行数据;
            }
        }
        System.out.println("    ✗ 缓冲区未命中，需要从磁盘读取（模拟）");
        return null;
    }

    /**
     * 将数据写入缓冲区
     *
     * @param 表名 表名
     * @param 主键值 主键的值
     * @param 数据 要缓存的数据行
     */
    public void 写入(String 表名, String 主键值, String[] 数据) {
        // 如果缓存中还没有这张表，先创建一个
        Map<String, String[]> 表数据 = 缓存.get(表名);
        if (表数据 == null) {
            表数据 = new HashMap<>();
            缓存.put(表名, 表数据);
        }
        // 把数据放入缓存
        表数据.put(主键值, 数据);
        System.out.println("    → 数据已写入缓冲区（内存中）");
    }

    /**
     * 获取缓冲区当前的缓存数量
     */
    public int 获取缓存条目数() {
        int 总数 = 0;
        for (Map<String, String[]> 表数据 : 缓存.values()) {
            总数 += 表数据.size();
        }
        return 总数;
    }
}
```

#### 存储引擎.java

```java
package com.ourdatabase.core;

import java.util.*;

/**
 * 存储引擎 —— 模拟数据在磁盘上的存储和读取
 *
 * 存储引擎是数据库内核最核心的模块之一。
 * 它负责：
 * 1. 把数据持久化到磁盘（写操作）
 * 2. 从磁盘读取数据（读操作）
 * 3. 配合缓冲区提高读取效率
 *
 * 在真实数据库中，存储引擎管理的是"数据页"（Page），第4章会详细学习。
 * 本章先用简单的HashMap模拟磁盘存储，帮助你理解数据流。
 */
public class 存储引擎 {

    // 模拟"磁盘存储"：把表名和数据行都存到HashMap里
    // 真实数据库中，数据是存在磁盘文件里的（第3章学习）
    // 结构：表名 → (主键值 → 数据行)
    private Map<String, Map<String, String[]>> 磁盘存储 = new HashMap<>();

    // 每个表的列定义
    // 结构：表名 → 列名数组
    private Map<String, String[]> 表结构 = new HashMap<>();

    // 缓冲区引用
    private 缓冲区 缓存;

    /**
     * 构造函数
     * @param 缓存 传入一个缓冲区对象
     */
    public 存储引擎(缓冲区 缓存) {
        this.缓存 = 缓存;
    }

    /**
     * 插入一条数据
     *
     * @param 表名 要插入的表名
     * @param 列名 表的列名
     * @param 值 要插入的数据值
     */
    public void 插入数据(String 表名, String[] 列名, String[] 值) {
        // 1. 先拿到（或创建）这张表的存储空间
        Map<String, String[]> 表数据 = 磁盘存储.get(表名);
        if (表数据 == null) {
            表数据 = new LinkedHashMap<>(); // LinkedHashMap保持插入顺序
            磁盘存储.put(表名, 表数据);
        }

        // 2. 保存表结构（列名信息）
        表结构.put(表名, 列名);

        // 3. 把数据存到"磁盘"中，用第一列作为主键
        //    真实数据库中，主键不一定总是第一列
        String 主键值 = 值[0];
        表数据.put(主键值, 值);

        // 4. 同时也放入缓冲区（加速后续读取）
        缓存.写入(表名, 主键值, 值);

        System.out.println("    → 已插入数据: 主键=" + 主键值 + ", 数据=" + Arrays.toString(值));
    }

    /**
     * 查询数据
     *
     * @param 分析结果 语法分析的结果（包含表名、列名、条件等）
     * @return 查询结果字符串
     */
    public String 查询数据(语法分析结果 分析结果) {
        String 表名 = 分析结果.获取表名();
        String 条件 = 分析结果.获取条件();
        List<String> 查询列 = 分析结果.获取列名();

        // 1. 检查表是否存在
        Map<String, String[]> 表数据 = 磁盘存储.get(表名);
        if (表数据 == null) {
            return "错误：表 '" + 表名 + "' 不存在！";
        }

        // 2. 解析 WHERE 条件（简化处理：只支持 "列名 = 值" 的格式）
        //    例如：id = 100
        String 条件列名 = null;
        String 条件值 = null;

        if (条件 != null && !条件.isEmpty()) {
            String[] 条件部分 = 条件.split("=");
            if (条件部分.length == 2) {
                条件列名 = 条件部分[0].trim();
                条件值 = 条件部分[1].trim();
            }
        }

        // 3. 查找数据
        StringBuilder 结果 = new StringBuilder();

        if (条件值 != null) {
            // 有条件的情况：精确查找
            // 3.1 先查缓冲区（内存）
            String[] 行数据 = 缓存.读取(表名, 条件列名, 条件值);

            // 3.2 如果缓冲区没有，再查"磁盘"
            if (行数据 == null) {
                行数据 = 表数据.get(条件值);
                // 找到后放入缓冲区
                if (行数据 != null) {
                    缓存.写入(表名, 条件值, 行数据);
                }
            }

            // 3.3 组装结果
            if (行数据 != null) {
                结果.append(组装结果行(表名, 查询列, 行数据));
            } else {
                结果.append("未找到匹配的数据（条件: ").append(条件).append("）");
            }

        } else {
            // 无条件的情况：返回所有数据
            for (Map.Entry<String, String[]> 条目 : 表数据.entrySet()) {
                结果.append(组装结果行(表名, 查询列, 条目.getValue()));
                结果.append("\n");
            }
        }

        return 结果.toString();
    }

    /**
     * 将一行原始数据按照需要的列组装成结果字符串
     *
     * @param 表名 表名
     * @param 需要的列 需要返回哪些列
     * @param 行数据 完整的行数据
     * @return 格式化的结果字符串
     */
    private String 组装结果行(String 表名, List<String> 需要的列, String[] 行数据) {
        String[] 列名 = 表结构.get(表名);
        StringBuilder sb = new StringBuilder("{ ");

        // 如果查询的是 "*"（所有列），返回所有列
        List<String> 实际需要的列 = 需要的列;
        if (需要的列.size() == 1 && 需要的列.get(0).equals("*")) {
            实际需要的列 = Arrays.asList(列名);
        }

        for (int i = 0; i < 实际需要的列.size(); i++) {
            String 目标列 = 实际需要的列.get(i);
            // 在列名数组中找到目标列的索引
            for (int j = 0; j < 列名.length; j++) {
                if (列名[j].equals(目标列)) {
                    sb.append(列名[j]).append("=").append(行数据[j]);
                    if (i < 实际需要的列.size() - 1) {
                        sb.append(", ");
                    }
                    break;
                }
            }
        }

        sb.append(" }");
        return sb.toString();
    }
}
```

#### 事务管理器.java

```java
package com.ourdatabase.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 事务管理器 —— 模拟数据库的事务控制
 *
 * 事务的核心概念：一组操作要么全部成功，要么全部失败。
 *
 * 第12章会详细学习事务的实现原理。
 * 本章先理解事务的基本概念：开始、提交、回滚。
 */
public class 事务管理器 {

    // 生成唯一的事务ID
    private static final AtomicLong 事务ID生成器 = new AtomicLong(1000);

    // 当前活动的事务ID
    private Long 当前事务ID = null;

    // 事务是否处于活动状态
    private boolean 事务活动中 = false;

    /**
     * 开始一个新事务
     *
     * 在真实数据库中，开始事务时会做很多初始化工作：
     * - 分配事务ID
     * - 记录当前时间戳
     * - 初始化事务的私有工作区
     * - 申请必要的锁资源
     */
    public void 开始事务() {
        if (事务活动中) {
            System.out.println("  ⚠ 警告：已经有一个事务在进行中，请先提交或回滚当前事务");
            return;
        }
        当前事务ID = 事务ID生成器.incrementAndGet();
        事务活动中 = true;
        System.out.println("  → 事务已开始");
    }

    /**
     * 提交事务
     *
     * "提交"的意思是：确认这个事务中的所有操作都是有效的。
     * 提交后，数据将持久化到磁盘，其他事务可以看到这些修改。
     *
     * 提交过程（简化版）：
     * 1. 将所有修改写入日志（第13章）
     * 2. 确认数据已持久化
     * 3. 释放锁资源（第14章）
     * 4. 标记事务完成
     */
    public void 提交事务() {
        if (!事务活动中) {
            System.out.println("  ⚠ 警告：当前没有活动的事务可以提交");
            return;
        }
        System.out.println("  → 正在将事务修改写入日志...");
        System.out.println("  → 确认数据已持久化到磁盘...");
        System.out.println("  → 释放锁资源...");
        事务活动中 = false;
        System.out.println("  → 事务提交成功！");
    }

    /**
     * 回滚事务
     *
     * "回滚"的意思是：撤销这个事务中所有的操作。
     * 比如转账事务中，扣了张三的钱但还没给李四加上，这时候回滚就把钱还给张三。
     *
     * 回滚过程（简化版）：
     * 1. 根据Undo日志恢复数据到事务开始前的状态（第13章）
     * 2. 释放锁资源
     * 3. 标记事务完成
     */
    public void 回滚事务() {
        if (!事务活动中) {
            System.out.println("  ⚠ 警告：当前没有活动的事务可以回滚");
            return;
        }
        System.out.println("  → 正在根据Undo日志恢复数据...");
        System.out.println("  → 释放锁资源...");
        事务活动中 = false;
        System.out.println("  → 事务回滚完成，数据已恢复到事务开始前的状态");
    }

    /**
     * 获取当前事务ID
     */
    public long 获取当前事务ID() {
        return 当前事务ID;
    }

    /**
     * 检查是否有活动的事务
     */
    public boolean 是否有活动事务() {
        return 事务活动中;
    }
}
```

### 运行步骤

```bash
# 1. 进入第1章的demo目录
cd chapter-01-basic-concepts/demo

# 2. 编译项目
mvn compile

# 3. 运行主程序
mvn exec:java -Dexec.mainClass="com.ourdatabase.core.Main"
```

### 运行结果说明

运行成功后会看到类似以下输出：

```
════════════════════════════════════════════════════════════
🎯 欢迎来到数据库内核模拟程序！
🎯 本程序将演示一条SQL语句在数据库内核中的完整旅程
════════════════════════════════════════════════════════════

【第1步】用户输入SQL语句：
  → SELECT name, age FROM student WHERE id = 100

【第2步】词法分析器开始工作：
    Token[0]: "SELECT" → 关键字(SELECT)
    Token[1]: "name" → 标识符(表名/列名)
    Token[2]: "," → 分隔符(逗号)
    Token[3]: "age" → 标识符(表名/列名)
    Token[4]: "FROM" → 关键字(FROM)
    Token[5]: "student" → 标识符(表名/列名)
    Token[6]: "WHERE" → 关键字(WHERE)
    Token[7]: "id" → 标识符(表名/列名)
    Token[8]: "=" → 运算符
    Token[9]: "100" → 数字(字面量)
  → 词法分析完成，共识别出 10 个单词

【第3步】语法分析器开始工作：
  → 操作类型：SELECT
  → 目标表名：student
  → 查询列：name, age
  → 过滤条件：id = 100

【第4步】事务管理器开始工作：
  → 事务已开始，事务ID：1001

【第5步】存储引擎开始工作：
  → 模拟数据准备中...
  → 已插入数据
  → 已插入数据
  → 已插入数据
  ✓ 缓冲区命中！直接从内存读取！
  → 查询结果：{ name=张三, age=20 }

【第6步】事务提交：
  → 事务提交成功！

════════════════════════════════════════════════════════════

📋 流程总结：
  用户输入SQL → 词法分析(拆单词) → 语法分析(理解含义)
  → 事务开始 → 存储引擎查询 → 返回结果 → 事务提交

  整个过程涉及了数据库内核的三大核心模块：
  ① 查询引擎（词法分析 + 语法分析）
  ② 事务管理器（事务控制）
  ③ 存储引擎（缓冲区 + 数据存取）
```

---

## 本章总结

### 核心知识点回顾

| 序号 | 知识点 | 一句话概括 |
|------|--------|-----------|
| 1 | 数据库 vs 数据库内核 | 数据库是整辆车，内核是发动机 |
| 2 | 存储引擎 | 负责数据的持久化存储和高效读取 |
| 3 | 查询引擎 | 理解SQL语句并生成执行计划 |
| 4 | 事务管理器 | 保证一组操作要么全成功要么全失败（ACID） |
| 5 | 日志系统 | 记录所有操作，用于崩溃恢复 |
| 6 | 并发控制 | 多个用户同时操作时保证数据不错乱 |
| 7 | SQL执行全流程 | 词法分析 → 语法分析 → 执行计划 → 存储引擎 → 返回结果 |

### Demo功能复盘

| 功能 | 实现方式 | 对应章节 |
|------|----------|----------|
| 词法分析 | 用空格拆分SQL字符串 | 第8-9章深入学习 |
| 语法分析 | 按关键字位置提取字段信息 | 第10章深入学习 |
| 事务管理 | 模拟开始/提交/回滚流程 | 第12章深入学习 |
| 缓冲区 | HashMap模拟内存缓存 | 第5章深入学习 |
| 存储引擎 | HashMap模拟磁盘+缓存读取 | 第3-4章深入学习 |

### 下一章预告

**第2章：极简数据库内核原型设计**

在第2章中，你将用Java实现一个真正能"存数据"和"取数据"的迷你数据库。虽然功能简单（类似HashMap），但它会包含真正的文件存储和基础的KV操作接口。

---

> 📌 提醒：如果本章有任何概念没理解透，不要着急。每个模块在后续章节都会有深入讲解和动手实践。本章的目的是帮你建立"全局地图"，后续各章就是在地图上逐个点亮每个区域。
