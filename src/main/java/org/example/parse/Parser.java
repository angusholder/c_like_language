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
        switch (tokenizer.peek()) {
            case K_FUNC -> {
                return parseFunction();
            }
            case K_LET -> {
                return parseLet();
            }
            default -> {
                throw tokenizer.reportWrongTokenType(TokenType.K_FUNC, TokenType.K_LET);
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
        return exprOr();
    }

    // atom :: STRING | NUMBER | IDENTIFIER | LPAREN expr RPAREN | IF_STMT | WHILE_STMT | BLOCK | LET_STMT | FUNC_STMT ;
    private AstExpr exprAtom() {
        switch (tokenizer.peek()) {
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
            case LPAREN -> {
                return parseParenExpr();
            }
            case NUMBER -> {
                var token = tokenizer.next();
                return new AstExpr.Number(tokenizer.getSourceOf(token));
            }
            case IDENTIFIER -> {
                var token = tokenizer.next();
                return new AstExpr.Identifier(tokenizer.getSourceOf(token));
            }
            default -> {
                throw tokenizer.reportWrongTokenType(TokenType.LBRACE, TokenType.K_LET, TokenType.K_FUNC, TokenType.K_WHILE, TokenType.K_IF, TokenType.LPAREN, TokenType.NUMBER, TokenType.IDENTIFIER);
            }
        }
    }

    // unary :: (MINUS | NOT) unary | atom ;
    private AstExpr exprUnary() {
        UnaryOp op;
        switch (tokenizer.peek()) {
            case MINUS -> op = UnaryOp.NEG;
            case NOT -> op = UnaryOp.NOT;
            default -> {
                return exprAtom();
            }
        }
        tokenizer.next();

        AstExpr expr = exprUnary();
        return new AstExpr.Unary(op, expr);
    }

    // mul :: unary ((STAR | DIVIDE) unary)* ;
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

    // add :: mul ((PLUS | MINUS) mul)* ;
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

    // comparison :: add ((EQUAL | NOT_EQUAL) add)* ;
    private AstExpr exprComparison() {
        AstExpr expr = exprAdd();
        while (true) {
            BinaryOp op;
            switch (tokenizer.peek()) {
                case EQUAL -> op = BinaryOp.EQUALS;
                case NOT_EQUAL -> op = BinaryOp.NOT_EQUALS;
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

    // and :: comparison (AND comparison)* ;
    private AstExpr exprAnd() {
        AstExpr expr = exprComparison();
        while (tokenizer.matchConsume(TokenType.AND)) {
            AstExpr right = exprComparison();
            expr = new AstExpr.Binary(expr, BinaryOp.AND, right);
        }
        return expr;
    }

    // or :: and (OR and)* ;
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

    atom :: STRING | NUMBER | IDENTIFIER | LPAREN expr RPAREN | IF_STMT | WHILE_STMT | BLOCK | LET_STMT | FUNC_STMT ;
    unary :: (MINUS | NOT) unary | atom ;
    mul :: unary ((STAR | DIVIDE) unary)* ;
    add :: mul ((PLUS | MINUS) mul)* ;
    comparison :: add ((EQUAL | NOT_EQUAL) add)* ;
    and :: comparison (AND comparison)* ;
    or :: and (OR and)* ;

    expr :: or ;
     */
}
