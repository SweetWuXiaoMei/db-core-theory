package com.ourdatabase.core;

import java.util.*;

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
        printSeparator();
        System.out.println("欢迎来到数据库内核模拟程序！");
        System.out.println("本程序将演示一条SQL语句在数据库内核中的完整旅程");
        printSeparator();

        // ========== 第一步：用户输入SQL ==========
        System.out.println("\n【第1步】用户输入SQL语句：");
        String sqlInput = "SELECT name, age FROM student WHERE id = 100";
        System.out.println("  -> " + sqlInput);

        // ========== 第二步：词法分析 ==========
        System.out.println("\n【第2步】词法分析器开始工作（将SQL字符串拆成单词）：");
        Lexer lexer = new Lexer();
        String[] tokenList = lexer.analyze(sqlInput);
        System.out.println("  -> 词法分析完成，共识别出 " + tokenList.length + " 个单词");

        // ========== 第三步：语法分析 ==========
        System.out.println("\n【第3步】语法分析器开始工作（理解SQL的含义）：");
        Parser parser = new Parser();
        ParseResult parseResult = parser.analyze(tokenList);
        System.out.println("  -> 操作类型：" + parseResult.getOperationType());
        System.out.println("  -> 目标表名：" + parseResult.getTableName());
        System.out.println("  -> 查询列：" + String.join(", ", parseResult.getColumnNames()));
        System.out.println("  -> 过滤条件：" + parseResult.getCondition());

        // ========== 第四步：事务管理器检查 ==========
        System.out.println("\n【第4步】事务管理器开始工作（确保操作安全）：");
        TransactionManager txnManager = new TransactionManager();
        txnManager.beginTransaction();
        System.out.println("  -> 事务已开启，事务ID：" + txnManager.getCurrentTransactionId());

        // ========== 第五步：缓冲区/存储引擎读取数据 ==========
        System.out.println("\n【第5步】存储引擎开始工作（从磁盘或缓存读取数据）：");
        Buffer buffer = new Buffer();
        StorageEngine storageEngine = new StorageEngine(buffer);

        // 先往数据库里放一些模拟数据
        System.out.println("  -> 模拟数据准备中...");
        storageEngine.insertData("student", new String[]{"id", "name", "age"},
                new String[]{"100", "张三", "20"});
        storageEngine.insertData("student", new String[]{"id", "name", "age"},
                new String[]{"101", "李四", "22"});
        storageEngine.insertData("student", new String[]{"id", "name", "age"},
                new String[]{"102", "王五", "19"});

        // 模拟查询
        String queryResult = storageEngine.queryData(parseResult);
        System.out.println("  -> 查询结果：" + queryResult);

        // ========== 第六步：事务提交 ==========
        System.out.println("\n【第6步】事务提交（确认操作完成）：");
        txnManager.commitTransaction();
        System.out.println("  -> 事务已提交，数据已持久化");

        // ========== 总结 ==========
        printSeparator();
        System.out.println("\n流程总结：");
        System.out.println("  用户输入SQL -> 词法分析(拆单词) -> 语法分析(理解含义)");
        System.out.println("  -> 事务开始 -> 存储引擎查询 -> 返回结果 -> 事务提交");
        System.out.println("\n  整个过程涉及了数据库内核的三大核心模块：");
        System.out.println("  (1) 查询引擎（词法分析 + 语法分析）");
        System.out.println("  (2) 事务管理器（事务控制）");
        System.out.println("  (3) 存储引擎（缓冲区 + 数据存取）");
        printSeparator();
    }

    /** 打印一条装饰性的分隔线 */
    private static void printSeparator() {
        System.out.println("=".repeat(60));
    }
}
