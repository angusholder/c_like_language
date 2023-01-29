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
    private final IdentityHashMap<Expr.Identifier, Symbol.Value> resolvedVarSymbols = new IdentityHashMap<>();
    private final IdentityHashMap<Expr.Call, Symbol.Function> resolvedCallSites = new IdentityHashMap<>();
    private final IdentityHashMap<TypeExpr, TypeInfo> resolvedTypeRefs = new IdentityHashMap<>();
    private final IdentityHashMap<Expr.Function, Symbol.Function> functionDeclarations = new IdentityHashMap<>();
    private final IdentityHashMap<Symbol.Function, FunctionScope> functionScopes = new IdentityHashMap<>();

    public SymbolTable() {
    }

    public static class Scope {
        final Map<String, TypeInfo> typesNamespace = new LinkedHashMap<>();
        // Variables and functions exist in the same namespace
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

        public FunctionScope expectFunction() {
            if (functionScope != null) {
                return functionScope;
            }
            throw new IllegalStateException("This scope does not belong to a function: " + this);
        }
    }

    public static class FunctionScope {
        public final Symbol.Function symbol;
        public final Expr.Function expr;
        public final List<Symbol.Var> locals = new ArrayList<>();
        public final Symbol.Param[] params;

        public FunctionScope(Symbol.Function symbol, Expr.Function expr) {
            this.symbol = symbol;
            this.expr = expr;
            this.params = new Symbol.Param[symbol.params().size()];
        }
    }

    public record FileScope(
        Map<String, TypeInfo> types,
        Map<String, Symbol> valuesNamespace,
        Symbols symbols
    ) {
    }

    public void pushGlobalScope() {
        Scope scope = Scope.createGlobal();
        scopes.add(scope);
    }

    public void pushFunctionScope(Symbol.Function symbol, Expr.Function expr) {
        FunctionScope functionScope = new FunctionScope(symbol, expr);
        functionScopes.put(symbol, functionScope);
        Scope scope = new Scope(functionScope);
        scopes.add(scope);

        List<Symbol.FunctionParam> params = symbol.params();
        for (int i = 0; i < params.size(); i++) {
            Symbol.FunctionParam param = params.get(i);
            addSymbol(scope, new Symbol.Param(param.name().text(), param.type(), symbol, i, functionScope.locals.size()), param.name());
        }
    }

    public void pushBlockScope() {
        Scope scope = new Scope(getCurrentScope().expectFunction());
        scopes.add(scope);
    }

    public void popScope() {
        if (scopes.size() == 0) {
            throw new IllegalStateException("There's no scope to pop");
        }
        scopes.remove(scopes.size() - 1);
    }

    public FileScope popGlobalScope() {
        if (scopes.size() != 1) {
            throw new IllegalStateException("There should be only one scope left, had " + scopes.size());
        }
        Scope scope = scopes.remove(0);
        if (scope.functionScope != null) {
            throw new IllegalStateException("Global scope should not have a function scope");
        }
        return new FileScope(scope.typesNamespace, scope.valuesNamespace, Symbols.fromTable(this));
    }

    @Nullable
    public Symbol.Function getCurrentFunctionSymbol() {
        var current = getCurrentScope().functionScope;
        return current == null ? null : current.symbol;
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
    public Symbol.Value resolveValue(Expr.Identifier name) {
        Symbol.Value value = lookupSymbol(name.text()).expectValue();
        resolvedVarSymbols.put(name, value);
        return value;
    }

    @NotNull
    public Symbol.Function lookupFunction(Expr.Identifier name) {
        return lookupSymbol(name.text()).expectFunction();
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

    private void addSymbol(Scope scope, Symbol symbol, Expr.Identifier identifier) {
        scope.valuesNamespace.put(symbol.name(), symbol);
        if (symbol instanceof Symbol.Var var) {
            FunctionScope functionScope = scope.expectFunction();
            assert var.localIndex() == functionScope.locals.size();
            functionScope.locals.add(var);
            if (symbol instanceof Symbol.Param param) {
                functionScope.params[param.paramIndex()] = param;
            }
            resolvedVarSymbols.put(identifier, var);
        }
    }

    public void addVariableSymbol(Expr.Identifier name, TypeInfo type) {
        Scope scope = getCurrentScope();
        if (scope.functionScope == null) {
            addSymbol(scope, new Symbol.Global(name.text(), type), name);
        } else {
            addSymbol(scope, new Symbol.Local(name.text(), type, scope.functionScope.symbol, scope.functionScope.locals.size()), name);
        }
    }

    public void addFunctionSymbol(Expr.Function funcAst, List<Symbol.FunctionParam> params, TypeInfo returnType) {
        var symbol = new Symbol.Function(funcAst.name().text(), params, returnType);
        addSymbol(getCurrentScope(), symbol, funcAst.name());
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
            IdentityHashMap<Expr.Identifier, Symbol.Value> resolvedVarSymbols,
            IdentityHashMap<Expr.Call, Symbol.Function> resolvedCallSites,
            IdentityHashMap<Symbol.Function, FunctionScope> functionScopes
    ) {
        public static Symbols fromTable(SymbolTable table) {
            return new Symbols(
                    new IdentityHashMap<>(table.resolvedExprTypes),
                    new IdentityHashMap<>(table.resolvedVarSymbols),
                    new IdentityHashMap<>(table.resolvedCallSites),
                    new IdentityHashMap<>(table.functionScopes)
            );
        }

        @NotNull
        public Symbol.Value lookupValue(Expr.Identifier ident) {
            Symbol.Value var = resolvedVarSymbols.get(ident);
            if (var == null) {
                throw new IllegalStateException("Var " + ident + " was not resolved.");
            }
            return var;
        }

        @NotNull
        public Symbol.Function lookupCallSite(Expr.Call call) {
            Symbol.Function function = resolvedCallSites.get(call);
            if (function == null) {
                throw new IllegalStateException("Callsite " + call + " was not resolved.");
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

        @NotNull
        public FunctionScope lookupFunctionScope(Symbol.Function function) {
            FunctionScope scope = functionScopes.get(function);
            if (scope == null) {
                throw new IllegalStateException("Function scope for " + function + " was not resolved.");
            }
            return scope;
        }
    }
}
