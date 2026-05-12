package com.ourdatabase.core;

import java.util.*;

/**
 * 存储引擎 —— 模拟数据在"磁盘"上的存储和读取
 *
 * 存储引擎是数据库内核最核心的模块之一。
 * 它负责：
 * 1. 把数据持久化到磁盘（写操作）
 * 2. 从磁盘读取数据（读操作）
 * 3. 配合缓冲区提高读取效率
 *
 * 在真实数据库中，存储引擎管理的是"数据页"（Page），第4章会详细学习。
 * 数据是存在真实的磁盘文件里的，第3章会详细学习文件操作。
 * 本章先用 HashMap 模拟"磁盘"存储，帮助你理解数据流。
 */
public class StorageEngine {

    // 模拟"磁盘存储"
    // 结构: 表名 -> (主键值 -> 数据行)
    private Map<String, Map<String, String[]>> diskStorage = new HashMap<>();

    // 每个表的列定义
    // 结构: 表名 -> 列名数组
    private Map<String, String[]> tableSchema = new HashMap<>();

    // 缓冲区引用（内存缓存）
    private Buffer buffer;

    /**
     * 构造函数
     * @param buffer 传入一个缓冲区对象
     */
    public StorageEngine(Buffer buffer) {
        this.buffer = buffer;
    }

    /**
     * 插入一条数据
     *
     * 插入数据的步骤：
     * 1. 保存表结构（如果是新表）
     * 2. 把数据存入"磁盘"（HashMap模拟）
     * 3. 同时写入缓冲区（加速后续读取）
     *
     * @param tableName 要插入的表名
     * @param columnNames 表的列名数组
     * @param values 要插入的数据值
     */
    public void insertData(String tableName, String[] columnNames, String[] values) {
        // 1. 先拿到（或创建）这张表的存储空间
        Map<String, String[]> tableData = diskStorage.get(tableName);
        if (tableData == null) {
            tableData = new LinkedHashMap<>(); // LinkedHashMap 保持插入顺序
            diskStorage.put(tableName, tableData);
        }

        // 2. 保存表结构（列名信息）
        tableSchema.put(tableName, columnNames);

        // 3. 把数据存到"磁盘"中，用第一列的值作为主键
        String primaryKeyValue = values[0];
        tableData.put(primaryKeyValue, values);

        // 4. 同时放入缓冲区（加速后续的读取）
        buffer.write(tableName, primaryKeyValue, values);

        System.out.println("    -> 已插入数据: 主键=" + primaryKeyValue + ", 数据=" + Arrays.toString(values));
    }

    /**
     * 查询数据
     *
     * 查询的步骤：
     * 1. 先查缓冲区（内存），如果命中直接返回
     * 2. 如果缓存未命中，从"磁盘"读取
     * 3. 找到后把数据放入缓冲区（下次就快了）
     * 4. 按照需要的列组装返回结果
     *
     * @param parseResult 语法分析的结果（包含表名、列名、条件等）
     * @return 查询结果字符串
     */
    public String queryData(ParseResult parseResult) {
        String tableName = parseResult.getTableName();
        String condition = parseResult.getCondition();
        List<String> queryColumns = parseResult.getColumnNames();

        // 1. 检查表是否存在
        Map<String, String[]> tableData = diskStorage.get(tableName);
        if (tableData == null) {
            return "错误：表 '" + tableName + "' 不存在！";
        }

        // 2. 解析 WHERE 条件
        //    简化处理：只支持 "列名 = 值" 格式
        //    例如："id = 100"
        String conditionColumn = null;
        String conditionValue = null;

        if (condition != null && !condition.isEmpty()) {
            String[] conditionParts = condition.split("=");
            if (conditionParts.length == 2) {
                conditionColumn = conditionParts[0].trim();
                conditionValue = conditionParts[1].trim();
            }
        }

        // 3. 查找数据
        StringBuilder result = new StringBuilder();

        if (conditionValue != null) {
            // === 有条件的情况：精确查找 ===

            // 3.1 先查缓冲区（内存）—— 这一步很快！
            String[] rowData = buffer.read(tableName, conditionColumn, conditionValue);

            // 3.2 如果缓冲区没有，从"磁盘"读取 —— 这一步较慢
            if (rowData == null) {
                rowData = tableData.get(conditionValue);
                // 找到后放入缓冲区（下次再查就快了）
                if (rowData != null) {
                    buffer.write(tableName, conditionValue, rowData);
                }
            }

            // 3.3 组装结果
            if (rowData != null) {
                result.append(buildResultRow(tableName, queryColumns, rowData));
            } else {
                result.append("未找到匹配的数据（条件: ").append(condition).append("）");
            }

        } else {
            // === 无条件的情况：返回所有数据 ===
            result.append("查询结果（共 ").append(tableData.size()).append(" 条）：\n");
            for (Map.Entry<String, String[]> entry : tableData.entrySet()) {
                result.append("  ").append(buildResultRow(tableName, queryColumns, entry.getValue()));
                result.append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 将一行原始数据按照需要的列组装成可读的结果字符串
     *
     * 例如：需要的列是 [name, age]，原始行数据是 [100, 张三, 20]
     *       -> 组装成 "{ name=张三, age=20 }"
     *
     * @param tableName 表名
     * @param requestedColumns 用户查询了哪些列
     * @param rowData 完整的一行数据
     * @return 格式化的结果字符串
     */
    private String buildResultRow(String tableName, List<String> requestedColumns, String[] rowData) {
        String[] columnNameArray = tableSchema.get(tableName);
        StringBuilder sb = new StringBuilder("{ ");

        // 如果查询的是 "*"（所有列），则返回所有列
        List<String> actualColumns = requestedColumns;
        if (requestedColumns.size() == 1 && requestedColumns.get(0).equals("*")) {
            actualColumns = Arrays.asList(columnNameArray);
        }

        for (int i = 0; i < actualColumns.size(); i++) {
            String targetColumn = actualColumns.get(i);
            // 在列名数组中查找目标列的位置
            for (int j = 0; j < columnNameArray.length; j++) {
                if (columnNameArray[j].equals(targetColumn)) {
                    sb.append(columnNameArray[j]).append("='").append(rowData[j]).append("'");
                    if (i < actualColumns.size() - 1) {
                        sb.append(", ");
                    }
                    break;
                }
            }
        }

        sb.append(" }");
        return sb.toString();
    }

    /**
     * 删除数据（留作后续章节扩展）
     */
    public boolean deleteData(String tableName, String primaryKeyValue) {
        Map<String, String[]> tableData = diskStorage.get(tableName);
        if (tableData != null) {
            return tableData.remove(primaryKeyValue) != null;
        }
        return false;
    }
}
