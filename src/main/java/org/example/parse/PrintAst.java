package org.example.parse;

import java.io.PrintStream;
import java.util.function.Supplier;

public class PrintAst {
    private final PrintStream stream;
    private int indent = 0;

    public PrintAst(PrintStream stream) {
        this.stream = stream;
    }

    public PrintAst() {
        this(System.out);
    }

    private void println(String s) {
        for (int i = 0; i < indent; i++) {
            stream.print("  ");
        }
        stream.println(s);
    }

    public void visit(AstFile file) {
        indented(() -> {
            for (AstItem item : file.items()) {
                visit(item);
            }
        });
    }

    public void visit(AstExpr bodyItem) {
        switch (bodyItem) {
            case AstExpr.Binary binary -> {
                println(binary.op().name());
                indented(() -> {
                    visit(binary.left());
                    visit(binary.right());
                });
            }
            case AstExpr.Block block -> {
                println("Block:");
                indented(() -> {
                    for (AstExpr item : block.items()) {
                        visit(item);
                    }
                });
            }
            case AstExpr.Call call -> {
                throw new UnsupportedOperationException();
            }
            case AstExpr.If anIf -> {
                throw new UnsupportedOperationException();
            }
            case AstExpr.Unary unary -> {
                throw new UnsupportedOperationException();
            }
            case AstExpr.While aWhile -> {
                println("while");
                indented(() -> visit(aWhile.condition()));
                println("do");
                indented(() -> visit(aWhile.body()));
            }
            case AstExpr.Atom atom -> {
                visit(atom);
            }
            case AstItem.Function function -> {
                println("Function: " + function.name());
                indented(() -> {
                    println("Args: []");
                    println("Body:");

                    indented(() -> {
                        for (AstExpr expr : function.body().items()) {
                            visit(expr);
                        }
                    });
                });
            }
            case AstItem.Let let -> {
                println("Let " + let.name());
            }
        }
    }

    private void visit(AstExpr.Atom atom) {
        switch (atom) {
            case AstExpr.Number number -> {
                println("Number: " + number.text());
            }
            case AstExpr.Identifier identifier -> {
                println("Identifier: " + identifier.text());
            }
        }
    }

    private <T> T indented(Supplier<T> block) {
        int prevIndent = indent;
        indent++;
        try {
            return block.get();
        } finally {
            indent = prevIndent;
        }
    }

    private void indented(Runnable block) {
        int prevIndent = indent;
        indent++;
        try {
            block.run();
        } finally {
            indent = prevIndent;
        }
    }

    private interface Idented {
        void close();
    }
}
