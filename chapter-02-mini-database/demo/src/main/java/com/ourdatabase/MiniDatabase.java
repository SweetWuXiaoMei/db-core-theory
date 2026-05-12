package com.ourdatabase;

import java.util.Scanner;

/**
 * 迷你数据库 —— 交互式主程序
 *
 * 这是你的第一个数据库产品！
 * 虽然不是真正的生产级数据库，但它已经具备了数据库的核心骨架：
 * - 可以创建表
 * - 可以插入数据
 * - 可以查询数据
 * - 使用可插拔存储引擎
 *
 * 如何使用：
 * 1. 运行本程序
 * 2. 在提示符 miniDB> 后面输入命令
 * 3. 输入 EXIT 退出
 *
 * 支持的命令格式：
 *   CREATE TABLE 表名 列1 列2 列3 ...
 *   INSERT INTO 表名 VALUES 值1 值2 值3 ...
 *   SELECT * FROM 表名
 *   SELECT * FROM 表名 WHERE 列 = 值
 *   SHOW TABLES
 *   DESC 表名
 *   EXIT
 */
public class MiniDatabase {

    // 存储引擎 —— 这里使用HashMap引擎
    // 如果想换引擎，只需要改变这一行！
    // 例如可以换成：new 文件存储引擎() 或 new BPlusTree引擎()
    private static final StorageEngineInterface engine = new HashMapStorageEngine();

    public static void main(String[] args) {
        printWelcome();

        // 创建Scanner来读取用户输入
        // System.in 是标准输入流，即键盘输入
        Scanner scanner = new Scanner(System.in);

        // 主循环：不断读取用户命令，直到用户输入 EXIT
        while (true) {
            // 打印提示符
            System.out.print("\nminiDB> ");

            // 读取一行用户输入
            String input = scanner.nextLine().trim();

            // 跳过空行
            if (input.isEmpty()) {
                continue;
            }

            // 检查是否要退出
            if (input.equalsIgnoreCase("EXIT") || input.equalsIgnoreCase("QUIT")) {
                System.out.println("  -> 再见！");
                break;
            }

            // 处理命令
            try {
                processCommand(input);
            } catch (Exception e) {
                System.out.println("  [错误] 命令执行失败：" + e.getMessage());
            }
        }

        scanner.close();
    }

    /**
     * 处理用户输入的命令
     *
     * 命令处理的流程：
     * 1. 把命令字符串按空格拆开
     * 2. 看第一个单词，判断是什么操作
     * 3. 调用对应的处理方法
     *
     * @param command 用户输入的命令字符串
     */
    private static void processCommand(String command) {
        // 第1步：拆分命令为单词
        String[] tokens = command.split("\\s+"); // \\s+ 表示一个或多个空格

        if (tokens.length == 0) return;

        // 第2步：根据第一个单词判断操作类型
        String operation = tokens[0].toUpperCase();

        switch (operation) {
            case "CREATE":
                handleCreateCommand(tokens);
                break;
            case "INSERT":
                handleInsertCommand(tokens);
                break;
            case "SELECT":
                handleSelectCommand(tokens);
                break;
            case "SHOW":
                handleShowCommand(tokens);
                break;
            case "DESC":
                handleDescCommand(tokens);
                break;
            case "HELP":
                printHelp();
                break;
            default:
                System.out.println("  [错误] 不支持的命令：" + operation);
                System.out.println("  [提示] 输入 HELP 查看支持的命令列表");
        }
    }

    /**
     * 处理 CREATE TABLE 命令
     *
     * 格式：CREATE TABLE 表名 列1 列2 列3 ...
     * 例如：CREATE TABLE student id name age
     *
     * @param tokens 拆分后的命令单词
     */
    private static void handleCreateCommand(String[] tokens) {
        // CREATE TABLE student id name age
        // [0]    [1]    [2]     [3] [4]  [5]

        if (tokens.length < 4 || !tokens[1].equalsIgnoreCase("TABLE")) {
            System.out.println("  [错误] CREATE命令格式：CREATE TABLE 表名 列1 列2 ...");
            return;
        }

        String tableName = tokens[2];

        // 提取列名（从第3个单词开始）
        String[] columnNames = new String[tokens.length - 3];
        for (int i = 3; i < tokens.length; i++) {
            columnNames[i - 3] = tokens[i];
        }

        boolean success = engine.createTable(tableName, columnNames);
        if (success) {
            System.out.println("  -> 表 '" + tableName + "' 创建成功，列: " + java.util.Arrays.toString(columnNames));
        } else {
            System.out.println("  [错误] 表 '" + tableName + "' 已经存在！");
        }
    }

