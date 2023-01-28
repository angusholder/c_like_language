package org.example.typecheck;

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
            Function owner
    ) implements Symbol, Value, Var {
    }

    record Param(
            String name,
            TypeInfo valueType,
            Function owner,
            int paramIndex
    ) implements Symbol, Value, Var {
    }

    record Function(
            String name,
            List<FunctionParam> params,
            TypeInfo returnType
    ) implements Symbol {
    }

    record FunctionParam(
            String name,
            TypeInfo type
    ) {}

    sealed interface Var extends Value {
        Function owner();
    }

    sealed interface Value extends Symbol {
        TypeInfo valueType();
    }

    String name();
}
