package com.ourdatabase.sql;

import java.util.*;

/**
 * SQL词法分析器 —— 完整的SQL Token解析器
 *
 * 功能：把SQL字符串拆分为Token列表
 *
 * 状态机包含5个状态：
 * START → 初始态
 * IN_WORD → 正在读取单词（可能是关键字或标识符）
 * IN_NUMBER → 正在读取数字（整数或浮点数）
 * IN_STRING → 正在读取字符串（单引号或双引号）
 * IN_OPERATOR → 正在读取运算符（单字符或双字符）
 */
public class Lexer {

    // SQL关键字映射表（大写关键字 → Token类型）
    private static final Map<String, TokenType> keywordMap = new HashMap<>();
    static {
        keywordMap.put("SELECT", TokenType.KEYWORD_SELECT);
        keywordMap.put("FROM",   TokenType.KEYWORD_FROM);
        keywordMap.put("WHERE",  TokenType.KEYWORD_WHERE);
        keywordMap.put("INSERT", TokenType.KEYWORD_INSERT);
        keywordMap.put("INTO",   TokenType.KEYWORD_INTO);
        keywordMap.put("VALUES", TokenType.KEYWORD_VALUES);
        keywordMap.put("DELETE", TokenType.KEYWORD_DELETE);
        keywordMap.put("UPDATE", TokenType.KEYWORD_UPDATE);
        keywordMap.put("SET",    TokenType.KEYWORD_SET);
        keywordMap.put("CREATE", TokenType.KEYWORD_CREATE);
        keywordMap.put("TABLE",  TokenType.KEYWORD_TABLE);
        keywordMap.put("DROP",   TokenType.KEYWORD_DROP);
        keywordMap.put("AND",    TokenType.KEYWORD_AND);
        keywordMap.put("OR",     TokenType.KEYWORD_OR);
        keywordMap.put("NOT",    TokenType.KEYWORD_NOT);
        keywordMap.put("ORDER",  TokenType.KEYWORD_ORDER);
        keywordMap.put("BY",     TokenType.KEYWORD_BY);
        keywordMap.put("GROUP",  TokenType.KEYWORD_GROUP);
        keywordMap.put("HAVING", TokenType.KEYWORD_HAVING);
        keywordMap.put("LIMIT",  TokenType.KEYWORD_LIMIT);
        keywordMap.put("JOIN",   TokenType.KEYWORD_JOIN);
        keywordMap.put("ON",     TokenType.KEYWORD_ON);
        keywordMap.put("AS",     TokenType.KEYWORD_AS);
        keywordMap.put("DISTINCT", TokenType.KEYWORD_DISTINCT);
    }

    // 状态枚举
    private enum State { START, IN_WORD, IN_NUMBER, IN_STRING, IN_OPERATOR }

    // 输入
    private final String input;
    private int position;
    private final int length;

    /**
     * 构造函数
     * @param input SQL字符串
     */
    public Lexer(String input) {
        this.input = input;
        this.position = 0;
        this.length = input.length();
    }

    /**
     * 执行词法分析，返回Token列表
     *
     * @return 解析出的所有Token
     */
    public List<Token> analyze() {
        List<Token> tokens = new ArrayList<>();
        State currentState = State.START;
        StringBuilder buffer = new StringBuilder();

        while (position < length) {
            char ch = input.charAt(position);

            switch (currentState) {
                case START:
                    currentState = handleStart(ch, buffer, tokens);
                    break;

                case IN_WORD:
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        buffer.append(ch);
                        position++;
                    } else {
                        emitWord(tokens, buffer.toString());
                        buffer.setLength(0);
                        currentState = State.START;
                    }
                    break;

                case IN_NUMBER:
                    if (Character.isDigit(ch)) {
                        buffer.append(ch);
                        position++;
                    } else if (ch == '.' && !buffer.toString().contains(".")) {
                        // 浮点数的小数点
                        buffer.append(ch);
                        position++;
                    } else {
                        emitNumber(tokens, buffer.toString());
                        buffer.setLength(0);
                        currentState = State.START;
                    }
                    break;

                case IN_STRING:
                    buffer.append(ch);
                    position++;
                    // 遇到配对的引号就结束
                    if ((buffer.charAt(0) == '\'' && ch == '\'') ||
                        (buffer.charAt(0) == '"' && ch == '"')) {
                        tokens.add(new Token(TokenType.STRING, buffer.toString()));
                        buffer.setLength(0);
                        currentState = State.START;
                    }
                    break;

                case IN_OPERATOR:
                    if (isOperatorChar(ch)) {
                        buffer.append(ch);
                        position++;
                    } else {
                        emitOperator(tokens, buffer.toString());
                        buffer.setLength(0);
                        currentState = State.START;
                    }
                    break;
            }
        }

