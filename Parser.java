// Parser.java

import java.util.*;

abstract class Node {}

// Expression Nodes
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
    List<Node> ifBranch;
    List<Node> elseBranch;
    IfNode(Node condition, List<Node> ifBranch, List<Node> elseBranch) {
        this.condition = condition;
        this.ifBranch = ifBranch;
        this.elseBranch = elseBranch;
    }
}

class ExpressionStatement extends Node {
    Node expr;
    ExpressionStatement(Node expr) { this.expr = expr; }
}

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

class FunctionDefNode extends Node {
    String name;
    List<String> parameters;
    List<Node> body;
    FunctionDefNode(String name, List<String> parameters, List<Node> body) {
        this.name = name;
        this.parameters = parameters;
        this.body = body;
    }
}

class FunctionCallNode extends Node {
    String name;
    List<Node> arguments;
    FunctionCallNode(String name, List<Node> arguments) {
        this.name = name;
        this.arguments = arguments;
    }
}

class ReturnNode extends Node {
    Node value;
    ReturnNode(Node value) {
        this.value = value;
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

    public List<Node> parse() {
        List<Node> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(parseStatement());
        }
        return statements;
    }

    public Node parseStatement() {
        if (match(TokenType.FUNCTION)) {
            return parseFunctionDef();
        }

        if (match(TokenType.RETURN)) {
            return parseReturn();
        }

        if (match(TokenType.LOOP)) {
            return parseLoop();
        }

        if (match(TokenType.WHILE)) {
            consume(TokenType.LPAREN, "Expect '(' after 'while'.");
            Node condition = parseExpression();
            consume(TokenType.RPAREN, "Expect ')' after while condition.");
            consume(TokenType.LBRACE, "Expect '{' to start while body.");
            List<Node> body = parseBlock();
            consume(TokenType.RBRACE, "Expect '}' after while body.");
            // Removed semicolon consumption after while loop
            return new WhileNode(condition, body);
        }

        if (match(TokenType.IF)) {
            consume(TokenType.LPAREN, "Expect '(' after 'if'.");
            Node cond = parseExpression();
            consume(TokenType.RPAREN, "Expect ')' after if condition.");
            consume(TokenType.LBRACE, "Expect '{' to start 'if' branch.");
            List<Node> ifBranch = parseBlock();
            consume(TokenType.RBRACE, "Expect '}' after 'if' branch.");
            List<Node> elseBranch = null;
            if (match(TokenType.ELSE)) {
                consume(TokenType.LBRACE, "Expect '{' to start 'else' branch.");
                elseBranch = parseBlock();
                consume(TokenType.RBRACE, "Expect '}' after 'else' branch.");
            }
            // Removed semicolon consumption after if-else statement
            return new IfNode(cond, ifBranch, elseBranch);
        }

        if (match(TokenType.PRINT)) {
            consume(TokenType.ARROW, "Expect '->' after 'print'");
            Node expr = parseExpression();
            consume(TokenType.SEMICOLON, "Expect ';' after print statement.");
            return new PrintNode(expr);
        }

        if (match(TokenType.INPUT)) {
            consume(TokenType.ARROW, "Expect '->' after 'input'");
            Node prompt = parseExpression();
            consume(TokenType.ARROW, "Expect '->' before variable in input statement");
            Node variable = parseExpression();
            consume(TokenType.SEMICOLON, "Expect ';' after input statement.");
            return new InputNode(prompt, variable);
        }

        Node expr = parseExpression();
        consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new ExpressionStatement(expr);
    }

    private Node parseFunctionDef() {
        // Syntax: function fname(param1, param2, ...) { body };
        Token nameToken = consume(TokenType.IDENTIFIER, "Expect function name.");
        String name = nameToken.lexeme;

        consume(TokenType.LPAREN, "Expect '(' after function name.");
        List<String> parameters = new ArrayList<>();
        if (!match(TokenType.RPAREN)) {
            do {
                Token param = consume(TokenType.IDENTIFIER, "Expect parameter name.");
                parameters.add(param.lexeme);
            } while (match(TokenType.COMMA));
            consume(TokenType.RPAREN, "Expect ')' after parameters.");
        }

        consume(TokenType.LBRACE, "Expect '{' to start function body.");
        List<Node> body = parseBlock();
        consume(TokenType.RBRACE, "Expect '}' after function body.");
        consume(TokenType.SEMICOLON, "Expect ';' after function definition.");
        return new FunctionDefNode(name, parameters, body);
    }

    private Node parseReturn() {
        // Syntax: return expr;
        Node value = null;
        if (!match(TokenType.SEMICOLON)) {
            value = parseExpression();
            consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        }
        return new ReturnNode(value);
    }

    private Node parseLoop() {
        // Syntax: loop <start> to <end> { <body> };
        Node start = parseExpression();
        consume(TokenType.TO, "Expect 'to' in loop declaration.");
        Node end = parseExpression();
        consume(TokenType.LBRACE, "Expect '{' to start loop body.");
        List<Node> body = parseBlock();
        consume(TokenType.RBRACE, "Expect '}' after loop body.");
        consume(TokenType.SEMICOLON, "Expect ';' after loop.");
        return new LoopNode(start, end, body);
    }

    private List<Node> parseBlock() {
        List<Node> statements = new ArrayList<>();
        while (!isAtEnd() && peek().type != TokenType.RBRACE) {
            statements.add(parseStatement());
        }
        return statements;
    }

    public Node parseExpression() { return parseAssignment(); }

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
            String name = tokens.get(current - 1).lexeme;
            if (match(TokenType.LPAREN)) {
                // Function call
                List<Node> args = new ArrayList<>();
                if (!match(TokenType.RPAREN)) {
                    do {
                        args.add(parseExpression());
                    } while (match(TokenType.COMMA));
                    consume(TokenType.RPAREN, "Expect ')' after arguments.");
                }
                return new FunctionCallNode(name, args);
            }
            return new VariableNode(name);
        }
        if (match(TokenType.LPAREN)) {
            Node expr = parseExpression();
            consume(TokenType.RPAREN, "Expect ')' after expression.");
            return expr;
        }
        throw new RuntimeException("Parse Error: Unexpected token " + peek().type + " (" + peek().lexeme + ")");
    }

    private Object parseNumber(String s) {
        if (s.contains(".")) return Double.parseDouble(s);
        return Integer.parseInt(s);
    }
}
