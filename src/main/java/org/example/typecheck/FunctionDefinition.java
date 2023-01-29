package org.example.typecheck;

import org.example.parse.Expr;

// This maybe belongs in the interpreter package?
// It's basically an immutable version of the SymbolTable.FunctionScope class.
public record FunctionDefinition(
        Symbol.Function symbol,
        Expr.Function expr,
        int numLocals,
        Symbol.Param[] params
) {
}
