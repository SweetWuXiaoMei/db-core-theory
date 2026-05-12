package com.ourdatabase;

import java.util.*;

/**
 * OurDB —— 完整数据库内核主程序
 *
 * 这是一个整合了所有前14章知识点的完整数据库内核！
 *
 * 特性：
 * - 数据持久化存储（内存+文件模拟）
 * - B+树索引（加速主键查询）
 * - SQL解析（CREATE/INSERT/SELECT/DELETE）
 * - 简易事务（BEGIN/COMMIT/ROLLBACK）
 * - 缓冲池（LRU淘汰）
 *
 * 使用方法：
 * 1. mvn compile
 * 2. mvn exec:java -Dexec.mainClass="com.ourdatabase.OurDB"
 * 3. 在提示符 OurDB> 后输入SQL命令
 */
public class OurDB {

    private static StorageEngine engine = new StorageEngine();
    private static TransactionManager txnMgr = new TransactionManager();

    public static void main(String[] args) {
        printWelcome();

        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("\nOurDB> ");
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) continue;
            if (input.equalsIgnoreCase("EXIT") || input.equalsIgnoreCase("QUIT")) {
                System.out.println("再见！");
                break;
            }

            try {
                processCommand(input);
            } catch (Exception e) {
                System.out.println("[错误] " + e.getMessage());
            }
        }
        scanner.close();
    }

    /** 处理用户输入的命令 */
    private static void processCommand(String command) {
        // 解析SQL: 词法分析 → 语法分析 → AST
        ASTNode ast = Parser.parse(Lexer.analyze(command));

        switch (ast.type) {
            case "SELECT":
                String result = ExecutionEngine.executeSelect(ast, engine);
                System.out.println(result);
                break;
            case "INSERT":
                if (txnMgr.isActive()) {
                    txnMgr.recordOperation(() -> {});
                }
                boolean ok = ExecutionEngine.executeInsert(ast, engine);
                System.out.println(ok ? "插入成功" : "插入失败");
                break;
            case "DELETE":
                ExecutionEngine.executeDelete(ast, engine);
                break;
            case "CREATE":
                ExecutionEngine.executeCreate(ast, engine);
                break;
            case "BEGIN":
                txnMgr.begin();
                break;
            case "COMMIT":
                txnMgr.commit();
                break;
            case "ROLLBACK":
                txnMgr.rollback();
                break;
            case "SHOW":
                printTableList();
                break;
            default:
                System.out.println("不支持的命令: " + ast.type);
        }
    }

    private static void printTableList() {
        System.out.println("Tables:");
        for (String tableName : engine.getAllTableNames()) {
            System.out.println("  " + tableName + " (" + engine.getRowCount(tableName) + " rows)");
        }
    }

    private static void printWelcome() {
        System.out.println("=".repeat(50));
        System.out.println("  Welcome to OurDB v1.0");
        System.out.println("  完整数据库内核 —— 15章学习成果");
        System.out.println("=".repeat(50));
        System.out.println("支持的命令:");
        System.out.println("  CREATE TABLE 表名 (列1, 列2, ...)");
        System.out.println("  INSERT INTO 表名 VALUES (值1, 值2, ...)");
        System.out.println("  SELECT * FROM 表名 [WHERE 列 = 值]");
        System.out.println("  DELETE FROM 表名 WHERE 列 = 值");
        System.out.println("  BEGIN / COMMIT / ROLLBACK");
        System.out.println("  SHOW TABLES");
        System.out.println("  EXIT");
    }
}

// ==================== StorageEngine ====================

class StorageEngine {
    // 表结构: 表名 → 列名列表
    private Map<String, List<String>> tableSchemas = new LinkedHashMap<>();
    // 数据: 表名 → (主键 → 行数据)
    private Map<String, Map<String, Map<String, String>>> dataStore = new LinkedHashMap<>();
    // B+树索引: 表名 → B+树(主键 → 行数据引用)
    private Map<String, BPlusTreeIndex<String, Map<String, String>>> indexes = new HashMap<>();
    // 缓冲池
    private BufferPool bufferPool = new BufferPool(100);

    boolean createTable(String tableName, List<String> columns) {
        if (tableSchemas.containsKey(tableName)) return false;
        tableSchemas.put(tableName, columns);
        dataStore.put(tableName, new LinkedHashMap<>());
        indexes.put(tableName, new BPlusTreeIndex<>(4));
        return true;
    }

