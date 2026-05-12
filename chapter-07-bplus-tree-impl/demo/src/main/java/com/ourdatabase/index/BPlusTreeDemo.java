package com.ourdatabase.index;

import java.util.List;
import java.util.Map;

/**
 * B+树演示 —— 第7章主程序
 *
 * 演示完整的B+树操作：插入、精确查找、范围查询、删除、结构打印
 */
public class BPlusTreeDemo {

    public static void main(String[] args) {
        printTitle("B+树索引演示");

        // Step 1: 创建B+树
        printStep(1, "创建B+树（阶=4）");
        BPlusTree<Integer, String> tree = new BPlusTree<>(4);
        System.out.println("    -> B+树创建成功，阶=4");
        System.out.println("    -> 每个节点最多3个键值、4个子节点");

        // Step 2: 插入数据
        printStep(2, "插入数据");
        int[] keys = {1, 5, 9, 13, 17, 21, 3, 7, 11, 15, 19, 2};
        String[] values = {"张三","李四","王五","赵六","孙七","周八","吴九","郑十","冯十一","陈十二","褚十三","卫十四"};

        for (int i = 0; i < keys.length; i++) {
            tree.insert(keys[i], values[i]);
            System.out.println("    -> 插入: " + keys[i] + " → " + values[i]);
        }

        tree.printStructure();
        System.out.println("    树中条目总数: " + tree.size());

        // Step 3: 精确查找
        printStep(3, "精确查找");
        int[] searchTargets = {7, 13, 20};
        for (int key : searchTargets) {
            String value = tree.search(key);
            if (value != null) {
                System.out.println("    -> 查找 " + key + " = " + value + "（找到！）");
            } else {
                System.out.println("    -> 查找 " + key + " = 不存在");
            }
        }

        // Step 4: 范围查询
        printStep(4, "范围查询（B+树的杀手锏功能！）");
        System.out.println("    -> 查询键范围 [5, 15]：");
        List<Map.Entry<Integer, String>> rangeResult = tree.rangeSearch(5, 15);
        for (Map.Entry<Integer, String> entry : rangeResult) {
            System.out.println("       " + entry.getKey() + " → " + entry.getValue());
        }
        System.out.println("    共找到 " + rangeResult.size() + " 条");

        // Step 5: 删除
        printStep(5, "删除数据");
        System.out.println("    -> 删除前，条目数：" + tree.size());
        int[] deleteTargets = {3, 9, 17};
        for (int key : deleteTargets) {
            boolean ok = tree.delete(key);
            System.out.println("    -> 删除 " + key + "：" + (ok ? "成功" : "失败"));
        }
        System.out.println("    -> 删除后，条目数：" + tree.size());
        tree.printStructure();

        // Step 6: 大量数据
        printStep(6, "大量数据插入（演示B+树自动分裂）");
        BPlusTree<Integer, String> bigTree = new BPlusTree<>(4);
        System.out.println("    -> 插入50条数据...");
        for (int i = 1; i <= 50; i++) {
            bigTree.insert(i, "值" + i);
        }
        bigTree.printStructure();
        System.out.println("    条目总数: " + bigTree.size());

        printTitle("演示总结");
        System.out.println("1. B+树内部节点只做导航，叶子节点存数据");
        System.out.println("2. 叶子节点之间有链表，范围查询极快");
        System.out.println("3. 节点满时自动分裂，树自动平衡");
        System.out.println("4. 查找复杂度 O(log n)，范围查询 O(log n + k)");
    }

    private static void printStep(int num, String desc) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("【步骤" + num + "】" + desc);
        System.out.println("-".repeat(50));
    }

    private static void printTitle(String title) {
        System.out.println("=".repeat(50));
        System.out.println("  " + title);
        System.out.println("=".repeat(50));
    }
}
