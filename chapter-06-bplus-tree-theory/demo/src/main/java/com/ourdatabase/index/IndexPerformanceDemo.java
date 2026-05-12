package com.ourdatabase.index;

import java.util.*;

/**
 * 索引性能演示 —— 第6章Demo
 *
 * 对比三种查找方式的性能差异：
 * 1. 全表扫描（O(n)）—— 没有索引
 * 2. 二分查找（O(log n)）—— 有序数组
 * 3. B+树查找（O(log n)）—— 数据库实际使用
 *
 * 关键洞察：数据量越大，索引的价值越明显！
 */
public class IndexPerformanceDemo {

    public static void main(String[] args) {
        printTitle("索引性能对比演示");

        // 测试不同数据规模下的查找性能
        int[] dataSizes = {1000, 10000, 100000, 1000000};

        for (int dataSize : dataSizes) {
            System.out.println("\n>>> 数据量 = " + String.format("%,d", dataSize));

            // 生成测试数据
            int[] sortedArray = generateSortedData(dataSize);

            // 要查找的目标值（在数据中间位置）
            int targetValue = dataSize / 2;

            // 1. 全表扫描
            long start = System.nanoTime();
            int result1 = fullTableScan(sortedArray, targetValue);
            long scanTime = System.nanoTime() - start;

            // 2. 二分查找
            start = System.nanoTime();
            int result2 = binarySearch(sortedArray, targetValue);
            long binaryTime = System.nanoTime() - start;

            System.out.println("  全表扫描: " + scanTime + " ns (" + result1 + " = " + targetValue + ")");
            System.out.println("  二分查找: " + binaryTime + " ns (" + result2 + " = " + targetValue + ")");
            System.out.println("  加速比: " + String.format("%.0f", (double)scanTime / binaryTime) + "x");
        }

        // 演示B+树的高度和IO次数
        printStep("B+树高度与磁盘IO次数");

        System.out.println("\n  假设：数据页=16KB, 每对(键+指针)=16字节, 每行数据=1KB");
        System.out.println("  一个内部节点可存 ~1000个键值");
        System.out.println("  一个叶子节点可存 ~16条数据记录\n");

        long[] dataAmounts = {1_000L, 10_000L, 100_000L, 1_000_000L, 100_000_000L, 1_000_000_000L};

        System.out.println("  数据量           | 树高度 | 磁盘IO次数 | vs 全表扫描");
        System.out.println("  ----------------|--------|-----------|------------");

        for (long rowCount : dataAmounts) {
            int height = calculateBPlusTreeHeight(rowCount, 1000);
            long fullScanCount = rowCount / 16; // 每页16行
            System.out.println(String.format("  %,15d | %6d | %9d | vs %,d 次",
                    rowCount, height, height, fullScanCount));
        }

        printTitle("结论");
        System.out.println("1. 索引的价值随数据量增长而增大");
        System.out.println("2. B+树的高度极低（百万数据只要3层）");
        System.out.println("3. 数据库查询的快慢取决于磁盘IO次数");
        System.out.println("4. B+树 = 为磁盘优化的二分查找");
    }

    /** 全表扫描（O(n)）：逐一检查每个元素 */
    private static int fullTableScan(int[] array, int target) {
        for (int value : array) {
            if (value == target) return value;
        }
        return -1;
    }

    /** 二分查找（O(log n)）：每次缩小一半范围 */
    private static int binarySearch(int[] array, int target) {
        int left = 0, right = array.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (array[mid] == target) return array[mid];
            else if (array[mid] < target) left = mid + 1;
            else right = mid - 1;
        }
        return -1;
    }

    /** 生成有序测试数据 */
    private static int[] generateSortedData(int count) {
        int[] data = new int[count];
        for (int i = 0; i < count; i++) data[i] = i + 1;
        return data;
    }

    /** 计算B+树的理论高度 */
    private static int calculateBPlusTreeHeight(long totalRecords, int keysPerNode) {
        int height = 0;
        long currentNodeCount = (long) Math.ceil((double) totalRecords / keysPerNode);
        while (currentNodeCount > 0) {
            height++;
            currentNodeCount = (long) Math.ceil((double) currentNodeCount / keysPerNode);
        }
        return height;
    }

    private static void printStep(String description) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("【" + description + "】");
        System.out.println("-".repeat(50));
    }

    private static void printTitle(String title) {
        System.out.println("=".repeat(50));
        System.out.println("  " + title);
        System.out.println("=".repeat(50));
    }
}
