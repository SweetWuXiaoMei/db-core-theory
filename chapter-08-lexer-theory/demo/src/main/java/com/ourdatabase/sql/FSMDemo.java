package com.ourdatabase.sql;

import java.util.*;

/**
 * 有限状态机（FSM）演示 —— 第8章Demo
 *
 * 演示用有限状态机识别SQL Token：
 * 1. 关键字（SELECT, FROM, WHERE等）
 * 2. 标识符（表名、列名）
 * 3. 数字
 * 4. 字符串（'xxx'）
 * 5. 运算符（=, >, <等）
 *
 * 状态机包含5个状态：
 * - 初始态（START）
 * - 读单词（READING_WORD）
 * - 读数字（READING_NUMBER）
 * - 读字符串（READING_STRING）
 * - 读运算符（READING_OPERATOR）
 */
public class FSMDemo {

    // 状态定义
    enum State { START, READING_WORD, READING_NUMBER, READING_STRING, READING_OPERATOR }

    // SQL关键字集合
    private static final Set<String> KEYWORDS = Set.of(
        "SELECT", "FROM", "WHERE", "INSERT", "INTO", "VALUES",
        "DELETE", "UPDATE", "SET", "CREATE", "TABLE", "DROP",
        "AND", "OR", "NOT", "IN", "LIKE", "ORDER", "BY", "GROUP",
        "HAVING", "LIMIT", "OFFSET", "JOIN", "ON", "AS", "DISTINCT"
    );

    public static void main(String[] args) {
        printTitle("有限状态机（FSM）词法分析演示");

        String[] testSqls = {
            "SELECT name FROM student WHERE id = 100",
            "INSERT INTO student VALUES 200 '张三'",
            "DELETE FROM student WHERE age > 20",
        };

        for (String sql : testSqls) {
            System.out.println("\n输入SQL: " + sql);
            System.out.println("-".repeat(40));

            List<Token> tokens = analyze(sql);
            for (int i = 0; i < tokens.size(); i++) {
                System.out.println("  Token[" + i + "] " + tokens.get(i));
            }
        }

        printTitle("演示总结");
        System.out.println("1. 有限状态机用'状态+转移'来逐字符识别Token");
        System.out.println("2. 关键字和标识符的区别：是否在关键字列表中");
        System.out.println("3. 空格是Token之间的分隔符（也有结束当前Token的作用）");
    }

    /**
     * 核心：有限状态机词法分析
     *
     * @param sql 输入的SQL字符串
     * @return Token列表
     */
    static List<Token> analyze(String sql) {
        List<Token> tokens = new ArrayList<>();
        State currentState = State.START;
        StringBuilder currentContent = new StringBuilder();

        // 逐字符扫描
        for (int i = 0; i < sql.length(); i++) {
            char ch = sql.charAt(i);

            switch (currentState) {
                case START:
                    if (Character.isLetter(ch) || ch == '_' || ch == '*') {
                        // 遇到字母 → 开始读单词
                        currentState = State.READING_WORD;
                        currentContent.append(ch);
                    } else if (Character.isDigit(ch)) {
                        // 遇到数字 → 开始读数字
                        currentState = State.READING_NUMBER;
                        currentContent.append(ch);
                    } else if (ch == '\'' || ch == '"') {
                        // 遇到引号 → 开始读字符串
                        currentState = State.READING_STRING;
                        currentContent.append(ch);
                    } else if (isOperatorChar(ch)) {
                        // 遇到运算符 → 开始读运算符
                        currentState = State.READING_OPERATOR;
                        currentContent.append(ch);
                    }
                    // 空格和逗号等 → 忽略
                    break;

                case READING_WORD:
                    if (Character.isLetterOrDigit(ch) || ch == '_') {
                        // 还是单词的一部分 → 继续读
                        currentContent.append(ch);
                    } else {
                        // 单词结束 → 输出Token
                        emitWordToken(tokens, currentContent.toString());
                        currentContent.setLength(0); // 清空
                        currentState = State.START;
                        // 回退一个字符（重新处理这个字符）
                        i--;
                    }
                    break;

                case READING_NUMBER:
                    if (Character.isDigit(ch) || ch == '.') {
                        currentContent.append(ch);
                    } else {
                        tokens.add(new Token(TokenType.NUMBER, currentContent.toString()));
                        currentContent.setLength(0);
                        currentState = State.START;
                        i--;
                    }
                    break;

                case READING_STRING:
                    currentContent.append(ch);
                    if (ch == '\'' || ch == '"') {
                        // 字符串结束
                        tokens.add(new Token(TokenType.STRING, currentContent.toString()));
                        currentContent.setLength(0);
                        currentState = State.START;
                    }
                    break;

                case READING_OPERATOR:
                    if (isOperatorChar(ch)) {
                        currentContent.append(ch);
                    } else {
                        tokens.add(new Token(TokenType.OPERATOR, currentContent.toString()));
                        currentContent.setLength(0);
                        currentState = State.START;
                        i--;
                    }
                    break;
            }
        }

        // 处理最后一个Token（如果有）
        if (currentContent.length() > 0) {
            switch (currentState) {
                case READING_WORD:
                    emitWordToken(tokens, currentContent.toString());
                    break;
                case READING_NUMBER:
                    tokens.add(new Token(TokenType.NUMBER, currentContent.toString()));
                    break;
                case READING_STRING:
                    tokens.add(new Token(TokenType.STRING, currentContent.toString()));
                    break;
                case READING_OPERATOR:
                    tokens.add(new Token(TokenType.OPERATOR, currentContent.toString()));
                    break;
            }
        }

        return tokens;
    }

    /** 判断关键字还是标识符，输出对应Token */
    private static void emitWordToken(List<Token> tokens, String word) {
        if (word.equals("*")) {
            tokens.add(new Token(TokenType.STAR, word));
        } else if (KEYWORDS.contains(word.toUpperCase())) {
            tokens.add(new Token(TokenType.KEYWORD, word.toUpperCase()));
        } else {
            tokens.add(new Token(TokenType.IDENTIFIER, word));
        }
    }

    /** 判断字符是否为运算符字符 */
    private static boolean isOperatorChar(char ch) {
        return ch == '=' || ch == '>' || ch == '<' ||
               ch == '!' || ch == '+' || ch == '-' ||
               ch == '*' || ch == '/';
    }

    // ==================== Token类型和Token类 ====================

    enum TokenType { KEYWORD, IDENTIFIER, NUMBER, STRING, OPERATOR, STAR }

    static class Token {
        TokenType type;
        String value;

        Token(TokenType type, String value) {
            this.type = type;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format("%-8s : \"%s\"", type, value);
        }
    }

    private static void printTitle(String title) {
        System.out.println("=".repeat(50));
        System.out.println("  " + title);
        System.out.println("=".repeat(50));
    }
}