        // 处理最后的Token
        if (buffer.length() > 0) {
            switch (currentState) {
                case IN_WORD: emitWord(tokens, buffer.toString()); break;
                case IN_NUMBER: emitNumber(tokens, buffer.toString()); break;
                case IN_STRING: tokens.add(new Token(TokenType.STRING, buffer.toString())); break;
                case IN_OPERATOR: emitOperator(tokens, buffer.toString()); break;
            }
        }

        tokens.add(new Token(TokenType.EOF, "EOF"));
        return tokens;
    }

    // ==================== 状态处理 ====================

    /** 处理初始态：根据遇到的字符类型决定进入哪个状态 */
    private State handleStart(char ch, StringBuilder buffer, List<Token> tokens) {
        // 跳过空白字符
        if (Character.isWhitespace(ch)) {
            position++;
            return State.START;
        }

        // 字母 → 进入读单词状态
        if (Character.isLetter(ch) || ch == '_') {
            buffer.append(ch);
            position++;
            return State.IN_WORD;
        }

        // 数字 → 进入读数字状态
        if (Character.isDigit(ch)) {
            buffer.append(ch);
            position++;
            return State.IN_NUMBER;
        }

        // 引号 → 进入读字符串状态
        if (ch == '\'' || ch == '"') {
            buffer.append(ch);
            position++;
            return State.IN_STRING;
        }

        // 运算符字符 → 进入读运算符状态
        if (isOperatorChar(ch)) {
            buffer.append(ch);
            position++;
            return State.IN_OPERATOR;
        }

        // 单字符分隔符（直接输出Token）
        switch (ch) {
            case '(': tokens.add(new Token(TokenType.LEFT_PAREN, "(")); position++; return State.START;
            case ')': tokens.add(new Token(TokenType.RIGHT_PAREN, ")")); position++; return State.START;
            case ',': tokens.add(new Token(TokenType.COMMA, ",")); position++; return State.START;
            case ';': tokens.add(new Token(TokenType.SEMICOLON, ";")); position++; return State.START;
            case '*': tokens.add(new Token(TokenType.STAR, "*")); position++; return State.START;
        }

        // 无法识别的字符
        throw new RuntimeException("无法识别的字符: '" + ch + "' (位置=" + position + ")");
    }

    // ==================== Token输出方法 ====================

    /** 判断单词是关键字还是标识符，输出对应Token */
    private void emitWord(List<Token> tokens, String word) {
        String upperWord = word.toUpperCase();
        TokenType type = keywordMap.get(upperWord);
        if (type != null) {
            tokens.add(new Token(type, upperWord));
        } else {
            tokens.add(new Token(TokenType.IDENTIFIER, word));
        }
    }

    /** 判断数字是整数还是浮点数，输出对应Token */
    private void emitNumber(List<Token> tokens, String number) {
        if (number.contains(".")) {
            tokens.add(new Token(TokenType.FLOAT, number));
        } else {
            tokens.add(new Token(TokenType.INTEGER, number));
        }
    }

    /** 识别运算符的具体类型 */
    private void emitOperator(List<Token> tokens, String operator) {
        switch (operator) {
            case "=":  tokens.add(new Token(TokenType.EQUALS, "=")); break;
            case ">":  tokens.add(new Token(TokenType.GREATER_THAN, ">")); break;
            case "<":  tokens.add(new Token(TokenType.LESS_THAN, "<")); break;
            case ">=": tokens.add(new Token(TokenType.GREATER_EQUAL, ">=")); break;
            case "<=": tokens.add(new Token(TokenType.LESS_EQUAL, "<=")); break;
            case "!=": tokens.add(new Token(TokenType.NOT_EQUAL1, "!=")); break;
            case "<>": tokens.add(new Token(TokenType.NOT_EQUAL2, "<>")); break;
            case "+":  tokens.add(new Token(TokenType.PLUS, "+")); break;
            case "-":  tokens.add(new Token(TokenType.MINUS, "-")); break;
            case "*":  tokens.add(new Token(TokenType.MULTIPLY, "*")); break;
            default:
                throw new RuntimeException("无法识别的运算符: " + operator);
        }
    }

    /** 判断字符是否可能构成运算符 */
    private boolean isOperatorChar(char ch) {
        return ch == '=' || ch == '>' || ch == '<' ||
               ch == '!' || ch == '+' || ch == '-';
    }
}
