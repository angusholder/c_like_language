package org.example.token;

import org.example.CompilerCtx;
import org.example.parse.Expr;
import org.example.parse.Expr.BinaryOp;
import org.example.parse.ParsedFile;
import org.example.parse.Parser;
import org.example.parse.PrintAst;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    @Test
    public void test2() {
        checkExprMatches("1+2*3", new Expr.Binary(
            new Expr.Number("1"),
            BinaryOp.ADD,
            new Expr.Binary(
                new Expr.Number("2"),
                BinaryOp.MUL,
                new Expr.Number("3")
            )
        ));
    }

    private static void checkExprMatches(String source, Expr expected) {
        Expr expr = CompilerCtx.parseExpr(source);
        assertEquals(expected, expr);
    }

    public static void printParseTree(String source) {
        CompilerCtx.parseAndPrintTree(source);
    }
}
