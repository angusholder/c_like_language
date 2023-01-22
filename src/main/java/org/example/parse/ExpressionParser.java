package org.example.parse;

import org.example.token.Tokenizer;

public class ExpressionParser {
    private final Tokenizer tokenizer;

    public ExpressionParser(Parser parser) {
        this.tokenizer = parser.getTokenizer();
    }

    public AstExpr parse() {
        throw new RuntimeException("Not implemented");
    }
}
