package org.example.token;

public enum TokenType {
    EOF("<EOF>"),

    IDENTIFIER("a-zA-Z"),
    NUMBER("0-9"),

    PLUS("+"),
    MINUS("-"),
    STAR("*"),
    DIVIDE("/"),

    AND("&&"),
    OR("||"),

    ASSIGN("="),
    EQUAL("=="),

    NOT("!"),
    NOT_EQUAL("!="),
    LT_EQ("<="),
    LT("<"),
    GT_EQ(">="),
    GT(">"),

    COLON(":"),
    SEMICOLON(";"),
    COMMA(","),

    LPAREN("("),
    RPAREN(")"),
    LBRACE("{"),
    RBRACE("}"),

    K_FUNC("func"),
    K_WHILE("while"),
    K_IF("if"),
    K_ELSE("else"),
    K_LET("let"),
    ;

    public final String repr;

    TokenType(String repr) {

        this.repr = repr;
    }

    public boolean shouldDisplaySource() {
        return switch (this) {
            case IDENTIFIER, NUMBER -> true;
            default -> false;
        };
    }
}
