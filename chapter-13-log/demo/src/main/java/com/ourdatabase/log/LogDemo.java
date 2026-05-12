package com.ourdatabase.log;

import java.util.*;

/**
 * 日志系统演示 —— 第13章主程序
 *
 * 演示 WAL（Write-Ahead Logging）机制：
 * 1. Redo日志：记录"做了什么修改"，崩溃后重放恢复数据
 * 2. Undo日志：记录"修改前的旧值"，回滚时恢复旧数据
 * 3. WAL原则：先写日志，再写数据
 *
 * 崩溃恢复流程：
 * - 分析日志 → 确定哪些事务已提交/未提交
 * - Redo：重放已提交事务的操作（确保持久性）
 * - Undo：回滚未提交事务的操作（确保原子性）
 */
public class LogDemo {

    public static void main(String[] args) {
        printTitle("Redo/Undo日志系统演示");

        // ===== 场景1：正常流程 =====
        printStep("场景1：正常事务流程（WAL原则）");
        LogManager log = new LogManager();

        System.out.println("  事务T1开始，修改 id=1 的 name 从 '张三' → '张三丰'");

        log.writeUndo("T1", "student", "1", "name", "张三");     // 先写Undo（旧值）
        log.writeRedo("T1", "student", "1", "name", "张三丰");  // 写Redo（新值）
        System.out.println("  [WAL] Redo/Undo日志已写入（在数据修改之前！）");

        // 模拟修改内存数据
        Map<String,String> data = new HashMap<>();
        data.put("id","1"); data.put("name","张三丰");
        System.out.println("  [数据] 内存数据已修改");
        log.writeCommit("T1");
        System.out.println("  [提交] 事务T1已提交");

        // ===== 场景2：崩溃恢复 =====
        printStep("场景2：模拟崩溃恢复");
        System.out.println("  *** 系统崩溃！***");

        // 模拟恢复
        System.out.println("  重启后，分析日志文件...");
        System.out.println("  发现事务T1：有COMMIT标记 → 已提交");

        System.out.println("  执行Redo重放：");
        System.out.println("    Redo: student.id=1.name = '张三丰'（重放修改）");
        System.out.println("    -> 数据已恢复！");

        System.out.println("  发现事务T2：无COMMIT标记 → 未提交");
        System.out.println("  执行Undo回滚：");
        System.out.println("    Undo: student.id=2.city = '上海'（恢复旧值）");
        System.out.println("    -> 未提交修改已回滚！");

        // ===== 场景3：实际操作日志 =====
        printStep("场景3：实际操作日志演示");
        SimpleLog simpleLog = new SimpleLog();

        simpleLog.begin(1);
        simpleLog.recordOp(1, "UPDATE student SET name='李四丰' WHERE id=2");
        simpleLog.recordOp(1, "UPDATE student SET age=25 WHERE id=2");
        simpleLog.commit(1);

        simpleLog.begin(2);
        simpleLog.recordOp(2, "INSERT INTO student VALUES (6,'赵六',20)");
        // 事务2没有提交就"崩溃"了

        System.out.println("\n  当前日志文件内容：");
        simpleLog.printLog();

        System.out.println("\n  崩溃恢复：");
        simpleLog.crashRecovery();

        printTitle("演示总结");
        System.out.println("1. WAL = 先写日志，再写数据（保证崩溃可恢复）");
        System.out.println("2. Redo = 重放已提交事务，确保不丢数据（持久性）");
        System.out.println("3. Undo = 回滚未提交事务，确保原子性");
        System.out.println("4. COMMIT标记 = 判断事务是否已提交的关键");
    }

    static void printStep(String s) { System.out.println("\n--- " + s + " ---"); }
    static void printTitle(String t) { System.out.println("=".repeat(50) + "\n  " + t + "\n" + "=".repeat(50)); }
}

/** 简易日志管理器（Redo + Undo） */
class LogManager {
    List<String> redoLog = new ArrayList<>();
    List<String> undoLog = new ArrayList<>();
    Set<String> committedTxns = new HashSet<>();

    void writeRedo(String txnId, String table, String pk, String col, String newVal) {
        redoLog.add(String.format("REDO[%s]: %s.%s.%s='%s'", txnId, table, pk, col, newVal));
    }

    void writeUndo(String txnId, String table, String pk, String col, String oldVal) {
        undoLog.add(String.format("UNDO[%s]: %s.%s.%s='%s'", txnId, table, pk, col, oldVal));
    }

    void writeCommit(String txnId) {
        redoLog.add(String.format("COMMIT[%s]", txnId));
        committedTxns.add(txnId);
    }
}

/** 简易日志（用于场景3的实际操作） */
class SimpleLog {
    static class LogEntry {
        int txnId; String operation; boolean committed;
        LogEntry(int t, String op, boolean c) { txnId=t; operation=op; committed=c; }
        @Override public String toString() { return (committed ? "[COMMIT]" : "[ACTIVE]") + " T" + txnId + ": " + operation; }
    }

    List<LogEntry> entries = new ArrayList<>();
    Set<Integer> activeTxns = new HashSet<>();

    void begin(int id) { activeTxns.add(id); System.out.println("  T" + id + " BEGIN"); }
    void recordOp(int id, String op) { entries.add(new LogEntry(id, op, false)); System.out.println("  T" + id + ": " + op); }
    void commit(int id) { entries.add(new LogEntry(id, "COMMIT", true)); activeTxns.remove(id); System.out.println("  T" + id + " COMMIT"); }

    void printLog() {
        for (LogEntry e : entries) System.out.println("  " + e);
    }

    void crashRecovery() {
        System.out.println("  === 开始崩溃恢复 ===");
        // Redo：重放所有COMMIT的操作
        for (LogEntry e : entries) {
            if (e.committed) {
                System.out.println("  [Redo] 重放: " + e.operation);
            }
        }
        // Undo：回滚所有未提交的操作
        for (int i = entries.size()-1; i >= 0; i--) {
            LogEntry e = entries.get(i);
            if (!e.committed && e.operation.contains("INSERT")) {
                System.out.println("  [Undo] 回滚: DELETE " + e.operation.substring(e.operation.indexOf("INTO")+5));
            }
        }
        System.out.println("  === 恢复完成 ===");
    }
}
