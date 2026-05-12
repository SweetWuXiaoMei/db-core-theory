package com.ourdatabase.storage;

import java.io.File;
import java.util.List;

/**
 * 文件存储演示 —— 第3章主程序
 *
 * 演示数据库最核心的能力：数据持久化到磁盘，重启后能恢复数据。
 *
 * 演示流程：
 * 1. 插入几条数据到文件
 * 2. 通过主键查询（验证写入成功）
 * 3. 全表扫描
 * 4. 模拟"数据库重启"（清空索引）
 * 5. 从文件重建索引（验证数据还在！）
 * 6. 再次查询（验证恢复成功）
 */
public class FileStorageDemo {

    // 数据文件存放的目录
    private static final String dataDir = "./data";
    private static final String dataFile = dataDir + "/student.db";

    public static void main(String[] args) throws Exception {
        printTitle("文件存储引擎演示");

        // ========== 准备工作 ==========
        // 创建数据目录
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
            System.out.println("创建数据目录：" + dir.getAbsolutePath());
        }

        // 删除旧数据文件（确保每次运行都是全新的开始）
        File oldFile = new File(dataFile);
        if (oldFile.exists()) {
            oldFile.delete();
        }

        // 创建文件存储引擎
        String[] columnNames = {"id", "name", "age"};
        FileStorageEngine engine = new FileStorageEngine(dataFile, "student", columnNames);

        // ========== 第1步：插入数据 ==========
        printStep(1, "插入数据到文件");

        engine.insert(new String[]{"100", "张三", "20"});
        engine.insert(new String[]{"101", "李四", "22"});
        engine.insert(new String[]{"102", "王五", "19"});

        System.out.println("\n    -> 插入完成，内存索引大小：" + engine.getIndexSize());

        // ========== 第2步：通过主键查询 ==========
        printStep(2, "通过主键查询（利用内存索引+文件随机读取）");

        String[] result1 = engine.findByPrimaryKey("100");
        System.out.println("    -> 查询 id=100：" + engine.formatResult(result1));

        String[] result2 = engine.findByPrimaryKey("102");
        System.out.println("    -> 查询 id=102：" + engine.formatResult(result2));

        // ========== 第3步：全表扫描 ==========
        printStep(3, "全表扫描（顺序读取整个文件）");

        List<String[]> allData = engine.findAll();
        System.out.println("    -> 全表扫描结果（共 " + allData.size() + " 行）：");
        for (String[] row : allData) {
            System.out.println("       " + engine.formatResult(row));
        }

        // ========== 第4步：模拟数据库重启 ==========
        printStep(4, "模拟数据库重启（清空内存索引，模拟进程重启）");

        // 关键演示：创建一个新的引擎对象，它的索引是空的
        FileStorageEngine engineAfterRestart = new FileStorageEngine(dataFile, "student", columnNames);
        System.out.println("    -> 重启后，索引大小：" + engineAfterRestart.getIndexSize() + "（索引丢失了！）");

        // ========== 第5步：从文件重建索引 ==========
        printStep(5, "从文件重建索引（数据恢复！）");

        engineAfterRestart.rebuildIndex();
        System.out.println("    -> 重建后，索引大小：" + engineAfterRestart.getIndexSize());

        // ========== 第6步：验证数据恢复 ==========
        printStep(6, "验证恢复后的数据");

        String[] recoveredResult = engineAfterRestart.findByPrimaryKey("101");
        System.out.println("    -> 查询 id=101：" + engineAfterRestart.formatResult(recoveredResult));

        List<String[]> recoveredAll = engineAfterRestart.findAll();
        System.out.println("    -> 恢复后全表扫描（共 " + recoveredAll.size() + " 行）：");
        for (String[] row : recoveredAll) {
            System.out.println("       " + engineAfterRestart.formatResult(row));
        }

        // ========== 第7步：查看实际文件大小 ==========
        printStep(7, "查看数据文件信息");

        File dataFileObj = new File(dataFile);
        System.out.println("    -> 文件路径：" + dataFileObj.getAbsolutePath());
        System.out.println("    -> 文件大小：" + dataFileObj.length() + " 字节");
        System.out.println("    -> 文件存在：" + dataFileObj.exists());

        // ========== 总结 ==========
        printTitle("演示总结");
        System.out.println("1. 数据持久化到磁盘文件后，程序重启数据不丢失");
        System.out.println("2. 内存索引加速查找（O(1)时间找到数据）");
        System.out.println("3. RandomAccessFile 支持随机访问（跳过无关数据，直接读目标）");
        System.out.println("4. 索引可以从文件重建（这就是崩溃恢复的基础！）");
        System.out.println("\n数据文件位置：" + new File(dataFile).getAbsolutePath());
        System.out.println("你可以用 hexdump 或 xxd 命令查看文件内容");
    }

    private static void printStep(int stepNumber, String description) {
        System.out.println("\n" + "-".repeat(50));
        System.out.println("【步骤" + stepNumber + "】" + description);
        System.out.println("-".repeat(50));
    }

    private static void printTitle(String title) {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("  " + title);
        System.out.println("=".repeat(50));
    }
}
