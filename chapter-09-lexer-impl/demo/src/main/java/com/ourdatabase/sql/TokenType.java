package com.ourdatabase.sql;

/**
 * Token类型枚举 —— 定义SQL中所有可能的词法单元类型
 */
public enum TokenType {
    // 关键字
    KEYWORD_SELECT, KEYWORD_FROM, KEYWORD_WHERE, KEYWORD_INSERT,
    KEYWORD_INTO, KEYWORD_VALUES, KEYWORD_DELETE, KEYWORD_UPDATE,
    KEYWORD_SET, KEYWORD_CREATE, KEYWORD_TABLE, KEYWORD_DROP,
    KEYWORD_AND, KEYWORD_OR, KEYWORD_NOT, KEYWORD_ORDER, KEYWORD_BY,
    KEYWORD_GROUP, KEYWORD_HAVING, KEYWORD_LIMIT, KEYWORD_JOIN,
    KEYWORD_ON, KEYWORD_AS, KEYWORD_DISTINCT,

    // 字面量
    INTEGER,
    FLOAT,
    STRING,

    // 标识符
    IDENTIFIER,

    // 运算符
    EQUALS,        // =
    GREATER_THAN,  // >
    LESS_THAN,     // <
    GREATER_EQUAL, // >=
    LESS_EQUAL,    // <=
    NOT_EQUAL1,    // !=
    NOT_EQUAL2,    // <>
    PLUS,          // +
    MINUS,         // -
    MULTIPLY,      // *

    // 分隔符
    LEFT_PAREN,    // (
    RIGHT_PAREN,   // )
    COMMA,         // ,
    SEMICOLON,     // ;
    STAR,          // * (特殊：SELECT *)

    // 结束标记
    EOF
}
