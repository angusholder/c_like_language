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

    public void visit(AstItem item) {
        indented(() -> {
            switch (item) {
                case AstItem.Function function -> {
                    println("Function: " + function.name());
                    indented(() -> {
                        println("Args: []");
                        println("Body:");

                        indented(() -> {
                            for (AstExpr bodyItem : function.body().items()) {
                                visit(bodyItem);
                            }
                        });
                    });
                }
                case AstItem.Let let -> {
                    println("Let " + let.name());
                }
            }
        });
    }

    private void visit(AstExpr bodyItem) {
        switch (bodyItem) {
            case AstExpr.Binary binary -> {
                throw new UnsupportedOperationException();
            }
            case AstExpr.Block block -> {
                throw new UnsupportedOperationException();
            }
            case AstExpr.Call call -> {
                throw new UnsupportedOperationException();
            }
            case AstExpr.If anIf -> {
                throw new UnsupportedOperationException();
            }
            case AstExpr.Item item -> {
                visit(item.item());
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
                throw new UnsupportedOperationException();
            }
            case AstItem.Let let -> {
                throw new UnsupportedOperationException();
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
