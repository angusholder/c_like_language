package org.example.typecheck;

import org.example.parse.Expr;
import org.example.parse.TypeExpr;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    private final List<Scope> scopes = new ArrayList<>();
    private final IdentityHashMap<Expr, TypeInfo> resolvedExprTypes = new IdentityHashMap<>();
    private final IdentityHashMap<Expr.Call, Symbol.Function> resolvedCallSites = new IdentityHashMap<>();
    private final IdentityHashMap<TypeExpr, TypeInfo> resolvedTypeRefs = new IdentityHashMap<>();
    private final IdentityHashMap<Expr.Function, Symbol.Function> functionDeclarations = new IdentityHashMap<>();
    private final IdentityHashMap<Symbol.Function, FunctionScope> functionScopes = new IdentityHashMap<>();

    public SymbolTable() {
    }

    public static class Scope {
        final Map<String, TypeInfo> typesNamespace = new LinkedHashMap<>();
        // TODO: Separate symbol list for functions?
        final Map<String, Symbol> valuesNamespace = new LinkedHashMap<>();
        @Nullable
        final SymbolTable.FunctionScope functionScope;

        public Scope(@Nullable SymbolTable.FunctionScope function) {
            this.functionScope = function;
        }

        @Override
        public String toString() {
            return "Scope{" +
                    "currentFunction=" + functionScope +
                    '}';
        }

        public static Scope createGlobal() {
            Scope globalScope = new Scope(null);
            globalScope.typesNamespace.put("i32", TypeInfo.I32);
            globalScope.typesNamespace.put("f32", TypeInfo.F32);
            globalScope.typesNamespace.put("bool", TypeInfo.BOOL);
            globalScope.typesNamespace.put("void", TypeInfo.VOID);
            return globalScope;
        }
    }

    public static class FunctionScope {
        public final Symbol.Function symbol;
        public final List<Symbol.Var> locals = new ArrayList<>();

        public FunctionScope(Symbol.Function symbol) {
            this.symbol = symbol;
        }
    }

    public void pushGlobalScope() {
        Scope scope = Scope.createGlobal();
        scopes.add(scope);
    }

    public void pushFunctionScope(Symbol.Function symbol) {
        FunctionScope functionScope = new FunctionScope(symbol);
        functionScopes.put(symbol, functionScope);
        Scope scope = new Scope(functionScope);
        scopes.add(scope);

        List<Symbol.FunctionParam> params = symbol.params();
        for (int i = 0; i < params.size(); i++) {
            Symbol.FunctionParam param = params.get(i);
            addSymbol(new Symbol.Param(param.name(), param.type(), symbol, i));
        }
    }

    public void pushBlockScope() {
        Scope scope = new Scope(expectCurrentFunction());
        scopes.add(scope);
    }

    public void popScope() {
        if (scopes.size() == 0) {
            throw new IllegalStateException("There's no scope to pop");
        }
        scopes.remove(scopes.size() - 1);
    }

    @Nullable
    public Symbol.Function getCurrentFunctionSymbol() {
        var current = getCurrentScope().functionScope;
        return current == null ? null : current.symbol;
    }

    @NotNull
    public SymbolTable.FunctionScope expectCurrentFunction() {
        Scope scope = getCurrentScope();
        if (scope.functionScope != null) {
            return scope.functionScope;
        }
        throw new IllegalStateException("This scope does not belong to a function: " + scope);
    }

    @NotNull
    private TypeInfo lookupType(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Scope scope = scopes.get(i);
            TypeInfo type = scope.typesNamespace.get(name);
            if (type != null) {
                return type;
            }
        }

        throw new IllegalArgumentException("Unknown type: " + name);
    }

    @NotNull
    public TypeInfo resolveType(@NotNull TypeExpr type) {
        TypeInfo resolved = resolvedTypeRefs.get(type);
        if (resolved != null) {
            return resolved;
        }
        resolved = switch (type) {
            case TypeExpr.Identifier ident -> lookupType(ident.name());
        };
        resolvedTypeRefs.put(type, resolved);
        return resolved;
    }

    @NotNull
    public Symbol lookupSymbol(String name) {
        for (int i = scopes.size() - 1; i >= 0; i--) {
            Scope scope = scopes.get(i);
            Symbol symbol = scope.valuesNamespace.get(name);
            if (symbol != null) {
                return symbol;
            }
        }

        throw new IllegalArgumentException("Unknown symbol: " + name);
    }

    @NotNull
    public Symbol.Value lookupValue(String name) {
        return lookupSymbol(name).expectValue();
    }

    @NotNull
    public Symbol.Function lookupFunction(String name) {
        return lookupSymbol(name).expectFunction();
    }

    @Nullable
    public TypeInfo tryLookupExpr(Expr expr) {
        return resolvedExprTypes.get(expr);
    }

    public void setExprType(Expr expr, TypeInfo type) {
        resolvedExprTypes.put(expr, type);
    }

    public void bindCallSite(Expr.Call call, Symbol.Function function) {
        // We don't currently support function pointers, only statically known functions.
        resolvedCallSites.put(call, function);
    }

    private void addSymbol(Symbol symbol) {
        Scope scope = getCurrentScope();
        scope.valuesNamespace.put(symbol.name(), symbol);
        if (symbol instanceof Symbol.Var var) {
            expectCurrentFunction().locals.add(var);
        }
    }

    public void addVariableSymbol(String name, TypeInfo type) {
        Scope scope = getCurrentScope();
        if (scope.functionScope == null) {
            addSymbol(new Symbol.Global(name, type));
        } else {
            addSymbol(new Symbol.Local(name, type, scope.functionScope.symbol));
        }
    }

    public void addFunctionSymbol(Expr.Function funcAst, List<Symbol.FunctionParam> params, TypeInfo returnType) {
        var symbol = new Symbol.Function(funcAst.name(), params, returnType);
        addSymbol(symbol);
        functionDeclarations.put(funcAst, symbol);
    }

    public Symbol.Function lookupFunction(Expr.Function funcAst) {
        Symbol.Function function = functionDeclarations.get(funcAst);
        if (function == null) {
            throw new IllegalStateException("Function " + funcAst + " was not resolved.");
        }
        return function;
    }

    private Scope getCurrentScope() {
        if (scopes.isEmpty()) {
            throw new IllegalStateException("No scopes");
        }
        return scopes.get(scopes.size() - 1);
    }

    public record Symbols(
            IdentityHashMap<Expr, TypeInfo> resolvedExprTypes,
            IdentityHashMap<Expr.Call, Symbol.Function> resolvedCallSites,
            IdentityHashMap<TypeExpr, TypeInfo> resolvedTypeRefs,
            IdentityHashMap<Expr.Function, Symbol.Function> resolvedFunctions
    ) {
        @NotNull
        public Symbol.Function lookupCallSite(Expr.Call call) {
            Symbol.Function function = resolvedCallSites.get(call);
            if (function == null) {
                throw new IllegalStateException("Callsite " + call + " was not resolved.");
            }
            return function;
        }

        @NotNull
        public TypeInfo lookupTypeRef(TypeExpr type) {
            TypeInfo typeInfo = resolvedTypeRefs.get(type);
            if (typeInfo == null) {
                throw new IllegalStateException("Type ref " + type + " was not resolved.");
            }
            return typeInfo;
        }

        @NotNull
        public Symbol.Function lookupFunction(Expr.Function funcAst) {
            Symbol.Function function = resolvedFunctions.get(funcAst);
            if (function == null) {
                throw new IllegalStateException("Function " + funcAst + " was not resolved.");
            }
            return function;
        }

        @NotNull
        public TypeInfo lookupExprType(Expr expr) {
            TypeInfo typeInfo = resolvedExprTypes.get(expr);
            if (typeInfo == null) {
                throw new IllegalStateException("Expr " + expr + " was not resolved.");
            }
            return typeInfo;
        }
    }
}
