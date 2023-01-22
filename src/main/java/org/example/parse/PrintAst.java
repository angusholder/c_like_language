package org.example.parse;

import java.io.PrintStream;

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
            if (i == indent - 1)
                stream.print("└─");
            else
                stream.print("  ");
        }
        stream.println(s);
    }

    public void visit(AstFile file) {
        println(file.file().name());
        indented(() -> {
            for (AstExpr.Item item : file.items()) {
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
                println("Call:");
                indented(() -> {
                    println(call.callee());
                    for (AstExpr arg : call.arguments()) {
                        visit(arg);
                    }
                });
            }
            case AstExpr.If anIf -> {
                println("if");
                indented(() -> visit(anIf.condition()));
                println("then");
                indented(() -> visit(anIf.thenBranch()));
                for (var elseif : anIf.elseIfs()) {
                    println("elseif");
                    indented(() -> visit(elseif.condition()));
                    indented(() -> visit(elseif.thenBranch()));
                }
                if (anIf.elseBranch() != null) {
                    println("else");
                    indented(() -> visit(anIf.elseBranch()));
                }
            }
            case AstExpr.Unary unary -> {
                println(unary.op().name());
                indented(() -> visit(unary.expr()));
            }
            case AstExpr.While aWhile -> {
                println("while");
                indented(() -> visit(aWhile.condition()));
                println("do");
                indented(() -> visit(aWhile.body()));
            }
            case AstExpr.Number number -> {
                println("Number: " + number.text());
            }
            case AstExpr.Identifier identifier -> {
                println("Identifier: " + identifier.text());
            }
            case AstExpr.Item.Function function -> {
                println("Function: " + function.name());
                indented(() -> {
                    println("Params:");
                    indented(() -> {
                        for (var param : function.parameters()) {
                            println(param.name());
                            println(param.type().toString());
                        }
                    });
                    println("Body:");

                    indented(() -> {
                        for (AstExpr expr : function.body().items()) {
                            visit(expr);
                        }
                    });
                });
            }
            case AstExpr.Item.Let let -> {
                println("Let");
                indented(() -> {
                    println(let.name());
                    println(let.type().toString());
                    visit(let.value());
                });
            }
            case AstExpr.Assign assign -> {
                println("Assign");
                indented(() -> {
                    println(assign.lhs());
                    visit(assign.rhs());
                });
            }
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
}
