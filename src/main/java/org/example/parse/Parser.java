package org.example.parse;

import org.example.token.Token;
import org.example.token.TokenType;
import org.example.token.Tokenizer;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private final Tokenizer tokenizer;

    public Parser(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public Tokenizer getTokenizer() {
        return tokenizer;
    }

    public AstFile parseFile() {
        var items = new ArrayList<AstItem>();
        while (tokenizer.hasNext()) {
            items.add(parseItem());
        }
        return new AstFile(items);
    }

    public AstExpr.Block parseBlock() {
        var items = new ArrayList<AstExpr>();
        tokenizer.expect(TokenType.LBRACE);
        while (tokenizer.hasNext()) {
            items.add(parseExpr());
        }
        tokenizer.expect(TokenType.RBRACE);
        return new AstExpr.Block(items);
    }

    public AstItem parseItem() {
        Token token = tokenizer.peek();
        switch (token.type()) {
            case K_FUNC -> {
                return parseFunction();
            }
            case K_LET -> {
                return parseLet();
            }
            case EOF -> {
                throw new IllegalArgumentException("Unexpected EOF");
            }
            default -> {
                throw new IllegalArgumentException("Unexpected token: " + token);
            }
        }
    }

    private AstExpr.While parseWhile() {
        tokenizer.expect(TokenType.K_WHILE);
        AstExpr condition = parseParenExpr();
        AstExpr.Block body = parseBlock();
        return new AstExpr.While(condition, body);
    }

    private AstExpr.If parseIf() {
        tokenizer.expect(TokenType.K_IF);
        AstExpr condition = parseParenExpr();
        AstExpr.Block thenBranch = parseBlock();
        List<AstExpr.ElseIf> elseIfs = new ArrayList<>();
        AstExpr elseBranch = null;
        while (tokenizer.matches(TokenType.K_ELSE)) {
            if (tokenizer.matches(TokenType.K_IF)) {
                AstExpr elseifCond = parseParenExpr();
                AstExpr.Block elseifBody = parseBlock();
                elseIfs.add(new AstExpr.ElseIf(elseifCond, elseifBody));
            } else if (tokenizer.peek().type() == TokenType.LBRACE) {
                elseBranch = parseBlock();
                break;
            } else {
                throw Tokenizer.reportWrongTokenType(List.of(TokenType.K_IF, TokenType.LBRACE), tokenizer.peek());
            }
        }
        return new AstExpr.If(condition, thenBranch, elseIfs, elseBranch);
    }

    private AstItem.Function parseFunction() {
        tokenizer.expect(TokenType.K_FUNC);
        Token name = tokenizer.expect(TokenType.IDENTIFIER);
        tokenizer.expect(TokenType.LPAREN);
        // TODO: Arguments
        tokenizer.expect(TokenType.RPAREN);
        AstExpr.Block body = parseBlock();
        return new AstItem.Function(name, tokenizer.getSourceOf(name), body);
    }

    private AstItem.Let parseLet() {
        tokenizer.expect(TokenType.K_LET);
        Token name = tokenizer.expect(TokenType.IDENTIFIER);
        tokenizer.expect(TokenType.COLON);
        Token type = tokenizer.expect(TokenType.IDENTIFIER);
        tokenizer.expect(TokenType.ASSIGN);
        AstExpr value = parseExpr();
        tokenizer.expect(TokenType.SEMICOLON);
        return new AstItem.Let(name, tokenizer.getSourceOf(name), type, value);
    }

    private AstExpr parseParenExpr() {
        tokenizer.expect(TokenType.LPAREN);
        AstExpr expr = parseExpr();
        tokenizer.expect(TokenType.RPAREN);
        return expr;
    }

    private AstExpr parseExpr() {
        switch (tokenizer.peek().type()) {
            case LBRACE -> {
                return parseBlock();
            }
            case K_LET, K_FUNC -> {
                return parseItem();
            }
            case K_WHILE -> {
                return parseWhile();
            }
            case K_IF -> {
                return parseIf();
            }
            default -> {
                return new ExpressionParser(this).parse();
            }
        }
    }
}
