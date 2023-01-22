package org.example.parse;

import org.example.token.Token;

public sealed interface AstItem extends AstExpr {
    record Function(
            Token nameToken,
            String name,
            AstExpr.Block body
    ) implements AstItem {
    }

    record Let(
            Token nameToken,
            String name,
            Token type,
            AstExpr value
    ) implements AstItem {
    }
}
