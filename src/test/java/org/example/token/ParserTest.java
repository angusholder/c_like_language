package org.example.token;

import org.example.CompilerCtx;
import org.example.parse.ParsedFile;
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
        CompilerCtx.parseAndPrintTree(source);
    }
}
