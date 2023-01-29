package org.example.typecheck;

import org.example.parse.Expr;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public sealed interface Symbol {
    record Global(
            String name,
            TypeInfo valueType
    ) implements Symbol, Value {
    }

    record Local(
            String name,
            TypeInfo valueType,
            Function owner,
            int localIndex
    ) implements Symbol, Value, Var {
    }

    record Param(
            String name,
            TypeInfo valueType,
            Function owner,
            int paramIndex,
            int localIndex
    ) implements Symbol, Value, Var {
    }

    record Function(
            String name,
            List<FunctionParam> params,
            TypeInfo returnType
    ) implements Symbol {
    }

    record FunctionParam(
            Expr.Identifier name,
            TypeInfo type
    ) {}

    sealed interface Var extends Value {
        Function owner();
        int localIndex();
    }

    sealed interface Value extends Symbol {
        TypeInfo valueType();
    }

    String name();

    @NotNull
    default Symbol.Value expectValue() {
        if (this instanceof Symbol.Value value) {
            return value;
        }
        throw new IllegalArgumentException("Not a returnValue: " + this);
    }

    @NotNull
    default Symbol.Function expectFunction() {
        if (this instanceof Symbol.Function function) {
            return function;
        }
        throw new IllegalArgumentException("Not a function: " + this);
    }
}
