package org.example.token;

import org.example.CompilerCtx;
import org.example.parse.ParsedFile;
import org.example.parse.Parser;
import org.example.typecheck.TypeChecker;
import org.junit.Test;

import static org.junit.Assert.assertThrows;

public class TypeCheckTest {
    @Test
    public void test() {
        checkTypes("""
        func println(a: i32) {}
        func printlnBool(a: bool) {}
        func main() -> i32 {
            let a: i32 = 1;
            let b: i32 = 2;
            let c: i32 = a + b;
            println(c);
            printlnBool(a+b == c);
            printlnBool(true);
            printlnBool(false);
            printlnBool((1 == 2) || (2 == 3));
            printlnBool((1 == 2) || true);
            printlnBool((1 == 2) == false);
            return 5;
        }
        
        func emptyFunc() {}
        func oneParam(a: i32) {}
        func oneParamCommad(a: i32,) {}
        func twoParams(a: i32, b: i32) {}
        func twoParamsComma(a: i32, b: i32,) {}
        func exprIf() -> i32 {
            return if (1 == 3) {
                1;
            } else if (1 == 2) {
                2;
            } else {
                3;
            };
        }
        """);
    }

    @Test
    public void test2() {
        assertThrows(RuntimeException.class, () -> {
            checkTypes("func foo() -> i32 { return true; }");
        });
    }

    @Test
    public void canReturnReturn() {
        checkTypes("func foo() -> void { return return; }");
    }

    public static void checkTypes(String source) {
        var ctx = new CompilerCtx();
        Parser parser = ctx.createParser(ctx.addInMemoryFile("anon-file", source));
        ParsedFile file = parser.parseFile();
        new TypeChecker(ctx).checkFile(file);
    }
}
