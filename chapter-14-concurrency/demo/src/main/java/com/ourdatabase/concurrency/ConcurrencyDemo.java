package com.ourdatabase.concurrency;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

/**
 * 并发控制演示 —— 第14章主程序
 *
 * 演示三种并发控制机制：
 * 1. 锁机制（读锁/写锁、行锁/表锁）
 * 2. MVCC（多版本并发控制）
 * 3. 死锁的产生和检测
 */
public class ConcurrencyDemo {

    public static void main(String[] args) throws Exception {
        printTitle("并发控制演示");

        // ===== 1. 锁机制演示 =====
        demoLocking();

        // ===== 2. MVCC演示 =====
        demoMVCC();

        // ===== 3. 死锁演示 =====
        demoDeadlock();

        printTitle("演示总结");
        System.out.println("1. 锁保证并发操作的数据正确性");
        System.out.println("2. 读锁（S锁）不互斥，写锁（X锁）排他");
        System.out.println("3. MVCC让读不阻塞写，大幅提升并发性能");
        System.out.println("4. 死锁需要检测和处理（一般回滚其中一个事务）");
    }

    /** 锁机制：读锁和写锁的基本使用 */
    static void demoLocking() throws Exception {
        printStep("1. 锁机制：读写锁");

        ReadWriteLock lock = new ReentrantReadWriteLock();
        List<String> log = Collections.synchronizedList(new ArrayList<>());

        // 启动两个线程并发读写
        Thread readThread1 = new Thread(() -> {
            lock.readLock().lock();
            log.add("读线程1: 获取读锁，开始读取数据...");
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            log.add("读线程1: 读取完成，释放读锁");
            lock.readLock().unlock();
        });

        Thread readThread2 = new Thread(() -> {
            lock.readLock().lock();
            log.add("读线程2: 获取读锁，开始读取数据...（和读线程1可以并发！）");
            try { Thread.sleep(300); } catch (InterruptedException e) {}
            log.add("读线程2: 读取完成，释放读锁");
            lock.readLock().unlock();
        });

        Thread writeThread = new Thread(() -> {
            try { Thread.sleep(100); } catch (InterruptedException e) {}
            lock.writeLock().lock();
            log.add("写线程: 获取写锁（独占了！），开始修改数据...");
            try { Thread.sleep(500); } catch (InterruptedException e) {}
            log.add("写线程: 修改完成，释放写锁");
            lock.writeLock().unlock();
        });

        readThread1.start(); readThread2.start(); writeThread.start();
        readThread1.join(); readThread2.join(); writeThread.join();

        for (String s : log) System.out.println("  " + s);
        System.out.println("  → 注意：两个读线程可以同时执行（读锁共享）");
        System.out.println("  → 注意：写线程必须等待（写锁排他）");
    }

    /** 简易MVCC：用版本号实现快照读 */
    static void demoMVCC() {
        printStep("2. MVCC：多版本并发控制");

        SimpleMVCC mvcc = new SimpleMVCC();

        // 事务1：开始快照读
        mvcc.begin(1);
        System.out.println("  事务1 快照读: name=" + mvcc.snapshotRead(1));

        // 事务2：修改数据
        mvcc.begin(2);
        mvcc.write(2, "id=1", "新名字");
        mvcc.commit(2);
        System.out.println("  事务2 修改 name → '新名字' 并提交");

        // 事务1再次快照读：应该还是旧值！
        System.out.println("  事务1 再次快照读: name=" + mvcc.snapshotRead(1));
        System.out.println("  → MVCC核心：事务1读到的是旧版本（快照），不受事务2影响！");

        // 新事务3读：应该看到最新值
        mvcc.begin(3);
        System.out.println("  事务3 快照读: name=" + mvcc.snapshotRead(3));
        System.out.println("  → 新事务看到的是最新版本");
    }

    /** 死锁产生和检测 */
    static void demoDeadlock() {
        printStep("3. 死锁演示");

        Object resourceA = new Object();
        Object resourceB = new Object();

        Thread thread1 = new Thread(() -> {
            synchronized (resourceA) {
                System.out.println("  线程1: 获取资源A");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (resourceB) {
                    System.out.println("  线程1: 获取资源B（成功）");
                }
            }
        });

        Thread thread2 = new Thread(() -> {
            synchronized (resourceB) {
                System.out.println("  线程2: 获取资源B");
                try { Thread.sleep(100); } catch (InterruptedException e) {}
                synchronized (resourceA) {
                    System.out.println("  线程2: 获取资源A（成功）");
                }
            }
        });

        thread1.start(); thread2.start();
        try { Thread.sleep(2000); thread1.interrupt(); thread2.interrupt(); } catch (Exception e) {}

        System.out.println("  → 死锁形成：线程1等资源B，线程2等资源A，互相等待");
        System.out.println("  → 数据库解决方案：死锁检测器，回滚其中一个事务");
    }

    static void printStep(String s) { System.out.println("\n--- " + s + " ---"); }
    static void printTitle(String t) { System.out.println("=".repeat(50) + "\n  " + t + "\n" + "=".repeat(50)); }
}

/** 简易MVCC实现（版本链） */
class SimpleMVCC {
    // 数据版本链：key → 版本列表（每个版本包含值+事务ID）
    Map<String, List<Version>> versionChain = new HashMap<>();
    Map<Integer, Integer> txnSnapshot = new HashMap<>();
    int globalVersion = 0;

    SimpleMVCC() {
        List<Version> v = new ArrayList<>();
        v.add(new Version(0, "张三"));
        versionChain.put("id=1", v);
    }

    void begin(int tid) {
        txnSnapshot.put(tid, globalVersion);
    }

    String snapshotRead(int tid) {
        int snapshotVer = txnSnapshot.get(tid);
        List<Version> history = versionChain.get("id=1");
        // 找到小于等于快照版本的最新版本
        for (int i = history.size()-1; i >= 0; i--) {
            if (history.get(i).version <= snapshotVer) {
                return history.get(i).value;
            }
        }
        return "无数据";
    }

    void write(int tid, String key, String newVal) {
        List<Version> history = versionChain.get(key);
        if (history == null) { history = new ArrayList<>(); versionChain.put(key, history); }
        history.add(new Version(tid, newVal));
    }

    void commit(int tid) {
        globalVersion = tid; // 事务ID就是新的全局版本号
    }

    static class Version {
        int version; String value;
        Version(int v, String s) { version=v; value=s; }
    }
}
