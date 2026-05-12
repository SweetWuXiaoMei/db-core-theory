package com.ourdatabase.sql;

/**
 * Token（词法单元）—— 词法分析的结果
 *
 * 每个Token包含两个信息：
 * - 类型：这个词是什么（关键字、标识符、数字等）
 * - 值：这个词的具体内容
 */
public class Token {
    private final TokenType type;
    private final String value;

    public Token(TokenType type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenType getType() { return type; }
    public String getValue() { return value; }

    /** 快速判断Token是否为某种类型的关键字 */
    public boolean isKeyword(String keyword) {
        return type.name().equals("KEYWORD_" + keyword.toUpperCase());
    }

    @Override
    public String toString() {
        return String.format("%-14s \"%s\"", type.name(), value);
    }
}
