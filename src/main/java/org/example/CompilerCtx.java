package org.example;

import org.example.parse.Expr;
import org.example.parse.Parser;
import org.example.token.SourceLoc;
import org.example.token.SourceSpan;
import org.example.token.Token;
import org.example.token.Tokenizer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CompilerCtx {
    public record FileInfo(
            int uid,
            @Nullable
            Path path,
            String name,
            String contents
    ) {}

    private final AtomicInteger nextUid = new AtomicInteger(1);
    private final Map<Integer, FileInfo> files = new LinkedHashMap<>();

    public CompilerCtx() {

    }

    private FileInfo addFile(FileInfo file) {
        files.put(file.uid(), file);
        return file;
    }

    public FileInfo addFile(Path path, String name, String contents) {
        return addFile(new FileInfo(nextUid.getAndIncrement(), path, name, contents));
    }

    public FileInfo addInMemoryFile(String name, String contents) {
        int uid = nextUid.getAndIncrement();
        return addFile(new FileInfo(uid, null, name + "-" + uid, contents));
    }

    public Tokenizer createTokenizer(FileInfo file) {
        return new Tokenizer(this, file);
    }

    public Parser createParser(FileInfo file) {
        return new Parser(createTokenizer(file));
    }

    private SourceLoc getSourceLocation(FileInfo file, int offset) {
        int line = 1;
        int column = 1;
        var source = file.contents();
        for (int i = 0; i < offset; i++) {
            if (source.charAt(i) == '\n') {
                line += 1;
                column = 1;
            } else {
                column += 1;
            }
        }
        return new SourceLoc(offset, line, column);
    }

    @NotNull
    public SourceSpan getSourceSpan(int fileUid, int start, int end) {
        var file = getFile(fileUid);
        return new SourceSpan(
                getSourceLocation(file, start),
                getSourceLocation(file, end - 1),
                file.contents().substring(start, end)
        );
    }

    public SourceSpan getSourceSpan(Token token) {
        return getSourceSpan(token.fileUid(), token.startOffset(), token.endOffset());
    }

    public SourceSpan getSourceSpan(Expr expr) {
        Expr.TokenRange range = expr.getTokenRange();
        return getSourceSpan(range.fileUid(), range.startOffset(), range.endOffset());
    }

    public FileInfo getFile(int fileUid) {
        return files.get(fileUid);
    }
}
