package org.example.parse;

/**
 * A type as written out in source code, eg `i32`, `*f32`, or `[5]i64`.
 */
public sealed interface TypeExpr {
    record Identifier(
            String name
    ) implements TypeExpr {}
}