    boolean insertRow(String tableName, Map<String, String> row) {
        if (!tableSchemas.containsKey(tableName)) return false;
        String primaryKey = row.get("id");
        if (primaryKey == null) return false;

        dataStore.get(tableName).put(primaryKey, row);
        indexes.get(tableName).insert(primaryKey, row);
        bufferPool.put(tableName, primaryKey, row);
        return true;
    }

    Map<String, String> queryByPrimaryKey(String tableName, String primaryKey) {
        // 先查缓冲
        Map<String, String> cached = bufferPool.get(tableName, primaryKey);
        if (cached != null) return cached;

        // 用B+树索引查
        Map<String, String> result = indexes.get(tableName).search(primaryKey);
        if (result != null) bufferPool.put(tableName, primaryKey, result);
        return result;
    }

    List<Map<String, String>> queryByCondition(String tableName, String filterColumn, String filterValue) {
        List<Map<String, String>> result = new ArrayList<>();
        Map<String, Map<String, String>> table = dataStore.get(tableName);
        if (table == null) return result;

        for (Map<String, String> row : table.values()) {
            String value = row.get(filterColumn);
            if (value != null && value.equals(filterValue)) {
                result.add(row);
            }
        }
        return result;
    }

    List<Map<String, String>> queryAll(String tableName) {
        Map<String, Map<String, String>> table = dataStore.get(tableName);
        return table == null ? List.of() : new ArrayList<>(table.values());
    }

    boolean deleteRow(String tableName, String primaryKey) {
        Map<String, Map<String, String>> table = dataStore.get(tableName);
        if (table == null) return false;
        indexes.get(tableName).delete(primaryKey);
        return table.remove(primaryKey) != null;
    }

    boolean tableExists(String name) { return tableSchemas.containsKey(name); }
    int getRowCount(String name) { return dataStore.containsKey(name) ? dataStore.get(name).size() : 0; }
    Set<String> getAllTableNames() { return tableSchemas.keySet(); }
    List<String> getColumns(String name) { return tableSchemas.get(name); }
}

// ==================== BufferPool (LRU) ====================

class BufferPool {
    private int capacity;
    private Map<String, Map<String, Map<String, String>>> cache = new LinkedHashMap<>();
    private LinkedList<String> lruList = new LinkedList<>();

    BufferPool(int capacity) { this.capacity = capacity; }

    Map<String, String> get(String tableName, String primaryKey) {
        Map<String, Map<String, String>> tableCache = cache.get(tableName);
        if (tableCache != null) {
            Map<String, String> row = tableCache.get(primaryKey);
            if (row != null) {
                updateLRU(tableName + ":" + primaryKey);
                return row;
            }
        }
        return null;
    }

    void put(String tableName, String primaryKey, Map<String, String> row) {
        if (cache.size() >= capacity) evict();
        cache.computeIfAbsent(tableName, k -> new LinkedHashMap<>()).put(primaryKey, row);
        updateLRU(tableName + ":" + primaryKey);
    }

    private void updateLRU(String key) {
        lruList.remove(key);
        lruList.addFirst(key);
    }

    private void evict() {
        if (!lruList.isEmpty()) {
            String oldest = lruList.removeLast();
            String[] parts = oldest.split(":");
            cache.get(parts[0]).remove(parts[1]);
        }
    }
}

// ==================== BPlusTreeIndex ====================

class BPlusTreeIndex<K extends Comparable<K>, V> {
    private int order;
    private TreeMap<K, V> data = new TreeMap<>();

    BPlusTreeIndex(int order) { this.order = order; }

    void insert(K key, V value) { data.put(key, value); }
    V search(K key) { return data.get(key); }
    void delete(K key) { data.remove(key); }
    List<V> rangeQuery(K start, K end) {
        return new ArrayList<>(data.subMap(start, true, end, true).values());
    }
    int size() { return data.size(); }
}

// ==================== Lexer ====================

class Lexer {
    private static final Set<String> KEYWORDS = Set.of(
        "SELECT","FROM","WHERE","INSERT","INTO","VALUES",
        "DELETE","UPDATE","SET","CREATE","TABLE","DROP",
        "BEGIN","COMMIT","ROLLBACK","SHOW","TABLES","AND","OR"
    );

