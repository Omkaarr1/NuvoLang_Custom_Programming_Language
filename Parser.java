import java.util.*;

abstract class Node {}

class BinaryNode extends Node {
    Node left, right;
    TokenType op;
    BinaryNode(Node left, TokenType op, Node right) {
        this.left = left; this.op = op; this.right = right;
    }
}

class UnaryNode extends Node {
    TokenType op;
    Node expr;
    boolean postfix;
    UnaryNode(TokenType op, Node expr, boolean postfix) {
        this.op = op; this.expr = expr; this.postfix = postfix;
    }
}

class LiteralNode extends Node {
    Object value;
    LiteralNode(Object value) { this.value = value; }
}

class VariableNode extends Node {
    String name;
    VariableNode(String name) { this.name = name; }
}

class AssignNode extends Node {
    String name;
    TokenType op;
    Node value;
    AssignNode(String name, TokenType op, Node value) {
        this.name = name; this.op = op; this.value = value;
    }
}

class PrintNode extends Node {
    Node expr;
    PrintNode(Node expr) { this.expr = expr; }
}

class IfNode extends Node {
    Node condition;
    Node ifBranch;
    Node elseBranch;
    IfNode(Node condition, Node ifBranch, Node elseBranch) {
        this.condition = condition;
        this.ifBranch = ifBranch;
        this.elseBranch = elseBranch;
    }
}

class ExpressionStatement extends Node {
    Node expr;
    ExpressionStatement(Node expr) { this.expr = expr; }
}

// New Node types for loops and input
class LoopNode extends Node {
    Node start;
    Node end;
    List<Node> body;
    LoopNode(Node start, Node end, List<Node> body) {
        this.start = start;
        this.end = end;
        this.body = body;
    }
}

class InputNode extends Node {
    Node prompt;
    Node variable;
    InputNode(Node prompt, Node variable) {
        this.prompt = prompt;
        this.variable = variable;
    }
}

class WhileNode extends Node {
    Node condition;
    List<Node> body;
    WhileNode(Node condition, List<Node> body) {
        this.condition = condition;
        this.body = body;
    }
}

class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token advanceToken() {
        if (!isAtEnd()) current++;
        return tokens.get(current - 1);
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (!isAtEnd() && peek().type == t) {
                advanceToken();
                return true;
            }
        }
        return false;
    }

    private Token consume(TokenType type, String errMsg) {
        if (peek().type == type) return advanceToken();
        throw new RuntimeException("Parse Error: Expected " + type + " but got " + peek().type + ". " + errMsg);
    }

    public Node parseStatement() {
        if (match(TokenType.LOOP)) {
            return parseLoop();
        }

        if (match(TokenType.WHILE)) {
            consume(TokenType.LPAREN, "Expect '(' after 'while'.");
            Node condition = parseExpression();
            consume(TokenType.RPAREN, "Expect ')' after while condition.");
            consume(TokenType.LBRACE, "Expect '{' to start while body.");
            List<Node> body = parseBlock();
            return new WhileNode(condition, body);
        }

        if (match(TokenType.IF)) {
            consume(TokenType.LPAREN, "Expect '(' after if.");
            Node cond = parseExpression();
            consume(TokenType.RPAREN, "Expect ')' after if condition.");
            Node ifBranch = parseStatement();
            Node elseBranch = null;
            if (match(TokenType.ELSE)) {
                elseBranch = parseStatement();
            }
            return new IfNode(cond, ifBranch, elseBranch);
        }

        if (match(TokenType.PRINT)) {
            consume(TokenType.ARROW, "Expect '->' after print");
            Node expr = parseExpression();
            return new PrintNode(expr);
        }

        if (match(TokenType.INPUT)) {
            consume(TokenType.ARROW, "Expect '->' after input");
            Node prompt = parseExpression();
            consume(TokenType.ARROW, "Expect '->' before variable in input statement");
            Node variable = parseExpression();
            return new InputNode(prompt, variable);
        }

        Node expr = parseExpression();
        return new ExpressionStatement(expr);
    }

    private Node parseLoop() {
        // Syntax: loop <start> to <end> { <body> }
        Node start = parseExpression();
        consume(TokenType.TO, "Expect 'to' in loop declaration.");
        Node end = parseExpression();
        consume(TokenType.LBRACE, "Expect '{' to start loop body.");
        List<Node> body = parseBlock();
        return new LoopNode(start, end, body);
    }

    private List<Node> parseBlock() {
        List<Node> statements = new ArrayList<>();
        while (!isAtEnd() && !match(TokenType.RBRACE)) {
            statements.add(parseStatement());
        }
        return statements;
    }

    private Node parseExpression() { return parseAssignment(); }
    private Node parseAssignment() {
        Node left = parseLogicalOr();

        if (match(TokenType.ASSIGN, TokenType.PLUS_EQ, TokenType.MINUS_EQ, TokenType.STAR_EQ, TokenType.SLASH_EQ)) {
            Token op = tokens.get(current - 1);
            Node right = parseAssignment();
            if (left instanceof VariableNode) {
                return new AssignNode(((VariableNode) left).name, op.type, right);
            } else {
                throw new RuntimeException("Invalid assignment target.");
            }
        }
        return left;
    }

    private Node parseLogicalOr() {
        Node left = parseLogicalAnd();
        while (match(TokenType.OR_OR)) {
            Token op = tokens.get(current - 1);
            Node right = parseLogicalAnd();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseLogicalAnd() {
        Node left = parseEquality();
        while (match(TokenType.AND_AND)) {
            Token op = tokens.get(current - 1);
            Node right = parseEquality();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseEquality() {
        Node left = parseComparison();
        while (match(TokenType.EQ_EQ, TokenType.NOT_EQ)) {
            Token op = tokens.get(current - 1);
            Node right = parseComparison();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseComparison() {
        Node left = parseTerm();
        while (match(TokenType.GT, TokenType.GT_EQ, TokenType.LT, TokenType.LT_EQ)) {
            Token op = tokens.get(current - 1);
            Node right = parseTerm();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseTerm() {
        Node left = parseFactor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = tokens.get(current - 1);
            Node right = parseFactor();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseFactor() {
        Node left = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            Token op = tokens.get(current - 1);
            Node right = parseUnary();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseUnary() {
        if (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            Token op = tokens.get(current - 1);
            Node expr = parseUnary();
            return new UnaryNode(op.type, expr, false);
        }

        Node primary = parsePrimary();

        while (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            Token op = tokens.get(current - 1);
            primary = new UnaryNode(op.type, primary, true);
        }

        return primary;
    }

    private Node parsePrimary() {
        if (match(TokenType.NUMBER)) {
            return new LiteralNode(parseNumber(tokens.get(current - 1).lexeme));
        }
        if (match(TokenType.STRING)) {
            return new LiteralNode(tokens.get(current - 1).lexeme);
        }
        if (match(TokenType.IDENTIFIER)) {
            return new VariableNode(tokens.get(current - 1).lexeme);
        }
        if (match(TokenType.LPAREN)) {
            Node expr = parseExpression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
            return expr;
        }
        throw new RuntimeException("Parse Error: Unexpected token " + peek());
    }

    private Object parseNumber(String s) {
        if (s.contains(".")) return Double.parseDouble(s);
        return Integer.parseInt(s);
    }
}
