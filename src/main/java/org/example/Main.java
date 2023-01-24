package org.example;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Arguments required:");
            System.err.println("  tokenize|parse|typecheck <file_path>");
            System.err.println("  codegen <expression>");
            return;
        }

        String operation = args[0];
        switch (operation) {
            case "tokenize" -> {
                String sourceCode = readFile(args[1]);
                if (sourceCode == null) return;
                CompilerCtx.printTokens(sourceCode);
            }
            case "parse" -> {
                String sourceCode = readFile(args[1]);
                if (sourceCode == null) return;
                CompilerCtx.parseAndPrintTree(sourceCode);
            }
            case "typecheck" -> {
                String sourceCode = readFile(args[1]);
                if (sourceCode == null) return;
                CompilerCtx.checkTypes(sourceCode);
            }
            case "codegen" -> {
                String exprSrc;
                try {
                    exprSrc = new String(System.in.readAllBytes());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                CompilerCtx.codeEmitForExpression(exprSrc);
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
