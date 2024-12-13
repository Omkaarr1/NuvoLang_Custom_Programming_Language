// Parser.java

import java.util.*;

abstract class Node {}

// Expression Nodes
class BinaryNode extends Node {
    Node left, right;
    TokenType op;

    BinaryNode(Node left, TokenType op, Node right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }
}

class UnaryNode extends Node {
    TokenType op;
    Node expr;
    boolean postfix;

    UnaryNode(TokenType op, Node expr, boolean postfix) {
        this.op = op;
        this.expr = expr;
        this.postfix = postfix;
    }
}

class LiteralNode extends Node {
    Object value;

    LiteralNode(Object value) {
        this.value = value;
    }
}

class VariableNode extends Node {
    String name;

    VariableNode(String name) {
        this.name = name;
    }
}

class AssignNode extends Node {
    String name;
    TokenType op;
    Node value;

    AssignNode(String name, TokenType op, Node value) {
        this.name = name;
        this.op = op;
        this.value = value;
    }
}

class PrintNode extends Node {
    Node expr;

    PrintNode(Node expr) {
        this.expr = expr;
    }
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

    ExpressionStatement(Node expr) {
        this.expr = expr;
    }
}

class LoopNode extends Node { // This will be removed
    // Deprecated: Replaced by ForNode
}

class ForNode extends Node { // New node for standard for loops
    Node initialization;
    Node condition;
    Node increment;
    List<Node> body;

    ForNode(Node initialization, Node condition, Node increment, List<Node> body) {
        this.initialization = initialization;
        this.condition = condition;
        this.increment = increment;
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

// New ArrayLiteralNode
class ArrayLiteralNode extends Node {
    List<Node> elements;

    ArrayLiteralNode(List<Node> elements) {
        this.elements = elements;
    }
}

public class Parser {
    private final List<Token> tokens;
    private int current = 0;

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Token advanceToken() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == TokenType.EOF;
    }

    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advanceToken();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advanceToken();
        Token token = peek();
        throw error(token, message);
    }

