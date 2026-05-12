package com.ourdatabase;

/**
 * 存储引擎接口 —— 定义数据库存储引擎必须实现的方法
 *
 * 这是"可插拔存储引擎"设计模式的核心。
 * 任何存储引擎（HashMap引擎、文件引擎、B+树引擎）只要实现了这个接口，
 * 就可以被数据库使用。
 *
 * 生活中的类比：
 * - 这个接口就像"USB接口标准"
 * - 无论你插的是U盘、移动硬盘还是键盘，只要符合USB标准就能用
 * - 同样，无论引擎底层是HashMap、文件还是B+树，只要实现了这个接口就能用
 *
 * 关键学习方法：
 * - interface（接口）只定义"能做什么"，不定义"怎么做"
 * - 具体"怎么做"由实现了这个接口的类来决定
 */
public interface StorageEngineInterface {

    /**
     * 创建一张新表
     *
     * @param tableName 表的名字，例如 "student"
     * @param columnNames 表的列名数组，例如 ["id", "name", "age"]
     * @return true表示创建成功，false表示表已存在
     */
    boolean createTable(String tableName, String[] columnNames);

    /**
     * 向表中插入一行数据
     *
     * @param tableName 要插入的表名
     * @param values 要插入的数据值，顺序必须和列名顺序一致
     * @return true表示插入成功，false表示失败
     */
    boolean insert(String tableName, String[] values);

    /**
     * 查询表中的数据
     *
     * @param tableName 要查询的表名
     * @param conditionColumn 按哪一列过滤（为null表示查所有行）
     * @param conditionValue 过滤的值（为null表示查所有行）
     * @return 查询结果的字符串表示
     */
    String query(String tableName, String conditionColumn, String conditionValue);

    /**
     * 获取表的所有列名
     *
     * @param tableName 表名
     * @return 列名数组，如果表不存在返回null
     */
    String[] getColumnNames(String tableName);

    /**
     * 获取表中的行数
     *
     * @param tableName 表名
     * @return 行数，如果表不存在返回0
     */
    int getRowCount(String tableName);

    /**
     * 检查表是否存在
     *
     * @param tableName 表名
     * @return true表示存在
     */
    boolean tableExists(String tableName);

    /**
     * 获取所有表名
     *
     * @return 表名数组
     */
    String[] getAllTableNames();
}