    /**
     * 处理 INSERT INTO 命令
     *
     * 格式：INSERT INTO 表名 VALUES 值1 值2 值3 ...
     * 例如：INSERT INTO student VALUES 100 张三 20
     *
     * @param tokens 拆分后的命令单词
     */
    private static void handleInsertCommand(String[] tokens) {
        // INSERT INTO student VALUES 100 张三 20
        // [0]    [1]  [2]     [3]    [4] [5] [6]

        if (tokens.length < 5 || !tokens[1].equalsIgnoreCase("INTO")
                || !tokens[3].equalsIgnoreCase("VALUES")) {
            System.out.println("  [错误] INSERT命令格式：INSERT INTO 表名 VALUES 值1 值2 ...");
            return;
        }

        String tableName = tokens[2];

        // 检查表是否存在
        if (!engine.tableExists(tableName)) {
            System.out.println("  [错误] 表 '" + tableName + "' 不存在！请先用 CREATE TABLE 创建。");
            return;
        }

        // 提取值（从第4个单词 VALUES 之后开始）
        String[] values = new String[tokens.length - 4];
        for (int i = 4; i < tokens.length; i++) {
            values[i - 4] = tokens[i];
        }

        boolean success = engine.insert(tableName, values);
        if (success) {
            System.out.println("  -> 插入成功，主键=" + values[0]);
        }
    }

    /**
     * 处理 SELECT 命令
     *
     * 格式1（无条件）：SELECT * FROM 表名
     * 格式2（有条件）：SELECT * FROM 表名 WHERE 列 = 值
     *
     * @param tokens 拆分后的命令单词
     */
    private static void handleSelectCommand(String[] tokens) {
        // SELECT * FROM student
        // [0]    [1][2]  [3]
        //
        // SELECT * FROM student WHERE id = 100
        // [0]    [1][2]  [3]     [4]  [5][6][7]

        if (tokens.length < 4 || !tokens[2].equalsIgnoreCase("FROM")) {
            System.out.println("  [错误] SELECT命令格式：SELECT * FROM 表名 [WHERE 列 = 值]");
            return;
        }

        String tableName = tokens[3];

        // 检查是否有 WHERE 子句
        String conditionColumn = null;
        String conditionValue = null;

        if (tokens.length >= 7 && tokens[4].equalsIgnoreCase("WHERE")
                && tokens[6].equals("=")) {
            conditionColumn = tokens[5];
            conditionValue = tokens[7];
        }

        String result = engine.query(tableName, conditionColumn, conditionValue);
        System.out.print("  -> " + result);
    }

    /**
     * 处理 SHOW TABLES 命令
     *
     * 显示当前数据库中所有的表
     *
     * @param tokens 拆分后的命令单词
     */
    private static void handleShowCommand(String[] tokens) {
        if (tokens.length >= 2 && tokens[1].equalsIgnoreCase("TABLES")) {
            String[] allTables = engine.getAllTableNames();
            if (allTables.length == 0) {
                System.out.println("  -> 当前没有表");
            } else {
                System.out.println("  -> 表列表：");
                for (String tableName : allTables) {
                    int rowCount = engine.getRowCount(tableName);
                    System.out.println("     " + tableName + " (" + rowCount + " 行)");
                }
            }
        } else {
            System.out.println("  [错误] SHOW命令格式：SHOW TABLES");
        }
    }

    /**
     * 处理 DESC 命令（查看表结构）
     *
     * 格式：DESC 表名
     *
     * @param tokens 拆分后的命令单词
     */
    private static void handleDescCommand(String[] tokens) {
        if (tokens.length < 2) {
            System.out.println("  [错误] DESC命令格式：DESC 表名");
            return;
        }

        String tableName = tokens[1];
        String[] columnNames = engine.getColumnNames(tableName);

        if (columnNames == null) {
            System.out.println("  [错误] 表 '" + tableName + "' 不存在！");
        } else {
            System.out.println("  -> 表 '" + tableName + "' 的结构：");
            System.out.println("     列数：" + columnNames.length);
            System.out.print("     列名：");
            for (int i = 0; i < columnNames.length; i++) {
                System.out.print(columnNames[i]);
                if (i < columnNames.length - 1) System.out.print(", ");
            }
            System.out.println();
            System.out.println("     行数：" + engine.getRowCount(tableName));
        }
    }

    /**
     * 打印欢迎信息和帮助
     */
    private static void printWelcome() {
        System.out.println("=".repeat(50));
        System.out.println("  欢迎使用迷你数据库（MiniDB）！");
        System.out.println("  这是你的第一个数据库内核原型");
        System.out.println("=".repeat(50));
        printHelp();
    }

    /**
     * 打印帮助信息
     */
    private static void printHelp() {
        System.out.println("\n支持的命令：");
        System.out.println("  CREATE TABLE 表名 列1 列2 ...    -- 创建新表");
        System.out.println("  INSERT INTO 表名 VALUES 值1 ...  -- 插入数据");
        System.out.println("  SELECT * FROM 表名               -- 查询所有数据");
        System.out.println("  SELECT * FROM 表名 WHERE 列 = 值 -- 条件查询");
        System.out.println("  SHOW TABLES                      -- 显示所有表");
        System.out.println("  DESC 表名                        -- 查看表结构");
        System.out.println("  HELP                             -- 显示此帮助");
        System.out.println("  EXIT                             -- 退出");
    }
}
