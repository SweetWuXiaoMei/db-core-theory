package com.ourdatabase.core;

/**
 * 词法分析器 —— 负责把SQL字符串拆分成一个一个的"单词"（Token）
 *
 * 这是查询引擎的第一个子模块。
 *
 * 工作原理（简化版）：
 * 1. 用空格把SQL字符串切开
 * 2. 去掉空字符串
 * 3. 识别每个单词的类型（关键字、标识符、运算符、字面量等）
 * 4. 返回单词数组
 */
public class Lexer {

    /**
     * 分析SQL字符串，拆成单词列表
     *
     * @param sql 用户输入的SQL语句
     * @return 拆分后的单词数组
     */
    public String[] analyze(String sql) {
        // 第1步：用空格拆分SQL字符串
        // 例如："SELECT name FROM student" -> ["SELECT", "name", "FROM", "student"]
        String[] rawTokens = sql.split(" ");

        // 第2步：过滤掉空字符串（多个连续空格会产生空字符串）
        int validTokenCount = 0;
        for (String token : rawTokens) {
            if (token.length() > 0) {
                validTokenCount++;
            }
        }

        // 第3步：把有效单词放入新数组
        String[] result = new String[validTokenCount];
        int index = 0;
        for (String token : rawTokens) {
            if (token.length() > 0) {
                result[index] = token;
                index++;
            }
        }

        // 第4步：打印每个单词的类型（帮助你理解词法分析的结果）
        for (int i = 0; i < result.length; i++) {
            String typeDesc = identifyTokenType(result[i]);
            System.out.println("    Token[" + i + "]: \"" + result[i] + "\" -> " + typeDesc);
        }

        return result;
    }

    /**
     * 识别一个单词的类型
     *
     * 在真实的数据库词法分析器中，这里会用"有限状态机（FSM）"来精确识别。
     * 第8章会详细解释什么是有限状态机，第9章会用Java实现。
     *
     * @param token 要识别的单词
     * @return 该单词的类型说明
     */
    private String identifyTokenType(String token) {
        String upperToken = token.toUpperCase();

        // 判断是否为SQL关键字
        switch (upperToken) {
            case "SELECT": return "关键字(SELECT)";
            case "FROM":   return "关键字(FROM)";
            case "WHERE":  return "关键字(WHERE)";
            case "INSERT": return "关键字(INSERT)";
            case "INTO":   return "关键字(INTO)";
            case "VALUES": return "关键字(VALUES)";
            case "DELETE": return "关键字(DELETE)";
            case "UPDATE": return "关键字(UPDATE)";
            case "SET":    return "关键字(SET)";
            case "CREATE": return "关键字(CREATE)";
            case "TABLE":  return "关键字(TABLE)";
            case "AND":    return "关键字(AND)";
            case "OR":     return "关键字(OR)";
            case "DROP":   return "关键字(DROP)";
            case "ALTER":  return "关键字(ALTER)";
        }

        // 判断是否为运算符
        if (token.equals("=") || token.equals(">") || token.equals("<") ||
                token.equals(">=") || token.equals("<=") || token.equals("!=") ||
                token.equals("<>")) {
            return "运算符";
        }

        // 判断是否为分隔符
        if (token.equals(","))  return "分隔符(逗号)";
        if (token.equals("("))  return "分隔符(左括号)";
        if (token.equals(")"))  return "分隔符(右括号)";
        if (token.equals(";"))  return "分隔符(分号)";

        // 判断是否为星号（表示查询所有列）
        if (token.equals("*")) return "通配符(所有列)";

        // 尝试解析为数字
        try {
            Integer.parseInt(token);
            return "数字(字面量)";
        } catch (NumberFormatException e) {
            // 不是数字，继续判断
        }

        // 判断是否为带引号的字符串
        if ((token.startsWith("'") && token.endsWith("'")) ||
                (token.startsWith("\"") && token.endsWith("\""))) {
            return "字符串(字面量)";
        }

        // 剩下的默认当作标识符（表名、列名等）
        return "标识符(表名/列名)";
    }
}
