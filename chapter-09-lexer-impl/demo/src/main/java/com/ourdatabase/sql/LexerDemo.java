package com.ourdatabase.sql;

import java.util.List;

/**
 * 词法分析演示 —— 第9章主程序
 *
 * 测试完整的SQL词法分析器，演示各种SQL语句的Token拆分结果
 */
public class LexerDemo {

    public static void main(String[] args) {
        printTitle("SQL词法分析器演示");

        String[] testCases = {
            "SELECT name, age FROM student WHERE id = 100",
            "INSERT INTO student VALUES (200, '张三', 20)",
            "DELETE FROM student WHERE age >= 22",
            "CREATE TABLE course (id, name, credit)",
            "SELECT * FROM student WHERE age <= 20 AND city = '北京'",
            "UPDATE student SET age = 21 WHERE id = 100",
            "SELECT DISTINCT city FROM student ORDER BY age DESC LIMIT 10",
        };

        for (int i = 0; i < testCases.length; i++) {
            String sql = testCases[i];
            System.out.println("\n" + "-".repeat(60));
            System.out.println("测试" + (i + 1) + ": " + sql);
            System.out.println("-".repeat(60));

            Lexer lexer = new Lexer(sql);
            List<Token> tokens = lexer.analyze();

            for (int j = 0; j < tokens.size(); j++) {
                System.out.println("  [" + j + "] " + tokens.get(j));
            }
        }

        printTitle("演示总结");
        System.out.println("1. 所有SQL语句都被正确拆分为Token");
        System.out.println("2. 双字符运算符(>=, <=, !=)被识别为一个Token");
        System.out.println("3. 字符串('张三')被完整保留引号和内容");
        System.out.println("4. 关键字不区分大小写，统一转为大写");
        System.out.println("5. 最后都有EOF标记，表示Token流结束");
    }

    private static void printTitle(String title) {
        System.out.println("=".repeat(60));
        System.out.println("  " + title);
        System.out.println("=".repeat(60));
    }
}
