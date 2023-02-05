package org.example.parse;

import org.example.CompilerCtx;
import org.example.parse.Expr.BinaryOp;
import org.example.parse.Expr.UnaryOp;
import org.example.token.SourceSpan;
import org.example.token.Token;
import org.example.token.TokenType;
import org.example.token.Tokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.stream.Collectors;

public class Parser {
    private final Tokenizer tokenizer;
    private final IdentityHashMap<Expr, Token> exprStarts;
    private final IdentityHashMap<Expr, Token> exprEnds;
    private final CompilerCtx ctx;

    public Parser(Tokenizer tokenizer, CompilerCtx ctx) {
        this.tokenizer = tokenizer;
        this.exprStarts = ctx.exprStarts;
        this.exprEnds = ctx.exprEnds;
        this.ctx = ctx;
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

    /** Records source range info for the expression then returns it. */
    private <E extends Expr> E hook(E expr, Token start, Token end) {
        exprStarts.put(expr, start);
        exprEnds.put(expr, end);
        return expr;
    }

    public Expr.Block parseBlock() {
        var items = new ArrayList<Expr>();
        var lbraceToken = expect(TokenType.LBRACE);
        while (tokenizer.hasNext()) {
            if (tokenizer.peek() == TokenType.RBRACE) {
                break;
            }
            items.add(parseExpr());
            expect(TokenType.SEMICOLON);
        }
        var rbraceToken = expect(TokenType.RBRACE);
        return hook(new Expr.Block(items), lbraceToken, rbraceToken);
    }

    private Expr.Item parseTopLevelItem() {
        switch (tokenizer.peek()) {
            case K_FUNC -> {
                return parseFunction();
            }
            case K_LET -> {
                Expr.Let expr = parseLet();
                expect(TokenType.SEMICOLON);
                return expr;
            }
            default -> {
                throw reportWrongTokenType(TokenType.K_FUNC, TokenType.K_LET);
            }
        }
    }

    public Expr parseExpr() {
        return parseExpr(0);
    }

    // A 'Pratt' expression parser, written using https://matklad.github.io/2020/04/13/simple-but-powerful-pratt-parsing.html
    private Expr parseExpr(int minBP) {
        UnaryOp unaryOp = getUnaryOpForToken(tokenizer.peek());
        Expr lhs;
        if (unaryOp == null) {
            lhs = exprAtom();
        } else {
            int rBP = prefixRightBindingPower(unaryOp);
            Token opToken = tokenizer.next();
            Expr rhs = parseExpr(rBP);
            lhs = hook(new Expr.Unary(unaryOp, rhs), opToken, exprEnds.get(rhs));
        }

        while (true) {
            BinaryOp binaryOp = getBinaryOpForToken(tokenizer.peek());
            if (binaryOp != null) {
                int lBP = infixLeftBindingPower(binaryOp);
                int rBP = infixRightBindingPower(binaryOp);

                if (lBP < minBP) {
                    break;
                }

                tokenizer.next();
                Expr rhs = parseExpr(rBP);

                lhs = hook(new Expr.Binary(lhs, binaryOp, rhs), exprStarts.get(lhs), exprEnds.get(rhs));
                continue;
            }

            break;
        }

        return lhs;
    }

    @Nullable
    private BinaryOp getBinaryOpForToken(TokenType type) {
        return switch (type) {
            case PLUS -> BinaryOp.ADD;
            case MINUS -> BinaryOp.SUB;
            case STAR -> BinaryOp.MUL;
            case DIVIDE -> BinaryOp.DIV;
            case EQUALS -> BinaryOp.EQUALS;
            case NOT_EQUALS -> BinaryOp.NOT_EQUALS;
            case LT -> BinaryOp.LT;
            case LT_EQ -> BinaryOp.LT_EQ;
            case GT -> BinaryOp.GT;
            case GT_EQ -> BinaryOp.GT_EQ;
            case AND -> BinaryOp.AND;
            case OR -> BinaryOp.OR;
            default -> null;
        };
    }

    @Nullable
    private UnaryOp getUnaryOpForToken(TokenType type) {
        return switch (type) {
            case MINUS -> UnaryOp.NEG;
            case NOT -> UnaryOp.NOT;
            default -> null;
        };
    }

    private int infixLeftBindingPower(@NotNull BinaryOp op) {
        return switch (op) {
            case OR -> 10;
            case AND -> 20;
            case EQUALS, NOT_EQUALS, LT, LT_EQ, GT, GT_EQ -> 30;
            case ADD, SUB -> 40;
            case MUL, DIV -> 50;
        };
    }

    private int infixRightBindingPower(@NotNull BinaryOp op) {
        return switch (op) {
            case OR -> 11;
            case AND -> 21;
            case EQUALS, NOT_EQUALS, LT, LT_EQ, GT, GT_EQ -> 31;
            case ADD, SUB -> 41;
            case MUL, DIV -> 51;
        };
    }

    private int prefixRightBindingPower(@NotNull UnaryOp op) {
        // Only field access, array indexing, and function calls will have higher binding power than us.
        return switch (op) {
            case NEG, NOT -> 60;
        };
    }

    private Expr.While parseWhile() {
        var whileToken = expect(TokenType.K_WHILE);
        Expr condition = parseParenExpr();
        Expr.Block body = parseBlock();
        return hook(new Expr.While(condition, body), whileToken, exprEnds.get(body));
    }

    private Expr.If parseIf() {
        var startToken = expect(TokenType.K_IF);
        Expr condition = parseParenExpr();
        Expr.Block thenBranch = parseBlock();
        Token endToken = exprEnds.get(thenBranch);
        List<Expr.ElseIf> elseIfs = new ArrayList<>();
        Expr.Block elseBranch = null;
        while (tokenizer.matchConsume(TokenType.K_ELSE)) {
            if (tokenizer.matchConsume(TokenType.K_IF)) {
                Expr elseifCond = parseParenExpr();
                Expr.Block elseifBody = parseBlock();
                elseIfs.add(new Expr.ElseIf(elseifCond, elseifBody));
                endToken = exprEnds.get(elseifBody);
            } else if (tokenizer.peek() == TokenType.LBRACE) {
                elseBranch = parseBlock();
                endToken = exprEnds.get(elseBranch);
                break;
            } else {
                throw reportWrongTokenType(TokenType.K_IF, TokenType.LBRACE);
            }
        }
        return hook(new Expr.If(condition, thenBranch, elseIfs, elseBranch), startToken, endToken);
    }

    private Expr.Function parseFunction() {
        var funcToken = expect(TokenType.K_FUNC);
        Expr.Identifier name = createIdentifierExpr(expect(TokenType.IDENTIFIER));
        expect(TokenType.LPAREN);
        List<Expr.FuncParam> params = new ArrayList<>();
        while (true) {
            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            Expr.Identifier paramName = createIdentifierExpr(expect(TokenType.IDENTIFIER));
            expect(TokenType.COLON);
            TypeExpr paramType = parseType();
            params.add(new Expr.FuncParam(paramName, paramType));

            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            expect(TokenType.COMMA);
        }
        expect(TokenType.RPAREN);
        TypeExpr returnType = null;
        if (tokenizer.matchConsume(TokenType.ARROW)) {
            returnType = parseType();
        }
        Expr.Block body = parseBlock();
        return hook(new Expr.Function(name, returnType, params, body), funcToken, exprEnds.get(body));
    }

    private Expr.Let parseLet() {
        var letToken = expect(TokenType.K_LET);
        Expr.Identifier name = createIdentifierExpr(expect(TokenType.IDENTIFIER));
        expect(TokenType.COLON);
        TypeExpr type = parseType();
        expect(TokenType.ASSIGN);
        Expr value = parseExpr();
        return hook(new Expr.Let(name, type, value), letToken, exprEnds.get(value));
    }

    private TypeExpr parseType() {
        return new TypeExpr.Identifier(tokenizer.getSourceOf(expect(TokenType.IDENTIFIER)));
    }

    private Expr parseParenExpr() {
        expect(TokenType.LPAREN);
        Expr expr = parseExpr();
        expect(TokenType.RPAREN);
        return expr;
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
                return hook(new Expr.Number(tokenizer.getSourceOf(token)), token, token);
            }
            case IDENTIFIER -> {
                Token ident = tokenizer.next();
                Expr.Identifier identifier = createIdentifierExpr(ident);
                if (tokenizer.peek() == TokenType.LPAREN) {
                    return parseCall(identifier);
                }
                if (tokenizer.peek() == TokenType.ASSIGN) {
                    return parseAssign(identifier);
                }
                return identifier;
            }
            case K_TRUE -> {
                var token = tokenizer.next();
                return hook(new Expr.Boolean(true), token, token);
            }
            case K_FALSE -> {
                var token = tokenizer.next();
                return hook(new Expr.Boolean(false), token, token);
            }
            case K_RETURN -> {
                var returnToken = tokenizer.next();
                Expr retValue = null;
                if (tokenizer.peek() != TokenType.SEMICOLON) {
                    retValue = parseExpr();
                }
                return hook(new Expr.Return(retValue), returnToken, retValue != null ? exprEnds.get(retValue) : returnToken);
            }
            default -> {
                throw reportWrongTokenType(TokenType.LBRACE, TokenType.K_LET, TokenType.K_FUNC, TokenType.K_WHILE, TokenType.K_IF, TokenType.LPAREN, TokenType.NUMBER, TokenType.IDENTIFIER, TokenType.K_FALSE, TokenType.K_TRUE, TokenType.K_RETURN);
            }
        }
    }

    @NotNull
    private Expr.Identifier createIdentifierExpr(Token ident) {
        return hook(new Expr.Identifier(tokenizer.getSourceOf(ident)), ident, ident);
    }

    private Expr parseAssign(Expr.Identifier lhs) {
        expect(TokenType.ASSIGN);
        Expr value = parseExpr();
        return hook(new Expr.Assign(lhs, value), exprStarts.get(lhs), exprEnds.get(value));
    }

    private Expr parseCall(Expr.Identifier name) {
        expect(TokenType.LPAREN);
        List<Expr> args = new ArrayList<>();
        while (true) {
            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            args.add(parseExpr());
            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
            expect(TokenType.COMMA);
            if (tokenizer.peek() == TokenType.RPAREN) {
                break;
            }
        }
        var closeParenToken = expect(TokenType.RPAREN);
        return hook(new Expr.Call(name, args), exprStarts.get(name), closeParenToken);
    }

    private Token expect(TokenType type) {
        Token token = tokenizer.next();
        if (token.type() != type) {
            throw reportWrongTokenType(token, type);
        }
        return token;
    }

    @NotNull
    private CompilerCtx.ParseError reportWrongTokenType(Token token, TokenType... expectedTypes) {
        SourceSpan span = ctx.getSourceSpan(token);
        if (expectedTypes.length == 1) {
            return ctx.reportParseError(span, " Got " + token.type() + ", expected " + expectedTypes[0].repr);
        } else {
            String expected = Arrays.stream(expectedTypes).map(t -> t.repr).collect(Collectors.joining(", "));
            return ctx.reportParseError(span, " Got " + token.type() + ", expected one of [" + expected + "]");
        }
    }

    @NotNull
    private CompilerCtx.ParseError reportWrongTokenType(TokenType... expectedTypes) {
        return reportWrongTokenType(tokenizer.peekToken(), expectedTypes);
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
