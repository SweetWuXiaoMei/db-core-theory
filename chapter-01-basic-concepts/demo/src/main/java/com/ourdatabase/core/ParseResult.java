package com.ourdatabase.core;

import java.util.ArrayList;
import java.util.List;

/**
 * 语法分析结果 —— 存储语法分析器提取的结构化信息
 *
 * 这是一个"数据对象"（Data Object），用来在模块之间传递信息。
 * 在真实的数据库中，这对应"抽象语法树（AST）"，第10章会详细实现。
 *
 * 数据对象的特点：
 * - 只有数据，没有复杂的业务逻辑
 * - 通过 getter/setter 方法读写数据
 * - 像一个"信封"，在不同模块之间传递
 */
public class ParseResult {

    private String operationType;       // SELECT / INSERT / DELETE / UPDATE / CREATE
    private String tableName;           // 要操作的表名
    private List<String> columnList;    // 要查询的列名（SELECT语句需要）
    private String condition;           // WHERE条件，例如 "id = 100"

    /** 构造函数 */
    public ParseResult() {
        this.columnList = new ArrayList<>();
    }

    // ==================== Getter 和 Setter 方法 ====================

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public List<String> getColumnNames() {
        return columnList;
    }

    public void addColumnName(String columnName) {
        this.columnList.add(columnName);
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public String toString() {
        return "语法分析结果{" +
                "操作类型='" + operationType + '\'' +
                ", 表名='" + tableName + '\'' +
                ", 列名=" + columnList +
                ", 条件='" + condition + '\'' +
                '}';
    }
}
