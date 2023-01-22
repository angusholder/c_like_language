package org.example.parse;

import java.util.List;

public record AstFile(
        List<AstItem> items
) {
}
