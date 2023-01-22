package org.example.parse;

public sealed interface AstType {
    record Identifier(
            String name
    ) implements AstType {}
}
