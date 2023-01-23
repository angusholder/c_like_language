package org.example.parse;

import org.example.parse.Expr.BinaryOp;
import org.example.parse.Expr.UnaryOp;
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

    public ParsedFile parseFile() {
        var items = new ArrayList<Expr.Item>();
        while (tokenizer.hasNext()) {
            items.add(parseTopLevelItem());
        }
        return new ParsedFile(tokenizer.getFile(), items);
    }

    public Expr.Block parseBlock() {
        var items = new ArrayList<Expr>();
        var lbraceToken = tokenizer.expect(TokenType.LBRACE);
        while (tokenizer.hasNext()) {
            if (tokenizer.peek() == TokenType.RBRACE) {
                break;
            }
            items.add(parseExpr());
            tokenizer.expect(TokenType.SEMICOLON);
        }
        var rbraceToken = tokenizer.expect(TokenType.RBRACE);
        return new Expr.Block(items, lbraceToken, rbraceToken);
    }

    private Expr.Item parseTopLevelItem() {
        switch (tokenizer.peek()) {
            case K_FUNC -> {
                return parseFunction();
            }
            case K_LET -> {
                Expr.Let expr = parseLet();
                tokenizer.expect(TokenType.SEMICOLON);
                return expr;
            }
            default -> {
                throw tokenizer.reportWrongTokenType(TokenType.K_FUNC, TokenType.K_LET);
            }
        }
    }

    private Expr.While parseWhile() {
        var whileToken = tokenizer.expect(TokenType.K_WHILE);
        Expr condition = parseParenExpr();
        Expr.Block body = parseBlock();
        return new Expr.While(condition, body, whileToken);
    }

    private Expr.If parseIf() {
        var ifToken = tokenizer.expect(TokenType.K_IF);
        Expr condition = parseParenExpr();
        Expr.Block thenBranch = parseBlock();
        List<Expr.ElseIf> elseIfs = new ArrayList<>();
        Expr.Block elseBranch = null;
        while (tokenizer.matchConsume(TokenType.K_ELSE)) {
            if (tokenizer.matchConsume(TokenType.K_IF)) {
                Expr elseifCond = parseParenExpr();
                Expr.Block elseifBody = parseBlock();
                elseIfs.add(new Expr.ElseIf(elseifCond, elseifBody));
            } else if (tokenizer.peek() == TokenType.LBRACE) {
                elseBranch = parseBlock();
                break;
            } else {
                throw tokenizer.reportWrongTokenType(TokenType.K_IF, TokenType.LBRACE);
            }
        }
        return new Expr.If(condition, thenBranch, elseIfs, elseBranch, ifToken);
    }

    private Expr.Function parseFunction() {
        var funcToken = tokenizer.expect(TokenType.K_FUNC);
        Token name = tokenizer.expect(TokenType.IDENTIFIER);
        tokenizer.expect(TokenType.LPAREN);
        List<Expr.FuncParam> params = new ArrayList<>();
        while (true) {
            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            Token paramName = tokenizer.expect(TokenType.IDENTIFIER);
            tokenizer.expect(TokenType.COLON);
            TypeExpr paramType = parseType();
            params.add(new Expr.FuncParam(tokenizer.getSourceOf(paramName), paramType));

            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            tokenizer.expect(TokenType.COMMA);
        }
        tokenizer.expect(TokenType.RPAREN);
        TypeExpr returnType = null;
        if (tokenizer.matchConsume(TokenType.ARROW)) {
            returnType = parseType();
        }
        Expr.Block body = parseBlock();
        return new Expr.Function(name, tokenizer.getSourceOf(name), returnType, params, body, funcToken);
    }

    private Expr.Let parseLet() {
        var letToken = tokenizer.expect(TokenType.K_LET);
        Token name = tokenizer.expect(TokenType.IDENTIFIER);
        tokenizer.expect(TokenType.COLON);
        TypeExpr type = parseType();
        tokenizer.expect(TokenType.ASSIGN);
        Expr value = parseExpr();
        return new Expr.Let(name, tokenizer.getSourceOf(name), type, value, letToken);
    }

    private TypeExpr parseType() {
        return new TypeExpr.Identifier(tokenizer.getSourceOf(tokenizer.expect(TokenType.IDENTIFIER)));
    }

    private Expr parseParenExpr() {
        tokenizer.expect(TokenType.LPAREN);
        Expr expr = parseExpr();
        tokenizer.expect(TokenType.RPAREN);
        return expr;
    }

    private Expr parseExpr() {
        return exprOr();
    }

    private Expr exprAtom() {
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
                return new Expr.Number(tokenizer.getSourceOf(token), token);
            }
            case IDENTIFIER -> {
                Token ident = tokenizer.next();
                if (tokenizer.peek() == TokenType.LPAREN) {
                    return parseCall(ident);
                }
                if (tokenizer.peek() == TokenType.ASSIGN) {
                    return parseAssign(ident);
                }
                return new Expr.Identifier(tokenizer.getSourceOf(ident), ident);
            }
            case K_TRUE -> {
                var token = tokenizer.next();
                return new Expr.Boolean(true, token);
            }
            case K_FALSE -> {
                var token = tokenizer.next();
                return new Expr.Boolean(false, token);
            }
            case K_RETURN -> {
                var returnToken = tokenizer.next();
                Expr retValue = null;
                if (tokenizer.peek() != TokenType.SEMICOLON) {
                    retValue = parseExpr();
                }
                return new Expr.Return(retValue, returnToken);
            }
            default -> {
                throw tokenizer.reportWrongTokenType(TokenType.LBRACE, TokenType.K_LET, TokenType.K_FUNC, TokenType.K_WHILE, TokenType.K_IF, TokenType.LPAREN, TokenType.NUMBER, TokenType.IDENTIFIER, TokenType.K_FALSE, TokenType.K_TRUE, TokenType.K_RETURN);
            }
        }
    }

    private Expr parseAssign(Token lhs) {
        tokenizer.expect(TokenType.ASSIGN);
        Expr value = parseExpr();
        return new Expr.Assign(tokenizer.getSourceOf(lhs), value, lhs);
    }

    private Expr parseCall(Token name) {
        tokenizer.expect(TokenType.LPAREN);
        List<Expr> args = new ArrayList<>();
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
        return new Expr.Call(tokenizer.getSourceOf(name), args, name, closeParenToken);
    }

    private Expr exprUnary() {
        UnaryOp op;
        switch (tokenizer.peek()) {
            case MINUS -> op = UnaryOp.NEG;
            case NOT -> op = UnaryOp.NOT;
            default -> {
                return exprAtom();
            }
        }
        var opToken = tokenizer.next();

        Expr expr = exprUnary();
        return new Expr.Unary(op, expr, opToken);
    }

    private Expr exprMul() {
        Expr expr = exprUnary();
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

            Expr right = exprUnary();
            expr = new Expr.Binary(expr, op, right);
        }
    }

    private Expr exprAdd() {
        Expr expr = exprMul();
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

            Expr right = exprMul();
            expr = new Expr.Binary(expr, op, right);
        }
    }

    private Expr exprComparison() {
        Expr expr = exprAdd();
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

            Expr right = exprAdd();
            expr = new Expr.Binary(expr, op, right);
        }
    }

    private Expr exprAnd() {
        Expr expr = exprComparison();
        while (tokenizer.matchConsume(TokenType.AND)) {
            Expr right = exprComparison();
            expr = new Expr.Binary(expr, BinaryOp.AND, right);
        }
        return expr;
    }

    private Expr exprOr() {
        Expr expr = exprAnd();
        while (tokenizer.matchConsume(TokenType.OR)) {
            Expr right = exprAnd();
            expr = new Expr.Binary(expr, BinaryOp.OR, right);
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
