package org.example.typecheck;

import org.example.CompilerCtx;
import org.example.parse.AstExpr;
import org.example.parse.AstFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TypeChecker {
    private final CompilerCtx ctx;
    private final SymbolTable table = new SymbolTable();

    public TypeChecker(CompilerCtx ctx) {
        this.ctx = ctx;
    }

    public void checkFile(AstFile file) {
        resolveExprList(file.items());
    }

    public <T extends AstExpr> void resolveExprList(List<T> exprList) {
        // TODO: 1) Resolve type definitions

        // 2) Resolve functions
        for (var expr : exprList) {
            if (expr instanceof AstExpr.Function function) {
                resolveFunctionSymbol(function);
            }
        }

        // 3) Resolve all other expressions
        for (var expr : exprList) {
            resolveExpr(expr);
        }
    }

    @NotNull
    private TypeInfo resolveExpr(AstExpr expr) {
        TypeInfo type = table.tryLookupExpr(expr);
        if (type == null) {
            type = checkExpr(expr);
            table.setExprType(expr, type);
        }
        return type;
    }

    @NotNull
    private TypeInfo checkExpr(AstExpr expr) {
        return switch (expr) {
            case AstExpr.Assign assign -> {
                Symbol.Value lhsSymbol = table.lookupValue(assign.lhs());
                TypeInfo rhsType = resolveExpr(assign.rhs());
                checkSame(lhsSymbol.valueType(), rhsType);
                yield TypeInfo.VOID;
            }
            case AstExpr.Binary binary -> checkBinaryExpr(binary);
            case AstExpr.Block block -> checkBlock(block);
            case AstExpr.Call call -> checkCall(call);
            case AstExpr.Identifier identifier -> table.lookupValue(identifier.text()).valueType();
            case AstExpr.If anIf -> checkIfStmt(anIf);
            case AstExpr.Number number -> {
                // TODO: Support more than one number type
                yield TypeInfo.I32;
            }
            case AstExpr.Boolean ignored -> TypeInfo.BOOL;
            case AstExpr.Unary unary -> {
                TypeInfo type = resolveExpr(unary.expr());
                yield switch (unary.op()) {
                    case NEG -> {
                        checkIsNumber(type);
                        yield type;
                    }
                    case NOT -> {
                        checkIsBool(type);
                        yield type;
                    }
                };
            }
            case AstExpr.While aWhile -> {
                TypeInfo condType = resolveExpr(aWhile.condition());
                checkIsBool(condType);
                resolveExpr(aWhile.body());
                yield TypeInfo.VOID;
            }
            case AstExpr.Function function -> checkFunctionBody(function);
            case AstExpr.Let let -> {
                TypeInfo type = table.resolveType(let.type());
                TypeInfo exprType = resolveExpr(let.value());
                checkSame(type, exprType);
                table.addVariableSymbol(let.name(), type);
                yield TypeInfo.VOID;
            }
            case AstExpr.Return ret -> {
                TypeInfo returnType;
                if (ret.returnValue() != null) {
                    returnType = resolveExpr(ret.returnValue());
                } else {
                    returnType = TypeInfo.VOID;
                }
                Symbol.Function currentFunction = table.getCurrentFunction();
                if (currentFunction == null) {
                    throw new IllegalStateException("Return statement outside of function");
                }
                checkSame(currentFunction.returnType(), returnType);
                // A return expression has type void. The return value is checked elsewhere.
                yield TypeInfo.VOID;
            }
        };
    }

    private TypeInfo checkIfStmt(AstExpr.If anIf) {
        checkIsBool(resolveExpr(anIf.condition()));
        TypeInfo thenType = resolveExpr(anIf.thenBranch());
        for (var elseif : anIf.elseIfs()) {
            checkIsBool(resolveExpr(elseif.condition()));
            TypeInfo elseifType = resolveExpr(elseif.thenBranch());
            checkSame(thenType, elseifType);
        }

        if (anIf.elseBranch() != null) {
            TypeInfo elseType = resolveExpr(anIf.elseBranch());
            checkSame(thenType, elseType);
            return thenType;
        } else {
            return TypeInfo.VOID;
        }
    }

    private void checkIsNumber(TypeInfo resolveExpr) {
        if (!resolveExpr.equals(TypeInfo.I32)) {
            throw new RuntimeException("Expected number, got " + resolveExpr);
        }
    }

    private TypeInfo checkCall(AstExpr.Call call) {
        Symbol.Function function = table.lookupFunction(call.callee());
        table.bindCallSite(call, function);

        if (function.params().size() != call.arguments().size()) {
            throw new RuntimeException("Expected " + function.params().size() + " arguments, got " + call.arguments().size());
        }
        for (int i = 0; i < function.params().size(); i++) {
            AstExpr arg = call.arguments().get(i);
            Symbol.FunctionParam param = function.params().get(i);
            var argType = resolveExpr(arg);
            checkSame(param.type(), argType);
        }

        return function.returnType();
    }

    private TypeInfo checkBlock(AstExpr.Block block) {
        if (block.items().isEmpty()) {
            return TypeInfo.VOID;
        } else {
            table.pushScope();
            resolveExprList(block.items());

            TypeInfo lastExprType = resolveExpr(block.items().get(block.items().size() - 1));
            table.popScope();

            return lastExprType;
        }
    }

    @NotNull
    private TypeInfo checkBinaryExpr(AstExpr.Binary binary) {
        var left = resolveExpr(binary.left());
        var right = resolveExpr(binary.right());

        return switch (binary.op()) {
            case ADD, SUB, MUL, DIV -> {
                checkSame(left, right);
                yield left;
            }
            case AND, OR -> {
                checkSame(left, TypeInfo.BOOL);
                checkSame(right, TypeInfo.BOOL);
                yield TypeInfo.BOOL;
            }
            case EQUALS, NOT_EQUALS, LT_EQ, LT, GT_EQ, GT -> {
                checkSame(left, right);
                yield TypeInfo.BOOL;
            }
        };
    }

    private void checkSame(TypeInfo left, TypeInfo right) {
        if (!left.equals(right)) {
            throw new RuntimeException("Type mismatch: " + left + " vs " + right);
        }
    }

    private void checkIsBool(TypeInfo type) {
        if (!type.equals(TypeInfo.BOOL)) {
            throw new RuntimeException("Expected bool, got " + type);
        }
    }

    private void resolveFunctionSymbol(AstExpr.Function function) {
        List<Symbol.FunctionParam> params = new ArrayList<>();
        for (var param : function.parameters()) {
            TypeInfo typeInfo = table.resolveType(param.type());
            params.add(new Symbol.FunctionParam(param.name(), typeInfo));
        }

        TypeInfo returnType;
        if (function.returnType() != null) {
            returnType = table.resolveType(function.returnType());
        } else {
            returnType = TypeInfo.VOID;
        }
        table.addFunctionSymbol(function, params, returnType);
    }

    @NotNull
    private TypeInfo checkFunctionBody(AstExpr.Function function) {
        table.pushScope();
        Symbol.Function funcSymbol = table.lookupFunction(function);
        table.setFunctionScope(funcSymbol);
        for (AstExpr item : function.body().items()) {
            resolveExpr(item);
        }
        table.popScope();

        return TypeInfo.VOID;
    }
}
