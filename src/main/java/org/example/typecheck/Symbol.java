package org.example.typecheck;

import java.util.List;

public sealed interface Symbol {
    record Global(
            String name,
            TypeInfo valueType
    ) implements Value {
    }

    record Local(
            String name,
            TypeInfo valueType
    ) implements Value {
    }

    record Param(
            String name,
            TypeInfo valueType
    ) implements Value {
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

    sealed interface Value extends Symbol {
        TypeInfo valueType();
    }

    String name();
}
