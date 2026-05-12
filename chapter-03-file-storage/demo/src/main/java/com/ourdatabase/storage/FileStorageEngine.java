package com.ourdatabase.storage;

import java.io.*;
import java.util.*;

/**
 * 文件存储引擎 —— 基于文件的持久化存储引擎
 *
 * 这是第3章的核心类：演示如何把数据真正存储到磁盘文件中。
 *
 * 设计思路：
 * 1. 内存中保留索引（主键 → 文件位置），加速查找
 * 2. 数据实际存在磁盘文件里
 * 3. 使用 RandomAccessFile 实现随机读写
 *
 * 文件内部结构：
 * ┌──────────────────────────────────┐
 * │ 行1序列化字节 │ 行2序列化字节 │ ... │
 * └──────────────────────────────────┘
 * 每行之间紧密排列，通过索引记录每行的起始位置。
 *
 * 索引结构（内存中）：
 * Map<主键, 文件位置>
 * "100" → 0    （第1行从文件字节0开始）
 * "101" → 18   （第2行从文件字节18开始）
 */
public class FileStorageEngine {

    // 数据文件路径
    private String filePath;

    // 内存索引：主键 → 文件中的位置（字节偏移量）
    // 这是加速查找的关键！没有它，每次查询都要扫描整个文件
    private Map<String, Long> index = new LinkedHashMap<>();

    // 表结构
    private String[] columnNames;
    private String tableName;

    /**
     * 构造函数
     *
     * @param filePath 数据文件存放的位置
     * @param tableName 表名
     * @param columnNames 列名数组
     */
    public FileStorageEngine(String filePath, String tableName, String[] columnNames) {
        this.filePath = filePath;
        this.tableName = tableName;
        this.columnNames = columnNames;
    }

    /**
     * 插入一行数据到文件
     *
     * 插入流程：
     * 1. 序列化数据行为字节数组
     * 2. 打开文件（追加模式），跳到文件末尾
     * 3. 记录当前文件位置（即这行数据的起始位置）
     * 4. 写入字节
     * 5. 更新内存索引
     *
     * @param values 要插入的数据值
     */
    public void insert(String[] values) throws IOException {
        // 1. 序列化数据行
        byte[] byteData = RowSerializer.serialize(values);

        // 2. 打开文件（RandomAccessFile 的 "rw" 模式 = 可读可写）
        //    文件不存在时会自动创建
        try (RandomAccessFile file = new RandomAccessFile(filePath, "rw")) {

            // 3. 跳到文件末尾（获取当前文件长度即为末尾位置）
            long writePosition = file.length();
            file.seek(writePosition);

            // 4. 写入字节数据
            file.write(byteData);

            // 5. 更新内存索引：记录主键 → 文件位置
            String primaryKey = values[0];
            index.put(primaryKey, writePosition);

            // 6. 强制刷新到磁盘（确保数据真正写入）
            //    getFD() 获取文件描述符，sync() 强制同步到磁盘
            file.getFD().sync();

            System.out.println("    -> 已写入磁盘：主键=" + primaryKey +
                    ", 文件位置=" + writePosition + ", 字节数=" + byteData.length);
        }
    }

