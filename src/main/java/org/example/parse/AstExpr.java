package org.example.parse;

import org.example.token.Token;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface AstExpr permits AstExpr.Atom, AstExpr.Binary, AstExpr.Block, AstExpr.Call, AstExpr.If, AstExpr.Unary, AstExpr.While, AstItem {
    sealed interface Atom extends AstExpr { }

    record Number(
            String text
    ) implements Atom {}

    record Identifier(
            String text
    ) implements Atom {}

    record Binary(
            AstExpr left,
            BinaryOp op,
            AstExpr right
    ) implements AstExpr {}

    enum BinaryOp {
        ADD,
        SUB,
        MUL,
        DIV,

        AND,
        OR,

        EQUALS,
        NOT_EQUALS,

        LT_EQ,
        LT,
        GT_EQ,
        GT,
    }

    record Unary(
            UnaryOp op,
            AstExpr expr
    ) implements AstExpr {}

    enum UnaryOp {
        NOT,
        NEG,
    }

    record Call(
            String callee,
            List<AstExpr> arguments
    ) implements AstExpr {}

    record Block(
            List<AstExpr> items
    ) implements AstExpr {}

    record If(
            AstExpr condition,
            AstExpr thenBranch,
            List<ElseIf> elseIfs,
            @Nullable
            AstExpr elseBranch
    ) implements AstExpr {}

    record ElseIf(
            AstExpr condition,
            AstExpr thenBranch
    ) {}

    record While(
            AstExpr condition,
            AstExpr.Block body
    ) implements AstExpr {}

    record Function(
            Token nameToken,
            String name,
            AstExpr.Block body
    ) implements AstItem {
    }

    record Let(
            Token nameToken,
            String name,
            String type,
            AstExpr value
    ) implements AstItem {
    }
}
