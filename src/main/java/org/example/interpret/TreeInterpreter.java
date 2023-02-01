package org.example.interpret;

import org.example.CompilerCtx;
import org.example.parse.Expr;
import org.example.typecheck.FunctionDefinition;
import org.example.typecheck.Symbol;
import org.example.typecheck.SymbolTable.FileScope;
import org.example.typecheck.TypeInfo;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.List;
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
        final FunctionDefinition function;
        private final Object[] locals;

        StackFrame(FunctionDefinition function) {
            this.function = function;
            this.locals = new Object[function.numLocals()];
        }

        Object getLocal(Symbol.Var var) {
            assert var.owner().equals(function.symbol());
            return locals[var.localIndex()];
        }

        void setLocal(Symbol.Var var, Object value) {
            assert var.owner().equals(function.symbol());
            locals[var.localIndex()] = value;
        }
    }

    private static FunctionDefinition lookupEntrypoint(FileScope fileScope) {
        Symbol.Function entrypoint = fileScope.valuesNamespace().get("main").expectFunction();
        if (!entrypoint.params().isEmpty()) {
            throw new IllegalStateException("Entrypoint must have no parameters");
        }
        if (entrypoint.returnType() != TypeInfo.VOID) {
            throw new IllegalStateException("Entrypoint must return void");
        }
        return fileScope.symbols().lookupFunctionScope(entrypoint);
    }

    public Object interpretFromEntrypoint() {
        return eval(currentFrame.function.expr().body());
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
                switch (lhsSymbol) {
                    case Symbol.Global global -> {
                        throw new UnsupportedOperationException("Global variables are not supported yet: " + global);
                    }
                    case Symbol.Var local -> {
                        currentFrame.setLocal(local, rhs);
                    }
                }
                yield voidValue();
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

                yield doFunctionCall(callSite, call.arguments());
            }
            case Expr.Identifier identifier -> {
                Symbol.Value symbol = fileScope.symbols().lookupValue(identifier);
                yield switch (symbol) {
                    case Symbol.Global global -> {
                        throw new UnsupportedOperationException("Global variables are not supported yet: " + global);
                    }
                    case Symbol.Var local -> currentFrame.getLocal(local);
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
                Symbol.Function function = currentFrame.function.symbol();
                if (aReturn.returnValue() != null) {
                    throw new ReturnException(eval(aReturn.returnValue()), function);
                } else {
                    throw new ReturnException(voidValue(), function);
                }
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
            case Expr.Function ignored -> {
                // nothing needs doing with function expressions at runtime, we don't support closures or anything currently.
                yield voidValue();
            }
            case Expr.Let let -> {
                Object rhs = eval(let.value());
                Symbol.Value lhsSymbol = fileScope.symbols().lookupValue(let.name());
                switch (lhsSymbol) {
                    case Symbol.Global global -> {
                        throw new UnsupportedOperationException("Global variables are not supported yet: " + global);
                    }
                    case Symbol.Var local -> {
                        currentFrame.setLocal(local, rhs);
                    }
                }
                yield voidValue();
            }
        };
    }

    private Object doFunctionCall(Symbol.Function callSite, List<Expr> arguments) {
        FunctionDefinition functionDefinition = fileScope.symbols().lookupFunctionScope(callSite);
        callStack.push(currentFrame);
        StackFrame newFrame = new StackFrame(functionDefinition);
        for (int i = 0; i < functionDefinition.params().length; i++) {
            Symbol.Param param = functionDefinition.params()[i];
            Expr arg = arguments.get(i);
            newFrame.setLocal(param, eval(arg));
        }

        currentFrame = newFrame;
        try {
            return interpretFromEntrypoint();
        } catch (ReturnException e) {
            if (!e.function.equals(callSite)) {
                throw new IllegalStateException("Return from function " + e.function + " but expected return from " + callSite, e);
            }
            return e.returnValue;
        } finally {
            currentFrame = callStack.pop();
        }
    }

    /**
     * Internal to this interpreter, used to implement return statements.
     */
    private static class ReturnException extends RuntimeException {
        /** The value to be returned from this function call, or voidValue() if it's a void-returning function. */
        final Object returnValue;
        /** For sanity checking, ensure our return matches with the expected function call. */
        final Symbol.Function function;

        public ReturnException(Object returnValue, Symbol.Function function) {
            this.returnValue = returnValue;
            this.function = function;
        }
    }
}