    /**
     * 根据主键查询一行数据
     *
     * 查询流程：
     * 1. 先查内存索引，获取文件位置
     * 2. 如果索引中有 → 直接跳到文件那个位置读取
     * 3. 如果索引中没有 → 返回null
     *
     * @param primaryKey 要查询的主键值
     * @return 数据行，如果没找到返回null
     */
    public String[] findByPrimaryKey(String primaryKey) throws IOException {
        // 1. 先查内存索引
        Long filePosition = index.get(primaryKey);
        if (filePosition == null) {
            System.out.println("    -> 索引中未找到主键=" + primaryKey);
            return null;
        }

        // 2. 从文件中读取
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            // 跳到指定位置
            file.seek(filePosition);

            // 3. 读取行数据的字节数
            //    我们需要知道这一行占多少字节，但文件中没有直接存。
            //    解决方案：先读字段数量（4字节），再逐个字段读取
            int fieldCount = file.readInt();

            String[] rowData = new String[fieldCount];
            for (int i = 0; i < fieldCount; i++) {
                int fieldLength = file.readInt();
                byte[] fieldBytes = new byte[fieldLength];
                file.readFully(fieldBytes);
                rowData[i] = new String(fieldBytes, java.nio.charset.StandardCharsets.UTF_8);
            }

            System.out.println("    -> 从磁盘读取：文件位置=" + filePosition);
            return rowData;
        }
    }

    /**
     * 查询所有数据（全表扫描）
     *
     * @return 所有数据行的列表
     */
    public List<String[]> findAll() throws IOException {
        List<String[]> result = new ArrayList<>();

        // 如果文件不存在，返回空列表
        File fileObj = new File(filePath);
        if (!fileObj.exists()) {
            return result;
        }

        // 从头到尾顺序读取整个文件
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            long fileLength = file.length();

            while (file.getFilePointer() < fileLength) {
                // 记录这一行的起始位置
                long rowStartPosition = file.getFilePointer();

                try {
                    // 读取字段数量
                    int fieldCount = file.readInt();
                    String[] rowData = new String[fieldCount];

                    for (int i = 0; i < fieldCount; i++) {
                        int fieldLength = file.readInt();
                        byte[] fieldBytes = new byte[fieldLength];
                        file.readFully(fieldBytes);
                        rowData[i] = new String(fieldBytes, java.nio.charset.StandardCharsets.UTF_8);
                    }

                    result.add(rowData);

                    // 更新索引（如果之前没有的话）
                    String primaryKey = rowData[0];
                    if (!index.containsKey(primaryKey)) {
                        index.put(primaryKey, rowStartPosition);
                    }

                } catch (EOFException e) {
                    // 文件读完了（正常结束）
                    break;
                }
            }
        }

        return result;
    }

    /**
     * 从文件重建索引
     *
     * 场景：程序重启后，内存索引丢失了，但数据还在文件里。
     * 需要扫描整个文件，重新构建内存索引。
     *
     * 这就是"数据恢复"的核心逻辑！
     */
    public void rebuildIndex() throws IOException {
        System.out.println("    -> 正在从文件重建索引...");
        index.clear();

        File fileObj = new File(filePath);
        if (!fileObj.exists()) {
            System.out.println("    -> 文件不存在，跳过（可能还没有数据）");
            return;
        }

        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            long fileLength = file.length();
            int rowCount = 0;

            while (file.getFilePointer() < fileLength) {
                long rowPosition = file.getFilePointer();

                try {
                    int fieldCount = file.readInt();
                    // 只读第一个字段（主键），快速扫描
                    int fieldLength = file.readInt();
                    byte[] fieldBytes = new byte[fieldLength];
                    file.readFully(fieldBytes);
                    String primaryKey = new String(fieldBytes, java.nio.charset.StandardCharsets.UTF_8);

                    index.put(primaryKey, rowPosition);
                    rowCount++;

                    // 跳过剩余的字段（不需要读内容，只要跳过字节）
                    for (int i = 1; i < fieldCount; i++) {
                        int remainingLength = file.readInt();
                        file.skipBytes(remainingLength);
                    }
                } catch (EOFException e) {
                    break;
                }
            }

            System.out.println("    -> 索引重建完成！共恢复 " + rowCount + " 行的索引");
        }
    }

    /**
     * 格式化输出查询结果
     */
    public String formatResult(String[] rowData) {
        if (rowData == null) return "未找到数据";

        StringBuilder sb = new StringBuilder("{ ");
        for (int i = 0; i < columnNames.length && i < rowData.length; i++) {
            sb.append(columnNames[i]).append("='").append(rowData[i]).append("'");
            if (i < Math.min(columnNames.length, rowData.length) - 1) {
                sb.append(", ");
            }
        }
        sb.append(" }");
        return sb.toString();
    }

    // ============ Getter 方法 ============

    public int getIndexSize() {
        return index.size();
    }

    public String getFilePath() {
        return filePath;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public String getTableName() {
        return tableName;
    }
}
