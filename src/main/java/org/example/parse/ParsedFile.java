package org.example.parse;

import org.example.CompilerCtx;

import java.util.List;

public record ParsedFile(
        CompilerCtx.FileInfo file,
        List<Expr.Item> items
) {
}
