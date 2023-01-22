package org.example.token;

public record SourceLoc(
        int offset,
        int line,
        int column
) {
}