    private RuntimeException error(Token token, String message) {
        return new RuntimeException("Parse Error at " + token.line + ":" + token.column + " - " + message);
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

        if (match(TokenType.FOR)) {
            return parseFor();
        }

        if (match(TokenType.WHILE)) {
            return parseWhile();
        }

        if (match(TokenType.IF)) {
            return parseIf();
        }

        if (match(TokenType.PRINT)) {
            return parsePrint();
        }

        if (match(TokenType.INPUT)) {
            return parseInput();
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
        Token returnToken = previous();
        Node value = null;
        if (!check(TokenType.SEMICOLON)) {
            value = parseExpression();
        }
        consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new ReturnNode(value);
    }

    private Node parseFor() {
        // Syntax: for (initialization; condition; increment) { body }
        consume(TokenType.LPAREN, "Expect '(' after 'for'.");
        Node initialization = parseExpression();
        consume(TokenType.SEMICOLON, "Expect ';' after for initialization.");
        Node condition = parseExpression();
        consume(TokenType.SEMICOLON, "Expect ';' after for condition.");
        Node increment = parseExpression();
        consume(TokenType.RPAREN, "Expect ')' after for clauses.");
        consume(TokenType.LBRACE, "Expect '{' to start for loop body.");
        List<Node> body = parseBlock();
        consume(TokenType.RBRACE, "Expect '}' after for loop body.");
        return new ForNode(initialization, condition, increment, body);
    }

    private Node parseWhile() {
        // Syntax: while (condition) { body }
        consume(TokenType.LPAREN, "Expect '(' after 'while'.");
        Node condition = parseExpression();
        consume(TokenType.RPAREN, "Expect ')' after while condition.");
        consume(TokenType.LBRACE, "Expect '{' to start while body.");
        List<Node> body = parseBlock();
        consume(TokenType.RBRACE, "Expect '}' after while body.");
        return new WhileNode(condition, body);
    }

    private Node parseIf() {
        // Syntax: if (condition) { ifBranch } else { elseBranch }
        consume(TokenType.LPAREN, "Expect '(' after 'if'.");
        Node condition = parseExpression();
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
        return new IfNode(condition, ifBranch, elseBranch);
    }

    private Node parsePrint() {
        // Syntax: print-> expression;
        consume(TokenType.ARROW, "Expect '->' after 'print'.");
        Node expr = parseExpression();
        consume(TokenType.SEMICOLON, "Expect ';' after print statement.");
        return new PrintNode(expr);
    }

    private Node parseInput() {
        // Syntax: input-> prompt -> variable;
        consume(TokenType.ARROW, "Expect '->' after 'input'.");
        Node prompt = parseExpression();
        consume(TokenType.ARROW, "Expect '->' before variable in input statement.");
        Node variable = parseExpression();
        consume(TokenType.SEMICOLON, "Expect ';' after input statement.");
        return new InputNode(prompt, variable);
    }

    private List<Node> parseBlock() {
        List<Node> statements = new ArrayList<>();
        while (!isAtEnd() && peek().type != TokenType.RBRACE) {
            statements.add(parseStatement());
        }
        return statements;
    }

    public Node parseExpression() {
        return parseAssignment();
    }

    private Node parseAssignment() {
        Node left = parseLogicalOr();

        if (match(TokenType.ASSIGN, TokenType.PLUS_EQ, TokenType.MINUS_EQ, TokenType.STAR_EQ, TokenType.SLASH_EQ)) {
            Token op = previous();
            Node right = parseAssignment();
            if (left instanceof VariableNode) {
                return new AssignNode(((VariableNode) left).name, op.type, right);
            } else {
                throw error(op, "Invalid assignment target.");
            }
        }
        return left;
    }

    private Node parseLogicalOr() {
        Node left = parseLogicalAnd();
        while (match(TokenType.OR_OR)) {
            Token op = previous();
            Node right = parseLogicalAnd();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseLogicalAnd() {
        Node left = parseEquality();
        while (match(TokenType.AND_AND)) {
            Token op = previous();
            Node right = parseEquality();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseEquality() {
        Node left = parseComparison();
        while (match(TokenType.EQ_EQ, TokenType.NOT_EQ)) {
            Token op = previous();
            Node right = parseComparison();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseComparison() {
        Node left = parseTerm();
        while (match(TokenType.GT, TokenType.GT_EQ, TokenType.LT, TokenType.LT_EQ)) {
            Token op = previous();
            Node right = parseTerm();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseTerm() {
        Node left = parseFactor();
        while (match(TokenType.PLUS, TokenType.MINUS)) {
            Token op = previous();
            Node right = parseFactor();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseFactor() {
        Node left = parseUnary();
        while (match(TokenType.STAR, TokenType.SLASH, TokenType.MOD)) {
            Token op = previous();
            Node right = parseUnary();
            left = new BinaryNode(left, op.type, right);
        }
        return left;
    }

    private Node parseUnary() {
        if (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS, TokenType.NOT)) {
            Token op = previous();
            Node expr = parseUnary();
            return new UnaryNode(op.type, expr, false);
        }

        Node primary = parsePrimary();

        while (match(TokenType.PLUS_PLUS, TokenType.MINUS_MINUS)) {
            Token op = previous();
            primary = new UnaryNode(op.type, primary, true);
        }

        return primary;
    }

    private Node parsePrimary() {
        if (match(TokenType.NUMBER)) {
            String numStr = previous().lexeme;
            if (numStr.contains(".")) {
                return new LiteralNode(Double.parseDouble(numStr));
            } else {
                return new LiteralNode(Integer.parseInt(numStr));
            }
        }

        if (match(TokenType.STRING)) {
            return new LiteralNode(previous().lexeme);
        }

        if (match(TokenType.BOOLEAN)) {
            String boolStr = previous().lexeme;
            return new LiteralNode(Boolean.parseBoolean(boolStr));
        }

        if (match(TokenType.IDENTIFIER)) {
            String name = previous().lexeme;
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

        if (match(TokenType.LBRACKET)) {
            // Array literal
            List<Node> elements = new ArrayList<>();
            if (!match(TokenType.RBRACKET)) {
                do {
                    elements.add(parseExpression());
                } while (match(TokenType.COMMA));
                consume(TokenType.RBRACKET, "Expect ']' after array elements.");
            }
            return new ArrayLiteralNode(elements);
        }

        Token token = peek();
        throw error(token, "Expect expression.");
    }
}