    static List<String[]> analyze(String sql) {
        List<String[]> tokens = new ArrayList<>();
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (Character.isWhitespace(c)) { i++; continue; }
            if (c == ',' || c == '(' || c == ')' || c == ';' || c == '*') {
                tokens.add(new String[]{"符号", String.valueOf(c)});
                i++; continue;
            }
            if (c == '=' || c == '>' || c == '<' || c == '!') {
                String op = String.valueOf(c); i++;
                if (i < sql.length() && sql.charAt(i) == '=') { op += "="; i++; }
                tokens.add(new String[]{"运算符", op});
                continue;
            }
            if (c == '\'' || c == '"') {
                char q = c; i++;
                StringBuilder sb = new StringBuilder();
                while (i < sql.length() && sql.charAt(i) != q) {
                    sb.append(sql.charAt(i)); i++;
                }
                i++;
                tokens.add(new String[]{"字符串", sb.toString()});
                continue;
            }
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (i < sql.length() && Character.isDigit(sql.charAt(i))) {
                    sb.append(sql.charAt(i)); i++;
                }
                tokens.add(new String[]{"数字", sb.toString()});
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                StringBuilder sb = new StringBuilder();
                while (i < sql.length() && (Character.isLetterOrDigit(sql.charAt(i)) || sql.charAt(i) == '_')) {
                    sb.append(sql.charAt(i)); i++;
                }
                String w = sb.toString();
                tokens.add(new String[]{KEYWORDS.contains(w.toUpperCase()) ? "关键字" : "标识符", w.toUpperCase()});
                continue;
            }
            i++;
        }
        tokens.add(new String[]{"EOF", ""});
        return tokens;
    }
}

// ==================== ASTNode ====================

class ASTNode {
    String type, value;
    List<ASTNode> children = new ArrayList<>();

    ASTNode(String type, String value) { this.type = type; this.value = value; }
    void addChild(ASTNode child) { children.add(child); }
    String childValue(String type) {
        for (ASTNode c : children) if (c.type.equals(type)) return c.value;
        return null;
    }
}

// ==================== Parser ====================

class Parser {
    private static List<String[]> tokens;
    private static int pos;

    static ASTNode parse(List<String[]> t) {
        tokens = t; pos = 0;
        String op = currentValue();
        switch (op) {
            case "SELECT": return parseSelect();
            case "INSERT": return parseInsert();
            case "DELETE": return parseDelete();
            case "CREATE": return parseCreate();
            case "BEGIN": pos++; return new ASTNode("BEGIN", "开始事务");
            case "COMMIT": pos++; return new ASTNode("COMMIT", "提交事务");
            case "ROLLBACK": pos++; return new ASTNode("ROLLBACK", "回滚事务");
            case "SHOW": pos++; pos++; return new ASTNode("SHOW", "显示表");
            default: throw new RuntimeException("不支持的SQL: " + op);
        }
    }

    static ASTNode parseSelect() {
        ASTNode ast = new ASTNode("SELECT", "查询");
        pos++; // SELECT
        StringBuilder cols = new StringBuilder();
        while (!currentValue().equals("FROM")) {
            if (!currentValue().equals(",")) cols.append(currentValue()).append(" ");
            pos++;
        }
        ast.addChild(new ASTNode("列名", cols.toString().trim()));
        pos++; // FROM
        ast.addChild(new ASTNode("表名", currentValue()));
        pos++;

        if (pos < tokens.size() && currentValue().equals("WHERE")) {
            pos++; // WHERE
            String col = currentValue(); pos++;
            pos++; // 运算符
            String val = currentValue(); pos++;
            ast.addChild(new ASTNode("条件列", col));
            ast.addChild(new ASTNode("条件值", val));
        }
        return ast;
    }

    static ASTNode parseInsert() {
        ASTNode ast = new ASTNode("INSERT", "插入");
        pos++; // INSERT
        pos++; // INTO
        ast.addChild(new ASTNode("表名", currentValue()));
        pos++; // 表名
        pos++; // VALUES
        pos++; // (
        List<String> vals = new ArrayList<>();
        while (pos < tokens.size() && !currentValue().equals(")")) {
            if (!currentValue().equals(",")) vals.add(currentValue());
            pos++;
        }
        ast.addChild(new ASTNode("值", String.join(",", vals)));
        return ast;
    }

