package com.ourdatabase;

import java.util.*;

/**
 * HashMap存储引擎 —— 存储引擎接口的第一个具体实现
 *
 * 使用嵌套HashMap来模拟数据存储：
 * - 外层Map：表名 → 表数据
 * - 内层Map：主键 → 行数据（字符串数组）
 *
 * 这是最简单、最直观的实现方式。
 * 从第3章开始，我们会逐步替换成基于文件、基于B+树的更高级实现。
 *
 * 关键技术点：
 * - LinkedHashMap：保持数据的插入顺序，查询结果更直观
 * - 第一列自动作为主键（简化设计）
 */
public class HashMapStorageEngine implements StorageEngineInterface {

    // ==================== 核心数据结构 ====================

    /**
     * 磁盘存储（用内存HashMap模拟）
     *
     * 结构示意：
     * {
     *   "student" -> {           ← 外层：表名 → 表数据
     *     "100" -> ["100","张三","20"],  ← 内层：主键 → 行数据
     *     "101" -> ["101","李四","22"]
     *   },
     *   "course" -> {
     *     "1" -> ["1","数学","4"]
     *   }
     * }
     */
    private Map<String, Map<String, String[]>> dataStore = new LinkedHashMap<>();

    /**
     * 表结构定义
     *
     * 结构示意：
     * {
     *   "student" -> ["id", "name", "age"],
     *   "course"  -> ["course_id", "course_name", "credit"]
     * }
     */
    private Map<String, String[]> tableSchema = new LinkedHashMap<>();

    // ==================== 接口方法实现 ====================

    /**
     * 创建一张新表
     *
     * @param tableName 表的名字
     * @param columnNames 表的列名数组
     * @return true=创建成功，false=表已存在
     */
    @Override
    public boolean createTable(String tableName, String[] columnNames) {
        // 检查表是否已经存在
        if (tableSchema.containsKey(tableName)) {
            return false; // 表已存在，不能重复创建
        }

        // 保存表结构
        tableSchema.put(tableName, columnNames);

        // 在数据存储中为这张表分配空间
        dataStore.put(tableName, new LinkedHashMap<>());

        return true;
    }

    /**
     * 向表中插入一行数据
     *
     * 插入步骤：
     * 1. 检查表是否存在
     * 2. 检查值的数量是否和列数量匹配
     * 3. 以第一个值作为主键存储
     *
     * @param tableName 要插入的表名
     * @param values 要插入的数据值
     * @return true=插入成功，false=失败
     */
    @Override
    public boolean insert(String tableName, String[] values) {
        // 1. 检查表是否存在
        if (!tableSchema.containsKey(tableName)) {
            System.out.println("  [错误] 表 '" + tableName + "' 不存在！请先用 CREATE TABLE 创建表。");
            return false;
        }

        // 2. 检查值的数量是否匹配
        String[] columnNameArray = tableSchema.get(tableName);
        if (values.length != columnNameArray.length) {
            System.out.println("  [错误] 值的数量(" + values.length +
                    ")与列的数量(" + columnNameArray.length + ")不匹配！");
            return false;
        }

        // 3. 获取主键值（第一列的值作为主键）
        String primaryKeyValue = values[0];

        // 4. 获取该表的存储空间
        Map<String, String[]> tableData = dataStore.get(tableName);

        // 5. 存入数据
        tableData.put(primaryKeyValue, values);

        return true;
    }

    /**
     * 查询表中的数据
     *
     * 查询流程：
     * 1. 检查表是否存在
     * 2. 如果没有条件 → 返回所有行
     * 3. 如果有条件 → 找到条件列的位置，逐行匹配
     * 4. 组装结果返回
     *
     * @param tableName 表名
     * @param conditionColumn 按哪一列过滤（null=查所有）
     * @param conditionValue 过滤的值（null=查所有）
     * @return 查询结果的字符串表示
     */
    @Override
    public String query(String tableName, String conditionColumn, String conditionValue) {
        // 1. 检查表是否存在
        if (!tableSchema.containsKey(tableName)) {
            return "[错误] 表 '" + tableName + "' 不存在！";
        }

        String[] columnNameArray = tableSchema.get(tableName);
        Map<String, String[]> tableData = dataStore.get(tableName);

        // 2. 找到条件列在列名数组中的位置（如果有条件的话）
        int conditionColumnIndex = -1;
        if (conditionColumn != null && conditionValue != null) {
            for (int i = 0; i < columnNameArray.length; i++) {
                if (columnNameArray[i].equals(conditionColumn)) {
                    conditionColumnIndex = i;
                    break;
                }
            }
            if (conditionColumnIndex == -1) {
                return "[错误] 列 '" + conditionColumn + "' 不存在！";
            }
        }

        // 3. 遍历数据，查找匹配的行
        List<String[]> resultList = new ArrayList<>();

        for (Map.Entry<String, String[]> entry : tableData.entrySet()) {
            String[] row = entry.getValue();

            // 如果有条件，检查条件列的值是否匹配
            if (conditionColumnIndex >= 0) {
                if (row[conditionColumnIndex].equals(conditionValue)) {
                    resultList.add(row);
                }
            } else {
                // 没有条件，所有行都加入结果
                resultList.add(row);
            }
        }

        // 4. 组装返回结果
        StringBuilder sb = new StringBuilder();

        if (conditionColumn != null && conditionValue != null) {
            // 带条件的查询结果
            if (resultList.isEmpty()) {
                sb.append("未找到匹配 ").append(conditionColumn).append("=").append(conditionValue).append(" 的数据");
            } else {
                for (String[] row : resultList) {
                    sb.append(formatRow(columnNameArray, row)).append("\n");
                }
            }
        } else {
            // 无条件查询（返回所有行）
            sb.append("查询结果（共 ").append(resultList.size()).append(" 条）：\n");
            for (String[] row : resultList) {
                sb.append("  ").append(formatRow(columnNameArray, row)).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 获取表的所有列名
     *
     * @param tableName 表名
     * @return 列名数组，表不存在返回null
     */
    @Override
    public String[] getColumnNames(String tableName) {
        return tableSchema.get(tableName);
    }

    /**
     * 获取表中的行数
     *
     * @param tableName 表名
     * @return 行数，表不存在返回0
     */
    @Override
    public int getRowCount(String tableName) {
        Map<String, String[]> tableData = dataStore.get(tableName);
        return tableData != null ? tableData.size() : 0;
    }

    /**
     * 检查表是否存在
     *
     * @param tableName 表名
     * @return true=存在
     */
    @Override
    public boolean tableExists(String tableName) {
        return tableSchema.containsKey(tableName);
    }

    /**
     * 获取所有表名
     *
     * @return 表名数组
     */
    @Override
    public String[] getAllTableNames() {
        return tableSchema.keySet().toArray(new String[0]);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 将一行数据格式化为可读的字符串
     *
     * 例如：列名=["id","name","age"]，行=["100","张三","20"]
     *       → "[id=100, name=张三, age=20]"
     *
     * @param columnNames 列名数组
     * @param row 行数据
     * @return 格式化的字符串
     */
    private String formatRow(String[] columnNames, String[] row) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < columnNames.length; i++) {
            sb.append(columnNames[i]).append("=").append(row[i]);
            if (i < columnNames.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
