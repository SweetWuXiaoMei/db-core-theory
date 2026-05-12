package com.ourdatabase.sql;

import java.util.*;

/**
 * 语法分析演示 —— 第10章主程序
 *
 * 演示递归下降解析器将SQL解析为抽象语法树（AST）
 * 并生成执行计划
 */
public class ParserDemo {

    public static void main(String[] args) {
        printTitle("SQL语法分析器演示");

        String[] testCases = {
            "SELECT name, age FROM student WHERE id = 100",
            "INSERT INTO student VALUES (200, '张三', 20)",
            "DELETE FROM student WHERE age > 22",
            "CREATE TABLE course (id, name, credit)",
        };

        for (String sql : testCases) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("SQL: " + sql);
            System.out.println("-".repeat(50));

            // 1. 词法分析
            SimpleLexer lexer = new SimpleLexer(sql);
            List<String[]> tokens = lexer.analyze();

            // 2. 语法分析
            SimpleParser parser = new SimpleParser(tokens);
            ASTNode ast = parser.parse();

            // 3. 打印AST
            System.out.println("\n抽象语法树（AST）：");
            printAST(ast, "");

            // 4. 生成执行计划
            System.out.println("\n执行计划：");
            String plan = generatePlan(ast);
            System.out.println(plan);
        }
    }

    /** 递归打印AST */
    static void printAST(ASTNode node, String indent) {
        if (node == null) return;
        System.out.println(indent + "├─ " + node.type + ": " + node.value);

        for (ASTNode child : node.children) {
            printAST(child, indent + "│  ");
        }
    }

    /** 根据AST生成执行计划 */
    static String generatePlan(ASTNode ast) {
        StringBuilder sb = new StringBuilder();
        String operation = ast.type;

        switch (operation) {
            case "SELECT":
                sb.append("执行计划：\n");
                sb.append("  1. 打开表 '" + ast.getChildValue("表名") + "'\n");
                String condition = ast.getChildValue("条件");
                if (condition != null && !condition.isEmpty()) {
                    sb.append("  2. 检查是否有索引可用（无 → 全表扫描）\n");
                    sb.append("  3. 逐行检查过滤条件: " + condition + "\n");
                } else {
                    sb.append("  2. 全表扫描（无过滤条件）\n");
                }
                sb.append("  4. 提取需要的列: " + ast.getChildValue("列名") + "\n");
                sb.append("  5. 返回结果集");
                break;
            case "INSERT":
                sb.append("执行计划：\n");
                sb.append("  1. 打开表 '" + ast.getChildValue("表名") + "'\n");
                sb.append("  2. 分配新的数据页（如需要）\n");
                sb.append("  3. 写入数据行\n");
                sb.append("  4. 更新索引（如有B+树索引）\n");
                sb.append("  5. 记录Redo日志\n");
                sb.append("  6. 返回插入成功");
                break;
            case "DELETE":
                sb.append("执行计划：\n");
                sb.append("  1. 打开表 '" + ast.getChildValue("表名") + "'\n");
                sb.append("  2. 找到符合条件的行\n");
                sb.append("  3. 标记行为已删除\n");
                sb.append("  4. 记录Undo日志（便于回滚）\n");
                sb.append("  5. 返回删除行数");
                break;
            case "CREATE":
                sb.append("执行计划：\n");
                sb.append("  1. 创建表 '" + ast.getChildValue("表名") + "'\n");
                sb.append("  2. 分配第一个数据页\n");
                sb.append("  3. 在系统表中记录表结构\n");
                sb.append("  4. 返回创建成功");
                break;
        }
        return sb.toString();
    }

    private static void printTitle(String title) {
        System.out.println("=".repeat(50));
        System.out.println("  " + title);
        System.out.println("=".repeat(50));
    }
}

// ==================== AST节点 ====================

class ASTNode {
    String type;           // SELECT, INSERT, DELETE, CREATE, 表名, 列名, 条件 等
    String value;          // 节点的值
    List<ASTNode> children = new ArrayList<>();

    ASTNode(String type, String value) {
        this.type = type;
        this.value = value;
    }

    void addChild(ASTNode child) { children.add(child); }

    String getChildValue(String type) {
        for (ASTNode child : children) {
            if (child.type.equals(type)) return child.value;
        }
        return null;
    }
}

// ==================== 简易词法器(内嵌版) ====================

class SimpleLexer {
    private String input;
    private int position;
    private static final Set<String> KEYWORDS = Set.of(
        "SELECT","FROM","WHERE","INSERT","INTO","VALUES",
        "DELETE","UPDATE","SET","CREATE","TABLE","AND","OR"
    );

    SimpleLexer(String input) { this.input = input; this.position = 0; }

