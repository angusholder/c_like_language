package org.example.parse;

import org.example.token.Token;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface Expr {

    record Number(
            String text
    ) implements Expr {
    }

    record Boolean(
            boolean value
    ) implements Expr {
    }

    record Identifier(
            String text
    ) implements Expr {
    }

    record Binary(
            Expr left,
            BinaryOp op,
            Expr right
    ) implements Expr {
    }

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
            Expr expr
    ) implements Expr {
    }

    enum UnaryOp {
        NOT,
        NEG,
    }

    record Call(
            String callee,
            List<Expr> arguments
    ) implements Expr {
    }

    record Block(
            List<Expr> items
    ) implements Expr {
    }

    record If(
            Expr condition,
            Expr.Block thenBranch,
            List<ElseIf> elseIfs,
            @Nullable
            Expr.Block elseBranch
    ) implements Expr {
    }

    record ElseIf(
            Expr condition,
            Expr.Block thenBranch
    ) {}

    record While(
            Expr condition,
            Expr.Block body
    ) implements Expr {
    }

    record Function(
            String name,
            @Nullable
            TypeExpr returnType,
            List<FuncParam> parameters,
            Expr.Block body
    ) implements Item {
    }

    record FuncParam(
            String name,
            TypeExpr type
    ) {}

    record Let(
            String name,
            TypeExpr type,
            Expr value
    ) implements Item {
    }

    /** The subset of expressions that are allowed at the top level in a file. */
    sealed interface Item extends Expr permits Function, Let {
    }

    record Assign(
            String lhs,
            Expr rhs
    ) implements Expr {
    }

    record Return(
            @Nullable
            Expr returnValue
    ) implements Expr {
    }
}
