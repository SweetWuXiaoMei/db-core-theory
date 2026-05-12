package com.ourdatabase.core;

/**
 * 语法分析器 —— 理解SQL语句的含义，生成结构化的分析结果
 *
 * 工作原理（简化版）：
 * 1. 检查SQL语句的基本语法是否正确
 * 2. 提取关键信息：操作类型、表名、列名、条件
 * 3. 返回结构化的分析结果（在第10章中，这对应"抽象语法树AST"）
 *
 * 第8章讲理论，第9章实现词法分析，第10章实现完整的语法分析器。
 * 本章的语法分析器是极度简化版本，只为演示各模块的协作流程。
 */
public class Parser {

    /**
     * 分析单词列表，提取SQL语句的结构化信息
     *
     * @param tokenList 词法分析器输出的单词数组
     * @return 结构化的分析结果
     */
    public ParseResult analyze(String[] tokenList) {
        ParseResult result = new ParseResult();

        // 获取第一个单词，判断操作类型（SELECT / INSERT / DELETE 等）
        String operationType = tokenList[0].toUpperCase();

        switch (operationType) {
            case "SELECT":
                parseSelect(tokenList, result);
                break;
            case "INSERT":
                parseInsert(tokenList, result);
                break;
            case "DELETE":
                parseDelete(tokenList, result);
                break;
            case "UPDATE":
                parseUpdate(tokenList, result);
                break;
            case "CREATE":
                parseCreate(tokenList, result);
                break;
            default:
                throw new IllegalArgumentException("不支持的SQL操作类型：" + operationType);
        }

        return result;
    }

    /**
     * 解析 SELECT 语句
     *
     * 标准格式：SELECT 列名1, 列名2, ... FROM 表名 WHERE 条件
     * 简化格式：SELECT * FROM 表名
     *
     * 例如：SELECT name, age FROM student WHERE id = 100
     *        [0]   [1] [2]  [3]   [4]     [5]  [6][7][8]
     */
    private void parseSelect(String[] tokenList, ParseResult result) {
        result.setOperationType("SELECT");

        // 找到 FROM 关键字的位置
        // FROM 前面是列名，FROM 后面是表名
        int fromIndex = -1;
        for (int i = 0; i < tokenList.length; i++) {
            if (tokenList[i].equalsIgnoreCase("FROM")) {
                fromIndex = i;
                break;
            }
        }

        if (fromIndex == -1) {
            throw new IllegalArgumentException("SELECT语句缺少FROM关键字！");
        }

        // 提取列名（SELECT 和 FROM 之间的单词）
        // SELECT name, age FROM ...  -> 列名是 ["name", "age"]
        for (int i = 1; i < fromIndex; i++) {
            String columnName = tokenList[i];
            // 去掉末尾的逗号（"name," -> "name"）
            if (columnName.endsWith(",")) {
                columnName = columnName.substring(0, columnName.length() - 1);
            }
            // 跳过单独的逗号分隔符
            if (!columnName.equals(",")) {
                result.addColumnName(columnName);
            }
        }

        // 提取表名（FROM 后面、WHERE 前面的部分）
        // FROM student WHERE ...  -> 表名是 "student"
        String tableName = tokenList[fromIndex + 1];
        result.setTableName(tableName);

        // 检查是否有 WHERE 子句
        int whereIndex = -1;
        for (int i = 0; i < tokenList.length; i++) {
            if (tokenList[i].equalsIgnoreCase("WHERE")) {
                whereIndex = i;
                break;
            }
        }

        // 如果有 WHERE 子句，提取条件
        if (whereIndex != -1) {
            StringBuilder condition = new StringBuilder();
            for (int i = whereIndex + 1; i < tokenList.length; i++) {
                condition.append(tokenList[i]);
                if (i < tokenList.length - 1) {
                    condition.append(" ");
                }
            }
            result.setCondition(condition.toString());
        }
    }

    /**
     * 解析 INSERT 语句
     *
     * 标准格式：INSERT INTO 表名 (列名1, 列名2) VALUES (值1, 值2)
     *
     * @param tokenList 词法分析后的单词数组
     * @param result 存放解析结果的对象
     */
    private void parseInsert(String[] tokenList, ParseResult result) {
        result.setOperationType("INSERT");
        // INSERT INTO student (id, name) VALUES (100, '张三')
        // [0]    [1]  [2]      [3]  [4]  [5]  [6]     [7]   [8]
        result.setTableName(tokenList[2]);
        System.out.println("    (INSERT语句的详细解析将在第10章实现)");
    }

    /**
     * 解析 DELETE 语句
     *
     * 标准格式：DELETE FROM 表名 WHERE 条件
     *
     * @param tokenList 词法分析后的单词数组
     * @param result 存放解析结果的对象
     */
    private void parseDelete(String[] tokenList, ParseResult result) {
        result.setOperationType("DELETE");
        // DELETE FROM student WHERE id = 100
        // [0]    [1]  [2]      [3]   [4][5][6]
        result.setTableName(tokenList[2]);

        if (tokenList.length > 3 && tokenList[3].equalsIgnoreCase("WHERE")) {
            StringBuilder condition = new StringBuilder();
            for (int i = 4; i < tokenList.length; i++) {
                condition.append(tokenList[i]);
                if (i < tokenList.length - 1) {
                    condition.append(" ");
                }
            }
            result.setCondition(condition.toString());
        }
    }

    /**
     * 解析 UPDATE 语句
     *
     * 标准格式：UPDATE 表名 SET 列名 = 值 WHERE 条件
     *
     * @param tokenList 词法分析后的单词数组
     * @param result 存放解析结果的对象
     */
    private void parseUpdate(String[] tokenList, ParseResult result) {
        result.setOperationType("UPDATE");
        result.setTableName(tokenList[1]);
        System.out.println("    (UPDATE语句的详细解析将在第10章实现)");
    }

    /**
     * 解析 CREATE 语句
     *
     * 标准格式：CREATE TABLE 表名 (列定义)
     *
     * @param tokenList 词法分析后的单词数组
     * @param result 存放解析结果的对象
     */
    private void parseCreate(String[] tokenList, ParseResult result) {
        result.setOperationType("CREATE");
        result.setTableName(tokenList[2]);
        System.out.println("    (CREATE TABLE语句的详细解析将在第10章实现)");
    }
}
