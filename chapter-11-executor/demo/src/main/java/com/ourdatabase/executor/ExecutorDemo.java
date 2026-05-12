package com.ourdatabase.executor;

import java.util.*;

/**
 * 查询执行引擎演示 —— 第11章主程序
 *
 * 演示执行计划如何被实际执行：
 * 1. 在内存中模拟表数据
 * 2. 解析简单SQL
 * 3. 执行查询并返回结果
 * 4. 展示全表扫描 vs 索引扫描
 */
public class ExecutorDemo {

    // 模拟数据库：表名 → (主键 → 行数据)
    static Map<String, Map<Integer, Map<String, String>>> database = new LinkedHashMap<>();
    // 模拟索引：表名 → (列名 → B+树结构)
    static Map<String, Map<String, Set<Integer>>> indexes = new HashMap<>();

    public static void main(String[] args) {
        printTitle("查询执行引擎演示");

        // 1. 初始化数据
        initData();

        // 2. 执行各种查询
        executeQuery("SELECT * FROM student");
        executeQuery("SELECT name, age FROM student WHERE id = 3");
        executeQuery("SELECT * FROM student WHERE age > 20");
        executeQuery("INSERT INTO student VALUES (6, '赵六', 22, '深圳')");
        executeQuery("SELECT * FROM student");
        executeQuery("DELETE FROM student WHERE id = 2");
        executeQuery("SELECT * FROM student");

        printTitle("执行总结");
        System.out.println("1. 执行引擎 = SQL解析 + 执行计划 + 存储引擎调用");
        System.out.println("2. 有索引时走索引扫描（O(log n)），无索引时走全表扫描（O(n)）");
        System.out.println("3. 过滤条件下推到存储引擎层执行");
    }

    /** 初始化模拟数据 */
    static void initData() {
        // 创建student表
        Map<Integer, Map<String, String>> studentTable = new LinkedHashMap<>();
        studentTable.put(1, createRow("id","1","name","张三","age","20","city","北京"));
        studentTable.put(2, createRow("id","2","name","李四","age","22","city","上海"));
        studentTable.put(3, createRow("id","3","name","王五","age","19","city","广州"));
        studentTable.put(4, createRow("id","4","name","孙七","age","23","city","杭州"));
        studentTable.put(5, createRow("id","5","name","周八","age","20","city","成都"));
        database.put("student", studentTable);

        // 创建id列索引
        Map<String, Set<Integer>> indexMap = new HashMap<>();
        indexMap.put("id", new TreeSet<>(Arrays.asList(1,2,3,4,5)));
        indexes.put("student", indexMap);
    }

    static Map<String,String> createRow(String... kv) {
        Map<String,String> row = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i+=2) row.put(kv[i], kv[i+1]);
        return row;
    }

    /** 执行一条SQL（简化处理） */
    static void executeQuery(String sql) {
        System.out.println("\nSQL> " + sql);
        System.out.println("-".repeat(40));

        String[] words = sql.split("\\s+");
        String operation = words[0].toUpperCase();

        try {
            switch (operation) {
                case "SELECT": doSelect(words); break;
                case "INSERT": doInsert(words); break;
                case "DELETE": doDelete(words); break;
                default: System.out.println("不支持的SQL操作");
            }
        } catch (Exception e) {
            System.out.println("执行错误: " + e.getMessage());
        }
    }

    static void doSelect(String[] words) {
        // 提取列名
        List<String> selectCols = new ArrayList<>();
        int i = 1;
        while (!words[i].equalsIgnoreCase("FROM")) {
            if (!words[i].equals(",")) selectCols.add(words[i]);
            i++;
        }
        // 表名
        String tableName = words[++i];

        // WHERE条件
        String condCol = null, condVal = null, condOp = "=";
        if (i + 3 < words.length && words[i+1].equalsIgnoreCase("WHERE")) {
            condCol = words[i+2];
            condOp = words[i+3];
            condVal = words[i+4];
        }

        // 检查索引
        boolean useIndex = false;
        if (condCol != null && indexes.containsKey(tableName)) {
            Map<String, Set<Integer>> tableIndex = indexes.get(tableName);
            if (tableIndex.containsKey(condCol) && condOp.equals("=")) {
                useIndex = true;
                System.out.println("  [索引扫描] 使用 " + condCol + " 列索引");
            }
        }

        if (!useIndex) System.out.println("  [全表扫描] 无可用索引");

        // 执行查询
        Map<Integer, Map<String, String>> table = database.get(tableName);
        if (table == null) { System.out.println("表不存在!"); return; }

        List<Map<String,String>> results = new ArrayList<>();
        for (Map.Entry<Integer, Map<String, String>> row : table.entrySet()) {
            Map<String,String> rowData = row.getValue();
            // 过滤条件
            if (condCol != null) {
                String actualVal = rowData.get(condCol);
                boolean match = false;
                switch (condOp) {
                    case "=": match = actualVal.equals(condVal); break;
                    case ">": match = Integer.parseInt(actualVal) > Integer.parseInt(condVal); break;
                    case "<": match = Integer.parseInt(actualVal) < Integer.parseInt(condVal); break;
                }
                if (!match) continue;
            }
            results.add(rowData);
        }

        // 打印结果
        System.out.println("  结果（" + results.size() + "行）：");
        for (Map<String,String> row : results) {
            StringBuilder sb = new StringBuilder("  | ");
            for (String col : (selectCols.contains("*") ? row.keySet() : selectCols)) {
                sb.append(col).append("=").append(row.get(col)).append(" | ");
            }
            System.out.println(sb);
        }
    }

    static void doInsert(String[] words) {
        String tableName = words[2];
        int pk = Integer.parseInt(words[5]);
        Map<String,String> rowData = new LinkedHashMap<>();
        // 简化：INSERT INTO student VALUES (6, '赵六', 22, '深圳')
        rowData.put("id", String.valueOf(pk));
        rowData.put("name", words[6]);
        rowData.put("age", words[7]);
        rowData.put("city", words[8]);

        database.get(tableName).put(pk, rowData);
        if (indexes.containsKey(tableName) && indexes.get(tableName).containsKey("id")) {
            indexes.get(tableName).get("id").add(pk);
        }
        System.out.println("  插入成功: id=" + pk);
    }

    static void doDelete(String[] words) {
        String tableName = words[2];
        int targetId = Integer.parseInt(words[6]);

        Map<Integer, Map<String, String>> table = database.get(tableName);
        if (table.remove(targetId) != null) {
            if (indexes.containsKey(tableName)) indexes.get(tableName).get("id").remove(targetId);
            System.out.println("  删除成功: id=" + targetId);
        } else {
            System.out.println("  未找到: id=" + targetId);
        }
    }

    static void printTitle(String t) { System.out.println("=".repeat(50) + "\n  " + t + "\n" + "=".repeat(50)); }
}
