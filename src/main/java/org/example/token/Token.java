package org.example.token;

import org.example.CompilerCtx;

public record Token(
        TokenType type,
        int fileUid,
        int startOffset,
        int endOffset
) {
    public Token {
        if (startOffset > endOffset) {
            throw new IllegalArgumentException("startOffset > endOffset");
        }
    }

    @Override
    public String toString() {
        return "Token[" + type + "]";
    }

    public String format(CompilerCtx ctx) {
        SourceSpan span = ctx.getSourceSpan(this);
        if (type.shouldDisplaySource()) {
            return "Token[" + span.text() + " " + type + " " + span.formattedLocation() + "]";
        } else {
            return "Token[" + type + " " + span.formattedLocation() + "]";
        }
    }

    public static Token EOF = new Token(TokenType.EOF, -1, -1, -1);
}
