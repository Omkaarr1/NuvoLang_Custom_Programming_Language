import java.util.*;

class Lexer {
    private final String input;
    private int pos;
    private final int length;

    public Lexer(String input) {
        this.input = input;
        this.pos = 0;
        this.length = input.length();
    }

    private boolean isAtEnd() {
        return pos >= length;
    }

    private char peek() {
        return isAtEnd() ? '\0' : input.charAt(pos);
    }

    private char advance() {
        return input.charAt(pos++);
    }

    private boolean match(char expected) {
        if (isAtEnd() || input.charAt(pos) != expected) return false;
        pos++;
        return true;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            char c = advance();
            switch (c) {
                case ' ': case '\t': case '\r': case '\n':
                    break;
                case '+':
                    if (match('+')) {
                        tokens.add(new Token(TokenType.PLUS_PLUS, "++"));
                    } else if (match('=')) {
                        tokens.add(new Token(TokenType.PLUS_EQ, "+="));
                    } else {
                        tokens.add(new Token(TokenType.PLUS, "+"));
                    }
                    break;
                case '-':
                    if (match('-')) {
                        tokens.add(new Token(TokenType.MINUS_MINUS, "--"));
                    } else if (match('=')) {
                        tokens.add(new Token(TokenType.MINUS_EQ, "-="));
                    } else if (match('>')) {
                        tokens.add(new Token(TokenType.ARROW, "->"));
                    } else {
                        tokens.add(new Token(TokenType.MINUS, "-"));
                    }
                    break;
                case '*':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.STAR_EQ, "*="));
                    } else {
                        tokens.add(new Token(TokenType.STAR, "*"));
                    }
                    break;
                case '/':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.SLASH_EQ, "/="));
                    } else {
                        tokens.add(new Token(TokenType.SLASH, "/"));
                    }
                    break;
                case '%':
                    tokens.add(new Token(TokenType.MOD, "%"));
                    break;
                case '=':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.EQ_EQ, "=="));
                    } else {
                        tokens.add(new Token(TokenType.ASSIGN, "="));
                    }
                    break;
                case '!':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.NOT_EQ, "!="));
                    } else {
                        tokens.add(new Token(TokenType.NOT, "!"));
                    }
                    break;
                case '>':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.GT_EQ, ">="));
                    } else {
                        tokens.add(new Token(TokenType.GT, ">"));
                    }
                    break;
                case '<':
                    if (match('=')) {
                        tokens.add(new Token(TokenType.LT_EQ, "<="));
                    } else {
                        tokens.add(new Token(TokenType.LT, "<"));
                    }
                    break;
                case '&':
                    if (match('&')) {
                        tokens.add(new Token(TokenType.AND_AND, "&&"));
                    }
                    break;
                case '|':
                    if (match('|')) {
                        tokens.add(new Token(TokenType.OR_OR, "||"));
                    }
                    break;
                case '{':
                    tokens.add(new Token(TokenType.LBRACE, "{"));
                    break;
                case '}':
                    tokens.add(new Token(TokenType.RBRACE, "}"));
                    break;
                case '(':
                    tokens.add(new Token(TokenType.LPAREN, "("));
                    break;
                case ')':
                    tokens.add(new Token(TokenType.RPAREN, ")"));
                    break;
                case '"':
                    tokens.add(new Token(TokenType.STRING, readString()));
                    break;
                default:
                    if (isDigit(c)) {
                        tokens.add(numberToken(c));
                    } else if (isAlpha(c)) {
                        tokens.add(identifierToken(c));
                    } else {
                        // Ignore unknown characters or handle as needed
                    }
            }
        }
        tokens.add(new Token(TokenType.EOF, ""));
        return tokens;
    }

    private Token numberToken(char first) {
        StringBuilder sb = new StringBuilder();
        sb.append(first);
        while (!isAtEnd() && (isDigit(peek()) || peek() == '.')) {
            sb.append(advance());
        }
        return new Token(TokenType.NUMBER, sb.toString());
    }

    private Token identifierToken(char first) {
        StringBuilder sb = new StringBuilder();
        sb.append(first);
        while (!isAtEnd() && isAlphaNumeric(peek())) {
            sb.append(advance());
        }
        String word = sb.toString();
        switch (word) {
            case "print":
                return new Token(TokenType.PRINT, word);
            case "if":
                return new Token(TokenType.IF, word);
            case "else":
                return new Token(TokenType.ELSE, word);
            case "loop":
                return new Token(TokenType.LOOP, word);
            case "to":
                return new Token(TokenType.TO, word);
            case "input":
                return new Token(TokenType.INPUT, word);
            case "while":
                return new Token(TokenType.WHILE, word);
            default:
                return new Token(TokenType.IDENTIFIER, word);
        }
    }

    private String readString() {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd() && peek() != '"') {
            sb.append(advance());
        }
        if (!isAtEnd()) advance(); // consume closing "
        return sb.toString();
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
