package org.example.parse;

import org.example.token.Token;

public sealed interface AstItem extends AstExpr permits AstExpr.Function, AstExpr.Let {
}