    List<String[]> analyze() {
        List<String[]> tokens = new ArrayList<>();
        while (position < input.length()) {
            char c = input.charAt(position);
            if (Character.isWhitespace(c)) { position++; continue; }
            if (c == ',' || c == '(' || c == ')' || c == ';' || c == '*') {
                tokens.add(new String[]{"符号", String.valueOf(c)});
                position++; continue;
            }
            if (c == '=' || c == '>' || c == '<' || c == '!') {
                String op = String.valueOf(c);
                position++;
                if (position < input.length() && input.charAt(position) == '=') { op += "="; position++; }
                if (position < input.length() && input.charAt(position) == '>') { op += ">"; position++; }
                tokens.add(new String[]{"运算符", op});
                continue;
            }
            if (c == '\'' || c == '"') {
                char q = c; position++;
                StringBuilder sb = new StringBuilder();
                while (position < input.length() && input.charAt(position) != q) {
                    sb.append(input.charAt(position)); position++;
                }
                position++; // skip closing quote
                tokens.add(new String[]{"字符串", sb.toString()});
                continue;
            }
            if (Character.isDigit(c)) {
                StringBuilder sb = new StringBuilder();
                while (position < input.length() && Character.isDigit(input.charAt(position))) {
                    sb.append(input.charAt(position)); position++;
                }
                tokens.add(new String[]{"数字", sb.toString()});
                continue;
            }
            if (Character.isLetter(c) || c == '_') {
                StringBuilder sb = new StringBuilder();
                while (position < input.length() && (Character.isLetterOrDigit(input.charAt(position)) || input.charAt(position) == '_')) {
                    sb.append(input.charAt(position)); position++;
                }
                String word = sb.toString();
                String category = KEYWORDS.contains(word.toUpperCase()) ? "关键字" : "标识符";
                tokens.add(new String[]{category, word.toUpperCase()});
                continue;
            }
            position++;
        }
        tokens.add(new String[]{"EOF", ""});
        return tokens;
    }
}

// ==================== 简易语法器（递归下降解析器） ====================

class SimpleParser {
    private List<String[]> tokens;
    private int position;

    SimpleParser(List<String[]> tokens) { this.tokens = tokens; this.position = 0; }

    ASTNode parse() {
        String type = currentValue();
        switch (type) {
            case "SELECT": return parseSelect();
            case "INSERT": return parseInsert();
            case "DELETE": return parseDelete();
            case "CREATE": return parseCreate();
            default: throw new RuntimeException("不支持的SQL类型: " + type);
        }
    }

    private ASTNode parseSelect() {
        ASTNode ast = new ASTNode("SELECT", "SELECT查询");
        advance(); // 跳过 SELECT

        // 解析列名
        StringBuilder colStr = new StringBuilder();
        while (!currentValue().equals("FROM")) {
            if (!currentValue().equals(",") && !currentValue().equals("*")) {
                colStr.append(currentValue());
            }
            if (currentValue().equals("*")) colStr.append("*");
            advance();
        }
        ast.addChild(new ASTNode("列名", colStr.toString()));

        advance(); // 跳过 FROM

        // 表名
        ast.addChild(new ASTNode("表名", currentValue()));
        advance();

        // WHERE子句
        if (currentValue().equals("WHERE")) {
            advance(); // 跳过 WHERE
            StringBuilder cond = new StringBuilder();
            while (!currentValue().equals("EOF") && !currentValue().equals("AND") && !currentValue().equals("OR")) {
                cond.append(currentValue()); advance();
            }
            ast.addChild(new ASTNode("条件", cond.toString()));
        }

        return ast;
    }

    private ASTNode parseInsert() {
        ASTNode ast = new ASTNode("INSERT", "INSERT插入");
        advance(); // INSERT
        advance(); // INTO

        ast.addChild(new ASTNode("表名", currentValue()));
        advance(); // 表名
        advance(); // VALUES

        // 值列表
        StringBuilder valStr = new StringBuilder();
        advance(); // 左括号
        while (!currentValue().equals(")")) {
            if (!currentValue().equals(",")) valStr.append(currentValue()).append(" ");
            advance();
        }
        ast.addChild(new ASTNode("值", valStr.toString().trim()));
        return ast;
    }

    private ASTNode parseDelete() {
        ASTNode ast = new ASTNode("DELETE", "DELETE删除");
        advance(); // DELETE
        advance(); // FROM

        ast.addChild(new ASTNode("表名", currentValue()));
        advance();

        if (currentValue().equals("WHERE")) {
            advance();
            StringBuilder cond = new StringBuilder();
            while (!currentValue().equals("EOF")) { cond.append(currentValue()); advance(); }
            ast.addChild(new ASTNode("条件", cond.toString()));
        }
        return ast;
    }

    private ASTNode parseCreate() {
        ASTNode ast = new ASTNode("CREATE", "CREATE创建");
        advance(); // CREATE
        advance(); // TABLE

        ast.addChild(new ASTNode("表名", currentValue()));
        advance(); // 表名

        // 列名列表
        StringBuilder colStr = new StringBuilder();
        advance(); // 左括号
        while (!currentValue().equals(")")) {
            if (!currentValue().equals(",")) colStr.append(currentValue()).append(" ");
            advance();
        }
        ast.addChild(new ASTNode("列定义", colStr.toString().trim()));
        return ast;
    }

    private String currentValue() { return tokens.get(position)[1]; }
    private void advance() { position++; }
}
