package org.example.typecheck;

public sealed interface TypeInfo {
    enum Primitive implements TypeInfo {
        I32,
        F32,
        BOOL,
        ;
    }

    enum Void implements TypeInfo {
        VOID,
        ;
    }

    Void VOID = Void.VOID;

    Primitive I32 = Primitive.I32;
    Primitive F32 = Primitive.F32;
    Primitive BOOL = Primitive.BOOL;
}
