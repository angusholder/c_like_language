package org.example.parse;

import org.example.token.Token;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface AstExpr {

    record Number(
            String text
    ) implements AstExpr {}

    record Identifier(
            String text
    ) implements AstExpr {}

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
            List<FuncParam> parameters,
            AstExpr.Block body
    ) implements Item {
    }

    record FuncParam(
            String name,
            AstType type
    ) {}

    record Let(
            Token nameToken,
            String name,
            AstType type,
            AstExpr value
    ) implements Item {
    }

    /** The subset of expressions that are allowed at the top level in a file. */
    sealed interface Item extends AstExpr permits Function, Let {
    }
}
