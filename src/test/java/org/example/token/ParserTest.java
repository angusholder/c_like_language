package org.example.token;

import org.example.CompilerCtx;
import org.example.parse.AstFile;
import org.example.parse.Parser;
import org.example.parse.PrintAst;
import org.junit.Test;

public class ParserTest {
    @Test
    public void test() {
        printParseTree("""
        func main() -> int {
            let a: int = 1;
            let b: int = 2;
            let c: int = a + b;
            println(c);
            println(c == a+b);
        }
        
        func emptyFunc() -> void {}
        func oneParam(a: int) {}
        func oneParamCommad(a: int,) {}
        func twoParams(a: int, b: int) {}
        func twoParamsComma(a: int, b: int,) {}
        """);
    }

    public static void printParseTree(String source) {
        var ctx = new CompilerCtx();
        Parser parser = ctx.createParser(ctx.addInMemoryFile("anon-file", source));
        AstFile file = parser.parseFile();
        new PrintAst().visit(file);
    }
}
