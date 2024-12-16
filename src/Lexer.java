package src;

import java.util.*;

public class Lexer {
    private final String input;
    private int pos;
    private final int length;
    private int line;
    private int column;

    public Lexer(String input) {
        this.input = input;
        this.pos = 0;
        this.length = input.length();
        this.line = 1;
        this.column = 1;
    }

    private boolean isAtEnd() {
        return pos >= length;
    }

    private char peek() {
        return isAtEnd() ? '\0' : input.charAt(pos);
    }

    private char peekNext() {
        return (pos + 1 >= length) ? '\0' : input.charAt(pos + 1);
    }

    private char advance() {
        char c = input.charAt(pos++);
        if (c == '\n') {
            line++;
            column = 1;
        } else {
            column++;
        }
        return c;
    }

    private boolean match(char expected) {
        if (isAtEnd() || input.charAt(pos) != expected)
            return false;
        pos++;
        column++;
        return true;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            int startLine = line;
            int startColumn = column;
            char c = advance();
            switch (c) {
                case ' ':
                case '\t':
                case '\r':
                case '\n':
                    // Skip whitespace
                    break;
                case '.':
                    tokens.add(new Token(TokenType.DOT, ".", startLine, startColumn));
                    break;
                case '+':
                    if (match('+')) {
                        tokens.add(new Token(TokenType.PLUS_PLUS, "++", startLine, startColumn));
                    } else if (match('=')) {
                        tokens.add(new Token(TokenType.PLUS_EQ, "+=", startLine, startColumn));
                    } else {
                        tokens.add(new Token(TokenType.PLUS, "+", startLine, startColumn));
                    }
                    break;
                case '-':
                    if (match('-')) {
                        tokens.add(new Token(TokenType.MINUS_MINUS, "--", startLine, startColumn));
                    } else if (match('=')) {
                        tokens.add(new Token(TokenType.MINUS_EQ, "-=", startLine, startColumn));
                    } else if (match('>')) {
                        tokens.add(new Token(TokenType.ARROW, "->", startLine, startColumn));
                    } else {
                        tokens.add(new Token(TokenType.MINUS, "-", startLine, startColumn));
                    }
                    break;
                case '*':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.STAR_EQ, "*=", startLine, startColumn));
                    } else {
                        tokens.add(new Token(TokenType.STAR, "*", startLine, startColumn));
                    }
                    break;
                case '/':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.SLASH_EQ, "/=", startLine, startColumn));
                    } else if (match('/')) {
                        // Comment, consume until end of line
                        while (!isAtEnd() && peek() != '\n')
                            advance();
                    } else {
                        tokens.add(new Token(TokenType.SLASH, "/", startLine, startColumn));
                    }
                    break;
                case '%':
                    tokens.add(new Token(TokenType.MOD, "%", startLine, startColumn));
                    break;
                case '=':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.EQ_EQ, "==", startLine, startColumn));
                    } else {
                        tokens.add(new Token(TokenType.ASSIGN, "=", startLine, startColumn));
                    }
                    break;
                case '!':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.NOT_EQ, "!=", startLine, startColumn));
                    } else {
                        tokens.add(new Token(TokenType.NOT, "!", startLine, startColumn));
                    }
                    break;
                case '>':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.GT_EQ, ">=", startLine, startColumn));
                    } else {
                        tokens.add(new Token(TokenType.GT, ">", startLine, startColumn));
                    }
                    break;
                case '<':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.LT_EQ, "<=", startLine, startColumn));
                    } else {
                        tokens.add(new Token(TokenType.LT, "<", startLine, startColumn));
                    }
                    break;
                case '&':
                    if (match('&')) {
                        tokens.add(new Token(TokenType.AND_AND, "&&", startLine, startColumn));
                    } else {
                        error(startLine, startColumn, "Unexpected character '&'");
                    }
                    break;
                case '|':
                    if (match('|')) {
                        tokens.add(new Token(TokenType.OR_OR, "||", startLine, startColumn));
                    } else {
                        error(startLine, startColumn, "Unexpected character '|'");
                    }
                    break;
                case '{':
                    tokens.add(new Token(TokenType.LBRACE, "{", startLine, startColumn));
                    break;
                case '}':
                    tokens.add(new Token(TokenType.RBRACE, "}", startLine, startColumn));
                    break;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, "(", startLine, startColumn));
                    break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, ")", startLine, startColumn));
                    break;
                case '[':
                    tokens.add(new Token(TokenType.LBRACKET, "[", startLine, startColumn));
                    break;
                case ']':
                    tokens.add(new Token(TokenType.RBRACKET, "]", startLine, startColumn));
                    break;
                case ',':
                    tokens.add(new Token(TokenType.COMMA, ",", startLine, startColumn));
                    break;
                case ';':
                    tokens.add(new Token(TokenType.SEMICOLON, ";", startLine, startColumn));
                    break;
                case '"':
                    tokens.add(new Token(TokenType.STRING, readString(), startLine, startColumn));
                    break;
                default:
                    if (isDigit(c)) {
                        tokens.add(numberToken(c, startLine, startColumn));
                    } else if (isAlpha(c) || c == '@') {
                        tokens.add(identifierToken(c, startLine, startColumn));
                    } else {
                        error(startLine, startColumn, "Unexpected character '" + c + "'");
                    }
            }
        }
        tokens.add(new Token(TokenType.EOF, "", line, column));
        return tokens;
    }

    private String readAtIdentifier(char first) {
        StringBuilder sb = new StringBuilder();
        sb.append(first);
        while (!isAtEnd() && isAlphaNumeric(peek())) {
            sb.append(advance());
        }
        return sb.toString();
    }

    private Token identifierToken(char first, int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append(first);
        while (!isAtEnd() && isAlphaNumeric(peek())) {
            sb.append(advance());
        }
        String word = sb.toString();

        boolean isEncrypted = false;
        String actualName = word;
        if (word.startsWith("@ENC")) {
            isEncrypted = true;
            actualName = word.substring(4); // Remove @ENC
        }

        // Check if identifier starts with @EVENT_TRIGGER
        if (word.startsWith("@EVENT_TRIGGER")) {
            return new Token(TokenType.EVENT_TRIGGER, word, startLine, startColumn);
        }

        // Additional keywords
        if (actualName.equals("use"))
            return new Token(TokenType.USE, word, startLine, startColumn);
        if (actualName.equals("print"))
            return new Token(TokenType.PRINT, word, startLine, startColumn);
        if (actualName.equals("if"))
            return new Token(TokenType.IF, word, startLine, startColumn);
        if (actualName.equals("else"))
            return new Token(TokenType.ELSE, word, startLine, startColumn);
        if (actualName.equals("for"))
            return new Token(TokenType.FOR, word, startLine, startColumn);
        if (actualName.equals("to"))
            return new Token(TokenType.TO, word, startLine, startColumn);
        if (actualName.equals("input"))
            return new Token(TokenType.INPUT, word, startLine, startColumn);
        if (actualName.equals("while"))
            return new Token(TokenType.WHILE, word, startLine, startColumn);
        if (actualName.equals("function"))
            return new Token(TokenType.FUNCTION, word, startLine, startColumn);
        if (actualName.equals("return"))
            return new Token(TokenType.RETURN, word, startLine, startColumn);
        if (actualName.equals("true") || actualName.equals("false"))
            return new Token(TokenType.BOOLEAN, word, startLine, startColumn);

        return new Token(TokenType.IDENTIFIER, word, startLine, startColumn);
    }

    private Token numberToken(char first, int startLine, int startColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append(first);
        boolean hasDot = false;
        while (!isAtEnd() && (isDigit(peek()) || peek() == '.')) {
            if (peek() == '.') {
                if (hasDot)
                    break;
                hasDot = true;
            }
            sb.append(advance());
        }
        return new Token(TokenType.NUMBER, sb.toString(), startLine, startColumn);
    }

    private String readString() {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            char c = advance();
            if (c == '\\') {
                if (!isAtEnd()) {
                    char next = advance();
                    switch (next) {
                        case 'n':
                            sb.append('\n');
                            break;
                        case 't':
                            sb.append('\t');
                            break;
                        case '"':
                            sb.append('"');
                            break;
                        case '\\':
                            sb.append('\\');
                            break;
                        default:
                            sb.append(next);
                            break;
                    }
                }
            } else {
                sb.append(c);
            }
        }
        if (!isAtEnd())
            advance(); // consume the closing quote
        else {
            throw new RuntimeException("Unterminated string literal at line " + line + ", column " + column);
        }
        return sb.toString();
    }

    private void error(int line, int column, String message) {
        throw new RuntimeException("Lexer Error at " + line + ":" + column + " - " + message);
    }

    private boolean isDigit(char c) {
        return (c >= '0' && c <= '9');
    }

    private boolean isAlpha(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
}
