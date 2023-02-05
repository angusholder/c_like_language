package org.example;

import org.example.codegen.Codegen;
import org.example.interpret.TreeInterpreter;
import org.example.parse.*;
import org.example.token.SourceLoc;
import org.example.token.SourceSpan;
import org.example.token.Token;
import org.example.token.TokenType;
import org.example.token.Tokenizer;
import org.example.typecheck.SymbolTable;
import org.example.typecheck.TypeChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.System.out;

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

    public final SymbolTable symbols = new SymbolTable();

    public final IdentityHashMap<Expr, Token> exprStarts = new IdentityHashMap<>();
    public final IdentityHashMap<Expr, Token> exprEnds = new IdentityHashMap<>();

    public record Error(
            SourceSpan span,
            String message
    ) {}

    private final List<Error> errors = new ArrayList<>();

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
        return new Parser(createTokenizer(file), this);
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
        Token start = exprStarts.get(expr);
        Token end = exprEnds.get(expr);
        if (start.fileUid() != end.fileUid()) {
            throw new IllegalStateException("start and end tokens are in different files");
        }
        return getSourceSpan(start.fileUid(), start.startOffset(), end.endOffset());
    }

    public FileInfo getFile(int fileUid) {
        return files.get(fileUid);
    }

    public ParseError reportParseError(SourceSpan span, String message) {
        Error err = new Error(span, message);
        errors.add(err);
        CompileErrors.printError(err);
        return new ParseError();
    }

    public static class ParseError extends RuntimeException {
    }

    public CompileErrors getCompileErrors() {
        return new CompileErrors(errors);
    }

    public boolean didError() {
        return !errors.isEmpty();
    }

    public void checkSourceRangeInfoIsPresent(ParsedFile file) {
        for (Expr.Item item : file.items()) {
            checkSourceRangeInfoIsPresent(item);
        }
    }

    public void checkSourceRangeInfoIsPresent(Expr firstExpr) {
        Expr.traverseAll(firstExpr, expr -> {
            if (!exprStarts.containsKey(expr)) {
                throw new IllegalStateException("missing start token for " + expr);
            }
            if (!exprEnds.containsKey(expr)) {
                throw new IllegalStateException("missing end token for " + expr);
            }
        });
    }

    /** Helper method for testing out the Tokenizer. */
    public static List<TokenType> tokenize(String source) {
        var ctx = new CompilerCtx();
        Tokenizer tokenizer = ctx.createTokenizer(ctx.addInMemoryFile("anon-file", source));
        ArrayList<Token> tokens = tokenizer.tokenizeAll();
        return tokens.stream().map(Token::type).toList();
    }

    /** Helper method for testing out the Tokenizer. */
    public static void printTokens(String source) {
        var ctx = new CompilerCtx();
        Tokenizer tokenizer = ctx.createTokenizer(ctx.addInMemoryFile("anon-file", source));
        ArrayList<Token> tokens = tokenizer.tokenizeAll();
        for (Token token : tokens) {
            out.println(token.format(ctx));
        }
    }

    /** Helper method for testing out the parser. */
    public static void parseAndPrintTree(String source) {
        var ctx = new CompilerCtx();
        Parser parser = ctx.createParser(ctx.addInMemoryFile("anon-file", source));
        if (ctx.didError()) {
            ctx.getCompileErrors().print();
            return;
        }
        ParsedFile file = parser.parseFile();
        new PrintAst().visit(file);
    }

    /** Helper method for testing out the parser. */
    public static Expr parseExpr(String source) {
        var ctx = new CompilerCtx();
        Parser parser = ctx.createParser(ctx.addInMemoryFile("anon-file", source));
        return parser.parseExpr();
    }

    /** Helper method for testing out the type checker. */
    public static void checkTypes(String source) {
        var ctx = new CompilerCtx();
        Parser parser = ctx.createParser(ctx.addInMemoryFile("anon-file", source));
        ParsedFile file = parser.parseFile();
        if (ctx.didError()) {
            ctx.getCompileErrors().print();
            return;
        }
        new TypeChecker(ctx).checkFile(file);
    }

    public static void codeEmitForExpression(String source) {
        System.out.println("\nCodegen for expression: " + source);
        var ctx = new CompilerCtx();
        Parser parser = ctx.createParser(ctx.addInMemoryFile("anon-file", source));
        Expr expr = parser.parseExpr();
        if (ctx.didError()) {
            ctx.getCompileErrors().print();
            return;
        }
        new TypeChecker(ctx).resolveExpr(expr);
        if (ctx.didError()) {
            ctx.getCompileErrors().print();
            return;
        }
        Codegen codegen = new Codegen(ctx);
        codegen.emitCode(expr);
    }

    public static void interpret(String source) {
        var ctx = new CompilerCtx();
        Parser parser = ctx.createParser(ctx.addInMemoryFile("anon-file", source));
        ParsedFile file = parser.parseFile();
        if (ctx.didError()) {
            ctx.getCompileErrors().print();
            return;
        }
        SymbolTable.FileScope fileScope = new TypeChecker(ctx).checkFile(file);
        if (ctx.didError()) {
            ctx.getCompileErrors().print();
            return;
        }
        new TreeInterpreter(ctx, fileScope).interpretFromEntrypoint();
    }

    @NotNull
    public static String readResource(String path) {
        String source;
        try (InputStream is = CompilerCtx.class.getResourceAsStream(path)) {
            if (is == null) {
                throw new FileNotFoundException("resource not found: " + path);
            }
            source = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return source;
    }
}
