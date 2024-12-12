enum TokenType {
    IDENTIFIER, NUMBER, STRING,
    PLUS, MINUS, STAR, SLASH, MOD,
    EQ, EQ_EQ, NOT_EQ, GT, LT, GT_EQ, LT_EQ,
    AND_AND, OR_OR, NOT,
    PLUS_EQ, MINUS_EQ, STAR_EQ, SLASH_EQ,
    PLUS_PLUS, MINUS_MINUS,
    ARROW,
    PRINT, IF, ELSE, LOOP, TO, INPUT, WHILE,
    LBRACE, RBRACE,
    LPAREN, RPAREN,
    EOF, ASSIGN,
    SEMICOLON
}

class Token {
    TokenType type;
    String lexeme;

    Token(TokenType type, String lexeme) {
        this.type = type;
        this.lexeme = lexeme;
    }

    public String toString() {
        return type + "('" + lexeme + "')";
    }
}
