package org.example.parse;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

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
            Identifier callee,
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
            Identifier name,
            @Nullable
            TypeExpr returnType,
            List<FuncParam> parameters,
            Expr.Block body
    ) implements Item {
    }

    record FuncParam(
            Identifier name,
            TypeExpr type
    ) {}

    record Let(
            Identifier name,
            TypeExpr type,
            Expr value
    ) implements Item {
    }

    /** The subset of expressions that are allowed at the top level in a file. */
    sealed interface Item extends Expr permits Function, Let {
    }

    record Assign(
            Identifier lhs,
            Expr rhs
    ) implements Expr {
    }

    record Return(
            @Nullable
            Expr returnValue
    ) implements Expr {
    }

    static void traverseAll(Expr firstExpr, Consumer<Expr> callback) {
        callback.accept(firstExpr);
        switch (firstExpr) {
            case Binary binary -> {
                traverseAll(binary.left(), callback);
                traverseAll(binary.right(), callback);
            }
            case Unary unary -> traverseAll(unary.expr(), callback);
            case Call call -> call.arguments().forEach(arg -> traverseAll(arg, callback));
            case Block block -> block.items().forEach(item -> traverseAll(item, callback));
            case If anIf -> {
                traverseAll(anIf.condition(), callback);
                traverseAll(anIf.thenBranch(), callback);
                anIf.elseIfs().forEach(elseIf -> {
                    traverseAll(elseIf.condition(), callback);
                    traverseAll(elseIf.thenBranch(), callback);
                });
                if (anIf.elseBranch() != null) {
                    traverseAll(anIf.elseBranch(), callback);
                }
            }
            case While aWhile -> {
                traverseAll(aWhile.condition(), callback);
                traverseAll(aWhile.body(), callback);
            }
            case Function function -> {
                traverseAll(function.body(), callback);
            }
            case Let let -> {
                traverseAll(let.value(), callback);
            }
            case Assign assign -> traverseAll(assign.rhs(), callback);
            case Return ret -> {
                if (ret.returnValue() != null) {
                    traverseAll(ret.returnValue(), callback);
                }
            }
            case Boolean ignored -> {
            }
            case Identifier ignored -> {
            }
            case Number ignored -> {
            }
        }
    }
}
