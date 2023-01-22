package org.example.typecheck;

import org.example.parse.AstExpr;
import org.example.parse.AstType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SymbolTable {

    private final Scope globalScope = Scope.createGlobal();

    private final List<Scope> scopes = new ArrayList<>();
    private final IdentityHashMap<AstExpr, TypeInfo> resolvedExprTypes = new IdentityHashMap<>();
    private final IdentityHashMap<AstExpr.Call, Symbol.Function> resolvedCallSites = new IdentityHashMap<>();
    private final IdentityHashMap<AstType, TypeInfo> resolvedTypeRefs = new IdentityHashMap<>();
    private final IdentityHashMap<AstExpr.Function, Symbol.Function> resolvedFunctions = new IdentityHashMap<>();

    public SymbolTable() {
        scopes.add(globalScope);
    }

    public static class Scope {
        final Map<String, TypeInfo> typesNamespace = new LinkedHashMap<>();
        // TODO: Separate symbol list for functions?
        final Map<String, Symbol> valuesNamespace = new LinkedHashMap<>();

        public static Scope createGlobal() {
            Scope globalScope = new Scope();
            globalScope.typesNamespace.put("i32", TypeInfo.I32);
            globalScope.typesNamespace.put("f32", TypeInfo.F32);
            globalScope.typesNamespace.put("bool", TypeInfo.BOOL);
            globalScope.typesNamespace.put("void", TypeInfo.VOID);
            return globalScope;
        }
    }

    public void pushScope() {
        Scope scope = new Scope();
        scopes.add(scope);
    }

    public void popScope() {
        if (scopes.size() == 1) {
            throw new IllegalStateException("Cannot pop global scope");
        }
        scopes.remove(scopes.size() - 1);
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
    public TypeInfo resolveType(AstType type) {
        TypeInfo resolved = resolvedTypeRefs.get(type);
        if (resolved != null) {
            return resolved;
        }
        resolved = switch (type) {
            case AstType.Identifier ident -> lookupType(ident.name());
        };
        resolvedTypeRefs.put(type, resolved);
        return resolved;
    }

    @NotNull
    public TypeInfo lookupTypeRef(AstType type) {
        TypeInfo typeInfo = resolvedTypeRefs.get(type);
        if (typeInfo == null) {
            throw new IllegalStateException("Type ref " + type + " was not resolved.");
        }
        return typeInfo;
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
        Symbol symbol = lookupSymbol(name);
        if (symbol instanceof Symbol.Value value) {
            return value;
        }
        throw new IllegalArgumentException("Not a value: " + name);
    }

    @NotNull
    public Symbol.Function lookupFunction(String name) {
        Symbol symbol = lookupSymbol(name);
        if (symbol instanceof Symbol.Function function) {
            return function;
        }
        throw new IllegalArgumentException("Not a function: " + name);
    }

    @Nullable
    public TypeInfo tryLookupExpr(AstExpr expr) {
        return resolvedExprTypes.get(expr);
    }

    public void setExprType(AstExpr expr, TypeInfo type) {
        resolvedExprTypes.put(expr, type);
    }

    public void bindCallSite(AstExpr.Call call, Symbol.Function function) {
        // We don't currently support function pointers, only statically known functions.
        resolvedCallSites.put(call, function);
    }

    @NotNull
    public Symbol.Function lookupCallSite(AstExpr.Call call) {
        Symbol.Function function = resolvedCallSites.get(call);
        if (function == null) {
            throw new IllegalStateException("Callsite " + call + " was not resolved.");
        }
        return function;
    }

    private void addSymbol(Symbol symbol) {
        Scope scope = getCurrentScope();
        scope.valuesNamespace.put(symbol.name(), symbol);
    }

    public void addVariableSymbol(String name, TypeInfo type) {
        if (getCurrentScope() == globalScope) {
            addSymbol(new Symbol.Global(name, type));
        } else {
            addSymbol(new Symbol.Local(name, type));
        }
    }

    public void addParamSymbol(String name, TypeInfo type) {
        addSymbol(new Symbol.Param(name, type));
    }

    public void addFunctionSymbol(AstExpr.Function funcAst, List<Symbol.FunctionParam> params, TypeInfo returnType) {
        var symbol = new Symbol.Function(funcAst.name(), params, returnType);
        addSymbol(symbol);
        resolvedFunctions.put(funcAst, symbol);
    }

    public Symbol.Function lookupFunction(AstExpr.Function funcAst) {
        Symbol.Function function = resolvedFunctions.get(funcAst);
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
}