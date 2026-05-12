# 第8章：SQL解析基础·词法分析原理

## 章节定位

### 本章学什么？

前7章学习了存储引擎和索引（数据怎么存、怎么快速查）。从本章开始进入**查询引擎**——理解用户输入的SQL字符串。

词法分析是SQL解析的第一步：**把SQL字符串拆成有意义的"单词"（Token）**。

你将学习：
1. 什么是Token（词法单元）？
2. 有限状态机（FSM）——词法分析的核心算法
3. SQL中的Token类型（关键字、标识符、数字、字符串、运算符）
4. 如何处理空白、注释、大小写

### 和前后章节的关联

```
第7章(B+树) → 【第8章(词法分析理论)】→ 第9章(词法分析器实现) → 第10章(语法分析)
```

---

## 详尽理论

### 一、概念解释

#### 1.1 什么是词法分析（Lexical Analysis）？

词法分析就是把一段"字符串"拆成一个个有意义的"词法单元（Token）"。

```
输入："SELECT name FROM student WHERE id = 100"

输出（Token列表）：
  Token[0]: SELECT    （关键字）
  Token[1]: name      （标识符）
  Token[2]: FROM      （关键字）
  Token[3]: student   （标识符）
  Token[4]: WHERE     （关键字）
  Token[5]: id        （标识符）
  Token[6]: =         （运算符）
  Token[7]: 100       （数字）
```

**类比**：把英文句子 "I love coding" 拆成 ["I", "love", "coding"] 三个单词。词法分析就是做这件事，只是输入是SQL。

#### 1.2 什么是Token（词法单元）？

一个Token包含两个信息：
- **类型**：这个单词是什么种类（关键字、标识符、数字等）
- **值**：这个单词的具体内容

```
Token {
    类型: KEYWORD（关键字）
    值: "SELECT"
}

Token {
    类型: IDENTIFIER（标识符）
    值: "student"
}

Token {
    类型: NUMBER（数字）
    值: "100"
}
```

#### 1.3 SQL中的Token类型

| Token类型 | 说明 | 示例 |
|-----------|------|------|
| KEYWORD | SQL关键字 | SELECT, FROM, WHERE, INSERT, CREATE |
| IDENTIFIER | 标识符（表名、列名） | student, name, id |
| NUMBER | 数字 | 100, 3.14, -5 |
| STRING | 字符串 | 'hello', '张三' |
| OPERATOR | 运算符 | =, >, <, >=, <=, != |
| PUNCTUATION | 标点符号 | , ( ) ; * |

---

### 二、原理详解：有限状态机（FSM）

#### 2.1 什么是有限状态机？

**有限状态机**是词法分析的核心算法。它由三部分组成：
1. **状态（State）**：当前在做什么（初始态、读数字、读单词等）
2. **转移（Transition）**：遇到什么字符就切换到什么状态
3. **动作（Action）**：进入某状态时做什么

#### 2.2 用FSM识别一个数字

```
状态图（用文字表示）：

[初始态] ──遇到数字──→ [读数字状态] ──遇到数字──→ [读数字状态]
                         │
                         └──遇到非数字──→ [完成] 输出数字Token
```

**逐字符处理过程**：

```
输入："100"

字符1: '1' → 当前状态=初始态，遇到数字 → 切换到[读数字状态]，记录'1'
字符2: '0' → 当前状态=读数字，遇到数字 → 继续读数字，追加'0'
字符3: '0' → 当前状态=读数字，遇到数字 → 继续读数字，追加'0'
读完 → 输出 Token{类型=NUMBER, 值="100"}
```

#### 2.3 用FSM识别一个标识符或关键字

```
[初始态] ──遇到字母→→ [读单词] ──遇到字母或数字──→ [读单词]
                       │
                       └──遇到非字母数字──→ [完成]
                           检查单词是否是关键字（SELECT/FROM...）
                           如果是关键字 → 输出KEYWORD Token
                           如果不是 → 输出IDENTIFIER Token
```

---

### 三、核心流程：完整词法分析流程

```
输入SQL字符串: "SELECT id FROM student WHERE age > 18"

步骤1: 初始化
  - 当前状态 = 初始态
  - Token列表 = []
  - 指针位置 = 0

步骤2: 逐字符扫描
  位置0: 'S' → 字母，进入[读单词]状态
  位置1: 'E' → 继续读单词: "SE"
  位置2: 'L' → 继续读单词: "SEL"
  ...
  位置5: 'T' → 继续读单词: "SELECT"
  位置6: ' ' (空格) → 单词结束！
    检查"SELECT" → 是关键字 → 输出 Token{KEYWORD, "SELECT"}
    状态回到初始态

  位置7: 'i' → 字母，进入[读单词]
  位置8: 'd' → 继续: "id"
  位置9: ' ' → 单词结束！
    检查"id" → 不是关键字 → 输出 Token{IDENTIFIER, "id"}

  ... 以此类推，直到扫描完整个字符串

步骤3: 输出Token列表
  [KEYWORD:SELECT, IDENTIFIER:id, KEYWORD:FROM, IDENTIFIER:student,
   KEYWORD:WHERE, IDENTIFIER:age, OPERATOR:>, NUMBER:18]
```

---

### 四、常见误区

#### 误区1：词法分析和语法分析是一回事

**错误**：以为"SQL解析"就是一个步骤。

**正确**：词法分析（Lexing）和语法分析（Parsing）是两个独立步骤。
- 词法分析 = 把字符串拆成单词（Token）
- 语法分析 = 理解单词之间的关系（构建语法树）

类比：词法分析 = 认识每个字；语法分析 = 理解整句话的意思。

#### 误区2：关键字和标识符用同样的方式处理

**错误**：所有单词都当作标识符处理。

**正确**：关键字（SELECT、FROM等）是SQL的"保留字"，有特殊含义。词法分析器需要区分关键字和普通标识符（表名、列名）。

---

## Java实战Demo

本章演示有限状态机的原理，用简单的字符扫描实现Token识别。

### 项目结构

```
chapter-08-lexer-theory/demo/
├── pom.xml
└── src/main/java/com/ourdatabase/sql/
    └── 有限状态机演示.java
```

### 运行步骤

```bash
cd chapter-08-lexer-theory/demo
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.sql.有限状态机演示"
```

---

## 本章总结

| 序号 | 知识点 | 说明 |
|------|--------|------|
| 1 | 词法分析 | 把SQL字符串拆成Token |
| 2 | Token | 类型+值的二元组 |
| 3 | 有限状态机 | 词法分析的核心算法 |
| 4 | Token类型 | KEYWORD, IDENTIFIER, NUMBER, STRING, OPERATOR |

### 下一章预告

**第9章：Java实现简易SQL词法解析器**

理论讲完了，第9章将用Java手写一个完整的SQL词法分析器，能把任何SQL语句拆成Token列表。
