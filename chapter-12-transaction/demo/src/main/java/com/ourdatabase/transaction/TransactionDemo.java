package com.ourdatabase.transaction;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 事务管理演示 —— 第12章主程序
 *
 * 模拟银行转账事务，演示ACID的核心概念：
 * 1. 原子性：转账要么全部完成，要么全部回滚
 * 2. 一致性：转账前后总余额不变
 * 3. 隔离性：多个事务并发执行互不干扰
 * 4. 持久性：事务提交后数据保存到"磁盘"
 */
public class TransactionDemo {

    public static void main(String[] args) {
        printTitle("事务管理演示 —— 银行转账场景");

        // ===== 场景1：正常转账 =====
        printStep("场景1：正常转账（张三 → 李四 100元）");
        BankAccount zhangsan = new BankAccount("张三", 1000);
        BankAccount lisi = new BankAccount("李四", 500);
        System.out.println("  转账前: " + zhangsan + ", " + lisi);

        TransactionManager txn = new TransactionManager();
        txn.begin();
        try {
            zhangsan.withdraw(100);
            lisi.deposit(100);
            txn.commit();
            System.out.println("  转账后: " + zhangsan + ", " + lisi);
            System.out.println("  ✓ 转账成功！总余额不变: " + (zhangsan.getBalance() + lisi.getBalance()));
        } catch (Exception e) {
            txn.rollback();
        }

        // ===== 场景2：转账失败回滚 =====
        printStep("场景2：转账失败回滚（余额不足）");
        System.out.println("  转账前: " + zhangsan + ", " + lisi);

        txn.begin();
        try {
            zhangsan.withdraw(2000); // 张三只有900元，余额不足！
            lisi.deposit(2000);
            txn.commit();
        } catch (Exception e) {
            System.out.println("  ✗ 转账失败: " + e.getMessage());
            txn.rollback();
        }
        System.out.println("  回滚后: " + zhangsan + ", " + lisi);
        System.out.println("  ✓ 回滚成功！数据恢复到事务开始前的状态");

        // ===== 场景3：并发事务隔离 =====
        printStep("场景3：并发事务隔离性演示");
        BankAccount wangwu = new BankAccount("王五", 100);

        // 事务A：读取余额
        TransactionManager txnA = new TransactionManager();
        txnA.begin();
        int txnARead = wangwu.getBalance();
        System.out.println("  事务A读到王五余额: " + txnARead);

        // 事务B：修改余额
        TransactionManager txnB = new TransactionManager();
        txnB.begin();
        wangwu.deposit(50); // 余额变成150
        txnB.commit();

        // 事务A再次读取：应该读到自己的快照值100（隔离性）
        int txnAReadAgain = wangwu.getBalance();
        System.out.println("  事务B提交后，事务A再次读到王五余额: " + txnAReadAgain);
        System.out.println("  （在没有MVCC的情况下，事务A可能读到150而不是100）");
        txnA.commit();

        printTitle("演示总结");
        System.out.println("1. 事务 = BEGIN + 操作 + COMMIT/ROLLBACK");
        System.out.println("2. 原子性：回滚能撤销事务中的所有操作");
        System.out.println("3. 隔离性需要锁或MVCC机制（第14章）");
        System.out.println("4. 持久性需要日志系统（第13章）");
    }

    static void printStep(String s) { System.out.println("\n--- " + s + " ---"); }
    static void printTitle(String t) { System.out.println("=".repeat(50) + "\n  " + t + "\n" + "=".repeat(50)); }
}

/** 银行账户（简化版） */
class BankAccount {
    String name;
    int balance;

    BankAccount(String name, int initialBalance) { this.name = name; this.balance = initialBalance; }

    void deposit(int amount) { this.balance += amount; }

    void withdraw(int amount) {
        if (amount > balance) throw new RuntimeException("余额不足！需要" + amount + "，只有" + balance);
        this.balance -= amount;
    }

    int getBalance() { return balance; }

    @Override
    public String toString() { return name + "(" + balance + "元)"; }
}

/** 事务管理器（简化版，含Undo日志） */
class TransactionManager {
    private static final AtomicLong idGenerator = new AtomicLong(1);
    private Long txnId;
    private List<Runnable> undoActions = new ArrayList<>();

    void begin() {
        txnId = idGenerator.getAndIncrement();
        undoActions.clear();
        System.out.println("  [事务" + txnId + "] BEGIN");
    }

    void commit() {
        System.out.println("  [事务" + txnId + "] COMMIT ✓");
        undoActions.clear();
    }

    void rollback() {
        System.out.println("  [事务" + txnId + "] ROLLBACK（执行 " + undoActions.size() + " 步回滚操作）");
        // 逆向执行回滚操作（后进先出）
        for (int i = undoActions.size() - 1; i >= 0; i--) {
            undoActions.get(i).run();
        }
        undoActions.clear();
    }

    void recordUndo(Runnable action) { undoActions.add(action); }
}
