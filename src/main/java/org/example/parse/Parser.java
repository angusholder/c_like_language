package org.example.parse;

import org.example.parse.AstExpr.BinaryOp;
import org.example.parse.AstExpr.UnaryOp;
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
        var items = new ArrayList<AstExpr.Item>();
        while (tokenizer.hasNext()) {
            items.add(parseTopLevelItem());
        }
        return new AstFile(tokenizer.getFile(), items);
    }

    public AstExpr.Block parseBlock() {
        var items = new ArrayList<AstExpr>();
        var lbraceToken = tokenizer.expect(TokenType.LBRACE);
        while (tokenizer.hasNext()) {
            if (tokenizer.peek() == TokenType.RBRACE) {
                break;
            }
            items.add(parseExpr());
            tokenizer.expect(TokenType.SEMICOLON);
        }
        var rbraceToken = tokenizer.expect(TokenType.RBRACE);
        return new AstExpr.Block(items, lbraceToken, rbraceToken);
    }

    private AstExpr.Item parseTopLevelItem() {
        switch (tokenizer.peek()) {
            case K_FUNC -> {
                return parseFunction();
            }
            case K_LET -> {
                AstExpr.Let expr = parseLet();
                tokenizer.expect(TokenType.SEMICOLON);
                return expr;
            }
            default -> {
                throw tokenizer.reportWrongTokenType(TokenType.K_FUNC, TokenType.K_LET);
            }
        }
    }

    private AstExpr.While parseWhile() {
        var whileToken = tokenizer.expect(TokenType.K_WHILE);
        AstExpr condition = parseParenExpr();
        AstExpr.Block body = parseBlock();
        return new AstExpr.While(condition, body, whileToken);
    }

    private AstExpr.If parseIf() {
        var ifToken = tokenizer.expect(TokenType.K_IF);
        AstExpr condition = parseParenExpr();
        AstExpr.Block thenBranch = parseBlock();
        List<AstExpr.ElseIf> elseIfs = new ArrayList<>();
        AstExpr.Block elseBranch = null;
        while (tokenizer.matchConsume(TokenType.K_ELSE)) {
            if (tokenizer.matchConsume(TokenType.K_IF)) {
                AstExpr elseifCond = parseParenExpr();
                AstExpr.Block elseifBody = parseBlock();
                elseIfs.add(new AstExpr.ElseIf(elseifCond, elseifBody));
            } else if (tokenizer.peek() == TokenType.LBRACE) {
                elseBranch = parseBlock();
                break;
            } else {
                throw tokenizer.reportWrongTokenType(TokenType.K_IF, TokenType.LBRACE);
            }
        }
        return new AstExpr.If(condition, thenBranch, elseIfs, elseBranch, ifToken);
    }

    private AstExpr.Function parseFunction() {
        var funcToken = tokenizer.expect(TokenType.K_FUNC);
        Token name = tokenizer.expect(TokenType.IDENTIFIER);
        tokenizer.expect(TokenType.LPAREN);
        List<AstExpr.FuncParam> params = new ArrayList<>();
        while (true) {
            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            Token paramName = tokenizer.expect(TokenType.IDENTIFIER);
            tokenizer.expect(TokenType.COLON);
            AstType paramType = parseType();
            params.add(new AstExpr.FuncParam(tokenizer.getSourceOf(paramName), paramType));

            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            tokenizer.expect(TokenType.COMMA);
        }
        tokenizer.expect(TokenType.RPAREN);
        AstType returnType = null;
        if (tokenizer.matchConsume(TokenType.ARROW)) {
            returnType = parseType();
        }
        AstExpr.Block body = parseBlock();
        return new AstExpr.Function(name, tokenizer.getSourceOf(name), returnType, params, body, funcToken);
    }

    private AstExpr.Let parseLet() {
        var letToken = tokenizer.expect(TokenType.K_LET);
        Token name = tokenizer.expect(TokenType.IDENTIFIER);
        tokenizer.expect(TokenType.COLON);
        AstType type = parseType();
        tokenizer.expect(TokenType.ASSIGN);
        AstExpr value = parseExpr();
        return new AstExpr.Let(name, tokenizer.getSourceOf(name), type, value, letToken);
    }

    private AstType parseType() {
        return new AstType.Identifier(tokenizer.getSourceOf(tokenizer.expect(TokenType.IDENTIFIER)));
    }

    private AstExpr parseParenExpr() {
        tokenizer.expect(TokenType.LPAREN);
        AstExpr expr = parseExpr();
        tokenizer.expect(TokenType.RPAREN);
        return expr;
    }

    private AstExpr parseExpr() {
        return exprOr();
    }

    private AstExpr exprAtom() {
        switch (tokenizer.peek()) {
            case LBRACE -> {
                return parseBlock();
            }
            case K_LET -> {
                return parseLet();
            }
            case K_FUNC -> {
                return parseFunction();
            }
            case K_WHILE -> {
                return parseWhile();
            }
            case K_IF -> {
                return parseIf();
            }
            case LPAREN -> {
                return parseParenExpr();
            }
            case NUMBER -> {
                var token = tokenizer.next();
                return new AstExpr.Number(tokenizer.getSourceOf(token), token);
            }
            case IDENTIFIER -> {
                Token ident = tokenizer.next();
                if (tokenizer.peek() == TokenType.LPAREN) {
                    return parseCall(ident);
                }
                if (tokenizer.peek() == TokenType.ASSIGN) {
                    return parseAssign(ident);
                }
                return new AstExpr.Identifier(tokenizer.getSourceOf(ident), ident);
            }
            case K_TRUE -> {
                var token = tokenizer.next();
                return new AstExpr.Boolean(true, token);
            }
            case K_FALSE -> {
                var token = tokenizer.next();
                return new AstExpr.Boolean(false, token);
            }
            case K_RETURN -> {
                var returnToken = tokenizer.next();
                AstExpr retValue = null;
                if (tokenizer.peek() != TokenType.SEMICOLON) {
                    retValue = parseExpr();
                }
                return new AstExpr.Return(retValue, returnToken);
            }
            default -> {
                throw tokenizer.reportWrongTokenType(TokenType.LBRACE, TokenType.K_LET, TokenType.K_FUNC, TokenType.K_WHILE, TokenType.K_IF, TokenType.LPAREN, TokenType.NUMBER, TokenType.IDENTIFIER, TokenType.K_FALSE, TokenType.K_TRUE, TokenType.K_RETURN);
            }
        }
    }

    private AstExpr parseAssign(Token lhs) {
        tokenizer.expect(TokenType.ASSIGN);
        AstExpr value = parseExpr();
        return new AstExpr.Assign(tokenizer.getSourceOf(lhs), value, lhs);
    }

    private AstExpr parseCall(Token name) {
        tokenizer.expect(TokenType.LPAREN);
        List<AstExpr> args = new ArrayList<>();
        while (true) {
            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            args.add(parseExpr());
            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            tokenizer.expect(TokenType.COMMA);
            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
        }
        var closeParenToken = tokenizer.expect(TokenType.RPAREN);
        return new AstExpr.Call(tokenizer.getSourceOf(name), args, name, closeParenToken);
    }

    private AstExpr exprUnary() {
        UnaryOp op;
        switch (tokenizer.peek()) {
            case MINUS -> op = UnaryOp.NEG;
            case NOT -> op = UnaryOp.NOT;
            default -> {
                return exprAtom();
            }
        }
        var opToken = tokenizer.next();

        AstExpr expr = exprUnary();
        return new AstExpr.Unary(op, expr, opToken);
    }

    private AstExpr exprMul() {
        AstExpr expr = exprUnary();
        while (true) {
            BinaryOp op;
            switch (tokenizer.peek()) {
                case STAR -> op = BinaryOp.MUL;
                case DIVIDE -> op = BinaryOp.DIV;
                default -> {
                    return expr;
                }
            };
            tokenizer.next();

            AstExpr right = exprUnary();
            expr = new AstExpr.Binary(expr, op, right);
        }
    }

    private AstExpr exprAdd() {
        AstExpr expr = exprMul();
        while (true) {
            BinaryOp op;
            switch (tokenizer.peek()) {
                case PLUS -> op = BinaryOp.ADD;
                case MINUS -> op = BinaryOp.SUB;
                default -> {
                    return expr;
                }
            };
            tokenizer.next();

            AstExpr right = exprMul();
            expr = new AstExpr.Binary(expr, op, right);
        }
    }

    private AstExpr exprComparison() {
        AstExpr expr = exprAdd();
        while (true) {
            BinaryOp op;
            switch (tokenizer.peek()) {
                case EQUALS -> op = BinaryOp.EQUALS;
                case NOT_EQUALS -> op = BinaryOp.NOT_EQUALS;
                case LT_EQ -> op = BinaryOp.LT_EQ;
                case LT -> op = BinaryOp.LT;
                case GT_EQ -> op = BinaryOp.GT_EQ;
                case GT -> op = BinaryOp.GT;
                default -> {
                    return expr;
                }
            };
            tokenizer.next();

            AstExpr right = exprAdd();
            expr = new AstExpr.Binary(expr, op, right);
        }
    }

    private AstExpr exprAnd() {
        AstExpr expr = exprComparison();
        while (tokenizer.matchConsume(TokenType.AND)) {
            AstExpr right = exprComparison();
            expr = new AstExpr.Binary(expr, BinaryOp.AND, right);
        }
        return expr;
    }

    private AstExpr exprOr() {
        AstExpr expr = exprAnd();
        while (tokenizer.matchConsume(TokenType.OR)) {
            AstExpr right = exprAnd();
            expr = new AstExpr.Binary(expr, BinaryOp.OR, right);
        }
        return expr;
    }

    /*
    Expression grammar:

    atom :: STRING | NUMBER | call | IDENTIFIER | '(' expr ')' | if_stmt | while_stmt | block | let_stmt | func_stmt | assign ;
    unary :: ('-' | '!') unary | atom ;
    mul :: unary (('*' | '/') unary)* ;
    add :: mul (('+' | '-') mul)* ;
    comparison :: add (('==' | '!=' | '<' | '<=' | '>' | '>=') add)* ;
    and :: comparison ('&&' comparison)* ;
    or :: and ('||' and)* ;

    expr :: or ;

    assign :: IDENTIFIER '=' expr ;
     */
}
