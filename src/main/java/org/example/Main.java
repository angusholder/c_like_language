package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Arguments required: tokenize|parse|typecheck <file_path>");
            return;
        }

        String sourceCode = readFile(args[1]);
        if (sourceCode == null) return;

        String operation = args[0];
        switch (operation) {
            case "tokenize" -> {
                CompilerCtx.printTokens(sourceCode);
            }
            case "parse" -> {
                CompilerCtx.parseAndPrintTree(sourceCode);
            }
            case "typecheck" -> {
                CompilerCtx.checkTypes(sourceCode);
            }
            default -> {
                System.err.println("Unknown operation: " + operation);
            }
        }
    }

    private static String readFile(String path) {
        try {
            return Files.readString(Path.of(path));
        } catch (IOException e) {
            System.err.println("Failed to read file");
            return null;
        }
    }
}
