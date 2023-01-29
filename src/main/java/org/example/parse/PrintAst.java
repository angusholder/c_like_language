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

    private void println(Expr.Identifier identifier) {
        println(identifier.text());
    }

    public void visit(ParsedFile file) {
        println(file.file().name());
        indented(() -> {
            for (Expr.Item item : file.items()) {
                visit(item);
            }
        });
    }

    public void visit(Expr bodyItem) {
        switch (bodyItem) {
            case Expr.Binary binary -> {
                println(binary.op().name());
                indented(() -> {
                    visit(binary.left());
                    visit(binary.right());
                });
            }
            case Expr.Block block -> {
                println("Block:");
                indented(() -> {
                    for (Expr item : block.items()) {
                        visit(item);
                    }
                });
            }
            case Expr.Call call -> {
                println("Call:");
                indented(() -> {
                    println(call.callee());
                    for (Expr arg : call.arguments()) {
                        visit(arg);
                    }
                });
            }
            case Expr.If anIf -> {
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
            case Expr.Unary unary -> {
                println(unary.op().name());
                indented(() -> visit(unary.expr()));
            }
            case Expr.While aWhile -> {
                println("while");
                indented(() -> visit(aWhile.condition()));
                println("do");
                indented(() -> visit(aWhile.body()));
            }
            case Expr.Number number -> {
                println("Number: " + number.text());
            }
            case Expr.Identifier identifier -> {
                println("Identifier: " + identifier.text());
            }
            case Expr.Boolean bool -> {
                println(String.valueOf(bool.value()));
            }
            case Expr.Function function -> {
                println("Function: " + function.name());
                indented(() -> {
                    if (function.returnType() != null) {
                        println("Return type: " + function.returnType());
                    }
                    println("Params:");
                    indented(() -> {
                        for (var param : function.parameters()) {
                            println(param.name());
                            println(param.type().toString());
                        }
                    });
                    println("Body:");

                    indented(() -> {
                        for (Expr expr : function.body().items()) {
                            visit(expr);
                        }
                    });
                });
            }
            case Expr.Let let -> {
                println("Let");
                indented(() -> {
                    println(let.name());
                    println(let.type().toString());
                    visit(let.value());
                });
            }
            case Expr.Assign assign -> {
                println("Assign");
                indented(() -> {
                    println(assign.lhs());
                    visit(assign.rhs());
                });
            }
            case Expr.Return ret -> {
                println("return");
                if (ret.returnValue() != null) {
                    indented(() -> visit(ret.returnValue()));
                }
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
