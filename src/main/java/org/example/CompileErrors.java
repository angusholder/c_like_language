package org.example;

import org.example.token.SourceSpan;

import java.util.List;

public record CompileErrors(
        List<CompilerCtx.Error> errors
) {
    public void print() {
        for (var error : errors) {
            printError(error);
        }
    }

    public static void printError(CompilerCtx.Error error) {
        SourceSpan span = error.span();
        System.err.println("[" + span.formattedLocation() + "] '" + span.text() + "': " + error.message() + "\n");
    }
}
