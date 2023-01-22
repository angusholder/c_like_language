package org.example.parse;

import org.example.CompilerCtx;

import java.util.List;

public record AstFile(
        CompilerCtx.FileInfo file,
        List<AstExpr.Item> items
) {
}
