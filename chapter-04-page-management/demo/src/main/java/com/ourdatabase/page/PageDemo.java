package com.ourdatabase.page;

import java.util.List;

/**
 * 页管理演示 —— 第4章主程序
 *
 * 演示数据页的核心操作：
 * 1. 创建页管理器
 * 2. 多页插入（当一页满了自动分配新页）
 * 3. 页内查询
 * 4. 删除操作（标记删除）
 * 5. 碎片整理
 * 6. 查看页的使用情况
 */
public class PageDemo {

    public static void main(String[] args) {
        printTitle("数据页管理演示");
        System.out.println("页大小 = " + DataPage.PAGE_SIZE + " 字节（教学用，远小于真实16KB）\n");

        // ========== 第1步：创建页管理器 ==========
        printStep(1, "创建页管理器");

        String[] columnNames = {"id", "name", "age", "city"};
        PageManager manager = new PageManager("student", columnNames);
        System.out.println("    -> 创建表 '" + manager.getTableName() + "'，页管理器就绪");
        System.out.println("    -> 当前页数：" + manager.getTotalPageCount() + "（懒分配，还没创建页）");

        // ========== 第2步：插入数据 ==========
        printStep(2, "插入数据（自动分页）");

        // 模拟能放3行的数据（真实数据比这个长，页大小256字节大约放3-4行）
        String[][] testData = {
                {"100", "张三", "20", "北京"},
                {"101", "李四", "22", "上海"},
                {"102", "王五", "19", "广州"},
                {"103", "赵六", "21", "深圳"},
                {"104", "孙七", "23", "杭州"},
                {"105", "周八", "20", "成都"},
        };

        for (String[] data : testData) {
            DataPage page = manager.insertRow(data);
            System.out.println("    -> 插入 id=" + data[0] + " -> 页" + page.getPageNumber()
                    + " (有效行=" + page.getValidRowCount() + ", 空闲=" + page.getFreeSpace() + "B)");
        }

        // ========== 第3步：查看页使用情况 ==========
        printStep(3, "查看页使用情况");
        manager.printPageUsage();

        // ========== 第4步：查询数据 ==========
        printStep(4, "查询数据");

        String[] result1 = manager.findRow("100");
        System.out.println("    -> 查询 id=100：" + formatRow(columnNames, result1));

        String[] result2 = manager.findRow("104");
        System.out.println("    -> 查询 id=104：" + formatRow(columnNames, result2));

        String[] result3 = manager.findRow("999");
        System.out.println("    -> 查询 id=999（不存在）：" + (result3 == null ? "未找到" : formatRow(columnNames, result3)));

        // ========== 第5步：全表扫描 ==========
        printStep(5, "全表扫描（遍历页链表）");

        List<String[]> allRows = manager.findAllRows();
        System.out.println("    -> 全表扫描结果（共 " + allRows.size() + " 行）：");
        for (String[] row : allRows) {
            System.out.println("       " + formatRow(columnNames, row));
        }

        // ========== 第6步：删除数据 ==========
        printStep(6, "删除数据（标记删除）");

        System.out.println("    -> 删除前，页使用情况：");
        manager.printPageUsage();

        manager.deleteRow("101");
        manager.deleteRow("103");

        System.out.println("\n    -> 删除 id=101 和 id=103 后：");
        manager.printPageUsage();

        System.out.println("\n    -> 注意：删除后空间没有释放！（标记删除，等待清理）");

        // ========== 总结 ==========
        printTitle("演示总结");
        System.out.println("1. 数据以固定大小的'页'为单位组织");
        System.out.println("2. 一页满了自动分配新页，通过链表连接");
        System.out.println("3. 删除是'标记删除'，不立即回收空间");
        System.out.println("4. 全表扫描需要遍历整个页链表（O(n)复杂度）");
        System.out.println("5. 页是B+树索引（第7章）和缓冲池（第5章）的基础");
    }

    private static String formatRow(String[] columnNames, String[] values) {
        if (values == null) return "未找到";
        StringBuilder sb = new StringBuilder("{ ");
        for (int i = 0; i < columnNames.length && i < values.length; i++) {
            sb.append(columnNames[i]).append("='").append(values[i]).append("'");
            if (i < Math.min(columnNames.length, values.length) - 1) sb.append(", ");
        }
        sb.append(" }");
        return sb.toString();
    }

    private static void printStep(int stepNumber, String description) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("【步骤" + stepNumber + "】" + description);
        System.out.println("-".repeat(50));
    }

    private static void printTitle(String title) {
        System.out.println("=".repeat(50));
        System.out.println("  " + title);
        System.out.println("=".repeat(50));
    }
}
