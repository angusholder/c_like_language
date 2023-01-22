package org.example.token;

import org.example.CompilerCtx;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Tokenizer {
    private final String source;
    private final CompilerCtx.FileInfo file;
    private final CompilerCtx ctx;
    private int position = 0;
    private int token_start = 0;

    @Nullable
    private Token peeked = null;

    public Tokenizer(CompilerCtx ctx, CompilerCtx.FileInfo file) {
        this.source = file.contents();
        this.file = file;
        this.ctx = ctx;
    }

    public CompilerCtx.FileInfo getFile() {
        return file;
    }

    @NotNull
    public TokenType peek() {
        return peekToken().type();
    }

    @NotNull
    public Token peekToken() {
        if (peeked != null) {
            return peeked;
        }

        peeked = tokenizeNext();
        return peeked;
    }

    public boolean hasNext() {
        return peek() != TokenType.EOF;
    }

    @NotNull
    public Token next() {
        Token result = peekToken();
//        new RuntimeException("Consumed " + result.type()).printStackTrace();
        peeked = null;
        return result;
    }

    public Token expect(TokenType type) {
        Token token = next();
        if (token.type() != type) {
            throw reportWrongTokenType(token, type);
        }
        return token;
    }

    @NotNull
    public WrongTokenTypeException reportWrongTokenType(Token token, TokenType... expectedTypes) {
        SourceSpan span = ctx.getSourceSpan(token);
        if (expectedTypes.length == 1) {
            return new WrongTokenTypeException(token, span.formattedLocation() + " Got " + token.type() + ", expected " + expectedTypes[0].repr);
        } else {
            String expected = Arrays.stream(expectedTypes).map(t -> t.repr).collect(Collectors.joining(", "));
            return new WrongTokenTypeException(token, span.formattedLocation() + " Got " + token.type() + ", expected one of [" + expected + "]");
        }
    }

    @NotNull
    public WrongTokenTypeException reportWrongTokenType(TokenType... expectedTypes) {
        return reportWrongTokenType(peekToken(), expectedTypes);
    }

    public boolean matchConsume(TokenType type) {
        if (peek() == type) {
            next();
            return true;
        } else {
            return false;
        }
    }

    public static class WrongTokenTypeException extends RuntimeException {
        public final Token token;

        public WrongTokenTypeException(Token token, String message) {
            super(message);
            this.token = token;
        }
    }

    @NotNull
    private Token tokenizeNext() {
        skipWhitespace();
        if (!hasMoreChars()) {
            return Token.EOF;
        }

        char cur = nextChar();
        switch (cur) {
            case '+' -> {
                return makeToken(TokenType.PLUS);
            }
            case '-' -> {
                if (matchesChar('>')) {
                    return makeToken(TokenType.ARROW);
                } else {
                    return makeToken(TokenType.MINUS);
                }
            }
            case '*' -> {
                return makeToken(TokenType.STAR);
            }
            case '/' -> {
                return makeToken(TokenType.DIVIDE);
            }
            case ';' -> {
                return makeToken(TokenType.SEMICOLON);
            }
            case ':' -> {
                return makeToken(TokenType.COLON);
            }
            case ',' -> {
                return makeToken(TokenType.COMMA);
            }
            case '(' -> {
                return makeToken(TokenType.LPAREN);
            }
            case ')' -> {
                return makeToken(TokenType.RPAREN);
            }
            case '{' -> {
                return makeToken(TokenType.LBRACE);
            }
            case '}' -> {
                return makeToken(TokenType.RBRACE);
            }
            case '&' -> {
                if (matchesChar('&')) {
                    return makeToken(TokenType.AND);
                } else {
                    throw reportUnexpected();
                }
            }
            case '|' -> {
                if (matchesChar('|')) {
                    return makeToken(TokenType.OR);
                } else {
                    throw reportUnexpected();
                }
            }
            case '=' -> {
                if (matchesChar('=')) {
                    return makeToken(TokenType.EQUAL);
                } else {
                    return makeToken(TokenType.ASSIGN);
                }
            }
            case '!' -> {
                if (matchesChar('=')) {
                    return makeToken(TokenType.NOT_EQUAL);
                } else {
                    return makeToken(TokenType.NOT);
                }
            }
            case '<' -> {
                if (matchesChar('=')) {
                    return makeToken(TokenType.LT_EQ);
                } else {
                    return makeToken(TokenType.LT);
                }
            }
            case '>' -> {
                if (matchesChar('=')) {
                    return makeToken(TokenType.GT_EQ);
                } else {
                    return makeToken(TokenType.GT);
                }
            }
            default -> {
                if (Character.isJavaIdentifierStart(cur)) {
                    while (Character.isJavaIdentifierPart(peekChar())) {
                        nextChar();
                    }
                    return makeToken(getKeywordType(getCurrentSpan()));
                }
                if (Character.isDigit(cur)) {
                    while (Character.isDigit(peekChar())) {
                        nextChar();
                    }
                    return makeToken(TokenType.NUMBER);
                }

                throw reportUnexpected();
            }
        }
    }

    private RuntimeException reportUnexpected() {
        SourceSpan span = getCurrentSourceSpan();
        return new RuntimeException("Unexpected text at line " + span.formattedLocation() + ": '" + span.text() + "'");
    }

    private SourceLoc getSourceLocation(int offset) {
        int line = 1;
        int column = 1;
        for (int i = 0; i < offset; i++) {
            if (source.charAt(i) == '\n') {
                line += 1;
                column = 1;
            } else {
                column += 1;
            }
        }
        return new SourceLoc(offset, line, column);
    }

    private SourceSpan getCurrentSourceSpan() {
        return getSourceSpan(token_start, position);
    }

    @NotNull
    private SourceSpan getSourceSpan(int start, int end) {
        return new SourceSpan(
                getSourceLocation(start),
                getSourceLocation(end - 1),
                source.substring(start, end)
        );
    }

    private boolean matchesChar(char c) {
        if (!hasMoreChars()) {
            return false;
        }
        if (peekChar() != c) {
            return false;
        }
        position++;
        return true;
    }

    private boolean hasMoreChars() {
        return position < source.length();
    }

    private char peekChar() {
        return source.charAt(position);
    }

    private char nextChar() {
        return source.charAt(position++);
    }

    private TokenType getKeywordType(String span) {
        return switch (span) {
            case "func" -> TokenType.K_FUNC;
            case "while" -> TokenType.K_WHILE;
            case "if" -> TokenType.K_IF;
            case "else" -> TokenType.K_ELSE;
            case "let" -> TokenType.K_LET;
            case "true" -> TokenType.K_TRUE;
            case "false" -> TokenType.K_FALSE;
            default -> TokenType.IDENTIFIER;
        };
    }

    private Token makeToken(TokenType type) {
        var token = new Token(type, file.uid(), token_start, position);
        token_start = position;
        return token;
    }

    private void skipWhitespace() {
        while (hasMoreChars() && Character.isWhitespace(peekChar())) {
            nextChar();
        }
        token_start = position;
    }

    private String getCurrentSpan() {
        return source.substring(token_start, position);
    }

    public String getSourceOf(Token token) {
        return source.substring(token.startOffset(), token.endOffset());
    }

    @NotNull
    public ArrayList<Token> tokenizeAll() {
        var tokens = new ArrayList<Token>();
        while (this.peek() != TokenType.EOF) {
            tokens.add(this.next());
        }
        return tokens;
    }
}
