package com.ourdatabase.core;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 事务管理器 —— 模拟数据库的事务控制
 *
 * 事务的核心概念：一组操作要么全部成功，要么全部失败。
 *
 * 事务的四个特性（ACID）：
 * A - 原子性（Atomicity）：事务不可分割，要么全做，要么全不做
 * C - 一致性（Consistency）：事务前后，数据都满足业务规则
 * I - 隔离性（Isolation）：多个事务同时执行时互不干扰
 * D - 持久性（Durability）：事务提交后，数据永久保存
 *
 * 生活类比：银行转账
 * - 张三给李四转100元
 * - 步骤1：张三账户 -100
 * - 步骤2：李四账户 +100
 * - 如果步骤1成功但步骤2失败，张三的钱就凭空消失了！
 * - 事务保证：要么两步都成功，要么都失败回滚
 *
 * 第12章会详细学习事务的实现原理。
 * 本章只需理解事务的基本操作：开始、提交、回滚。
 */
public class TransactionManager {

    // 生成全局唯一的事务ID（原子操作，线程安全）
    private static final AtomicLong txnIdGenerator = new AtomicLong(1000);

    // 当前活动的事务ID
    private Long currentTxnId = null;

    // 事务是否处于活动状态
    private boolean txnActive = false;

    /**
     * 开始一个新事务
     *
     * 在真实数据库中，"开始事务"会做以下工作：
     * - 分配一个全局唯一的事务ID
     * - 记录当前时间戳
     * - 初始化事务的私有工作区（事务修改的数据先放在这里）
     * - 获取必要的锁资源
     * - 开始记录Undo日志（以便回滚时恢复数据）
     */
    public void beginTransaction() {
        if (txnActive) {
            System.out.println("  [警告] 已经有一个活动的事务（ID=" + currentTxnId + "），请先提交或回滚");
            return;
        }
        currentTxnId = txnIdGenerator.incrementAndGet();
        txnActive = true;
        System.out.println("  -> 事务已开始（事务ID=" + currentTxnId + "）");
        System.out.println("  -> 初始化事务私有工作区...");
        System.out.println("  -> 开始记录Undo日志...");
    }

    /**
     * 提交事务
     *
     * "提交"的意思是：确认这个事务中所有的修改都是有效的。
     * 提交后数据将被持久化，其他事务可以看到这些修改。
     *
     * 提交过程（简化版）：
     * 1. 将修改写入Redo日志（第13章）
     * 2. 将数据持久化到磁盘
     * 3. 写入COMMIT标记到日志
     * 4. 释放锁资源（第14章）
     * 5. 清理事务私有工作区
     */
    public void commitTransaction() {
        if (!txnActive) {
            System.out.println("  [警告] 当前没有活动的事务可以提交");
            return;
        }
        System.out.println("  -> 将事务修改写入Redo日志...");
        System.out.println("  -> 将数据持久化到磁盘...");
        System.out.println("  -> 写入COMMIT标记到日志...");
        System.out.println("  -> 释放锁资源...");
        txnActive = false;
        System.out.println("  -> 事务提交成功！（事务ID=" + currentTxnId + "）");
    }

    /**
     * 回滚事务
     *
     * "回滚"的意思是：撤销这个事务中所有的修改，恢复到事务开始前的状态。
     *
     * 什么时候需要回滚？
     * - 转账时，扣钱成功了但加钱失败了 -> 回滚，钱回到原账户
     * - 用户主动取消操作
     * - 数据库检测到死锁
     *
     * 回滚过程（简化版）：
     * 1. 根据Undo日志，将修改过的数据恢复成旧值
     * 2. 写入ROLLBACK标记到日志
     * 3. 释放锁资源
     * 4. 清理事务私有工作区
     */
    public void rollbackTransaction() {
        if (!txnActive) {
            System.out.println("  [警告] 当前没有活动的事务可以回滚");
            return;
        }
        System.out.println("  -> 根据Undo日志恢复旧数据...");
        System.out.println("  -> 写入ROLLBACK标记到日志...");
        System.out.println("  -> 释放锁资源...");
        txnActive = false;
        System.out.println("  -> 事务回滚完成！（事务ID=" + currentTxnId + "）");
        System.out.println("  -> 数据已恢复到事务开始前的状态");
    }

    /**
     * 获取当前事务ID
     */
    public long getCurrentTransactionId() {
        return currentTxnId != null ? currentTxnId : -1;
    }

    /**
     * 检查当前是否有活动的事务
     */
    public boolean hasActiveTransaction() {
        return txnActive;
    }
}