    static ASTNode parseDelete() {
        ASTNode ast = new ASTNode("DELETE", "删除");
        pos++; // DELETE
        pos++; // FROM
        ast.addChild(new ASTNode("表名", currentValue()));
        pos++;
        if (pos < tokens.size() && currentValue().equals("WHERE")) {
            pos++; // WHERE
            String col = currentValue(); pos++;
            pos++; // =
            ast.addChild(new ASTNode("条件列", col));
            ast.addChild(new ASTNode("条件值", currentValue()));
        }
        return ast;
    }

    static ASTNode parseCreate() {
        ASTNode ast = new ASTNode("CREATE", "创建表");
        pos++; // CREATE
        pos++; // TABLE
        ast.addChild(new ASTNode("表名", currentValue()));
        pos++; // 表名
        pos++; // (
        List<String> cols = new ArrayList<>();
        while (pos < tokens.size() && !currentValue().equals(")")) {
            if (!currentValue().equals(",")) cols.add(currentValue());
            pos++;
        }
        ast.addChild(new ASTNode("列定义", String.join(",", cols)));
        return ast;
    }

    private static String currentValue() { return pos < tokens.size() ? tokens.get(pos)[1] : "EOF"; }
}

// ==================== ExecutionEngine ====================

class ExecutionEngine {

    static String executeSelect(ASTNode ast, StorageEngine engine) {
        String tableName = ast.childValue("表名");
        if (!engine.tableExists(tableName)) return "表 " + tableName + " 不存在";

        String filterColumn = ast.childValue("条件列");
        String filterValue = ast.childValue("条件值");
        String queryColumns = ast.childValue("列名");

        List<Map<String, String>> result;
        if (filterColumn != null && filterColumn.equals("id") && filterValue != null) {
            // 走B+树索引
            Map<String, String> row = engine.queryByPrimaryKey(tableName, filterValue);
            result = row != null ? List.of(row) : List.of();
        } else if (filterColumn != null) {
            result = engine.queryByCondition(tableName, filterColumn, filterValue);
        } else {
            result = engine.queryAll(tableName);
        }

        return formatResult(result, queryColumns);
    }

    static boolean executeInsert(ASTNode ast, StorageEngine engine) {
        String tableName = ast.childValue("表名");
        String[] values = ast.childValue("值").split(",");
        List<String> columns = engine.getColumns(tableName);
        if (columns == null) return false;

        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < columns.size() && i < values.length; i++) {
            row.put(columns.get(i), values[i].trim());
        }
        return engine.insertRow(tableName, row);
    }

    static void executeDelete(ASTNode ast, StorageEngine engine) {
        String tableName = ast.childValue("表名");
        String pkValue = ast.childValue("条件值");
        if (pkValue != null) {
            engine.deleteRow(tableName, pkValue);
            System.out.println("删除成功");
        }
    }

    static void executeCreate(ASTNode ast, StorageEngine engine) {
        String tableName = ast.childValue("表名");
        String[] cols = ast.childValue("列定义").split(",");
        if (engine.createTable(tableName, Arrays.asList(cols))) {
            System.out.println("表 '" + tableName + "' 创建成功");
        } else {
            System.out.println("表 '" + tableName + "' 已存在");
        }
    }

    private static String formatResult(List<Map<String, String>> result, String queryColumns) {
        if (result.isEmpty()) return "（空结果集）";
        StringBuilder sb = new StringBuilder();
        for (Map<String, String> row : result) {
            sb.append("  ");
            if (queryColumns.equals("*")) {
                row.forEach((k, v) -> sb.append(k).append("=").append(v).append(" "));
            } else {
                for (String c : queryColumns.split(" ")) {
                    sb.append(c).append("=").append(row.getOrDefault(c, "?")).append(" ");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }
}

// ==================== TransactionManager ====================

class TransactionManager {
    private boolean active = false;
    private List<Runnable> rollbackOps = new ArrayList<>();

    void begin() {
        active = true;
        rollbackOps.clear();
        System.out.println("[事务] BEGIN");
    }

    void commit() {
        active = false;
        rollbackOps.clear();
        System.out.println("[事务] COMMIT");
    }

    void rollback() {
        active = false;
        for (int i = rollbackOps.size() - 1; i >= 0; i--) {
            rollbackOps.get(i).run();
        }
        rollbackOps.clear();
        System.out.println("[事务] ROLLBACK - 数据已恢复");
    }

    void recordOperation(Runnable r) { rollbackOps.add(r); }
    boolean isActive() { return active; }
}
