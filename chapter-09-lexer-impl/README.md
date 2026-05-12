# 第9章：Java实现简易SQL词法解析器

## 章节定位

本章将第8章的有限状态机理论落地为**完整的、可复用的SQL词法分析器**。

## 核心实现要点

1. **完整的Token类型支持**：关键字(15+)、标识符、整数、浮点数、字符串(单引号/双引号)、运算符(单字符/双字符)、分隔符
2. **双字符运算符**：`>=`, `<=`, `!=`, `<>` 必须作为一个Token整体识别
3. **浮点数识别**：`3.14` → NUMBER(3.14)，不是两个NUMBER
4. **错误处理**：遇到无法识别的字符时给出友好提示

## Java实战Demo

### 项目结构

```
chapter-09-lexer-impl/demo/
├── pom.xml
└── src/main/java/com/ourdatabase/sql/
    ├── Token类型.java    (所有Token类型的枚举)
    ├── Token.java        (Token数据类)
    ├── 词法分析器.java     (核心：完整的词法分析器)
    └── 词法分析演示.java   (主程序)
```

### 运行步骤

```bash
cd chapter-09-lexer-impl/demo
mvn compile
mvn exec:java -Dexec.mainClass="com.ourdatabase.sql.词法分析演示"
```

## 本章总结

完整实现了SQL词法分析器，可以处理真实SQL语句的Token拆分。下一步进入语法分析。
