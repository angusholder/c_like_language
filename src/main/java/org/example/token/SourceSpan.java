package org.example.token;

public record SourceSpan(
        SourceLoc start,
        SourceLoc end,
        String text
) {
    public String formattedLocation() {
        if (start.offset() == end.offset()) {
            return "L%d:%d".formatted(start.line(), start.column());
        } else if (start.line() == end.line()) {
            return "L%d:%d-%d".formatted(start.line(), start.column(), end.column());
        } else {
            return "L%d:%d - L%d-%d".formatted(start.line(), start.column(), end.line(), end.column());
        }
    }
}
