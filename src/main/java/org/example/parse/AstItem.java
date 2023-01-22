package org.example.parse;

public sealed interface AstItem extends AstExpr permits AstExpr.Function, AstExpr.Let {
}
