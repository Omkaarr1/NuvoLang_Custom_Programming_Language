// TokenType.java

public enum TokenType {
    // Single-character tokens
    PLUS, MINUS, STAR, SLASH, MOD, DOT,
    EQ, EQ_EQ, NOT, NOT_EQ,
    GT, LT, GT_EQ, LT_EQ,
    AND_AND, OR_OR,
    PLUS_EQ, MINUS_EQ, STAR_EQ, SLASH_EQ,
    PLUS_PLUS, MINUS_MINUS,
    ARROW,
    COMMA, SEMICOLON,
    LBRACE, RBRACE,
    LPAREN, RPAREN,
    LBRACKET, RBRACKET,

    // Literals
    IDENTIFIER, NUMBER, STRING, BOOLEAN,

    // Keywords
    PRINT, IF, ELSE, FOR, TO, INPUT, WHILE,
    FUNCTION, RETURN,

    // End of file
    EOF,
    ASSIGN
}
