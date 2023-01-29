package org.example.interpret;

import org.example.CompilerCtx;
import org.example.parse.Expr;
import org.example.typecheck.Symbol;
import org.example.typecheck.SymbolTable;
import org.example.typecheck.SymbolTable.FileScope;
import org.example.typecheck.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Objects;

public class TreeInterpreter {
    private final CompilerCtx ctx;
    private final FileScope fileScope;
    private StackFrame currentFrame;
    private final ArrayDeque<StackFrame> callStack = new ArrayDeque<>();

    public TreeInterpreter(CompilerCtx ctx, FileScope fileScope) {
        this.ctx = ctx;
        this.fileScope = fileScope;
        this.currentFrame = new StackFrame(lookupEntrypoint(fileScope));
    }

    private static class StackFrame {
        final SymbolTable.FunctionScope function;
        final Object[] locals;

        StackFrame(SymbolTable.FunctionScope function) {
            this.function = function;
            this.locals = new Object[function.locals.size()];
        }
    }

    private static SymbolTable.FunctionScope lookupEntrypoint(FileScope fileScope) {
        Symbol.Function entrypoint = fileScope.valuesNamespace().get("main").expectFunction();
        if (!entrypoint.params().isEmpty()) {
            throw new IllegalStateException("Entrypoint must have no parameters");
        }
        if (entrypoint.returnType() != TypeInfo.VOID) {
            throw new IllegalStateException("Entrypoint must return void");
        }
        return fileScope.symbols().lookupFunctionScope(entrypoint);
    }

    public void interpretFromEntrypoint() {
        eval(currentFrame.function.expr.body());
    }

    @Nullable
    private static Void voidValue() {
        return null;
    }

    private Object eval(Expr expr) {
        return switch (expr) {
            case Expr.Assign assign -> {
                Object rhs = eval(assign.rhs());
                Symbol.Value lhsSymbol = fileScope.symbols().lookupValue(assign.lhs());
                yield switch (lhsSymbol) {
                    case Symbol.Global global -> {
                        throw new UnsupportedOperationException("Global variables are not supported yet: " + global);
                    }
                    case Symbol.Var local -> {
                        currentFrame.locals[local.localIndex()] = rhs;
                        yield voidValue();
                    }
                };
            }
            case Expr.Binary binary -> {
                Object left = eval(binary.left());
                Object right = eval(binary.right());
                yield switch (binary.op()) {
                    case ADD -> (int) left + (int) right;
                    case SUB -> (int) left - (int) right;
                    case MUL -> (int) left * (int) right;
                    case DIV -> (int) left / (int) right;
                    case AND -> (boolean) left && (boolean) right;
                    case OR -> (boolean) left || (boolean) right;
                    case EQUALS -> Objects.equals(left, right);
                    case NOT_EQUALS -> !Objects.equals(left, right);
                    case LT_EQ -> (int) left <= (int) right;
                    case LT -> (int) left < (int) right;
                    case GT_EQ -> (int) left >= (int) right;
                    case GT -> (int) left > (int) right;
                };
            }
            case Expr.Block block -> {
                Object last = voidValue(); // void by default
                for (Expr item : block.items()) {
                    last = eval(item);
                }

                yield last;
            }
            case Expr.Boolean aBoolean -> {
                yield aBoolean.value();
            }
            case Expr.Call call -> {
                Symbol.Function callSite = fileScope.symbols().lookupCallSite(call);
                if (callSite.name().equals("println")) {
                    if (call.arguments().size() != 1) {
                        throw new IllegalStateException("println must have exactly one argument");
                    }
                    if (callSite.returnType() != TypeInfo.VOID) {
                        throw new IllegalStateException("println must return void");
                    }
                    System.out.println((int) eval(call.arguments().get(0)));
                    yield voidValue();
                }
                if (callSite.name().equals("printlnBool")) {
                    if (call.arguments().size() != 1) {
                        throw new IllegalStateException("printlnBool must have exactly one argument");
                    }
                    if (callSite.returnType() != TypeInfo.VOID) {
                        throw new IllegalStateException("printlnBool must return void");
                    }
                    System.out.println((boolean) eval(call.arguments().get(0)));
                    yield voidValue();
                }

                throw new UnsupportedOperationException("Function calls are not supported yet: " + call);
            }
            case Expr.Identifier identifier -> {
                Symbol.Value symbol = fileScope.symbols().lookupValue(identifier);
                yield switch (symbol) {
                    case Symbol.Global global -> {
                        throw new UnsupportedOperationException("Global variables are not supported yet: " + global);
                    }
                    case Symbol.Var local -> currentFrame.locals[local.localIndex()];
                };
            }
            case Expr.If anIf -> {
                if ((boolean) eval(anIf.condition())) {
                    yield eval(anIf.thenBranch());
                }
                for (var elseif : anIf.elseIfs()) {
                    if ((boolean) eval(elseif.condition())) {
                        yield eval(elseif.thenBranch());
                    }
                }
                if (anIf.elseBranch() != null) {
                    yield eval(anIf.elseBranch());
                } else {
                    yield voidValue();
                }
            }
            case Expr.Number number -> {
                yield Integer.parseInt(number.text());
            }
            case Expr.Return aReturn -> {
                throw new UnsupportedOperationException("Return statements are not supported yet: " + aReturn);
            }
            case Expr.Unary unary -> {
                Object inner = eval(unary.expr());
                yield switch (unary.op()) {
                    case NEG -> -(int) inner;
                    case NOT -> !(boolean) inner;
                };
            }
            case Expr.While aWhile -> {
                while ((boolean) eval(aWhile.condition())) {
                    eval(aWhile.body());
                }
                yield voidValue();
            }
            case Expr.Function function -> {
                // nothing needs doing with function expressions at runtime, we don't support closures or anything currently.
                yield voidValue();
            }
            case Expr.Let let -> {
                Object rhs = eval(let.value());
                Symbol.Value lhsSymbol = fileScope.symbols().lookupValue(let.name());
                yield switch (lhsSymbol) {
                    case Symbol.Global global -> {
                        throw new UnsupportedOperationException("Global variables are not supported yet: " + global);
                    }
                    case Symbol.Var local -> {
                        currentFrame.locals[local.localIndex()] = rhs;
                        yield voidValue();
                    }
                };
            }
        };
    }
}