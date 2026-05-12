package com.ourdatabase.core;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓冲区（Buffer） —— 模拟数据库的内存缓存
 *
 * 为什么需要缓冲区？
 * 数据库中最慢的操作是读写磁盘（硬盘）。
 * 内存读写的速度大约是磁盘的 100,000 倍以上！
 *
 * 为了加速，数据库会把常用的数据留在内存里，这就是"缓冲区"。
 *
 * 生活类比：
 * - 办公桌（内存/缓冲区）：放着常用的文件，伸手就能拿到
 * - 文件柜（磁盘）：存着所有文件，需要站起来走过去才能拿到
 * - 缓冲区命中：你要的东西刚好在桌上 -> 很快！
 * - 缓冲区未命中：你要的东西在柜子里 -> 需要走过去拿 -> 慢
 *
 * 第5章会详细学习缓冲区的设计原理，包括：
 * - LRU淘汰算法（最近最少使用）
 * - 脏页管理
 * - 缓冲池的分区设计
 */
public class Buffer {

    // 用HashMap模拟内存缓存
    // 外层Map: 表名 -> 该表的缓存数据
    // 内层Map: 主键值 -> 行数据数组
    private Map<String, Map<String, String[]>> cache = new HashMap<>();

    /**
     * 从缓冲区读取数据
     *
     * @param tableName 要查询的表名
     * @param primaryKeyName 主键列名
     * @param primaryKeyValue 主键的值
     * @return 如果命中缓存则返回数据行，否则返回 null
     */
    public String[] read(String tableName, String primaryKeyName, String primaryKeyValue) {
        Map<String, String[]> tableData = cache.get(tableName);
        if (tableData != null) {
            String[] rowData = tableData.get(primaryKeyValue);
            if (rowData != null) {
                System.out.println("    [缓存命中] 直接从内存读取，速度极快！");
                return rowData;
            }
        }
        System.out.println("    [缓存未命中] 需要从磁盘读取...");
        return null;
    }

    /**
     * 将数据写入缓冲区
     *
     * @param tableName 表名
     * @param primaryKeyValue 主键的值
     * @param data 要缓存的数据行
     */
    public void write(String tableName, String primaryKeyValue, String[] data) {
        // 如果缓存中还没有这张表的缓存区，先创建一个
        Map<String, String[]> tableData = cache.get(tableName);
        if (tableData == null) {
            tableData = new HashMap<>();
            cache.put(tableName, tableData);
        }
        // 把数据放入缓存
        tableData.put(primaryKeyValue, data);
        System.out.println("    -> 数据已写入缓冲区（内存中）");
    }

    /**
     * 获取缓冲区当前的缓存条目总数
     */
    public int getCacheEntryCount() {
        int total = 0;
        for (Map<String, String[]> tableData : cache.values()) {
            total += tableData.size();
        }
        return total;
    }

    /**
     * 清空缓冲区（模拟数据库重启）
     */
    public void clear() {
        cache.clear();
        System.out.println("    -> 缓冲区已清空（模拟数据库重启）");
    }
}
