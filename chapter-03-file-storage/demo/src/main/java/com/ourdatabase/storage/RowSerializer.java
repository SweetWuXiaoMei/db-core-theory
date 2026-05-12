package com.ourdatabase.storage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 行数据序列化器 —— 负责把数据行转换为字节，以及反向转换
 *
 * 核心作用：在"内存中的数据结构"和"文件中的字节"之间转换
 *
 * 序列化格式（非常重要！）：
 * ┌──────────┬──────────┬──────────┬─────┐
 * │ 字段1长度 │ 字段1内容 │ 字段2长度 │ ... │
 * │ (4字节)   │ (变长)    │ (4字节)   │     │
 * └──────────┴──────────┴──────────┴─────┘
 *
 * 为什么前面要存长度？
 * 因为字符串长度不固定，"张三"是6字节，"John"是4字节。
 * 如果不知道长度，读的时候就不知道读多少字节。
 *
 * 例如：["100", "张三", "20"] 序列化后：
 * │ 3 │ 100 │ 6 │ 张三 │ 2 │ 20 │
 *  ↑   ↑     ↑    ↑      ↑   ↑
 * 长度 内容  长度 内容   长度 内容
 */
public class RowSerializer {

    /**
     * 将一行字符串数据序列化为字节数组
     *
     * @param rowData 要序列化的数据行，例如 ["100", "张三", "20"]
     * @return 序列化后的字节数组
     */
    public static byte[] serialize(String[] rowData) throws IOException {
        // ByteArrayOutputStream：一个在内存中自动扩容的字节数组
        // DataOutputStream：可以方便地写入Java基本类型（int、long、String等）
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        DataOutputStream dataOutput = new DataOutputStream(byteStream);

        // 先写入字段数量（4字节整数）
        // 读取时可以先知道这行有多少个字段
        dataOutput.writeInt(rowData.length);

        // 逐个字段写入：先写长度（字节数），再写内容
        for (String fieldValue : rowData) {
            // String.getBytes() 将字符串转换为字节数组（UTF-8编码）
            // UTF-8：中文一个字符占3字节，英文一个字符占1字节
            byte[] fieldBytes = fieldValue.getBytes(StandardCharsets.UTF_8);

            // 写入字段的字节长度（4字节整数）
            dataOutput.writeInt(fieldBytes.length);

            // 写入字段的字节内容
            dataOutput.write(fieldBytes);
        }

        dataOutput.flush();
        return byteStream.toByteArray();
    }

    /**
     * 从字节数组中反序列化出一行字符串数据
     *
     * @param byteArray 从文件读取的字节
     * @return 反序列化后的字符串数组，例如 ["100", "张三", "20"]
     */
    public static String[] deserialize(byte[] byteArray) throws IOException {
        // DataInputStream：可以方便地读取Java基本类型
        DataInputStream dataInput = new DataInputStream(
                new ByteArrayInputStream(byteArray));

        // 先读取字段数量
        int fieldCount = dataInput.readInt();
        String[] rowData = new String[fieldCount];

        // 逐个字段读取：先读长度，再读内容
        for (int i = 0; i < fieldCount; i++) {
            // 读取字段的字节长度
            int fieldLength = dataInput.readInt();

            // 读取指定长度的字节
            byte[] fieldBytes = new byte[fieldLength];
            dataInput.readFully(fieldBytes); // readFully确保读满指定长度

            // 将字节转换为字符串（UTF-8解码）
            rowData[i] = new String(fieldBytes, StandardCharsets.UTF_8);
        }

        return rowData;
    }

    /**
     * 计算序列化后的字节数（用于预估文件大小）
     *
     * @param rowData 数据行
     * @return 序列化后的字节数
     */
    public static int calculateByteSize(String[] rowData) {
        int totalBytes = 4; // 字段数量占用4字节
        for (String fieldValue : rowData) {
            totalBytes += 4; // 长度标记占用4字节
            totalBytes += fieldValue.getBytes(StandardCharsets.UTF_8).length; // 内容字节数
        }
        return totalBytes;
    }
}
