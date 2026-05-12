package com.ourdatabase.buffer;

public class BufferPoolDemo {
    public static void main(String[] args) {
        printTitle("BufferPool Demo");

        printStep(1, "Create pool (capacity=3)");
        BufferPool pool = new BufferPool(3);
        System.out.println("    -> pool created, max 3 pages");

        printStep(2, "First access pages 0-2 (all MISS)");
        pool.getPage(0); pool.getPage(1); pool.getPage(2);
        pool.printStatus();

        printStep(3, "Re-access pages 0,1 (HIT!)");
        pool.getPage(0); pool.getPage(1);

        printStep(4, "Access page 3 (triggers LRU eviction)");
        pool.getPage(3);
        pool.printStatus();

        printStep(5, "Modify page 3 (mark dirty)");
        pool.modifyPage(3, "modified data");
        pool.printStatus();

        printStep(6, "Access page 4 (evicts dirty page 3)");
        pool.getPage(4);
        pool.printStatus();

        printTitle("Summary");
        System.out.println("1. BufferPool caches hot pages in memory");
        System.out.println("2. LRU evicts least recently used pages");
        System.out.println("3. Dirty pages must flush to disk before eviction");
    }

    static void printStep(int n, String d) { System.out.println("\n" + "-".repeat(50) + "\n【Step " + n + "】" + d + "\n" + "-".repeat(50)); }
    static void printTitle(String t) { System.out.println("=".repeat(50) + "\n  " + t + "\n" + "=".repeat(50)); }
}
